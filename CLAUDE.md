# erp_spring_manager

Este repositorio se gestiona en memoria-proyectos con el proyecto `erp_spring_manager`.

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
