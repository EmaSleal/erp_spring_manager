#!/usr/bin/env bash
# Respaldo completo de la base: datos, rutinas, triggers y eventos (T-021).
#
#   backup.sh --out <directorio> [--env-file <ruta>]
#
# Genera <directorio>/<base>_<YYYYMMDD_HHMMSS>.sql.gz y, junto a él, el .sha256 (formato sha256sum, nombre
# relativo). Si algo falla, borra el archivo parcial y termina con error.
# Conexión: ver _common.sh (sin valores por defecto).
# HACIENDA_CRYPTO_KEY NO está en la base: respáldela aparte (ver database/README.md).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=_common.sh
. "$SCRIPT_DIR/_common.sh"

USAGE="Uso: $(basename "$0") --out <directorio> [--env-file <ruta>]

  --out <dir>   directorio de salida (obligatorio; se crea si no existe)

$DB_CONN_USAGE"

OUT_DIR=""
while [ $# -gt 0 ]; do
    case "$1" in
        --out)
            [ $# -ge 2 ] || { printf '%s\n' "$USAGE" >&2; exit 2; }
            OUT_DIR="$2"
            shift 2
            ;;
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

if [ -z "$OUT_DIR" ]; then
    printf 'Error: falta --out <directorio>\n\n%s\n' "$USAGE" >&2
    exit 2
fi

db_require_config "$USAGE"
db_require_client mysqldump gzip sha256sum
db_create_cnf

mkdir -p -- "$OUT_DIR"
OUT_DIR="$(cd "$OUT_DIR" && pwd)"
STAMP="$(date +%Y%m%d_%H%M%S)"
FILE_NAME="${DB_NAME}_${STAMP}.sql.gz"
OUT_FILE="$OUT_DIR/$FILE_NAME"
SUM_FILE="$OUT_FILE.sha256"
[ ! -e "$OUT_FILE" ] || db_die "ya existe $OUT_FILE"

COMPLETED=0
# Llamada desde db_cleanup (EXIT/INT/TERM): si no terminó bien, no deja archivos a medias.
db_on_cleanup() {
    if [ "$COMPLETED" -ne 1 ]; then
        rm -f -- "$OUT_FILE" "$SUM_FILE"
    fi
}

DUMP_ARGS=(--single-transaction --routines --triggers --events)
# --set-gtid-purged solo existe en el cliente de MySQL (no en el de MariaDB)
if db_mysqldump --help 2> /dev/null | grep -q -- '--set-gtid-purged'; then
    DUMP_ARGS+=(--set-gtid-purged=OFF)
fi

echo "Respaldando $(db_target) -> $OUT_FILE"
umask 077
db_mysqldump "${DUMP_ARGS[@]}" "$DB_NAME" | gzip -c > "$OUT_FILE"
gzip -t "$OUT_FILE"
(cd "$OUT_DIR" && sha256sum -- "$FILE_NAME" > "$FILE_NAME.sha256")
COMPLETED=1

echo "Respaldo listo:"
echo "  $OUT_FILE"
echo "  $SUM_FILE"
echo "Verificar: (cd \"$OUT_DIR\" && sha256sum -c \"$FILE_NAME.sha256\")"
