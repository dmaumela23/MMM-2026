"""Energy schemas. Everything here is SIMULATED data (see is_simulated / disclaimer)."""
import datetime as dt

from pydantic import BaseModel

from app.enums import EnergyStatus
from app.schemas.common import MoneyOut


class EnergyPlanOut(BaseModel):
    id: int
    name: str
    description: str
    price_per_kwh: MoneyOut
    monthly_fee: MoneyOut
    max_kwh: int


class UsagePoint(BaseModel):
    date: dt.date
    kwh: float
    estimated_cost: MoneyOut  # energy charge for that day: kwh x plan price per kWh
    is_simulated: bool


class EnergyUsageResponse(BaseModel):
    days: int
    current_kwh: float
    estimated_monthly_kwh: float
    estimated_cost: MoneyOut
    status: EnergyStatus
    plan: EnergyPlanOut
    history: list[UsagePoint]
    is_simulated: bool
    disclaimer: str


class EnergyPlansResponse(BaseModel):
    current_plan_id: int | None
    plans: list[EnergyPlanOut]
    is_simulated: bool
    disclaimer: str


class MonthlyReport(BaseModel):
    month: str  # "YYYY-MM"
    total_kwh: float
    average_daily_kwh: float
    days_recorded: int
    estimated_cost: MoneyOut


class EnergyReportsResponse(BaseModel):
    months: list[MonthlyReport]
    is_simulated: bool
    disclaimer: str