"""Shared model helpers: timestamp columns and CHECK-constraint builder."""
from datetime import datetime
from enum import Enum

from sqlalchemy import DateTime, func
from sqlalchemy.orm import Mapped, mapped_column


class TimestampMixin:
    """created_at / updated_at, stored in UTC by PostgreSQL (timestamptz)."""

    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False
    )


def in_check(column: str, enum_cls: type[Enum]) -> str:
    """SQL text such as: role IN ('CUSTOMER', 'PROVIDER', 'BUSINESS')."""
    values = ", ".join(f"'{member.value}'" for member in enum_cls)
    return f"{column} IN ({values})"