"""Shared schema pieces: money types, error models, reusable constrained strings."""
from decimal import Decimal
from typing import Annotated

from pydantic import BaseModel, Field, PlainSerializer, StringConstraints


def _format_money(value: Decimal) -> str:
    return f"{Decimal(value):.2f}"


# Output: always a decimal STRING with two places, e.g. "1499.00" (never a float).
MoneyOut = Annotated[Decimal, PlainSerializer(_format_money, return_type=str, when_used="json")]
# Input: positive, max 12 digits, at most 2 decimal places. Accepts "1499.00" or 1499.5.
MoneyIn = Annotated[Decimal, Field(gt=0, max_digits=12, decimal_places=2)]

ListingName = Annotated[str, StringConstraints(strip_whitespace=True, min_length=2, max_length=120)]
Category = Annotated[str, StringConstraints(strip_whitespace=True, min_length=2, max_length=50)]


class MessageResponse(BaseModel):
    detail: str


class ErrorResponse(BaseModel):
    detail: str


class HealthResponse(BaseModel):
    status: str
    service: str
    database: str


_ERROR_DESCRIPTIONS = {
    400: "Invalid request or validation error",
    401: "Missing, invalid or expired token",
    403: "Not allowed for this account",
    404: "Not found",
    409: "Conflict with existing data",
}


def error_responses(*codes: int) -> dict[int | str, dict]:
    """OpenAPI documentation for error responses, all shaped {"detail": "..."}."""
    return {code: {"model": ErrorResponse, "description": _ERROR_DESCRIPTIONS[code]} for code in codes}