"""Enumerations shared by models, schemas and services.

The database stores the plain string values (with CHECK constraints), which
avoids the pain of altering PostgreSQL ENUM types later.
"""
from enum import StrEnum


class UserRole(StrEnum):
    CUSTOMER = "CUSTOMER"
    PROVIDER = "PROVIDER"
    BUSINESS = "BUSINESS"


class Sector(StrEnum):
    DIGITAL = "DIGITAL"
    SERVICE = "SERVICE"
    ENERGY = "ENERGY"


class OrderStatus(StrEnum):
    PENDING = "PENDING"
    PROCESSING = "PROCESSING"
    COMPLETED = "COMPLETED"
    CANCELLED = "CANCELLED"


class ItemStatus(StrEnum):
    PENDING = "PENDING"
    ACCEPTED = "ACCEPTED"
    IN_PROGRESS = "IN_PROGRESS"
    COMPLETED = "COMPLETED"
    CANCELLED = "CANCELLED"


class ItemType(StrEnum):
    PRODUCT = "PRODUCT"
    SERVICE = "SERVICE"


class Theme(StrEnum):
    LIGHT = "LIGHT"
    DARK = "DARK"
    SYSTEM = "SYSTEM"


class EnergyStatus(StrEnum):
    NORMAL = "NORMAL"
    HIGH = "HIGH"
    OVER_LIMIT = "OVER_LIMIT"