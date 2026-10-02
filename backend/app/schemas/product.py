"""Product schemas."""
from datetime import datetime

from pydantic import BaseModel, Field, field_validator

from app.enums import Sector
from app.schemas.common import Category, ListingName, MoneyIn, MoneyOut
from app.utils.validation import normalize_image_url


class ProductCreate(BaseModel):
    name: ListingName
    description: str = Field(default="", max_length=2000)
    price: MoneyIn
    category: Category
    sector: Sector
    image_url: str | None = Field(default=None, max_length=500)
    stock_quantity: int = Field(default=0, ge=0, le=1_000_000)
    availability: bool = True

    @field_validator("image_url")
    @classmethod
    def _check_image_url(cls, value: str | None) -> str | None:
        return normalize_image_url(value)


class ProductUpdate(BaseModel):
    """Partial update: only the fields that are sent are changed."""

    name: ListingName | None = None
    description: str | None = Field(default=None, max_length=2000)
    price: MoneyIn | None = None
    category: Category | None = None
    sector: Sector | None = None
    image_url: str | None = Field(default=None, max_length=500)
    stock_quantity: int | None = Field(default=None, ge=0, le=1_000_000)
    availability: bool | None = None

    @field_validator("image_url")
    @classmethod
    def _check_image_url(cls, value: str | None) -> str | None:
        return normalize_image_url(value)


class ProductOut(BaseModel):
    id: int
    seller_id: int
    seller_name: str
    name: str
    description: str
    price: MoneyOut
    category: str
    sector: Sector
    image_url: str | None
    stock_quantity: int
    availability: bool
    rating: float
    created_at: datetime
    updated_at: datetime