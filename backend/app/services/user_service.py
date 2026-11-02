"""Profile, password and settings logic."""
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.auth.hashing import hash_password, verify_password
from app.enums import Theme
from app.models import User, UserSettings
from app.schemas.settings import SettingsOut, SettingsUpdate
from app.schemas.user import UserUpdate
from app.utils.errors import BadRequestError, ConflictError, NotFoundError

EMAIL_TAKEN = "An account with this email already exists"


def get_user_or_404(db: Session, user_id: int) -> User:
    user = db.get(User, user_id)
    if user is None:
        raise NotFoundError("User not found")
    return user


def update_profile(db: Session, user: User, data: UserUpdate) -> User:
    changes = data.model_dump(exclude_unset=True)
    for required in ("full_name", "email"):
        if required in changes and changes[required] is None:
            raise BadRequestError(f"{required} cannot be empty")

    if "email" in changes and changes["email"] != user.email:
        taken = db.scalar(select(User.id).where(User.email == changes["email"], User.id != user.id))
        if taken is not None:
            raise ConflictError(EMAIL_TAKEN)

    for field, value in changes.items():
        setattr(user, field, value)
    try:
        db.commit()
    except IntegrityError as exc:
        db.rollback()
        raise ConflictError(EMAIL_TAKEN) from exc
    db.refresh(user)
    return user


def change_password(db: Session, user: User, current_password: str, new_password: str) -> None:
    if not verify_password(current_password, user.password_hash):
        raise BadRequestError("Current password is incorrect")
    if current_password == new_password:
        raise BadRequestError("New password must be different from the current password")
    user.password_hash = hash_password(new_password)
    db.commit()


def _get_or_create_settings(db: Session, user: User) -> UserSettings:
    settings = user.settings
    if settings is None:  # defensive: every registered user gets a row
        settings = UserSettings(user_id=user.id)
        db.add(settings)
        db.flush()
    return settings


def _settings_to_out(settings: UserSettings) -> SettingsOut:
    return SettingsOut(
        notification_enabled=settings.notification_enabled,
        theme=settings.theme,
        fcm_token=settings.fcm_token,
    )


def get_settings(db: Session, user: User) -> SettingsOut:
    settings = _get_or_create_settings(db, user)
    db.commit()
    return _settings_to_out(settings)


def update_settings(db: Session, user: User, data: SettingsUpdate) -> SettingsOut:
    changes = data.model_dump(exclude_unset=True)
    for required in ("notification_enabled", "theme"):
        if required in changes and changes[required] is None:
            raise BadRequestError(f"{required} cannot be null")

    settings = _get_or_create_settings(db, user)
    if "notification_enabled" in changes:
        settings.notification_enabled = changes["notification_enabled"]
    if "theme" in changes:
        settings.theme = Theme(changes["theme"]).value
    if "fcm_token" in changes:  # null clears the token (notifications switched off)
        settings.fcm_token = changes["fcm_token"]
    db.commit()
    db.refresh(settings)
    return _settings_to_out(settings)