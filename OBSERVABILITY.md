# Modulzuweisung und Observability

## Kommunikation

```text
Client → User-Service → Module-Service → Managed MySQL
              ↓
       Managed PostgreSQL
```

Der User-Service greift für Benutzerdaten auf PostgreSQL zu.
Module und Zuweisungen verwaltet ausschliesslich der Module-Service in MySQL.
Der User-Service besitzt keine MySQL-Zugangsdaten.

## Modul zuweisen

`PUT /users/{userId}/modules/{moduleId}` benötigt ein gültiges JWT.
Über den Ingress lautet der Pfad `/api/users/{userId}/modules/{moduleId}`.

1. Die Berechtigungsprüfung erlaubt die eigene User-ID oder die Berechtigung `USER_MODIFY`.
2. Der User-Service prüft, ob der Benutzer existiert.
3. Er fragt das Modul synchron über `GET /api/v1/modules/{moduleId}` ab.
4. Existiert das Modul, erfolgt die Zuweisung über `PUT /api/v1/users/{userId}/modules/{moduleId}`.

Wiederholte Zuweisungen erzeugen keinen zweiten Eintrag.

| Fall | HTTP-Status |
|---|---|
| Zuweisung oder Wiederholung erfolgreich | 204 |
| Benutzer oder Modul nicht vorhanden | 404 |
| Ungültige UUID | 400 |
| Ohne Anmeldung oder ohne Berechtigung | 403 |
| Module-Service vorübergehend nicht erreichbar | 503 |
| Unerwartete Antwort des Module-Service, z. B. sonstiger 4xx-Status | 502 |

Implementierung: [Controller](src/main/java/com/example/jwt/domain/module/ModuleAssignmentController.java)
und [REST-Client](src/main/java/com/example/jwt/domain/module/ModuleClient.java).

## Verhalten bei Ausfällen

Der HTTP-Client hat einen Verbindungs-Timeout von einer Sekunde und einen
Request-Timeout von zwei Sekunden. Bei I/O-Fehlern, HTTP 429 oder 5xx versucht er
den Aufruf höchstens dreimal, mit jeweils 200 ms Abstand. Andere Fehler wie 404
werden nicht wiederholt. Diese Begrenzung gilt jeweils für GET und PUT.

Der Circuit Breaker wertet die letzten fünf logischen Aufrufe aus. Nach mindestens
fünf Aufrufen öffnet er bei einer Fehlerrate von mindestens 50 %. Nach 15 Sekunden
lässt er zwei Probeaufrufe zu. Ein offener Circuit Breaker antwortet direkt mit 503.

Beim [Ausfalltest](https://github.com/xxtimmyplaysxx/user-mgmt-ops/blob/main/evidence/module-resilience.md)
wurde die Netzwerkverbindung zwischen den beiden Diensten kurz blockiert.
Die Zuweisung lieferte 503, während die Anmeldung weiter funktionierte.
Nach Wiederherstellung erholte sich die Zuweisung ohne Neustart.

## Metriken

| Dienst | Endpunkt | Metriken |
|---|---|---|
| User-Service | `/actuator/prometheus` | `http_server_requests_seconds_count` und Dauer-Histogramm |
| Module-Service | `/metrics` | `module_http_requests_total` und `module_http_request_duration_seconds` |

ServiceMonitors erfassen beide Endpunkte. Die Grafana-Dashboards zeigen Request
Rate, Response Time und Error Rate. Ein weiteres Dashboard zeigt CPU, RAM und HPA.
Die Konfiguration liegt im [Ops-Repository](https://github.com/xxtimmyplaysxx/user-mgmt-ops/tree/main/monitoring).

## Datenbank und Start des Module-Service

Ein Init-Container führt `python -m app.initialize` aus. Damit werden Tabellen und
die vier Unterrichtsmodule bei Bedarf angelegt. Mehrfaches Ausführen erzeugt keine
doppelten Module. Anwendung und Init-Container prüfen bei der MySQL-Verbindung
das CA-Zertifikat und den Hostnamen. Verbindungsdaten und CA kommen aus einem
Kubernetes Secret.

## Tests

Die Java-Tests prüfen unter anderem Retry, Timeout, Circuit Breaker, Berechtigungen
und Fehlerstatus. Die HTTP-Tests verwenden einen eingebetteten Server und eine
H2-Testdatenbank. Die Python-Tests prüfen die API, doppelte Modulcodes, idempotente
Zuweisungen und Metriken mit einer SQLite-Testdatenbank.

Für einen Test gegen das laufende Staging zuerst einen Port-Forward starten:

```powershell
kubectl --context do-fra1-vsc-orchestrierung -n user-mgmt-staging port-forward svc/user-mgmt-backend 18080:8080
```

In einem zweiten Terminal aus dem Application-Repository:

```powershell
python scripts/e2e.py --base-url http://127.0.0.1:18080
```

Das Skript registriert zwei Testbenutzer, meldet sie an und prüft sechs Fälle:
Zuweisung, Wiederholung, fehlendes Modul, ungültige ID, fehlende Anmeldung und
fremde User-ID. Die [Testergebnisse](https://github.com/xxtimmyplaysxx/user-mgmt-ops/blob/main/evidence/module-rollout.md)
und der [Test nach der PostgreSQL-Migration](https://github.com/xxtimmyplaysxx/user-mgmt-ops/blob/main/evidence/postgres-final-copy.md)
sind im Ops-Repository dokumentiert.

## CI/CD

Pull Requests führen Tests und Builds aus. Bei einem Push auf `main` veröffentlicht
GitHub Actions zusätzlich die drei Images in GHCR und trägt deren Commit-SHA-Tags
in die Staging-Werte des Ops-Repositories ein. Argo CD synchronisiert diese Werte.

Der Schreibzugriff auf das Ops-Repository erfolgt über den Deploy-Key im
Actions-Secret `OPS_DEPLOY_KEY`. Zugangsdaten werden nicht im Repository gespeichert.
