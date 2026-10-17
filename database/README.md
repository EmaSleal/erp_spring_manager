# Database Seeding

This folder contains seed data for a fresh database setup.

## Structure

- `seeds/` — 10 SQL files, numbered by FK dependency order. Committed to git.
- `scripts/` — seed, export and backup scripts (`_common.sh` holds the shared connection rules).
- `export/` — gitignored output from export scripts. May contain sensitive data.

## Connection (all Linux/Mac scripts)

`export.sh`, `seed.sh` and `backup.sh` share `scripts/_common.sh`. There are **no defaults**
for host, user, password or database: set `DB_HOST`, `DB_USER`, `DB_PASS`, `DB_NAME`
(`DB_PORT` defaults to 3306) or pass `--env-file <path>`. If anything is missing the script
prints its usage and exits non-zero without contacting any server.

`--env-file` is parsed as `KEY=VALUE` lines (optional `export ` prefix and surrounding quotes);
it is **not** executed, and only the five `DB_*` keys above are read. The password is written
to a temporary file (`mktemp`, mode 600, deleted on exit/INT/TERM) passed to the client as the
first option `--defaults-extra-file`; it never appears on a command line or in the client's
environment (`MYSQL_PWD` is not used).

```sh
DB_HOST=127.0.0.1 DB_USER=app DB_NAME=facturas DB_PASS='...' bash database/scripts/backup.sh --out ~/backups
bash database/scripts/backup.sh --out ~/backups --env-file ~/secrets/db-prod.env
```

The Windows scripts `seed.ps1` and `export.ps1` were retired in T-021 (they had a default host, user and
password and used `MYSQL_PWD`); they now only print an error. On Windows, run the bash scripts from Git Bash
or WSL.

## Seeding (development) — `seed.sh`

Requirements: `mysql` CLI on PATH, DB running.

```sh
bash database/scripts/seed.sh --env-file ~/secrets/db-dev.env
```

- Prints `host:port/db` and asks you to type the database name to confirm.
- Non-interactive only with `--yes` **and** `CONFIRM_DB=<db name>` matching `DB_NAME`.
- Aborts if the database already has business data (`factura`, `asientos_contables`, or any
  `cliente` other than the generic one `DataInitializer` creates), unless `--force`.

**Windows:** run `seed.sh` from Git Bash or WSL (`seed.ps1` was retired, see above).

## Exporting reference data — `export.sh`

Exports current DB state to `database/export/`. Interactive prompts for optional tables.
`configuracion_hacienda` is **never** exported (it holds Hacienda credentials).

**Linux/Mac/Windows (Git Bash or WSL):** `bash database/scripts/export.sh --env-file <path>` (`export.ps1` was retired, see above).

## Full backup — `backup.sh`

```sh
bash database/scripts/backup.sh --out <dir> [--env-file <path>]
```

Runs `mysqldump --single-transaction --routines --triggers --events` (plus
`--set-gtid-purged=OFF` when the client supports it) of the whole database and writes
`<dir>/<db>_<YYYYMMDD_HHMMSS>.sql.gz` and `<...>.sql.gz.sha256` next to it. On any failure the
partial file is removed and the script exits non-zero.

Verify a backup: `(cd <dir> && sha256sum -c <db>_<stamp>.sql.gz.sha256)`.

### Restore

1. Verify the checksum (above).
2. Create the empty database:
   ```sh
   mysql --defaults-extra-file=<cnf> -e "CREATE DATABASE <db> CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
   ```
   `<cnf>` is a mode-600 file with a `[client]` section (`host`, `port`, `user`, `password`);
   `--defaults-extra-file` must be the first option.
3. Load the dump:
   ```sh
   gunzip -c <db>_<stamp>.sql.gz | mysql --defaults-extra-file=<cnf> <db>
   ```
4. Check that the routines came back:
   ```sql
   SHOW PROCEDURE STATUS WHERE Db = '<db>';
   SHOW FUNCTION STATUS WHERE Db = '<db>';
   ```
5. Start the application pointing `DB_URL` at the restored database, with the **same**
   `HACIENDA_CRYPTO_KEY` as before (see below). Flyway sees the restored history and only
   applies newer migrations.

### `HACIENDA_CRYPTO_KEY` is not in the backup

Hacienda credentials in `configuracion_hacienda` (password, certificate PIN, OAuth tokens) are
stored encrypted (AES-256-GCM, `enc:v1:` prefix). The key lives only in the `HACIENDA_CRYPTO_KEY`
environment variable and **must be backed up separately** (password manager / vault), never next
to the dumps.

- Since T-022 the same key also encrypts the SMTP password (`configuracion_email.smtp_password`).
  Plain-text values and the old `{enc}` format are re-encrypted to `enc:v1:` on startup
  (`SmtpPasswordCifradoRunner`); `{enc}` values need the old `SMTP_ENCRYPTION_KEY` on that start.
- Without the key the application refuses to start while `configuracion_hacienda` or
  `configuracion_email` has rows.
- If the key is lost the encrypted values cannot be recovered: generate a new key
  (`openssl rand -base64 32`) and re-enter the credentials. Because rows can no longer be
  decrypted, overwrite them directly so the app can start and re-encrypt them:
  ```sql
  UPDATE configuracion_hacienda
     SET password_hacienda = '<password>', pin_certificado = '<pin>',
         access_token = NULL, refresh_token = NULL, token_expira = NULL
   WHERE id = <id>;
  ```
  Plain values (without the `enc:v1:` prefix) are encrypted with the new key on the next start.

## Docker

`docker compose up` no longer seeds anything (T-022). The `seed` service only runs on demand, behind the
`seed` profile, and uses `scripts/seed.sh` with its safeguards (asks for the database name, aborts if the
database already has business data, password via `--defaults-extra-file`, never `MYSQL_PWD`):

```sh
docker compose --profile seed run --rm seed
```

If migrating from a previous Docker volume, run `docker compose down -v` first to reset.

## First administrator (bootstrap)

There is no built-in test user or fixed password (T-022, ADR-026). On startup, if there is **no active user
with role ADMIN**, `BootstrapAdminInitializer` creates one named "Administrador" from:

| Variable | Notes |
|----------|-------|
| `BOOTSTRAP_ADMIN_EMAIL` | Login name; must not belong to another user |
| `BOOTSTRAP_ADMIN_TELEFONO` | Must not belong to another user |
| `BOOTSTRAP_ADMIN_PASSWORD` | At least 12 characters; a password change is required on first login |

If they are missing (or the password is too short) and there is no active ADMIN, the application does not
start and names the missing variables. With an active ADMIN they are ignored and can be removed from `.env`.
The password is never logged; only `admin de bootstrap creado (id=N)`. Existing user rows are never
modified by code.

## Populating live-data seed files (T-07 to T-13)

Seeds 04 through 10 are stubs that require data from the live DB. To populate them:

1. Run the export script from a machine with access to the source database:
   ```sh
   bash database/scripts/export.sh --env-file <path>
   ```
2. The export writes to `database/export/`. Review the output.
3. Copy each `database/export/<table>.sql` content into the corresponding `database/seeds/0N_<table>.sql`.
4. Replace all `INSERT INTO` with `INSERT IGNORE INTO` in the pasted content.
5. Commit the updated seed files.

## Warnings

- **Existing volume migration**: developers with an existing `mysql_data` volume populated by the old `/docker-entrypoint-initdb.d` mount must run `docker compose down -v` before `docker compose up` to get a clean seeded database.
- **Password change**: administrators change their password from the UI. The `BOOTSTRAP_ADMIN_*` variables only matter while there is no active ADMIN.
- **`database/export/` is gitignored**: files in that folder may contain sensitive configuration data and must not be committed. Only `database/export/.gitkeep` is tracked.
