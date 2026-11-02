"""Product endpoints."""
from decimal import Decimal
from typing import Literal

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.auth.dependencies import get_current_user, require_business
from app.database import get_db
from app.enums import Sector
from app.models import User
from app.schemas.common import MessageResponse, error_responses
from app.schemas.product import ProductCreate, ProductOut, ProductUpdate
from app.services import product_service
from app.utils.query import ListingFilters

router = APIRouter(prefix="/api/products", tags=["Products"])


@router.get(
    "",
    response_model=list[ProductOut],
    summary="List, search and filter products",
    responses=error_responses(400, 401),
)
def list_products(
    q: str | None = Query(None, max_length=100, description="Search in name and description"),
    category: str | None = Query(None, max_length=50),
    sector: Sector | None = Query(None),
    min_price: Decimal | None = Query(None, ge=0),
    max_price: Decimal | None = Query(None, ge=0),
    min_rating: float | None = Query(None, ge=0, le=5),
    availability: bool | None = Query(None, description="true = available and in stock"),
    owner: Literal["me"] | None = Query(None, description="Use owner=me for my own products"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    filters = ListingFilters(
        q=q,
        category=category,
        sector=sector,
        min_price=min_price,
        max_price=max_price,
        min_rating=min_rating,
        availability=availability,
        owner_id=current_user.id if owner == "me" else None,
    )
    return product_service.list_products(db, filters)


@router.get(
    "/{product_id}",
    response_model=ProductOut,
    summary="Get one product",
    responses=error_responses(400, 401, 404),
)
def get_product(
    product_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return product_service.get_product(db, product_id)


@router.post(
    "",
    response_model=ProductOut,
    status_code=201,
    summary="Create a product (Business accounts only)",
    responses=error_responses(400, 401, 403),
)
def create_product(
    payload: ProductCreate,
    user: User = Depends(require_business),
    db: Session = Depends(get_db),
):
    return product_service.create_product(db, user, payload)


@router.put(
    "/{product_id}",
    response_model=ProductOut,
    summary="Update your own product",
    responses=error_responses(400, 401, 403, 404),
)
def update_product(
    product_id: int,
    payload: ProductUpdate,
    user: User = Depends(require_business),
    db: Session = Depends(get_db),
):
    return product_service.update_product(db, user, product_id, payload)


@router.delete(
    "/{product_id}",
    response_model=MessageResponse,
    summary="Delete your own product",
    responses=error_responses(400, 401, 403, 404, 409),
)
def delete_product(
    product_id: int,
    user: User = Depends(require_business),
    db: Session = Depends(get_db),
):
    product_service.delete_product(db, user, product_id)
    return MessageResponse(detail="Deleted")