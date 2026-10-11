# Recuperación de documentos eliminados de docs/

| Campo | Valor |
|---|---|
| Fecha | 2026-10-10 |
| Commit base | `e0a14813837d71993c0196af23c0a9bc91bca60f` (`prod`) |
| Tarea | T-003 (pendiente P-002) |

## Método

1. Se extrajeron los enlaces relativos de `docs/INDICE.md` y se comprobó cuáles no existen. Los enlaces a `base%20de%20datos/*` se resuelven correctamente (`docs/base de datos/`) y no cuentan como rotos. Resultado: **16 enlaces rotos**.
2. Para cada ruta se buscó su eliminación con `git log --all --diff-filter=D -- docs/<ruta>`.
3. Se revisó si hubo renombres con `git show -M --name-status <commit>`. Todas las rutas aparecen como `D` (eliminación) y ninguna como `R` (renombre).
4. Se buscó si el contenido existe hoy en otra ruta, comparando el blob de la última versión con todos los blobs de `HEAD` (`git ls-tree -r HEAD`). No hubo coincidencias, así que **ningún documento fue movido** y `docs/INDICE.md` no se modificó.
5. Cada archivo se restauró con `git show <commit>^:docs/<ruta>` y se verificó byte a byte con `git hash-object`, comparando contra `git rev-parse <commit>^:docs/<ruta>`. Los 14 coinciden.

Los documentos restaurados **no se editaron**: reflejan el estado del proyecto en la fecha en que se escribieron.

## Resultado

| Ruta | Existió en historial | Commit de eliminación o movimiento | Commit de origen restaurado | Restaurado | Observación |
|---|---|---|---|---|---|
| `docs/planificacion/DECISIONES_TECNICAS.txt` | Sí | `025c1b3` (2026-07-08, "cleaning docs"); el mismo cambio está en `cf4ae8a`, fuera de cualquier rama | `025c1b3^` (último cambio: `6182ddf`) | Sí | Decisiones del Sprint 1 (10/10/2025). Es la fuente para migrar a ADR. `cf4ae8a^` tiene el mismo contenido |
| `docs/arquitectura/ARQUITECTURA_PROYECTO.md` | Sí | `308ac30` (2026-05-07, "creating formatting in docs") | `308ac30^` (último cambio: `f0cf4fc`) | Sí | Solo existió en la historia de `legacy/main`, que no está relacionada con `prod`. Creado el 26/10/2025 |
| `docs/arquitectura/COMPONENTES.md` | Sí | `308ac30` | `308ac30^` (`f0cf4fc`) | Sí | Historia de `legacy/main` |
| `docs/arquitectura/ESTRUCTURA_PROYECTO.md` | Sí | `308ac30` | `308ac30^` (`f0cf4fc`) | Sí | Historia de `legacy/main` |
| `docs/diseno/INDICE.md` | Sí | `025c1b3` / `cf4ae8a` (2026-07-08) | `025c1b3^` (último cambio: `0906f7d`) | Sí | Al eliminar `docs/diseno/` también se borraron otros archivos no enlazados desde el índice, que no se restauraron (fuera de alcance) |
| `docs/diseno/MOCKUPS_VISUALES.md` | Sí | `025c1b3` / `cf4ae8a` | `025c1b3^` (`0906f7d`) | Sí | |
| `docs/diseno/README.md` | Sí | `025c1b3` / `cf4ae8a` | `025c1b3^` (`0906f7d`) | Sí | |
| `docs/guias/GUIA_INICIALIZACION_PREFERENCIAS.md` | Sí | `308ac30` | `308ac30^` (`f0cf4fc`) | Sí | Historia de `legacy/main` |
| `docs/guias/GUIA_LOGGING.md` | Sí | `308ac30` | `308ac30^` (`f0cf4fc`) | Sí | Historia de `legacy/main` |
| `docs/guias/GUIA_MENU_HAMBURGUESA.md` | No | — | — | No | Nunca existió con esa ruta. En el historial hay un `docs/diseno/RESUMEN_MENU_HAMBURGUESA.md` (eliminado en `025c1b3`), que no se restauró porque no es la ruta enlazada |
| `docs/guias/GUIA_TABLAS_RESPONSIVE.md` | No | — | — | No | Nunca existió con esa ruta. En el historial hay un `docs/diseno/RESUMEN_TABLAS_RESPONSIVE.md` (eliminado en `025c1b3`), que no se restauró |
| `docs/permisos/MAPEO_SISTEMA_PERMISOS.md` | Sí | `308ac30` | `308ac30^` (`f0cf4fc`) | Sí | Historia de `legacy/main` |
| `docs/permisos/RESUMEN_ROLES_PERMISOS.md` | Sí | `308ac30` | `308ac30^` (`f0cf4fc`) | Sí | Historia de `legacy/main` |
| `docs/refactorizacion/INDICE_REFACTORIZACION.md` | Sí | `025c1b3` / `cf4ae8a` | `025c1b3^` (`6182ddf`) | Sí | |
| `docs/reportes/ESTADO_PROYECTO.md` | Sí | `308ac30` | `308ac30^` (`f0cf4fc`) | Sí | Historia de `legacy/main`. Contenido obsoleto: describe el Sprint 4 al 27/12/2025. Las auditorías `docs/sprints/SPRINT_5/AUDITORIA_ESTADO_SPRINT_5.md` y `docs/sprints/AUDITORIA_ALCANCE_SPRINTS_6_9.md` (T-001/T-002) lo reemplazan |
| `docs/reportes/PROXIMOS_PASOS.md` | Sí | `308ac30` | `308ac30^` (`f0cf4fc`) | Sí | Historia de `legacy/main`. Contenido obsoleto: Sprint 1 (13/10/2025) |

Totales: 14 restaurados, 2 que nunca existieron, 0 movidos. No hubo cambios en `docs/INDICE.md`.

## Notas

- `308ac30` y `f0cf4fc` solo son alcanzables desde la etiqueta `legacy/main`, que conserva la historia de la antigua rama `main`, no relacionada con `prod`. Por eso `git log HEAD -- <ruta>` no los muestra y fue necesario usar `--all`.
- `cf4ae8a` repite el cambio de `025c1b3` (mismo mensaje y fecha) y no pertenece a ninguna rama. Para las rutas afectadas, el contenido de `cf4ae8a^` es idéntico al de `025c1b3^`.
