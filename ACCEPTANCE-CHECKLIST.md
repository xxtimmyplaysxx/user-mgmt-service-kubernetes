# Abgabekontrolle: Observability und Microservices

Diese Checkliste beschreibt die fertige Abgabe für **Aufgaben 1–6 der Prüfung 3**.
Code und Anwendungsdokumentation liegen in diesem Repository; Infrastruktur,
Betriebsanleitungen und datierte Testnachweise im
[Ops-Repository](https://github.com/xxtimmyplaysxx/user-mgmt-ops).

## Aufgabe 1: Observability

- [x] kube-prometheus-stack installiert; Helm-Werte versioniert.
- [x] CPU-/RAM-/HPA-Dashboard und zwei RED-Dashboards mit echten Daten.
- [x] Metrik-Endpunkte und ServiceMonitors für User- und Module-Service.
- [x] Alarm und Entwarnung über Alertmanager an den internen Webhook zugestellt.

[Monitoring](https://github.com/xxtimmyplaysxx/user-mgmt-ops/blob/main/evidence/monitoring-installation.md),
[Alarmzustellung](https://github.com/xxtimmyplaysxx/user-mgmt-ops/blob/main/evidence/alert-delivery.md).

## Aufgabe 2: Lasttest und Skalierung

- [x] k6-Test mit echtem Login-Endpunkt und bis zu 20 virtuellen Benutzern.
- [x] 3282 erfolgreiche Logins, keine HTTP-Fehler, P95 1.06 s.
- [x] HPA 1 → 2 → 1, durchgehend verfügbares Backend, keine Neustarts im bestandenen Lauf.
- [x] Messkurven, Rohdaten und Auswertung in Git.

[Lasttestnachweis](https://github.com/xxtimmyplaysxx/user-mgmt-ops/blob/main/evidence/loadtest-passed.md).

## Aufgabe 3: Infrastructure as Code

- [x] Bestehenden DigitalOcean-Kubernetes-Cluster in Terraform importiert.
- [x] Generierte Konfiguration bereinigt und parametrisiert.
- [x] Formatierung, Validierung und abschliessender Plan ohne Änderungen geprüft.
- [x] State, Zugangsdaten und Plan-Dateien vom Repository ausgeschlossen.

[Terraform-Dokumentation](https://github.com/xxtimmyplaysxx/user-mgmt-ops/blob/main/terraform/README.md).

## Aufgabe 4: Managed PostgreSQL

- [x] Managed-Datenbank und Firewall mit Terraform erstellt.
- [x] Aktuelles Backup bei gestoppten Schreibzugriffen migriert und Daten verglichen.
- [x] Anwendung auf Managed PostgreSQL umgestellt; TLS und Funktion geprüft.
- [x] Alte Staging-Datenbank samt Service, PVC und Cloud-Volume entfernt.
- [x] Planerstatistiken nach dem Restore mit ANALYZE aktualisiert.

[Migration](https://github.com/xxtimmyplaysxx/user-mgmt-ops/blob/main/evidence/postgres-final-copy.md),
[Betriebsanleitung](https://github.com/xxtimmyplaysxx/user-mgmt-ops/blob/main/evidence/postgres-cutover-runbook.md).

## Aufgabe 5: Policy as Code

- [x] Kyverno installiert und Konfiguration versioniert.
- [x] Drei Enforce-Policies für Ressourcen, Non-root-Betrieb und versionierte Images.
- [x] Ungültiges Deployment abgelehnt; zwölf Admission-Gegenproben bestanden.

[Policy-Nachweis](https://github.com/xxtimmyplaysxx/user-mgmt-ops/blob/main/evidence/kyverno-admission.md).

## Aufgabe 6: Microservices

- [x] Module-Service des Lehrers integriert; Herkunft dokumentiert.
- [x] Module und Zuweisungen in Managed MySQL mit geprüfter TLS-Verbindung.
- [x] Authentifizierte Modulzuweisung über HTTP mit Existenz- und Berechtigungsprüfung.
- [x] Idempotente Zuweisung, Timeout, begrenzte Retries und Circuit Breaker.
- [x] User-Service verwendet keine direkte MySQL-Verbindung.
- [x] Java-/Python-Tests, sechs Live-E2E-Fälle sowie Ausfall und Erholung geprüft.
- [x] Metriken für beide Dienste; drei Images mit Commit-SHA-Tags über CI/GitOps ausgerollt.

[Anwendungsdokumentation](OBSERVABILITY.md),
[Ausfalltest](https://github.com/xxtimmyplaysxx/user-mgmt-ops/blob/main/evidence/module-resilience.md).

Die Nachweise enthalten Testzeitpunkte, geprüfte Images und Grenzen des Testumfangs.
Sie ersetzen keine allgemeine Verfügbarkeits- oder Kapazitätsgarantie.
