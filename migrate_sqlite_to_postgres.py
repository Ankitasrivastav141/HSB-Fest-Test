"""
Migration Script: SQLite -> PostgreSQL for Zenith '26
Usage:
  python migrate_sqlite_to_postgres.py <POSTGRES_DATABASE_URL>
or set DATABASE_URL in your environment / .env and run:
  python migrate_sqlite_to_postgres.py
"""

import sys
import os
import sqlite3
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
SQLITE_DB = BASE_DIR / "fest.db"

# Check arguments or environment
postgres_url = None
if len(sys.argv) > 1:
    postgres_url = sys.argv[1]
else:
    postgres_url = os.environ.get("DATABASE_URL") or os.environ.get("POSTGRES_URL")

if not postgres_url:
    # Try .env
    env_file = BASE_DIR / ".env"
    if env_file.exists():
        with open(env_file, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith("#") and "=" in line:
                    k, v = line.split("=", 1)
                    if k.strip() in ("DATABASE_URL", "POSTGRES_URL"):
                        postgres_url = v.strip().strip("'\"")

if not postgres_url:
    print("ERROR: No PostgreSQL connection URL provided.")
    print("Usage:")
    print("  python migrate_sqlite_to_postgres.py postgresql://user:password@host:port/dbname")
    sys.exit(1)

if postgres_url.startswith("postgres://"):
    postgres_url = postgres_url.replace("postgres://", "postgresql://", 1)

try:
    import psycopg2
    import psycopg2.extras
except ImportError:
    print("ERROR: psycopg2-binary is not installed. Please run: pip install psycopg2-binary")
    sys.exit(1)

print(f"Connecting to PostgreSQL...")
pg_conn = psycopg2.connect(postgres_url)
pg_cur = pg_conn.cursor()

print("Creating tables in PostgreSQL...")
pg_cur.execute("""
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

pg_cur.execute("""
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
pg_conn.commit()

# Read from SQLite
if not SQLITE_DB.exists():
    print(f"SQLite file '{SQLITE_DB}' not found, initializing seed data directly...")
    from backend.seed_data import seed_events
    os.environ["DATABASE_URL"] = postgres_url
    from backend.database import init_db
    init_db()
    seed_events()
    print("Seeded events directly into PostgreSQL!")
    sys.exit(0)

print(f"Reading data from SQLite database '{SQLITE_DB}'...")
sq_conn = sqlite3.connect(SQLITE_DB)
sq_conn.row_factory = sqlite3.Row
sq_cur = sq_conn.cursor()

# Migrate events
sq_cur.execute("SELECT id, title, category, description, event_date, event_time, venue, capacity, fee, icon FROM events")
events = sq_cur.fetchall()

print(f"Migrating {len(events)} events...")
for ev in events:
    pg_cur.execute("""
        INSERT INTO events (id, title, category, description, event_date, event_time, venue, capacity, fee, icon)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        ON CONFLICT (id) DO UPDATE SET
            title = EXCLUDED.title,
            category = EXCLUDED.category,
            description = EXCLUDED.description,
            event_date = EXCLUDED.event_date,
            event_time = EXCLUDED.event_time,
            venue = EXCLUDED.venue,
            capacity = EXCLUDED.capacity,
            fee = EXCLUDED.fee,
            icon = EXCLUDED.icon;
    """, (ev['id'], ev['title'], ev['category'], ev['description'], ev['event_date'], ev['event_time'], ev['venue'], ev['capacity'], ev['fee'], ev['icon']))

# Reset sequence
pg_cur.execute("SELECT setval(pg_get_serial_sequence('events', 'id'), COALESCE(MAX(id), 1)) FROM events;")

# Migrate registrations
sq_cur.execute("SELECT id, event_id, student_name, roll_number, email, phone, registered_at FROM registrations")
registrations = sq_cur.fetchall()

print(f"Migrating {len(registrations)} registrations...")
for reg in registrations:
    pg_cur.execute("""
        INSERT INTO registrations (id, event_id, student_name, roll_number, email, phone, registered_at)
        VALUES (%s, %s, %s, %s, %s, %s, %s)
        ON CONFLICT (event_id, email) DO NOTHING;
    """, (reg['id'], reg['event_id'], reg['student_name'], reg['roll_number'], reg['email'], reg['phone'], reg['registered_at']))

if registrations:
    pg_cur.execute("SELECT setval(pg_get_serial_sequence('registrations', 'id'), COALESCE(MAX(id), 1)) FROM registrations;")

pg_conn.commit()
sq_conn.close()
pg_conn.close()

print("=" * 50)
print("SUCCESS: Database migration to PostgreSQL complete!")
print("=" * 50)
