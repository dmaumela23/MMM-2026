"""Error responses always look like {"detail": "..."} and never leak internals. Also /health and /docs."""
import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient
from sqlalchemy.exc import OperationalError

from app.database import get_db
from app.error_handlers import INTERNAL_ERROR_MESSAGE, register_exception_handlers
from app.main import app


def test_401_has_detail_and_www_authenticate_header(client):
    response = client.get("/api/auth/me")
    assert response.status_code == 401
    assert list(response.json()) == ["detail"]
    assert response.headers["www-authenticate"] == "Bearer"


def test_403_has_only_a_detail_field(client, customer):
    response = client.post("/api/services", json={"name": "x"}, headers=customer.headers)
    assert response.status_code == 403
    assert list(response.json()) == ["detail"]


def test_404_for_unknown_route_uses_the_same_shape(client):
    response = client.get("/api/does-not-exist")
    assert response.status_code == 404
    assert response.json() == {"detail": "Not Found"}


def test_405_uses_the_same_shape(client):
    response = client.delete("/api/auth/me")
    assert response.status_code == 405
    assert response.json() == {"detail": "Method Not Allowed"}


def test_409_uses_the_same_shape(client, reg_payload):
    client.post("/api/auth/register", json=reg_payload())
    response = client.post("/api/auth/register", json=reg_payload())
    assert response.status_code == 409
    assert list(response.json()) == ["detail"]


def test_validation_errors_are_400_with_a_string_detail(client, reg_payload):
    response = client.post("/api/auth/register", json=reg_payload(email="bad"))
    assert response.status_code == 400
    assert isinstance(response.json()["detail"], str)


def test_malformed_json_returns_400_not_a_crash(client):
    response = client.post(
        "/api/auth/login", content="{this is not json", headers={"Content-Type": "application/json"}
    )
    assert response.status_code == 400
    assert isinstance(response.json()["detail"], str)


def test_unexpected_exception_returns_a_generic_500_without_internal_details():
    mini_app = FastAPI()
    register_exception_handlers(mini_app)

    @mini_app.get("/boom")
    def boom():
        raise RuntimeError("password=hunter2 host=10.0.0.5 table=users")

    response = TestClient(mini_app, raise_server_exceptions=False).get("/boom")
    assert response.status_code == 500
    assert response.json() == {"detail": INTERNAL_ERROR_MESSAGE}
    for secret in ("hunter2", "10.0.0.5", "users"):
        assert secret not in response.text


def test_health_reports_ok_when_the_database_is_reachable(client):
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok", "service": "MMM API", "database": "ok"}


def test_health_returns_503_without_leaking_details_when_the_database_is_down(client):
    class BrokenSession:
        def execute(self, *args, **kwargs):
            raise OperationalError("SELECT 1", {}, Exception("connection refused to db.internal:5432"))

    def broken_db():
        yield BrokenSession()

    app.dependency_overrides[get_db] = broken_db
    try:
        response = client.get("/health")
    finally:
        app.dependency_overrides.clear()
    assert response.status_code == 503
    assert response.json() == {"detail": "Database unavailable"}
    assert "db.internal" not in response.text


def test_swagger_ui_and_openapi_document_are_served(client):
    assert client.get("/docs").status_code == 200
    schema = client.get("/openapi.json")
    assert schema.status_code == 200
    paths = schema.json()["paths"]
    for expected in (
        "/health",
        "/api/auth/register",
        "/api/auth/login",
        "/api/auth/me",
        "/api/products",
        "/api/products/{product_id}",
        "/api/services/{service_id}",
        "/api/orders/incoming",
        "/api/orders/{order_id}/items/{item_id}/status",
        "/api/energy/usage",
        "/api/settings",
    ):
        assert expected in paths, expected


def test_openapi_documents_400_instead_of_422_for_validation_errors(client):
    register = client.get("/openapi.json").json()["paths"]["/api/auth/register"]["post"]["responses"]
    assert "400" in register and "409" in register
    assert "422" not in register


@pytest.mark.parametrize("path", ["/api/orders/incoming", "/api/energy/usage"])
def test_openapi_marks_protected_routes_with_bearer_security(client, path):
    operation = client.get("/openapi.json").json()["paths"][path]["get"]
    assert operation.get("security")