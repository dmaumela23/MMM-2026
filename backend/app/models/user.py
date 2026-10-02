"""users and user_settings tables."""
from typing import Optional

from sqlalchemy import Boolean, CheckConstraint, ForeignKey, String, true
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base
from app.enums import Theme, UserRole
from app.models.mixins import TimestampMixin, in_check


class User(TimestampMixin, Base):
    __tablename__ = "users"
    __table_args__ = (CheckConstraint(in_check("role", UserRole), name="ck_users_role"),)

    id: Mapped[int] = mapped_column(primary_key=True)
    full_name: Mapped[str] = mapped_column(String(100))
    email: Mapped[str] = mapped_column(String(255), unique=True, index=True)
    phone: Mapped[str | None] = mapped_column(String(20))
    password_hash: Mapped[str] = mapped_column(String(255))  # BCrypt hash, never the password
    role: Mapped[str] = mapped_column(String(20), default=UserRole.CUSTOMER.value)
    avatar_url: Mapped[str | None] = mapped_column(String(500))
    energy_plan_id: Mapped[int | None] = mapped_column(
        ForeignKey("energy_plans.id", ondelete="SET NULL")
    )

    energy_plan: Mapped[Optional["EnergyPlan"]] = relationship()
    settings: Mapped["UserSettings"] = relationship(
        back_populates="user", uselist=False, cascade="all, delete-orphan"
    )


class UserSettings(TimestampMixin, Base):
    __tablename__ = "user_settings"
    __table_args__ = (CheckConstraint(in_check("theme", Theme), name="ck_user_settings_theme"),)

    id: Mapped[int] = mapped_column(primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), unique=True)
    notification_enabled: Mapped[bool] = mapped_column(
        Boolean, default=True, server_default=true()
    )
    theme: Mapped[str] = mapped_column(
        String(10), default=Theme.SYSTEM.value, server_default=Theme.SYSTEM.value
    )
    fcm_token: Mapped[str | None] = mapped_column(String(1024))

    user: Mapped["User"] = relationship(back_populates="settings")