#!/usr/bin/env bash
# Carga database/seeds/*.sql y database/routines/*.sql en la base indicada.
# Conexión: ver _common.sh (sin valores por defecto).
#
# Salvaguardas (T-021):
#   * Muestra host:puerto/base y pide escribir el nombre de la base para confirmar.
#     Sin terminal, solo continúa con --yes Y CONFIRM_DB=<nombre de la base>.
#   * Si la base ya tiene datos de negocio (factura, asientos_contables o clientes
#     distintos del genérico que crea DataInitializer), aborta salvo --force.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=_common.sh
. "$SCRIPT_DIR/_common.sh"

USAGE="Uso: $(basename "$0") [--env-file <ruta>] [--yes] [--force]

  --yes     no preguntar; exige CONFIRM_DB=<nombre de la base> en el entorno
  --force   cargar aunque la base ya tenga datos de negocio

$DB_CONN_USAGE"

ASSUME_YES=0
FORCE=0
while [ $# -gt 0 ]; do
    case "$1" in
        --env-file)
            [ $# -ge 2 ] || { printf '%s\n' "$USAGE" >&2; exit 2; }
            db_load_env_file "$2"
            shift 2
            ;;
        --yes) ASSUME_YES=1; shift ;;
        --force) FORCE=1; shift ;;
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
db_require_client mysql
db_create_cnf

SEED_DIR="$SCRIPT_DIR/../seeds"
ROUTINES_DIR="$SCRIPT_DIR/../routines"

echo "Destino: $(db_target)"

# --- confirmación -----------------------------------------------------------
if [ "$ASSUME_YES" -eq 1 ]; then
    if [ "${CONFIRM_DB:-}" != "$DB_NAME" ]; then
        db_die "--yes requiere CONFIRM_DB=$DB_NAME (nombre exacto de la base destino)"
    fi
elif [ -t 0 ]; then
    read -r -p "Escriba el nombre de la base para confirmar la carga: " typed
    [ "$typed" = "$DB_NAME" ] || db_die "confirmación incorrecta; no se cargó nada"
else
    db_die "sin terminal: use --yes con CONFIRM_DB=$DB_NAME"
fi

# --- ¿la base ya tiene datos de negocio? ------------------------------------
# Imprime 1 si la tabla existe y tiene filas que cumplen la condición; 0 si no.
table_has_rows() {
    local table="$1" where="${2:-1=1}" exists
    exists="$(db_mysql -N -B -e "SELECT COUNT(*) FROM information_schema.tables
        WHERE table_schema = '$DB_NAME' AND table_name = '$table'")"
    [ "$exists" = "1" ] || { echo 0; return; }
    db_mysql -N -B -e "SELECT EXISTS(SELECT 1 FROM \`$DB_NAME\`.\`$table\` WHERE ($where) LIMIT 1)"
}

# Asignaciones separadas: si una consulta falla, set -e aborta (no se asume "vacía").
con_datos=()
r="$(table_has_rows factura)"
if [ "$r" = "1" ]; then con_datos+=(factura); fi
r="$(table_has_rows asientos_contables)"
if [ "$r" = "1" ]; then con_datos+=(asientos_contables); fi
# DataInitializer crea "Cliente Genérico" (000000000) en el primer arranque; ese no cuenta.
r="$(table_has_rows cliente "identificacion IS NULL OR identificacion <> '000000000'")"
if [ "$r" = "1" ]; then con_datos+=(cliente); fi

if [ "${#con_datos[@]}" -gt 0 ]; then
    if [ "$FORCE" -eq 1 ]; then
        echo "Aviso: la base ya tiene datos de negocio (${con_datos[*]}); se continúa por --force."
    else
        db_die "la base $(db_target) ya tiene datos de negocio (${con_datos[*]}). No se cargó nada; use --force si es intencional."
    fi
fi

# --- carga --------------------------------------------------------------------
for f in "$SEED_DIR"/*.sql; do
    [ -e "$f" ] || continue
    echo "Running $(basename "$f")..."
    db_mysql "$DB_NAME" < "$f"
    echo "  OK"
done

echo "Seeding complete."

for f in "$ROUTINES_DIR"/*.sql; do
    [ -e "$f" ] || continue
    echo "Applying routine $(basename "$f")..."
    db_mysql "$DB_NAME" < "$f"
    echo "  OK"
done
