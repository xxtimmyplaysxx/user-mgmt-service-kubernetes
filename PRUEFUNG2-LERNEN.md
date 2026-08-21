# Prüfung 2: Kubernetes-Orchestrierung einfach erklärt

## Sicherheitsstatus

- Prüfung 1 bleibt im ursprünglichen Repository unverändert.
- Die Prüfung-2-Arbeit liegt nur in dieser lokalen Kopie.
- Der Push dieses lokalen Repositories ist technisch gesperrt.
- Es wurde keine DigitalOcean-Ressource erstellt oder verändert.
- HPA skaliert nur Pods; ein DigitalOcean-Node-Autoscaler wird nicht aktiviert.

## Architektur

Der Browser erreicht nur den Ingress Controller. Dieser verteilt `/` an das
Frontend und `/api` an das Backend. Das Backend erreicht PostgreSQL über den
internen Service-Namen. PostgreSQL ist nicht öffentlich erreichbar.

```text
Internet -> Ingress -> Frontend Pods
                  \-> Backend Pods -> PostgreSQL Service -> PostgreSQL + PVC
```

## Aufgabe 1: Kubernetes-Manifeste

- Ein **Pod** ist die kleinste laufende Einheit in Kubernetes.
- Ein **Deployment** hält die gewünschte Anzahl Pods am Leben und führt Updates aus.
- Ein **Service** gibt wechselnden Pods eine stabile interne Adresse.
- Eine **ConfigMap** enthält nicht geheime Konfiguration.
- Ein **Secret** enthält sensible Konfiguration.
- Ein **PersistentVolumeClaim (PVC)** fordert dauerhaften Speicher an.
- Ein **Ingress** ist der kontrollierte Eingang aus dem Internet.

Die statischen Lernmanifeste liegen unter `k8s/`. Für die endgültige Bereitstellung
wird der Helm Chart verwendet.

## Aufgabe 2: Helm

Helm ist ein Paketmanager und eine Vorlagen-Engine für Kubernetes. Die Dateien
unter `templates/` enthalten Platzhalter. `values.yaml`, `values-staging.yaml`
und `values-prod.yaml` liefern die konkreten Werte. `_helpers.tpl` definiert
wiederverwendbare Namen und Labels.

Prüfbefehle:

```powershell
helm lint .\ops\charts\user-mgmt
helm template user-mgmt .\ops\charts\user-mgmt `
  --namespace user-mgmt-staging `
  -f .\ops\charts\user-mgmt\values-staging.yaml
```

## Aufgabe 3: Argo CD und GitOps

Argo CD vergleicht laufend zwei Zustände:

1. Soll-Zustand im Ops-Git-Repository
2. Ist-Zustand im Kubernetes-Cluster

Bei einer Abweichung synchronisiert Argo CD den Cluster. `selfHeal` korrigiert
manuelle Änderungen im Cluster. `prune` entfernt Ressourcen, die aus Git entfernt
wurden. Das Dashboard kann sicher per `kubectl port-forward` erreicht werden.

## Aufgabe 4: Pipeline

Die neue Pipeline macht bei einem Push auf `main` Folgendes:

1. Backend- und Frontend-Image bauen
2. beide Images mit dem unveränderlichen Git-Commit-Hash taggen
3. Images nach GitHub Container Registry (GHCR) pushen
4. `values-staging.yaml` im Ops-Repository aktualisieren
5. Änderung im Ops-Repository committen
6. Argo CD erkennt den Commit und deployt ihn

Die Pipeline verbindet sich nicht mehr per SSH mit einem Server und führt kein
`docker compose` aus.

## Aufgabe 5: Namespaces

Staging und Production laufen in den Namespaces `user-mgmt-staging` und
`user-mgmt-production`. Jeder Namespace erhält:

- eigene Helm-Werte
- eigene Argo-CD-Application
- eine ResourceQuota als Kosten- und Ressourcenschutz
- NetworkPolicies zur Isolation

Namespaces sind eine logische Trennung. Sie sind keine getrennten Server.

## Aufgabe 6: horizontale Skalierung und Verfügbarkeit

- **requests** reservieren Ressourcen und bilden die HPA-Berechnungsgrundlage.
- **limits** begrenzen den maximalen Verbrauch eines Containers.
- **readinessProbe** entscheidet, ob ein Pod Traffic erhalten darf.
- **livenessProbe** erkennt einen festgefahrenen Prozess und startet ihn neu.
- **startupProbe** gibt langsam startenden Anwendungen genügend Zeit.
- **HPA** verändert die Anzahl Backend-Pods abhängig von CPU-Auslastung.
- **RollingUpdate** ersetzt Pods schrittweise ohne geplanten Unterbruch.
- **PDB** schützt eine Mindestzahl Pods bei freiwilligen Unterbrüchen, etwa Wartung.

Wichtig für die Prüfung: Ein PDB schützt nicht vor einem kompletten Serverausfall.
Echte Hochverfügbarkeit benötigt mehrere Worker Nodes in unterschiedlichen
Failure Domains. Ein Cluster mit nur einem Node kann die Konzepte zeigen, aber
keine echte Node-Hochverfügbarkeit garantieren.

## Kostenregeln für DigitalOcean

Vor der Cluster-Erstellung müssen Node-Anzahl, Node-Grösse und Zusatzressourcen
mit der Lehrperson bestätigt werden. Nicht ohne Freigabe aktivieren:

- Node Autoscaling
- zusätzliche Node Pools
- mehrere externe Load Balancer
- unnötig grosse Persistent Volumes
- automatische Backups oder weitere kostenpflichtige Add-ons

Nach dem Unterricht immer im DigitalOcean-Dashboard prüfen, welche Ressourcen
laufen. Ein Kubernetes Service vom Typ `LoadBalancer` kann eine zusätzliche
kostenpflichtige Cloud-Ressource erzeugen.

## Kurze mündliche Musterantwort

> Wir haben die Compose-Anwendung deklarativ auf Kubernetes migriert. Deployments
> verwalten die Pods, Services bieten stabile interne Adressen, ein Ingress nimmt
> externen Traffic an, und ein PVC speichert PostgreSQL-Daten dauerhaft. Helm macht
> die Konfiguration für Staging und Production wiederverwendbar. Die Pipeline baut
> unveränderlich getaggte Images und aktualisiert nur das Ops-Repository. Argo CD
> synchronisiert daraus den Cluster. Probes, Ressourcen, HPA, Rolling Updates und
> PDB verbessern Skalierbarkeit und Verfügbarkeit.
