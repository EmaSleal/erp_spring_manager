# Triage de ramas y tags

| Campo | Valor |
|---|---|
| Fecha | 2026-10-10 |
| Base de comparación (P) | `t-004-higiene-de-ramas-integrar-ramas-de-audit` = `1cb7551aaf3c499d2150b6a29a9143c711ae998a` ("prod tras fast-forward a t-004") |
| `prod` actual | `e0a14813837d71993c0196af23c0a9bc91bca60f` (= `origin/prod`) |
| Relación P / prod | P = prod + 4 merges `--no-ff` (t-001, t-002, t-003, chore/memoria-proyectos-setup); `git rev-list --count prod..P` = 8, `P..prod` = 0 |
| Alcance | Solo lectura. Ninguna rama, tag ni commit fue creado, movido o borrado. |

## Método

Todo dato de este documento proviene de comandos git de solo lectura ejecutados sobre el repositorio local (sin `fetch`; el estado de GitHub no se consultó por red):

- Inventario: `git for-each-ref refs/heads refs/remotes refs/tags` (no existe `refs/stash`; `git stash list` vacío; no hay otros namespaces).
- Por ref: `git rev-list --count P..<ref>`, `git rev-list --left-right --count P...<ref>`, `git merge-base P <ref>`.
- Contención de commits: `git for-each-ref --contains <hash>`.
- Equivalencia de parches: `git cherry P legacy/main` y mapeo por `git patch-id --stable` (commits de P vs. commits de `legacy/main` fuera de P).
- Equivalencia de contenido: `git diff --name-status legacy/main P -- src`; para cada archivo modificado, se buscó si el blob de `legacy/main` aparece en algún commit del historial de P (`git log P --full-history --no-renames -- <archivo>` + `git rev-parse <commit>:<archivo>`); para cada archivo solo-legacy, se buscó el commit de P que lo eliminó (`--diff-filter=D --full-history --no-renames`).
- Área de cada commit: `git show --name-only -- src`, clasificando por `modules/<mod>/` (Java, templates y static), `shared/`, `application*.yml`, `config/` y capas pre-modulares (`controllers/`, `models/`, `services/`, `templates/<área>` fuera de `modules/`), denominadas "monolito". Un commit que toca 4 o más áreas se clasifica como "transversal"; en otro caso, por el área con más archivos.

## 1. Inventario de refs (criterio 3a)

Estados: **INTEGRADA** = sin commits fuera de P; **RESCATAR** = contiene trabajo ausente en P que debe llevarse a P; **ARCHIVAR** = contiene commits fuera de P pero sin contenido a recuperar (conservar como referencia o eliminar). Se añade **MANTENER** solo para la rama de entorno `stage`, cuyo contenido propio no debe ir a P por diseño.

| Ref | Último commit | Commits no contenidos en P | ¿Ancestro común con P? | Estado | Justificación |
|---|---|---|---|---|---|
| `refs/heads/prod` | `e0a1481` 2026-08-26 Merge pull request #6 from EmaSleal/feat/reporte-productos-presentacion | 0 | sí, `e0a1481` (es ancestro de P) | INTEGRADA | Tronco. Queda 8 commits detrás de P hasta el fast-forward. |
| `refs/remotes/origin/prod` | `e0a1481` (ídem) | 0 | sí, `e0a1481` | INTEGRADA | Igual a `prod` local. |
| `refs/remotes/origin/HEAD` | `e0a1481` (ídem) | 0 | sí, `e0a1481` | INTEGRADA | Apunta a `origin/prod`. |
| `refs/heads/t-004-higiene-de-ramas-integrar-ramas-de-audit` | `1cb7551` 2026-10-10 Merge branch 'chore/memoria-proyectos-setup' into t-004-… | 0 | es P | INTEGRADA (tras FF) | Es la base P; queda integrada cuando `prod` se avance a `1cb7551`. |
| `refs/heads/t-001-auditoria-del-estado-real-del-sprint-5-c` | `37d280e` 2026-10-10 T-001: Auditoría del estado real del Sprint 5 contra el código | 0 | sí, `37d280e` | INTEGRADA (tras FF) | Mergeada en P por `a14dd2d`. |
| `refs/heads/t-002-inventario-de-alcance-de-los-sprints-6-a` | `92372d7` 2026-10-10 T-002: Inventario de alcance de los Sprints 6 a 9 contra el código | 0 | sí, `92372d7` | INTEGRADA (tras FF) | Mergeada en P por `564e779`. |
| `refs/heads/t-003-recuperar-del-historial-git-los-document` | `cafd6ad` 2026-10-10 T-003: Recuperar del historial git los documentos eliminados de docs/ | 0 | sí, `cafd6ad` | INTEGRADA (tras FF) | Mergeada en P por `71dc17f`. |
| `refs/heads/chore/memoria-proyectos-setup` | `f98ded2` 2026-10-10 chore: set up memoria-proyectos context and add pixel art icons | 0 | sí, `f98ded2` | INTEGRADA (tras FF) | Mergeada en P por `1cb7551`. |
| `refs/heads/feat/configurable-server-port` | `d01892d` 2026-08-10 feat(config): make server port configurable via SERVER_PORT env var | 0 | sí, `d01892d` | INTEGRADA | Ya contenida en `prod`. |
| `refs/remotes/origin/feat/configurable-server-port` | `d01892d` (ídem) | 0 | sí, `d01892d` | INTEGRADA | Ídem. |
| `refs/heads/feat/reporte-productos-presentacion` | `f547188` 2026-08-26 feat(reportes): add Presentación column to productos export | 0 | sí, `f547188` | INTEGRADA | Ya contenida en `prod` (PR #6). |
| `refs/remotes/origin/feat/reporte-productos-presentacion` | `f547188` (ídem) | 0 | sí, `f547188` | INTEGRADA | Ídem. |
| `refs/heads/fix/reporte-productos-lazy-init` | `98c6ce9` 2026-08-26 fix(reportes): eager-fetch articuloMaestro to avoid LazyInitializationException | 0 | sí, `98c6ce9` | INTEGRADA | Ya contenida en `prod`. |
| `refs/remotes/origin/fix/reporte-productos-lazy-init` | `98c6ce9` (ídem) | 0 | sí, `98c6ce9` | INTEGRADA | Ídem. |
| `refs/heads/fix/root-redirect-and-contabilidad-preauthorize` | `0b6bb94` 2026-08-10 fix(contabilidad): fix @PreAuthorize NPE and always-denied report endpoints | 0 | sí, `0b6bb94` | INTEGRADA | Ya contenida en `prod`. |
| `refs/remotes/origin/fix/root-redirect-and-contabilidad-preauthorize` | `0b6bb94` (ídem) | 0 | sí, `0b6bb94` | INTEGRADA | Ídem. |
| `refs/heads/fix/ubicacion-cr-duplicates` | `85d2a79` 2026-08-06 fix(configuracion): dedupe cat_canton_cr/cat_distrito_cr and add missing unique constraints | 0 | sí, `85d2a79` | INTEGRADA | Ya contenida en `prod`. |
| `refs/remotes/origin/fix/ubicacion-cr-duplicates` | `85d2a79` (ídem) | 0 | sí, `85d2a79` | INTEGRADA | Ídem. |
| `refs/heads/stage` | `73c3642` 2026-08-03 merge: bring prod up to date into stage | 9 (3 sin merges) | sí, `e1aa7eb` | MANTENER | Rama de entorno pre-prod. Sus 3 commits propios (`f22dc6a`, `df3d958`, `fdc008b`) solo tocan `docker-compose.yml` (servicios `mysql-stage`/`app-stage`, puerto 9091) y son configuración específica de stage; ninguno toca `src/`. Está 19 commits detrás de P. |
| `refs/remotes/origin/stage` | `73c3642` (ídem) | 9 (3 sin merges) | sí, `e1aa7eb` | MANTENER | Igual a `stage` local. |
| `refs/tags/legacy/main` (tag ligero) | `9678c55` 2026-07-18 docs(api): fix response schemas for guardar and actualizar-estado | 196 (193 sin merges + 3 merges) | sí, solo `3804c67` ("Initial commit", 2025-10-13, contiene únicamente `LICENSE`) | ARCHIVAR | Historia paralela reescrita de `prod`: los 147 commits que tocan `src/` tienen contraparte en P (ver §3). Contiene credenciales SMTP en texto plano en `application.yml` (ver §3.4). |

Observaciones del inventario:

- No existe `refs/heads/main` ni `refs/remotes/origin/main` en este clon. No se verificó el remoto por red (`fetch`/`ls-remote` fuera del alcance).
- No existen `refs/stash`, `ver-2` ni ramas `copilot/...` en ningún namespace local.
- Una vez que `prod` haga fast-forward a la punta de t-004 (descendiente de `1cb7551`), las 5 ramas `t-001`, `t-002`, `t-003`, `chore/memoria-proyectos-setup` y `t-004-…` quedan INTEGRADAS (`git rev-list --count prod..<rama>` = 0).

### 1.1 Commits sueltos señalados en tareas previas

| Commit | Asunto | Refs que lo contienen (`for-each-ref --contains`) | Observación |
|---|---|---|---|
| `308ac30` 2026-05-07 | creating formatting in docs | solo `legacy/main` | Sin equivalente de parche en P (`git cherry` = `+`). Solo afecta `docs/`; tratado en T-003. |
| `cf4ae8a` 2026-07-08 | cleaning docs | solo `legacy/main` | No está huérfano: lo contiene el tag. Mismo asunto y fecha de autor que `025c1b3` (en P), pero no es idéntico: patch-id distinto, 760 vs. 763 archivos. Es la variante reescrita del mismo commit. |
| `40b7b5e` 2025-10-13 | feat: Add user management forms and list views | solo `legacy/main` | Commit raíz (sin padres) de la línea legacy; su contraparte en P es la raíz `c3d4e3d`. |

## 2. Estado de `main` frente a `prod` (criterio 3b)

- **`main` ya no existe** como rama local ni como rama remota rastreada. Su último estado se conserva en el tag ligero `legacy/main` → `9678c55b0f0ecf6bcd045ec08c76b1fcf40bd1c5` (2026-07-18 10:51:34 -0600, "docs(api): fix response schemas for guardar and actualizar-estado").
- **Ahead/behind** (`git rev-list --left-right --count A...legacy/main`):

  | Contra | Solo en la base | Solo en `legacy/main` |
  |---|---|---|
  | `prod` (`e0a1481`) | 233 | 196 |
  | P (`1cb7551`) | 241 | 196 |

- **Ancestro común**: `git merge-base` devuelve `3804c67` ("Initial commit", 2025-10-13), que solo contiene `LICENSE`. Corrección a la nota de memoria: las historias **no** son totalmente no relacionadas; comparten ese único commit inicial y divergen inmediatamente después. El repositorio tiene tres raíces (`git rev-list --max-parents=0 --all`): `3804c67` (compartida), `c3d4e3d` (línea de `prod`) y `40b7b5e` (línea de `legacy/main`). Ambas líneas se unen a `3804c67` mediante un merge "feat: Merge ver-2 into main — Sprint 5 complete…" con hashes distintos: `0ebd8bd` en P y `6f72d81` en `legacy/main`.
- **Naturaleza de la divergencia**: `legacy/main` es una copia reescrita de la misma historia. De sus 193 commits sin merges fuera de P, `git cherry` marca 172 como equivalentes por parche. Además, `legacy/main` contiene merges internos que reintroducen la línea pre-modular (`7ad784d` "Merge branch 'main' of github.com:…" y `a9164db` "Merge branch 'prod'"), por lo que su árbol final mezcla `modules/` con los paquetes antiguos `controllers/`, `models/`, `services/`, etc.

## 3. Commits fuera de prod que tocan `src/` (criterio 4)

Comando: `git log --all --not P --no-merges --format='%h %ad %s' --date=short -- src`

- **Total: 147 commits** (2025-10-13 a 2026-07-17). Contra `prod` (`e0a1481`) el mismo comando devuelve 148; la diferencia es `f98ded2` (chore/memoria-proyectos-setup), que ya está en P.
- **Ref que los contiene: solo `refs/tags/legacy/main`, los 147** (verificado con `for-each-ref --contains` en cada uno). `stage` aporta 0 commits en `src/`; las demás ramas tienen 0 commits fuera de P.
- **Equivalencia por parche**: 133 de 147 tienen un commit con el mismo patch-id en P. Los 14 restantes tienen en P un commit con **asunto idéntico** y difieren solo en archivos fuera del código (ver §3.2).

### 3.1 Resumen por área

| Ref | Área | Nº commits | Rango de fechas | Hashes de ejemplo | ¿Contenido en P? | Propuesta | Justificación |
|---|---|---|---|---|---|---|---|
| legacy/main | facturacion | 35 | 2025-12-27 – 2026-07-17 | `93230ba`, `335101e`, `4be13f1` | Sí: 34 por patch-id; `15c7665` ≈ `42d5f16` | ARCHIVAR | Mismos parches; la única diferencia de `15c7665` es que su par en P toca además `docker-compose.yml` y `application.yml`. |
| legacy/main | transversal (≥4 áreas) | 28 | 2025-10-13 – 2026-07-17 | `40b7b5e`, `66864d3`, `bde7de3` | Sí: 26 por patch-id; `40b7b5e` ≈ `c3d4e3d`, `ec0f37e` ≈ `f308763` | ARCHIVAR | Los 2 no equivalentes solo difieren en el blob de `application.yml`. |
| legacy/main | configuracion | 16 | 2025-12-27 – 2026-07-12 | `bbb8908`, `dd07503`, `13bafab` | Sí: 15 por patch-id; `13bafab` ≈ `c50d14d` | ARCHIVAR | `c50d14d` (P) además limpia `application.yml`; P es superconjunto. |
| legacy/main | seguridad | 15 | 2025-12-28 – 2026-07-14 | `6b3dbc7`, `0ab7a14`, `5d0618f` | Sí: 15/15 por patch-id | ARCHIVAR | Duplicados exactos de parche. |
| legacy/main | monolito (pre-modular) | 10 | 2025-10-13 – 2025-12-27 | `ae6088b`, `65a2a82`, `27f93d3` | Sí: 2 por patch-id; 8 con asunto idéntico en P | ARCHIVAR | Difieren solo en `application.yml`, `logs/*.log`, docs y archivos temporales (`temp_*.txt`). P migró después estos paquetes a `modules/` (§3.3). |
| legacy/main | cliente | 9 | 2026-05-07 – 2026-06-20 | `6a25b61`, `73ac9f0`, `c9db13d` | Sí: 9/9 por patch-id | ARCHIVAR | Duplicados exactos de parche. |
| legacy/main | config-app (`application*.yml`) | 7 | 2025-12-28 – 2026-07-11 | `cf4e683`, `81ea593`, `4e6d19a` | Sí: 5 por patch-id; `81ea593` ≈ `14cc645`, `4e6d19a` ≈ `378ae5a` | ARCHIVAR | Diferencias solo en docs y `application.yml`; el `application.yml` de P es el vigente y no tiene credenciales embebidas. |
| legacy/main | shared (incluye filter-bar) | 7 | 2025-12-28 – 2026-07-17 | `6bd4352`, `cd6263e`, `f6797cc` | Sí: 7/7 por patch-id | ARCHIVAR | Ver §3.5 (filter-bar). |
| legacy/main | whatsapp | 6 | 2025-12-27 – 2026-06-20 | `2a726d0`, `73c6ecd`, `bad2c50` | Sí: 6/6 por patch-id | ARCHIVAR | Duplicados exactos de parche. |
| legacy/main | producto, proveedores, inventario, notificacion, reportes, contabilidad, config-java | 14 (3+3+2+2+2+1+1) | 2025-12-27 – 2026-07-15 | `465a78f`, `2800782`, `3aacb98`, `196f3b5` | Sí: 14/14 por patch-id | ARCHIVAR | Duplicados exactos de parche. |
| **Total** | | **147** | 2025-10-13 – 2026-07-17 | | 133 por patch-id + 14 con contraparte en P | **ARCHIVAR todo** | Ningún grupo aporta código ausente en P. |

### 3.2 Los 14 commits sin equivalencia de parche

Comparación de cada commit legacy con su contraparte en P (mismo asunto): archivos que solo toca uno de los dos y archivos comunes con blob resultante distinto.

| Legacy | Contraparte en P | Diferencia observada |
|---|---|---|
| `40b7b5e` | `c3d4e3d` | Blob distinto solo en `application.yml`. |
| `ae6088b` | `c3d4e3d` | El de P toca además scripts SQL en `docs/base de datos/`. |
| `65a2a82` | `c9b8648` | Legacy toca `EJECUTAR_MIGRACION.sql` y `temp_*.txt` en la raíz; P toca docs. |
| `53076dd` | `c9b8648` | Blobs distintos en 2 docs, `logs/whats-orders-manager.log` y `application.yml`. |
| `f8d8ce6` | `ccff2bb` | P toca además `application.yml`. |
| `d86a34c` | `ccff2bb` | Blobs distintos en `logs/…log` y `application.yml`. |
| `27f93d3` | `2fbf5a8` | P toca además `config/WhatsAppRestConfig.java` y `application.yml` (P es superconjunto). |
| `06d64d9` | `2fbf5a8` | Blobs distintos en `logs/…log` y `application.yml`. |
| `bcbb71b` | `f308763` | Legacy modifica además controladores/modelos pre-modulares (`ClienteController`, `Cliente`, `Producto`…), que P eliminó más tarde al migrar a `modules/`; P toca además SQL de `docs/`. |
| `ec0f37e` | `f308763` | Blob distinto solo en `application.yml`. |
| `81ea593` | `14cc645` | Blobs distintos en 2 docs de `docs/configuracion/`. |
| `4e6d19a` | `378ae5a` | Blob distinto solo en `application.yml`. |
| `13bafab` | `c50d14d` | P toca además `application.yml`. |
| `15c7665` | `42d5f16` | P toca además `docker-compose.yml` y `application.yml`. |

### 3.3 Comparación de árboles finales (`legacy/main` vs. P, solo `src/`)

`git diff --name-status legacy/main P -- src`: 495 archivos (240 D, 111 M, 144 A).

- **240 archivos solo en `legacy/main`**: son los paquetes pre-modulares (`controllers/`, `models/`, `repositories/`, `services/`, `enums/`, `events/`, `listeners/`, `schedulers/`, `util/`, `config/SecurityConfig.java`…) y `templates/`/`static/` fuera de `modules/`/`shared/`. **Los 240 existieron en P y P los eliminó deliberadamente** en la migración modular de 2025-12-27/28. Principales commits de borrado: `cf34124` (51 archivos, "Reorganizar templates en estructura modular"), `c54d300` (32), `f69d81e` (31, módulo Seguridad), `1b75665` (23, JS), `697ad37` (17), `537bce1` (17), `dc5520e` (16), `8cc9984` (16), `dbb7db6` (15, Facturación), `a6bd284` (13, CSS) y otros. Ningún archivo bajo `modules/` o `shared/` está solo en `legacy/main`.
- **111 archivos con contenido distinto**: en 110, el blob de `legacy/main` aparece en algún commit del historial de P (versión anterior que P siguió evolucionando). La única excepción es `src/main/resources/application.yml`.
- **144 archivos solo en P**: trabajo posterior a 2026-07-18 o eliminado en legacy (p. ej. `modules/nomina`, `modules/rrhh`, `config/FlywayConfig.java`, `config/RootRedirectConfig.java`).

### 3.4 `application.yml` (única versión de legacy no presente en P)

Diferencias de `legacy/main` frente a P: puerto fijo `9090` (P: `${SERVER_PORT:15808}`), sin `spring.config.import` de `.env`, sin bloque `flyway`, `ddl-auto: update` (P: `validate`) y **valores por defecto de usuario y contraseña SMTP de Gmail escritos en claro** (P: `${EMAIL_USERNAME:}` y sin valor por defecto). La versión de P es la correcta; la de legacy no debe rescatarse. Según `git log -S` sobre `application.yml`, ese valor por defecto de la contraseña aparece en `40b7b5e` (raíz de la línea legacy) y nunca se elimina en `legacy/main`. En la historia de P no aparece en ningún commit, así que solo se alcanza desde el tag `legacy/main`.

### 3.5 Veredicto filter-bar (`6bd4352`)

- `6bd4352` (2026-07-17, solo en `legacy/main`) añade `templates/shared/fragments/{filter-bar,detail-field,status-badge,table-card}.html`.
- P contiene `db05680` con asunto idéntico y **el mismo patch-id** (`git cherry` lo marca como `-`).
- Los 4 archivos existen en P en la misma ruta (`git ls-tree -r P --name-only | rg -i filter`), y `git diff 6bd4352 P -- <archivo>` no muestra diferencias en ninguno de los cuatro: **contenido idéntico**.
- P ya usa el fragmento: `git grep -i filter-bar P -- src` lo encuentra en `modules/contabilidad/asientos/listar.html`, `modules/contabilidad/cuentas/listar.html` y `modules/facturacion/facturas.html`.
- **Conclusión: el grupo filter-bar ya existe en P. No hay nada que rescatar.**

## 4. Recomendación (criterio 5)

**No se ejecutó ninguna de estas acciones.** Son propuestas para revisión.

Rescates: **ninguno**. No hay cherry-picks ni merges necesarios: los 147 commits de `src/` fuera de P ya están en P por parche o tienen contraparte en P, y el único contenido de código exclusivo de legacy (`application.yml`) es inferior a la versión de P y contiene credenciales.

Pasos sugeridos, en orden:

1. Avanzar `prod` por fast-forward a la punta de `t-004-higiene-de-ramas-integrar-ramas-de-audit` (`1cb7551` más el commit que agrega este informe) y publicarlo en `origin/prod`.
2. Eliminar las ramas ya integradas (locales y, donde existan, remotas):
   - Tras el paso 1: `t-001-auditoria-del-estado-real-del-sprint-5-c`, `t-002-inventario-de-alcance-de-los-sprints-6-a`, `t-003-recuperar-del-historial-git-los-document`, `chore/memoria-proyectos-setup`, `t-004-higiene-de-ramas-integrar-ramas-de-audit` (solo locales).
   - Ya integradas hoy: `feat/configurable-server-port`, `feat/reporte-productos-presentacion`, `fix/reporte-productos-lazy-init`, `fix/root-redirect-and-contabilidad-preauthorize`, `fix/ubicacion-cr-duplicates` (locales y `origin/`).
3. `stage`: mantenerla. Actualizarla desde `prod` (está 19 commits detrás de P) conservando sus 3 ajustes de `docker-compose.yml`, o mover esa configuración a un override (`docker-compose.stage.yml`/`.env`) para que deje de divergir.
4. `legacy/main`: ARCHIVAR. Dos opciones con distinto coste:
   - Conservar el tag (opción por defecto, coste cero): mantiene trazabilidad, incluidos `308ac30` y `cf4ae8a`.
   - Eliminar el tag local y remoto (si está publicado): reduce ruido y deja inalcanzable la contraseña SMTP en este repositorio, que en P no existe. Contras: se pierde trazabilidad, y cualquier clon o fork existente conserva el secreto, así que la revocación del paso 5 sigue siendo necesaria.
5. Seguridad (independiente del triage): confirmar que la contraseña de aplicación SMTP de Gmail que aparece en `application.yml` de `legacy/main` fue revocada. Si no, revocarla. **Verificado con `git ls-remote --tags origin`: el tag `legacy/main` (`9678c55`) está publicado en GitHub**, así que la contraseña se puede obtener desde el remoto. La revocación es prioritaria. Si se decide eliminar el tag, hay que hacerlo también en `origin`.

## Apéndice A. Commits fuera de P que tocan `src/`, agrupados por ref

### `refs/tags/legacy/main` (147 commits)

"≡ X" = mismo patch-id que el commit X de P. "~ X (no equiv.)" = contraparte en P con asunto idéntico, parche distinto (ver §3.2).

| Fecha | Hash | Área | Equivalente en P | Asunto |
|---|---|---|---|---|
| 2025-10-13 | 40b7b5e | transversal | ~ c3d4e3d (no equiv.) | feat: Add user management forms and list views |
| 2025-10-13 | ae6088b | monolito | ~ c3d4e3d (no equiv.) | feat: Add user management forms and list views |
| 2025-10-26 | 53076dd | monolito | ~ c9b8648 (no equiv.) | feat: Add product and sales report templates with filtering and export functionality |
| 2025-10-26 | 65a2a82 | monolito | ~ c9b8648 (no equiv.) | feat: Add product and sales report templates with filtering and export functionality |
| 2025-10-28 | d86a34c | monolito | ~ ccff2bb (no equiv.) | adding changes to MensajeWhatsApp and PlantillaWhatsApp models and their repositories |
| 2025-10-28 | f8d8ce6 | monolito | ~ ccff2bb (no equiv.) | adding changes to MensajeWhatsApp and PlantillaWhatsApp models and their repositories |
| 2025-11-10 | 06d64d9 | monolito | ~ 2fbf5a8 (no equiv.) | Add WhatsApp templates and messages management functionality |
| 2025-11-10 | 27f93d3 | monolito | ~ 2fbf5a8 (no equiv.) | Add WhatsApp templates and messages management functionality |
| 2025-12-27 | 07be17b | notificacion | ≡ dc5520e | feat: Migrar módulo Notificación completo |
| 2025-12-27 | 2a726d0 | whatsapp | ≡ 8cc9984 | feat: Migrar módulo WhatsApp completamente |
| 2025-12-27 | 335101e | facturacion | ≡ dbb7db6 | feat: Migrar módulo Facturación completamente |
| 2025-12-27 | 66864d3 | transversal | ≡ 537bce1 | feat: Completar migración modular - Módulo Shared y DTOs (18 archivos) |
| 2025-12-27 | 70ca6a8 | transversal | ≡ c54d300 | feat: Expandir módulos con archivos adicionales (32 archivos) |
| 2025-12-27 | 76702c0 | transversal | ≡ f69d81e | feat: Migrar módulo Seguridad completo |
| 2025-12-27 | 7fcb9e3 | monolito | ≡ 2756b86 | feat: Migrar módulo Cliente completamente |
| 2025-12-27 | 8ad2453 | reportes | ≡ 30a02e6 | feat: Migrar módulo Reportes completamente |
| 2025-12-27 | 93230ba | facturacion | ≡ 2c8cc52 | refactor: Organizar ServiceImpl en subcarpeta impl/ |
| 2025-12-27 | a277d40 | monolito | ≡ bab3053 | feat: Migrar módulo Producto completamente |
| 2025-12-27 | bbb8908 | configuracion | ≡ 697ad37 | feat: Migrar módulo Configuración completamente |
| 2025-12-27 | bcbb71b | monolito | ~ f308763 (no equiv.) | feat: Crear estructura base para refactorización modular |
| 2025-12-27 | ec0f37e | transversal | ~ f308763 (no equiv.) | feat: Crear estructura base para refactorización modular |
| 2025-12-28 | 05a6603 | transversal | ≡ a6bd284 | refactor(resources): Reorganizar archivos CSS en estructura modular |
| 2025-12-28 | 0ab7a14 | seguridad | ≡ 645b164 | fix(templates): Eliminar referencia a footer inexistente en templates de roles |
| 2025-12-28 | 557d8ff | seguridad | ≡ 5cf6592 | fix(seguridad): Agregar propiedad password a UsuarioAdminDTO para formularios |
| 2025-12-28 | 5d0618f | seguridad | ≡ d2688dc | fix(templates): Corregir referencia a footer en templates de roles |
| 2025-12-28 | 658cd56 | shared | ≡ 6f7c9cf | debug(navbar): Agregar más logs para diagnosticar eventos del dropdown |
| 2025-12-28 | 65f9168 | transversal | ≡ 1b15d1b | refactor: Reorganizar estructura shared al nivel correcto |
| 2025-12-28 | 6b3dbc7 | seguridad | ≡ 22bfd55 | fix(templates): Agregar scripts de Bootstrap a formulario de roles |
| 2025-12-28 | 6da6d67 | transversal | ≡ 1b75665 | refactor(resources): Reorganizar archivos JavaScript en estructura modular |
| 2025-12-28 | 73c6ecd | whatsapp | ≡ 418921d | fix(whatsapp): Corregir nombre de propiedad createDate a fechaCreacion |
| 2025-12-28 | 8059488 | shared | ≡ 76345fb | fix(navbar): Forzar visibilidad del dropdown con !important |
| 2025-12-28 | 816227e | shared | ≡ bf03a97 | debug(navbar): Agregar logs para diagnosticar problema del botón logout |
| 2025-12-28 | 884b30e | shared | ≡ 2a71c95 | debug(navbar): Agregar logs de debugging al dropdown del navbar |
| 2025-12-28 | a549189 | transversal | ≡ cf34124 | refactor(resources): Reorganizar templates en estructura modular |
| 2025-12-28 | bad2c50 | whatsapp | ≡ 875a8a2 | fix: Eliminar archivos duplicados en carpetas antiguas |
| 2025-12-28 | cd6263e | shared | ≡ cee656d | refactor(navbar): Migrar dropdown personalizado a Bootstrap dropdown nativo |
| 2025-12-28 | cf4e683 | config-app | ≡ 29e4c7d | feat(performance): Implementar optimizaciones de memoria |
| 2025-12-28 | f2da4f3 | transversal | ≡ df60b74 | fix(templates): Corregir referencias th:replace en templates |
| 2026-01-05 | 81ea593 | config-app | ~ 14cc645 (no equiv.) | Add consolidated completed tasks document for all sprints in ERP Spring Manager project |
| 2026-01-29 | 7f4626a | transversal | ≡ 1fc58af | feat: Add report generation for daily cash report and modular components |
| 2026-05-07 | 49f28ef | transversal | ≡ 4b30ff6 | feat: Implement UbicacionService for managing locations in Costa Rica |
| 2026-05-07 | 4be13f1 | facturacion | ≡ 968162e | feat: Refactor factura management and enhance UI for line items |
| 2026-05-07 | 6a25b61 | cliente | ≡ 7f188f1 | feat: Actualizar valores por defecto en formularios y agregar botón para agregar factura en la vista de clientes |
| 2026-05-09 | 73ac9f0 | cliente | ≡ b222339 | fix: validate OtrasSenas minLength in client form |
| 2026-05-09 | 94c48da | transversal | ≡ 6182ddf | Refactor project references from "WhatsApp Orders Manager" to "ERP Orders Manager" across multiple files, including service implementations, email templates, JavaScript files, CSS styles, and HTML templates. This change standardizes the application branding and ensures consistency throughout the codebase. |
| 2026-05-09 | f6a0397 | transversal | ≡ 4adb231 | fix: complete electronic invoicing pipeline for Hacienda CR v4.4 |
| 2026-05-10 | 4e6d19a | config-app | ~ 378ae5a (no equiv.) | Actualizar la dirección IP de MySQL en la documentación y archivos de configuración para reflejar el nuevo servidor. |
| 2026-05-10 | 706d166 | seguridad | ≡ 4a97cb7 | fix: set telefono and rol String in DataInitializer for Spring Security compatibility |
| 2026-05-10 | 7efb58c | seguridad | ≡ 00f24c0 | feat: add database seeding system for fresh deployments |
| 2026-05-10 | 9439439 | seguridad | ≡ 631f66f | Update application to use port 9090 instead of 8080 across multiple documentation and code files |
| 2026-05-10 | 9b82665 | seguridad | ≡ c8d9903 | feat: agregar número de teléfono al usuario de prueba en DataInitializer |
| 2026-05-10 | be7eed4 | config-app | ≡ f7f0275 | fix: ajustar la configuración del servidor en application.yml para incluir el puerto 9090 |
| 2026-05-11 | d8d0267 | seguridad | ≡ e5cdb2d | feat: agregar ConfiguracionEmpresaRepository y crear configuración predeterminada en DataInitializer |
| 2026-05-11 | dd07503 | configuracion | ≡ 7d65b07 | fix: resolve read-only transaction error and seed default entities on startup |
| 2026-06-06 | 917a9fb | seguridad | ≡ 80cb574 | feat: agregar configuración de logs, actualizar dependencias y permitir healthcheck en Docker |
| 2026-06-08 | 1531e80 | config-app | ≡ c90f3a8 | fix: trust Cloudflare proxy headers and set public base URL |
| 2026-06-08 | f40294c | seguridad | ≡ 3da6a7b | feat: actualizar datos de inicialización de usuario y cliente, modificar nombre de campo en registro |
| 2026-06-10 | 41af29b | config-java | ≡ 3f5824a | fix: agregar encabezados User-Agent y Accept al RestTemplate en HttpClientConfig |
| 2026-06-10 | 4d68135 | configuracion | ≡ b1c6898 | fix: agregar registro de depuración para la URL construida en HaciendaConsultaServiceImpl |
| 2026-06-10 | a41b069 | configuracion | ≡ 1fad736 | fix: corregir registro de depuración para la URL construida en HaciendaConsultaServiceImpl |
| 2026-06-10 | d8b35b2 | configuracion | ≡ 0873fcf | fix: agregar encabezados por defecto al RestClient en HaciendaConsultaServiceImpl |
| 2026-06-11 | 00e9913 | facturacion | ≡ c6851e5 | fix(facturas): include FE fields in new invoice creation payload |
| 2026-06-11 | 019190a | facturacion | ≡ 8aab5ed | feat(facturas): add PDF download button to mobile card view |
| 2026-06-11 | 0c6572f | facturacion | ≡ 8388424 | fix(facturas): allow delete with lines, block on payments or entregado status |
| 2026-06-11 | 1823b63 | facturacion | ≡ e095587 | fix(facturas): only increment invoice counter on create, not on update |
| 2026-06-11 | 2a296b9 | facturacion | ≡ bc9b94f | fix(facturas): corregir referencia de URL en la función confirmarEliminacion |
| 2026-06-11 | 5784bf2 | cliente | ≡ 2b05131 | feat(clientes): agregar botón para crear factura desde la lista de clientes |
| 2026-06-11 | 631b91e | facturacion | ≡ a6fddcb | fix(facturas): eliminar líneas de factura al eliminar factura |
| 2026-06-11 | 70b6fdc | facturacion | ≡ c02951b | fix(facturas): recalculate subtotal and total after saving line items |
| 2026-06-11 | c9db13d | cliente | ≡ c320725 | fix(clientes): eliminar div innecesario en el grupo de botones de acciones |
| 2026-06-11 | f71569e | cliente | ≡ 7a42d1e | fix(clientes): ajustar estilos de tabla y mejorar la disposición de las acciones |
| 2026-06-11 | fc29ed6 | facturacion | ≡ 35474eb | feat(facturas): redirect after edit, persist fields, PDF download |
| 2026-06-11 | fca7789 | facturacion | ≡ 0d0059f | fix(pdf): calculate subtotal from line items when factura total is null |
| 2026-06-12 | 28f19fb | facturacion | ≡ 46f8b68 | refactor(pagos): thin controller — move business logic to service layer |
| 2026-06-12 | 412b59d | configuracion | ≡ b47c6cd | refactor(configuracion): clean up controller and push business logic to service layer |
| 2026-06-12 | 5d80a12 | facturacion | ≡ 6c42528 | fix(facturas): handle nullable Boolean entregado in Thymeleaf SpEL expressions |
| 2026-06-12 | 63074fd | configuracion | ≡ 8a04300 | refactor(configuracion): extract mapping to mapper components and add saveOrUpdate to services |
| 2026-06-12 | 6ee2a5b | cliente | ≡ 086ddc4 | refactor(cliente): clean up controller and fix plain-text password in service |
| 2026-06-12 | 93624aa | configuracion | ≡ a849bcc | refactor(configuracion): replace in-controller mapping with mapper components across REST controllers |
| 2026-06-12 | 9e5c1c9 | configuracion | ≡ efa32ea | fix(configuracion): persist CR electronic invoicing fields in actualizarConfiguracion |
| 2026-06-12 | ec9e7b3 | facturacion | ≡ d5c44f8 | refactor(facturas): thin controller — move business logic to service layer |
| 2026-06-16 | 234ac51 | facturacion | ≡ 100463c | feat(certificados): agregar funcionalidad para subir certificados .p12 y .pfx |
| 2026-06-16 | 26082c6 | facturacion | ≡ 550849d | feat(facturacion): agregar funcionalidad de búsqueda en el dropdown de productos |
| 2026-06-16 | 40d828a | transversal | ≡ 9f76aea | refactor(clientes): decouple Cliente from Usuario entity |
| 2026-06-16 | 7594001 | facturacion | ≡ b0906cd | feat(configuracion-hacienda): agregar encabezados CSRF al subir certificados |
| 2026-06-16 | d9ce7a6 | facturacion | ≡ 148e4df | feat(certificados): mejorar manejo de nombres de archivos y errores al guardar certificados |
| 2026-06-16 | f020e9c | facturacion | ≡ b96db7e | feat(facturacion): agregar selección de producto con dropdown en la tarjeta de línea |
| 2026-06-17 | 3d1df89 | facturacion | ≡ 2b52a45 | fix(email): compress XML attachments into ZIP to reduce spam score |
| 2026-06-17 | 5bbfca7 | facturacion | ≡ 3e7e671 | fix(hacienda): propagate respuesta-xml through DTO and persist xml_respuesta on comprobante |
| 2026-06-17 | 7839aca | facturacion | ≡ 1106618 | fix(comprobantes): add CSRF token to email fetch and replace native dialogs with SweetAlert2 |
| 2026-06-17 | 87348dd | configuracion | ≡ daee294 | feat(empresa): agregar sello PYME, texto legal, actividad económica y cuentas bancarias |
| 2026-06-17 | a8bd2ea | configuracion | ≡ 0acab72 | feat(configuracion-empresa): agregar upload de logo, favicon y sello con volúmenes Docker |
| 2026-06-17 | b8e13dd | facturacion | ≡ ee0c5d4 | fix(hacienda): add xmlRespuesta to JPQL constructor expression in RespuestaHaciendaRepository |
| 2026-06-17 | c8f9275 | facturacion | ≡ 7f9aa31 | feat(comprobantes): implement enviarPorEmail with XML and PDF attachments |
| 2026-06-17 | c9f7f2e | cliente | ≡ 38447e7 | feat(cliente): agregar campo email en formulario y edición |
| 2026-06-17 | d86f28b | facturacion | ≡ d9f69a1 | fix(factura): corregir valores por defecto de tipo cambio y plazo crédito |
| 2026-06-17 | e5a269f | facturacion | ≡ 671f61e | feat(factura-pdf): rediseñar layout PDF con referencia ASVEQUI |
| 2026-06-17 | f6797cc | shared | ≡ 053e892 | feat(email): add multi-attachment support to EmailService |
| 2026-06-20 | 08e1302 | cliente | ≡ 5beea3f | feat(clientes): agregar endpoint para listar todos los clientes y actualizar modelo WebhookLog |
| 2026-06-20 | 1a025b4 | whatsapp | ≡ 6a57e77 | feat(conversaciones): renombrar referencias de telefonoCliente a telefonoVendedor en el CRUD de conversaciones |
| 2026-06-20 | 3c16502 | whatsapp | ≡ d694871 | feat(whatsapp): agregar endpoint para registrar logs de webhook manualmente |
| 2026-06-20 | 44a06e5 | seguridad | ≡ ddc3ba2 | feat(auth): implementar endpoint de inicio de sesión y DTOs para LoginRequest y LoginResponse |
| 2026-06-20 | 71c6d54 | whatsapp | ≡ 1d6e46f | feat(conversaciones): agregar CRUD para conversaciones de pedidos por WhatsApp |
| 2026-06-20 | c91aae8 | cliente | ≡ f446477 | Add OpenAPI specification for Orders Manager REST API |
| 2026-06-26 | 941fc61 | seguridad | ≡ 7e5fa31 | feat(usuarios): mejorar manejo de contraseñas en el formulario de administración |
| 2026-06-28 | 465a78f | producto | ≡ a81377f | feat(productos): agregar entidad ArticuloMaestro para agrupar variantes por presentacion |
| 2026-06-28 | f75bbb8 | configuracion | ≡ b8c5d4d | feat(presentaciones): agregar controlador REST para manejar presentaciones |
| 2026-06-29 | b234bf3 | producto | ≡ 35747e3 | feat(articulo_maestro): hacer que la descripción sea única en la entidad ArticuloMaestro |
| 2026-07-03 | 65663c2 | facturacion | ≡ 9680d60 | fix(facturacion): usar ConfiguracionEmpresa en el PDF de factura |
| 2026-07-03 | 7b1f71e | configuracion | ≡ db0e118 | feat(configuracion): agregar descripcionActividad a ConfiguracionEmpresa |
| 2026-07-03 | c58da42 | configuracion | ≡ 3a6c050 | fix(hacienda): serializar estaAlDia y estaInscrito en la consulta de Hacienda |
| 2026-07-05 | 073e4d5 | facturacion | ≡ e0aefb6 | feat(facturacion): agregar filtros de búsqueda por fechas, estado de pago y estado de comprobante electrónico |
| 2026-07-07 | 06f88d2 | reportes | ≡ 35fe33a | feat(reportes): mejorar visualización y estadísticas en reportes, agregar estilos y optimizar datos |
| 2026-07-07 | 2c57f59 | config-app | ≡ af1b14a | feat(configuracion): importar configuración opcional desde archivo .env en perfil prod |
| 2026-07-07 | 94f7adb | producto | ≡ 25fb8e4 | feat(producto): mejorar la visualización de artículos con vista de tarjetas para móviles y tabla responsiva fix(contabilidad): agregar control de acceso a la vista de contabilidad según permisos |
| 2026-07-08 | 963d7e9 | transversal | ≡ 44e1cf0 | feat(registro): agregar campo de correo electrónico opcional y validaciones en AuthController |
| 2026-07-11 | 208fa71 | facturacion | ≡ 0fcfe19 | refactor(templates): extraer KPI stat cards a fragment reutilizable |
| 2026-07-11 | 2e598e5 | transversal | ≡ b7877f6 | refactor(templates): extraer alerts dismissibles a fragment reutilizable |
| 2026-07-11 | 469d782 | transversal | ≡ 6d39da0 | refactor(templates): extraer page header y breadcrumb a fragments reutilizables |
| 2026-07-11 | 6d1cb41 | transversal | ≡ 709837e | refactor(templates): extraer botón volver a fragment reutilizable |
| 2026-07-11 | 70483df | transversal | ≡ fcedbbf | refactor(frontend): consolidar SweetAlert2 y optimizar carga de CSS |
| 2026-07-11 | 75225c7 | transversal | ≡ 85357f5 | refactor(templates): extraer paginación a fragment reutilizable |
| 2026-07-11 | 7c5e8f9 | notificacion | ≡ f5a9cd5 | refactor(frontend): migrar fetch nativo a httpGet en notificaciones y ubicacion |
| 2026-07-11 | 8887132 | transversal | ≡ f40cb77 | refactor(templates): extraer search bar a fragment reutilizable |
| 2026-07-11 | 89df59c | transversal | ≡ 5af5bc9 | refactor(templates): extraer empty state a fragment reutilizable |
| 2026-07-11 | 919d1bf | transversal | ≡ ef89365 | refactor(frontend): consolidar SweetAlert2 y migrar fetch nativo a httpGet |
| 2026-07-11 | edd86f0 | config-app | ≡ a01e1e3 | config: deshabilitar mail health indicator |
| 2026-07-12 | 13bafab | configuracion | ~ c50d14d (no equiv.) | fix(security): eliminar credenciales SMTP hardcodeadas y proteger smtpPassword |
| 2026-07-12 | 515ea5b | seguridad | ≡ 8f5e22f | fix(security): restringir /api/configuracion/** a rol ADMIN |
| 2026-07-12 | 753ec0f | transversal | ≡ b17507e | fix(security): validar sortBy contra whitelist en todos los controllers paginados |
| 2026-07-12 | ce9d841 | configuracion | ≡ f3768b9 | feat(security): encriptar smtpPassword en reposo con AES-256-GCM |
| 2026-07-13 | 15c7665 | facturacion | ~ 42d5f16 (no equiv.) | feat(pagos): agregar vista de conciliación y endpoint de lote |
| 2026-07-14 | 2107d30 | facturacion | ≡ c1e2e0d | feat(facturacion): soporte multi-divisa CRC/USD/EUR con tipo de cambio |
| 2026-07-14 | 2800782 | proveedores | ≡ fb86126 | feat(proveedores): implementar módulo de proveedores Sprint 6 Fase 3 |
| 2026-07-14 | 3aacb98 | inventario | ≡ f3e1f11 | feat(inventario): implementar módulo de inventario Sprint 6 Fase 2 |
| 2026-07-14 | 44a6175 | seguridad | ≡ af90493 | feat(seguridad): sincronizar permisos Sprint 6 desde enum a la BD en startup |
| 2026-07-14 | 94d3257 | proveedores | ≡ 211098b | fix(proveedores): aplicar correcciones Judgment Day S-1 a S-4 |
| 2026-07-14 | ec6eead | facturacion | ≡ f00d248 | feat(pagos): agregar estado de cuenta por cliente |
| 2026-07-15 | 196f3b5 | contabilidad | ≡ 45b32f2 | feat(contabilidad): add double-entry accounting hooks and cuentas-pagar view |
| 2026-07-15 | 5e94238 | inventario | ≡ ed77a7a | fix(inventario): fix SpEL NPE in ajustes and enable all-product kardex |
| 2026-07-15 | 728a657 | facturacion | ≡ 169186b | adding test to repo |
| 2026-07-15 | cbe8e9d | proveedores | ≡ b4ba9d8 | refactor(contabilidad): migrate CuentaPorPagar to contabilidad module |
| 2026-07-17 | 2f4f8cf | transversal | ≡ 4284c10 | fix(templates): replace #dates with #temporals after PR4 LocalDateTime migration |
| 2026-07-17 | 4cef109 | facturacion | ≡ ec8c30c | fix(facturas): fix modal missing elements, JSON response, SP Timestamp cast and const conflict |
| 2026-07-17 | 6bd4352 | shared | ≡ db05680 | feat(templates): add shared fragments for filter-bar, detail-field, status-badge, and table-card |
| 2026-07-17 | 7e129a3 | transversal | ≡ 48a8cf9 | refactor(frontend): extract inline styles from JS and templates to CSS files |
| 2026-07-17 | bde7de3 | transversal | ≡ 9ee693e | feat(seguridad): Sprint 7 - lockout, audit, email auth, LocalDateTime, headers, rate-limit, remember-me |

### Otras refs

Ninguna. `stage` tiene 3 commits sin merges fuera de P, pero ninguno toca `src/`; el resto de ramas y tags tienen 0 commits fuera de P.
