"""Application errors. Services raise these; error_handlers.py turns them into JSON."""


class AppError(Exception):
    status_code = 500
    default_detail = "Unexpected error"

    def __init__(self, detail: str | None = None, headers: dict[str, str] | None = None):
        self.detail = detail or self.default_detail
        self.headers = headers
        super().__init__(self.detail)


class BadRequestError(AppError):
    status_code = 400
    default_detail = "Bad request"


class UnauthorizedError(AppError):
    status_code = 401
    default_detail = "Authentication required"


class ForbiddenError(AppError):
    status_code = 403
    default_detail = "You do not have permission to do this"


class NotFoundError(AppError):
    status_code = 404
    default_detail = "Not found"


class ConflictError(AppError):
    status_code = 409
    default_detail = "Conflict"


class ServiceUnavailableError(AppError):
    status_code = 503
    default_detail = "Service unavailable"