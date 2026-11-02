"""Service CRUD, roles, ownership, filters. Service quantity is always 1 (see test_orders)."""
import pytest

from app.database import SessionLocal
from app.models import Service


def names(response) -> list[str]:
    assert response.status_code == 200, response.text
    return sorted(item["name"] for item in response.json())


def test_customer_cannot_create_a_service(client, customer):
    response = client.post(
        "/api/services",
        json={"name": "Thing", "price": "10.00", "category": "Misc", "sector": "SERVICE", "delivery_days": 2},
        headers=customer.headers,
    )
    assert response.status_code == 403
    assert "Provider or Business" in response.json()["detail"]


@pytest.mark.parametrize("role_fixture", ["provider", "business"])
def test_provider_and_business_can_create_services(request, make_service, role_fixture):
    account = request.getfixturevalue(role_fixture)
    service = make_service(account)
    assert service["provider_id"] == account.id
    assert service["price"] == "250.50"
    assert service["delivery_days"] == 5
    assert service["availability"] is True
    assert "notes" not in service  # notes belong to the order checkout


def test_provider_can_read_update_and_delete_their_service(client, provider, customer, make_service):
    service = make_service(provider)
    fetched = client.get(f"/api/services/{service['id']}", headers=customer.headers)
    assert fetched.status_code == 200
    assert fetched.json()["provider_name"] == "Pieter Provider"

    updated = client.put(
        f"/api/services/{service['id']}",
        json={"price": "300.00", "delivery_days": 7, "availability": False},
        headers=provider.headers,
    )
    assert updated.status_code == 200
    assert updated.json()["price"] == "300.00"
    assert updated.json()["delivery_days"] == 7
    assert updated.json()["availability"] is False
    assert updated.json()["name"] == "Solar Installation"

    assert client.delete(f"/api/services/{service['id']}", headers=provider.headers).json() == {"detail": "Deleted"}
    assert client.get(f"/api/services/{service['id']}", headers=provider.headers).status_code == 404


@pytest.mark.parametrize(
    "overrides",
    [{"price": "0"}, {"delivery_days": 0}, {"delivery_days": 366}, {"name": "A"}, {"sector": "FOOD"}, {"price": "1.234"}],
)
def test_invalid_service_input_returns_400(client, provider, overrides):
    payload = {"name": "Valid Name", "price": "10.00", "category": "Misc", "sector": "SERVICE", "delivery_days": 3}
    payload.update(overrides)
    assert client.post("/api/services", json=payload, headers=provider.headers).status_code == 400


def test_other_users_cannot_edit_or_delete_my_service(client, provider, business, customer, make_service):
    service = make_service(provider)
    for account in (business, customer):  # business passes the role check but is not the owner
        assert client.put(f"/api/services/{service['id']}", json={"price": "1.00"}, headers=account.headers).status_code == 403
        assert client.delete(f"/api/services/{service['id']}", headers=account.headers).status_code == 403
    assert client.get(f"/api/services/{service['id']}", headers=provider.headers).json()["price"] == "250.50"


def test_unknown_service_returns_404(client, provider):
    assert client.get("/api/services/99999", headers=provider.headers).status_code == 404
    assert client.put("/api/services/99999", json={"delivery_days": 2}, headers=provider.headers).status_code == 404


def test_service_with_order_history_cannot_be_deleted(client, provider, customer, make_service):
    service = make_service(provider)
    order = client.post("/api/orders", json={"items": [{"service_id": service["id"]}]}, headers=customer.headers)
    assert order.status_code == 201
    response = client.delete(f"/api/services/{service['id']}", headers=provider.headers)
    assert response.status_code == 409


@pytest.fixture
def service_catalogue(client, provider, business, make_service):
    created = {
        "install": make_service(provider, name="Solar Installation", description="Roof work", price="250.50",
                                category="Installation", sector="ENERGY", delivery_days=5),
        "design": make_service(provider, name="Website Design", description="Responsive site design", price="7500.00",
                               category="Web", sector="SERVICE", delivery_days=14),
        "audit": make_service(business, name="Energy Audit", description="Efficiency audit", price="1500.00",
                              category="Consulting", sector="ENERGY", delivery_days=4, availability=False),
    }
    with SessionLocal() as db:
        db.get(Service, created["install"]["id"]).rating = 4.5
        db.get(Service, created["design"]["id"]).rating = 3.5
        db.commit()
    return created


def test_service_search_and_filters(client, provider, service_catalogue):
    h = provider.headers
    assert names(client.get("/api/services", params={"q": "responsive"}, headers=h)) == ["Website Design"]
    assert names(client.get("/api/services", params={"category": "INSTALLATION"}, headers=h)) == ["Solar Installation"]
    assert names(client.get("/api/services", params={"min_price": 1000, "max_price": 2000}, headers=h)) == ["Energy Audit"]
    assert names(client.get("/api/services", params={"min_rating": 4}, headers=h)) == ["Solar Installation"]
    assert names(client.get("/api/services", params={"sector": "SERVICE"}, headers=h)) == ["Website Design"]
    assert names(client.get("/api/services", params={"availability": True}, headers=h)) == [
        "Solar Installation", "Website Design",
    ]
    assert names(client.get("/api/services", params={"availability": False}, headers=h)) == ["Energy Audit"]


def test_service_owner_me_and_invalid_price_range(client, provider, business, service_catalogue):
    assert len(client.get("/api/services", params={"owner": "me"}, headers=provider.headers).json()) == 2
    assert len(client.get("/api/services", params={"owner": "me"}, headers=business.headers).json()) == 1
    bad = client.get("/api/services", params={"min_price": 10, "max_price": 5}, headers=provider.headers)
    assert bad.status_code == 400