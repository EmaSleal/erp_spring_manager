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
