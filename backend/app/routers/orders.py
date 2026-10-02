"""Order endpoints. NOTE: /incoming must be declared BEFORE /{order_id}."""
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.auth.dependencies import get_current_user, require_seller
from app.database import get_db
from app.models import User
from app.schemas.common import error_responses
from app.schemas.order import (
    IncomingItem,
    ItemStatusUpdate,
    OrderCancelRequest,
    OrderCreate,
    OrderItemOut,
    OrderOut,
    OrderSummary,
)
from app.services import order_service

router = APIRouter(prefix="/api/orders", tags=["Orders"])


@router.post(
    "",
    response_model=OrderOut,
    status_code=201,
    summary="Create an order (prices and total are calculated by the server)",
    responses=error_responses(400, 401, 404),
)
def create_order(
    payload: OrderCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return order_service.create_order(db, current_user, payload)


@router.get(
    "",
    response_model=list[OrderSummary],
    summary="List my orders",
    responses=error_responses(401),
)
def list_orders(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    return order_service.list_orders(db, current_user)


@router.get(
    "/incoming",
    response_model=list[IncomingItem],
    summary="Incoming product and service lines for me (Provider or Business)",
    responses=error_responses(401, 403),
)
def incoming_items(user: User = Depends(require_seller), db: Session = Depends(get_db)):
    return order_service.list_incoming_items(db, user)


@router.get(
    "/{order_id}",
    response_model=OrderOut,
    summary="Get one order",
    responses=error_responses(400, 401, 403, 404),
)
def get_order(
    order_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return order_service.get_order(db, current_user, order_id)


@router.put(
    "/{order_id}",
    response_model=OrderOut,
    summary="Cancel my order (send status CANCELLED)",
    responses=error_responses(400, 401, 403, 404),
)
def cancel_order(
    order_id: int,
    payload: OrderCancelRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return order_service.cancel_order(db, current_user, order_id, payload.status)


@router.put(
    "/{order_id}/items/{item_id}/status",
    response_model=OrderItemOut,
    summary="Update the status of an order line (seller/provider of that line)",
    responses=error_responses(400, 401, 403, 404),
)
def update_item_status(
    order_id: int,
    item_id: int,
    payload: ItemStatusUpdate,
    user: User = Depends(require_seller),
    db: Session = Depends(get_db),
):
    return order_service.update_item_status(db, user, order_id, item_id, payload.item_status)