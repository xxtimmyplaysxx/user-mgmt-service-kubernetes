import logging
import time

from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import JSONResponse

from app.api import module_router, user_module_router
from app.config import get_settings
from app.database import engine
from sqlalchemy import text
from prometheus_client import Counter, Histogram, generate_latest, CONTENT_TYPE_LATEST
from fastapi.responses import Response

settings = get_settings()
logging.basicConfig(
    level=settings.log_level, format="%(asctime)s %(levelname)s %(name)s %(message)s"
)

app = FastAPI(
    title="Module Service",
    description="Manages modules.",
    version=settings.app_version,
)
app.include_router(module_router)
app.include_router(user_module_router)

REQUESTS = Counter("module_http_requests_total", "HTTP requests", ["method", "route", "status"])
DURATION = Histogram("module_http_request_duration_seconds", "HTTP duration", ["method", "route"])


@app.middleware("http")
async def observe(request: Request, call_next):
    start = time.perf_counter()
    status = 500
    try:
        response = await call_next(request)
        status = response.status_code
        return response
    finally:
        route = getattr(request.scope.get("route"), "path", "unmatched")
        if route not in ("/metrics", "/health/live", "/health/ready"):
            REQUESTS.labels(request.method, route, str(status)).inc()
            DURATION.labels(request.method, route).observe(time.perf_counter() - start)


@app.get("/metrics", include_in_schema=False)
def metrics():
    return Response(generate_latest(), media_type=CONTENT_TYPE_LATEST)


@app.get("/health/live", include_in_schema=False)
def live():
    return {"status": "UP"}


@app.get("/health/ready", include_in_schema=False)
def ready():
    try:
        with engine.connect() as connection:
            connection.execute(text("SELECT 1"))
        return {"status": "UP"}
    except Exception:
        return JSONResponse(status_code=503, content={"status": "DOWN"})

@app.exception_handler(HTTPException)
async def http_exception_handler(_: Request, exc: HTTPException) -> JSONResponse:
    detail = exc.detail
    if not isinstance(detail, dict) or "code" not in detail:
        detail = {"code": "HTTP_ERROR", "message": str(detail)}
    return JSONResponse(status_code=exc.status_code, content=detail, headers=exc.headers)
