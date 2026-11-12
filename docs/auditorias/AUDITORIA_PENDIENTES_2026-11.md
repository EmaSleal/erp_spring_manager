# Auditoría de pendientes abiertos contra prod (T-029, P-050)

Objetivo: determinar con evidencia del código actual de `prod` el estado real de 29 pendientes abiertos (P-008 a P-055, sin P-050) y proponer su severidad y su bloque del plan, para depurar el backlog y repriorizar ADR-005. Es una tarea de solo lectura: este documento no corrige código, no escribe pruebas, no cierra ni edita pendientes ni ADR. Propone; decide el usuario.

## Fuente y método

| Elemento | Valor |
|---|---|
| Fecha | 2026-11-01 |
| Base auditada | rama `prod`, commit `cc73bdc` (incluye T-021 a T-028) |
| Método | lectura de código, historial git y bloques Result de tareas (T-005 a T-028); lectura previa del detalle de cada P-NNN en el almacén de memoria del proyecto |
| Ejecución | solo para P-053, P-040, P-032 y P-023, en MySQL aislado `t029-mysql` (8.0.32) restaurado desde el dump local `facturas_monrachem_20261011_173406.sql` (esquema Flyway V18) y migrado a V38 al arrancar la app; app con `env -i`, DNS bloqueado con hosts vacío, sin `.env`. **El dump es del 2026-10-11; no se tomó un dump fresco de producción**, por lo que los datos (por ejemplo, cero CxP) pueden diferir de producción hoy |
| Revisión independiente | completada, ver sección propia |

Abreviaturas de rutas: `M/` = `src/main/java/api/astro/whats_orders_manager/modules/`, `R/` = `src/main/resources/`, `S/` = `src/main/resources/static/`.

### Reglas de estado

- **RESUELTO / OBSOLETO**: exigen evidencia concreta (el código que demuestra la corrección o que el componente ya no existe).
- **VIGENTE**: exige el `archivo:línea` donde persiste el defecto.
- **PARCIAL**: parte resuelta y parte vigente; exige la línea "Qué falta".
- **NO_VERIFICABLE**: sin evidencia concluyente desde el repositorio; nunca se degrada a RESUELTO.
- Severidad ∈ {alta, media, baja} con razón ∈ {seguridad, dinero, fiscal, operación, estética}. Bloque ∈ {1 contabilidad, 2 base operable, FE, fuera}.

## Hallazgo de seguridad destacado

Dos defectos nuevos de seguridad aparecieron de paso (detalle en 7d, no corregidos):

1. `GET /api/notificaciones/preferencias` serializa la entidad con el `usuario` completo, incluido el hash de la clave y el rol con sus permisos (`M/notificacion/controller/NotificacionRestController.java:292-312`). Observado en ejecución. **Severidad alta (seguridad).**
2. `POST /api/facturas/configuracion-hacienda/upload-certificado` no tiene `@PreAuthorize` y usa el nombre original del archivo para resolver la ruta de destino (`M/facturacion/electronica/controller/ConfiguracionHaciendaController.java:211-223`). Con `SecurityConfig.java:117` (`/api/facturas/**` para ADMIN, USER y VENDEDOR) y sin `@PreAuthorize`, un VENDEDOR puede subir un `.p12/.pfx` y reemplazar (`REPLACE_EXISTING`) el certificado fiscal; el recorrido de directorios (`../`) es **hipótesis por lectura, no ejecutada**. **Severidad alta (seguridad y fiscal) por el reemplazo del certificado por un rol sin relación con la configuración de Hacienda, aun sin recorrido de directorios.**

## Tabla principal

| ID y tema | Estado | Evidencia | Qué falta | Severidad (razón) | Bloque | Relaciones |
|---|---|---|---|---|---|---|
| **P-008** Reglas de URL de SecurityConfig vs permisos dinámicos | PARCIAL | Persiste: `M/seguridad/config/SecurityConfig.java:95-96` (`/facturas/anular/**` solo ADMIN/USER; GERENTE tiene FACTURA_ANULAR) y `:197` (`/error/403` sin controlador). Resuelto: parte API por T-026/ADR-031 §2 (`SecurityConfig.java:160-166`). Obsoleto: webhook de Hacienda (ADR-025) | Decidir regla de `/facturas/anular/**` (permitir GERENTE o delegar en `@PreAuthorize`) y crear handler de `/error/403` | media (operación) | fuera | ADR-025, ADR-031 §2, T-022, T-026 |
| **P-009** CREATE ROUTINE para Flyway y rutinas fuera de Flyway | VIGENTE | `R/db/migration/V19__seed_ventas_cobros_parametros_contables.sql:24`, `V31:44`, `V32:40`, `V35:32` crean procedimientos; `M/producto/repository/ProductoRepository.java:21` invoca `ObtenerProductos()` que ninguna migración crea (solo `database/routines/`) | Ver pronunciamiento: ADR-028 absorbe las rutinas, no el privilegio para el replay histórico | alta (operación) | 2 | ADR-028, P-017, P-013 (3b), P-038 |
| **P-010** `incluir_igv_en_precio` y rótulos IGV | VIGENTE | `M/facturacion/model/ConfiguracionFacturacion.java:105-106` sin lector Java; `R/templates/modules/reportes/ventas.html:182`; `M/reportes/service/impl/ExportServiceImpl.java:265,590,689`; `gravado/porcentajeImpuesto` duplicados en `Producto` y `ArticuloMaestro.java:49-56` | Quitar la opción y su UI, renombrar IGV a IVA, retirar campos duplicados | baja (estética) | fuera | ADR-009, ADR-010, T-006, T-024 |
| **P-011** Revaluación mensual de saldos en moneda extranjera | VIGENTE | Ausencia verificada (`grep -ri revalu` sin proceso); solo existe el diferencial realizado: `M/contabilidad/service/AsientoContableService.java:391-394` | Proceso de revaluación al cierre con TC de cierre y reversión | alta (fiscal) | 1 (requiere decisión de alcance) | ADR-011, P-018, P-013, T-007, T-008 |
| **P-013** Monedas mezcladas y fecha de CxP desde OC | VIGENTE | `M/facturacion/service/impl/PagoServiceImpl.java:712-713`; `M/facturacion/repository/FacturaRepository.java:34`; `M/contabilidad/service/impl/CuentaPorPagarServiceImpl.java:169` (fecha de hoy) y `:272-276`; `R/templates/modules/facturacion/pagos/conciliacion.html:217` vs `S/modules/facturacion/js/facturas.js:4`. 3b (`/productos/records`) NO_VERIFICABLE | Convertir a CRC o separar por moneda; fecha de CxP desde OC; quitar la redeclaración JS | media (dinero) | 1 (2 y 1d); 3a fuera | P-016, P-018, P-009, P-011, P-023, ADR-011, ADR-014 |
| **P-014** Sin pantalla de tipos de cambio | VIGENTE | Solo API: `M/facturacion/controller/TipoCambioRestController.java:25,69,89`; sin plantilla en `R/templates` | Vista de listado, alta manual y sincronización | baja (operación) | fuera | T-008, P-015, P-011 |
| **P-015** Edición UI del porcentaje patronal ASE | VIGENTE | Solo lectura: `M/rrhh/controller/CatalogoRegulatorioViewController.java:31-50`; `ParametroNominaEmpresaService` sin escritura | Alta de vigencia con permiso y auditoría | baja (operación) | fuera | ADR-013, P-014, T-027 |
| **P-017** Flyway no construye la BD desde cero | VIGENTE | `R/db/migration/V1__email_not_null_unique.sql:4` altera `usuario` sin crearla; ninguna migración crea `usuario`; `R/application.yml:28-37` (baseline 0, `ddl-auto: validate`) | Baseline de esquema o reescribir V1 | alta (operación) | 2 | P-009, P-038, P-043, T-027 |
| **P-018** Sin periodos contables ni cierre | VIGENTE | Sin entidad ni tabla de periodo; `M/contabilidad/service/AsientoContableService.java:1641-1645` solo valida fecha nula; `:687` reversión fechada hoy sin periodo | Todo el diseño (entidad, validación central, cierre, reapertura) | alta (fiscal) | 1 (requiere ADR propio) | P-011, P-012, P-016, P-019, ADR-014, T-020 |
| **P-019** Numeración de asientos: año del sistema y concurrencia | VIGENTE | `AsientoContableService.java:1592` (`LocalDate.now().getYear()`), `:1593-1603` (max+1 sin bloqueo); `M/contabilidad/repository/AsientoContableRepository.java:130-132`; índice único `AsientoContable.java:36` | Secuencia por año de la fecha del asiento y bloqueo o reintento | media (operación) | 1 | P-018, P-016, ADR-014, T-011 |
| **P-020** CxP con varias líneas o contrapartidas | VIGENTE | Una sola contrapartida: `M/contabilidad/model/CuentaPorPagar.java:72-80`; `CuentaPorPagarServiceImpl.java:109`; sin entidad de detalle | Modelo de detalle y asiento con un débito por línea | media (operación) | fuera (alcance nuevo, ADR-016 lo excluye) | ADR-016, ADR-015, P-021, P-023 |
| **P-021** Sin flujo para anular una CxP ni revertir su asiento | VIGENTE | `EstadoCuentaPorPagar.ANULADA` solo se lee (`M/proveedores/service/impl/PagoProveedorServiceImpl.java:49,116`); ningún método de anulación en `CuentaPorPagarServiceImpl`; el controller anula solo pagos | Anulación con motivo, reversión del asiento, ajuste de saldo y liberación de la OC | alta (dinero) | 1 | ADR-016, ADR-023, ADR-005 §1, T-020, P-023 (3) |
| **P-023** CxP: listado con 500, saldo con monedas mezcladas, CxP por total de OC parcial | VIGENTE | (1) 500 reproducido: `M/contabilidad/controller/CuentaPorPagarRestController.java:35-44` + `CuentaPorPagar.java:41-50` (LAZY sin DTO); (2) `CuentaPorPagarServiceImpl.java:272-276`; (3) `:187` acepta RECIBIDA_PARCIAL y `:165-166`, `:84` usan `oc.getTotal()`; (4) parcial por T-020 (`df81e93`) | Ver notas: DTO de salida, saldo en CRC o por moneda, regla de CxP por lo recibido; nombre de cuenta roto NO_VERIFICABLE | alta (dinero) | 1 (subítem 1 en 2, subítem 4 a 7d) | P-013, P-021, P-037 (4), ADR-011, ADR-032, T-012, T-017 |
| **P-028** Unificar Empresa y ConfiguracionEmpresa | VIGENTE | `M/configuracion/model/Empresa.java:32`; consumidores `FacturaPdfServiceImpl.java:76`, `ExportServiceImpl.java:139`, `EmailServiceImpl.java:409,457`; `CuentaBancaria.java:34-35`; residuos `ConfiguracionEmpresa.java:46-50` | Pasos 1 a 5 del propio pendiente | media (operación) | 2 | P-026, P-029, P-045, ADR-019, ADR-021, ADR-028 |
| **P-030** PDF titulado "Factura electrónica" sin comprobante aceptado | VIGENTE | `M/facturacion/service/impl/FacturaPdfServiceImpl.java:174` título fijo; `:175-181` campos fiscales sin mirar el estado. Subpunto "0 líneas, total 0,00": NO_VERIFICABLE | Título y campos según estado del comprobante | alta (fiscal) | FE | ADR-029, ADR-030, ADR-021, T-018 |
| **P-032** Preferencias de notificaciones no guardan | VIGENTE | Reproducido: PUT da 500 y deja fila NULL/NULL. `M/notificacion/controller/NotificacionRestController.java:319-368`, `S/modules/notificacion/js/preferencias-notificaciones.js:210-224`, `PreferenciaNotificacionServiceImpl.java:70-75`. Síntoma distinto al descrito | DTO tipo x canal a filas por canal, validación de payload | media (operación) | fuera | T-017, ADR-020, P-033 |
| **P-033** Registro de actividad revierte la operación del llamador; User-Agent sin truncar | VIGENTE | `M/seguridad/service/impl/UsuarioActividadServiceImpl.java:76-79,99,125,152,176,199,229` (`@Transactional` sin aislar); `:492` sin truncar vs `UsuarioActividad.java:99` (`length = 500`). Corregidos solo `:257,:274` (T-017) | Persistencia aislada común y truncado a 500 | media (operación) | 2 | T-017, T-022, P-032 |
| **P-035** Subida de certificados: nombre original y sobrescritura | VIGENTE | `M/facturacion/electronica/controller/ConfiguracionHaciendaController.java:211-223` (`REPLACE_EXISTING`, nombre del cliente) | Nombre generado por servidor por ambiente, validar PIN, control de ruta, `@PreAuthorize` | alta (fiscal): además del nombre/sobrescritura, un VENDEDOR puede reemplazar el certificado (7d) | FE | T-018, P-034, ADR-024, ADR-032 |
| **P-037** Defectos de la UI de asientos manuales | PARCIAL | Resueltos por T-020 (`df81e93`): 1, 2, 4, 5. Vigentes: 3 (`R/templates/modules/contabilidad/asientos/form.html:71-75` lista tipos AUTOMATICO_*; irreversible por `AsientoContable.java:249-250`), 6 (`form.html:111`, `detalle.html:196`), 7 (`AsientoContableService.java:227-231`) | Subítems 3, 6 y 7 | media (operación) | 1 por el subítem 3; 6 y 7 fuera | T-019, T-020, P-036, P-039, P-023 (4) |
| **P-038** Respaldos sin procedimientos almacenados | PARCIAL | Resuelto: `database/scripts/backup.sh:71` (`--routines --triggers --events`, T-021 `6bf638e`). Vigente: `README-DOCKER.md:284` usa `mysqldump` sin rutinas; sin prueba de restauración | (a) corregir `README-DOCKER.md:284`; (b) prueba periódica de restauración; (c) confirmar origen del respaldo de producción | media (operación) | 2 | ADR-028, P-009, P-017, P-013, T-021, T-023 |
| **P-039** Interpolaciones HTML sin escapar en el frontend | VIGENTE | `S/modules/producto/js/productos.js:216,228`; `S/modules/facturacion/js/facturas.js:136,196,480`; `editar-factura.js:541-543`; `S/shared/js/websocket-notificaciones.js:182-185,206` (`urlAccion` sin validar esquema) | Escapar o usar `textContent`; validar `urlAccion` | alta (seguridad) | fuera (propuesta: ADR de excepción, ver pronunciamiento) | P-037 (1), P-040, P-044, P-053, T-020 |
| **P-040** Selector de facturas de /pagos/nuevo duplicado | VIGENTE | Dos manejadores del mismo cambio: `R/templates/modules/facturacion/pagos/form.html:68` (`onchange`) y `S/modules/facturacion/js/pagos.js:422` (`addEventListener`); ambos limpian (`:130`), esperan (`:142`) y agregan (`:157`). Duplicado visual no observado en navegador | Dejar un solo manejador | baja (operación) | fuera (7d) | P-013, ADR-011, T-020 |
| **P-043** Seed 13 desactualizado (columna `igv`) | VIGENTE | `database/seeds/13_configuracion_facturacion.sql:8` inserta `igv`; `R/db/migration/V24__drop_configuracion_facturacion_igv.sql:12` la elimina; T-021 Result confirma el error | Quitar `igv` y probar seeds 01..18 sobre V38 | media (operación) | 2 | P-017, P-038, ADR-024 |
| **P-044** Webhook de WhatsApp público sin firma; variables desalineadas | VIGENTE | `M/whatsapp/controller/WhatsAppWebhookController.java:103-104` sin `X-Hub-Signature-256`; `SecurityConfig.java:72` (`permitAll`) y `:126` (sin CSRF); `docker-compose.yml:54-56` (`WHATSAPP_*`) vs `R/application.yml:72-75` (`META_WHATSAPP_*`) | Validar firma o desactivar la ruta; alinear variables | alta (seguridad) | fuera | P-042, P-008, T-022, P-039 |
| **P-047** Consultas ad hoc con BI y usuario MySQL de solo lectura | NO_VERIFICABLE | Sin referencia a BI ni `GRANT`/`CREATE USER` en el repo; los usuarios y servicios viven en el servidor | Decisión (herramienta, despliegue, tablas visibles) | baja (operación) | fuera | ADR-028, T-024, P-034 |
| **P-049** Nota de crédito/débito electrónica inexistente | VIGENTE | Solo XML y XSD: `M/facturacion/electronica/service/impl/XmlGeneratorServiceImpl.java:115,147`; `ComprobanteElectronicoServiceImpl.java:192` (siempre FACTURA) y `:814` (TODO). T-025 bloquea anular facturas aceptadas (`PoliticaMutabilidadFactura.java:200-201`) pero la NC no existe | NC y ND de punta a punta con asiento de reversión | alta (fiscal) | FE | ADR-029, ADR-030, ADR-032, P-048, P-021 |
| **P-053** `inventario/ajustes.html` no renderiza | VIGENTE | `R/templates/modules/inventario/ajustes.html:159` (`productosData[/*[(...)]*/`); reproducido en ejecución | Quitar la secuencia `[/` del acceso al índice | media (operación) | fuera (7d) | T-026, P-037 |
| **P-054** Auditoría: `updateDate` con `@CreatedDate`; merges anulan `createDate` | PARCIAL | Vigente: `M/facturacion/model/Factura.java:67-69`, `LineaFactura.java:52-54`; merges desde `@RequestBody` en `ArticuloMaestroRestController.java:104`, `ProductoController.java:121`, `PresentacionRestController.java:71`, `ConfiguracionFacturacionRestController.java:112`. Resuelto: punto 2 solo para Factura por T-026 (`df6f75a`, `FacturaServiceImpl.java:152-158`); `updatable = false` de Factura/LineaFactura es anterior (2025, `c3d4e3d`/`dbb7db6`). Punto 3 falso (ya tienen `updatable = false`) | `@LastModifiedDate` en Factura y LineaFactura; retirar merge directo en 4 controladores; confirmar si LineaFactura necesita el mismo resguardo de `createDate` (T-026 no la tocó); retirar el punto 3 | baja (operación) | fuera | T-026, ADR-014 |
| **P-055** Listado de comprobantes sin Reenviar; jobs sin propiedad de desactivación | VIGENTE | `ComprobanteElectronicoServiceImpl.java:936-941` no fija `puedeReenviar`; `R/templates/modules/facturacion/electronica/comprobantes.html:304`; `M/facturacion/electronica/scheduled/ComprobanteReintentosJob.java:31,48` sin `@ConditionalOnProperty` | `puedeReenviar` en el DTO; propiedad `app.scheduling.enabled` | baja (operación) | fuera | ADR-028, ADR-031 §4, T-026 |

## Notas por pendiente

### P-008
- Subafirmación sobre el webhook `/api/hacienda/webhook`: OBSOLETA. No existe mapeo en `src/main` (ADR-025 eliminó el callback).
- Parte API: resuelta por T-026 con `RequestMatcherDelegatingAccessDeniedHandler` (JSON 403/401 para `/api/**` y AJAX). Las páginas siguen redirigiendo a `/error/403`, que no tiene controlador y termina en 404 (T-022, T-026).
- Se mantiene PARCIAL, no RESUELTO, porque los dos defectos de URL siguen en código.

### P-009
- Ninguna migración V1..V38 crea `ObtenerProductos` ni los `sp_*`; viven solo en `database/routines/` (aún presente: ADR-028 no se ha ejecutado, no hay V39).
- No verificable desde código: privilegio del usuario Flyway en producción y si `ObtenerProductos` existe en la BD de producción.

### P-013
- 1a/1b/1d: mezcla de monedas sin conversión; 1c sin defecto (`ReporteServiceImpl` usa `getTotalEnCRC`). T-012 observó 113 000 CRC + 113 USD = 113 113 en el saldo del proveedor.
- 3b (`/productos/records` con 500) NO_VERIFICABLE: depende de si la rutina existe en producción. No se ejercitó en la ejecución.

### P-023
1. Listado con 500: VIGENTE por ejecución (ver sección de ejecución). Falta DTO de salida o `@JsonIgnore` en `proveedor` y `ordenCompra` (verificado: solo `cuentaGasto` tiene `@JsonIgnore`, `CuentaPorPagar.java:79`; la lista `pagos`, línea 112, no lo tiene y conviene revisarla al diseñar el DTO).
2. Saldo del proveedor con monedas mezcladas: VIGENTE por código (`CuentaPorPagarServiceImpl.java:146,176,256,267,272-276`).
3. CxP por el total de una OC RECIBIDA_PARCIAL: VIGENTE por código (`:187`, `:165-166`, `:84`). Hay que definir la regla (por lo recibido o bloquear hasta recepción total) en las dos vías.
4. Cosmético: parcialmente resuelto por T-020 (`df81e93`): símbolo en asientos (`S/modules/contabilidad/js/asientos.js:72`, `detalle.html:161`). Persiste el `$` fijo en reportes (7d). El nombre "IVA Crdito Fiscal" no está en el repo (`V29__plan_de_cuentas_coherente.sql:206` es correcto): NO_VERIFICABLE, dato de producción.
- Estado asignado: VIGENTE, no PARCIAL, porque tres de los cuatro subítems siguen sin cambio; el subítem 4 es menor y está anotado aquí.

### P-030
- Subpunto "factura sin líneas y total 0,00": NO_VERIFICABLE por lectura (T-024 cambió el modelo de líneas); requiere abrir el PDF de la factura afectada en un respaldo.

### P-032
- El síntoma cambió: el pendiente dice que responde OK sin hacer nada; hoy responde 500 y deja una fila basura (ver ejecución). La pantalla ya muestra el error en vez de mentir que guardó.

### P-035
- Severidad alta: el endpoint queda abierto a ADMIN, USER y VENDEDOR (`SecurityConfig.java:117`) sin `@PreAuthorize`, y `REPLACE_EXISTING` permite a un VENDEDOR sustituir el certificado fiscal. El recorrido de directorios (7d) sigue siendo hipótesis y agravaría el caso.

### P-037
- Tabla de subítems: 1 RESUELTO (`S/modules/contabilidad/js/asientos.js:317`), 2 RESUELTO (`asientos.js:127,132`; `AsientoContableMapper.java:163`), 3 VIGENTE, 4 RESUELTO en asientos (queda el `$` en reportes), 5 RESUELTO (`S/shared/js/common.js:159-164`), 6 VIGENTE, 7 VIGENTE.
- El subítem 3 es el único con impacto contable: un asiento manual de tipo automático no se puede revertir (`AsientoContable.java:249-250`). El método `TipoAsiento.esAutomatico()` ya existe (`M/contabilidad/enums/TipoAsiento.java:43-45`).

### P-038
- Absorción por ADR-028: ver pronunciamiento. La prueba de restauración no se ejecutó dentro de esta auditoría.

### P-039
- El conteo "≈205" del pendiente es heurístico y no se recontó. Contraejemplos ya corregidos: `S/shared/js/navbar.js:444`, `S/modules/notificacion/js/notificaciones.js:148,153`; `escapeHtml` global en `S/shared/js/common.js:185`.

### P-040
- Ver pronunciamiento y sección de ejecución. El backend devuelve facturas únicas (`FacturaController.java:488-500`, `FacturaRepository.java:110-111` sin join).
- Con `?idCliente=` en la URL solo corre una llamada (`pagos.js:425-426`) y no se duplica; el defecto aparece al cambiar de cliente en el selector.

### P-047
- NO_VERIFICABLE porque la ausencia de un BI o de un usuario de solo lectura no se puede demostrar desde el repositorio; es una decisión, no un defecto de código.

### P-049
- La anulación de factura con comprobante aceptado ya queda bloqueada por T-025 (`FacturaServiceImpl.java:388-392,408-416`), pero sin NC no hay forma de corregirla. El pendiente sigue VIGENTE.

### P-054
- Punto 3 (Nomina, Notificacion, UsuarioActividad, UsuarioSesion sin `updatable = false`): falso en prod (`Nomina.java:106-108`, `Notificacion.java:181-183`, `UsuarioActividad.java:120-122`, `UsuarioSesion.java:118-120`).
- Punto 2: T-026 (`df6f75a`) conserva `createDate` solo para Factura (`FacturaServiceImpl.java:152-158`). El `updatable = false` de la columna en Factura y LineaFactura viene de 2025 (`c3d4e3d`/`dbb7db6`) y T-026 no tocó LineaFactura: para LineaFactura no hay evidencia de resolución por T-026 (si el merge deja `createDate` nulo en memoria no se verificó). Persiste en las 4 entidades indicadas. El estado sigue PARCIAL; el "Qué falta" incluye LineaFactura.

### P-055
- El listado sale de `sp_listar_comprobantes_*` (`ComprobanteElectronicoRepository.java:44-71`); el punto 1 se resuelve de forma natural al migrar a Java (ADR-028). Único `@ConditionalOnProperty` del repo: `M/facturacion/scheduler/TipoCambioSyncScheduler.java:23`.

## Verificación en ejecución

Entorno: MySQL 8.0.32 `t029-mysql` con volumen `t029-mysql-data`; restaurado desde el dump local del 2026-10-11 (esquema V18); Flyway aplicó V19..V38 al arrancar la app; app en el puerto 18029 con `env -i`, DNS bloqueado. Inicio de sesión como ADMIN correcto (302 a `/dashboard`); las llamadas REST llevaron token CSRF. Nunca se consultó `configuracion_hacienda` ni `configuracion_email`. Al arrancar, `IdentidadFiscalStartupCheck` registró un ERROR esperado en el entorno aislado (la configuración activa de Hacienda apunta a una ruta de certificado de Windows; se anota en 7d).

### P-053 `inventario/ajustes`
- Acción: GET `/inventario/ajustes` como ADMIN.
- Resultado: HTTP 200 pero cuerpo truncado (24 685 bytes, sin `</html>`, termina en un `<script>` abierto). Log: `TextParseException: (Line = 5, Column = 18) Malformed template: unnamed element is never closed` → `TemplateInputException` procesando `modules/inventario/ajustes`.
- Conclusión: VIGENTE, reproducido tal como lo describe el pendiente. Causa: `ajustes.html:159` (`productosData[/*[(${p.idProducto})]*/ 0]`, la secuencia `[/` rompe el parser inline).

### P-040 `/pagos/nuevo`
- Acción: GET `/pagos/nuevo` (200, una sola carga de `pagos.js`), GET `/pagos/nuevo?idCliente=1` (200) y GET `/facturas/api/cliente/1` y `/2` (endpoint que puebla el selector). No se envió ningún pago.
- Resultado: la BD tiene 3 facturas; para el cliente 1 el endpoint devuelve exactamente 2 elementos (idFactura 1 y 2, sin repetidos); el cliente 2 devuelve `[]`.
- Conclusión: el backend no duplica. La duplicación se explica por lectura de los dos manejadores (ver pronunciamiento). **No se observó en un navegador**; la conclusión es por código, con causa determinista.

### P-032 Preferencias de notificaciones
- Acción: login ADMIN; GET `/notificaciones/preferencias` (200); GET `/api/notificaciones/preferencias` (200); PUT `/api/notificaciones/preferencias` con el payload que arma el JS (`preferencias-notificaciones.js:210-218`): dos elementos `{tipo, activoWeb, activoEmail, activoWhatsapp}` (FACTURA_VENCIDA y STOCK_BAJO).
- Antes: 25 filas en `preferencia_notificacion`; el usuario 1 tenía las filas 22 (FACTURA_VENCIDA/EMAIL activa) y 23 (STOCK_BAJO/EMAIL activa).
- Resultado: PUT → HTTP 500 `{"error":"Error al guardar preferencias"}`; log `IllegalArgumentException: Ya existe una preferencia para este usuario, tipo y canal` (`PreferenciaNotificacionServiceImpl.java:73`). Después: 26 filas; la fila 26 tiene `tipo_notificacion` y `canal` en NULL; las filas 22 y 23 sin cambios (lo marcado no persistió). Todo guardado posterior vuelve a dar 500.
- Conclusión: VIGENTE, peor que lo descrito (500 más fila basura en lugar de "OK sin guardar"). El controller no es transaccional como un todo; cada `crear` hace commit propio.

### P-023 API de listado de CxP
- Acción: GET `/api/cuentas-pagar` con 0 CxP en el dump → 200 `{"data":[]}` (no concluyente: sin filas que serializar). Para ejercitarlo se creó, por la propia API, un proveedor (POST `/api/proveedores`) y una CxP manual (POST `/api/cuentas-pagar`, subtotal 1000 + impuesto 130, CRC; ambos 200). Luego GET `/api/cuentas-pagar` y GET `/api/cuentas-pagar?estado=PENDIENTE`.
- Resultado: HTTP 500 en ambos. Log: `HttpMessageConversionException: Type definition error: [simple type, class org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor]` / `InvalidDefinitionException: No serializer found for class ...ByteBuddyInterceptor`.
- Conclusión: subítem 1 VIGENTE. Los subítems 2 a 4 no se ejercitaron en ejecución (sin OC ni multimoneda); se clasifican por código.

### Escrituras realizadas en la BD aislada
1. Preparación: `DROP TRIGGER` de `trg_after_insert_usuario` y `trg_after_update_usuario`; `UPDATE usuario` del usuario 1 (clave de prueba, sin cambio obligatorio de clave, sin expiración, intentos fallidos a 0) para poder iniciar sesión.
2. Provocadas por las acciones observadas: fila 26 de `preferencia_notificacion` (NULL/NULL, P-032); un POST `/api/proveedores` sin cédula rechazado con 400 (sin fila); un proveedor (id 1) y una CxP (id 1, `CPP-2026-00001`) creados para P-023; el inicio de sesión actualiza campos de acceso del usuario 1; Flyway V19..V38 aplicados al arrancar.
3. Ninguna escritura sobre la BD de producción ni sobre el repositorio.

### Limpieza
Proceso java detenido; `docker rm -f t029-mysql`; `docker volume rm t029-mysql-data`; borrados la copia del dump, los hashes, cookies, respuestas y logs. El conjunto de contenedores y volúmenes de Docker quedó idéntico a la línea base.

## Pronunciamientos expresos

### P-039: severidad ALTA (seguridad); bloque FUERA, con propuesta de ADR de excepción
- Severidad alta: XSS almacenado entre usuarios internos (`productos.js:216,228`, `websocket-notificaciones.js:182-185` con `urlAccion` aceptando `javascript:`), y texto de terceros si se reactiva WhatsApp (P-044).
- Bloque: ADR-032 pide evaluar subirlo al bloque 2. El bloque 2 de ADR-005 se define por infraestructura (todas las tablas con Flyway, arranque limpio con `ddl-auto=validate`, `./mvnw verify` en verde con *IT, JaCoCo). Un barrido de escape en el frontend no entra en esa definición, y ADR-005 exige un ADR propio para toda excepción (precedente: ADR-016). Veredicto: **no subir de oficio; bloque fuera**.
- Recomendación al usuario: decidir mediante un ADR de excepción si se trata antes de cerrar los bloques; en todo caso, antes del paso de FE a PRODUCCIÓN. El arreglo directo (escapar productos.js, facturas.js, editar-factura.js, websocket-notificaciones.js y validar `urlAccion`) es acotado.

### P-023: severidad ALTA (dinero); bloque 1, con el subítem 1 en bloque 2
- Los subítems 2 y 3 producen saldos y asientos de compra con montos incorrectos (monedas mezcladas; pasivo por mercadería no recibida), que ADR-005 §1 (asiento correcto en CRC por operación) no tolera. ADR-032 pide evaluar subirlo al bloque 1: **se recomienda subirlo**.
- El subítem 1 (500 del listado, reproducido) es operación y baja a bloque 2 (media o baja: la pantalla usa Thymeleaf y el endpoint JSON no tiene consumidor conocido). El subítem 4 va a 7d.
- El subítem 2 coincide con P-013 (1d): conviene atenderlos juntos.

### P-040: severidad BAJA (operación); bloque FUERA (7d); estado VIGENTE
- Resolución de la discrepancia entre exploraciones (B y C: NO_VERIFICABLE; ejecución: backend sin duplicados y causa probable en el cliente). Lectura directa del código: el `<select id="idCliente">` registra `onchange="cargarFacturasCliente()"` (`R/templates/modules/facturacion/pagos/form.html:68`) y además `idClienteSelect.addEventListener('change', cargarFacturasCliente)` (`S/modules/facturacion/js/pagos.js:422`). Son dos manejadores distintos sobre el mismo elemento y el mismo evento: ambos se ejecutan en cada cambio. `cargarFacturasCliente` (`pagos.js:124`) limpia el select (`:130`) antes del primer `await` (`:142`) y luego agrega las opciones (`:157`); como la limpieza de ambas llamadas ocurre antes de que cualquiera resuelva, ambas anexan y cada factura queda dos veces. No hay guarda entre `:130` y `:157`.
- Por tanto la duplicación es determinista en cada cambio de cliente: VIGENTE con `form.html:68` + `pagos.js:422`. La carga inicial por `?idCliente=` no duplica (una sola llamada, `:425-426`). Salvedad: no se observó el DOM en un navegador.
- Severidad baja: las dos opciones llevan el mismo `idFactura` (el backend devuelve filas únicas), de modo que el cobro no se aplica a un documento equivocado; no hay impacto en dinero. Queda en bloque fuera y no pasa a bloque 1.

### P-009: absorbido por ADR-028 solo en parte
- Absorbido: las rutinas fuera de Flyway y las llamadas Java a procedimientos (`ProductoRepository.java:21`, `FacturaRepository.java:69,79,92`, `ComprobanteElectronicoRepository.java:44,54`), porque ADR-028 las retira y hace DROP de los 35 objetos.
- No absorbido: el privilegio `CREATE ROUTINE` para reproducir desde cero las migraciones históricas V19, V29 a V32 y V35, que crean y borran procedimientos (`V19:24`, `V31:44`, `V32:40`, `V35:32`) y que ADR-028 declara que no se modifican. Su desaparición depende de la línea base de P-017.
- Veredicto: **P-009 sigue abierto con alcance reducido** al privilegio y al replay histórico. Se resuelve junto con P-017, no con ADR-028.

### P-038: absorbido por ADR-028 solo en parte
- Absorbido: el síntoma (restaurar y no poder facturar por faltar rutinas) desaparece cuando ADR-028 retire las rutinas y el código no las invoque.
- No absorbido: (1) la prueba de restauración (no hay ninguna en el repo); (2) `README-DOCKER.md:284` sigue sin `--routines`/`--triggers` y con la clave en la línea de comandos; (3) un respaldo restaurado a BD vacía reejecuta V19 y V29 a V35, con el mismo requisito de privilegio; (4) confirmar el origen del respaldo de producción.
- Veredicto: **no queda absorbido; se mantiene PARCIAL** hasta corregir el README y probar una restauración (o hasta ejecutar ADR-028 y probar).

## Revisión independiente (criterio 8)

Ninguna fila completa está marcada RESUELTO u OBSOLETO; se revisaron los sub-puntos resueltos u obsoletos y las cuatro conclusiones de ejecución, contra el código de `prod` (`cc73bdc`), `git log`/`git show` y la evidencia registrada de la ejecución (el entorno ya estaba destruido; no se re-ejecutó).

| Elemento revisado | Fuente | Resultado | Nota |
|---|---|---|---|
| P-008, webhook de Hacienda OBSOLETO | grep `hacienda/webhook` en `src/main`; ADR-025 | confirmada | Sin mapeo alguno; el único webhook vivo es `/api/whatsapp/webhook`. |
| P-008, parte API resuelta (T-026/ADR-031 §2) | `SecurityConfig.java:159-166, 195-197` | confirmada | Handler JSON para `/api/**` y AJAX. `/error/403` sigue sin controlador ni plantilla `error/403`, y `/facturas/anular/**` solo ADMIN/USER (`:95-96`): los defectos vigentes persisten. |
| P-054 punto 3 falso | `Nomina.java:106-108`, `Notificacion.java:181-183`, `UsuarioActividad.java:120-122`, `UsuarioSesion.java:118-120` | confirmada | Las cuatro llevan `updatable = false` en su campo `@CreatedDate` (nombres de columna distintos de `createDate`). |
| P-054 punto 2 Factura/LineaFactura "por T-026" | `FacturaServiceImpl.java:152-158` (`df6f75a`); `git blame` de `Factura.java:63-65` y `LineaFactura.java:48-50` | corregida | Para `Factura` es cierto: `save` conserva `createDate` desde la BD (T-026). Pero `updatable = false` en ambas entidades es de 2025 (`c3d4e3d`), no de T-026, y T-026 no tocó `LineaFactura` ni su guardado. Sustituir "createDate con `updatable = false` y `FacturaServiceImpl.save` lo preserva" por: "`Factura`: `save` preserva `createDate` (T-026); `LineaFactura`: sin cambio de T-026, `updatable = false` preexistente". |
| P-037 subítem 1 RESUELTO | `asientos.js:317` (`escapeHtml`), blame `df81e93` | confirmada | |
| P-037 subítem 2 RESUELTO | `asientos.js:127,132`; `AsientoContableMapper.java:163-164` (blame `df81e93`) | confirmada | `toDTOSimple` rellena `puedeModificarse`/`puedeContabilizarse`. |
| P-037 subítem 4 RESUELTO en asientos | `asientos.js:72`, `detalle.html:161` | confirmada | El `$` fijo persiste en reportes (7d, punto 9). |
| P-037 subítem 5 RESUELTO | `common.js:159-164` | confirmada | Un tercer argumento `'warning'/'error'` se trata como icono. |
| P-037 subítems 3, 6, 7 vigentes | `form.html:71-75` y controller `:66,89` (`TipoAsiento.values()`); `form.html:111`, `detalle.html:196`; `AsientoContableService.java:225-232`; `AsientoContable.java:249-250` | confirmada | Nota menor: `TipoAsiento.esAutomatico()` está en `TipoAsiento.java:43-45`, no `:41-44`. |
| P-023 subítem 4 parcial por T-020 | `asientos.js:72`, `detalle.html:161` | confirmada | `V29__plan_de_cuentas_coherente.sql:206` no se verificó contra producción: queda NO_VERIFICABLE, correcto. |
| P-038 `backup.sh --routines` resuelto por T-021 | `database/scripts/backup.sh:71`; `git show 6bf638e --stat` | confirmada | `README-DOCKER.md:284` sigue sin `--routines` (confirmado), por eso PARCIAL. |
| Ejecución P-053 | `ajustes.html:159`; `runtime.md` | confirmada | La secuencia `[/` está en la columna 18 de la línea 5 del bloque inline, exactamente la posición del `TextParseException`. |
| Ejecución P-040 | `form.html:62-68`, `pagos.js:124-157, 420-426`, `common.js:510` | confirmada | Ver análisis abajo. Solo por lectura; el DOM no se observó. |
| Ejecución P-032 | `NotificacionRestController.java:319-368`, `PreferenciaNotificacionServiceImpl.java:66-75`, `preferencias-notificaciones.js:205-224`; `runtime.md` | confirmada | El payload (`tipo/activoWeb/...`) se deserializa como entidad vacía (las propiedades desconocidas se ignoran), `crear` guarda la primera fila NULL/NULL y la segunda choca con el control de duplicado: coherente con 500 + fila 26. |
| Ejecución P-023 | `CuentaPorPagarRestController.java:35-44`, `CuentaPorPagar.java:41-50` | confirmada | `proveedor` y `ordenCompra` LAZY sin `@JsonIgnore` ni DTO; el 0 filas inicial (200) se descartó correctamente como no concluyente. Nota: `pagos` no es el problema aquí (solo `cuentaGasto` lleva `@JsonIgnore`, `:79`). |

Análisis de P-040. El `<select id="idCliente">` lleva `onchange="cargarFacturasCliente()"` (`form.html:68`; la etiqueta abre en `:62`) y `pagos.js:422` añade `addEventListener('change', cargarFacturasCliente)` sobre `document.getElementById('idCliente')`: es el mismo elemento y el mismo evento, y un atributo `onchange` y un listener coexisten, así que ambos se ejecutan en cada cambio. En `cargarFacturasCliente`, la limpieza (`pagos.js:130`) y el `await httpGet` (`:142`) son consecutivos sin otro punto de suspensión: la primera llamada limpia y queda suspendida, luego corre la segunda, que limpia (nada que borrar) y se suspende; al resolver, cada una ejecuta el `appendChild` (`:157`). Resultado: cada factura aparece dos veces. `httpGet` (`common.js:510`) no tiene caché ni deduplicación de peticiones en vuelo. Con `?idCliente=` solo corre la llamada de `:425-426`, sin duplicar. Conclusión: confirmada por lectura (causa determinista); falta la observación en navegador, ya declarada en el documento.

Revisor: agente revisor independiente (verify), distinto de los clasificadores. Fecha: 2026-11-01.

## Cierre

### 7a. Conteo por estado

| Estado | Cantidad | Pendientes |
|---|---|---|
| VIGENTE | 24 | P-009, P-010, P-011, P-013, P-014, P-015, P-017, P-018, P-019, P-020, P-021, P-023, P-028, P-030, P-032, P-033, P-035, P-039, P-040, P-043, P-044, P-049, P-053, P-055 |
| PARCIAL | 4 | P-008, P-037, P-038, P-054 |
| NO_VERIFICABLE | 1 | P-047 |
| RESUELTO | 0 | — |
| OBSOLETO | 0 | — |
| **Total** | **29** | |

### 7b. Candidatos a cerrar (RESUELTO u OBSOLETO)

**Ninguno: ningún pendiente completo es RESUELTO ni OBSOLETO.** Sub-puntos que se pueden retirar de su pendiente (sin cerrarlo), con evidencia:

- P-008, subafirmación del webhook de Hacienda: OBSOLETA. No existe mapeo `/api/hacienda/webhook` en `src/main`; ADR-025 eliminó el callback.
- P-008, parte API (403 en JSON): resuelta por T-026 / ADR-031 §2 (`SecurityConfig.java:160-166`).
- P-054, punto 3: falso. `Nomina.java:106-108`, `Notificacion.java:181-183`, `UsuarioActividad.java:120-122` y `UsuarioSesion.java:118-120` ya tienen `updatable = false`.
- P-054, punto 2 solo para Factura: resuelto por T-026 (`df6f75a`; `createDate` conservado en `FacturaServiceImpl.java:152-158`). LineaFactura no: T-026 no la tocó y su `updatable = false` es de 2025.
- P-037, subítems 1, 2, 4 (en asientos) y 5: resueltos por T-020 (`df81e93`). El pendiente puede dividirse: cerrar esos cuatro y mantener 3, 6 y 7.
- P-038, parte `backup.sh`: resuelta por T-021 (`database/scripts/backup.sh:71`).
- P-023, subítem 4 en asientos: resuelto por T-020; queda el `$` en reportes.

### 7c. Subir o bajar prioridad respecto de ADR-005

Subir (con justificación):
- **P-023 a bloque 1, alta/dinero**: saldos y pasivos con monedas mezcladas o por mercadería no recibida (ver pronunciamiento). Atender junto con P-013 (1d).
- **P-021 a alta/dinero, bloque 1**: ADR-005 §1 exige reversión de toda anulación y hoy no hay vía para anular una CxP ni corregir el saldo del proveedor.
- **P-037 subítem 3 al bloque 1**: un asiento manual con tipo automático no se puede revertir; la corrección es filtrar `esAutomatico()` en el selector (cambio mínimo).
- **P-018, P-019, P-013 (2 y 1d) y P-011 en bloque 1**: afectan la confiabilidad del libro diario y el cierre. P-018 requiere ADR propio; P-011 requiere decidir si la revaluación es parte del bloque 1 o del cierre.
- **P-044 a acción inmediata (alta/seguridad)**: endpoint público de escritura sin firma ni CSRF; el cambio es pequeño (validar firma o desactivar la ruta mientras WhatsApp esté diferido).
- **P-039**: ver pronunciamiento (ADR de excepción propuesto, no se sube de oficio).
- **P-035 a alta** por la falta de `@PreAuthorize` con acceso de VENDEDOR (7d), antes del paso a PRODUCCIÓN.
- **P-030 y P-049 en FE**: bloqueantes del paso a PRODUCCIÓN (ADR-032); sin cambio de bloque.

Mantener en bloque 2: P-009 (alcance reducido), P-017, P-028, P-033, P-038 (con prueba de restauración), P-043.

Bajar (alcance nuevo o trivial):
- A fuera por alcance nuevo congelado: P-014, P-015, P-020 (ADR-016 lo excluye), P-047.
- A fuera/7d por trivial o bajo impacto: P-010, P-040 (borrar un manejador), P-053 (una línea), P-054, P-055; P-032 queda fuera (WhatsApp diferido), aunque la fila basura recomienda un arreglo rápido.
- P-008 baja a fuera (corrección de seguridad menor).

### 7d. Defectos nuevos de paso (no corregidos)

Seguridad:
1. **ALTA.** `M/notificacion/controller/NotificacionRestController.java:292-312` y `M/notificacion/model/PreferenciaNotificacion.java` (campo `usuario` sin `@JsonIgnore`): `GET /api/notificaciones/preferencias` devuelve la entidad con el `usuario` completo, es decir el hash de la clave y el rol con sus permisos (respuesta de unos 122 KB). Observado en ejecución. Además, una llamada inmediata posterior al PUT falló con `Document nesting depth (1001) exceeds the maximum` (ciclo de serialización), de forma intermitente. Corrección: DTO de salida.
2. **ALTA** (control de acceso ausente, verificado por lectura; el recorrido de directorios es hipótesis no ejecutada). `M/facturacion/electronica/controller/ConfiguracionHaciendaController.java:211-223`: sin `@PreAuthorize` en método ni clase; el acceso lo da solo `SecurityConfig.java:117` (`/api/facturas/**` para ADMIN, USER y VENDEDOR), de modo que un VENDEDOR puede subir y reemplazar el certificado fiscal; `originalName.replace(" ", "_")` (`:219`) y `dir.resolve(safeName)` (`:222`) no neutralizan `../` ni rutas absolutas que terminen en `.p12`. Verificar con un POST multipart de nombre `../x.p12`.
3. **MEDIA.** `M/proveedores/controller/ProveedorRestController.java:58-70`: el `catch` devuelve `e.getMessage()` y `ProveedorDTO` no valida la cédula; un POST sin cédula respondió 400 con el texto SQL crudo (`Column 'cedula' cannot be null ... insert into proveedores ...`).
4. **MEDIA (cubierto por P-039).** `S/shared/js/websocket-notificaciones.js:185`: `urlAccion` en `href` sin validar esquema (`javascript:`).
5. **BAJA.** `README-DOCKER.md:284`: la clave de root en la línea de comandos y respaldo sin rutinas (P-038).

Contabilidad y datos:
6. `M/contabilidad/service/impl/CuentaPorPagarServiceImpl.java:272-276`: `actualizarSaldoProveedor` suma montos de cualquier moneda (P-013 1d, P-023 2).
7. `M/contabilidad/service/impl/CuentaPorPagarServiceImpl.java:279`: `synchronized` en `generarNumero()` solo protege dentro de una JVM; con dos instancias el insert falla por unicidad (500).
8. `M/contabilidad/service/AsientoContableService.java:687`: la reversión toma `LocalDate.now()`, no la fecha económica de ADR-014, y no pasa por validación de periodo (P-018). Verificar contra ADR-014 antes de decidir.
9. `R/templates/modules/contabilidad/reportes/estado-resultados.html:50,56,72,78,91` y `balance-general.html:50`: `$` fijo para montos en CRC.
10. `database/seeds/13_configuracion_facturacion.sql:8`: `simbolo_moneda` con texto corrupto en lugar de `₡`.

Frontend y configuración:
11. `R/templates/modules/facturacion/pagos/conciliacion.html:217` vs `S/modules/facturacion/js/facturas.js:4`: `const facturaModal` y `let facturaModal` en el mismo ámbito global; rompe la carga de `facturas.js` en esa página (P-013 3a).
12. `S/modules/facturacion/js/pagos.js:151` y `:156`: `option.textContent` asignado dos veces (código muerto); `:211` usa `innerHTML` con un dato que viene de la API (patrón de P-039); `S/modules/facturacion/js/facturas.js:480` interpola el email del cliente en un diálogo HTML sin escapar.
13. `M/whatsapp/model/PlantillaWhatsApp.java:95`: `@Column(name = "", updatable = false)` con nombre vacío; con `ddl-auto=validate` conviene fijar el nombre.
14. `src/main/java/api/astro/whats_orders_manager/config/CacheConfig.java:35` y `src/main/java/api/astro/whats_orders_manager/WhatsOrdersManagerApplication.java:9`: `@EnableScheduling` duplicado (inocuo).
15. `M/configuracion/config/IdentidadFiscalStartupCheck.java`: en el dump de producción la configuración activa de Hacienda apunta a una ruta de certificado de Windows; esperado en el entorno aislado, pero revisar el dato real en producción.
16. Documentación de pendientes: P-054 punto 3 es falso en prod (ver 7b); corregir el texto del pendiente en la depuración.
