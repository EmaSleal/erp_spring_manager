# Agents context para el proyecto "whats_orders_manager"

## Propósito
Este archivo ofrece al agente (Copilot / subagentes) un resumen conciso del proyecto, su estructura de carpetas, documentación relevante y comandos útiles para entender y actuar sobre el código sin necesidad de adivinar dónde están los recursos.

## Resumen del proyecto
- Nombre: whats_orders_manager
- Tipo: Aplicación Java Spring Boot (Maven)
- Ubicación del proyecto: raíz del workspace
- Entradas principales: `pom.xml`, `mvnw`, `mvnw.cmd`

## Estructura relevante (detallada)

- `src/`
	- `main/`
		- `java/` : Código fuente Java organizado por paquetes (controladores, servicios, modelos, repositorios)
		- `resources/` : Plantillas Thymeleaf, archivos de propiedades, mensajes y recursos estáticos
	- `test/` : Tests unitarios e integración

- `docs/`
	- `arquitectura/` : Diagramas y decisiones arquitectónicas
	- `base de datos/` : Scripts SQL, migraciones y datos iniciales
	- `README.md`, `INDICE.md` y guías específicas por sprint
	- `scripts/` (ej. `*.ps1`) : Scripts de ayuda para Windows PowerShell incluidos en la raíz
## Convenciones y notas para el agente

Ver la sección **Convenciones, contactos y uso para agentes** al final de este documento para las reglas y prácticas recomendadas.
- Raíz del proyecto
	- `pom.xml`, `mvnw`, `mvnw.cmd` — build y wrapper Maven
## Documentación clave

- Cargar variables de entorno (Windows PowerShell): `./load-env.ps1` (ejecutar desde la raíz del proyecto)
- [INICIO_RAPIDO.md](INICIO_RAPIDO.md) — pasos rápidos para levantar la aplicación localmente
- [HELP.md](HELP.md) — notas operativas y problemas comunes

Para cambios estructurales, priorizar `docs/ARQUITECTURA_PROYECTO.md` y `INICIO_RAPIDO.md`.

### Documentos clave adicionales (rápida referencia)

- `docs/ESTADO_PROYECTO.md` — estado consolidado y dashboard
- `docs/COMPONENTES.md` — descripción de componentes del sistema
- `docs/planificacion/PLAN_MAESTRO.txt` — plan maestro del proyecto
- `docs/sprints/` — documentación por sprint (resúmenes, checklists, fixes)
- `docs/base de datos/CREATE_DB.txt` — esquema y scripts de creación de BD
- `docs/snippets/` — fragmentos de código reutilizables

Consulta `docs/INDICE.md` para rutas y prioridad de lectura por rol (Desarrollador, Arquitecto, Stakeholder).

## Comandos útiles
- Compilar con wrapper (Windows): `mvnw.cmd clean package`
- Compilar con wrapper (Unix): `./mvnw clean package`
- Ejecutar tests: `mvnw.cmd test` o `./mvnw test`
- Levantar con Docker Compose: `docker-compose up --build`

Comandos de desarrollo y despliegue (rápida guía):

- Ejecutar aplicación localmente (wrapper): `mvnw.cmd spring-boot:run` o `./mvnw spring-boot:run`
- Limpiar y compilar: `mvnw.cmd clean package -DskipTests=false`
- Ejecutar solo compilación de pruebas: `mvnw.cmd -DskipTests=false test-compile`
- Ejecutar profile local: `mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local`
- Ejecutar migraciones / scripts SQL: revisar `docs/base de datos/` y ejecutar los archivos con su cliente DB (p. ej., `psql`, `mysql`)
- Generar certificado de prueba (Windows PowerShell): `generar-certificado-prueba.ps1`
- Cargar variables de entorno (Windows PowerShell): `.
	load-env.ps1` (ejecutar desde la raíz del proyecto)

Docker / Entorno:

- Construir imagen Docker: `docker build -t whats_orders_manager .`
- Levantar con Docker Compose (recrear): `docker-compose up --build --force-recreate`

Notas:

- Cuando propongas cambios que afecten a la base de datos, referencia los scripts en `docs/base de datos/` y documenta la secuencia de migración.
- Para cambios de configuración sensibles (certificados, secretos), no modificar `certificados/` sin aprobación explícita.

## Convenciones y notas para el agente
- Priorizar lectura de `README.md`, `INICIO_RAPIDO.md` y `docs/` antes de proponer cambios estructurales.
- Cuando sugieras cambios en código, indica archivos afectados y pruebas necesarias para validar.
- No modifiques archivos de `certificados/` ni scripts sin confirmación explícita.

## Uso previsto de este archivo
- Proveer contexto para respuestas automatizadas, generación de instrucciones y creación de tareas.
- No sustituye al análisis de código: el agente debería abrir archivos concretos (`src/...`) cuando necesite detalles.

## Contacto / Mantenimiento
- Mantener sincronizado con `README.md` y `INICIO_RAPIDO.md`.
- Autor del repositorio: revisar `pom.xml` o `README.md` para información de contacto.

--
Archivo generado automáticamente para facilitar la interacción del agente con el repo.

## Convenciones, contactos y uso para agentes

- **Prioridad de lectura**: antes de modificar código, revisar en orden: `README.md`, `INICIO_RAPIDO.md`, `docs/INDICE.md`, `docs/ARQUITECTURA_PROYECTO.md`.
- **Estilo de código**: seguir las convenciones Java estándar del proyecto (uso de Lombok y paquetes por dominio). Cuando propongas cambios de formato, sugiere comandos de formateo y no ejecutes cambios masivos sin aprobación.
- **Commits y branches**:
	- Si el repositorio está bajo control de versiones, crear ramas temáticas `feature/<descripcion>` o `fix/<descripcion>` y usar mensajes de commit claros: `Tipo: Breve descripción — detalles (ops)`.
	- No realizar pushes por cuenta propia sin aprobación humana.
- **Ejecución de builds/tests**: usar el wrapper (`mvnw`/`mvnw.cmd`) para compilar y ejecutar tests. Ejecutar `mvnw.cmd -DskipTests=false test-compile` antes de proponer cambios grandes.
- **Base de datos y migraciones**: cualquier cambio que afecte a esquemas debe incluir scripts en `docs/base de datos/` y una secuencia de migración documentada.
- **Archivos sensibles**: no modificar `certificados/`, ni incluir secretos en commits; si es necesario, solicitar instrucciones explícitas.
- **Documentación de cambios**: cuando propongas o apliques cambios, actualiza `docs/` y añade una nota en `docs/ESTADO_PROYECTO.md` o en el sprint correspondiente.
- **Interacción con el agente**:
	- Antes de aplicar cambios destructivos pedirá confirmación al usuario.
	- Al sugerir refactors proporcionará un plan de pasos pequeños, pruebas necesarias y listas de verificación.
	- Priorizar reproducibilidad: incluir comandos reproducibles y entornos (JDK, Maven) cuando sean relevantes.
- **Contacto / Responsables**:
	- Owner del repositorio: revisar `README.md` o `pom.xml` para el contacto principal.
	- Para cambios de infraestructura o despliegue, contactar al equipo de DevOps (información en `docs/` si está disponible).

## Uso previsto y límites
- Este archivo debe usarse como guía rápida; no sustituye a políticas internas ni a revisiones humanas.
- El agente debe siempre abrir y revisar los archivos relevantes (`src/**`, `docs/**`, `pom.xml`) antes de actuar.

