# Auditoría del estado real del Sprint 5

| Campo | Valor |
|---|---|
| Fecha de auditoría | 2026-10-10 |
| Commit auditado | `e0a14813837d71993c0196af23c0a9bc91bca60f` |
| Rama | `t-001-auditoria-del-estado-real-del-sprint-5-c` |
| Alcance | Fase 1 Pagos, Fase 2 Contabilidad, Fase 3 Facturación Electrónica CR v4.4, Fase 4 Testing, Fase 5 Documentación |
| Método | Evidencia tomada del código fuente (`src/`, `pom.xml`, `application.yml`) y del historial Git. La documentación solo se usó para obtener la lista de entregables. |

## Contexto: documentación contradictoria

- `README.md` (raíz, último cambio 2026-08-10) declara "Sprint 5 completado (Facturación Electrónica CR sandbox validada)", pero en su propio roadmap deja abiertos "Notas de crédito y débito electrónicas" y "Migración a ambiente de producción Hacienda CR".
- `docs/sprints/SPRINT_5/INDICE_SPRINT_5.md` (fecha interna 2026-01-23) informa 113/199 tareas (56.8 %): Pagos 0/45, Contabilidad 58/58, FE 55/58, Testing 0/32, Documentación 0/12.
- `docs/sprints/SPRINT_5/fases/FASE_1_PAGOS.md` y `FASE_3_FACTURACION_ELECTRONICA_CR.md` marcan varias tareas como hechas que el índice da por pendientes (por ejemplo, Resilience4j y el job de reintentos).

Ninguna de las tres fuentes describe el código actual con exactitud. Este documento lo hace.

Leyenda de estado: **IMPLEMENTADO** (existe y está conectado al flujo), **PARCIAL** (existe pero incompleto, sin conectar o con defectos que impiden su uso), **AUSENTE** (no hay código).

Todas las rutas Java son relativas a `src/main/java/api/astro/whats_orders_manager/`.

---

## Fase 1: Pagos

| Entregable | Estado | Evidencia |
|---|---|---|
| Entidad `Pago` con validaciones | IMPLEMENTADO | `modules/facturacion/model/Pago.java` (`@Table(name="pagos")`, `@Positive` en monto, `validarReferencia()`, `validarFacturaRequerida()`, `conciliar()`, `rechazar()`, `anular()`) |
| Enum `MetodoPago` (catálogo Hacienda CR) | IMPLEMENTADO | `modules/facturacion/enums/MetodoPago.java` (códigos 01, 02, 03, 04, 05 y 99: 6 métodos; el plan pedía 8) |
| Enums `EstadoPago`, `EstadoPagoFactura`, `TipoPago` | IMPLEMENTADO | `modules/facturacion/enums/EstadoPago.java`, `EstadoPagoFactura.java`, `TipoPago.java` |
| Relación `Factura` → `Pago` y estado de pago | IMPLEMENTADO | `modules/facturacion/model/Factura.java`: `List<Pago> pagos` (l. 90), `estadoPago` (l. 105), `calcularSaldoPendiente()` (l. 184), `actualizarEstadoPago()` (l. 205) |
| Script de migración de tabla `pagos` | PARCIAL | Solo como SQL manual: `docs/base de datos/MIGRATION_PAGOS.sql` (`CREATE TABLE IF NOT EXISTS pagos`), `MIGRATION_PAGOS_DATA.sql`, `MIGRATION_PAGOS_PERMISOS.sql`. No hay migración Flyway (`rg -i pagos src/main/resources/db/migration` sin resultados) y `spring.jpa.hibernate.ddl-auto: validate` |
| `PagoRepository` con consultas y agregaciones | IMPLEMENTADO | `modules/facturacion/repository/PagoRepository.java` (usado por `PagoServiceImpl.findPagosPendientesConciliacion`, `sumIngresosDiaActual`, `sumByMetodoPagoAndFecha`, etc.) |
| `PagoDTO` y `PagoMapper` | IMPLEMENTADO | `modules/facturacion/dto/PagoDTO.java`, `modules/facturacion/dto/mapper/PagoMapper.java` |
| DTOs de estado de cuenta y reportes | IMPLEMENTADO | `modules/facturacion/dto/EstadoCuentaClienteDTO.java`, `ReporteCajaDTO.java` |
| `PagoService` / `PagoServiceImpl` | IMPLEMENTADO | `modules/facturacion/service/impl/PagoServiceImpl.java`: `registrarPago(Pago)` (l. 139), `registrarPago(PagoDTO)` (l. 233), `confirmarPago`, `conciliarPago`, `rechazarPago`, `anularPago` (l. 339), `conciliarPagosLote`, `buildEstadoCuenta` (l. 554), `buildReporteCaja` (l. 514) |
| Validaciones de negocio (saldo, referencia, duplicados) | IMPLEMENTADO | `PagoServiceImpl.registrarPago`: monto vs. `factura.calcularSaldoPendiente()`, `existsByReferenciaBancaria`, `existsByNumeroPago` |
| Validación de fecha de pago | AUSENTE | Sin `@PastOrPresent` ni comprobación equivalente en `Pago.java` o `PagoServiceImpl` (patrón: `rg '@PastOrPresent\|fechaPago.*isAfter'`) |
| `PagoController` (vistas + REST) | IMPLEMENTADO | `modules/facturacion/controller/PagoController.java` (`@RequestMapping("/pagos")`): listar, `/nuevo`, `/{id}`, `/registrar`, `/{id}/confirmar`, `/{id}/conciliar`, `/conciliar-lote`, `/{id}/rechazar`, `/{id}/anular`, `DELETE /{id}`, `/factura/{idFactura}`, `/pendientes-conciliacion`, `/estado-cuenta/{idCliente}`, `/reporte-caja` |
| Vistas Thymeleaf | IMPLEMENTADO | `src/main/resources/templates/modules/facturacion/pagos/`: `listar.html`, `form.html`, `detalle.html`, `estado-cuenta-cliente.html`, `conciliacion.html`, `reporte-caja.html` |
| JavaScript del formulario | IMPLEMENTADO | `src/main/resources/static/modules/facturacion/js/pagos.js` |
| Integración con Factura (estado, botón "Registrar pago", bloqueo de borrado) | IMPLEMENTADO | `templates/modules/facturacion/facturas.html` l. 237 (`/pagos/nuevo(idFactura=...)`); `FacturaServiceImpl.eliminarConValidacion` lanza "la factura tiene pagos registrados"; cada operación de `PagoServiceImpl` llama `factura.actualizarEstadoPago()` |
| Conciliación básica | IMPLEMENTADO | `PagoServiceImpl.conciliarPago`, `conciliarPagosLote`; `PagoController.pagosPendientesConciliacion`; vista `pagos/conciliacion.html` |
| Recordatorios de pago (extra) | IMPLEMENTADO | `modules/facturacion/scheduler/RecordatorioPagoScheduler.java` (`@Scheduled(cron="0 0 9 * * *")`) |
| **Asiento contable automático al registrar un pago** | **AUSENTE** | `AsientoContableService.generarAsientoPago(...)` existe (l. 434) pero **no tiene ningún llamador**: `rg 'generarAsientoPago\('` solo encuentra la definición y una línea comentada en `AsientoContableController.generarAsientoPago` (l. 348), cuyo endpoint `POST /contabilidad/asientos/api/generar-pago` devuelve siempre `400` con cuerpo nulo. `PagoServiceImpl` no referencia `AsientoContableService` |
| Reversión del asiento al anular un pago | AUSENTE | `PagoServiceImpl.anularPago` (l. 339) solo cambia el estado del pago y de la factura; no hay llamada a `AsientoContableService.anular` |

**Conclusión de la fase:** el módulo de Pagos está construido de punta a punta (contrario a "0/45" del índice). Falta la integración contable, que es justamente lo que el plan declaraba como integración crítica.

---

## Fase 2: Contabilidad

| Entregable | Estado | Evidencia |
|---|---|---|
| Entidad `CuentaContable` (plan de cuentas jerárquico) | IMPLEMENTADO | `modules/contabilidad/model/CuentaContable.java` (`cuentas_contables`); `CuentaContableService.calcularNivel`, `generarSiguienteCodigo`, `obtenerSubcuentas`, `obtenerRutaCuenta` |
| Entidades `AsientoContable` y `DetalleAsiento` | IMPLEMENTADO | `modules/contabilidad/model/AsientoContable.java` (`asientos_contables`, FKs a `factura`, `pago`, `pagoProveedor`, `nomina`), `DetalleAsiento.java` (`detalles_asiento`) |
| Entidad `PeriodoContable` | AUSENTE | No encontrado (patrón: `rg -i 'PeriodoContable\|periodo_contable' src/main/java`) |
| Enums (`TipoCuenta`, `NaturalezaCuenta`, `EstadoAsiento`, `TipoAsiento`) | IMPLEMENTADO | `modules/contabilidad/enums/` |
| Repositorios | IMPLEMENTADO | `modules/contabilidad/repository/` (`CuentaContableRepository`, `AsientoContableRepository`, `DetalleAsientoRepository`, `ParametroContableRepository`, `CuentaPorPagarRepository`) |
| DTOs y mappers | IMPLEMENTADO | `modules/contabilidad/dto/` (`AsientoContableDTO`, `CuentaContableDTO`, `DetalleAsientoDTO`, `CuentaPorPagarDTO`) y `dto/mapper/` (3 mappers) |
| Servicios de cuentas y asientos (manual) | IMPLEMENTADO | `CuentaContableService` (CRUD, `tieneMovimientos`); `AsientoContableService.crear`, `actualizar`, `agregarDetalle`, `contabilizar`, `anular`, `eliminar` |
| Asiento automático desde factura (venta) | PARCIAL | `AsientoContableService.generarAsientoVenta` (l. 359) existe pero **no tiene llamadores**; además usa `Long.getLong(factura.getIdFactura().toString())` (l. 364), que lee una propiedad de sistema y devuelve `null`, por lo que la verificación de duplicados no funciona. El mismo defecto aparece en `AsientoContableMapper` l. 54 (`facturaId` siempre `null`) |
| Asiento automático desde pago | PARCIAL | `AsientoContableService.generarAsientoPago` (l. 434) sin llamadores; endpoint stub (ver Fase 1) |
| Asientos automáticos de compras, pagos a proveedores y nómina (fuera del plan original) | IMPLEMENTADO | `generarAsientoCompra` llamado desde `CuentaPorPagarServiceImpl` l. 98; `generarAsientoPagoProveedor` desde `PagoProveedorServiceImpl` l. 69; `generarAsientoNomina` desde `NominaService` l. 275 |
| Parámetros contables (mapeo de cuentas para automatismos) | IMPLEMENTADO | `ParametroContable.java`, `ParametroContableService`, `ParametroContableController`, vista `contabilidad/parametros/listar.html` |
| Libro diario | AUSENTE | Sin servicio ni vista específica (patrón: `rg -i diario modules/contabilidad templates/modules/contabilidad`, sin resultados). Solo existe el listado de asientos `asientos/listar.html` |
| Libro mayor | IMPLEMENTADO | `CalculoContableService.obtenerLibroMayor` (l. 126); `ReporteContableController.libroMayor`; vista `reportes/libro-mayor.html` |
| Balance de comprobación | IMPLEMENTADO | `CalculoContableService.generarBalanceComprobacion` (l. 179); vista `reportes/balance-comprobacion.html` |
| Balance general | IMPLEMENTADO | `CalculoContableService.generarBalanceGeneral` (l. 207); vista `reportes/balance-general.html` |
| Estado de resultados | IMPLEMENTADO | `CalculoContableService.generarEstadoResultados` (l. 255); vista `reportes/estado-resultados.html` |
| Controllers | IMPLEMENTADO | `CuentaContableController`, `AsientoContableController`, `ReporteContableController`, `ContabilidadViewController`, `ParametroContableController`, `CuentaPorPagarRestController` |
| Vistas Thymeleaf | IMPLEMENTADO | `templates/modules/contabilidad/`: `cuentas/{listar,form,detalle}.html`, `asientos/{listar,form,detalle}.html`, `reportes/{index,libro-mayor,balance-comprobacion,balance-general,estado-resultados}.html`, `contabilidad.html`, `cuentas-pagar.html` |
| JavaScript | IMPLEMENTADO | `static/modules/contabilidad/js/{cuentas,asientos,reportes}.js` |
| Catálogo de cuentas de Costa Rica precargado | PARCIAL | Solo `src/main/resources/db/migration/V16__seed_nomina_parametros_contables.sql` inserta algunas cuentas para nómina. El catálogo completo solo existe como texto en `docs/sprints/SPRINT_5/fases/FASE_2_CONTABILIDAD.md` |
| DDL versionado de las tablas contables | AUSENTE | No hay `CREATE TABLE` de `cuentas_contables`, `asientos_contables` ni `detalles_asiento` en `db/migration` ni en `docs/base de datos/` (patrón: `rg -i 'create table.*(cuentas_contables\|asientos_contables\|detalle_asiento)'`). Solo existe `V13__asientos_nomina_fk.sql` (`ALTER TABLE`) |

**Conclusión de la fase:** el núcleo contable y los reportes funcionan. Las afirmaciones "100 %" e "Integración con Pagos y Facturas" del índice son falsas: los automatismos de venta y cobro no están conectados.

---

## Fase 3: Facturación Electrónica CR v4.4

### Secciones 1 a 4 (configuración, XML, firma, API Hacienda)

| Entregable | Estado | Evidencia |
|---|---|---|
| Entidades `ConfiguracionHacienda`, `ComprobanteElectronico`, `RespuestaHacienda` | IMPLEMENTADO | `modules/facturacion/electronica/model/` (`configuracion_hacienda`, `comprobante_electronico`, `respuesta_hacienda`) |
| Enums FE | IMPLEMENTADO | `electronica/enums/`: `AmbienteHacienda`, `EstadoComprobante`, `TipoComprobanteElectronico` (01 a 09), `MensajeHacienda`, `CondicionVentaFE`, `MedioPagoFE`, `MonedaFE` |
| Repositorios | IMPLEMENTADO | `electronica/repository/` (3 repositorios) |
| Stored procedures | IMPLEMENTADO (SQL manual) | `docs/base de datos/SP_COMPROBANTES_ELECTRONICOS.sql` (8 `CREATE PROCEDURE`). No están en Flyway |
| DDL de tablas FE | AUSENTE | No hay `CREATE TABLE` para `comprobante_electronico`, `configuracion_hacienda` ni `respuesta_hacienda` en ningún `.sql` del repositorio. `docs/base de datos/MIGRATION_FACTURA_FE.sql` solo hace `ALTER TABLE factura` |
| Controllers y vistas | IMPLEMENTADO | `ComprobanteElectronicoController` (`/api/facturas/electronica/comprobantes`, 24 endpoints), `ConfiguracionHaciendaController`, `FacturacionElectronicaViewController`; vistas `templates/modules/facturacion/electronica/{comprobantes,configuracion-hacienda}.html` |
| Generación de XML (Factura, Tiquete) | IMPLEMENTADO | `XmlGeneratorServiceImpl.generarXmlFactura` (l. 33), `generarXmlTiquete` (l. 80) |
| Clave numérica de 50 dígitos | IMPLEMENTADO | `electronica/util/ClaveNumericaGenerator.generar`, `validar`, `extraerInfo` |
| Validación XSD | PARCIAL | `XmlValidator.validarContraXsd` existe, pero no hay ningún `.xsd` en `src/main/resources` (`fd -e xsd src/main/resources` vacío); el método cae a "solo bien formado" |
| Firma digital | IMPLEMENTADO | `FirmaDigitalServiceImpl.firmarXml` (l. 53), XAdES-**BES** con `SignedProperties`. El índice dice XAdES-EPES; el código no incluye `SignaturePolicyIdentifier` |
| OAuth2, envío y consulta en API Hacienda | IMPLEMENTADO | `HaciendaApiServiceImpl`: token con `@Cacheable("haciendaToken")` (l. 53), `enviarComprobante` (l. 157), `consultarComprobante` (l. 249), consulta de mensajes (l. 328) |

### Verificaciones específicas solicitadas

| Entregable | Estado | Evidencia |
|---|---|---|
| **(a) Sección 5: gestión de respuestas de Hacienda** | PARCIAL | Ver el desglose de la tabla siguiente |
| **(b) Resilience4j / circuit breaker** | IMPLEMENTADO | `pom.xml` l. 159-170: `resilience4j-spring-boot3`, `resilience4j-circuitbreaker`, `resilience4j-retry` (+ `spring-boot-starter-aop`). `HaciendaApiServiceImpl` l. 157-158 `@CircuitBreaker(name="haciendaAPI", fallbackMethod="enviarComprobanteFallback")` + `@Retry`, l. 249-250 sobre `consultarComprobante`; fallbacks en l. 457 y l. 484. Configuración en `application.yml` l. 115-140 (`resilience4j.circuitbreaker.instances.haciendaAPI`, `retry.instances.haciendaAPI`, `maxAttempts: 3`) |
| **(c) `@Scheduled` para comprobantes ENVIADO sin respuesta** | IMPLEMENTADO | `electronica/scheduled/ComprobanteReintentosJob.java`: `consultarEstadosPendientes()` con `@Scheduled(cron="0 0 * * * *")` busca `findByEstadoAndFechaEnvioLessThan(EstadoComprobante.ENVIADO, ...)` y llama `consultarYActualizarEstado`. Además, `reintentarEnviosPendientes()` (cada 15 min, `MAX_INTENTOS = 5`) y `limpiezaComprobantesAntiguos()` (02:00). `@EnableScheduling` en `WhatsOrdersManagerApplication` |
| **(d) Nota de Crédito / Nota de Débito de punta a punta** | PARCIAL | Solo existe la generación del XML: `XmlGeneratorServiceImpl.generarXmlNotaCredito` (l. 104) y `generarXmlNotaDebito` (l. 136), despachadas por `ComprobanteElectronicoServiceImpl.generarXml` (l. 706). **No hay ningún flujo que cree una NC/ND:** `generarDesdeFactura` (l. 159) fija `TipoComprobanteElectronico.FACTURA_ELECTRONICA`; ningún endpoint acepta el tipo de comprobante; `anular()` (l. 607) deja `// TODO: Generar nota de crédito para anular` y solo cambia el estado local a ANULADO. Defectos en el XML: la NC usa `<FechaEmision>` de la propia nota como fecha del documento referenciado y fija `Codigo=01`/`Razon=Anulación`; la ND **no** emite `InformacionReferencia`, que v4.4 exige |
| **(e) Ambiente Hacienda por defecto** | IMPLEMENTADO (SANDBOX) | `electronica/model/ConfiguracionHacienda.java` l. 48-49: `@Builder.Default private AmbienteHacienda ambiente = AmbienteHacienda.SANDBOX;`. URLs en `electronica/enums/AmbienteHacienda.java`: SANDBOX → `https://api-sandbox.comprobanteselectronicos.go.cr` + realm `rut-stag`; PRODUCCION → `https://api.comprobanteselectronicos.go.cr` + realm `rut`. No hay ninguna propiedad de ambiente en `application.yml` (ni en los perfiles `dev`/`prod`): el ambiente lo decide el registro `configuracion_hacienda` que el usuario guarda desde la UI (`ConfiguracionHaciendaDTO.ambiente` es `@NotNull`, sin valor por defecto) |

### Desglose de la Sección 5 (gestión de respuestas)

| Entregable | Estado | Evidencia |
|---|---|---|
| `procesarFactura` (generar → firmar → enviar) | PARCIAL | `ComprobanteElectronicoServiceImpl.procesarFactura` (l. 75) llama `procesarFacturaAsync` (l. 115, `@Async`). No existe `@EnableAsync` en el proyecto (`rg '@EnableAsync' src/main/java` sin resultados) y además es una autoinvocación, así que el "envío asíncrono" se ejecuta de forma **síncrona** dentro de la petición HTTP y de la transacción, con `Thread.sleep(30_000)` hasta 5 veces (hasta 150 s de bloqueo) |
| `consultarYActualizarEstado` | IMPLEMENTADO | l. 422: actualiza código, mensaje, fecha, XML de respuesta decodificado de Base64 y estado ACEPTADO/RECHAZADO/ENVIADO |
| Mapeo de estados de Hacienda | IMPLEMENTADO | `mapearEstadoHacienda` (l. 835-857) |
| Historial de respuestas (`RespuestaHacienda`) | AUSENTE | `RespuestaHaciendaRepository` no se inyecta en ninguna clase (`rg 'RespuestaHaciendaRepository' src/main/java` solo encuentra su propia declaración). La respuesta se sobrescribe en `ComprobanteElectronico.xmlRespuesta` |
| Guardar XML en BD | IMPLEMENTADO | `comprobante.setXmlComprobante(xml)` en `generarDesdeFactura`; `setXmlRespuesta` en l. 342-345, 444-447, 794-802 |
| Guardar XML en filesystem | PARCIAL | `guardarXmlEnFilesystem` (l. 665) existe pero no tiene llamadores (`rg guardarXmlEnFilesystem` solo encuentra la definición) |
| Descarga de XML | IMPLEMENTADO | `ComprobanteElectronicoController` `GET /{id}/xml`, `/{id}/xml-content`, `/{id}/xml-respuesta`; `descargarXml` (l. 634) |
| Envío de comprobante por email | IMPLEMENTADO | `enviarPorEmail` (l. 478) usa `emailService.enviarEmailConMultiplesAdjuntos` |
| Notificación al cambiar de estado | AUSENTE | `notificarCambioEstado` (l. 902) y `enviarEmailNotificacion` contienen solo `// TODO` y un log |
| Webhook (callback) de Hacienda | PARCIAL | `HaciendaWebhookController` `POST /api/hacienda/callback` y `procesarCallbackHacienda` (l. 747) existen, pero `modules/seguridad/config/SecurityConfig.java` no hace `permitAll` de `/api/hacienda/**` (solo `/api/whatsapp/webhook`, l. 61) ni lo excluye de CSRF (l. 114-116). Una llamada externa de Hacienda, sin sesión, será rechazada |
| Integración FE en la vista de facturas | IMPLEMENTADO | `ComprobanteElectronicoController.procesarFacturaCompleta` (`POST /procesar/{facturaId}`); vistas en `templates/modules/facturacion/` |

**Conclusión de la fase:** el ciclo factura → XML → firma → envío → consulta en sandbox está implementado, con circuit breaker y jobs. Quedan abiertos NC/ND, el webhook bloqueado por seguridad, el envío asíncrono que en realidad es síncrono, el historial de respuestas, los XSD y el DDL.

---

## Fase 4: Testing

### Clases de test existentes (`fd -e java . src/test`)

| Clase | Módulo | `@Test` | ¿Plan Sprint 5? |
|---|---|---|---|
| `WhatsOrdersManagerApplicationTests` | raíz | 1 | No |
| `modules/contabilidad/service/AsientoNominaIntegrationTest` | Contabilidad (nómina) | 4 | No (cubre `generarAsientoNomina`, no venta/pago) |
| `modules/facturacion/FacturaRepositoryIT` | Facturación | 5 | No |
| `modules/facturacion/electronica/service/impl/XmlGeneratorServiceImplTest` | FE | 3 | Parcial (solo resolución de campos vía `ArticuloMaestro`; no estructura, XSD ni clave) |
| `modules/facturacion/service/TipoCambioDtoMappingTest` | Facturación | 2 | No |
| `modules/facturacion/service/TipoCambioServiceTest` | Facturación | 6 | No |
| `modules/nomina/NominaEntityTest` | Nómina | 12 | No |
| `modules/nomina/PermisoNominaTest` | Nómina | 7 | No |
| `modules/nomina/controller/NominaRestControllerTest` | Nómina | 4 | No |
| `modules/nomina/service/CalculosNominaServiceTest` | Nómina | 8 | No |
| `modules/nomina/service/NominaServiceTest` | Nómina | 6 | No |
| `modules/producto/service/ArticuloMaestroServiceImplTest` | Producto | 10 | No |
| `modules/rrhh/repository/AusenciaRepositoryBetweenTest` | RRHH | 3 | No |
| `modules/rrhh/repository/AusenciaRepositoryTest` | RRHH | 3 | No |
| `modules/rrhh/service/AusenciaServiceTest` | RRHH | 13 | No |
| `modules/rrhh/service/ContratoEmpleadoServiceTest` | RRHH | 12 | No |
| `modules/rrhh/service/DepartamentoServiceTest` | RRHH | 16 | No |
| `modules/rrhh/service/EmpleadoServiceTest` | RRHH | 21 | No |
| `modules/rrhh/service/ParametroCCSSServiceTest` | RRHH | 7 | No |
| `modules/rrhh/service/PuestoServiceTest` | RRHH | 10 | No |
| `modules/rrhh/service/SalarioMinimoServiceTest` | RRHH | 8 | No |
| `modules/rrhh/service/SaldoVacacionesServiceTest` | RRHH | 5 | No |
| `modules/rrhh/service/TramoImpuestoSalarioServiceTest` | RRHH | 5 | No |
| `modules/seguridad/AuditorAwareImplTest` | Seguridad | 2 | No |
| `modules/seguridad/ConfigChangeAuditIT` | Seguridad | 4 | No |
| `modules/seguridad/RememberMeIT` | Seguridad | 7 | No |
| `modules/seguridad/SecurityHeadersIT` | Seguridad | 7 | No |
| `modules/seguridad/UsuarioActividadAuditIT` | Seguridad | 6 | No |
| `modules/seguridad/controller/PerfilControllerTest` | Seguridad | 2 | No |
| `modules/seguridad/security/LoginFailureHandlerTest` | Seguridad | 4 | No |
| `modules/seguridad/security/LoginSuccessHandlerTest` | Seguridad | 2 | No |
| `modules/seguridad/security/RateLimitFilterTest` | Seguridad | 8 | No |
| `modules/seguridad/security/UserDetailsServiceImplTest` | Seguridad | 3 | No |
| `modules/seguridad/service/UserDetailsServiceImplEmailTest` | Seguridad | 2 | No |
| `api/whatsordersmanager/service/PermisoServiceTest` (paquete distinto al de la aplicación) | Seguridad | 22 | No |

Total: 35 clases, 240 métodos `@Test` (conteo con `rg -c '@Test'`). Nota: `pom.xml` no configura `maven-failsafe-plugin`, y la convención por defecto de Surefire no incluye `*IT.java`, así que las 5 clases `*IT` probablemente no se ejecutan con `./mvnw test`.

### Entregables del plan

| Entregable | Estado | Evidencia |
|---|---|---|
| `PagoServiceTest` (8 tests) | AUSENTE | No encontrado (patrón: `fd -i pago src/test`) |
| `AsientoContableServiceTest` (venta/pago/manual) | AUSENTE | No encontrado (`fd -i AsientoContableService src/test`); solo existe `AsientoNominaIntegrationTest` |
| `LibroContableServiceTest` | AUSENTE | No encontrado (`fd -i Libro src/test`) |
| `XMLGeneratorServiceTest` (estructura, XSD, clave, consecutivo) | PARCIAL | `XmlGeneratorServiceImplTest` existe con 3 tests de resolución de campos; no cubre los 5 casos del plan |
| `FirmaDigitalServiceTest` | AUSENTE | No encontrado (`fd -i Firma src/test`) |
| Tests con TestContainers (MySQL) | AUSENTE | Sin dependencia `testcontainers` en `pom.xml`; sin `@Testcontainers` en `src/test` |
| E2E factura + pago + contabilidad | AUSENTE | No encontrado |
| Tests contra Hacienda Sandbox | AUSENTE | No encontrado (`rg -i sandbox src/test`) |
| CI con GitHub Actions | AUSENTE | No existe el directorio `.github/` |
| JaCoCo en `pom.xml` | AUSENTE | `rg -i jacoco pom.xml` sin resultados; los únicos plugins son `spring-boot-maven-plugin` y `maven-compiler-plugin` |
| Cobertura mínima del 80 % | AUSENTE | No se puede medir sin JaCoCo |

### Resultado de ./mvnw test

Ejecutado el 2026-10-10 sobre el commit auditado (`./mvnw test --no-transfer-progress`):

```
Tests run: 211, Failures: 5, Errors: 6, Skipped: 0
BUILD FAILURE
```

Se ejecutaron 211 de los 240 métodos `@Test`. La diferencia (29) coincide exactamente con los métodos de las 5 clases `*IT` (5 + 4 + 7 + 7 + 6), lo que confirma que Surefire no las ejecuta.

| Clase | Fallos | Errores | Causa |
|---|---|---|---|
| `WhatsOrdersManagerApplicationTests.contextLoads` | 0 | 1 | **Entorno:** Flyway no obtiene conexión a MySQL (`Access denied for user 'root'`). No es un defecto de código |
| `AsientoNominaIntegrationTest` | 0 | 3 | Código: `IllegalStateException` "Asiento de nómina no cuadra — debe: 1268300.00, haber: 1536600.00" |
| `LoginFailureHandlerTest` | 3 | 1 | Código: aserciones fallidas y `UnnecessaryStubbing` de Mockito |
| `LoginSuccessHandlerTest` | 1 | 1 | Código: aserción fallida y `UnnecessaryStubbing` |
| `UserDetailsServiceImplTest` | 1 | 0 | Código: `loadUserByUsername_bloqueado_throwsLockedException` |

Ninguna de las clases que fallan pertenece al alcance del Sprint 5. Son fallos preexistentes de Seguridad y Nómina, y no se corrigen en esta auditoría (fuera de alcance).

### Cobertura JaCoCo

JaCoCo **no está configurado** en `pom.xml`. No hay informe de cobertura que registrar.

---

## Fase 5: Documentación

| Entregable (`INDICE_SPRINT_5.md`) | Estado | Evidencia |
|---|---|---|
| `MANUAL_PAGOS.md` | AUSENTE | No encontrado en el árbol (`fd -i MANUAL_PAGOS`) ni en el historial Git (`git log --all -- ':(glob,icase)**/*MANUAL_PAGOS*'`) |
| `MANUAL_CONTABILIDAD.md` | AUSENTE | Igual: árbol e historial vacíos |
| `MANUAL_FACTURACION_ELECTRONICA_CR.md` | AUSENTE | Igual: árbol e historial vacíos |
| `GUIA_CONFIGURACION_HACIENDA.md` | AUSENTE | Igual: árbol e historial vacíos |
| `GUIA_OBTENCION_CERTIFICADO_FIRMA.md` | AUSENTE | Igual: árbol e historial vacíos |
| `FAQ_FACTURACION_ELECTRONICA.md` | AUSENTE | Igual: árbol e historial vacíos |
| Directorio `docs/sprints/SPRINT_5/manuales/` | AUSENTE | No existe |

Documentación técnica relacionada que sí existe (no sustituye a los manuales):

- `docs/sprints/SPRINT_5/fases/guias/GUIA_WEBHOOK_HACIENDA.md`
- `docs/sprints/SPRINT_5/fases/guias/GUIA_INTEGRACION_VISTAS_FE.md`
- `docs/sprints/SPRINT_5/INFRAESTRUCTURA_PAGOS_COMPLETADA.md`
- `docs/base de datos/README_MIGRACION_PAGOS.md`
- Sección "Facturación Electrónica CR" de `README.md`

---

## Discrepancias de documentación

### Documentos referenciados que no existen

Para cada ruta se ejecutó `git log --all --oneline --name-status -- '<ruta>'` y `git log --all --diff-filter=DR --summary -- '**/<basename>'`. Para los documentos del Sprint 5 también se probó `':(glob,icase)**/*<nombre>*'`.

| Documento referenciado | Referenciado en | ¿Existió en el historial? | Commit de borrado o movimiento |
|---|---|---|---|
| `docs/sprints/SPRINT_5/CHECKLIST_SPRINT_5.md` | `INDICE_SPRINT_5.md` | No, nunca | No aplica |
| `docs/sprints/SPRINT_5/RESUMEN_SPRINT_5.md` | `INDICE_SPRINT_5.md` | No, nunca | No aplica |
| `docs/sprints/SPRINT_5/SPRINT_5_PLAN_MAESTRO.md` | `INDICE_SPRINT_5.md` | No, nunca | No aplica |
| `docs/sprints/SPRINT_5/README.md` | `INDICE_SPRINT_5.md` (estructura de archivos) | No, nunca | No aplica |
| `docs/sprints/SPRINT_5/manuales/*` (6 manuales) | `INDICE_SPRINT_5.md` | No, nunca | No aplica |
| `docs/planificacion/DECISIONES_TECNICAS.txt` | `docs/INDICE.md` l. 31 | Sí | Borrado en `025c1b3` y `cf4ae8a` ("cleaning docs", 2026-07-08; dos ramas con el mismo cambio) |
| `docs/arquitectura/ARQUITECTURA_PROYECTO.md` | `docs/INDICE.md` | Sí (movido desde `docs/` en `f0cf4fc`, 2026-01-05) | Borrado en `308ac30` ("creating formatting in docs", 2026-05-07) |
| `docs/arquitectura/COMPONENTES.md` | `docs/INDICE.md` | Sí (movido en `f0cf4fc`) | Borrado en `308ac30` (2026-05-07) |
| `docs/arquitectura/ESTRUCTURA_PROYECTO.md` | `docs/INDICE.md` | Sí (movido en `f0cf4fc`) | Borrado en `308ac30` (2026-05-07) |
| `docs/diseno/INDICE.md` | `docs/INDICE.md` | Sí (añadido en `0906f7d`/`d72b309`) | Borrado en `025c1b3`/`cf4ae8a` (2026-07-08) |
| `docs/diseno/MOCKUPS_VISUALES.md` | `docs/INDICE.md` | Sí (añadido en `0906f7d`/`d72b309`) | Borrado en `025c1b3`/`cf4ae8a` (2026-07-08) |
| `docs/diseno/README.md` | `docs/INDICE.md` | Sí (añadido en `0906f7d`/`d72b309`) | Borrado en `025c1b3`/`cf4ae8a` (2026-07-08) |
| `docs/guias/GUIA_INICIALIZACION_PREFERENCIAS.md` | `docs/INDICE.md` | Sí (movido en `f0cf4fc`) | Borrado en `308ac30` (2026-05-07) |
| `docs/guias/GUIA_LOGGING.md` | `docs/INDICE.md` | Sí (movido en `f0cf4fc`) | Borrado en `308ac30` (2026-05-07) |
| `docs/guias/GUIA_MENU_HAMBURGUESA.md` | `docs/INDICE.md` | No, nunca | No aplica |
| `docs/guias/GUIA_TABLAS_RESPONSIVE.md` | `docs/INDICE.md` | No, nunca | No aplica |
| `docs/permisos/MAPEO_SISTEMA_PERMISOS.md` | `docs/INDICE.md` | Sí (movido en `f0cf4fc`) | Borrado en `308ac30` (2026-05-07) |
| `docs/permisos/RESUMEN_ROLES_PERMISOS.md` | `docs/INDICE.md` | Sí (movido en `f0cf4fc`) | Borrado en `308ac30` (2026-05-07) |
| `docs/refactorizacion/INDICE_REFACTORIZACION.md` | `docs/INDICE.md` | Sí | Borrado en `025c1b3`/`cf4ae8a` (2026-07-08) |
| `docs/reportes/ESTADO_PROYECTO.md` | `docs/INDICE.md` | Sí (movido en `f0cf4fc`) | Borrado en `308ac30` (2026-05-07) |
| `docs/reportes/PROXIMOS_PASOS.md` | `docs/INDICE.md` | Sí (movido en `f0cf4fc`) | Borrado en `308ac30` (2026-05-07) |

En total, `docs/INDICE.md` tiene 16 enlaces relativos rotos. Los documentos de planificación del Sprint 5 (checklist, resumen, plan maestro, README y manuales) **nunca se versionaron**: el directorio `docs/sprints/SPRINT_5/` aparece por primera vez en `6cfdc72`/`da83aa0` ("restoring sprints documentation", 2026-07-09) sin ellos.

### Afirmaciones de la documentación que el código contradice

| Afirmación | Fuente | Realidad en el código |
|---|---|---|
| "Pagos 0/45" | `INDICE_SPRINT_5.md` | El módulo está implementado; falta la integración contable |
| "Contabilidad 100 %, integración con Pagos y Facturas" | `INDICE_SPRINT_5.md` | `generarAsientoVenta`/`generarAsientoPago` no tienen llamadores; los endpoints son stubs que devuelven 400 |
| "Circuit Breaker con Resilience4j (pendiente)", "Job de reintentos (pendiente)" | `INDICE_SPRINT_5.md` | Ambos implementados (`HaciendaApiServiceImpl`, `ComprobanteReintentosJob`) |
| "Firma digital XAdES-EPES" | `INDICE_SPRINT_5.md` | El código firma XAdES-BES (`FirmaDigitalServiceImpl` l. 54) |
| "Soporte: Factura, Tiquete, NC, ND" | `INDICE_SPRINT_5.md` | NC/ND solo generan XML; no se pueden emitir |
| "Envío asíncrono a la API de Hacienda" | `README.md` l. 98 | Sin `@EnableAsync`: el envío es síncrono y bloqueante |
| "Sprint 5 completado" | `README.md` l. 326 y l. 364 | Testing y Documentación sin hacer; el propio README deja NC/ND y producción como pendientes (l. 345, l. 347) |
| "Enum `MetodoPago` con 8 métodos" | `INDICE_SPRINT_5.md` | El enum tiene 6 valores |

---

## Conclusión

### Porcentaje real estimado por fase

| Fase | Índice (2026-01-23) | README (2026-08) | **Estimación de esta auditoría** | Comentario |
|---|---|---|---|---|
| 1. Pagos | 0 % | completo | **~85 %** | CRUD, vistas, conciliación, estado de cuenta e integración con Factura hechos. Faltan el asiento automático al registrar o anular un pago, la validación de fecha y la migración Flyway |
| 2. Contabilidad | 100 % | completo | **~80 %** | Faltan los automatismos de venta y cobro conectados (con el bug de `Long.getLong`), `PeriodoContable`, libro diario, el catálogo CR precargado y el DDL versionado |
| 3. FE CR v4.4 | 95 % | completo | **~75 %** | Faltan NC/ND de punta a punta, un webhook accesible, envío realmente asíncrono, el historial `RespuestaHacienda`, notificaciones, los XSD y el DDL. Ambiente por defecto: SANDBOX |
| 4. Testing | 0 % | no mencionado | **~5 %** | Solo un test FE parcial. Sin tests de Pagos, Contabilidad (venta/pago) ni Firma; sin TestContainers, CI ni JaCoCo. La suite actual no está en verde: 211 tests, 5 fallos y 6 errores, todos fuera del alcance del Sprint 5; además, los `*IT` no se ejecutan. Esto confirma la estimación. |
| 5. Documentación | 0 % | no mencionado | **0 %** | Ninguno de los 6 manuales existe ni existió en el historial |

Estimación global ponderada por número de tareas del plan (45/58/58/32/12): **aproximadamente 63 %**.

### Lo que falta para cerrar el Sprint 5 (por prioridad)

1. **Integración contable de ventas y cobros (P1, bloqueante funcional).** Llamar a `generarAsientoPago` desde `PagoServiceImpl.registrarPago` y reversar el asiento en `anularPago`. Conectar `generarAsientoVenta` en la emisión de factura, usando `ParametroContable` en lugar de IDs de cuenta por parámetro. Corregir `Long.getLong(...)` en `AsientoContableService` l. 364 y `AsientoContableMapper` l. 54. Eliminar o implementar los stubs `/api/generar-venta` y `/api/generar-pago`.
2. **Webhook de Hacienda accesible (P1).** Añadir `/api/hacienda/callback` a `permitAll` y a `ignoringRequestMatchers` de CSRF en `SecurityConfig`, manteniendo la validación por `X-Webhook-Token`.
3. **Envío asíncrono real (P1).** Añadir `@EnableAsync` y mover `procesarFacturaAsync` a otro bean (o publicar un evento) para quitar el `Thread.sleep` del hilo HTTP y de la transacción.
4. **Esquema de base de datos versionado (P1).** Crear migraciones Flyway para `pagos`, las tablas contables y las tablas FE (hoy dependen de scripts manuales o no tienen DDL, con `ddl-auto: validate`).
5. **NC/ND electrónicas (P2).** Permitir crear comprobantes de tipo 02/03 que referencien la factura original (fecha y clave reales), emitir `InformacionReferencia` en la ND y conectar `anular()` con la emisión de una NC.
6. **Testing mínimo (P2).** `PagoServiceTest`, `AsientoContableServiceTest` (venta/pago), `FirmaDigitalServiceTest`, tests de `ClaveNumericaGenerator`/XML; JaCoCo en `pom.xml`; failsafe para `*IT`; workflow de GitHub Actions. Antes de exigir CI en verde hay que resolver los 10 fallos de código preexistentes de la suite (Seguridad y Nómina).
7. **Gestión de respuestas completa (P3).** Persistir `RespuestaHacienda`, implementar `notificarCambioEstado`/`enviarEmailNotificacion`, incluir los XSD v4.4 en `src/main/resources/xsd/` y llamar a `guardarXmlEnFilesystem` o eliminarlo.
8. **Contabilidad complementaria (P3).** `PeriodoContable` (cierre de periodos), libro diario y migración con el catálogo de cuentas CR.
9. **Documentación (P3).** Los 6 manuales de Fase 5, y corregir `INDICE_SPRINT_5.md`, `docs/INDICE.md` (16 enlaces rotos) y `README.md` para que reflejen este estado.

### Recomendación

**No cerrar el Sprint 5 en su estado actual.** Antes de iniciar el Sprint 6 hay que completar los puntos 1 a 4 (integración contable de pagos y ventas, webhook accesible, envío asíncrono real y migraciones versionadas), porque afectan a la exactitud de la contabilidad y a la operación con Hacienda. Además, conviene llegar a una base mínima de testing (punto 6) para los flujos de Pagos, Contabilidad y FE. Los puntos 5, 7, 8 y 9 se pueden replanificar explícitamente como deuda del Sprint 6, siempre que `README.md` e `INDICE_SPRINT_5.md` se actualicen para dejar de declarar el sprint como completo.
