"""Password hashing with BCrypt.

Why hashing and not encryption? Encryption is reversible: whoever obtains the key can
read every password. A hash is one-way, and the server never needs the original password,
only whether a login attempt produces the same hash. BCrypt also adds a random salt per
password (identical passwords get different hashes) and a cost factor that makes brute
forcing slow.
"""
import bcrypt

from app.config import settings

_MAX_BCRYPT_BYTES = 72  # BCrypt only uses the first 72 bytes; newer bcrypt versions reject more


def _to_bytes(plain_password: str) -> bytes:
    return plain_password.encode("utf-8")[:_MAX_BCRYPT_BYTES]


def hash_password(plain_password: str) -> str:
    salt = bcrypt.gensalt(rounds=settings.bcrypt_rounds)
    return bcrypt.hashpw(_to_bytes(plain_password), salt).decode("utf-8")


def verify_password(plain_password: str, password_hash: str) -> bool:
    try:
        return bcrypt.checkpw(_to_bytes(plain_password), password_hash.encode("utf-8"))
    except ValueError:  # malformed hash
        return False