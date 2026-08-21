# Akzeptanzcheckliste

## Aufgabe 1

- [x] Erreichbare Registry-Pfade für Backend und Frontend vorbereitet
- [x] Deployment und Service für Frontend, Backend und PostgreSQL
- [x] ConfigMap für nicht sensible Werte
- [x] Secret für sensible Werte (nur Demo-Platzhalter, vor Produktion ersetzen)
- [x] PostgreSQL PersistentVolumeClaim
- [x] Backend nutzt den internen PostgreSQL-Service
- [x] Ingress für Frontend und `/api`

## Aufgabe 2

- [x] Helm Chart erstellt
- [x] Kubernetes-Ressourcen templatisiert
- [x] Werte zentral in Values-Dateien
- [x] Wiederverwendbare Funktionen in `_helpers.tpl`
- [x] `helm lint` für Standard, Staging und Production fehlerfrei

## Aufgabe 3

- [ ] Separates GitHub-Ops-Repository erstellen (erst nach Freigabe)
- [ ] Argo CD im DigitalOcean-Cluster installieren
- [x] Zwei Argo-CD-Application-Manifeste vorbereitet
- [x] Getrennter Argo-CD-Namespace vorgesehen
- [x] Automatische Synchronisierung, Self-Heal und Prune konfiguriert
- [x] Sicherer Dashboard-Zugriff per Port-Forward dokumentiert

## Aufgabe 4

- [x] Pipeline startet bei Push auf `main`
- [x] Images werden mit Git-Commit-Hash getaggt
- [x] Veröffentlichung in GHCR vorbereitet
- [x] Automatische Aktualisierung des Staging-Image-Tags vorbereitet
- [x] SSH-, Compose- und imperative Deployment-Schritte entfernt
- [x] Benötigte Zugangsdaten als GitHub Secrets referenziert

## Aufgabe 5

- [x] `values-staging.yaml` und `values-prod.yaml`
- [x] Zwei getrennte Argo-CD-Applications und Namespaces
- [x] ResourceQuota pro Umgebung
- [x] NetworkPolicies für Default-Deny und erlaubte Kommunikationswege

## Aufgabe 6

- [x] HPA für Backend mit zwei bis fünf Replicas
- [x] Requests und Limits für alle Pods
- [x] Startup-, Liveness- und Readiness-Probes
- [x] Ingress verteilt nur auf Ready-Service-Endpunkte
- [x] RollingUpdate mit `maxUnavailable: 0`
- [x] PDB für Backend und Frontend

## Noch zwingend im echten Cluster zu testen

- [ ] Images in GHCR publizieren und Pull-Zugriff prüfen
- [ ] echte Domains in Values und Argo-CD-Ingress einsetzen
- [ ] Ingress Controller und Metrics Server installieren/prüfen
- [ ] beide Argo-CD-Applications synchron und healthy
- [ ] Registrierung und Login über das Frontend
- [ ] Neustart eines Pods und Erhalt der PostgreSQL-Daten
- [ ] Lasttest: HPA skaliert hoch und später wieder herunter
- [ ] Rolling Update ohne Unterbruch demonstrieren
- [ ] ResourceQuota und NetworkPolicies im Cluster nachweisen
