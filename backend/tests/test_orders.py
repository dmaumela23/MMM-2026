"""Orders: server-side pricing, stock, transactions, status workflow, access control."""
from decimal import Decimal

import pytest

from app.enums import ItemStatus, OrderStatus
from app.services.order_service import (
    calculate_line_total,
    calculate_order_total,
    can_transition,
    derive_order_status,
)


def place(client, account, items, **extra):
    return client.post("/api/orders", json={"items": items, **extra}, headers=account.headers)


def stock_of(client, account, product_id) -> int:
    return client.get(f"/api/products/{product_id}", headers=account.headers).json()["stock_quantity"]


def item_status_url(order_id, item_id) -> str:
    return f"/api/orders/{order_id}/items/{item_id}/status"


# ------------------------------------------------- pure business rules
def test_line_and_order_totals_use_exact_decimal_arithmetic():
    assert calculate_line_total(Decimal("19.99"), 3) == Decimal("59.97")
    lines = [(Decimal("0.10"), 3, "PENDING"), (Decimal("0.20"), 1, "PENDING")]
    assert calculate_order_total(lines) == Decimal("0.50")  # floats would give 0.5000000000000001


def test_cancelled_lines_are_excluded_from_the_total():
    lines = [(Decimal("100.00"), 1, "PENDING"), (Decimal("50.00"), 2, "CANCELLED")]
    assert calculate_order_total(lines) == Decimal("100.00")


@pytest.mark.parametrize(
    "statuses, expected",
    [
        ([], OrderStatus.PENDING),
        (["PENDING", "PENDING"], OrderStatus.PENDING),
        (["ACCEPTED", "PENDING"], OrderStatus.PROCESSING),
        (["COMPLETED", "PENDING"], OrderStatus.PROCESSING),
        (["COMPLETED", "IN_PROGRESS"], OrderStatus.PROCESSING),
        (["COMPLETED", "COMPLETED"], OrderStatus.COMPLETED),
        (["COMPLETED", "CANCELLED"], OrderStatus.COMPLETED),
        (["CANCELLED", "PENDING"], OrderStatus.PENDING),
        (["CANCELLED", "CANCELLED"], OrderStatus.CANCELLED),
    ],
)
def test_order_status_is_derived_from_its_lines(statuses, expected):
    assert derive_order_status(statuses) == expected


@pytest.mark.parametrize(
    "current, target, allowed",
    [
        ("PENDING", "ACCEPTED", True),
        ("PENDING", "CANCELLED", True),
        ("PENDING", "IN_PROGRESS", False),
        ("PENDING", "COMPLETED", False),
        ("ACCEPTED", "IN_PROGRESS", True),
        ("ACCEPTED", "CANCELLED", True),
        ("ACCEPTED", "COMPLETED", False),
        ("IN_PROGRESS", "COMPLETED", True),
        ("IN_PROGRESS", "CANCELLED", False),
        ("COMPLETED", "CANCELLED", False),
        ("CANCELLED", "PENDING", False),
    ],
)
def test_line_status_transition_rules(current, target, allowed):
    assert can_transition(current, target) is allowed
    assert ItemStatus(current) in ItemStatus  # both values are real statuses


# --------------------------------------------------------- creation
def test_total_is_calculated_by_the_server_and_client_values_are_ignored(
    client, customer, business, provider, make_product, make_service
):
    product = make_product(business, price="100.00", stock_quantity=10)
    service = make_service(provider, price="250.50")
    response = place(
        client,
        customer,
        [
            {"product_id": product["id"], "quantity": 3, "unit_price": "0.01"},  # ignored
            {"service_id": service["id"]},
        ],
        total_amount="1.00",  # ignored
        notes="  Please call first  ",
    )
    assert response.status_code == 201, response.text
    order = response.json()
    assert order["total_amount"] == "550.50"
    assert order["status"] == "PENDING"
    assert order["notes"] == "Please call first"
    assert order["customer_id"] == customer.id
    product_line, service_line = order["items"]
    assert (product_line["item_type"], product_line["unit_price"], product_line["line_total"]) == ("PRODUCT", "100.00", "300.00")
    assert (service_line["item_type"], service_line["unit_price"], service_line["quantity"]) == ("SERVICE", "250.50", 1)
    assert product_line["seller_name"] == "Busi Business"
    assert {line["item_status"] for line in order["items"]} == {"PENDING"}


def test_decimal_prices_are_exact_in_the_api(client, customer, business, make_product):
    product = make_product(business, price="19.99")
    order = place(client, customer, [{"product_id": product["id"], "quantity": 3}]).json()
    assert order["total_amount"] == "59.97"


def test_creating_an_order_reduces_stock_and_ordering_the_last_units_works(client, customer, business, make_product):
    product = make_product(business, stock_quantity=2)
    assert place(client, customer, [{"product_id": product["id"], "quantity": 2}]).status_code == 201
    assert stock_of(client, customer, product["id"]) == 0
    again = place(client, customer, [{"product_id": product["id"], "quantity": 1}])
    assert again.status_code == 400
    assert "Insufficient stock" in again.json()["detail"]


def test_insufficient_stock_returns_400_and_changes_nothing(client, customer, business, make_product):
    product = make_product(business, stock_quantity=2)
    response = place(client, customer, [{"product_id": product["id"], "quantity": 3}])
    assert response.status_code == 400
    assert "Insufficient stock" in response.json()["detail"]
    assert stock_of(client, customer, product["id"]) == 2
    assert client.get("/api/orders", headers=customer.headers).json() == []


def test_failed_order_leaves_stock_of_other_lines_untouched(client, customer, business, make_product):
    fine = make_product(business, name="Fine Product", stock_quantity=10)
    scarce = make_product(business, name="Scarce Product", stock_quantity=1)
    response = place(
        client,
        customer,
        [{"product_id": fine["id"], "quantity": 2}, {"product_id": scarce["id"], "quantity": 5}],
    )
    assert response.status_code == 400
    assert stock_of(client, customer, fine["id"]) == 10
    assert client.get("/api/orders", headers=customer.headers).json() == []


def test_unavailable_product_or_service_cannot_be_ordered(client, customer, business, provider, make_product, make_service):
    product = make_product(business, availability=False)
    service = make_service(provider, availability=False)
    by_product = place(client, customer, [{"product_id": product["id"], "quantity": 1}])
    by_service = place(client, customer, [{"service_id": service["id"]}])
    assert by_product.status_code == 400 and "unavailable" in by_product.json()["detail"]
    assert by_service.status_code == 400 and "unavailable" in by_service.json()["detail"]


def test_unknown_product_or_service_returns_404(client, customer):
    assert place(client, customer, [{"product_id": 9999, "quantity": 1}]).status_code == 404
    assert place(client, customer, [{"service_id": 9999}]).status_code == 404


@pytest.mark.parametrize(
    "items",
    [
        [],
        [{"quantity": 1}],
        [{"product_id": 1, "service_id": 1, "quantity": 1}],
        [{"product_id": 1, "quantity": 0}],
        [{"product_id": 1, "quantity": 100}],
        [{"service_id": 1, "quantity": 2}],
    ],
)
def test_invalid_order_shapes_return_400(client, customer, items):
    assert place(client, customer, items).status_code == 400


def test_the_same_product_twice_in_one_order_returns_400(client, customer, business, make_product):
    product = make_product(business)
    response = place(client, customer, [{"product_id": product["id"], "quantity": 1}, {"product_id": product["id"], "quantity": 1}])
    assert response.status_code == 400
    assert stock_of(client, customer, product["id"]) == 10


def test_order_keeps_the_price_from_order_time(client, customer, business, make_product):
    product = make_product(business, price="100.00")
    order = place(client, customer, [{"product_id": product["id"], "quantity": 1}]).json()
    client.put(f"/api/products/{product['id']}", json={"price": "999.00"}, headers=business.headers)
    detail = client.get(f"/api/orders/{order['id']}", headers=customer.headers).json()
    assert detail["items"][0]["unit_price"] == "100.00"
    assert detail["total_amount"] == "100.00"


# ----------------------------------------------------- reading orders
def test_list_orders_returns_only_my_orders_with_summaries(client, customer, make_user, business, make_product):
    product = make_product(business, name="Widget")
    stranger = make_user("CUSTOMER")
    place(client, customer, [{"product_id": product["id"], "quantity": 2}])
    place(client, stranger, [{"product_id": product["id"], "quantity": 1}])
    mine = client.get("/api/orders", headers=customer.headers).json()
    assert len(mine) == 1
    assert mine[0]["total_amount"] == "2998.00"
    assert mine[0]["item_count"] == 1
    assert mine[0]["first_item_name"] == "Widget"
    assert mine[0]["status"] == "PENDING"


def test_order_detail_access_rules(client, customer, make_user, business, other_business, make_product):
    product = make_product(business)
    order = place(client, customer, [{"product_id": product["id"], "quantity": 1}]).json()
    url = f"/api/orders/{order['id']}"
    assert client.get(url, headers=customer.headers).status_code == 200
    assert client.get(url, headers=business.headers).status_code == 200  # seller of a line in it
    assert client.get(url, headers=make_user("CUSTOMER").headers).status_code == 403
    assert client.get(url, headers=other_business.headers).status_code == 403
    assert client.get("/api/orders/99999", headers=customer.headers).status_code == 404


# ------------------------------------------------ customer cancellation
def test_customer_cancel_restores_stock_and_cancels_every_line(client, customer, business, make_product):
    product = make_product(business, stock_quantity=10)
    order = place(client, customer, [{"product_id": product["id"], "quantity": 4}]).json()
    assert stock_of(client, customer, product["id"]) == 6
    response = client.put(f"/api/orders/{order['id']}", json={"status": "CANCELLED"}, headers=customer.headers)
    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "CANCELLED"
    assert body["total_amount"] == "0.00"
    assert {line["item_status"] for line in body["items"]} == {"CANCELLED"}
    assert stock_of(client, customer, product["id"]) == 10


def test_cancelling_twice_or_with_other_statuses_returns_400(client, customer, business, make_product):
    product = make_product(business)
    order = place(client, customer, [{"product_id": product["id"], "quantity": 1}]).json()
    url = f"/api/orders/{order['id']}"
    assert client.put(url, json={"status": "COMPLETED"}, headers=customer.headers).status_code == 400
    assert client.put(url, json={"status": "NONSENSE"}, headers=customer.headers).status_code == 400
    assert client.put(url, json={"status": "CANCELLED"}, headers=customer.headers).status_code == 200
    assert client.put(url, json={"status": "CANCELLED"}, headers=customer.headers).status_code == 400


def test_only_the_customer_can_cancel_the_order(client, customer, business, make_product):
    product = make_product(business)
    order = place(client, customer, [{"product_id": product["id"], "quantity": 1}]).json()
    response = client.put(f"/api/orders/{order['id']}", json={"status": "CANCELLED"}, headers=business.headers)
    assert response.status_code == 403


def test_customer_cannot_cancel_after_the_seller_has_started_work(client, customer, business, make_product):
    product = make_product(business)
    order = place(client, customer, [{"product_id": product["id"], "quantity": 1}]).json()
    client.put(item_status_url(order["id"], order["items"][0]["id"]), json={"item_status": "ACCEPTED"}, headers=business.headers)
    response = client.put(f"/api/orders/{order['id']}", json={"status": "CANCELLED"}, headers=customer.headers)
    assert response.status_code == 400
    assert "no longer be cancelled" in response.json()["detail"]


# ------------------------------------------------- seller status updates
def test_product_only_order_progresses_and_never_stays_pending(client, customer, business, make_product):
    product = make_product(business)
    order = place(client, customer, [{"product_id": product["id"], "quantity": 1}]).json()
    url = item_status_url(order["id"], order["items"][0]["id"])
    for new_status, expected_order_status in (
        ("ACCEPTED", "PROCESSING"),
        ("IN_PROGRESS", "PROCESSING"),
        ("COMPLETED", "COMPLETED"),
    ):
        response = client.put(url, json={"item_status": new_status}, headers=business.headers)
        assert response.status_code == 200, response.text
        assert response.json()["item_status"] == new_status
        detail = client.get(f"/api/orders/{order['id']}", headers=customer.headers).json()
        assert detail["status"] == expected_order_status


def test_service_line_can_be_progressed_by_its_provider(client, customer, provider, make_service):
    service = make_service(provider)
    order = place(client, customer, [{"service_id": service["id"]}]).json()
    url = item_status_url(order["id"], order["items"][0]["id"])
    for new_status in ("ACCEPTED", "IN_PROGRESS", "COMPLETED"):
        assert client.put(url, json={"item_status": new_status}, headers=provider.headers).status_code == 200
    assert client.get(f"/api/orders/{order['id']}", headers=customer.headers).json()["status"] == "COMPLETED"


def test_invalid_transitions_and_terminal_states_return_400(client, customer, business, make_product):
    product = make_product(business)
    order = place(client, customer, [{"product_id": product["id"], "quantity": 1}]).json()
    url = item_status_url(order["id"], order["items"][0]["id"])
    skip = client.put(url, json={"item_status": "COMPLETED"}, headers=business.headers)
    assert skip.status_code == 400
    assert "Cannot change item status" in skip.json()["detail"]
    for step in ("ACCEPTED", "IN_PROGRESS", "COMPLETED"):
        client.put(url, json={"item_status": step}, headers=business.headers)
    assert client.put(url, json={"item_status": "CANCELLED"}, headers=business.headers).status_code == 400
    assert client.put(url, json={"item_status": "DONE"}, headers=business.headers).status_code == 400


def test_only_the_seller_of_the_line_can_change_its_status(
    client, customer, provider, business, other_business, make_product
):
    product = make_product(business)
    order = place(client, customer, [{"product_id": product["id"], "quantity": 1}]).json()
    url = item_status_url(order["id"], order["items"][0]["id"])
    body = {"item_status": "ACCEPTED"}
    assert client.put(url, json=body, headers=other_business.headers).status_code == 403
    assert client.put(url, json=body, headers=provider.headers).status_code == 403
    assert client.put(url, json=body, headers=customer.headers).status_code == 403
    assert client.put(url, json=body).status_code == 401
    assert client.put(url, json=body, headers=business.headers).status_code == 200


def test_status_update_with_unknown_order_or_item_returns_404(client, customer, business, make_product):
    product = make_product(business)
    order = place(client, customer, [{"product_id": product["id"], "quantity": 1}]).json()
    body = {"item_status": "ACCEPTED"}
    assert client.put(item_status_url(99999, 1), json=body, headers=business.headers).status_code == 404
    assert client.put(item_status_url(order["id"], 99999), json=body, headers=business.headers).status_code == 404


def test_seller_cancelling_a_line_restores_stock_and_lowers_the_total(client, customer, business, make_product):
    first = make_product(business, name="First", price="100.00", stock_quantity=10)
    second = make_product(business, name="Second", price="50.00", stock_quantity=10)
    order = place(
        client, customer, [{"product_id": first["id"], "quantity": 1}, {"product_id": second["id"], "quantity": 2}]
    ).json()
    assert order["total_amount"] == "200.00"
    first_line, second_line = order["items"]

    cancel_first = client.put(item_status_url(order["id"], first_line["id"]), json={"item_status": "CANCELLED"}, headers=business.headers)
    assert cancel_first.status_code == 200
    detail = client.get(f"/api/orders/{order['id']}", headers=customer.headers).json()
    assert detail["total_amount"] == "100.00"
    assert detail["status"] == "PENDING"
    assert stock_of(client, customer, first["id"]) == 10

    # Customer cancels the rest: the already-cancelled line must not have its stock restored twice.
    assert client.put(f"/api/orders/{order['id']}", json={"status": "CANCELLED"}, headers=customer.headers).status_code == 200
    assert stock_of(client, customer, first["id"]) == 10
    assert stock_of(client, customer, second["id"]) == 10
    final = client.get(f"/api/orders/{order['id']}", headers=customer.headers).json()
    assert final["status"] == "CANCELLED"
    assert final["total_amount"] == "0.00"


# ------------------------------------------------------------ incoming
def test_incoming_lists_only_my_lines_for_products_and_services(
    client, customer, provider, business, other_business, make_product, make_service
):
    product = make_product(business, name="Battery")
    service = make_service(provider, name="Install")
    order = place(
        client, customer, [{"product_id": product["id"], "quantity": 2}, {"service_id": service["id"]}], notes="Gate code 1234"
    ).json()

    business_items = client.get("/api/orders/incoming", headers=business.headers).json()
    provider_items = client.get("/api/orders/incoming", headers=provider.headers).json()
    assert [(i["item_type"], i["name"], i["quantity"]) for i in business_items] == [("PRODUCT", "Battery", 2)]
    assert [(i["item_type"], i["name"], i["quantity"]) for i in provider_items] == [("SERVICE", "Install", 1)]
    assert business_items[0]["order_id"] == order["id"]
    assert business_items[0]["customer_name"] == "Cathy Customer"
    assert business_items[0]["notes"] == "Gate code 1234"
    assert business_items[0]["line_total"] == "2998.00"
    assert client.get("/api/orders/incoming", headers=other_business.headers).json() == []


def test_customers_cannot_use_the_incoming_endpoint(client, customer):
    assert client.get("/api/orders/incoming", headers=customer.headers).status_code == 403