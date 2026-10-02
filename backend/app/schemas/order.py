"""Order schemas. The client sends ids and quantities ONLY; prices and totals are server-side."""
from datetime import datetime

from pydantic import BaseModel, Field, field_validator, model_validator

from app.enums import ItemStatus, ItemType, OrderStatus
from app.schemas.common import MoneyOut


class OrderItemCreate(BaseModel):
    product_id: int | None = Field(default=None, ge=1)
    service_id: int | None = Field(default=None, ge=1)
    quantity: int = Field(default=1, ge=1, le=99)

    @model_validator(mode="after")
    def _exactly_one_target(self) -> "OrderItemCreate":
        if (self.product_id is None) == (self.service_id is None):
            raise ValueError("Each item needs exactly one of product_id or service_id")
        if self.service_id is not None and self.quantity != 1:
            raise ValueError("Service quantity is always 1")
        return self


class OrderCreate(BaseModel):
    items: list[OrderItemCreate] = Field(min_length=1, max_length=50)
    notes: str | None = Field(default=None, max_length=1000)

    @field_validator("notes")
    @classmethod
    def _blank_notes_is_none(cls, value: str | None) -> str | None:
        if value is None:
            return None
        cleaned = value.strip()
        return cleaned or None


class OrderCancelRequest(BaseModel):
    status: OrderStatus


class ItemStatusUpdate(BaseModel):
    item_status: ItemStatus


class OrderItemOut(BaseModel):
    id: int
    item_type: ItemType
    product_id: int | None
    service_id: int | None
    name: str
    seller_name: str
    quantity: int
    unit_price: MoneyOut
    line_total: MoneyOut
    item_status: ItemStatus


class OrderOut(BaseModel):
    id: int
    customer_id: int
    status: OrderStatus
    total_amount: MoneyOut
    notes: str | None
    created_at: datetime
    updated_at: datetime
    items: list[OrderItemOut]


class OrderSummary(BaseModel):
    id: int
    status: OrderStatus
    total_amount: MoneyOut
    created_at: datetime
    item_count: int
    first_item_name: str | None


class IncomingItem(BaseModel):
    order_id: int
    item_id: int
    item_type: ItemType
    name: str
    quantity: int
    unit_price: MoneyOut
    line_total: MoneyOut
    item_status: ItemStatus
    customer_name: str
    notes: str | None
    order_created_at: datetime