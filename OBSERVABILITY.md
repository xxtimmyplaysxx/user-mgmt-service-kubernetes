# VSC: Observability and Microservices

This branch prepares the user management application for VSC tasks 1 and 6.
Infrastructure, migration instructions and the honest acceptance-status table:
https://github.com/xxtimmyplaysxx/user-mgmt-ops

## Request flow

Client -> user-mgmt-backend -> user-mgmt-module -> Managed MySQL.
The backend continues to own its users in PostgreSQL. It has no MySQL credentials.

Authenticated `PUT /users/{userId}/modules/{moduleId}`:

1. Only the user themself or a caller with `USER_MODIFY` may assign a module.
2. The backend verifies the user exists (404 for a missing user).
3. A synchronous GET to `/api/v1/modules/{moduleId}` verifies the module exists.
4. A synchronous idempotent PUT delegates assignment to the Module Service.
5. Success returns 204. Missing module returns 404. Downstream failure returns 503.

Through the existing ingress the endpoint is `/api/users/{userId}/modules/{moduleId}`.
No frontend change is required by the acceptance criteria; the API is demonstrated
using `scripts/e2e.py`.

## Resilience

Java HttpClient: 1 second connect timeout, 2 second request timeout by default.
Retries: at most 3 attempts, 200 ms between attempts, only IO failures, 429 and 5xx.
404 and other non-transient errors are not retried. GET and PUT are idempotent.
Circuit breaker: 5 logical calls minimum, opens at >=50% failures, waits 15 seconds,
then allows 2 half-open probes. An open circuit returns 503 without downstream calls.
Each GET/PUT has a bounded retry budget; one assignment contains two calls.

## Metrics

- Spring: `/actuator/prometheus`, `http_server_requests_seconds_count` and histogram.
- Python: `/metrics`, `module_http_requests_total` and `module_http_request_duration_seconds`.
- Route templates avoid a different metrics label for each UUID.
- Health and metrics endpoints are excluded from the Python business metrics.
- Kubernetes NetworkPolicies and the existing ingress keep metrics off public routes.

## Module Service

The course implementation is included under `module-service/`, with provenance in
`UPSTREAM.md`. It retains the teacher's API, schema and seeded module IDs.
An init container runs `python -m app.initialize` to create/seed MySQL idempotently.
Both the init container and application verify the Managed MySQL CA and hostname.
`DATABASE_URL` and `ca.crt` come from a Kubernetes Secret.

## Local checks

```powershell
.\gradlew.bat test --no-daemon
cd module-service
uv sync --frozen --extra dev
uv run pytest
```

Java tests cover retry, circuit opening, timeout, missing module, missing user and
controller delegation. Python tests cover API validation, duplicate module codes,
idempotent assignments and metrics. These local tests do not replace live MySQL,
Kubernetes admission or end-to-end checks.

## GitOps

The existing workflow tests Java and Python, builds three images, publishes them to
GHCR using the commit SHA and updates staging image tags in the Ops repository.
Argo CD applies the reviewed Helm configuration. Pull requests build/test only;
they do not publish or promote. Main pushes can change the live staging application.

Ops promotion uses the encrypted Actions secret `OPS_DEPLOY_KEY`, an SSH deploy
key with write access only to `xxtimmyplaysxx/user-mgmt-ops`. The key's public half
is registered under that repository's Deploy keys; the private half is never
committed. The old `OPS_REPO_TOKEN` is no longer used. Checkout selects `main`
explicitly and keeps SSH host verification enabled.

On 27 September, main commit `6c932b34bb69f1d3af03ca5529b887874d9b6112` passed
Java/Python tests and published all three images. The final promotion failed with
`Bad credentials` from the old Ops token. A repository-scoped SSH deploy key was
then configured and verified with `git ls-remote` and a push dry-run. The revised
workflow still needs a successful main-branch promotion run.

Status: the owner released the earlier cluster-change freeze. Cluster import,
managed databases, backup restore test, and infrastructure monitoring are live;
module rollout, final PostgreSQL cutover, and live acceptance checks are pending.
The Ops repository's evidence/STATUS.md tracks the remaining checks.
