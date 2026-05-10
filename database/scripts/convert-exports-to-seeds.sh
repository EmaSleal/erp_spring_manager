#!/bin/bash
set -eu

EXPORT_DIR="$(dirname "$0")/../export"
SEEDS_DIR="$(dirname "$0")/../seeds"

declare -A TABLES=(
    ["04"]="parametro_sistema"
    ["05"]="permiso"
    ["06"]="rol"
    ["07"]="rol_permiso"
    ["08"]="plantilla_notificacion"
    ["09"]="plantilla_whatsapp"
    ["10"]="presentacion"
)

for num in 04 05 06 07 08 09 10; do
    table="${TABLES[$num]}"
    src="$EXPORT_DIR/$table.sql"
    dest="$SEEDS_DIR/${num}_${table}.sql"

    if [ ! -f "$src" ]; then
        echo "  SKIP $table.sql — not found in export/"
        continue
    fi

    {
        printf -- "-- ============================================================\n"
        printf -- "-- Seed: %s\n" "$table"
        printf -- "-- Idempotency: INSERT IGNORE\n"
        printf -- "-- ============================================================\n"
        printf -- "SET NAMES utf8mb4;\n"
        printf -- "USE facturas_monrachem;\n\n"

        # Extract INSERT lines only, convert to INSERT IGNORE
        sed -n '/^INSERT INTO/p' "$src" \
            | sed 's/^INSERT INTO/INSERT IGNORE INTO/'
    } > "$dest"

    echo "  OK  ${num}_${table}.sql"
done

echo "Done. Seeds ready in: $SEEDS_DIR"
