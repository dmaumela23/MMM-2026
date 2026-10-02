"""Money helpers. Money is ALWAYS Decimal, never float."""
from decimal import ROUND_HALF_UP, Decimal

TWO_PLACES = Decimal("0.01")


def to_money(value: Decimal | int | str) -> Decimal:
    """Round to 2 decimal places (half up), e.g. Decimal('10.005') -> Decimal('10.01')."""
    return Decimal(str(value)).quantize(TWO_PLACES, rounding=ROUND_HALF_UP)