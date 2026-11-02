"""Profile updates and password change: users can only touch their own account."""
PASSWORD = "Passw0rd123"


def test_user_can_read_their_own_profile(client, customer):
    response = client.get(f"/api/users/{customer.id}", headers=customer.headers)
    assert response.status_code == 200
    assert response.json()["email"] == customer.email


def test_user_cannot_read_another_users_profile(client, customer, business):
    response = client.get(f"/api/users/{business.id}", headers=customer.headers)
    assert response.status_code == 403


def test_unknown_user_returns_404(client, customer):
    assert client.get("/api/users/99999", headers=customer.headers).status_code == 404


def test_user_can_update_name_phone_and_email(client, customer):
    response = client.put(
        f"/api/users/{customer.id}",
        json={"full_name": "  Cathy Updated ", "phone": "+27 82 555 0100", "email": "New.Email@Example.com"},
        headers=customer.headers,
    )
    assert response.status_code == 200
    body = response.json()
    assert body["full_name"] == "Cathy Updated"
    assert body["phone"] == "+27825550100"
    assert body["email"] == "new.email@example.com"
    login = client.post("/api/auth/login", json={"email": "new.email@example.com", "password": PASSWORD})
    assert login.status_code == 200


def test_partial_update_changes_only_the_sent_fields(client, customer):
    before = client.get(f"/api/users/{customer.id}", headers=customer.headers).json()
    response = client.put(f"/api/users/{customer.id}", json={"full_name": "Only Name"}, headers=customer.headers)
    assert response.status_code == 200
    assert response.json()["full_name"] == "Only Name"
    assert response.json()["email"] == before["email"]
    assert response.json()["phone"] == before["phone"]


def test_user_cannot_update_another_users_profile(client, customer, business):
    response = client.put(f"/api/users/{business.id}", json={"full_name": "Hacked"}, headers=customer.headers)
    assert response.status_code == 403
    untouched = client.get(f"/api/users/{business.id}", headers=business.headers).json()
    assert untouched["full_name"] == "Busi Business"


def test_updating_to_an_email_that_is_taken_returns_409(client, customer, business):
    response = client.put(f"/api/users/{customer.id}", json={"email": business.email}, headers=customer.headers)
    assert response.status_code == 409


def test_keeping_the_same_email_is_not_a_conflict(client, customer):
    response = client.put(f"/api/users/{customer.id}", json={"email": customer.email}, headers=customer.headers)
    assert response.status_code == 200


def test_invalid_profile_values_return_400(client, customer):
    for body in ({"phone": "abc"}, {"email": "nope"}, {"full_name": "A"}, {"full_name": None}):
        response = client.put(f"/api/users/{customer.id}", json=body, headers=customer.headers)
        assert response.status_code == 400, body


def test_role_cannot_be_changed_through_the_profile_endpoint(client, customer):
    client.put(f"/api/users/{customer.id}", json={"role": "BUSINESS"}, headers=customer.headers)
    assert client.get("/api/auth/me", headers=customer.headers).json()["role"] == "CUSTOMER"


def test_change_password_then_login_only_works_with_the_new_password(client, customer):
    response = client.put(
        f"/api/users/{customer.id}/password",
        json={"current_password": PASSWORD, "new_password": "NewPassw0rd9"},
        headers=customer.headers,
    )
    assert response.status_code == 200
    assert response.json() == {"detail": "Password updated"}
    old = client.post("/api/auth/login", json={"email": customer.email, "password": PASSWORD})
    new = client.post("/api/auth/login", json={"email": customer.email, "password": "NewPassw0rd9"})
    assert old.status_code == 401
    assert new.status_code == 200


def test_change_password_rejects_wrong_current_password(client, customer):
    response = client.put(
        f"/api/users/{customer.id}/password",
        json={"current_password": "WrongPass1", "new_password": "NewPassw0rd9"},
        headers=customer.headers,
    )
    assert response.status_code == 400
    assert "current password" in response.json()["detail"].lower()


def test_change_password_rejects_weak_or_unchanged_password(client, customer):
    weak = client.put(
        f"/api/users/{customer.id}/password",
        json={"current_password": PASSWORD, "new_password": "weak"},
        headers=customer.headers,
    )
    same = client.put(
        f"/api/users/{customer.id}/password",
        json={"current_password": PASSWORD, "new_password": PASSWORD},
        headers=customer.headers,
    )
    assert weak.status_code == 400
    assert same.status_code == 400


def test_change_password_for_another_user_is_forbidden(client, customer, business):
    response = client.put(
        f"/api/users/{business.id}/password",
        json={"current_password": PASSWORD, "new_password": "NewPassw0rd9"},
        headers=customer.headers,
    )
    assert response.status_code == 403