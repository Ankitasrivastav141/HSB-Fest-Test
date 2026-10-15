import os
import shutil
import sqlite3
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent

# Read .env if present
env_file = BASE_DIR / ".env"
if env_file.exists():
    with open(env_file, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                k, v = line.split("=", 1)
                os.environ.setdefault(k.strip(), v.strip().strip("'\""))

DATABASE_URL = os.environ.get("DATABASE_URL") or os.environ.get("POSTGRES_URL")
if DATABASE_URL and DATABASE_URL.startswith("postgres://"):
    DATABASE_URL = DATABASE_URL.replace("postgres://", "postgresql://", 1)

try:
    import psycopg2
    import psycopg2.extras
    PSYCOPG2_AVAILABLE = True
except ImportError:
    PSYCOPG2_AVAILABLE = False


class PostgresCursorWrapper:
    """Wraps a psycopg2 cursor to provide sqlite3-compatible query interface."""
    def __init__(self, cursor):
        self._cursor = cursor
        self.lastrowid = None

    def execute(self, query, params=None):
        pg_query = query.replace("?", "%s")
        # If insert and doesn't specify RETURNING id, auto-append to capture lastrowid
        is_insert = pg_query.strip().upper().startswith("INSERT INTO")
        if is_insert and "RETURNING" not in pg_query.upper():
            pg_query = pg_query.rstrip("; ") + " RETURNING id"
            if params is not None:
                self._cursor.execute(pg_query, params)
            else:
                self._cursor.execute(pg_query)
            try:
                row = self._cursor.fetchone()
                if row:
                    self.lastrowid = row[0]
            except Exception:
                self.lastrowid = None
            return self

        if params is not None:
            self._cursor.execute(pg_query, params)
        else:
            self._cursor.execute(pg_query)
        return self

    def fetchone(self):
        return self._cursor.fetchone()

    def fetchall(self):
        return self._cursor.fetchall()

    def fetchmany(self, size=None):
        return self._cursor.fetchmany(size) if size is not None else self._cursor.fetchmany()

    @property
    def rowcount(self):
        return self._cursor.rowcount

    def close(self):
        self._cursor.close()

    def __iter__(self):
        return iter(self._cursor)


class PostgresConnectionWrapper:
    """Wraps a psycopg2 connection to provide sqlite3-compatible interface."""
    def __init__(self, conn):
        self._conn = conn

    def cursor(self):
        raw_cursor = self._conn.cursor(cursor_factory=psycopg2.extras.DictCursor)
        return PostgresCursorWrapper(raw_cursor)

    def execute(self, query, params=None):
        cur = self.cursor()
        cur.execute(query, params)
        return cur

    def commit(self):
        self._conn.commit()

    def rollback(self):
        self._conn.rollback()

    def close(self):
        self._conn.close()


# SQLite fallback path
if os.environ.get("VERCEL"):
    DB_PATH = Path("/tmp/fest.db")
else:
    DB_PATH = BASE_DIR / "fest.db"


def is_postgres():
    return bool(DATABASE_URL and PSYCOPG2_AVAILABLE)


def get_db_connection():
    """
    Returns a database connection.
    If DATABASE_URL is set and psycopg2 is installed, connects to PostgreSQL.
    Otherwise, connects to SQLite with sqlite3.Row factory.
    """
    if is_postgres():
        conn = psycopg2.connect(DATABASE_URL)
        return PostgresConnectionWrapper(conn)

    # SQLite fallback
    if os.environ.get("VERCEL") and not DB_PATH.exists():
        init_db()
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    return conn


def init_db():
    """Initializes the database schema if tables do not exist."""
    if is_postgres():
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS events (
                id SERIAL PRIMARY KEY,
                title VARCHAR(255) NOT NULL,
                category VARCHAR(100) NOT NULL,
                description TEXT,
                event_date VARCHAR(50) NOT NULL,
                event_time VARCHAR(50) NOT NULL,
                venue VARCHAR(255) NOT NULL,
                capacity INTEGER NOT NULL,
                fee INTEGER NOT NULL DEFAULT 0,
                icon VARCHAR(50)
            );
        """)
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS registrations (
                id SERIAL PRIMARY KEY,
                event_id INTEGER NOT NULL REFERENCES events(id) ON DELETE CASCADE,
                student_name VARCHAR(255) NOT NULL,
                roll_number VARCHAR(100) NOT NULL,
                email VARCHAR(255) NOT NULL,
                phone VARCHAR(50),
                registered_at VARCHAR(100) NOT NULL,
                UNIQUE(event_id, email)
            );
        """)
        conn.commit()
        conn.close()
        return

    # SQLite
    if os.environ.get("VERCEL") and not DB_PATH.exists():
        src_db = BASE_DIR / "fest.db"
        if src_db.exists():
            try:
                shutil.copy2(src_db, DB_PATH)
                return
            except Exception as e:
                print(f"Warning: Failed to copy fest.db to /tmp: {e}")

    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS events (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT NOT NULL,
            category TEXT NOT NULL,
            description TEXT,
            event_date TEXT NOT NULL,
            event_time TEXT NOT NULL,
            venue TEXT NOT NULL,
            capacity INTEGER NOT NULL,
            fee INTEGER NOT NULL DEFAULT 0,
            icon TEXT
        )
    """)
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS registrations (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            event_id INTEGER NOT NULL REFERENCES events(id),
            student_name TEXT NOT NULL,
            roll_number TEXT NOT NULL,
            email TEXT NOT NULL,
            phone TEXT,
            registered_at TEXT NOT NULL,
            UNIQUE(event_id, email)
        )
    """)
    conn.commit()
    conn.close()


if __name__ == "__main__":
    init_db()
    if is_postgres():
        print("PostgreSQL Database initialized successfully.")
    else:
        print(f"SQLite Database initialized successfully at {DB_PATH}")
