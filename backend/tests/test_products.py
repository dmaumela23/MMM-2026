"""Product CRUD, roles, ownership, search and filters."""
from decimal import Decimal

import pytest
from sqlalchemy.exc import IntegrityError

from app.database import SessionLocal
from app.models import Product


def names(response) -> list[str]:
    assert response.status_code == 200, response.text
    return sorted(item["name"] for item in response.json())


# ---------------------------------------------------------------- roles
def test_customer_cannot_create_a_product(client, customer):
    response = client.post(
        "/api/products",
        json={"name": "Thing", "price": "10.00", "category": "Misc", "sector": "DIGITAL"},
        headers=customer.headers,
    )
    assert response.status_code == 403
    assert list(response.json()) == ["detail"]


def test_provider_cannot_create_a_product(client, provider):
    response = client.post(
        "/api/products",
        json={"name": "Thing", "price": "10.00", "category": "Misc", "sector": "DIGITAL"},
        headers=provider.headers,
    )
    assert response.status_code == 403
    assert "Business" in response.json()["detail"]


def test_creating_a_product_requires_a_token(client):
    response = client.post("/api/products", json={"name": "Thing"})
    assert response.status_code == 401


# ----------------------------------------------------------------- CRUD
def test_business_can_create_read_update_and_delete_a_product(client, business, customer, make_product):
    created = make_product(business)
    assert created["price"] == "1499.00"  # money is a decimal STRING
    assert created["seller_id"] == business.id
    assert created["seller_name"] == "Busi Business"
    assert created["rating"] == 0
    assert created["availability"] is True
    assert created["created_at"] and created["updated_at"]

    fetched = client.get(f"/api/products/{created['id']}", headers=customer.headers)
    assert fetched.status_code == 200
    assert fetched.json()["name"] == "Solar Panel 550W"

    updated = client.put(
        f"/api/products/{created['id']}",
        json={"price": "1299.50", "stock_quantity": 4},
        headers=business.headers,
    )
    assert updated.status_code == 200
    assert updated.json()["price"] == "1299.50"
    assert updated.json()["stock_quantity"] == 4
    assert updated.json()["name"] == "Solar Panel 550W"  # untouched

    deleted = client.delete(f"/api/products/{created['id']}", headers=business.headers)
    assert deleted.status_code == 200
    assert deleted.json() == {"detail": "Deleted"}
    assert client.get(f"/api/products/{created['id']}", headers=business.headers).status_code == 404


def test_seller_and_rating_cannot_be_forced_by_the_client(client, business, other_business):
    response = client.post(
        "/api/products",
        json={
            "name": "Sneaky", "price": "10.00", "category": "Misc", "sector": "DIGITAL",
            "seller_id": other_business.id, "rating": 5,
        },
        headers=business.headers,
    )
    assert response.status_code == 201
    assert response.json()["seller_id"] == business.id
    assert response.json()["rating"] == 0


@pytest.mark.parametrize(
    "overrides",
    [
        {"price": "0"},
        {"price": "-5.00"},
        {"price": "10.999"},
        {"name": "A"},
        {"sector": "FOOD"},
        {"stock_quantity": -1},
        {"image_url": "ftp://example.com/x.png"},
        {"category": ""},
    ],
)
def test_invalid_product_input_returns_400(client, business, overrides):
    payload = {"name": "Valid Name", "price": "10.00", "category": "Misc", "sector": "DIGITAL"}
    payload.update(overrides)
    response = client.post("/api/products", json=payload, headers=business.headers)
    assert response.status_code == 400
    assert isinstance(response.json()["detail"], str)


def test_explicit_null_for_a_required_field_returns_400(client, business, make_product):
    product = make_product(business)
    response = client.put(f"/api/products/{product['id']}", json={"name": None}, headers=business.headers)
    assert response.status_code == 400


def test_unknown_product_returns_404(client, business):
    assert client.get("/api/products/99999", headers=business.headers).status_code == 404
    assert client.put("/api/products/99999", json={"stock_quantity": 1}, headers=business.headers).status_code == 404
    assert client.delete("/api/products/99999", headers=business.headers).status_code == 404


def test_non_numeric_product_id_returns_400(client, business):
    assert client.get("/api/products/abc", headers=business.headers).status_code == 400


# ------------------------------------------------------------ ownership
def test_another_business_cannot_edit_or_delete_my_product(client, business, other_business, make_product):
    product = make_product(business)
    edit = client.put(f"/api/products/{product['id']}", json={"price": "1.00"}, headers=other_business.headers)
    delete = client.delete(f"/api/products/{product['id']}", headers=other_business.headers)
    assert edit.status_code == 403
    assert delete.status_code == 403
    unchanged = client.get(f"/api/products/{product['id']}", headers=business.headers).json()
    assert unchanged["price"] == "1499.00"


def test_provider_and_customer_cannot_edit_or_delete_products(client, business, provider, customer, make_product):
    product = make_product(business)
    for account in (provider, customer):
        assert client.put(f"/api/products/{product['id']}", json={"price": "1.00"}, headers=account.headers).status_code == 403
        assert client.delete(f"/api/products/{product['id']}", headers=account.headers).status_code == 403


def test_product_with_order_history_cannot_be_deleted(client, business, customer, make_product):
    product = make_product(business)
    order = client.post(
        "/api/orders", json={"items": [{"product_id": product["id"], "quantity": 1}]}, headers=customer.headers
    )
    assert order.status_code == 201
    response = client.delete(f"/api/products/{product['id']}", headers=business.headers)
    assert response.status_code == 409
    assert "order history" in response.json()["detail"]
    assert client.get(f"/api/products/{product['id']}", headers=business.headers).status_code == 200


def test_database_check_constraint_rejects_a_non_positive_price(business):
    with SessionLocal() as db:
        db.add(
            Product(
                seller_id=business.id, name="Bad", description="", price=Decimal("-1.00"),
                category="Test", sector="DIGITAL", stock_quantity=1,
            )
        )
        with pytest.raises(IntegrityError):
            db.commit()


# ------------------------------------------------------ search / filters
@pytest.fixture
def catalogue(client, business, make_product):
    specs = {
        "solar": dict(name="Solar Panel 550W", description="Monocrystalline panel", price="1499.00",
                      category="Solar", sector="ENERGY", stock_quantity=10),
        "backup": dict(name="Cloud Backup", description="Backup for 100% of your files", price="299.00",
                       category="Software", sector="DIGITAL", stock_quantity=50),
        "battery": dict(name="Battery Pack", description="Lithium storage", price="9999.00",
                        category="Storage", sector="ENERGY", stock_quantity=0),
        "legacy": dict(name="Legacy Kit", description="Old stock", price="50.00",
                       category="Solar", sector="ENERGY", stock_quantity=5, availability=False),
    }
    created = {key: make_product(business, **spec) for key, spec in specs.items()}
    with SessionLocal() as db:  # ratings are stored values; the API has no way to set them
        for key, rating in {"solar": 4.5, "backup": 3.0, "battery": 4.8}.items():
            db.get(Product, created[key]["id"]).rating = rating
        db.commit()
    return created


def test_search_is_case_insensitive_and_covers_name_and_description(client, business, catalogue):
    assert names(client.get("/api/products", params={"q": "SOLAR"}, headers=business.headers)) == ["Solar Panel 550W"]
    assert names(client.get("/api/products", params={"q": "lithium"}, headers=business.headers)) == ["Battery Pack"]


def test_search_treats_percent_and_underscore_literally(client, business, catalogue):
    percent = client.get("/api/products", params={"q": "%"}, headers=business.headers)
    underscore = client.get("/api/products", params={"q": "_"}, headers=business.headers)
    assert names(percent) == ["Cloud Backup"]  # only the description containing a literal %
    assert names(underscore) == []


def test_sql_injection_text_is_just_a_search_term(client, business, catalogue):
    response = client.get("/api/products", params={"q": "'; DROP TABLE products; --"}, headers=business.headers)
    assert names(response) == []
    assert len(client.get("/api/products", headers=business.headers).json()) == 4


def test_category_filter_is_case_insensitive(client, business, catalogue):
    assert names(client.get("/api/products", params={"category": "solar"}, headers=business.headers)) == [
        "Legacy Kit", "Solar Panel 550W",
    ]


def test_price_filters(client, business, catalogue):
    assert names(client.get("/api/products", params={"min_price": 1000}, headers=business.headers)) == [
        "Battery Pack", "Solar Panel 550W",
    ]
    assert names(client.get("/api/products", params={"max_price": 299}, headers=business.headers)) == [
        "Cloud Backup", "Legacy Kit",
    ]
    assert names(client.get("/api/products", params={"min_price": 300, "max_price": 2000}, headers=business.headers)) == [
        "Solar Panel 550W",
    ]


def test_min_price_greater_than_max_price_returns_400(client, business, catalogue):
    response = client.get("/api/products", params={"min_price": 500, "max_price": 100}, headers=business.headers)
    assert response.status_code == 400
    assert "min_price" in response.json()["detail"]


def test_rating_filter(client, business, catalogue):
    assert names(client.get("/api/products", params={"min_rating": 4}, headers=business.headers)) == [
        "Battery Pack", "Solar Panel 550W",
    ]
    assert client.get("/api/products", params={"min_rating": 6}, headers=business.headers).status_code == 400


def test_availability_filter_uses_flag_and_stock(client, business, catalogue):
    assert names(client.get("/api/products", params={"availability": True}, headers=business.headers)) == [
        "Cloud Backup", "Solar Panel 550W",
    ]
    assert names(client.get("/api/products", params={"availability": False}, headers=business.headers)) == [
        "Battery Pack", "Legacy Kit",
    ]


def test_sector_filter_and_invalid_sector(client, business, catalogue):
    assert names(client.get("/api/products", params={"sector": "DIGITAL"}, headers=business.headers)) == ["Cloud Backup"]
    assert client.get("/api/products", params={"sector": "FOOD"}, headers=business.headers).status_code == 400


def test_filters_can_be_combined(client, business, catalogue):
    response = client.get("/api/products", params={"category": "solar", "availability": True}, headers=business.headers)
    assert names(response) == ["Solar Panel 550W"]


def test_owner_me_returns_only_my_products(client, business, other_business, catalogue):
    assert len(client.get("/api/products", params={"owner": "me"}, headers=business.headers).json()) == 4
    assert client.get("/api/products", params={"owner": "me"}, headers=other_business.headers).json() == []