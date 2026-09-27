# User Management Service

Abgabe für VSC, Unterrichtsblöcke 7–9: Observability und Microservices.

Die bestehende Benutzerverwaltung wurde um einen Module-Service erweitert.
Ein Benutzer kann sich anmelden und über die API einem Unterrichtsmodul zugewiesen werden.
Der User-Service speichert Benutzerdaten in PostgreSQL, der Module-Service seine Daten in MySQL.

## Aufbau

| Verzeichnis | Inhalt |
|---|---|
| [src/](src/) | User-Service mit Spring Boot, JWT-Anmeldung und Modulzuweisung |
| [frontend/](frontend/) | Weboberfläche mit Next.js |
| [module-service/](module-service/) | Python-Service für Unterrichtsmodule |
| [scripts/e2e.py](scripts/e2e.py) | Test der Modulzuweisung und Fehlerfälle |
| [.github/workflows/](.github/workflows/) | Tests, Image-Build und Übergabe an das Ops-Repository |

Der Module-Service basiert auf der [Vorlage aus dem Unterricht](https://github.com/yagan93/module_service).
Die übernommenen und ergänzten Teile stehen in [UPSTREAM.md](module-service/UPSTREAM.md).

## Dokumentation und Deployment

- [API, Kommunikation zwischen den Diensten und Tests](OBSERVABILITY.md)
- [Ops-Repository: Terraform, Helm, Argo CD, Monitoring und Policies](https://github.com/xxtimmyplaysxx/user-mgmt-ops)
- [Zuordnung der Aufgaben zu Konfigurationen und Testnachweisen](https://github.com/xxtimmyplaysxx/user-mgmt-ops/blob/main/evidence/STATUS.md)

Die Abgabe läuft im Namespace `user-mgmt-staging` auf DigitalOcean.
Die Pipeline baut Backend, Frontend und Module-Service mit dem Commit-SHA als Image-Tag.
Anschliessend aktualisiert sie die Staging-Konfiguration im Ops-Repository; Argo CD übernimmt das Deployment.

Die Verzeichnisse `k8s/` und `k8s-task6/` stammen aus der vorherigen Kubernetes-Aufgabe.
Für diese Abgabe gelten die Helm-Konfigurationen im Ops-Repository.

## Tests lokal ausführen

Voraussetzungen: Java 25, Python ab 3.12 und uv.

```powershell
.\gradlew.bat test --no-daemon
cd module-service
uv sync --frozen --extra dev
uv run pytest
```
