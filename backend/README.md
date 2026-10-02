# MMM Backend (FastAPI + PostgreSQL)

REST API for Maumela Magnum Management. Energy data is a **simulation**; no payments are processed.

## Quick start
1. Create the databases (see "PostgreSQL setup" below).
2. `python -m venv .venv` then activate it.
3. `pip install -r requirements.txt`
4. `cp .env.example .env` and fill in DATABASE_URL, TEST_DATABASE_URL, JWT_SECRET, SEED_DEMO_PASSWORD.
5. `python seed.py`
6. `uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload`
7. Open http://localhost:8000/docs

## PostgreSQL setup
As a PostgreSQL superuser (for example `psql -U postgres`):

    CREATE ROLE mmm_user WITH LOGIN PASSWORD 'choose-a-strong-password';
    CREATE DATABASE mmm OWNER mmm_user;
    CREATE DATABASE mmm_test OWNER mmm_user;

## Tests
    pytest -v

Tests run only against a database whose name ends in `_test` and never touch `mmm`.

## Demo accounts (created by seed.py, password = SEED_DEMO_PASSWORD)
- demo.customer@example.com (CUSTOMER)
- demo.provider@example.com (PROVIDER)
- demo.business@example.com (BUSINESS)

## Security notes (prototype)
BCrypt password hashing, JWT (HS256, no refresh tokens or revocation), role and ownership checks,
parameterised SQL. No rate limiting, lockout or email verification. Use HTTPS in any real deployment.