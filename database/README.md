# Database Seeding

This folder contains seed data for a fresh database setup.

## Structure

- `seeds/` — 10 SQL files, numbered by FK dependency order. Committed to git.
- `scripts/` — seed and export scripts for Windows and Linux.
- `export/` — gitignored output from export scripts. May contain sensitive data.

## Seeding (development)

Requirements: `mysql` CLI on PATH, DB running.

**Windows:**
```powershell
.\database\scripts\seed.ps1
```

**Linux/Mac:**
```sh
sh database/scripts/seed.sh
```

Override DB connection:
```sh
DB_HOST=localhost DB_USER=root DB_PASS=secret sh database/scripts/seed.sh
```

## Exporting current data

Exports current DB state to `database/export/`. Interactive prompts for optional tables.

**Windows:** `.\database\scripts\export.ps1`
**Linux/Mac:** `bash database/scripts/export.sh`

## Docker

On `docker compose up`, the `seed` sidecar automatically seeds the DB after the app reports healthy (tables created by Hibernate). Re-runs are safe — all seeds are idempotent.

If migrating from a previous Docker volume, run `docker compose down -v` first to reset.

## Test user

The "usuario prueba" account is created automatically by `DataInitializer.java` on first boot.
Credentials: `usuario prueba` / `JhfKHZ2%mJMI` — ADMIN role.
To change the password, update `DataInitializer.java` and rebuild.

## Populating live-data seed files (T-07 to T-13)

Seeds 04 through 10 are stubs that require data from the live DB. To populate them:

1. Run the export script from a machine with access to 192.168.100.93:
   ```sh
   bash database/scripts/export.sh
   # or on Windows:
   .\database\scripts\export.ps1
   ```
2. The export writes to `database/export/`. Review the output.
3. Copy each `database/export/<table>.sql` content into the corresponding `database/seeds/0N_<table>.sql`.
4. Replace all `INSERT INTO` with `INSERT IGNORE INTO` in the pasted content.
5. Commit the updated seed files.

## Warnings

- **Existing volume migration**: developers with an existing `mysql_data` volume populated by the old `/docker-entrypoint-initdb.d` mount must run `docker compose down -v` before `docker compose up` to get a clean seeded database.
- **Password change**: to change the admin password, update `DataInitializer.java` and then either `docker compose down -v` (volume reset) or manually `DELETE FROM usuario WHERE nombre='usuario prueba'` before restarting.
- **`database/export/` is gitignored**: files in that folder may contain sensitive configuration data and must not be committed. Only `database/export/.gitkeep` is tracked.
