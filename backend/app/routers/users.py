"""User profile endpoints. Users can only read and change their OWN profile."""
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.auth.dependencies import get_current_user
from app.database import get_db
from app.models import User
from app.schemas.common import MessageResponse, error_responses
from app.schemas.user import PasswordChange, UserOut, UserUpdate
from app.services import user_service
from app.utils.errors import ForbiddenError

router = APIRouter(prefix="/api/users", tags=["Users"])


def _own_user_or_error(db: Session, current_user: User, user_id: int) -> User:
    target = user_service.get_user_or_404(db, user_id)
    if target.id != current_user.id:
        raise ForbiddenError("You can only access your own profile")
    return target


@router.get(
    "/{user_id}",
    response_model=UserOut,
    summary="Get your profile",
    responses=error_responses(400, 401, 403, 404),
)
def get_user(
    user_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return UserOut.model_validate(_own_user_or_error(db, current_user, user_id))


@router.put(
    "/{user_id}",
    response_model=UserOut,
    summary="Update your name, phone or email",
    responses=error_responses(400, 401, 403, 404, 409),
)
def update_user(
    user_id: int,
    payload: UserUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    target = _own_user_or_error(db, current_user, user_id)
    return UserOut.model_validate(user_service.update_profile(db, target, payload))


@router.put(
    "/{user_id}/password",
    response_model=MessageResponse,
    summary="Change your password",
    responses=error_responses(400, 401, 403, 404),
)
def change_password(
    user_id: int,
    payload: PasswordChange,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    target = _own_user_or_error(db, current_user, user_id)
    user_service.change_password(db, target, payload.current_password, payload.new_password)
    return MessageResponse(detail="Password updated")