"""Global exception handlers. Every error response has the shape {"detail": "..."}."""
import logging
from typing import Any

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

from app.utils.errors import AppError

logger = logging.getLogger("mmm.errors")

# The approved spec maps FastAPI's default 422 to 400. Change to 422 if your lecturer prefers it.
VALIDATION_ERROR_STATUS = 400

INTERNAL_ERROR_MESSAGE = "Something went wrong on our side. Please try again later."


def format_validation_errors(errors: list[dict[str, Any]]) -> str:
    """Turn Pydantic errors into one short human-readable sentence."""
    parts: list[str] = []
    for error in errors[:3]:
        location = [str(p) for p in error.get("loc", ()) if p not in ("body", "query", "path")]
        message = str(error.get("msg", "Invalid value")).removeprefix("Value error, ")
        field = ".".join(location)
        parts.append(f"{field}: {message}" if field else message)
    return "; ".join(parts) or "Invalid request"


def register_exception_handlers(app: FastAPI) -> None:
    @app.exception_handler(AppError)
    async def handle_app_error(request: Request, exc: AppError) -> JSONResponse:
        return JSONResponse(
            status_code=exc.status_code, content={"detail": exc.detail}, headers=exc.headers
        )

    @app.exception_handler(RequestValidationError)
    async def handle_validation_error(request: Request, exc: RequestValidationError) -> JSONResponse:
        return JSONResponse(
            status_code=VALIDATION_ERROR_STATUS,
            content={"detail": format_validation_errors(exc.errors())},
        )

    @app.exception_handler(StarletteHTTPException)
    async def handle_http_exception(request: Request, exc: StarletteHTTPException) -> JSONResponse:
        return JSONResponse(
            status_code=exc.status_code,
            content={"detail": str(exc.detail)},
            headers=getattr(exc, "headers", None),
        )

    @app.exception_handler(Exception)
    async def handle_unexpected_error(request: Request, exc: Exception) -> JSONResponse:
        # Full details go to the server log only; the client gets a generic message
        # so database internals or stack traces are never exposed.
        logger.exception("Unhandled error on %s %s", request.method, request.url.path)
        return JSONResponse(status_code=500, content={"detail": INTERNAL_ERROR_MESSAGE})
import logging
from typing import Any

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

from app.utils.errors import AppError

logger = logging.getLogger("mmm.errors")

# The approved spec maps FastAPI's default 422 to 400. Change to 422 if your lecturer prefers it.
VALIDATION_ERROR_STATUS = 400

INTERNAL_ERROR_MESSAGE = "Something went wrong on our side. Please try again later."


def format_validation_errors(errors: list[dict[str, Any]]) -> str:
    """Turn Pydantic errors into one short human-readable sentence."""
    parts: list[str] = []
    for error in errors[:3]:
        location = [str(p) for p in error.get("loc", ()) if p not in ("body", "query", "path")]
        message = str(error.get("msg", "Invalid value")).removeprefix("Value error, ")
        field = ".".join(location)
        parts.append(f"{field}: {message}" if field else message)
    return "; ".join(parts) or "Invalid request"


def register_exception_handlers(app: FastAPI) -> None:
    @app.exception_handler(AppError)
    async def handle_app_error(request: Request, exc: AppError) -> JSONResponse:
        return JSONResponse(
            status_code=exc.status_code, content={"detail": exc.detail}, headers=exc.headers
        )

    @app.exception_handler(RequestValidationError)
    async def handle_validation_error(request: Request, exc: RequestValidationError) -> JSONResponse:
        return JSONResponse(
            status_code=VALIDATION_ERROR_STATUS,
            content={"detail": format_validation_errors(exc.errors())},
        )

    @app.exception_handler(StarletteHTTPException)
    async def handle_http_exception(request: Request, exc: StarletteHTTPException) -> JSONResponse:
        return JSONResponse(
            status_code=exc.status_code,
            content={"detail": str(exc.detail)},
            headers=getattr(exc, "headers", None),
        )

    @app.exception_handler(Exception)
    async def handle_unexpected_error(request: Request, exc: Exception) -> JSONResponse:
        # Full details go to the server log only; the client gets a generic message
        # so database internals or stack traces are never exposed.
        logger.exception("Unhandled error on %s %s", request.method, request.url.path)
        return JSONResponse(status_code=500, content={"detail": INTERNAL_ERROR_MESSAGE})