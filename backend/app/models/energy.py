"""energy_plans and energy_usage tables. ALL energy data is simulated."""
from datetime import date
from decimal import Decimal

from sqlalchemy import (
    Boolean,
    CheckConstraint,
    Date,
    Float,
    ForeignKey,
    Integer,
    Numeric,
    String,
    Text,
    UniqueConstraint,
    true,
)
from sqlalchemy.orm import Mapped, mapped_column

from app.database import Base
from app.models.mixins import TimestampMixin


class EnergyPlan(TimestampMixin, Base):
    __tablename__ = "energy_plans"
    __table_args__ = (
        CheckConstraint("price_per_kwh >= 0", name="ck_energy_plans_price_non_negative"),
        CheckConstraint("monthly_fee >= 0", name="ck_energy_plans_fee_non_negative"),
        CheckConstraint("max_kwh > 0", name="ck_energy_plans_max_kwh_positive"),
    )

    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(80), unique=True)
    description: Mapped[str] = mapped_column(Text, default="")
    price_per_kwh: Mapped[Decimal] = mapped_column(Numeric(10, 2))
    monthly_fee: Mapped[Decimal] = mapped_column(Numeric(12, 2))
    max_kwh: Mapped[int] = mapped_column(Integer)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, server_default=true())


class EnergyUsage(TimestampMixin, Base):
    __tablename__ = "energy_usage"
    __table_args__ = (
        # One reading per user per day; the unique index also speeds up date-range queries.
        UniqueConstraint("user_id", "recorded_on", name="uq_energy_usage_user_date"),
        CheckConstraint("kwh_used >= 0", name="ck_energy_usage_kwh_non_negative"),
    )

    id: Mapped[int] = mapped_column(primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"))
    recorded_on: Mapped[date] = mapped_column(Date)
    kwh_used: Mapped[float] = mapped_column(Float)
    is_simulated: Mapped[bool] = mapped_column(Boolean, default=True, server_default=true())