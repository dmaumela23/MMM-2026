"""Google single sign-on. Google itself is replaced by a fake verifier, so these tests need no network."""
import pytest
from google.auth.exceptions import TransportError
from sqlalchemy import func, select

from app.config import settings
from app.database import SessionLocal
from app.models import EnergyUsage, User, UserSettings
from app.services import auth_service

CLIENT_ID = "test-client.apps.googleusercontent.com"
GOOD_TOKEN = "good-token-0123456789-abcdefghij"
BAD_TOKEN = "bad-token-0123456789-abcdefghij"
PASSWORD = "Passw0rd123"


def make_claims(**overrides) -> dict:
    claims = {
        "iss": "https://accounts.google.com",
        "aud": CLIENT_ID,
        "sub": "1122334455",
        "email": "gina.google@example.com",
        "email_verified": True,
        "name": "Gina Google",
        "picture": "https://example.com/gina.png",
    }
    claims.update(overrides)
    return claims


@pytest.fixture(autouse=True)
def google_is_configured(monkeypatch):
    monkeypatch.setattr(settings, "google_client_id", CLIENT_ID)


@pytest.fixture
def google(monkeypatch):
    """Stands in for Google. Tokens starting with 'bad' are rejected the way Google's library rejects them."""
    state = {"claims": make_claims(), "error": None}

    def fake_verify(token, client_id):
        assert client_id == CLIENT_ID  # the server must check the token against OUR client id
        if state["error"] is not None:
            raise state["error"]
        if token.startswith("bad"):
            raise ValueError("Token used too late")
        return state["claims"]

    monkeypatch.setattr(auth_service, "_verify_google_token", fake_verify)
    return state


def sign_in(client, token=GOOD_TOKEN, **extra):
    return client.post("/api/auth/google", json={"id_token": token, **extra})


# ------------------------------------------------------------------ new accounts
def test_new_google_user_is_created_and_receives_an_mmm_jwt(client, google):
    response = sign_in(client)
    assert response.status_code == 200, response.text
    body = response.json()
    assert body["token_type"] == "bearer"
    assert body["user"]["email"] == "gina.google@example.com"
    assert body["user"]["full_name"] == "Gina Google"
    assert body["user"]["role"] == "CUSTOMER"
    assert body["user"]["avatar_url"] == "https://example.com/gina.png"

    me = client.get("/api/auth/me", headers={"Authorization": f"Bearer {body['access_token']}"})
    assert me.status_code == 200
    assert me.json()["id"] == body["user"]["id"]


def test_new_google_user_gets_settings_default_plan_and_simulated_energy_data(client, google):
    user_id = sign_in(client).json()["user"]["id"]
    with SessionLocal() as db:
        user = db.get(User, user_id)
        assert user.energy_plan_id is not None
        assert db.scalars(select(UserSettings).where(UserSettings.user_id == user_id)).one()
        usage = db.scalar(select(func.count()).select_from(EnergyUsage).where(EnergyUsage.user_id == user_id))
        assert usage == 90


def test_email_is_matched_in_lower_case_and_a_second_sign_in_reuses_the_account(client, google):
    google["claims"] = make_claims(email="Gina.Google@Example.COM")
    first = sign_in(client).json()["user"]
    second = sign_in(client).json()["user"]
    assert first["id"] == second["id"]
    assert first["email"] == "gina.google@example.com"
    with SessionLocal() as db:
        assert db.scalar(select(func.count()).select_from(User)) == 1


def test_missing_name_falls_back_to_the_email_prefix(client, google):
    google["claims"] = make_claims(name="")
    assert sign_in(client).json()["user"]["full_name"] == "gina.google"


def test_the_requested_role_is_used_only_when_a_new_account_is_created(client, google):
    assert sign_in(client, role="PROVIDER").json()["user"]["role"] == "PROVIDER"
    # A later request cannot promote the account: the existing account's role is kept.
    assert sign_in(client, role="BUSINESS").json()["user"]["role"] == "PROVIDER"


# ------------------------------------------------------------------ existing accounts
def test_existing_password_account_can_sign_in_with_google_and_keeps_its_password(client, google, make_user):
    existing = make_user("BUSINESS", email="gina.google@example.com")
    result = sign_in(client).json()["user"]
    assert result["id"] == existing.id
    assert result["role"] == "BUSINESS"
    login = client.post("/api/auth/login", json={"email": existing.email, "password": PASSWORD})
    assert login.status_code == 200


def test_google_account_cannot_be_opened_with_a_guessed_password(client, google):
    sign_in(client)
    for guess in (PASSWORD, "password", "google", "Passw0rd!"):
        response = client.post("/api/auth/login", json={"email": "gina.google@example.com", "password": guess})
        assert response.status_code == 401, guess


# ------------------------------------------------------------------ rejected tokens
def test_invalid_or_expired_token_is_rejected_with_a_generic_401(client, google):
    response = sign_in(client, token=BAD_TOKEN)
    assert response.status_code == 401
    assert response.json() == {"detail": "Google sign-in failed. Please try again."}
    assert "late" not in response.text  # the reason stays in the server log only


@pytest.mark.parametrize("claims", [{"email_verified": False}, {"email_verified": None}, {"email": ""}, {"email": None}])
def test_unverified_or_missing_email_is_rejected(client, google, claims):
    google["claims"] = make_claims(**claims)
    response = sign_in(client)
    assert response.status_code == 401
    assert "not verified" in response.json()["detail"]
    with SessionLocal() as db:
        assert db.scalar(select(func.count()).select_from(User)) == 0


def test_rejected_token_never_creates_an_account(client, google):
    sign_in(client, token=BAD_TOKEN)
    with SessionLocal() as db:
        assert db.scalar(select(func.count()).select_from(User)) == 0


# ------------------------------------------------------------------ configuration and availability
def test_returns_503_when_google_sign_in_is_not_configured(client, google, monkeypatch):
    monkeypatch.setattr(settings, "google_client_id", None)
    response = sign_in(client)
    assert response.status_code == 503
    assert response.json() == {"detail": "Google sign-in is not available right now."}


def test_returns_503_when_google_cannot_be_reached(client, google):
    google["error"] = TransportError("connection refused")
    response = sign_in(client)
    assert response.status_code == 503
    assert "connection refused" not in response.text


def test_request_validation(client, google):
    assert client.post("/api/auth/google", json={}).status_code == 400
    assert sign_in(client, token="short").status_code == 400
    assert sign_in(client, role="ADMIN").status_code == 400


def test_endpoint_is_documented_in_openapi(client):
    paths = client.get("/openapi.json").json()["paths"]
    assert "/api/auth/google" in paths
    assert "post" in paths["/api/auth/google"]
