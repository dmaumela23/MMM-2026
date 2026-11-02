"""Shared pytest fixtures.

SAFETY: these tests DROP AND RECREATE all tables. They therefore refuse to run unless
TEST_DATABASE_URL points at a database whose name ends with "_test". DATABASE_URL is
overwritten with the same value BEFORE the application is imported, so the application's
own engine can never point at your development database during a test run.
"""
import itertools
import os
from dataclasses import dataclass
from pathlib import Path

import pytest
from dotenv import load_dotenv
from sqlalchemy import text
from sqlalchemy.engine import make_url

load_dotenv(Path(__file__).resolve().parent.parent / ".env")

_test_url = os.environ.get("TEST_DATABASE_URL", "")
if not _test_url:
    pytest.exit("TEST_DATABASE_URL is not set. Add it to backend/.env (see .env.example).", returncode=2)
if not (make_url(_test_url).database or "").endswith("_test"):
    pytest.exit("Refusing to run: the database name in TEST_DATABASE_URL must end with '_test'.", returncode=2)

os.environ["DATABASE_URL"] = _test_url
if len(os.environ.get("JWT_SECRET", "")) < 32:
    os.environ["JWT_SECRET"] = "pytest-only-secret-key-not-for-production-0123456789"
os.environ["BCRYPT_ROUNDS"] = "4"  # fast hashing for tests only

from fastapi.testclient import TestClient  # noqa: E402

from app.database import Base, engine  # noqa: E402
from app.main import app  # noqa: E402

PASSWORD = "Passw0rd123"


@dataclass
class Account:
    id: int
    email: str
    role: str
    token: str
    password: str = PASSWORD

    @property
    def headers(self) -> dict[str, str]:
        return {"Authorization": f"Bearer {self.token}"}


@pytest.fixture(scope="session", autouse=True)
def database_schema():
    Base.metadata.drop_all(bind=engine)
    Base.metadata.create_all(bind=engine)
    yield
    Base.metadata.drop_all(bind=engine)
    engine.dispose()


@pytest.fixture(autouse=True)
def clean_database():
    yield
    table_names = ", ".join(f'"{table.name}"' for table in Base.metadata.sorted_tables)
    with engine.begin() as connection:
        connection.execute(text(f"TRUNCATE TABLE {table_names} RESTART IDENTITY CASCADE"))


@pytest.fixture
def client() -> TestClient:
    return TestClient(app)


@pytest.fixture
def reg_payload():
    def build(**overrides) -> dict:
        data = {
            "full_name": "Test User",
            "email": "test.user@example.com",
            "phone": "0821234567",
            "password": PASSWORD,
            "confirm_password": PASSWORD,
            "role": "CUSTOMER",
        }
        data.update(overrides)
        return data

    return build


@pytest.fixture
def make_user(client):
    counter = itertools.count(1)

    def create(role: str = "CUSTOMER", full_name: str | None = None, email: str | None = None) -> Account:
        number = next(counter)
        email = email or f"{role.lower()}{number}@example.com"
        response = client.post(
            "/api/auth/register",
            json={
                "full_name": full_name or f"{role.title()} {number}",
                "email": email,
                "phone": "0821234567",
                "password": PASSWORD,
                "confirm_password": PASSWORD,
                "role": role,
            },
        )
        assert response.status_code == 201, response.text
        body = response.json()
        return Account(id=body["user"]["id"], email=email, role=role, token=body["access_token"])

    return create


@pytest.fixture
def customer(make_user) -> Account:
    return make_user("CUSTOMER", full_name="Cathy Customer")


@pytest.fixture
def provider(make_user) -> Account:
    return make_user("PROVIDER", full_name="Pieter Provider")


@pytest.fixture
def business(make_user) -> Account:
    return make_user("BUSINESS", full_name="Busi Business")


@pytest.fixture
def other_business(make_user) -> Account:
    return make_user("BUSINESS", full_name="Other Business")


@pytest.fixture
def make_product(client):
    def create(account: Account, **overrides) -> dict:
        payload = {
            "name": "Solar Panel 550W",
            "description": "Monocrystalline panel",
            "price": "1499.00",
            "category": "Solar",
            "sector": "ENERGY",
            "image_url": "https://example.com/panel.png",
            "stock_quantity": 10,
        }
        payload.update(overrides)
        response = client.post("/api/products", json=payload, headers=account.headers)
        assert response.status_code == 201, response.text
        return response.json()

    return create


@pytest.fixture
def make_service(client):
    def create(account: Account, **overrides) -> dict:
        payload = {
            "name": "Solar Installation",
            "description": "Roof-mounted installation",
            "price": "250.50",
            "category": "Installation",
            "sector": "ENERGY",
            "delivery_days": 5,
        }
        payload.update(overrides)
        response = client.post("/api/services", json=payload, headers=account.headers)
        assert response.status_code == 201, response.text
        return response.json()

    return create