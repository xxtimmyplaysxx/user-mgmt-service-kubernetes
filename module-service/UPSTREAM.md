# Herkunft des Module-Service

Vorlage aus dem Unterricht: [yagan93/module_service](https://github.com/yagan93/module_service)

Übernommener Commit: `ab09f1d8506f5c8a67593a7f26908408e0f6a5a3`

API, Datenbankschema und die IDs der Beispielmodule wurden beibehalten.
Für die Abgabe ergänzt wurden:

- Docker-Image und Einbindung in die bestehende CI-Pipeline
- Prometheus-Metriken sowie Liveness- und Readiness-Endpunkte
- MySQL-Verbindung mit Prüfung von CA-Zertifikat und Hostnamen
- Automatisierte Tests
- Idempotente Modulzuweisung auch bei gleichzeitigen Anfragen
