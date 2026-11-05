"""Authentication endpoints."""
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.auth.dependencies import get_current_user
from app.auth.jwt import create_access_token
from app.database import get_db
from app.models import User
from app.schemas.auth import AuthResponse, GoogleLoginRequest, LoginRequest, RegisterRequest
from app.schemas.common import error_responses
from app.schemas.user import UserOut
from app.services import auth_service

router = APIRouter(prefix="/api/auth", tags=["Authentication"])


@router.post(
    "/register",
    response_model=AuthResponse,
    status_code=201,
    summary="Register a new account",
    responses=error_responses(400, 409),
)
def register(payload: RegisterRequest, db: Session = Depends(get_db)):
    user = auth_service.register_user(db, payload)
    return AuthResponse(access_token=create_access_token(user.id), user=UserOut.model_validate(user))


@router.post(
    "/login",
    response_model=AuthResponse,
    summary="Log in and receive a JWT",
    responses=error_responses(400, 401),
)
def login(payload: LoginRequest, db: Session = Depends(get_db)):
    user = auth_service.authenticate_user(db, payload.email, payload.password)
    return AuthResponse(access_token=create_access_token(user.id), user=UserOut.model_validate(user))


@router.post(
    "/google",
    response_model=AuthResponse,
    summary="Single sign-on: exchange a Google ID token for an MMM JWT",
    description=(
        "The app sends the ID token it received from Google. The server verifies the token "
        "(signature, expiry, audience, verified email), signs in the MMM account with that email "
        "or creates one, and returns the normal MMM JWT. Responds 503 if Google sign-in is not "
        "configured or Google cannot be reached."
    ),
    responses=error_responses(400, 401),
)
def google_login(payload: GoogleLoginRequest, db: Session = Depends(get_db)):
    user = auth_service.authenticate_google(db, payload.id_token, payload.role)
    return AuthResponse(access_token=create_access_token(user.id), user=UserOut.model_validate(user))


@router.get(
    "/me",
    response_model=UserOut,
    summary="Get the logged-in user",
    responses=error_responses(401),
)
def me(current_user: User = Depends(get_current_user)):
    return UserOut.model_validate(current_user)
