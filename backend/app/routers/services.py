"""Service-listing endpoints."""
from decimal import Decimal
from typing import Literal

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.auth.dependencies import get_current_user, require_seller
from app.database import get_db
from app.enums import Sector
from app.models import User
from app.schemas.common import MessageResponse, error_responses
from app.schemas.service import ServiceCreate, ServiceOut, ServiceUpdate
from app.services import service_service
from app.utils.query import ListingFilters

router = APIRouter(prefix="/api/services", tags=["Services"])


@router.get(
    "",
    response_model=list[ServiceOut],
    summary="List, search and filter services",
    responses=error_responses(400, 401),
)
def list_services(
    q: str | None = Query(None, max_length=100, description="Search in name and description"),
    category: str | None = Query(None, max_length=50),
    sector: Sector | None = Query(None),
    min_price: Decimal | None = Query(None, ge=0),
    max_price: Decimal | None = Query(None, ge=0),
    min_rating: float | None = Query(None, ge=0, le=5),
    availability: bool | None = Query(None),
    owner: Literal["me"] | None = Query(None, description="Use owner=me for my own services"),
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
    return service_service.list_services(db, filters)


@router.get(
    "/{service_id}",
    response_model=ServiceOut,
    summary="Get one service",
    responses=error_responses(400, 401, 404),
)
def get_service(
    service_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return service_service.get_service(db, service_id)


@router.post(
    "",
    response_model=ServiceOut,
    status_code=201,
    summary="Create a service (Provider or Business accounts)",
    responses=error_responses(400, 401, 403),
)
def create_service(
    payload: ServiceCreate,
    user: User = Depends(require_seller),
    db: Session = Depends(get_db),
):
    return service_service.create_service(db, user, payload)


@router.put(
    "/{service_id}",
    response_model=ServiceOut,
    summary="Update your own service",
    responses=error_responses(400, 401, 403, 404),
)
def update_service(
    service_id: int,
    payload: ServiceUpdate,
    user: User = Depends(require_seller),
    db: Session = Depends(get_db),
):
    return service_service.update_service(db, user, service_id, payload)


@router.delete(
    "/{service_id}",
    response_model=MessageResponse,
    summary="Delete your own service",
    responses=error_responses(400, 401, 403, 404, 409),
)
def delete_service(
    service_id: int,
    user: User = Depends(require_seller),
    db: Session = Depends(get_db),
):
    service_service.delete_service(db, user, service_id)
    return MessageResponse(detail="Deleted")