#!/usr/bin/env bash
# Exporta datos de referencia de la base a database/export/ (y rutinas a database/routines/).
# Conexión: ver _common.sh (sin valores por defecto). configuracion_hacienda NO se exporta (T-021):
# guarda credenciales de Hacienda.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=_common.sh
. "$SCRIPT_DIR/_common.sh"

USAGE="Uso: $(basename "$0") [--env-file <ruta>]

$DB_CONN_USAGE"

while [ $# -gt 0 ]; do
    case "$1" in
        --env-file)
            [ $# -ge 2 ] || { printf '%s\n' "$USAGE" >&2; exit 2; }
            db_load_env_file "$2"
            shift 2
            ;;
        -h | --help)
            printf '%s\n' "$USAGE"
            exit 0
            ;;
        *)
            printf 'Error: argumento desconocido: %s\n\n%s\n' "$1" "$USAGE" >&2
            exit 2
            ;;
    esac
done

db_require_config "$USAGE"
db_require_client mysqldump
db_create_cnf

EXPORT_DIR="$SCRIPT_DIR/../export"
ROUTINES_DIR="$SCRIPT_DIR/../routines"
mkdir -p "$EXPORT_DIR" "$ROUTINES_DIR"

DUMP_ARGS=(--no-create-info --complete-insert --skip-extended-insert --skip-triggers)

BASE_TABLES=(
    cat_provincia_cr cat_canton_cr cat_distrito_cr
    parametro_sistema permiso rol rol_permiso
    plantilla_notificacion plantilla_whatsapp presentacion
    articulo_maestro
)

# Sin configuracion_hacienda: contiene credenciales de Hacienda (T-021).
CONFIG_TABLES=(
    configuracion_email configuracion_empresa configuracion_facturacion configuracion_notificaciones
)

echo "Origen: $(db_target)"
echo "Exporting base tables..."
for table in "${BASE_TABLES[@]}"; do
    out="$EXPORT_DIR/$table.sql"
    db_mysqldump "${DUMP_ARGS[@]}" "$DB_NAME" "$table" > "$out"
    echo "  $table -> $out"
done

read -r -p "Export configuracion tables? [y/N] " exp_config
if [[ "$exp_config" =~ ^[Yy] ]]; then
    for t in "${CONFIG_TABLES[@]}"; do
        out="$EXPORT_DIR/$t.sql"
        db_mysqldump "${DUMP_ARGS[@]}" "$DB_NAME" "$t" > "$out"
        echo "  $t -> $out"
    done
fi

read -r -p "Export empresa table? [y/N] " exp_empresa
if [[ "$exp_empresa" =~ ^[Yy] ]]; then
    db_mysqldump "${DUMP_ARGS[@]}" "$DB_NAME" empresa > "$EXPORT_DIR/empresa.sql"
    echo "  empresa -> $EXPORT_DIR/empresa.sql"
fi

read -r -p "Export producto table? [y/N] " exp_producto
if [[ "$exp_producto" =~ ^[Yy] ]]; then
    db_mysqldump "${DUMP_ARGS[@]}" "$DB_NAME" producto > "$EXPORT_DIR/producto.sql"
    echo "  producto -> $EXPORT_DIR/producto.sql"
fi

read -r -p "Export stored procedures? [y/N] " exp_sp
if [[ "$exp_sp" =~ ^[Yy] ]]; then
    out="$ROUTINES_DIR/stored_procedures.sql"
    db_mysqldump --no-data --no-create-info --no-tablespaces \
        --routines --skip-triggers "$DB_NAME" | sed 's/DEFINER=[^ ]*//g' > "$out"
    echo "  stored procedures -> $out"
fi

read -r -p "Export triggers? [y/N] " exp_triggers
if [[ "$exp_triggers" =~ ^[Yy] ]]; then
    out="$ROUTINES_DIR/triggers.sql"
    db_mysqldump --no-data --no-create-info --no-tablespaces \
        --skip-routines --add-drop-trigger "$DB_NAME" | sed 's/DEFINER=[^ ]*//g' > "$out"
    echo "  triggers -> $out"
fi

echo "Export complete. Files in: $EXPORT_DIR"
