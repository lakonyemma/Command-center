import json
import os
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request as StarletteRequest
from starlette.responses import JSONResponse

from backend.main import app

EXPECTED_GOOGLE_ACCOUNT = os.getenv("REVENUE_GOOGLE_ACCOUNT", "lakonyemmanuel92@gmail.com").strip().lower()
SERVER_API_KEY = os.getenv("REVENUE_API_KEY", "")


def google_email_for_token(token: str) -> str:
    request = Request(
        "https://gmail.googleapis.com/gmail/v1/users/me/profile",
        headers={"Authorization": f"Bearer {token}", "Accept": "application/json"},
    )
    try:
        with urlopen(request, timeout=10) as response:
            payload = json.loads(response.read().decode("utf-8"))
            return str(payload.get("emailAddress", "")).strip().lower()
    except (HTTPError, URLError, TimeoutError, ValueError):
        return ""


class GoogleRevenueAuthMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: StarletteRequest, call_next):
        if not request.url.path.startswith("/v1/"):
            return await call_next(request)

        if request.headers.get("x-api-key"):
            return await call_next(request)

        authorization = request.headers.get("authorization", "")
        if not authorization.lower().startswith("bearer "):
            return JSONResponse({"detail": "Google authorization required"}, status_code=401)

        token = authorization.split(" ", 1)[1].strip()
        email = google_email_for_token(token)
        if not email or email != EXPECTED_GOOGLE_ACCOUNT:
            return JSONResponse({"detail": "Google account is not authorized for Revenue OS"}, status_code=403)
        if not SERVER_API_KEY:
            return JSONResponse({"detail": "Revenue API key is not configured"}, status_code=503)

        headers = list(request.scope.get("headers", []))
        headers.append((b"x-api-key", SERVER_API_KEY.encode("utf-8")))
        request.scope["headers"] = headers
        return await call_next(request)


app.add_middleware(GoogleRevenueAuthMiddleware)
