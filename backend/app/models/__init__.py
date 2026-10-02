"""Importing this package registers every model on Base.metadata."""
from app.models.energy import EnergyPlan, EnergyUsage
from app.models.order import Order, OrderItem
from app.models.product import Product
from app.models.service import Service
from app.models.user import User, UserSettings

__all__ = [
    "EnergyPlan",
    "EnergyUsage",
    "Order",
    "OrderItem",
    "Product",
    "Service",
    "User",
    "UserSettings",
]