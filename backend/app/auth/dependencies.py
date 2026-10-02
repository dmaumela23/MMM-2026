"""FastAPI dependencies for authentication and role-based authorisation."""
from fastapi import Depends
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.orm import Session

from app.auth.jwt import TokenError, decode_access_token
from app.database import get_db
from app.enums import UserRole
from app.models import User
from app.utils.errors import ForbiddenError, UnauthorizedError

# auto_error=False so that WE return a consistent 401 {"detail": ...} response.
bearer_scheme = HTTPBearer(auto_error=False, description="Paste the access_token from /api/auth/login")

_WWW_AUTH = {"WWW-Authenticate": "Bearer"}


def get_current_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
    db: Session = Depends(get_db),
) -> User:
    """Resolve the logged-in user from the bearer token, or raise 401."""
    if credentials is None:
        raise UnauthorizedError("Authentication required. Please log in.", headers=_WWW_AUTH)
    try:
        payload = decode_access_token(credentials.credentials)
        user_id = int(payload["sub"])
    except TokenError as exc:
        raise UnauthorizedError(str(exc), headers=_WWW_AUTH) from exc
    except (KeyError, ValueError) as exc:
        raise UnauthorizedError("Invalid authentication token.", headers=_WWW_AUTH) from exc

    user = db.get(User, user_id)
    if user is None:
        raise UnauthorizedError("This account no longer exists.", headers=_WWW_AUTH)
    return user


def require_roles(*allowed: UserRole, message: str | None = None):
    """Build a dependency that only lets the given roles through (403 otherwise).

    The role is read from the database on every request, never trusted from the token.
    """
    allowed_values = {role.value for role in allowed}

    def dependency(user: User = Depends(get_current_user)) -> User:
        if user.role not in allowed_values:
            raise ForbiddenError(message or "Your account type is not allowed to do this.")
        return user

    return dependency


require_business = require_roles(
    UserRole.BUSINESS, message="Only Business accounts can create or manage products."
)
require_seller = require_roles(
    UserRole.PROVIDER,
    UserRole.BUSINESS,
    message="Only Provider or Business accounts can do this.",
)