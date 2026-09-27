# VSC: Observability and Microservices

This repository implements the user management application for VSC tasks 1 and 6.
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
controller delegation. A real embedded HTTP server also verifies that servlet
error dispatch preserves 400/404/502/503, while unauthenticated and other-user
assignments remain forbidden. Its H2 database is test-only. Python tests cover API validation, duplicate module codes,
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

## Verified deployment and submission

As of **27 September 2026, 17:49 Europe/Zurich**, the documented live acceptance
checks for VSC tasks 1-6 have passed. The tested application image tag is
`fd1e6343585fdf92ba437e929d23cabe1f894133`. Staging and Production report
Synced/Healthy in Argo CD. Staging uses Managed PostgreSQL and Managed MySQL with
verified TLS; the old staging PostgreSQL deployment and volume have been retired.

- Registration/login and all six live assignment/security E2E cases passed.
- The login load test passed: 3282 successful requests, zero HTTP errors, P95 1.06 s,
  HPA 1 -> 2 -> 1 and no backend restarts.
- A controlled backend-to-module network outage returned bounded HTTP 503 responses.
  The open circuit failed quickly without downstream requests; login still worked.
  Assignments recovered after the waiting period. Network policy and Argo auto-sync
  were restored to their original settings, without application restarts.
- The authenticated lookup was initially slow after migration. Updating the missing
  PostgreSQL planner statistics with ANALYZE reduced the five-request parallel
  assignment control from 10-second client timeouts to 0.115-0.251 s.

The initial deployment exposed an incorrect 403 for missing modules. The internal
servlet ERROR redispatch was being authenticated a second time. The security
configuration now permits that dispatcher while ordinary requests remain protected;
HTTP regression tests and the repeated live E2E passed. The initial pipeline's
invalid Ops credentials were replaced with the scoped SSH deploy key described above.

The submission consists of this Application repository and the
[Ops repository](https://github.com/xxtimmyplaysxx/user-mgmt-ops).
Infrastructure configuration, dated acceptance results and limitations are maintained
in its [status and evidence](https://github.com/xxtimmyplaysxx/user-mgmt-ops/blob/main/evidence/STATUS.md),
[resilience report](https://github.com/xxtimmyplaysxx/user-mgmt-ops/blob/main/evidence/module-resilience.md)
and [German oral-exam guide](https://github.com/xxtimmyplaysxx/user-mgmt-ops/blob/main/PRUEFUNGSVORBEREITUNG.md).
The teacher performs the final assessment and oral examination.
