"""Product business logic: list/search/filter, CRUD, ownership."""
from sqlalchemy import func, or_, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session, joinedload

from app.models import OrderItem, Product, User
from app.schemas.product import ProductCreate, ProductOut, ProductUpdate
from app.utils.errors import ConflictError, ForbiddenError, NotFoundError
from app.utils.query import ListingFilters, apply_listing_filters, apply_updates

NON_NULLABLE = {"name", "description", "price", "category", "sector", "stock_quantity", "availability"}
HAS_ORDERS = "This product has order history and cannot be deleted. Set availability to false instead."


def _to_out(product: Product) -> ProductOut:
    return ProductOut(
        id=product.id,
        seller_id=product.seller_id,
        seller_name=product.seller.full_name,
        name=product.name,
        description=product.description,
        price=product.price,
        category=product.category,
        sector=product.sector,
        image_url=product.image_url,
        stock_quantity=product.stock_quantity,
        availability=product.availability,
        rating=product.rating,
        created_at=product.created_at,
        updated_at=product.updated_at,
    )


def _get_or_404(db: Session, product_id: int) -> Product:
    product = db.scalar(
        select(Product).options(joinedload(Product.seller)).where(Product.id == product_id)
    )
    if product is None:
        raise NotFoundError("Product not found")
    return product


def _ensure_owner(product: Product, user: User) -> None:
    if product.seller_id != user.id:
        raise ForbiddenError("You can only modify your own products")


def list_products(db: Session, filters: ListingFilters) -> list[ProductOut]:
    stmt = select(Product).options(joinedload(Product.seller))
    stmt = apply_listing_filters(stmt, Product, filters)
    if filters.owner_id is not None:
        stmt = stmt.where(Product.seller_id == filters.owner_id)
    if filters.availability is True:
        stmt = stmt.where(Product.availability.is_(True), Product.stock_quantity > 0)
    elif filters.availability is False:
        stmt = stmt.where(or_(Product.availability.is_(False), Product.stock_quantity == 0))
    stmt = stmt.order_by(Product.created_at.desc(), Product.id.desc())
    return [_to_out(p) for p in db.scalars(stmt)]


def get_product(db: Session, product_id: int) -> ProductOut:
    return _to_out(_get_or_404(db, product_id))


def create_product(db: Session, user: User, data: ProductCreate) -> ProductOut:
    values = data.model_dump()
    values["sector"] = data.sector.value
    product = Product(seller_id=user.id, **values)  # seller is ALWAYS the logged-in user
    db.add(product)
    db.commit()
    db.refresh(product)
    return _to_out(product)


def update_product(db: Session, user: User, product_id: int, data: ProductUpdate) -> ProductOut:
    product = _get_or_404(db, product_id)
    _ensure_owner(product, user)
    apply_updates(product, data.model_dump(exclude_unset=True), NON_NULLABLE)
    db.commit()
    db.refresh(product)
    return _to_out(product)


def delete_product(db: Session, user: User, product_id: int) -> None:
    product = _get_or_404(db, product_id)
    _ensure_owner(product, user)
    order_lines = db.scalar(
        select(func.count()).select_from(OrderItem).where(OrderItem.product_id == product_id)
    )
    if order_lines:
        raise ConflictError(HAS_ORDERS)
    try:
        db.delete(product)
        db.commit()
    except IntegrityError as exc:  # an order line appeared between the check and the delete
        db.rollback()
        raise ConflictError(HAS_ORDERS) from exc