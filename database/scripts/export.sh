#!/bin/bash
set -eu

DB_HOST="${DB_HOST:-192.168.100.93}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-password}"
DB_NAME="${DB_NAME:-facturas_monrachem}"
export MYSQL_PWD="$DB_PASS"

EXPORT_DIR="$(dirname "$0")/../export"
ROUTINES_DIR="$(dirname "$0")/../routines"
mkdir -p "$EXPORT_DIR" "$ROUTINES_DIR"

DUMP_ARGS="--no-create-info --complete-insert --skip-extended-insert --skip-triggers"

BASE_TABLES=(
    cat_provincia_cr cat_canton_cr cat_distrito_cr
    parametro_sistema permiso rol rol_permiso
    plantilla_notificacion plantilla_whatsapp presentacion
)

echo "Exporting base tables..."
for table in "${BASE_TABLES[@]}"; do
    out="$EXPORT_DIR/$table.sql"
    mysqldump -h "$DB_HOST" -u "$DB_USER" $DUMP_ARGS "$DB_NAME" "$table" > "$out"
    echo "  $table -> $out"
done

read -p "Export configuracion tables? [y/N] " exp_config
if [[ "$exp_config" =~ ^[Yy] ]]; then
    for t in configuracion_email configuracion_empresa configuracion_facturacion configuracion_hacienda configuracion_notificaciones; do
        out="$EXPORT_DIR/$t.sql"
        mysqldump -h "$DB_HOST" -u "$DB_USER" $DUMP_ARGS "$DB_NAME" "$t" > "$out"
        echo "  $t -> $out"
    done
fi

read -p "Export empresa table? [y/N] " exp_empresa
if [[ "$exp_empresa" =~ ^[Yy] ]]; then
    mysqldump -h "$DB_HOST" -u "$DB_USER" $DUMP_ARGS "$DB_NAME" empresa > "$EXPORT_DIR/empresa.sql"
    echo "  empresa -> $EXPORT_DIR/empresa.sql"
fi

read -p "Export producto table? [y/N] " exp_producto
if [[ "$exp_producto" =~ ^[Yy] ]]; then
    mysqldump -h "$DB_HOST" -u "$DB_USER" $DUMP_ARGS "$DB_NAME" producto > "$EXPORT_DIR/producto.sql"
    echo "  producto -> $EXPORT_DIR/producto.sql"
fi

read -p "Export stored procedures? [y/N] " exp_sp
if [[ "$exp_sp" =~ ^[Yy] ]]; then
    out="$ROUTINES_DIR/stored_procedures.sql"
    mysqldump -h "$DB_HOST" -u "$DB_USER" --no-data --no-create-info --no-tablespaces \
        --routines --skip-triggers "$DB_NAME" | sed 's/DEFINER=[^ ]*//g' > "$out"
    echo "  stored procedures -> $out"
fi

read -p "Export triggers? [y/N] " exp_triggers
if [[ "$exp_triggers" =~ ^[Yy] ]]; then
    out="$ROUTINES_DIR/triggers.sql"
    mysqldump -h "$DB_HOST" -u "$DB_USER" --no-data --no-create-info --no-tablespaces \
        --skip-routines --add-drop-trigger "$DB_NAME" | sed 's/DEFINER=[^ ]*//g' > "$out"
    echo "  triggers -> $out"
fi

echo "Export complete. Files in: $EXPORT_DIR"
