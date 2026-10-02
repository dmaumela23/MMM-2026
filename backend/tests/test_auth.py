"""Registration, login, /me and JWT protection."""
import json
from datetime import timedelta

import pytest
from sqlalchemy import delete, func, select

from app.auth.jwt import create_access_token
from app.database import SessionLocal
from app.models import EnergyUsage, User

PASSWORD = "Passw0rd123"


def test_register_returns_201_with_token_and_public_user_fields_only(client, reg_payload):
    response = client.post("/api/auth/register", json=reg_payload())
    assert response.status_code == 201
    body = response.json()
    assert body["token_type"] == "bearer"
    assert len(body["access_token"]) > 20
    user = body["user"]
    assert user["email"] == "test.user@example.com"
    assert user["role"] == "CUSTOMER"
    assert user["full_name"] == "Test User"
    assert "password" not in user and "password_hash" not in user
    assert "password" not in json.dumps(body).lower().replace("token", "")


def test_register_stores_only_a_bcrypt_hash(client, reg_payload):
    client.post("/api/auth/register", json=reg_payload())
    with SessionLocal() as db:
        user = db.scalars(select(User).where(User.email == "test.user@example.com")).one()
        assert user.password_hash != PASSWORD
        assert user.password_hash.startswith("$2")


def test_register_creates_settings_default_plan_and_90_days_of_simulated_usage(client, reg_payload):
    client.post("/api/auth/register", json=reg_payload())
    with SessionLocal() as db:
        user = db.scalars(select(User).where(User.email == "test.user@example.com")).one()
        assert user.energy_plan_id is not None
        assert user.energy_plan.name == "Home Standard"
        assert user.settings is not None
        assert user.settings.theme == "SYSTEM"
        total = db.scalar(select(func.count()).select_from(EnergyUsage).where(EnergyUsage.user_id == user.id))
        simulated = db.scalar(
            select(func.count())
            .select_from(EnergyUsage)
            .where(EnergyUsage.user_id == user.id, EnergyUsage.is_simulated.is_(True))
        )
        assert total == 90
        assert simulated == 90


@pytest.mark.parametrize("role", ["CUSTOMER", "PROVIDER", "BUSINESS"])
def test_register_accepts_every_valid_role(client, reg_payload, role):
    response = client.post("/api/auth/register", json=reg_payload(role=role, email=f"{role}@example.com"))
    assert response.status_code == 201
    assert response.json()["user"]["role"] == role


def test_register_duplicate_email_returns_409_even_with_different_case(client, reg_payload):
    assert client.post("/api/auth/register", json=reg_payload()).status_code == 201
    response = client.post("/api/auth/register", json=reg_payload(email="TEST.User@Example.com"))
    assert response.status_code == 409
    assert response.json() == {"detail": "An account with this email already exists"}


def test_register_lowercases_the_email(client, reg_payload):
    response = client.post("/api/auth/register", json=reg_payload(email="Mixed.Case@Example.com"))
    assert response.json()["user"]["email"] == "mixed.case@example.com"


@pytest.mark.parametrize(
    "overrides, fragment",
    [
        ({"password": "short1", "confirm_password": "short1"}, "at least 8"),
        ({"password": "onlyletters", "confirm_password": "onlyletters"}, "digit"),
        ({"password": "12345678", "confirm_password": "12345678"}, "letter"),
        ({"confirm_password": "Different123"}, "do not match"),
        ({"email": "not-an-email"}, "email"),
        ({"role": "ADMIN"}, "role"),
        ({"phone": "abc"}, "phone"),
        ({"full_name": "A"}, "full_name"),
    ],
)
def test_register_rejects_invalid_input_with_400_and_readable_detail(client, reg_payload, overrides, fragment):
    response = client.post("/api/auth/register", json=reg_payload(**overrides))
    assert response.status_code == 400
    detail = response.json()["detail"]
    assert isinstance(detail, str)
    assert fragment.lower() in detail.lower()


def test_register_with_empty_body_returns_400(client):
    response = client.post("/api/auth/register", json={})
    assert response.status_code == 400
    assert isinstance(response.json()["detail"], str)


def test_login_returns_token_for_correct_credentials(client, reg_payload):
    client.post("/api/auth/register", json=reg_payload())
    response = client.post("/api/auth/login", json={"email": "test.user@example.com", "password": PASSWORD})
    assert response.status_code == 200
    assert response.json()["access_token"]
    assert response.json()["user"]["email"] == "test.user@example.com"


def test_login_email_is_case_insensitive(client, reg_payload):
    client.post("/api/auth/register", json=reg_payload())
    response = client.post("/api/auth/login", json={"email": "TEST.USER@EXAMPLE.COM", "password": PASSWORD})
    assert response.status_code == 200


def test_login_with_wrong_password_and_unknown_email_give_the_same_401(client, reg_payload):
    client.post("/api/auth/register", json=reg_payload())
    wrong_password = client.post("/api/auth/login", json={"email": "test.user@example.com", "password": "Wrong123x"})
    unknown_email = client.post("/api/auth/login", json={"email": "nobody@example.com", "password": PASSWORD})
    assert wrong_password.status_code == unknown_email.status_code == 401
    assert wrong_password.json() == unknown_email.json() == {"detail": "Incorrect email or password"}


def test_login_with_missing_fields_returns_400(client):
    assert client.post("/api/auth/login", json={"email": "a@example.com"}).status_code == 400


def test_me_returns_the_logged_in_user(client, customer):
    response = client.get("/api/auth/me", headers=customer.headers)
    assert response.status_code == 200
    assert response.json()["id"] == customer.id
    assert response.json()["email"] == customer.email


PROTECTED = [
    ("GET", "/api/auth/me", None),
    ("GET", "/api/users/1", None),
    ("GET", "/api/settings", None),
    ("GET", "/api/products", None),
    ("GET", "/api/services", None),
    ("GET", "/api/orders", None),
    ("GET", "/api/orders/incoming", None),
    ("POST", "/api/orders", {"items": [{"product_id": 1, "quantity": 1}]}),
    ("GET", "/api/energy/usage", None),
    ("GET", "/api/energy/plans", None),
    ("GET", "/api/energy/reports", None),
]


@pytest.mark.parametrize("method, path, body", PROTECTED)
def test_protected_endpoints_return_401_without_a_token(client, method, path, body):
    response = client.request(method, path, json=body)
    assert response.status_code == 401
    assert response.json()["detail"]
    assert response.headers["www-authenticate"] == "Bearer"


def test_garbage_token_returns_401(client):
    response = client.get("/api/auth/me", headers={"Authorization": "Bearer not.a.token"})
    assert response.status_code == 401
    assert response.json() == {"detail": "Invalid authentication token."}


def test_expired_token_returns_401_with_session_expired_message(client, customer):
    expired = create_access_token(customer.id, expires_delta=timedelta(seconds=-30))
    response = client.get("/api/auth/me", headers={"Authorization": f"Bearer {expired}"})
    assert response.status_code == 401
    assert "expired" in response.json()["detail"].lower()


def test_token_of_a_deleted_user_returns_401(client, customer):
    with SessionLocal() as db:
        db.execute(delete(User).where(User.id == customer.id))
        db.commit()
    response = client.get("/api/auth/me", headers=customer.headers)
    assert response.status_code == 401