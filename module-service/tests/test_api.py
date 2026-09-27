import os
from uuid import uuid4

os.environ["DATABASE_URL"] = "sqlite:///./test-module.db"

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import select, func
from app.database import Base, engine, SessionLocal
from app.main import app
from app.models import UserModule


@pytest.fixture(autouse=True)
def schema():
    Base.metadata.drop_all(engine)
    Base.metadata.create_all(engine)
    yield
    Base.metadata.drop_all(engine)


client = TestClient(app)


def test_assignment_is_idempotent_and_missing_module_rejected():
    created = client.post("/api/v1/modules", json={"code": "VSC", "name": "Cloud"})
    assert created.status_code == 201
    module_id = created.json()["id"]
    url = f"/api/v1/users/{uuid4()}/modules/{module_id}"
    assert client.put(url).status_code == 204
    assert client.put(url).status_code == 204
    with SessionLocal() as db:
        assert db.scalar(select(func.count()).select_from(UserModule)) == 1
    assert client.put(f"/api/v1/users/{uuid4()}/modules/{uuid4()}").status_code == 404


def test_health_validation_and_metrics():
    assert client.get("/health/ready").status_code == 200
    assert client.get("/health/live").status_code == 200
    assert client.get("/api/v1/modules/not-a-uuid").status_code == 422
    client.get(f"/api/v1/modules/{uuid4()}")
    metrics = client.get("/metrics").text
    assert "module_http_requests_total" in metrics
    assert 'route="/api/v1/modules/{module_id}"' in metrics
    assert "module_http_request_duration_seconds_bucket" in metrics


def test_duplicate_code_conflict():
    data = {"code": "VSC", "name": "Cloud"}
    assert client.post("/api/v1/modules", json=data).status_code == 201
    assert client.post("/api/v1/modules", json=data).status_code == 409
