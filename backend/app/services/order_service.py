"""Order business logic.

Guarantees:
* The client sends ids + quantities only. Prices are loaded from PostgreSQL and totals are
  calculated here, so a tampered client cannot change what it pays.
* Order + lines + stock changes happen in ONE transaction (all or nothing).
* Stock is reserved with a conditional UPDATE (stock >= quantity), so two simultaneous orders
  can never oversell.
* The order row is locked (SELECT ... FOR UPDATE) while its status changes, so a seller update
  and a customer cancellation cannot interleave.
"""
from collections.abc import Iterable
from decimal import Decimal

from sqlalchemy import or_, select, update
from sqlalchemy.orm import Session, joinedload, selectinload

from app.enums import ItemStatus, ItemType, OrderStatus
from app.models import Order, OrderItem, Product, Service, User
from app.schemas.order import (
    IncomingItem,
    OrderCreate,
    OrderItemOut,
    OrderOut,
    OrderSummary,
)
from app.utils.errors import BadRequestError, ForbiddenError, NotFoundError
from app.utils.money import to_money

# Line workflow. COMPLETED and CANCELLED are terminal.
ALLOWED_TRANSITIONS: dict[ItemStatus, frozenset[ItemStatus]] = {
    ItemStatus.PENDING: frozenset({ItemStatus.ACCEPTED, ItemStatus.CANCELLED}),
    ItemStatus.ACCEPTED: frozenset({ItemStatus.IN_PROGRESS, ItemStatus.CANCELLED}),
    ItemStatus.IN_PROGRESS: frozenset({ItemStatus.COMPLETED}),
    ItemStatus.COMPLETED: frozenset(),
    ItemStatus.CANCELLED: frozenset(),
}

_ORDER_LOAD = (
    selectinload(Order.items).options(
        joinedload(OrderItem.product).joinedload(Product.seller),
        joinedload(OrderItem.service).joinedload(Service.provider),
    ),
)


# ------------------------------------------------------- pure business rules
def can_transition(current: str, target: str) -> bool:
    return ItemStatus(target) in ALLOWED_TRANSITIONS[ItemStatus(current)]


def derive_order_status(item_statuses: Iterable[str]) -> OrderStatus:
    """Order status from its lines. Cancelled lines are ignored unless ALL are cancelled."""
    statuses = [ItemStatus(s) for s in item_statuses]
    if not statuses:
        return OrderStatus.PENDING
    active = [s for s in statuses if s != ItemStatus.CANCELLED]
    if not active:
        return OrderStatus.CANCELLED
    if all(s == ItemStatus.COMPLETED for s in active):
        return OrderStatus.COMPLETED
    if any(s != ItemStatus.PENDING for s in active):
        return OrderStatus.PROCESSING
    return OrderStatus.PENDING


def calculate_line_total(unit_price: Decimal, quantity: int) -> Decimal:
    return to_money(Decimal(unit_price) * quantity)


def calculate_order_total(lines: Iterable[tuple[Decimal, int, str]]) -> Decimal:
    """Sum of unit_price x quantity over NON-cancelled lines. Each line is (price, qty, status)."""
    total = Decimal("0.00")
    for unit_price, quantity, status in lines:
        if ItemStatus(status) != ItemStatus.CANCELLED:
            total += calculate_line_total(unit_price, quantity)
    return to_money(total)


# ------------------------------------------------------------------ mapping
def _item_seller_id(item: OrderItem) -> int:
    return item.product.seller_id if item.product_id is not None else item.service.provider_id


def _item_name(item: OrderItem) -> str:
    return item.product.name if item.product_id is not None else item.service.name


def _item_to_out(item: OrderItem) -> OrderItemOut:
    if item.product_id is not None:
        item_type, name, seller_name = ItemType.PRODUCT, item.product.name, item.product.seller.full_name
    else:
        item_type, name, seller_name = ItemType.SERVICE, item.service.name, item.service.provider.full_name
    return OrderItemOut(
        id=item.id,
        item_type=item_type,
        product_id=item.product_id,
        service_id=item.service_id,
        name=name,
        seller_name=seller_name,
        quantity=item.quantity,
        unit_price=item.unit_price,
        line_total=calculate_line_total(item.unit_price, item.quantity),
        item_status=item.item_status,
    )


def _order_to_out(order: Order) -> OrderOut:
    return OrderOut(
        id=order.id,
        customer_id=order.customer_id,
        status=order.status,
        total_amount=order.total_amount,
        notes=order.notes,
        created_at=order.created_at,
        updated_at=order.updated_at,
        items=[_item_to_out(i) for i in order.items],
    )


def _order_to_summary(order: Order) -> OrderSummary:
    return OrderSummary(
        id=order.id,
        status=order.status,
        total_amount=order.total_amount,
        created_at=order.created_at,
        item_count=len(order.items),
        first_item_name=_item_name(order.items[0]) if order.items else None,
    )


# ------------------------------------------------------------------ helpers
def _load_order(db: Session, order_id: int, *, for_update: bool = False) -> Order | None:
    stmt = select(Order).where(Order.id == order_id).options(*_ORDER_LOAD)
    if for_update:
        stmt = stmt.with_for_update(of=Order)
    return db.scalar(stmt)


def _refresh_order_aggregate(order: Order) -> None:
    order.status = derive_order_status(i.item_status for i in order.items).value
    order.total_amount = calculate_order_total(
        (i.unit_price, i.quantity, i.item_status) for i in order.items
    )


def _reserve_stock(db: Session, product_id: int, quantity: int, name: str) -> None:
    """Atomically take stock; fails (rowcount 0) if it is no longer available."""
    result = db.execute(
        update(Product)
        .where(
            Product.id == product_id,
            Product.availability.is_(True),
            Product.stock_quantity >= quantity,
        )
        .values(stock_quantity=Product.stock_quantity - quantity)
        .execution_options(synchronize_session=False)
    )
    if result.rowcount != 1:
        raise BadRequestError(f"Insufficient stock for '{name}'.")


def _release_line_stock(db: Session, item: OrderItem) -> None:
    if item.product_id is not None:
        db.execute(
            update(Product)
            .where(Product.id == item.product_id)
            .values(stock_quantity=Product.stock_quantity + item.quantity)
            .execution_options(synchronize_session=False)
        )


# ----------------------------------------------------------------- use cases
def create_order(db: Session, user: User, data: OrderCreate) -> OrderOut:
    product_ids = [i.product_id for i in data.items if i.product_id is not None]
    service_ids = [i.service_id for i in data.items if i.service_id is not None]
    if len(set(product_ids)) != len(product_ids) or len(set(service_ids)) != len(service_ids):
        raise BadRequestError("Each product or service can only appear once per order")

    # Phase 1: validate everything against CURRENT database values (no writes yet).
    products = (
        {p.id: p for p in db.scalars(select(Product).where(Product.id.in_(product_ids)))}
        if product_ids
        else {}
    )
    services = (
        {s.id: s for s in db.scalars(select(Service).where(Service.id.in_(service_ids)))}
        if service_ids
        else {}
    )

    planned: list[tuple[Product | Service, int]] = []
    for line in data.items:
        if line.product_id is not None:
            product = products.get(line.product_id)
            if product is None:
                raise NotFoundError(f"Product {line.product_id} was not found")
            if not product.availability:
                raise BadRequestError(f"'{product.name}' is currently unavailable")
            if product.stock_quantity < line.quantity:
                raise BadRequestError(
                    f"Insufficient stock for '{product.name}'. Only {product.stock_quantity} left."
                )
            planned.append((product, line.quantity))
        else:
            service = services.get(line.service_id)
            if service is None:
                raise NotFoundError(f"Service {line.service_id} was not found")
            if not service.availability:
                raise BadRequestError(f"'{service.name}' is currently unavailable")
            planned.append((service, 1))  # service quantity is always 1

    # Phase 2: write order, lines and stock changes in a single transaction.
    try:
        order = Order(
            customer_id=user.id,
            status=OrderStatus.PENDING.value,
            total_amount=Decimal("0.00"),
            notes=data.notes,
        )
        db.add(order)
        db.flush()
        for entity, quantity in planned:
            if isinstance(entity, Product):
                _reserve_stock(db, entity.id, quantity, entity.name)
                order.items.append(
                    OrderItem(
                        product_id=entity.id,
                        quantity=quantity,
                        unit_price=entity.price,  # frozen price, loaded from the database
                        item_status=ItemStatus.PENDING.value,
                    )
                )
            else:
                order.items.append(
                    OrderItem(
                        service_id=entity.id,
                        quantity=1,
                        unit_price=entity.price,
                        item_status=ItemStatus.PENDING.value,
                    )
                )
        _refresh_order_aggregate(order)  # total is calculated HERE, never taken from the client
        order_id = order.id
        db.commit()
    except Exception:
        db.rollback()
        raise

    return _order_to_out(_load_order(db, order_id))


def list_orders(db: Session, user: User) -> list[OrderSummary]:
    stmt = (
        select(Order)
        .where(Order.customer_id == user.id)
        .options(*_ORDER_LOAD)
        .order_by(Order.created_at.desc(), Order.id.desc())
    )
    return [_order_to_summary(o) for o in db.scalars(stmt)]


def get_order(db: Session, user: User, order_id: int) -> OrderOut:
    order = _load_order(db, order_id)
    if order is None:
        raise NotFoundError("Order not found")
    is_customer = order.customer_id == user.id
    is_involved_seller = any(_item_seller_id(i) == user.id for i in order.items)
    if not (is_customer or is_involved_seller):
        raise ForbiddenError("You do not have access to this order")
    return _order_to_out(order)


def list_incoming_items(db: Session, user: User) -> list[IncomingItem]:
    """Order lines (products AND services) that belong to the current seller/provider."""
    stmt = (
        select(OrderItem)
        .join(Order, OrderItem.order_id == Order.id)
        .outerjoin(Product, OrderItem.product_id == Product.id)
        .outerjoin(Service, OrderItem.service_id == Service.id)
        .where(or_(Product.seller_id == user.id, Service.provider_id == user.id))
        .options(
            joinedload(OrderItem.order).joinedload(Order.customer),
            joinedload(OrderItem.product),
            joinedload(OrderItem.service),
        )
        .order_by(Order.created_at.desc(), OrderItem.id.desc())
    )
    result = []
    for item in db.scalars(stmt):
        result.append(
            IncomingItem(
                order_id=item.order_id,
                item_id=item.id,
                item_type=ItemType.PRODUCT if item.product_id is not None else ItemType.SERVICE,
                name=_item_name(item),
                quantity=item.quantity,
                unit_price=item.unit_price,
                line_total=calculate_line_total(item.unit_price, item.quantity),
                item_status=item.item_status,
                customer_name=item.order.customer.full_name,
                notes=item.order.notes,
                order_created_at=item.order.created_at,
            )
        )
    return result


def cancel_order(db: Session, user: User, order_id: int, requested: OrderStatus) -> OrderOut:
    if requested != OrderStatus.CANCELLED:
        raise BadRequestError("Only cancelling is supported: set status to CANCELLED")
    order = _load_order(db, order_id, for_update=True)
    if order is None:
        raise NotFoundError("Order not found")
    if order.customer_id != user.id:
        raise ForbiddenError("You can only cancel your own orders")

    active = [i for i in order.items if i.item_status != ItemStatus.CANCELLED.value]
    if not active:
        raise BadRequestError("This order is already cancelled")
    if any(i.item_status != ItemStatus.PENDING.value for i in active):
        raise BadRequestError("This order can no longer be cancelled because work on it has started")

    try:
        for item in active:
            _release_line_stock(db, item)
            item.item_status = ItemStatus.CANCELLED.value
        _refresh_order_aggregate(order)
        db.commit()
    except Exception:
        db.rollback()
        raise
    return _order_to_out(_load_order(db, order_id))


def update_item_status(
    db: Session, user: User, order_id: int, item_id: int, new_status: ItemStatus
) -> OrderItemOut:
    order = _load_order(db, order_id, for_update=True)
    if order is None:
        raise NotFoundError("Order not found")
    item = next((i for i in order.items if i.id == item_id), None)
    if item is None:
        raise NotFoundError("Order item not found in this order")
    if _item_seller_id(item) != user.id:
        raise ForbiddenError("Only the seller of this item can change its status")
    if not can_transition(item.item_status, new_status.value):
        raise BadRequestError(
            f"Cannot change item status from {item.item_status} to {new_status.value}"
        )

    try:
        if new_status == ItemStatus.CANCELLED:
            _release_line_stock(db, item)  # cancelled product lines return their stock
        item.item_status = new_status.value
        _refresh_order_aggregate(order)  # order status/total follow the lines, so never stuck PENDING
        db.commit()
    except Exception:
        db.rollback()
        raise
    return _item_to_out(item)