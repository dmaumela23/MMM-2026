"""Energy usage, plans and reports. All data is SIMULATED and must say so."""
import re
from datetime import date, timedelta
from decimal import Decimal

import pytest
from sqlalchemy import delete

from app.database import SessionLocal
from app.enums import EnergyStatus
from app.models import EnergyUsage
from app.services import energy_service

MONEY = re.compile(r"^\d+\.\d{2}$")


@pytest.mark.parametrize("days", [7, 30, 90])
def test_usage_returns_the_requested_number_of_simulated_days(client, customer, days):
    response = client.get("/api/energy/usage", params={"days": days}, headers=customer.headers)
    assert response.status_code == 200
    body = response.json()
    assert body["days"] == days
    assert len(body["history"]) == days
    assert body["is_simulated"] is True
    assert "SIMULATION" in body["disclaimer"]
    for point in body["history"]:
        assert set(point) == {"date", "kwh", "estimated_cost", "is_simulated"}
        assert point["is_simulated"] is True
        assert point["kwh"] >= 1.0
        assert MONEY.match(point["estimated_cost"])


def test_default_range_is_30_days_and_history_is_in_date_order(client, customer):
    body = client.get("/api/energy/usage", headers=customer.headers).json()
    dates = [point["date"] for point in body["history"]]
    assert len(dates) == 30
    assert dates == sorted(dates)
    assert dates[-1] == energy_service.utc_today().isoformat()


@pytest.mark.parametrize("days", ["15", "0", "-7", "365", "abc"])
def test_invalid_days_value_returns_400(client, customer, days):
    response = client.get("/api/energy/usage", params={"days": days}, headers=customer.headers)
    assert response.status_code == 400
    assert isinstance(response.json()["detail"], str)


def test_summary_values_match_the_documented_formulas(client, customer):
    body = client.get("/api/energy/usage", params={"days": 30}, headers=customer.headers).json()
    kwh_values = [point["kwh"] for point in body["history"]]
    plan = body["plan"]
    assert plan["name"] == "Home Standard"

    expected_monthly = energy_service.estimate_monthly_kwh(kwh_values, energy_service.utc_today())
    assert body["estimated_monthly_kwh"] == pytest.approx(expected_monthly)
    assert body["current_kwh"] == kwh_values[-1]

    expected_cost = energy_service.estimate_cost(
        body["estimated_monthly_kwh"], Decimal(plan["price_per_kwh"]), Decimal(plan["monthly_fee"])
    )
    assert body["estimated_cost"] == f"{expected_cost:.2f}"
    assert body["status"] == energy_service.classify_status(body["estimated_monthly_kwh"], plan["max_kwh"]).value


def test_missing_recent_days_are_topped_up_with_simulated_readings(client, customer):
    today = energy_service.utc_today()
    with SessionLocal() as db:
        db.execute(
            delete(EnergyUsage).where(
                EnergyUsage.user_id == customer.id, EnergyUsage.recorded_on > today - timedelta(days=3)
            )
        )
        db.commit()
    body = client.get("/api/energy/usage", params={"days": 7}, headers=customer.headers).json()
    assert len(body["history"]) == 7
    assert body["history"][-1]["date"] == today.isoformat()


def test_each_user_only_sees_their_own_usage(client, customer, business):
    mine = client.get("/api/energy/usage", params={"days": 30}, headers=customer.headers).json()
    theirs = client.get("/api/energy/usage", params={"days": 30}, headers=business.headers).json()
    assert [p["kwh"] for p in mine["history"]] != [p["kwh"] for p in theirs["history"]]


def test_plans_are_listed_read_only_with_the_users_current_plan(client, customer):
    response = client.get("/api/energy/plans", headers=customer.headers)
    assert response.status_code == 200
    body = response.json()
    assert {plan["name"] for plan in body["plans"]} == {"Eco Saver", "Home Standard", "Business Power"}
    current = next(plan for plan in body["plans"] if plan["id"] == body["current_plan_id"])
    assert current["name"] == "Home Standard"
    assert MONEY.match(current["price_per_kwh"]) and MONEY.match(current["monthly_fee"])
    assert body["is_simulated"] is True
    assert "SIMULATION" in body["disclaimer"]


def test_plans_cannot_be_modified_through_the_api(client, customer):
    for method in ("post", "put", "delete", "patch"):
        response = getattr(client, method)("/api/energy/plans", headers=customer.headers)
        assert response.status_code == 405, method


def test_monthly_report_covers_all_90_simulated_days(client, customer):
    body = client.get("/api/energy/reports", headers=customer.headers).json()
    months = body["months"]
    assert body["is_simulated"] is True
    assert 3 <= len(months) <= 4
    assert sum(month["days_recorded"] for month in months) == 90
    assert [m["month"] for m in months] == sorted(m["month"] for m in months)
    for month in months:
        assert re.fullmatch(r"\d{4}-\d{2}", month["month"])
        assert month["average_daily_kwh"] == pytest.approx(month["total_kwh"] / month["days_recorded"], abs=0.01)
        assert MONEY.match(month["estimated_cost"])


# ------------------------------------------------------- pure functions
def test_simulated_readings_are_deterministic_and_user_specific():
    days = [date(2025, 1, 1) + timedelta(days=n) for n in range(30)]
    first = [energy_service.simulated_kwh(1, d) for d in days]
    assert first == [energy_service.simulated_kwh(1, d) for d in days]
    assert first != [energy_service.simulated_kwh(2, d) for d in days]


def test_simulated_readings_never_drop_below_one_kwh():
    days = [date(2025, 1, 1) + timedelta(days=n) for n in range(365)]
    assert min(energy_service.simulated_kwh(user_id, d) for user_id in range(1, 6) for d in days) >= 1.0


def test_monthly_estimate_uses_the_days_in_the_current_month():
    assert energy_service.estimate_monthly_kwh([10.0, 20.0], date(2025, 4, 15)) == 450.0  # 15 x 30
    assert energy_service.estimate_monthly_kwh([10.0, 20.0], date(2024, 2, 10)) == 435.0  # 15 x 29
    assert energy_service.estimate_monthly_kwh([], date(2025, 4, 15)) == 0.0


def test_estimated_cost_is_monthly_fee_plus_energy_charge():
    cost = energy_service.estimate_cost(300.0, Decimal("2.60"), Decimal("150.00"))
    assert cost == Decimal("930.00")  # 150 + 300 x 2.60
    assert energy_service.estimate_cost(0.0, Decimal("2.60"), Decimal("150.00")) == Decimal("150.00")


@pytest.mark.parametrize(
    "estimated, expected",
    [
        (0.0, EnergyStatus.NORMAL),
        (799.9, EnergyStatus.NORMAL),
        (800.0, EnergyStatus.HIGH),
        (1000.0, EnergyStatus.HIGH),
        (1000.1, EnergyStatus.OVER_LIMIT),
    ],
)
def test_status_boundaries_for_a_1000_kwh_plan(estimated, expected):
    assert energy_service.classify_status(estimated, 1000) == expected