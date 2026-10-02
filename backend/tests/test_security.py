"""Password hashing, JWT handling and validation helpers."""
from datetime import datetime, timedelta, timezone

import jwt as pyjwt
import pytest

from app.auth.hashing import hash_password, verify_password
from app.auth.jwt import TokenError, create_access_token, decode_access_token
from app.config import settings
from app.utils.validation import normalize_phone, validate_password_strength


def test_hash_is_bcrypt_and_never_contains_the_plain_password():
    hashed = hash_password("Passw0rd123")
    assert hashed != "Passw0rd123"
    assert "Passw0rd123" not in hashed
    assert hashed.startswith("$2b$")


def test_same_password_produces_different_hashes_because_of_salting():
    assert hash_password("Passw0rd123") != hash_password("Passw0rd123")


def test_verify_password_accepts_the_right_password_and_rejects_a_wrong_one():
    hashed = hash_password("Passw0rd123")
    assert verify_password("Passw0rd123", hashed) is True
    assert verify_password("Passw0rd124", hashed) is False


def test_verify_password_returns_false_for_a_malformed_hash():
    assert verify_password("Passw0rd123", "not-a-bcrypt-hash") is False


def test_passwords_longer_than_bcrypt_limit_do_not_crash():
    hashed = hash_password("a1" * 60)
    assert verify_password("a1" * 60, hashed) is True


def test_access_token_round_trip_keeps_the_user_id():
    payload = decode_access_token(create_access_token(42))
    assert payload["sub"] == "42"
    assert payload["exp"] > payload["iat"]


def test_expired_token_is_rejected():
    token = create_access_token(1, expires_delta=timedelta(seconds=-30))
    with pytest.raises(TokenError, match="expired"):
        decode_access_token(token)


def test_token_signed_with_a_different_secret_is_rejected():
    forged = pyjwt.encode(
        {"sub": "1", "exp": datetime.now(timezone.utc) + timedelta(hours=1)},
        "x" * 40,
        algorithm="HS256",
    )
    with pytest.raises(TokenError):
        decode_access_token(forged)


def test_token_with_swapped_payload_is_rejected():
    header, _payload, signature = create_access_token(1).split(".")
    other_payload = create_access_token(2).split(".")[1]
    with pytest.raises(TokenError):
        decode_access_token(f"{header}.{other_payload}.{signature}")


def test_unsigned_token_with_alg_none_is_rejected():
    unsigned = pyjwt.encode(
        {"sub": "1", "exp": datetime.now(timezone.utc) + timedelta(hours=1)}, None, algorithm="none"
    )
    with pytest.raises(TokenError):
        decode_access_token(unsigned)


def test_token_without_expiry_is_rejected():
    no_exp = pyjwt.encode({"sub": "1"}, settings.jwt_secret, algorithm=settings.jwt_algorithm)
    with pytest.raises(TokenError):
        decode_access_token(no_exp)


@pytest.mark.parametrize("password", ["Passw0rd123", "abcdefg1", "1234567a", "A" * 63 + "1"])
def test_strong_passwords_are_accepted(password):
    assert validate_password_strength(password) == password


@pytest.mark.parametrize(
    "password, fragment",
    [
        ("short1", "at least 8"),
        ("onlyletters", "digit"),
        ("12345678", "letter"),
        ("a1" * 40, "at most 64"),
    ],
)
def test_weak_passwords_are_rejected_with_a_helpful_message(password, fragment):
    with pytest.raises(ValueError, match=fragment):
        validate_password_strength(password)


def test_phone_numbers_are_normalised_and_validated():
    assert normalize_phone("+27 82 123 4567") == "+27821234567"
    assert normalize_phone("082-123-4567") == "0821234567"
    assert normalize_phone("  ") is None
    assert normalize_phone(None) is None
    with pytest.raises(ValueError):
        normalize_phone("abc")
    with pytest.raises(ValueError):
        normalize_phone("12345")