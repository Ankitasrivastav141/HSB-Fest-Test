import sqlite3
import os
import shutil
from pathlib import Path

# fest.db is created in the project root folder (or /tmp on serverless hosts like Vercel)
BASE_DIR = Path(__file__).resolve().parent.parent

if os.environ.get("VERCEL"):
    DB_PATH = Path("/tmp/fest.db")
else:
    DB_PATH = BASE_DIR / "fest.db"

def get_db_connection():
    """Returns a SQLite connection with row_factory set to sqlite3.Row."""
    if os.environ.get("VERCEL") and not DB_PATH.exists():
        init_db()
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    return conn

def init_db():
    """Initializes the database schema if tables do not exist."""
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
    print(f"Database initialized successfully at {DB_PATH}")
