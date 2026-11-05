"""Registration and login logic: email + password, and Google single sign-on."""
import logging
import secrets
from functools import lru_cache

from google.auth.exceptions import GoogleAuthError, TransportError
from google.auth.transport import requests as google_requests
from google.oauth2 import id_token as google_id_token
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.auth.hashing import hash_password, verify_password
from app.config import settings
from app.enums import UserRole
from app.models import User, UserSettings
from app.schemas.auth import RegisterRequest
from app.services import energy_service
from app.utils.errors import ConflictError, ServiceUnavailableError, UnauthorizedError

logger = logging.getLogger("mmm.auth")

INVALID_CREDENTIALS = "Incorrect email or password"
EMAIL_TAKEN = "An account with this email already exists"
GOOGLE_FAILED = "Google sign-in failed. Please try again."
GOOGLE_UNVERIFIED = "Your Google account's email address is not verified."
GOOGLE_NOT_CONFIGURED = "Google sign-in is not available right now."
GOOGLE_UNREACHABLE = "Could not reach Google to verify your sign-in. Please try again."


@lru_cache(maxsize=1)
def _dummy_hash() -> str:
    # Verified against when the email is unknown so response time doesn't reveal which emails exist.
    return hash_password("not-a-real-password-1")


def register_user(db: Session, data: RegisterRequest) -> User:
    """Create the user, default settings, default energy plan and 90 days of simulated usage."""
    if db.scalar(select(User.id).where(User.email == data.email)) is not None:
        raise ConflictError(EMAIL_TAKEN)

    plan = energy_service.get_default_plan(db)
    user = User(
        full_name=data.full_name,
        email=data.email,
        phone=data.phone,
        password_hash=hash_password(data.password),  # only the hash is stored
        role=data.role.value,
        energy_plan_id=plan.id,
        settings=UserSettings(),
    )
    db.add(user)
    try:
        db.flush()  # assigns user.id; raises IntegrityError on a race for the same email
        energy_service.generate_usage_history(db, user.id)
        db.commit()
    except IntegrityError as exc:
        db.rollback()
        raise ConflictError(EMAIL_TAKEN) from exc
    except Exception:
        db.rollback()
        raise
    db.refresh(user)
    return user


def authenticate_user(db: Session, email: str, password: str) -> User:
    user = db.scalar(select(User).where(User.email == email))
    if user is None:
        verify_password(password, _dummy_hash())
        raise UnauthorizedError(INVALID_CREDENTIALS)
    if not verify_password(password, user.password_hash):
        raise UnauthorizedError(INVALID_CREDENTIALS)
    return user


# --------------------------------------------------------------------------- Google single sign-on
def _verify_google_token(token: str) -> dict:
    """Ask Google's library to check the token's signature (against Google's public keys), its expiry
    and its issuer (accounts.google.com). Raises ValueError if anything is wrong.

    The AUDIENCE is deliberately not checked here: authenticate_google compares it with the client IDs
    this server accepts, so several clients (web, Android...) can be allowed. This function is separate
    so the tests can replace it without calling Google.

    Reference: Google Identity, "Verify the Google ID token on your server",
    https://developers.google.com/identity/gsi/web/guides/verify-google-id-token
    """
    return google_id_token.verify_oauth2_token(token, google_requests.Request())


def _display_name(claims: dict, email: str) -> str:
    name = str(claims.get("name") or "").strip()
    if len(name) < 2:
        name = email.split("@")[0].strip()
    if len(name) < 2:
        name = "MMM user"
    return name[:100]


def authenticate_google(db: Session, raw_token: str, role: UserRole) -> User:
    """Sign in with Google. The app sends the ID token Google issued; this function VERIFIES it and then
    - finds the MMM account with the same (verified) email, or
    - creates a new account for it.
    Nothing from the request body except the token (and, for a NEW account, the role) is trusted.
    """
    audiences = settings.google_audiences
    if not audiences:
        raise ServiceUnavailableError(GOOGLE_NOT_CONFIGURED)

    try:
        claims = _verify_google_token(raw_token)
    except TransportError as exc:  # could not download Google's public keys
        logger.warning("Google token check failed: Google unreachable (%s)", exc)
        raise ServiceUnavailableError(GOOGLE_UNREACHABLE) from exc
    except (ValueError, GoogleAuthError) as exc:  # bad signature, expired, wrong issuer
        logger.warning("Rejected a Google token: %s", exc)
        raise UnauthorizedError(GOOGLE_FAILED) from exc

    # A valid Google token for SOME OTHER app must never sign anyone in to MMM.
    if claims.get("aud") not in audiences:
        logger.warning("Rejected a Google token issued for a different app (aud=%s)", claims.get("aud"))
        raise UnauthorizedError(GOOGLE_FAILED)

    email = str(claims.get("email") or "").strip().lower()
    if not email or claims.get("email_verified") not in (True, "true"):
        logger.warning("Rejected a Google token: email missing or not verified")
        raise UnauthorizedError(GOOGLE_UNVERIFIED)

    user = db.scalar(select(User).where(User.email == email))
    if user is not None:
        logger.info("Google sign-in for existing user id=%s", user.id)
        return user
    return _create_google_user(db, email, claims, role)


def _create_google_user(db: Session, email: str, claims: dict, role: UserRole) -> User:
    plan = energy_service.get_default_plan(db)
    picture = str(claims.get("picture") or "").strip()[:500] or None
    user = User(
        full_name=_display_name(claims, email),
        email=email,
        phone=None,
        # Google users have no password. This is the BCrypt hash of a random value nobody knows, so
        # password login can never succeed for them. (The column is NOT NULL, so it cannot stay empty.)
        password_hash=hash_password(secrets.token_urlsafe(48)),
        role=role.value,
        avatar_url=picture,
        energy_plan_id=plan.id,
        settings=UserSettings(),
    )
    db.add(user)
    try:
        db.flush()
        energy_service.generate_usage_history(db, user.id)
        db.commit()
    except IntegrityError:
        # Two sign-ins for the same new email at once: the other one won, so use that account.
        db.rollback()
        existing = db.scalar(select(User).where(User.email == email))
        if existing is None:
            raise
        return existing
    except Exception:
        db.rollback()
        raise
    db.refresh(user)
    logger.info("Created a new account from Google sign-in: user id=%s role=%s", user.id, user.role)
    return user
