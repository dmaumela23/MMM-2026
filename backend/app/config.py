"""Application settings loaded from environment variables (and an optional .env file).

No secrets live in source code. If a required variable is missing or too weak,
the application refuses to start with a clear validation error.
"""
from functools import lru_cache
from pathlib import Path

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict

BASE_DIR = Path(__file__).resolve().parent.parent


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


@lru_cache
def get_settings() -> Settings:
    return Settings()


settings = get_settings()