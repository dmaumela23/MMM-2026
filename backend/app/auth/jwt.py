"""JWT creation and verification (HS256, secret from the environment)."""
from datetime import datetime, timedelta, timezone

import jwt as pyjwt  # PyJWT

from app.config import settings


class TokenError(Exception):
    """Raised when a token is missing claims, tampered with, or expired."""


def create_access_token(user_id: int, expires_delta: timedelta | None = None) -> str:
    now = datetime.now(timezone.utc)
    lifetime = expires_delta if expires_delta is not None else timedelta(minutes=settings.jwt_expire_minutes)
    payload = {"sub": str(user_id), "iat": now, "exp": now + lifetime}
    return pyjwt.encode(payload, settings.jwt_secret, algorithm=settings.jwt_algorithm)


def decode_access_token(token: str) -> dict:
    try:
        return pyjwt.decode(
            token,
            settings.jwt_secret,
            algorithms=[settings.jwt_algorithm],  # never trust the algorithm named in the token
            options={"require": ["exp", "sub"]},
        )
    except pyjwt.ExpiredSignatureError as exc:
        raise TokenError("Session expired. Please log in again.") from exc
    except pyjwt.PyJWTError as exc:
        raise TokenError("Invalid authentication token.") from exc