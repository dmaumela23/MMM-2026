"""orders and order_items tables."""
from decimal import Decimal
from typing import Optional

from sqlalchemy import CheckConstraint, ForeignKey, Index, Integer, Numeric, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base
from app.enums import ItemStatus, OrderStatus
from app.models.mixins import TimestampMixin, in_check


class Order(TimestampMixin, Base):
    __tablename__ = "orders"
    __table_args__ = (
        CheckConstraint(in_check("status", OrderStatus), name="ck_orders_status"),
        CheckConstraint("total_amount >= 0", name="ck_orders_total_non_negative"),
        Index("ix_orders_customer_status", "customer_id", "status"),
    )

    id: Mapped[int] = mapped_column(primary_key=True)
    customer_id: Mapped[int] = mapped_column(ForeignKey("users.id"))
    status: Mapped[str] = mapped_column(String(20), default=OrderStatus.PENDING.value)
    total_amount: Mapped[Decimal] = mapped_column(Numeric(12, 2), default=Decimal("0.00"))
    notes: Mapped[str | None] = mapped_column(Text)

    customer: Mapped["User"] = relationship()
    items: Mapped[list["OrderItem"]] = relationship(
        back_populates="order", cascade="all, delete-orphan", order_by="OrderItem.id"
    )


class OrderItem(TimestampMixin, Base):
    __tablename__ = "order_items"
    __table_args__ = (
        CheckConstraint(
            "(product_id IS NOT NULL AND service_id IS NULL) "
            "OR (product_id IS NULL AND service_id IS NOT NULL)",
            name="ck_order_items_exactly_one_target",
        ),
        CheckConstraint("quantity >= 1", name="ck_order_items_quantity_positive"),
        CheckConstraint("unit_price >= 0", name="ck_order_items_price_non_negative"),
        CheckConstraint(in_check("item_status", ItemStatus), name="ck_order_items_status"),
    )

    id: Mapped[int] = mapped_column(primary_key=True)
    order_id: Mapped[int] = mapped_column(ForeignKey("orders.id", ondelete="CASCADE"), index=True)
    # RESTRICT: a listing that appears on an order cannot be deleted (API returns 409).
    product_id: Mapped[int | None] = mapped_column(
        ForeignKey("products.id", ondelete="RESTRICT"), index=True
    )
    service_id: Mapped[int | None] = mapped_column(
        ForeignKey("services.id", ondelete="RESTRICT"), index=True
    )
    quantity: Mapped[int] = mapped_column(Integer, default=1)
    unit_price: Mapped[Decimal] = mapped_column(Numeric(12, 2))  # price frozen at order time
    item_status: Mapped[str] = mapped_column(String(20), default=ItemStatus.PENDING.value)

    order: Mapped["Order"] = relationship(back_populates="items")
    product: Mapped[Optional["Product"]] = relationship()
    service: Mapped[Optional["Service"]] = relationship()