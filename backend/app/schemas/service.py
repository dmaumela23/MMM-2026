"""Service schemas. There is deliberately no notes field: notes belong to the order."""
from datetime import datetime

from pydantic import BaseModel, Field

from app.enums import Sector
from app.schemas.common import Category, ListingName, MoneyIn, MoneyOut


class ServiceCreate(BaseModel):
    name: ListingName
    description: str = Field(default="", max_length=2000)
    price: MoneyIn
    category: Category
    sector: Sector
    delivery_days: int = Field(ge=1, le=365)
    availability: bool = True


class ServiceUpdate(BaseModel):
    """Partial update: only the fields that are sent are changed."""

    name: ListingName | None = None
    description: str | None = Field(default=None, max_length=2000)
    price: MoneyIn | None = None
    category: Category | None = None
    sector: Sector | None = None
    delivery_days: int | None = Field(default=None, ge=1, le=365)
    availability: bool | None = None


class ServiceOut(BaseModel):
    id: int
    provider_id: int
    provider_name: str
    name: str
    description: str
    price: MoneyOut
    category: str
    sector: Sector
    availability: bool
    rating: float
    delivery_days: int
    created_at: datetime
    updated_at: datetime