"""Idempotent course schema/seed initialization, run by the init container."""
from pathlib import Path
from app.database import engine


def main():
    sql = (Path(__file__).resolve().parents[1] / "schema.sql").read_text(encoding="utf-8")
    with engine.begin() as connection:
        for statement in sql.split(";"):
            if statement.strip():
                connection.exec_driver_sql(statement)
    print("Module schema and course examples initialized")


if __name__ == "__main__":
    main()
