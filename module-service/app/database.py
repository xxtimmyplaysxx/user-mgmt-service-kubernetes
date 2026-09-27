from collections.abc import Generator

from sqlalchemy import create_engine
from sqlalchemy.orm import DeclarativeBase, Session, sessionmaker

from app.config import get_settings


class Base(DeclarativeBase):
    pass


def _connect_args() -> dict[str, object]:
    settings = get_settings()
    if settings.database_url.startswith("mysql"):
        if not settings.mysql_ssl_disabled:
            if not settings.mysql_ssl_ca:
                raise ValueError("MYSQL_SSL_CA is required when MySQL TLS is enabled")
            return {"ssl_ca": settings.mysql_ssl_ca, "ssl_verify_cert": True,
                    "ssl_verify_identity": True, "connect_timeout": 5,
                    "read_timeout": 5, "write_timeout": 5}
        return {"ssl_disabled": settings.mysql_ssl_disabled}
    if settings.database_url.startswith("sqlite"):
        return {"check_same_thread": False}
    return {}


settings = get_settings()
engine = create_engine(
    settings.database_url,
    connect_args=_connect_args(),
    pool_pre_ping=True,
    pool_recycle=300,
)
SessionLocal = sessionmaker(bind=engine, autoflush=False, expire_on_commit=False)


def get_db() -> Generator[Session, None, None]:
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
