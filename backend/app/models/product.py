"""products table."""
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


class Product(TimestampMixin, Base):
    __tablename__ = "products"
    __table_args__ = (
        CheckConstraint("price > 0", name="ck_products_price_positive"),
        CheckConstraint("stock_quantity >= 0", name="ck_products_stock_non_negative"),
        CheckConstraint("rating >= 0 AND rating <= 5", name="ck_products_rating_range"),
        CheckConstraint(in_check("sector", Sector), name="ck_products_sector"),
    )

    id: Mapped[int] = mapped_column(primary_key=True)
    seller_id: Mapped[int] = mapped_column(ForeignKey("users.id"), index=True)
    name: Mapped[str] = mapped_column(String(120))
    description: Mapped[str] = mapped_column(Text, default="")
    price: Mapped[Decimal] = mapped_column(Numeric(12, 2), index=True)
    category: Mapped[str] = mapped_column(String(50), index=True)
    sector: Mapped[str] = mapped_column(String(20), index=True)
    image_url: Mapped[str | None] = mapped_column(String(500))
    stock_quantity: Mapped[int] = mapped_column(Integer, default=0)
    availability: Mapped[bool] = mapped_column(Boolean, default=True, server_default=true())
    # Rating is a seeded/stored value in the prototype (no reviews system yet).
    rating: Mapped[float] = mapped_column(Float, default=0.0, server_default=text("0"))

    seller: Mapped["User"] = relationship()