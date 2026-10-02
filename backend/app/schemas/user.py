"""User profile schemas."""
from datetime import datetime
from typing import Annotated

from pydantic import BaseModel, ConfigDict, EmailStr, Field, StringConstraints, field_validator

from app.enums import UserRole
from app.utils.validation import normalize_phone, validate_password_strength

FullName = Annotated[str, StringConstraints(strip_whitespace=True, min_length=2, max_length=100)]


class UserOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    full_name: str
    email: str
    phone: str | None
    role: UserRole
    avatar_url: str | None
    energy_plan_id: int | None
    created_at: datetime


class UserUpdate(BaseModel):
    """Partial profile update: only the fields that are sent are changed."""

    full_name: FullName | None = None
    email: EmailStr | None = None
    phone: str | None = None

    @field_validator("email")
    @classmethod
    def _lowercase_email(cls, value: str | None) -> str | None:
        return value.lower() if value else value

    @field_validator("phone")
    @classmethod
    def _check_phone(cls, value: str | None) -> str | None:
        return normalize_phone(value)


class PasswordChange(BaseModel):
    current_password: str = Field(min_length=1, max_length=128)
    new_password: str

    @field_validator("new_password")
    @classmethod
    def _check_strength(cls, value: str) -> str:
        return validate_password_strength(value)