"""Registration and login schemas."""
from pydantic import BaseModel, EmailStr, Field, field_validator, model_validator

from app.enums import UserRole
from app.schemas.user import FullName, UserOut
from app.utils.validation import normalize_phone, validate_password_strength


class RegisterRequest(BaseModel):
    full_name: FullName
    email: EmailStr
    phone: str | None = None
    password: str
    confirm_password: str
    role: UserRole

    @field_validator("email")
    @classmethod
    def _lowercase_email(cls, value: str) -> str:
        return value.lower()

    @field_validator("phone")
    @classmethod
    def _check_phone(cls, value: str | None) -> str | None:
        return normalize_phone(value)

    @field_validator("password")
    @classmethod
    def _check_strength(cls, value: str) -> str:
        return validate_password_strength(value)

    @model_validator(mode="after")
    def _passwords_match(self) -> "RegisterRequest":
        if self.password != self.confirm_password:
            raise ValueError("Passwords do not match")
        return self


class LoginRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=1, max_length=128)

    @field_validator("email")
    @classmethod
    def _lowercase_email(cls, value: str) -> str:
        return value.lower()


class GoogleLoginRequest(BaseModel):
    """The ID token Google gave the app (Credential Manager). It is verified by the SERVER, never trusted."""

    id_token: str = Field(min_length=20, max_length=4096)
    # Only used when this Google account has no MMM account yet and one is created.
    role: UserRole = UserRole.CUSTOMER


class AuthResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: UserOut
