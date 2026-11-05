"""Application settings loaded from environment variables (and an optional .env file).

No secrets live in source code. If a required variable is missing or too weak,
the application refuses to start with a clear validation error.
"""
import re
from functools import lru_cache
from pathlib import Path

from pydantic import Field, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

BASE_DIR = Path(__file__).resolve().parent.parent


def normalise_database_url(value: str) -> str:
    """Hosting providers give postgres:// or postgresql://. SQLAlchemy needs the driver name."""
    if value.startswith("postgres://"):
        value = "postgresql://" + value[len("postgres://"):]
    if value.startswith("postgresql://"):
        value = "postgresql+psycopg2://" + value[len("postgresql://"):]
    return value


def parse_client_ids(*values: str | None) -> list[str]:
    """Turn 'a.apps.googleusercontent.com, b.apps.googleusercontent.com' into a list.

    Entries may be separated by commas, semicolons or whitespace. Blanks and duplicates are dropped
    and the order is kept.
    """
    found: list[str] = []
    for value in values:
        for part in re.split(r"[,;\s]+", value or ""):
            if part and part not in found:
                found.append(part)
    return found


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=BASE_DIR / ".env", env_file_encoding="utf-8", extra="ignore"
    )

    database_url: str
    jwt_secret: str = Field(min_length=32)
    jwt_algorithm: str = "HS256"
    jwt_expire_minutes: int = Field(default=1440, ge=1)
    bcrypt_rounds: int = Field(default=12, ge=4, le=16)
    seed_demo_password: str | None = None
    test_database_url: str | None = None

    # Google single sign-on. These are OAuth client IDs from Google Cloud Console. A Google ID token is
    # only accepted if it was issued FOR one of them (its "aud" claim). Client IDs are public identifiers,
    # not secrets, but they live in the environment (and Render) rather than in the code.
    # Leave both unset to switch Google sign-in off.
    google_client_id: str | None = None   # one client ID
    google_client_ids: str | None = None  # or several, separated by commas

    @field_validator("database_url", "test_database_url")
    @classmethod
    def _fix_database_url(cls, value: str | None) -> str | None:
        return normalise_database_url(value) if value else value

    @property
    def google_audiences(self) -> list[str]:
        """Every Google client ID the server accepts tokens for."""
        return parse_client_ids(self.google_client_id, self.google_client_ids)


@lru_cache
def get_settings() -> Settings:
    return Settings()


settings = get_settings()
