"""Service-listing business logic: list/search/filter, CRUD, ownership."""
from sqlalchemy import func, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session, joinedload

from app.models import OrderItem, Service, User
from app.schemas.service import ServiceCreate, ServiceOut, ServiceUpdate
from app.utils.errors import ConflictError, ForbiddenError, NotFoundError
from app.utils.query import ListingFilters, apply_listing_filters, apply_updates

NON_NULLABLE = {"name", "description", "price", "category", "sector", "delivery_days", "availability"}
HAS_ORDERS = "This service has order history and cannot be deleted. Set availability to false instead."


def _to_out(service: Service) -> ServiceOut:
    return ServiceOut(
        id=service.id,
        provider_id=service.provider_id,
        provider_name=service.provider.full_name,
        name=service.name,
        description=service.description,
        price=service.price,
        category=service.category,
        sector=service.sector,
        availability=service.availability,
        rating=service.rating,
        delivery_days=service.delivery_days,
        created_at=service.created_at,
        updated_at=service.updated_at,
    )


def _get_or_404(db: Session, service_id: int) -> Service:
    service = db.scalar(
        select(Service).options(joinedload(Service.provider)).where(Service.id == service_id)
    )
    if service is None:
        raise NotFoundError("Service not found")
    return service


def _ensure_owner(service: Service, user: User) -> None:
    if service.provider_id != user.id:
        raise ForbiddenError("You can only modify your own services")


def list_services(db: Session, filters: ListingFilters) -> list[ServiceOut]:
    stmt = select(Service).options(joinedload(Service.provider))
    stmt = apply_listing_filters(stmt, Service, filters)
    if filters.owner_id is not None:
        stmt = stmt.where(Service.provider_id == filters.owner_id)
    if filters.availability is not None:
        stmt = stmt.where(Service.availability.is_(filters.availability))
    stmt = stmt.order_by(Service.created_at.desc(), Service.id.desc())
    return [_to_out(s) for s in db.scalars(stmt)]


def get_service(db: Session, service_id: int) -> ServiceOut:
    return _to_out(_get_or_404(db, service_id))


def create_service(db: Session, user: User, data: ServiceCreate) -> ServiceOut:
    values = data.model_dump()
    values["sector"] = data.sector.value
    service = Service(provider_id=user.id, **values)  # provider is ALWAYS the logged-in user
    db.add(service)
    db.commit()
    db.refresh(service)
    return _to_out(service)


def update_service(db: Session, user: User, service_id: int, data: ServiceUpdate) -> ServiceOut:
    service = _get_or_404(db, service_id)
    _ensure_owner(service, user)
    apply_updates(service, data.model_dump(exclude_unset=True), NON_NULLABLE)
    db.commit()
    db.refresh(service)
    return _to_out(service)


def delete_service(db: Session, user: User, service_id: int) -> None:
    service = _get_or_404(db, service_id)
    _ensure_owner(service, user)
    order_lines = db.scalar(
        select(func.count()).select_from(OrderItem).where(OrderItem.service_id == service_id)
    )
    if order_lines:
        raise ConflictError(HAS_ORDERS)
    try:
        db.delete(service)
        db.commit()
    except IntegrityError as exc:
        db.rollback()
        raise ConflictError(HAS_ORDERS) from exc