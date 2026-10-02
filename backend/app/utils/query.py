"""Helpers for building product/service list queries.

All values reach PostgreSQL as bound parameters (SQLAlchemy), so SQL injection
is not possible. LIKE wildcards typed by the user are escaped so that searching
for "100%" matches the literal text, not "everything".
"""
from dataclasses import dataclass
from decimal import Decimal
from enum import Enum
from typing import Any

from sqlalchemy import Select, func, or_

from app.enums import Sector
from app.utils.errors import BadRequestError


@dataclass
class ListingFilters:
    q: str | None = None
    category: str | None = None
    sector: Sector | None = None
    min_price: Decimal | None = None
    max_price: Decimal | None = None
    min_rating: float | None = None
    availability: bool | None = None
    owner_id: int | None = None


def escape_like(term: str) -> str:
    """Escape LIKE wildcards. Used together with escape='\\\\' on the ILIKE call."""
    return term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")


def apply_listing_filters(stmt: Select, model: Any, filters: ListingFilters) -> Select:
    """Filters common to Product and Service (name, description, category, sector, price, rating)."""
    if (
        filters.min_price is not None
        and filters.max_price is not None
        and filters.min_price > filters.max_price
    ):
        raise BadRequestError("min_price cannot be greater than max_price")

    term = (filters.q or "").strip()
    if term:
        pattern = f"%{escape_like(term)}%"
        stmt = stmt.where(
            or_(
                model.name.ilike(pattern, escape="\\"),
                model.description.ilike(pattern, escape="\\"),
            )
        )
    if filters.category and filters.category.strip():
        stmt = stmt.where(func.lower(model.category) == filters.category.strip().lower())
    if filters.sector is not None:
        stmt = stmt.where(model.sector == filters.sector.value)
    if filters.min_price is not None:
        stmt = stmt.where(model.price >= filters.min_price)
    if filters.max_price is not None:
        stmt = stmt.where(model.price <= filters.max_price)
    if filters.min_rating is not None:
        stmt = stmt.where(model.rating >= filters.min_rating)
    return stmt


def apply_updates(entity: Any, changes: dict[str, Any], non_nullable: set[str]) -> None:
    """Copy validated changes onto an ORM object; reject explicit nulls for required fields."""
    for field, value in changes.items():
        if value is None and field in non_nullable:
            raise BadRequestError(f"{field} cannot be null")
        setattr(entity, field, value.value if isinstance(value, Enum) else value)