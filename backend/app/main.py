"""MMM API entry point. Run with: uvicorn app.main:app --host 0.0.0.0 --port 8000"""
import logging
from contextlib import asynccontextmanager

from fastapi import Depends, FastAPI
from fastapi.openapi.utils import get_openapi
from sqlalchemy import text
from sqlalchemy.exc import SQLAlchemyError
from sqlalchemy.orm import Session

from app.database import SessionLocal, get_db, init_db
from app.error_handlers import VALIDATION_ERROR_STATUS, register_exception_handlers
from app.routers import auth as auth_router
from app.routers import energy as energy_router
from app.routers import orders as orders_router
from app.routers import products as products_router
from app.routers import services as services_router
from app.routers import settings as settings_router
from app.routers import users as users_router
from app.schemas.common import ErrorResponse, HealthResponse
from app.services import energy_service
from app.utils.errors import ServiceUnavailableError

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
logger = logging.getLogger("mmm")

API_TITLE = "MMM API"
API_VERSION = "1.0.0"
DESCRIPTION = """
REST API for **Maumela Magnum Management (MMM)**: products, services, orders and a
**simulated** energy module.

* Authenticate with `POST /api/auth/login`, copy `access_token`, click **Authorize** and paste it.
* Energy data is a **prototype simulation**. It is not connected to a real meter or electrical grid.
* No payments are processed: orders are recorded only.
* Errors always look like `{"detail": "Human-readable message"}`.
"""


@asynccontextmanager
async def lifespan(app: FastAPI):
    init_db()  # create tables that do not exist yet
    with SessionLocal() as db:
        energy_service.ensure_default_plans(db)
        db.commit()
    logger.info("MMM API started")
    yield


app = FastAPI(title=API_TITLE, version=API_VERSION, description=DESCRIPTION, lifespan=lifespan)
register_exception_handlers(app)

for router_module in (
    auth_router,
    users_router,
    settings_router,
    products_router,
    services_router,
    orders_router,
    energy_router,
):
    app.include_router(router_module.router)


@app.get(
    "/health",
    response_model=HealthResponse,
    tags=["Health"],
    summary="Check that the API and its database connection are working",
    responses={503: {"model": ErrorResponse, "description": "Database unreachable"}},
)
def health(db: Session = Depends(get_db)):
    try:
        db.execute(text("SELECT 1"))
    except SQLAlchemyError:
        logger.exception("Health check failed: database unreachable")
        raise ServiceUnavailableError("Database unavailable") from None
    return HealthResponse(status="ok", service=API_TITLE, database="ok")


def custom_openapi() -> dict:
    """Standard OpenAPI, minus FastAPI's default 422 entries (we return 400 instead)."""
    if app.openapi_schema:
        return app.openapi_schema
    schema = get_openapi(
        title=app.title, version=app.version, description=app.description, routes=app.routes
    )
    if VALIDATION_ERROR_STATUS != 422:
        for path_item in schema.get("paths", {}).values():
            for operation in path_item.values():
                if isinstance(operation, dict):
                    operation.get("responses", {}).pop("422", None)
        for name in ("HTTPValidationError", "ValidationError"):
            schema.get("components", {}).get("schemas", {}).pop(name, None)
    app.openapi_schema = schema
    return schema


app.openapi = custom_openapi