"""Registration and login logic."""
from functools import lru_cache

from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.auth.hashing import hash_password, verify_password
from app.models import User, UserSettings
from app.schemas.auth import RegisterRequest
from app.services import energy_service
from app.utils.errors import ConflictError, UnauthorizedError

INVALID_CREDENTIALS = "Incorrect email or password"
EMAIL_TAKEN = "An account with this email already exists"


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