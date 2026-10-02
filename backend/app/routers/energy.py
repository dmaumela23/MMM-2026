"""Energy endpoints. ALL data is simulated; responses say so explicitly."""
from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.auth.dependencies import get_current_user
from app.database import get_db
from app.models import User
from app.schemas.common import error_responses
from app.schemas.energy import EnergyPlansResponse, EnergyReportsResponse, EnergyUsageResponse
from app.services import energy_service

router = APIRouter(prefix="/api/energy", tags=["Energy (simulated)"])


@router.get(
    "/usage",
    response_model=EnergyUsageResponse,
    summary="Simulated usage history and summary (days = 7, 30 or 90)",
    responses=error_responses(400, 401),
)
def get_usage(
    days: int = Query(30, description="History length: 7, 30 or 90"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return energy_service.get_usage(db, current_user, days)


@router.get(
    "/plans",
    response_model=EnergyPlansResponse,
    summary="Energy plans (read-only in the prototype)",
    responses=error_responses(401),
)
def get_plans(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    return energy_service.get_plans(db, current_user)


@router.get(
    "/reports",
    response_model=EnergyReportsResponse,
    summary="Simulated monthly report",
    responses=error_responses(401),
)
def get_reports(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    return energy_service.get_reports(db, current_user)