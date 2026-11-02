"""services table (service listings offered by providers/businesses)."""
from decimal import Decimal

from sqlalchemy import (
    Boolean,
    CheckConstraint,
    Float,
    ForeignKey,
    Integer,
    Numeric,
    String,
    Text,
    text,
    true,
)
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base
from app.enums import Sector
from app.models.mixins import TimestampMixin, in_check


class Service(TimestampMixin, Base):
    __tablename__ = "services"
    __table_args__ = (
        CheckConstraint("price > 0", name="ck_services_price_positive"),
        CheckConstraint("delivery_days >= 1 AND delivery_days <= 365", name="ck_services_delivery_days"),
        CheckConstraint("rating >= 0 AND rating <= 5", name="ck_services_rating_range"),
        CheckConstraint(in_check("sector", Sector), name="ck_services_sector"),
    )

    id: Mapped[int] = mapped_column(primary_key=True)
    provider_id: Mapped[int] = mapped_column(ForeignKey("users.id"), index=True)
    name: Mapped[str] = mapped_column(String(120))
    description: Mapped[str] = mapped_column(Text, default="")
    price: Mapped[Decimal] = mapped_column(Numeric(12, 2), index=True)
    category: Mapped[str] = mapped_column(String(50), index=True)
    sector: Mapped[str] = mapped_column(String(20), index=True)
    availability: Mapped[bool] = mapped_column(Boolean, default=True, server_default=true())
    rating: Mapped[float] = mapped_column(Float, default=0.0, server_default=text("0"))
    delivery_days: Mapped[int] = mapped_column(Integer)

    provider: Mapped["User"] = relationship()