# Module Service

Der Service verwaltet Module über eine REST-API und speichert sie in MySQL.

## API

| Methode | Pfad | Beschreibung | Status |
| --- | --- | --- | --- |
| `POST` | `/api/v1/modules` | Modul anlegen | `201` |
| `GET` | `/api/v1/modules` | Alle Module lesen | `200` |
| `GET` | `/api/v1/modules/{module_id}` | Modul lesen | `200` |
| `PATCH` | `/api/v1/modules/{module_id}` | Modul teilweise aktualisieren | `200` |
| `DELETE` | `/api/v1/modules/{module_id}` | Modul löschen | `204` |
| `PUT` | `/api/v1/users/{user_id}/modules/{module_id}` | Modul einem User zuweisen | `204` |

Ein Modul enthält folgende Felder:

```json
{
  "id": "c02f58f2-3aca-4f1e-8076-bacf6f1999e6",
  "code": "CLOUD-ARCH",
  "name": "Cloud Architecture",
  "description": "Designing reliable and scalable cloud systems",
  "created_at": "2026-09-16T09:02:09",
  "updated_at": "2026-09-16T09:02:09"
}
```

Die OpenAPI-Dokumentation ist unter `/docs` erreichbar.

Die Zuweisung ist idempotent: Wiederholte `PUT`-Requests für denselben User und dasselbe
Modul erzeugen nur einen Eintrag in `users_modules`. Die User-ID stammt aus dem
`user_mgmt_service`; der Module Service prüft nur, ob das angegebene Modul existiert.

## Lokal starten

Voraussetzungen: Python ab 3.12, uv und eine erreichbare MySQL-Datenbank `modules`.
In einer lokalen, nicht versionierten `.env` die eigenen Verbindungsdaten eintragen:

```dotenv
DATABASE_URL=mysql+pymysql://BENUTZER:PASSWORT@HOST:3306/modules
MYSQL_SSL_DISABLED=true
```

`MYSQL_SSL_DISABLED=true` gilt nur fuer eine lokale Testdatenbank. Fuer Managed MySQL
`MYSQL_SSL_DISABLED=false` setzen und mit `MYSQL_SSL_CA` den Pfad zur CA-Datei angeben.
Sonderzeichen im Passwort muessen in der URL kodiert werden.

Aus dem Verzeichnis `module-service/`:

```powershell
uv sync --frozen --extra dev
uv run python -m app.initialize
uv run uvicorn app.main:app --reload --port 8080
```

Im Kubernetes-Deployment uebernimmt der Init-Container die Initialisierung.
Herkunft und Anpassungen der Unterrichtsvorlage: [UPSTREAM.md](UPSTREAM.md).
