# shellcheck shell=bash
# =============================================================================
# _common.sh — conexión a MySQL compartida por export.sh, seed.sh y backup.sh
# (T-021). Se incluye con `source`; no se ejecuta solo.
#
# Reglas:
#   * Sin valores por defecto de host, usuario, clave ni base: DB_HOST, DB_USER,
#     DB_PASS y DB_NAME son obligatorias (DB_PORT, 3306 si falta). Vienen del
#     entorno o de un archivo explícito con --env-file <ruta>.
#   * --env-file se lee como KEY=VALUE, sin ejecutarlo (no se hace `source`);
#     solo se aceptan las claves DB_HOST, DB_PORT, DB_USER, DB_PASS, DB_NAME.
#   * La clave va a un archivo temporal (mktemp, 600) que se pasa con
#     --defaults-extra-file como PRIMERA opción de mysql/mysqldump y se borra
#     al salir (EXIT/INT/TERM). Nunca va en la línea de comandos ni en el
#     entorno de mysql/mysqldump (no se usa MYSQL_PWD).
# =============================================================================

DB_CONN_USAGE="Conexión (obligatoria, sin valores por defecto):
  DB_HOST, DB_USER, DB_PASS, DB_NAME   variables de entorno (DB_PORT opcional, 3306)
  --env-file <ruta>                    o un archivo KEY=VALUE con esas claves
                                       (no se ejecuta; los valores del archivo
                                       reemplazan a los del entorno)"

DB_CNF=""

# Los errores dentro de $(...) también abortan (con set -e en el script que incluye este archivo)
shopt -s inherit_errexit

db_die() {
    printf 'Error: %s\n' "$*" >&2
    exit 1
}

# db_load_env_file <ruta>: asigna DB_HOST/DB_PORT/DB_USER/DB_PASS/DB_NAME desde el archivo.
db_load_env_file() {
    local file="$1" line key value lineno=0
    [ -n "$file" ] || db_die "--env-file requiere una ruta"
    [ -f "$file" ] && [ -r "$file" ] || db_die "no se puede leer el archivo --env-file: $file"
    while IFS= read -r line || [ -n "$line" ]; do
        lineno=$((lineno + 1))
        line="${line%$'\r'}"
        # quitar espacios iniciales
        line="${line#"${line%%[![:space:]]*}"}"
        case "$line" in
            '' | '#'*) continue ;;
        esac
        line="${line#export }"
        if [[ ! "$line" =~ ^([A-Za-z_][A-Za-z0-9_]*)=(.*)$ ]]; then
            db_die "$file:$lineno: línea no válida (se espera KEY=VALUE)"
        fi
        key="${BASH_REMATCH[1]}"
        value="${BASH_REMATCH[2]}"
        case "$key" in
            DB_HOST | DB_PORT | DB_USER | DB_PASS | DB_NAME) ;;
            *) continue ;; # otras claves (p. ej. las de la aplicación) se ignoran
        esac
        # comillas envolventes opcionales, sin interpretar nada más
        if [[ "$value" =~ ^\"(.*)\"$ ]] || [[ "$value" =~ ^\'(.*)\'$ ]]; then
            value="${BASH_REMATCH[1]}"
        fi
        printf -v "$key" '%s' "$value"
    done < "$file"
}

# db_require_config <usage>: valida la configuración o muestra el uso y termina con código 2.
db_require_config() {
    local usage="$1" missing=() var
    for var in DB_HOST DB_USER DB_PASS DB_NAME; do
        # DB_PASS puede ser vacía, pero debe estar definida
        if [ "$var" = DB_PASS ]; then
            [ -n "${DB_PASS+x}" ] || missing+=("$var")
        elif [ -z "${!var:-}" ]; then
            missing+=("$var")
        fi
    done
    if [ "${#missing[@]}" -gt 0 ]; then
        printf 'Error: faltan variables de conexión: %s\n\n%s\n' "${missing[*]}" "$usage" >&2
        exit 2
    fi
    DB_PORT="${DB_PORT:-3306}"
    [[ "$DB_PORT" =~ ^[0-9]{1,5}$ ]] || db_die "DB_PORT no es un número de puerto: $DB_PORT"
    # El nombre de la base se interpola en SQL (`...`): solo caracteres seguros
    [[ "$DB_NAME" =~ ^[A-Za-z0-9_$-]+$ ]] || db_die "DB_NAME contiene caracteres no permitidos: $DB_NAME"
    case "$DB_HOST$DB_USER$DB_PASS" in
        *$'\n'*) db_die "DB_HOST, DB_USER y DB_PASS no pueden contener saltos de línea" ;;
    esac
}

# Escapa un valor para ir entre comillas dobles en un archivo de opciones de MySQL.
db_cnf_quote() {
    local v="$1"
    v="${v//\\/\\\\}"
    v="${v//\"/\\\"}"
    printf '"%s"' "$v"
}

db_cleanup() {
    if declare -F db_on_cleanup > /dev/null; then
        db_on_cleanup
    fi
    if [ -n "$DB_CNF" ] && [ -f "$DB_CNF" ]; then
        rm -f -- "$DB_CNF"
    fi
}

# db_create_cnf: escribe el archivo temporal de credenciales y registra su borrado.
db_create_cnf() {
    # La clave no se hereda a ningún proceso hijo
    export -n DB_PASS 2> /dev/null || true
    unset MYSQL_PWD
    trap db_cleanup EXIT
    trap 'db_cleanup; trap - EXIT; exit 130' INT
    trap 'db_cleanup; trap - EXIT; exit 143' TERM
    local old_umask
    old_umask="$(umask)"
    umask 077
    DB_CNF="$(mktemp "${TMPDIR:-/tmp}/db-client.XXXXXX.cnf")" || db_die "no se pudo crear el archivo temporal"
    umask "$old_umask"
    chmod 600 "$DB_CNF"
    {
        printf '[client]\n'
        printf 'host=%s\n' "$(db_cnf_quote "$DB_HOST")"
        printf 'port=%s\n' "$DB_PORT"
        printf 'user=%s\n' "$(db_cnf_quote "$DB_USER")"
        printf 'password=%s\n' "$(db_cnf_quote "$DB_PASS")"
    } > "$DB_CNF"
}

# Ejecutan el cliente con --defaults-extra-file como primera opción y sin la clave en el entorno.
db_mysql() {
    env -u DB_PASS -u MYSQL_PWD mysql --defaults-extra-file="$DB_CNF" "$@"
}

db_mysqldump() {
    env -u DB_PASS -u MYSQL_PWD mysqldump --defaults-extra-file="$DB_CNF" "$@"
}

db_require_client() {
    local bin
    for bin in "$@"; do
        command -v "$bin" > /dev/null 2>&1 || db_die "no se encontró '$bin' en el PATH"
    done
}

db_target() {
    printf '%s:%s/%s' "$DB_HOST" "$DB_PORT" "$DB_NAME"
}
