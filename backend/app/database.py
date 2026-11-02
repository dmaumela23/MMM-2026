"""SQLAlchemy engine, session factory and the FastAPI database dependency."""
from collections.abc import Iterator

from sqlalchemy import create_engine
from sqlalchemy.orm import DeclarativeBase, Session, sessionmaker

from app.config import settings


class Base(DeclarativeBase):
    """Base class for all ORM models."""


# pool_pre_ping drops dead connections (e.g. after PostgreSQL restarts).
engine = create_engine(settings.database_url, pool_pre_ping=True)
SessionLocal = sessionmaker(bind=engine, class_=Session, autoflush=True, expire_on_commit=True)


def get_db() -> Iterator[Session]:
    """One session per request; always closed afterwards."""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def init_db() -> None:
    """Create all tables that do not exist yet (prototype: no Alembic migrations)."""
    import app.models  # noqa: F401  (importing registers every model on Base.metadata)

    Base.metadata.create_all(bind=engine)