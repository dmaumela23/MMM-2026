"""Validation helpers shared by Pydantic schemas and the seed script."""
import re

PASSWORD_MIN_LENGTH = 8
PASSWORD_MAX_LENGTH = 64
BCRYPT_MAX_BYTES = 72  # BCrypt ignores/rejects anything beyond 72 bytes

_PHONE_RE = re.compile(r"\+?[0-9]{9,15}")
_URL_RE = re.compile(r"https?://\S+", re.IGNORECASE)


def validate_password_strength(password: str) -> str:
    """8-64 characters (and <= 72 bytes for BCrypt), at least one letter and one digit."""
    if len(password) < PASSWORD_MIN_LENGTH:
        raise ValueError(f"Password must be at least {PASSWORD_MIN_LENGTH} characters long")
    if len(password) > PASSWORD_MAX_LENGTH or len(password.encode("utf-8")) > BCRYPT_MAX_BYTES:
        raise ValueError(f"Password must be at most {PASSWORD_MAX_LENGTH} characters long")
    if not any(char.isalpha() for char in password):
        raise ValueError("Password must contain at least one letter")
    if not any(char.isdigit() for char in password):
        raise ValueError("Password must contain at least one digit")
    return password


def normalize_phone(value: str | None) -> str | None:
    """Optional '+' followed by 9-15 digits. Spaces, dashes and brackets are removed."""
    if value is None:
        return None
    cleaned = re.sub(r"[\s\-()]", "", value.strip())
    if cleaned == "":
        return None
    if not _PHONE_RE.fullmatch(cleaned):
        raise ValueError("Phone number must have 9 to 15 digits and may start with +")
    return cleaned


def normalize_image_url(value: str | None) -> str | None:
    if value is None:
        return None
    cleaned = value.strip()
    if cleaned == "":
        return None
    if not _URL_RE.fullmatch(cleaned):
        raise ValueError("image_url must be a valid http:// or https:// URL")
    return cleaned