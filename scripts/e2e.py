"""After deployment: python scripts/e2e.py --base-url http://127.0.0.1:18080

Creates two explicitly named test users. No secrets are printed.
Use a local kubectl port-forward or HTTPS, never send real credentials over HTTP.
"""
import argparse
import json
import secrets
import uuid
from urllib.error import HTTPError
from urllib.request import Request, urlopen

parser = argparse.ArgumentParser()
parser.add_argument("--base-url", default="http://127.0.0.1:18080")
args = parser.parse_args()
base = args.base_url.rstrip("/")


def call(method, path, body=None, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = token
    data = json.dumps(body).encode() if body is not None else None
    try:
        with urlopen(Request(base + path, data, headers, method=method), timeout=15) as r:
            return r.status, r.headers, r.read()
    except HTTPError as error:
        return error.code, error.headers, error.read()


def register():
    email = f"vsc-e2e-{uuid.uuid4().hex}@example.com"
    password = secrets.token_urlsafe(24)
    status, _, body = call("POST", "/users/register", {
        "firstName": "VSC", "lastName": "E2E", "email": email, "password": password})
    assert status == 201, f"Registration returned {status}"
    user = json.loads(body)["id"]
    status, headers, _ = call("POST", "/users/login", {"email": email, "password": password})
    assert status == 200 and headers.get("Authorization"), f"Login returned {status}"
    return user, headers["Authorization"]


user, token = register()
other_user, _ = register()
module = "c02f58f2-3aca-4f1e-8076-bacf6f1999e6"
path = f"/users/{user}/modules/{module}"
cases = [
    ("assignment", path, token, {204}),
    ("idempotent repeat", path, token, {204}),
    ("missing module", f"/users/{user}/modules/{uuid.uuid4()}", token, {404}),
    ("invalid module ID", f"/users/{user}/modules/invalid", token, {400}),
    ("unauthenticated", path, None, {401, 403}),
    ("another user's assignment", f"/users/{other_user}/modules/{module}", token, {403}),
]
for label, endpoint, auth, expected in cases:
    status, _, _ = call("PUT", endpoint, token=auth)
    assert status in expected, f"{label}: expected {expected}, got {status}"
    print(f"PASS {label}: HTTP {status}")
