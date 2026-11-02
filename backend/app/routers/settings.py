"""User settings endpoints (notification preference, theme, FCM token)."""
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.auth.dependencies import get_current_user
from app.database import get_db
from app.models import User
from app.schemas.common import error_responses
from app.schemas.settings import SettingsOut, SettingsUpdate
from app.services import user_service

router = APIRouter(prefix="/api/settings", tags=["Settings"])


@router.get("", response_model=SettingsOut, summary="Get my settings", responses=error_responses(401))
def get_settings(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    return user_service.get_settings(db, current_user)


@router.put(
    "",
    response_model=SettingsOut,
    summary="Update my settings (partial update)",
    responses=error_responses(400, 401),
)
def update_settings(
    payload: SettingsUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return user_service.update_settings(db, current_user, payload)