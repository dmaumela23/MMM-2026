"""User settings: notification preference, theme and FCM token."""


def test_new_user_has_default_settings(client, customer):
    response = client.get("/api/settings", headers=customer.headers)
    assert response.status_code == 200
    assert response.json() == {"notification_enabled": True, "theme": "SYSTEM", "fcm_token": None}


def test_partial_update_changes_only_the_sent_fields_and_persists(client, customer):
    response = client.put("/api/settings", json={"theme": "DARK"}, headers=customer.headers)
    assert response.status_code == 200
    assert response.json()["theme"] == "DARK"
    assert response.json()["notification_enabled"] is True
    again = client.get("/api/settings", headers=customer.headers).json()
    assert again["theme"] == "DARK"


def test_notifications_can_be_turned_off_and_on(client, customer):
    off = client.put("/api/settings", json={"notification_enabled": False}, headers=customer.headers)
    assert off.json()["notification_enabled"] is False
    on = client.put("/api/settings", json={"notification_enabled": True}, headers=customer.headers)
    assert on.json()["notification_enabled"] is True


def test_fcm_token_can_be_registered_and_cleared_with_null(client, customer):
    set_token = client.put("/api/settings", json={"fcm_token": "  token-abc-123  "}, headers=customer.headers)
    assert set_token.json()["fcm_token"] == "token-abc-123"
    cleared = client.put("/api/settings", json={"fcm_token": None}, headers=customer.headers)
    assert cleared.json()["fcm_token"] is None
    assert client.get("/api/settings", headers=customer.headers).json()["fcm_token"] is None


def test_omitting_fcm_token_does_not_clear_an_existing_token(client, customer):
    client.put("/api/settings", json={"fcm_token": "keep-me"}, headers=customer.headers)
    client.put("/api/settings", json={"theme": "LIGHT"}, headers=customer.headers)
    assert client.get("/api/settings", headers=customer.headers).json()["fcm_token"] == "keep-me"


def test_invalid_theme_returns_400(client, customer):
    response = client.put("/api/settings", json={"theme": "PURPLE"}, headers=customer.headers)
    assert response.status_code == 400
    assert isinstance(response.json()["detail"], str)


def test_null_theme_or_notification_flag_returns_400(client, customer):
    assert client.put("/api/settings", json={"theme": None}, headers=customer.headers).status_code == 400
    assert client.put("/api/settings", json={"notification_enabled": None}, headers=customer.headers).status_code == 400


def test_settings_are_private_to_each_user(client, customer, business):
    client.put("/api/settings", json={"theme": "DARK", "fcm_token": "customer-token"}, headers=customer.headers)
    other = client.get("/api/settings", headers=business.headers).json()
    assert other["theme"] == "SYSTEM"
    assert other["fcm_token"] is None


def test_settings_require_authentication(client):
    assert client.get("/api/settings").status_code == 401
    assert client.put("/api/settings", json={"theme": "DARK"}).status_code == 401