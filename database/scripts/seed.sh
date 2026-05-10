#!/bin/sh
set -eu

DB_HOST="${DB_HOST:-192.168.100.93}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-password}"
DB_NAME="${DB_NAME:-facturas_monrachem}"

SEED_DIR="$(dirname "$0")/../seeds"
export MYSQL_PWD="$DB_PASS"

for f in "$SEED_DIR"/*.sql; do
    echo "Running $(basename "$f")..."
    mysql -h "$DB_HOST" -u "$DB_USER" "$DB_NAME" < "$f"
    echo "  OK"
done

echo "Seeding complete."
