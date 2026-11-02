"""User settings schemas."""
from pydantic import BaseModel, Field, field_validator

from app.enums import Theme


class SettingsOut(BaseModel):
    notification_enabled: bool
    theme: Theme
    fcm_token: str | None


class SettingsUpdate(BaseModel):
    """Partial update. Send "fcm_token": null to clear the token."""

    notification_enabled: bool | None = None
    theme: Theme | None = None
    fcm_token: str | None = Field(default=None, max_length=1024)

    @field_validator("fcm_token")
    @classmethod
    def _blank_token_is_none(cls, value: str | None) -> str | None:
        if value is None:
            return None
        cleaned = value.strip()
        return cleaned or None