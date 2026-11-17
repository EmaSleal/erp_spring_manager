## Project memory (read first)
This repository uses the `ratifact` MCP server with project `erp_spring_manager`. Before working, call
`get_context("erp_spring_manager")`. Only **accepted** decisions are rules; proposals are not implemented without the
user's confirmation. Code tasks run with `/run-task T-NNN` (in Codex: `$run-task T-NNN`).
Decisions, pending items and tasks live in `ratifact`; on a contradiction with another memory, the
accepted decision wins.
Test command (from the repository root): `./mvnw test`

Contexto general, estructura y comandos: ver `agents.md`. Comando de pruebas: `./mvnw test`.

## Reglas de ejecución

- Prohibido ejecutar comandos de limpieza global, aunque parezcan inofensivos:
  - `docker system prune`, `docker volume prune`, `docker image prune`, `docker container prune`, `docker network prune`.
  - `docker rm` o `docker rmi` masivos (por ejemplo `docker rm $(docker ps -aq)`).
  - `git clean -fdx` fuera del worktree propio de la tarea.
  - `rm -rf` fuera del repositorio o del directorio temporal propio del agente.
- Todo recurso que cree una tarea lleva un nombre explícito con el prefijo de la tarea
  (por ejemplo `docker run --name t007-mysql`, volumen nombrado `t007-mysql-data`, o `--tmpfs` en lugar de volúmenes).
  Evitar volúmenes anónimos.
- Esos recursos se eliminan solo por su nombre exacto (`docker rm -f t007-mysql`, `docker volume rm t007-mysql-data`).
  Al terminar, el conjunto de contenedores y volúmenes debe ser igual al que había antes de empezar.
- Nunca ejecutar la aplicación ni Maven desde la raíz del repositorio con `.env` presente: contiene credenciales
  de producción. Trabajar en una copia sin `.env`/`.env.local` y sin acceso a red externa.

## Dependencias

- Toda actualización de Spring Boot o Hibernate exige BloqueoFiscalFacturaIT en verde antes de integrarse
  (PoliticaMutabilidadFactura usa PersistenceContext, SPI interna). Las *IT corren con `./mvnw verify`.

## Protocolo de verificación (smoke tests)

Aplica a cualquier arranque de la aplicación para verificar un cambio (T-036; complementa «Reglas de ejecución»).

1. **Copia sin secretos.** Trabajar en una copia sin `.env`/`.env.local` (`git worktree add --detach` o `git archive`),
   nunca en la raíz del repositorio.
2. **Base de datos propia.** MySQL en un contenedor con nombre de la tarea y `tmpfs` (por ejemplo
   `docker run --name tNNN-mysql --tmpfs /var/lib/mysql ...`). Nunca `SELECT *` sobre `configuracion_hacienda` ni
   `configuracion_email`. Al terminar, `docker rm -f tNNN-mysql`: el estado de Docker queda igual al inicial.
3. **Jobs apagados.** Arrancar con `app.scheduling.enabled=false` (variable `APP_SCHEDULING_ENABLED=false`): ningún método
   `@Scheduled` se ejecuta (reintentos de comprobantes, recordatorios, tipo de cambio, alertas de inventario, caché).
   Ya no se usan ventanas de tiempo para evitar los jobs.
4. **Sin salida a hosts externos.** Arrancar con `-Djdk.net.hosts.file=<archivo>`; el archivo solo resuelve localhost:

   ```
   127.0.0.1 localhost
   ::1 localhost
   ```

   Con esa opción de la JVM ningún otro nombre resuelve (falla con `UnknownHostException`): ni `api.hacienda.go.cr`
   ni las URL de recepción e IdP de Hacienda fijas en el enum `AmbienteHacienda`, que apuntar las URL configurables a
   127.0.0.1 no bloquearía.

Comando completo (desde la copia, con el jar ya construido). Las variables obligatorias son `DB_URL`, `DB_USERNAME`,
`DB_PASSWORD`, `META_WEBHOOK_VERIFY_TOKEN`, `APP_BASE_URL` y `CORS_ALLOWED_ORIGINS` (sin ellas la app falla con
«Faltan variables de entorno obligatorias»); `HACIENDA_CRYPTO_KEY` debe ser base64 de 32 bytes. Los directorios por
defecto (`/app/certificados`, `/app/sellos`, `/app/uploads`) no son escribibles fuera de Docker: se apuntan a un
directorio temporal. Sustituir solo lo que va entre `<>`; el resto son valores desechables:

```
S="$(mktemp -d)"; mkdir -p "$S"/{certificados,sellos,uploads,comprobantes}
printf '127.0.0.1 localhost\n::1 localhost\n' > "$S/hosts-solo-localhost"
env -i PATH="$PATH" HOME="$HOME" \
  DB_URL='jdbc:mysql://127.0.0.1:<puerto-mysql>/<base>' DB_USERNAME=<usuario> DB_PASSWORD=<clave> \
  META_WEBHOOK_VERIFY_TOKEN=smoke-test-token \
  APP_BASE_URL=http://127.0.0.1:<puerto-app> CORS_ALLOWED_ORIGINS=http://127.0.0.1:<puerto-app> \
  SERVER_PORT=<puerto-app> \
  CERTIFICADOS_DIR="$S/certificados" SELLOS_DIR="$S/sellos" UPLOADS_DIR="$S/uploads" \
  FACTURACION_DIRECTORIO_BASE="$S/comprobantes" \
  HACIENDA_CRYPTO_KEY="$(openssl rand -base64 32)" \
  APP_SCHEDULING_ENABLED=false TIPO_CAMBIO_SYNC_ENABLED=false \
  java -Djdk.net.hosts.file="$S/hosts-solo-localhost" -jar target/*.jar
```

Al terminar, borrar `$S` (directorio temporal propio).
