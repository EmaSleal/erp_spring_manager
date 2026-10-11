# Auditoría de alcance real de los Sprints 6 a 9

| Campo | Valor |
|---|---|
| Fecha de auditoría | 2026-10-10 |
| Commit auditado | `e0a14813837d71993c0196af23c0a9bc91bca60f` |
| Rama | `t-002-inventario-de-alcance-de-los-sprints-6-a` |
| Alcance | Sprint 6 (Multi-divisa, Inventario, Proveedores), Sprint 7 (Producción, Mejoras técnicas, Seguridad), Sprint 8 (RRHH, Nómina, Reportes financieros), Sprint 9 (UX/UI, PWA, Optimizaciones), más sus fases de Testing y Documentación |
| Método | Evidencia tomada del código fuente (`src/`, `pom.xml`, `application.yml`, migraciones Flyway) y del historial Git. La documentación (`docs/sprints/SPRINT_N/INDICE_SPRINT_N.md` y `fases/*.md`) solo se usó para obtener la lista de entregables. |
| Referencia | Formato y criterios de `docs/sprints/SPRINT_5/AUDITORIA_ESTADO_SPRINT_5.md` (rama `t-001-auditoria-del-estado-real-del-sprint-5-c`). No se vuelve a auditar el Sprint 5; se citan sus hallazgos cuando afectan a estos sprints. |

## Contexto: documentación desactualizada

- Los cuatro índices (`INDICE_SPRINT_6.md` a `INDICE_SPRINT_9.md`) declaran "📋 PLANIFICADO" y todas las fases "0/N tareas". Ninguna casilla de `fases/*.md` está marcada (`rg -c '\- \[x\]'` devuelve 0 en los 19 archivos de fase).
- El historial contradice esa situación. Hay commits explícitos de implementación: `c1e2e0d` (multi-divisa, 2026-07-14), `f3e1f11` ("módulo de inventario Sprint 6 Fase 2", 2026-07-14), `fb86126` ("módulo de proveedores Sprint 6 Fase 3", 2026-07-14), `9ee693e` ("Sprint 7 - lockout, audit, email auth, LocalDateTime, headers, rate-limit, remember-me", 2026-07-17), `bbba19a` (RRHH fases 1 y 2, 2026-07-22) y `408d6de` (nómina, 2026-07-23).
- Los documentos de control que citan los índices (`CHECKLIST_SPRINT_N.md`, `RESUMEN_SPRINT_N.md`, `SPRINT_N_PLAN_MAESTRO.md`, N = 6 a 9) no existen en el árbol (`fd -i 'CHECKLIST_SPRINT_[6-9]|RESUMEN_SPRINT_[6-9]|SPRINT_[6-9]_PLAN_MAESTRO' docs` sin resultados).
- **Sprint 6:** el índice referencia `fases/FASE_4_TESTING.md` y `fases/FASE_5_DOCUMENTACION.md`, pero **no existen**. Solo hay `FASE_1_MULTI_DIVISA.md`, `FASE_2_INVENTARIO.md` y `FASE_3_PROVEEDORES.md`. Para las Fases 4 y 5 se usaron los entregables que lista el propio índice.
- **Sprint 8:** además de las cinco fases existe `fases/Cumplimiento normativo laboral y de seguridad social de Costa Rica para un módulo de RRHH en un ERP (2026).md`, una referencia normativa añadida en `552f770` (2026-07-23).
- Los Sprints 7 y 9 tienen sus cinco archivos de fase.

Leyenda de estado: **IMPLEMENTADO** (existe y está conectado al flujo), **PARCIAL** (existe pero incompleto, sin conectar o con defectos que impiden su uso), **AUSENTE** (no hay código).

Todas las rutas Java son relativas a `src/main/java/api/astro/whats_orders_manager/`. Las plantillas son relativas a `src/main/resources/templates/`.

---

## Sprint 6: Multi-divisa, Inventario y Proveedores

### Fase 1: Multi-divisa

| Entregable | Estado | Evidencia |
|---|---|---|
| Modelo `Moneda` (USD, EUR, CRC…) | PARCIAL | No hay entidad `Moneda`. Las monedas son un enum fijo de 3 valores: `modules/facturacion/electronica/enums/MonedaFE.java` (`CRC`, `USD`, `EUR`, con símbolo). Para añadir una moneda hay que cambiar el código |
| Modelo `TipoCambio` con histórico | IMPLEMENTADO | `modules/facturacion/model/TipoCambio.java` (`@Table(name="tipos_cambio")`, `monedaOrigen`, `monedaDestino`, `fecha`, `tasaCompra`, `tasaVenta`, `fuente`); `TipoCambioService.getTasaActual`, `registrar`, `listarPorMoneda`; `TipoCambioRestController` (`/api/tipos-cambio/actual`, `POST /api/tipos-cambio`, `/historial`) |
| Servicio de actualización de tasas (API externa) | AUSENTE | `modules/facturacion/enums/FuenteTipoCambio.java` declara `BCCR` y `API_EXTERNA`, pero no hay cliente HTTP ni `@Scheduled` que obtenga tasas. `TipoCambioService` solo tiene `registrar` (carga manual, `hasRole('ADMIN')`). Patrón: `rg -i 'bccr\|WebClient\|RestTemplate' modules/facturacion/service/TipoCambioService.java` |
| Conversión automática en transacciones | PARCIAL | Factura: `Factura.monedaFE`, `Factura.tipoCambio` (l. 130, 137); `FacturaServiceImpl.save` (l. 131-138) exige `tipoCambio > 0` si la moneda no es CRC; el formulario lo precarga desde `/api/tipos-cambio/actual` (`static/modules/facturacion/js/factura-fe-utils.js` l. 98). **Sin conversión** en `OrdenCompra.moneda`, `CuentaPorPagar.moneda` ni en los asientos (ver "Integración contable transversal") |
| Reportes multi-divisa | PARCIAL | `Factura.getTotalEnCRC()` (l. 192) consolida en CRC; lo usan `ReporteServiceImpl` (l. 116, 137, 385, 430) y `ReporteController` (l. 170). No hay reportes por moneda ni de diferencial cambiario |
| Integración con `formatearMoneda()` | IMPLEMENTADO | `shared/service/MonedaService.java`: `obtenerSimboloPorCodigo(String)` (l. 55), `formatearConMoneda` (l. 101) |
| Integración con XML de FE | IMPLEMENTADO | `XmlGeneratorServiceImpl` l. 375-382 emite `CodigoTipoMoneda` con `CodigoMoneda` y `TipoCambio` |
| DDL versionado de `tipos_cambio` | AUSENTE | Sin `CREATE TABLE` en `db/migration` ni en `docs/base de datos/` (solo aparece como texto en `docs/planificacion/MEJORAS_FUTURAS.md`). `ddl-auto: validate` (`application.yml` l. 35) |

### Fase 2: Inventario avanzado

| Entregable | Estado | Evidencia |
|---|---|---|
| Modelo `MovimientoInventario` | IMPLEMENTADO | `modules/inventario/model/MovimientoInventario.java` (`movimientos_inventario`); `TipoMovimientoInventario` (9 tipos) |
| Registro de movimientos con bloqueo y costo promedio | IMPLEMENTADO | `MovimientoInventarioServiceImpl.registrarSalida` (l. 34, `findByIdForUpdate` y validación de stock insuficiente) y `registrarEntrada` (l. 86, `actualizarCostoPromedio`) |
| Salida automática por venta | IMPLEMENTADO | `FacturaServiceImpl.save` l. 180 → `registrarSalidasInventario` (l. 278), que hace `SALIDA_VENTA` por línea |
| Entrada automática por compra | IMPLEMENTADO | `OrdenCompraServiceImpl.recibirParcial` → `movimientoService.registrarEntrada(... ENTRADA_COMPRA ...)` (l. 152) |
| Reversión de stock al eliminar una factura | AUSENTE | `FacturaServiceImpl.eliminarConValidacion` (l. 311) y `deleteById` (l. 188) borran la factura sin registrar `ENTRADA_DEVOLUCION`. Ningún llamador usa `ENTRADA_DEVOLUCION` ni `SALIDA_DEVOLUCION` (`rg 'ENTRADA_DEVOLUCION' src/main/java` solo encuentra el enum) |
| Kardex por producto | IMPLEMENTADO | `MovimientoInventarioService.obtenerKardex`, `obtenerKardexTodos`; `KardexRestController` (`/api/inventario/kardex`, `/{productoId}`); vista `modules/inventario/kardex.html` |
| Lotes y vencimientos | PARCIAL | `LoteProducto.java` (`lotes_productos`), `LoteProductoServiceImpl.crearLote` (l. 29), `marcarVencidos`, `findLotesPorVencer`; consumo FEFO en `aplicarSalidaConLotes` (`findByProductoAndEstadoOrderByFechaVencimientoAsc`). **Defecto:** en `OrdenCompraServiceImpl.recibirParcial`, `crearLote(..., aRecibir, ...)` (l. 142) crea el lote con `cantidad = aRecibir` y a continuación `registrarEntrada(..., lote)` suma otra vez `aRecibir` al lote (`lote.setCantidad(lote.getCantidad() + cantidad)`, `MovimientoInventarioServiceImpl` l. 128). Cada lote recibido queda con el doble de unidades que el stock |
| Alertas de stock mínimo/bajo | PARCIAL | `AlertaInventarioService.verificarAlertas` (`@Scheduled(cron="0 0 6 * * *")`) solo escribe en el log (`log.warn`); no genera notificación ni correo. La vista `inventario/stock.html` muestra los productos críticos y los lotes por vencer (`InventarioViewController.stock`, l. 34) |
| Reporte de rotación de inventario | AUSENTE | No encontrado (patrón: `rg -i rotacion src/main`) |
| Ajustes de inventario (positivos/negativos) con aprobación | IMPLEMENTADO | `AjusteInventarioServiceImpl.crear`, `aprobar` (l. 86 `registrarEntrada`, l. 91 `registrarSalida`), `rechazar`; `AjusteInventarioRestController` (`/api/inventario/ajustes`, `/{id}/aprobar`, `/{id}/rechazar`); vista `inventario/ajustes.html` |
| Activar `PRODUCTO_AJUSTAR_INVENTARIO` | PARCIAL | Se creó un permiso nuevo, `INVENTARIO_AJUSTAR` (`seguridad/enums/Permiso.java` l. 105), que protege los ajustes. `PRODUCTO_AJUSTAR_INVENTARIO` sigue mapeado a `/productos/inventario` (`PermisoServiceImpl` l. 335), una ruta que no existe en ningún controller |
| Filtro `stockBajo` en reportes | IMPLEMENTADO | `ReporteServiceImpl.generarReporteProductos(stockBajo, sinVentas)` (l. 251, filtro en l. 262); `ReporteController` l. 246; `reportes/productos.html` l. 51-57 |
| Asiento contable de movimientos de inventario | AUSENTE | Ningún archivo de `modules/inventario` referencia `AsientoContableService` (`rg -i asiento modules/inventario` sin resultados) |
| DDL versionado | AUSENTE | No hay `CREATE TABLE` de `movimientos_inventario`, `lotes_productos`, `ajustes_inventario` ni `detalles_ajuste_inventario` fuera de `docs/sprints/SPRINT_6/fases/FASE_2_INVENTARIO.md` |

### Fase 3: Proveedores (cuentas por pagar)

| Entregable | Estado | Evidencia |
|---|---|---|
| Modelo `Proveedor` | IMPLEMENTADO | `modules/proveedores/model/Proveedor.java` (`proveedores`), `ProveedorItem.java`; `ProveedorServiceImpl`; `ProveedorRestController` (`/api/proveedores`, CRUD + `/buscar`); vistas `proveedores/proveedores.html`, `proveedor-form.html` |
| Modelo `OrdenCompra` | IMPLEMENTADO | `OrdenCompra.java` (`ordenes_compra`, `recalcularTotales()` l. 112), `DetalleOrdenCompra.java`; `OrdenCompraServiceImpl` (crear, aprobar, `cancelar` l. 103, `recibirParcial` l. 115); `OrdenCompraRestController` (`/api/ordenes-compra`, `/{id}/aprobar`, `/cancelar`, `/recibir`, `/generar-cpp`); vista `proveedores/ordenes-compra.html` |
| Modelo `CuentaPorPagar` | IMPLEMENTADO | Movido a `modules/contabilidad/model/CuentaPorPagar.java` (`cuentas_por_pagar`) en `b4ba9d8`; `CuentaPorPagarServiceImpl.crear` (l. 39), `crearDesdeOrdenCompra` (l. 70), `actualizarVencidas`, `aplicarPago`, `revertirPago`; `CuentaPorPagarRestController` (`/api/cuentas-pagar`); vista `contabilidad/cuentas-pagar.html` |
| Gestión de pagos a proveedores | IMPLEMENTADO | `PagoProveedorServiceImpl.registrarPago` (l. 37) y `anularPago` (l. 78); endpoints `POST /api/cuentas-pagar/{id}/pagar` y `/pagos/{pagoId}/anular` |
| Asientos automáticos de compra y de pago a proveedor | PARCIAL | Se generan en `CuentaPorPagarServiceImpl` l. 98 y `PagoProveedorServiceImpl` l. 69. Faltan las reversiones y el asiento de la CxP manual (ver "Integración contable transversal") |
| Historial de compras y pagos por proveedor | PARCIAL | Existen `OrdenCompraRepository.findByProveedorOrderByFechaDesc` y `CuentaPorPagarRepository.findByProveedorOrderByFechaVencimientoAsc`, pero ningún servicio ni controller los usa (`rg` solo encuentra las declaraciones). Hay listados globales (`/api/cuentas-pagar/pagos?desde&hasta`), no por proveedor |
| Evaluación de proveedores | PARCIAL | Solo el campo `Proveedor.calificacion` (l. 95). No hay lógica ni vista de evaluación |
| Conciliación de cuentas | AUSENTE | No encontrado (patrón: `rg -i concilia modules/proveedores modules/contabilidad/service/impl/CuentaPorPagarServiceImpl.java`) |
| DDL versionado | AUSENTE | No hay `CREATE TABLE` de `proveedores`, `proveedor_item`, `ordenes_compra`, `detalle_ordenes_compra`, `pagos_proveedor` ni `cuentas_por_pagar` fuera de `FASE_3_PROVEEDORES.md`. Además, los nombres del documento (`detalles_orden_compra`, `pagos_proveedores`) difieren de los de las entidades (`detalle_ordenes_compra`, `pagos_proveedor`) |

### Fase 4: Testing (archivo de fase inexistente; entregables tomados del índice)

| Entregable | Estado | Evidencia |
|---|---|---|
| Tests de conversión de divisas | PARCIAL | `src/test/.../modules/facturacion/service/TipoCambioServiceTest.java` (6 tests) y `TipoCambioDtoMappingTest.java` (2). No prueban la conversión en las transacciones |
| Tests de movimientos de inventario (kardex) | AUSENTE | `fd . src/test/java/api/astro/whats_orders_manager/modules/inventario` sin resultados |
| Tests de cuentas por pagar / proveedores | AUSENTE | Sin directorio `src/test/.../modules/proveedores`; `fd -i 'CuentaPorPagar\|PagoProveedor' src/test` sin resultados |
| 4 tests E2E y cobertura ≥ 75 % | AUSENTE | Sin JaCoCo (`rg -i jacoco pom.xml` vacío) |

### Fase 5: Documentación (archivo de fase inexistente)

| Entregable | Estado | Evidencia |
|---|---|---|
| `MANUAL_MULTI_DIVISA.md`, `MANUAL_INVENTARIO.md`, `MANUAL_PROVEEDORES.md`, `GUIA_CONFIGURACION_API_TASAS.md` | AUSENTE | `fd -i 'MANUAL_(MULTI\|INVENTARIO\|PROVEEDORES)\|GUIA_CONFIGURACION_API' docs` sin resultados |

**Conclusión del sprint:** Inventario y Proveedores están construidos de punta a punta (contrario al "0/48" y "0/44" de los índices), pero tienen defectos de cantidades (lotes duplicados, ventas eliminadas sin devolver el stock) y ninguna tabla tiene DDL versionado. Multi-divisa se limita a facturas con tipo de cambio manual.

---

## Sprint 7: Producción, Mejoras técnicas y Seguridad

### Fase 1: Producción (opcional)

| Entregable | Estado | Evidencia |
|---|---|---|
| `OrdenProduccion`, `RecetaProduccion` (BOM), `ProcesoProduccion` | AUSENTE | `rg -l -i 'OrdenProduccion\|RecetaProduccion\|ProcesoProduccion\|BOM' src/main` sin resultados |
| Materias primas, costeo, órdenes de trabajo, consumo de materiales, producto terminado | AUSENTE | Único rastro: el valor `SALIDA_PRODUCCION` de `TipoMovimientoInventario`, sin uso |

### Fase 2: Mejoras técnicas

| Entregable | Estado | Evidencia |
|---|---|---|
| Migración de username (teléfono) a email | IMPLEMENTADO | `UserDetailsServiceImpl.loadUserByUsername` (l. 31-35): `findByEmail(...).or(() -> findByNombre(...))`; `db/migration/V1__email_not_null_unique.sql`; commit `9ee693e` |
| Migración de `Timestamp` a `LocalDateTime` | IMPLEMENTADO | Ninguna entidad usa `java.sql.Timestamp` ni `java.util.Date`. Quedan usos legítimos de lectura JDBC (`ComprobanteElectronicoServiceImpl` l. 733, `WhatsAppFacturaService` l. 329) y `java.util.Date` en `ParametroSistemaService` y `ExportServiceImpl` |
| Remember me | IMPLEMENTADO | `SecurityConfig` l. 136-139 (`rememberMe`, `jdbcTokenRepository()`); `db/migration/V2__persistent_logins.sql`; test `RememberMeIT` |
| Sistema de auditoría completo | IMPLEMENTADO | `config/PersistenceConfig.java` (`@EnableJpaAuditing(auditorAwareRef="auditorAwareImpl")`), `seguridad/config/AuditorAwareImpl.java`; `UsuarioActividadService` (`registrarLogin`, `registrarActividadFallida`), llamado desde `LoginSuccessHandler`, `LoginFailureHandler`, `AppLogoutSuccessHandler`, `PermisoAdminController`, `ParametroSistemaRestController`, `ConfiguracionEmpresaRestController` |
| Optimización de queries N+1 | PARCIAL | `JOIN FETCH`/`@EntityGraph` solo en 5 repositorios (`AusenciaRepository`, `LineaFacturaRepository`, `RolRepository`, `ProductoRepository`, `ParametroContableRepository`); último arreglo en `98c6ce9` (2026-08-26, `LazyInitializationException` en reportes) |
| Caché de segundo nivel (Redis) | AUSENTE | `config/CacheConfig.java` usa `SimpleCacheManager` + `ConcurrentMapCache` (en memoria, sin TTL). Sin Redis ni caché de segundo nivel de Hibernate (`rg -i 'redis\|hibernate.cache' pom.xml application.yml` vacío) |
| Paginación mejorada | PARCIAL | `shared/util/PaginacionUtil.java`, `shared/dto/PaginacionDTO.java`; `Pageable` en 9 controllers. Los listados de inventario, proveedores y nómina no paginan |
| Validaciones exhaustivas | PARCIAL | 18 `@Valid` en 9 archivos de `src/main/java` |
| Manejo de excepciones centralizado | AUSENTE | `config/GlobalControllerAdvice.java` es un `@ControllerAdvice` sin ningún `@ExceptionHandler`. Solo hay handlers locales en 3 controllers de contabilidad |

### Fase 3: Seguridad avanzada

| Entregable | Estado | Evidencia |
|---|---|---|
| 2FA / MFA | AUSENTE | `rg -i 'totp\|twofactor\|mfa' src/main/java pom.xml` sin resultados |
| JWT con refresh tokens | AUSENTE | Sin dependencia JWT; `ApiAuthController` (`POST /api/auth/login`) autentica con `AuthenticationManager` y sesión |
| Bloqueo por intentos fallidos | IMPLEMENTADO | `LoginFailureHandler` (`FAILURE_THRESHOLD = 6`, l. 25; bloqueo en l. 72-76); `UsuarioServiceImpl.incrementarIntentosFallidos` (l. 173) y `resetearIntentosFallidos` (l. 188). Sus tests fallan (ver Sprint 5, "Resultado de ./mvnw test") |
| Políticas de contraseñas robustas | PARCIAL | `shared/util/PasswordUtil.esPasswordValida` (l. 63) solo comprueba la longitud mínima, y **no tiene llamadores** |
| Encriptación de datos sensibles | PARCIAL | Solo `configuracion/converter/SmtpPasswordConverter.java` (AES-256-GCM sobre la contraseña SMTP). Las contraseñas de usuario usan BCrypt (`SecurityConfig` l. 202) |
| Rate limiting | IMPLEMENTADO | `seguridad/security/RateLimitFilter.java` (Bucket4j, `pom.xml` l. 174-177), limitado a `/auth/login` y `/api/auth/login`; `RateLimitFilterConfig` |
| CORS | IMPLEMENTADO | `SecurityConfig.corsConfigurationSource()` (l. 218), orígenes desde `app.cors.allowed-origins` |
| Headers de seguridad (CSP, X-Frame-Options, HSTS, Referrer-Policy) | IMPLEMENTADO | `SecurityConfig` l. 155-172; test `SecurityHeadersIT` |
| Sanitización de inputs (XSS, SQLi) | PARCIAL | No hay un sanitizador explícito (`rg -i 'jsoup\|owasp\|sanitiz'` vacío). Se depende del escape por defecto de Thymeleaf y de las consultas parametrizadas de JPA |
| Logging de seguridad | IMPLEMENTADO | `UsuarioActividadService.registrarLoginFallido` (desde `LoginFailureHandler` l. 45, 50); `log.warn` en `RateLimitFilter` l. 60 |

### Fase 4: Testing

| Entregable | Estado | Evidencia |
|---|---|---|
| Tests de seguridad, autenticación y bloqueo | PARCIAL | 11 clases en `src/test/.../modules/seguridad` (`LoginFailureHandlerTest`, `LoginSuccessHandlerTest`, `RateLimitFilterTest`, `UserDetailsServiceImplTest`, `RememberMeIT`, `SecurityHeadersIT`, etc.) y `src/test/java/api/whatsordersmanager/service/PermisoServiceTest.java`. Según la auditoría del Sprint 5, 3 de ellas fallan y las `*IT` no se ejecutan con Surefire |
| Tests de mejoras técnicas | PARCIAL | `AuditorAwareImplTest`, `ConfigChangeAuditIT`, `UsuarioActividadAuditIT`, `UserDetailsServiceImplEmailTest` |
| Tests de producción | AUSENTE | No hay módulo de producción |
| Cobertura ≥ 75 % | AUSENTE | Sin JaCoCo |

### Fase 5: Documentación

| Entregable | Estado | Evidencia |
|---|---|---|
| Manual de Producción, Guía de Seguridad Avanzada, Manual de Mejoras Técnicas, Guía 2FA | AUSENTE | `fd -i 'MANUAL_PRODUCCION\|GUIA_SEGURIDAD\|MANUAL_MEJORAS\|GUIA.*2FA' docs` sin resultados |

**Conclusión del sprint:** el commit `9ee693e` implementó de una vez el núcleo de las Fases 2 y 3 (email como usuario, `LocalDateTime`, remember me, auditoría, bloqueo, rate limiting y headers). Producción, 2FA, JWT, Redis y el manejo centralizado de excepciones no existen.

---

## Sprint 8: RRHH, Nómina y Reportes financieros

### Fase 1: RRHH

| Entregable | Estado | Evidencia |
|---|---|---|
| Modelo `Empleado` | IMPLEMENTADO | `modules/rrhh/model/Empleado.java`; `EmpleadoServiceImpl`; `EmpleadoRestController`, `EmpleadoViewController` (paginado); vistas `rrhh/empleados/{lista,form,detalle}.html`; `V5__rrhh_empleados.sql` |
| Modelo `Departamento` | IMPLEMENTADO | `Departamento.java`, `DepartamentoServiceImpl`, `DepartamentoRestController`/`ViewController`, `rrhh/departamentos/lista.html`; `V3__rrhh_catalogos_base.sql` |
| Modelo `Puesto` | IMPLEMENTADO | `Puesto.java`, `PuestoServiceImpl`, `PuestoRestController`/`ViewController`, `rrhh/puestos/lista.html` |
| Contratos laborales | IMPLEMENTADO | `ContratoEmpleado.java`, `ContratoEmpleadoServiceImpl`, vistas `rrhh/contratos/{lista,form,detalle}.html`; `V6__rrhh_contratos_ausencias.sql`, `V8__rrhh_contratos_justificacion.sql` |
| Control de asistencia (marcas) | AUSENTE | `rg -l -i 'asistencia\|marcacion' modules/rrhh` sin resultados |
| Vacaciones y permisos | IMPLEMENTADO | `Ausencia.java` + `TipoAusencia` (`VACACIONES`, `INCAPACIDAD_CCSS`, `INCAPACIDAD_INS`, `PERMISO_SIN_GOCE`, `PERMISO_CON_GOCE`…); `AusenciaServiceImpl`; `SaldoVacaciones.java` + `SaldoVacacionesServiceImpl`; vistas `rrhh/ausencias/*`; `V10__rrhh_saldo_vacaciones.sql`; autoservicio `/perfil` (`1f5ea25`) |
| Evaluaciones de desempeño | AUSENTE | `rg -i desempe modules/rrhh` sin resultados |
| Historial laboral | PARCIAL | Solo implícito en la lista de contratos por empleado; no hay una entidad de historial |
| Expediente digital | AUSENTE | `rg -i 'expediente\|DocumentoEmpleado' modules/rrhh` sin resultados |
| Catálogos regulatorios CR (CCSS, salarios mínimos, tramos de renta) | IMPLEMENTADO (extra) | `ParametroCCSS.java`, `SalarioMinimo.java`, `TramoImpuestoSalario.java` con sus servicios; `CatalogoRegulatorioViewController`; `V4__rrhh_catalogos_regulatorios.sql` (obrero 10.83 %, patronal 26.83 %) |
| Dashboard RRHH | IMPLEMENTADO (extra) | `RrhhDashboardController`, `rrhh/dashboard.html` |

### Fase 2: Nómina

| Entregable | Estado | Evidencia |
|---|---|---|
| Modelos `Nomina` y `DetalleNomina` | IMPLEMENTADO | `modules/nomina/model/Nomina.java`, `DetalleNomina.java`; `V12__nomina_tables.sql`, `V14__nomina_periodo_unique.sql`, `V15__nomina_audit_columns_to_int.sql` |
| Ciclo de vida (crear, calcular, aprobar, contabilizar, anular) | IMPLEMENTADO | `NominaService.crear` (l. 81), `calcular` (l. 124), `aprobar` (l. 234), `contabilizar` (l. 260), `anular` (l. 291); `NominaRestController` (`/api/nomina`, `/{id}/calcular`, `/aprobar`, `/contabilizar`); vistas `nomina/{lista,ver}.html` |
| Cálculo de salario ordinario, extra y nocturno | PARCIAL | Solo el ordinario prorrateado por horas ausentes no pagadas: `CalculosNominaService.calcular` (l. 170-246) y `NominaService.calcular` (8 h por ausencia sin goce). Sin horas extra ni nocturnas (`rg -i 'extra\|nocturn' modules/nomina` sin resultados) |
| Deducción CCSS obrero | IMPLEMENTADO | `CalculosNominaService.calcularCcssObrero` (l. 55), porcentaje desde `ParametroCCSS` |
| Deducción INS | IMPLEMENTADO (a validar) | `calcularIns` (l. 67) con `INS_RATE = 0.0100` fijo en el código (l. 30), deducido al trabajador como pide el plan. Conviene validar con un contador si corresponde como deducción obrera |
| Impuesto sobre la renta progresivo con créditos familiares | IMPLEMENTADO | `calcularImpuestoRenta` + `TramoImpuestoSalario` (hijos, cónyuge) |
| Aguinaldo | AUSENTE | Solo el indicador `Ausencia.computaParaAguinaldo` (`AusenciaServiceImpl` l. 57); no hay cálculo |
| Cesantía y preaviso | AUSENTE | `rg -i 'cesant\|preaviso' modules/nomina modules/rrhh/service` sin resultados |
| Préstamos y embargos | PARCIAL | Solo `solidarista` (% del bruto) y `pensionAlimentaria` (monto fijo), en `CalculosNominaService.calcular` paso 5. No hay préstamos ni embargos generales |
| Reporte para la CCSS (SICERE) | AUSENTE | No encontrado (`rg -i sicere src/main`) |
| Comprobantes de pago | AUSENTE | Ninguna colilla PDF ni email por empleado en `modules/nomina` |
| Integración con contabilidad | PARCIAL | `NominaService.contabilizar` → `AsientoContableService.generarAsientoNomina` (l. 656). Cuadra en el caso base, pero **falla cuando hay solidarista o pensión alimentaria** (ver "Integración contable transversal"). No hay asiento del pago de salarios ni del pago a la CCSS |

### Fase 3: Reportes financieros avanzados

| Entregable | Estado | Evidencia |
|---|---|---|
| Estado de Resultados | PARCIAL | `CalculoContableService.generarEstadoResultados` y `ReporteContableController` (`/contabilidad/reportes/estado-resultados`) existen desde el Sprint 5 (el último arreglo de permisos es `0b6bb94`, 2026-08-10). El reporte es **incompleto en datos**: las ventas, los cobros y el costo de ventas nunca se contabilizan (ver "Integración contable transversal") |
| Balance General | PARCIAL | `generarBalanceGeneral`, `/contabilidad/reportes/balance-general`. Mismo problema: faltan CxC, caja de cobros e inventario |
| Flujo de Caja | AUSENTE | `ContabilidadViewController` l. 179-185 pinta una tarjeta "Flujo de Caja" que enlaza a `/contabilidad/flujo-caja`, pero **ningún controller mapea esa ruta** (`rg '"/flujo-caja"' --glob '*Controller*'` sin resultados): el enlace devuelve 404 |
| Ratios financieros | AUSENTE | `rg -l -i 'liquidez\|endeudamiento\|razon corriente' src/main` sin resultados |
| Dashboard ejecutivo | PARCIAL | `reportes/controller/DashboardController` (`/dashboard`, vista `presentacion/dashboard/dashboard.html`) muestra KPIs operativos (clientes, productos, facturas del día, total pendiente), no financieros |
| Gráficos interactivos (Chart.js) | PARCIAL | Chart.js en `shared/layout.html`, `static/modules/reportes/js/reportes.js`, `reportes/ventas.html`, `static/modules/presentacion/js/dashboard.js`. Ninguno sobre estados financieros |
| Exportación a Excel/PDF | PARCIAL | `reportes/service/ExportService` exporta ventas, clientes y productos (PDF, Excel, CSV; iText y POI en `pom.xml` l. 107-138). No exporta estados financieros |
| Comparativos mensuales/anuales | PARCIAL | `ReporteController` `GET /reportes/api/comparativa-ingresos` (l. 797) compara los ingresos mensuales contra su propio promedio; no es un comparativo de periodos contables |
| Proyecciones financieras | AUSENTE | `rg -i 'proyecci' src/main/java` sin resultados |

### Fase 4: Testing

| Entregable | Estado | Evidencia |
|---|---|---|
| Tests de cálculo de nómina y validación de deducciones | IMPLEMENTADO | `nomina/service/CalculosNominaServiceTest` (8), `NominaServiceTest` (6), `NominaEntityTest` (12), `PermisoNominaTest` (7), `controller/NominaRestControllerTest` (4) |
| Tests de RRHH | IMPLEMENTADO | 11 clases en `src/test/.../modules/rrhh` (servicios y repositorios) |
| Test de integración contable de nómina | PARCIAL | `contabilidad/service/AsientoNominaIntegrationTest`: 3 de 4 tests fallan por un error del fixture (ver "Integración contable transversal") |
| Tests de reportes financieros | AUSENTE | `fd -i 'Calculo\|Reporte' src/test` sin resultados |
| Tests de integración con la CCSS | AUSENTE | No hay integración con la CCSS |
| Cobertura ≥ 75 % | AUSENTE | Sin JaCoCo |

### Fase 5: Documentación

| Entregable | Estado | Evidencia |
|---|---|---|
| Manual de RRHH, Manual de Nómina, Manual de Reportes Financieros | AUSENTE | `fd -i 'MANUAL_(RRHH\|NOMINA\|REPORTES)' docs` sin resultados |
| Guía de Normativa Laboral CR | PARCIAL | Existe una referencia de investigación, no un manual: `docs/sprints/SPRINT_8/fases/Cumplimiento normativo laboral y de seguridad social de Costa Rica para un módulo de RRHH en un ERP (2026).md` (`552f770`) |

**Conclusión del sprint:** RRHH y Nómina existen y son los módulos mejor testeados del proyecto. La nómina cubre el bruto a neto ordinario, pero no aguinaldo, cesantía, preaviso, horas extra, reporte CCSS ni comprobantes. Los reportes financieros son los del Sprint 5, sin flujo de caja ni ratios, y no reflejan ventas ni cobros.

---

## Sprint 9: UX/UI, PWA y Optimizaciones

### Fase 1: UX/UI

| Entregable | Estado | Evidencia |
|---|---|---|
| Tema oscuro, selector de tema (claro/oscuro/auto) y persistencia | AUSENTE | Único rastro: `static/shared/css/responsive.css` l. 201-212, un bloque `@media (prefers-color-scheme: dark)` **comentado** ("Modo oscuro (futuro)"). Sin `data-bs-theme` ni preferencia de tema en ninguna entidad |
| Tema claro mejorado / unificación visual | PARCIAL | Revisión de "look & feel" por módulo el 2026-08-03 (`c2ca58f`, `ae486e4`, `b707194`, `a920d2e`, `7252166`, `10cf46f`, `e4c00ec`), sin trazabilidad al plan del Sprint 9 |
| Animaciones y transiciones | PARCIAL | `transition` en 13 hojas CSS; no hay un sistema unificado |
| Accesibilidad WCAG 2.1 AA | PARCIAL | 165 atributos `aria-*` en las plantillas; sin modo alto contraste, sin ajuste de tamaño de fuente y sin auditoría axe |
| Responsive mejorado | IMPLEMENTADO | `static/shared/css/responsive.css`, `responsive-table.css`; vista de tarjetas móvil en producto (`25fb8e4`) |
| Breadcrumbs | IMPLEMENTADO | `shared/fragments/page-header.html`, `shared/components/navbar.html`; 74 plantillas los usan; `6d39da0` (2026-07-11) |
| Tooltips y ayudas contextuales | PARCIAL | 35 plantillas con `data-bs-toggle="tooltip"` o `title=` |
| Mensajes de feedback y loader unificados | PARCIAL | Spinner o loader en `shared/components/navbar.html` y `static/shared/css/common.css`; toasts en `shared/layout.html` |
| Tablas con paginación y filtros avanzados | PARCIAL | Paginación en 9 controllers; el fragmento `filter-bar` se usa en `facturacion/facturas.html` y `contabilidad/{cuentas,asientos}/listar.html`. El commit que introdujo los fragmentos (`6bd4352`) no es alcanzable desde HEAD |

### Fase 2: PWA

| Entregable | Estado | Evidencia |
|---|---|---|
| `manifest.json`, Service Worker, estrategias de caché, modo offline, cola de sincronización, iconos y splash, instalación, push, banner de instalación | AUSENTE | `fd -i 'manifest\|sw\.js\|service-worker\|offline' src/main/resources` sin resultados |

### Fase 3: Optimizaciones de rendimiento

| Entregable | Estado | Evidencia |
|---|---|---|
| Lazy loading de imágenes | AUSENTE | `rg 'loading="lazy"' templates` sin resultados |
| Code splitting, minificación y bundle reducido | AUSENTE | Sin pipeline de front-end (no hay `package.json` ni plugin de minificación en `pom.xml`) |
| Compresión Gzip/Brotli y caché HTTP | AUSENTE | Sin `server.compression` ni `spring.web.resources.cache` en `application.yml` |
| Optimización de imágenes (WebP), CDN, preload y prefetch | AUSENTE | No encontrado |
| Optimización de queries SQL | PARCIAL | Ver Sprint 7, Fase 2 (N+1). `@Cacheable` en 6 servicios (`ReporteServiceImpl`, `HaciendaApiServiceImpl`, `EmpresaServiceImpl`, etc.) |
| Índices de base de datos | PARCIAL | `@Index` declarado en 31 entidades, pero sin DDL versionado para la mayoría de tablas; los índices reales dependen de scripts manuales |
| Connection pooling optimizado | AUSENTE | Sin configuración `hikari` (se usa la de Spring Boot por defecto) |
| Lighthouse > 90 | AUSENTE | Sin medición |

### Fase 4: Testing y validación

| Entregable | Estado | Evidencia |
|---|---|---|
| axe-core, tests de tema oscuro, instalación PWA, Lighthouse CI, caché offline | AUSENTE | No hay tests de front-end ni directorio `.github/` |

### Fase 5: Documentación final

| Entregable | Estado | Evidencia |
|---|---|---|
| Manual UX/UI, Guía de Instalación PWA, Manual de Optimizaciones | AUSENTE | `fd -i 'MANUAL_UX\|GUIA_INSTALACION_PWA\|MANUAL_OPTIMIZ' docs` sin resultados |

**Conclusión del sprint:** el Sprint 9 no se inició como tal. Lo que existe (breadcrumbs, responsive, unificación visual) proviene de refactors de plantillas de julio y agosto de 2026, no del plan del sprint.

---

## Mapa de módulos

Método de conteo, por paquete bajo `modules/<mod>/`:

- Entidades: archivos con `^@Entity`.
- Servicios: archivos con `^@Service`, es decir, clases concretas; las interfaces no cuentan.
- Controllers: archivos con `^@(Rest)?Controller`.
- Vistas: archivos `.html` bajo `templates/modules/<mod>/`.
- Tests: clases bajo `src/test/java/.../modules/<mod>/`.

| Módulo | Sprint según docs | # Entidades | # Servicios | # Controllers | # Vistas | ¿Tests? |
|---|---|---|---|---|---|---|
| `cliente` | Sprints 1-4 (núcleo previo) | 1 | 2 | 1 | 2 | No |
| `configuracion` | Sprints 1-4 (núcleo previo) | 10 | 10 | 7 | 7 | No |
| `contabilidad` | Sprint 5 (Fase 2); Sprint 6 (Fase 3, `CuentaPorPagar`); Sprint 8 (Fase 3) | 5 | 5 | 6 | 14 | Sí: `AsientoNominaIntegrationTest` (4 tests, 3 fallan) |
| `facturacion` | Sprints 2-4; Sprint 5 (Pagos, FE); Sprint 6 (Fase 1, `TipoCambio`) | 8 | 14 | 9 | 12 | Sí: `FacturaRepositoryIT`, `XmlGeneratorServiceImplTest`, `TipoCambioServiceTest`, `TipoCambioDtoMappingTest` |
| `inventario` | Sprint 6 (Fase 2) | 4 | 5 | 4 | 3 | No |
| `nomina` | Sprint 8 (Fase 2) | 2 | 2 | 2 | 2 | Sí: `NominaEntityTest`, `PermisoNominaTest`, `NominaRestControllerTest`, `CalculosNominaServiceTest`, `NominaServiceTest` |
| `notificacion` | Sprints 1-4 (núcleo previo) | 3 | 3 | 3 | 2 | No |
| `presentacion` | Sin sprint documentado | 0 | 0 | 0 | 1 (`dashboard/dashboard.html`) | No. El paquete Java solo tiene subdirectorios vacíos (`controller`, `dto`, `model`, `repository`, `service`) que Git no versiona. El controller del dashboard vive en `reportes/controller/DashboardController` |
| `producto` | Sprints 1-4 (núcleo previo) | 2 | 3 | 3 | 4 | Sí: `ArticuloMaestroServiceImplTest` |
| `proveedores` | Sprint 6 (Fase 3) | 5 | 3 | 3 | 3 | No |
| `reportes` | Sprints 1-4; Sprint 8 (Fase 3) | 0 | 3 | 2 | 4 | No |
| `rrhh` | Sprint 8 (Fase 1) | 9 | 9 | 12 | 14 | Sí: 11 clases (`EmpleadoServiceTest`, `AusenciaServiceTest`, `ContratoEmpleadoServiceTest`, `DepartamentoServiceTest`, `ParametroCCSSServiceTest`, `PuestoServiceTest`, `SalarioMinimoServiceTest`, `SaldoVacacionesServiceTest`, `TramoImpuestoSalarioServiceTest`, `AusenciaRepositoryTest`, `AusenciaRepositoryBetweenTest`) |
| `seguridad` | Sprints 1-4; Sprint 7 (Fases 2 y 3) | 6 | 6 | 7 | 14 | Sí: 11 clases en `modules/seguridad` y `PermisoServiceTest` en el paquete `api.whatsordersmanager` |
| `whatsapp` | Sprints 1-4 (núcleo previo) | 4 | 8 | 7 | 4 | No |

Notas:

- `templates/modules/email/` (plantillas de correo) no corresponde a ningún paquete Java.
- La columna "Sprint según docs" se basa en los índices de los Sprints 5 a 9. Para los Sprints 1 a 4 la documentación menciona todos los módulos base sin asignarlos a un sprint concreto, por eso figuran como "núcleo previo".

---

## Integración contable transversal

Todos los automatismos viven en `modules/contabilidad/service/AsientoContableService.java`. Cada asiento comprueba `estaCuadrado()` antes de guardarse, o lo exige `AsientoContable.contabilizar()` (l. 257-262, vía `puedeContabilizarse()`).

| Origen | ¿Automático? | Clase/método generador → llamador | ¿Cuadra (debe = haber)? | Observaciones |
|---|---|---|---|---|
| Venta / factura | **No** | `generarAsientoVenta` (l. 359) **sin llamadores**; el endpoint `AsientoContableController.generarAsientoVenta` (l. 318) tiene la llamada comentada (l. 327) | Si se llamara: DR CxC `total`; CR Ventas `subtotal` + CR IVA `impuesto`. Cuadra solo si `total = subtotal + impuesto`; lo garantiza `contabilizar()` | Hallazgo del Sprint 5. Sigue abierto el bug `Long.getLong(factura.getIdFactura().toString())` en l. 364 (y en `AsientoContableMapper` l. 54), que devuelve `null` y anula la verificación de duplicados. Las facturas en USD/EUR se contabilizarían por el monto nominal, sin `tipoCambio` |
| Pago de cliente (cobro) | **No** | `generarAsientoPago` (l. 434) **sin llamadores**; el endpoint `AsientoContableController.generarAsientoPago` (l. 340) tiene la llamada comentada (l. 348) | Si se llamara: DR Caja `monto` / CR CxC `monto`. Cuadra | Hallazgo del Sprint 5. `PagoServiceImpl` no referencia `AsientoContableService` |
| Compra (CxP desde orden de compra) | Sí | `generarAsientoCompra` (l. 506) ← `CuentaPorPagarServiceImpl.crearDesdeOrdenCompra` l. 98 | Sí: DR `CPP_CUENTA_COMPRAS` `oc.subtotal` + DR `CPP_IVA_CREDITO_FISCAL` `oc.impuesto` = CR `CPP_CUENTA_POR_PAGAR` `oc.total`, porque `OrdenCompra.recalcularTotales()` (l. 112-116) fija `total = subtotal + impuesto` | (1) `CuentaPorPagarServiceImpl.crear` (CxP manual, l. 39) **no genera asiento**, y `generarAsientoCompra` devuelve `null` si no hay orden de compra (l. 507-510). (2) Los montos van en la moneda de la orden de compra **sin convertir a CRC**. (3) Se debita "Compras" (método periódico) mientras el inventario usa costo promedio perpetuo |
| Pago a proveedor | Sí | `generarAsientoPagoProveedor` (l. 584) ← `PagoProveedorServiceImpl.registrarPago` l. 69 | Sí: DR `CPP_CUENTA_POR_PAGAR` `monto` / CR `CPP_BANCO_<método>` `monto` | (1) `anularPago` (l. 78) **no reversa el asiento** (`// TODO: reversing entry on anularPago — out of scope`, l. 79): la CxP se restituye en el subledger, pero el mayor sigue mostrando el pago. (2) Un pago sobre una CxP manual debita una CxP que nunca se acreditó en el mayor. (3) Sin conversión de moneda |
| Nómina | Sí | `generarAsientoNomina` (l. 656) ← `NominaService.contabilizar` l. 275 | **Solo si no hay deducciones de solidarista ni de pensión alimentaria** (ver más abajo) | No hay asiento del pago de los salarios netos (DR Salarios por pagar / CR Banco) ni del pago de CCSS, INS o renta |
| Movimientos de inventario (venta, compra, ajuste) | **No** | Ningún archivo de `modules/inventario` referencia `AsientoContableService` | No aplica | No hay asiento de costo de ventas (DR Costo de ventas / CR Inventario) ni de ajustes (merma o sobrante). El valor del inventario y el costo de ventas no llegan al mayor |

### Causa concreta del fallo de `AsientoNominaIntegrationTest`

Tests afectados: `generarAsientoNomina_producesBalancedEntry`, `generarAsientoNomina_linksAsientoToNomina` y `generarAsientoNomina_asientoIsContabilizado`. Mensaje: `IllegalStateException: Asiento de nómina no cuadra — debe: 1268300.00, haber: 1536600.00`.

**Fixture** (`src/test/.../contabilidad/service/AsientoNominaIntegrationTest.buildNomina()` y su sobrescritura en cada test):

| Campo | Valor del fixture | Origen |
|---|---|---|
| `totalBruto` | 1 000 000.00 | `buildNomina()` |
| `totalCcssPatronal` | 268 300.00 (26.83 %) | `buildNomina()` |
| `detalle.ccssObrero` | 108 300.00 (10.83 %) | `buildNomina()` |
| `detalle.ins` | 10 000.00 (1 %) | `buildNomina()` |
| `detalle.impuestoRenta` | 0.00 | `buildNomina()` |
| `totalNeto` | **1 150 000.00** | `nomina.setTotalNeto(new BigDecimal("1150000.00")); // adjusted to make it balance` en los tres tests |

**Líneas que genera `AsientoContableService.generarAsientoNomina` con ese fixture:**

| Línea del código | Clave → cuenta del fixture | Debe | Haber |
|---|---|---|---|
| DR 1 "Sueldos brutos" (l. 676) | `NOMINA_GASTO_SUELDOS` → 5.1.01 | 1 000 000.00 | |
| DR 2 "Carga patronal CCSS" (l. 686) | `NOMINA_GASTO_SUELDOS` → 5.1.01 | 268 300.00 | |
| CR 1 "CCSS obrero por pagar" | `NOMINA_CCSS_POR_PAGAR` → 2.1.01 | | 108 300.00 |
| CR 2 "INS por pagar" | `NOMINA_INS_POR_PAGAR` → 2.1.02 | | 10 000.00 |
| CR 3 "Renta por pagar" | se omite (renta = 0) | | — |
| CR 4 "Salarios netos por pagar" | `NOMINA_SALARIOS_POR_PAGAR` → 2.1.04 | | 1 150 000.00 |
| CR 5 "CCSS patronal por pagar" (l. 745) | `NOMINA_CCSS_PATRONAL_POR_PAGAR` → 5.1.02 | | 268 300.00 |
| **Totales** | | **1 268 300.00** | **1 536 600.00** |

Diferencia: 1 536 600.00 − 1 268 300.00 = **268 300.00 = `totalCcssPatronal`**.

**Error conceptual: la carga patronal se acredita dos veces, y el error está en el fixture, no en el servicio.**

1. El neto real es `bruto − deducciones obreras` = 1 000 000.00 − 108 300.00 − 10 000.00 − 0.00 = **881 700.00**. Es la misma fórmula de `CalculosNominaService.calcular`, paso 7 (`salarioNeto = brutoProrrateado − totalDeducciones`), y la carga patronal no entra en ella (paso 8: "employer cost, not deducted from employee").
2. El fixture fija `totalNeto = 1 150 000.00 = 881 700.00 + 268 300.00`. Es decir, mete la carga patronal **dentro** del neto. El comentario del test lo deja explícito: `1_000_000 + 268_300 = 108_300 + 10_000 + 0 + 1_150_000`.
3. Ese ajuste cuadraba con la versión anterior del asiento, que el mensaje del commit `90474c3` (2026-07-23) describe así: la carga patronal "was debited … but never credited". Sin un CR 5, la única forma de cuadrar era inflar el neto.
4. El commit `90474c3` añadió el CR 5 (CCSS patronal por pagar = 268 300.00). Es correcto en partida doble (DR gasto patronal / CR pasivo patronal), pero desde entonces los 268 300.00 se acreditan **dos veces**: una dentro del neto inflado del fixture (CR 4) y otra en el CR 5. El test nunca se actualizó (su único commit es `408d6de`).
5. Comprobación con el neto correcto (881 700.00): Debe = 1 000 000.00 + 268 300.00 = 1 268 300.00; Haber = 108 300.00 + 10 000.00 + 881 700.00 + 268 300.00 = 1 268 300.00. **Cuadra.**
6. Defectos menores del fixture: el `totalNeto` por defecto de `buildNomina()` (882 000.00) tampoco es el neto correcto (difiere en 300.00), y la clave `NOMINA_CCSS_PATRONAL_POR_PAGAR` se mapea a una cuenta con código de gasto ("5.1.02") en lugar de pasivo. El Javadoc de `generarAsientoNomina` (l. 643-651) está desactualizado: dice "DR NOMINA_CCSS_PATRONAL_POR_PAGAR" y no menciona el CR 5.

**Defecto real de producción, distinto del anterior y no cubierto por el test.** `CalculosNominaService.calcular` resta del neto `solidarista` y `pensionAlimentaria` (pasos 5 y 6), pero `generarAsientoNomina` no las acredita en ninguna cuenta, y `V16__seed_nomina_parametros_contables.sql` no define cuentas para ellas. Por ejemplo, con un solidarista del 3 % sobre 1 000 000.00:

- Neto = 1 000 000.00 − 108 300.00 − 10 000.00 − 30 000.00 = 851 700.00.
- Debe = 1 268 300.00.
- Haber = 108 300.00 + 10 000.00 + 851 700.00 + 268 300.00 = 1 238 300.00.
- Diferencia: 30 000.00.

`NominaService.contabilizar` lanzará `IllegalStateException` para **cualquier planilla** con al menos un empleado que tenga solidarista o pensión alimentaria.

---

## Estado de la suite

Se ejecutó `./mvnw test --no-transfer-progress` el 2026-10-10 sobre el commit auditado:

```
Tests run: 211, Failures: 5, Errors: 6, Skipped: 0
BUILD FAILURE
```

Hay 240 métodos `@Test`, pero solo se ejecutaron 211. Los 29 restantes son de las 5 clases `*IT`, que Surefire no ejecuta porque no hay `maven-failsafe-plugin` (ver T-001). El resultado es idéntico al de T-001 porque se auditó el mismo commit.

| Test | Módulo | Tipo | Causa raíz |
|---|---|---|---|
| `WhatsOrdersManagerApplicationTests.contextLoads` (E) | raíz | Entorno | Flyway no conecta a MySQL: `Access denied for user 'root'` |
| `AsientoNominaIntegrationTest.generarAsientoNomina_producesBalancedEntry` (E) | contabilidad / nómina | Defecto de código (fixture del test) | `totalNeto=1150000` incluye la CCSS patronal (268 300), que desde `90474c3` también se acredita aparte. Resultado: debe 1 268 300 frente a haber 1 536 600. Ver "Integración contable transversal" |
| `AsientoNominaIntegrationTest.generarAsientoNomina_linksAsientoToNomina` (E) | contabilidad / nómina | Defecto de código (fixture) | La misma causa |
| `AsientoNominaIntegrationTest.generarAsientoNomina_asientoIsContabilizado` (E) | contabilidad / nómina | Defecto de código (fixture) | La misma causa |
| `LoginFailureHandlerTest.onAuthenticationFailure_userExists_incrementsFailedAttempts` (F) | seguridad | Defecto de código (test desactualizado) | El test simula `usuarioRepository.findByTelefono(...)`, pero `LoginFailureHandler` l. 58-59 ahora busca con `findByEmail`/`findByNombre`; nunca encuentra al usuario |
| `LoginFailureHandlerTest.onAuthenticationFailure_sixthAttempt_blocksUser` (F) | seguridad | Defecto de código (test desactualizado) | La misma causa (`findByTelefono` frente a `findByEmail`/`findByNombre`) |
| `LoginFailureHandlerTest.onAuthenticationFailure_beforeSixthAttempt_doesNotBlock` (F) | seguridad | Defecto de código (test desactualizado) | La misma causa |
| `LoginFailureHandlerTest.onAuthenticationFailure_userNotFound_doesNothing` (E) | seguridad | Defecto de código (test desactualizado) | `UnnecessaryStubbingException`: el stub de `findByTelefono` no se usa |
| `LoginSuccessHandlerTest.onAuthenticationSuccess_userExists_resetsFailedAttempts` (F) | seguridad | Defecto de código (test desactualizado) | El test simula `findByTelefono`, pero `LoginSuccessHandler` l. 41 usa `findByEmail` |
| `LoginSuccessHandlerTest.onAuthenticationSuccess_userNotFound_doesNothing` (E) | seguridad | Defecto de código (test desactualizado) | `UnnecessaryStubbingException` del stub `findByTelefono` |
| `UserDetailsServiceImplTest.loadUserByUsername_bloqueado_throwsLockedException` (F) | seguridad | Defecto de código (contrato distinto) | `UserDetailsServiceImpl` (`seguridad/service/impl`, l. 42-51) no lanza `LockedException`: devuelve `UserDetails` con `accountLocked(true)`, y el bloqueo lo aplica después el `AccountStatusUserDetailsChecker` de Spring. El test espera la excepción en `loadUserByUsername` |

En resumen: 1 fallo es de entorno y 10 son defectos de código. Ninguno de los 10 es un fallo funcional confirmado: son tests desactualizados respecto del login por email/nombre y del contrato de bloqueo, más el fixture de nómina, que quedó inflado cuando se corrigió el servicio.

---

## Cronología

Consultas: `git log --format='%h %ad %s' --date=short -- <rutas>` para el último commit y `git log --reverse ... | head -1` para el primero. Solo se consideran commits alcanzables desde HEAD (`e0a1481`, 2026-08-26).

| Ámbito (rutas) | # commits | Primer commit | Último commit |
|---|---|---|---|
| Multi-divisa (`facturacion/model/TipoCambio.java`, `service/TipoCambioService.java`, `controller/TipoCambioRestController.java`, `enums/FuenteTipoCambio.java`) | 1 | `c1e2e0d` 2026-07-14 feat(facturacion): soporte multi-divisa CRC/USD/EUR con tipo de cambio | `c1e2e0d` 2026-07-14 (mismo) |
| Inventario (`modules/inventario`, `templates/modules/inventario`, `static/modules/inventario`) | 5 | `f3e1f11` 2026-07-14 feat(inventario): implementar módulo de inventario Sprint 6 Fase 2 | `b707194` 2026-08-03 fix(inventario): apply look & feel review across the module |
| Proveedores (`modules/proveedores`, plantillas, estáticos) | 6 | `fb86126` 2026-07-14 feat(proveedores): implementar módulo de proveedores Sprint 6 Fase 3 | `a920d2e` 2026-08-03 fix(proveedores): apply look & feel review across the module |
| Cuentas por pagar (`contabilidad/model/CuentaPorPagar.java`, `CuentaPorPagarServiceImpl.java`, `cuentas-pagar.html`) | 4 | `b4ba9d8` 2026-07-15 refactor(contabilidad): migrate CuentaPorPagar to contabilidad module | `c2ca58f` 2026-08-03 fix(contabilidad): apply look & feel review across the module |
| Producción | 0 | No existe | No existe |
| Seguridad (`modules/seguridad`, plantillas, estáticos) | 53 | `f308763` 2025-12-27 feat: Crear estructura base para refactorización modular | `54a957e` 2026-08-10 fix(auth): redirect unauthenticated root path to login. Trabajo del Sprint 7: `9ee693e` 2026-07-17 |
| Migraciones Flyway (`db/migration`) | 8 | `9ee693e` 2026-07-17 (Sprint 7) | `85d2a79` 2026-08-06 fix(configuracion): dedupe cat_canton_cr/cat_distrito_cr |
| RRHH (`modules/rrhh`, plantillas, estáticos) | 4 | `1f5ea25` 2026-07-22 feat(rrhh): add /perfil self-service hub | `7252166` 2026-08-03 fix(rrhh): apply look & feel review across the module, including Nómina |
| Nómina (`modules/nomina`, plantillas, estáticos) | 2 | `408d6de` 2026-07-23 feat(nomina): add payroll module fase 2 | `7252166` 2026-08-03 (mismo que RRHH) |
| Reportes contables (`CalculoContableService.java`, `ReporteContableController.java`, `templates/modules/contabilidad/reportes`) | 3 | `1fc58af` 2026-01-29 feat: Add report generation for daily cash report | `0b6bb94` 2026-08-10 fix(contabilidad): fix @PreAuthorize NPE and always-denied report endpoints |
| Reportes (`modules/reportes`, plantillas, estáticos) | 28 | `f308763` 2025-12-27 | `f547188` 2026-08-26 feat(reportes): add Presentación column to productos export |
| UI compartida, base del Sprint 9 (`static/shared`, `templates/shared`) | 31 | `a6bd284` 2025-12-28 refactor(resources): Reorganizar archivos CSS en estructura modular | `10cf46f` 2026-08-03 fix(facturacion): apply look & feel review across the module |
| Presentación (dashboard) | 8 | `f308763` 2025-12-27 | `da7339b` 2026-08-03 fix(dashboard): fix module-grid tiles showing another module's color |

Lectura:

- El trabajo de los Sprints 6 a 8 se hizo en una ráfaga de 10 días, del **2026-07-14 al 2026-07-23**: multi-divisa, inventario y proveedores el 07-14; contabilidad de compras y pagos el 07-15; seguridad del Sprint 7 el 07-17; RRHH el 07-22; nómina y su integración contable el 07-23.
- Después solo hubo revisiones de "look & feel" (2026-08-03) y arreglos puntuales (2026-08-06, 08-10 y 08-26).
- **Último trabajo real en el código alcanzable desde HEAD: 2026-08-26** (`f547188`, reporte de productos). Último trabajo sobre un módulo de los Sprints 6 a 8: 2026-08-03.
- Fuera de HEAD hay 148 commits sin merge que tocan `src/`, alcanzables solo desde otras referencias (`git log --all --not HEAD --no-merges -- src`). Abarcan del 2025-10-13 al 2026-07-17 (p. ej., `6bd4352`, fragmentos `filter-bar`), más `f98ded2` (2026-10-10, rama `chore/memoria-proyectos-setup`, configuración e iconos). No cambian las conclusiones de esta auditoría, pero conviene revisar si quedó trabajo de UI sin integrar.

---

## Conclusión

### Porcentaje real estimado

Ponderación por número de tareas del plan (índices). Las fases se estiman según la proporción de entregables IMPLEMENTADO (1), PARCIAL (0.5) y AUSENTE (0), ajustada por la gravedad de los defectos.

| Sprint / Fase | Tareas del plan | Índice | **Estimación** | Comentario |
|---|---|---|---|---|
| S6 F1 Multi-divisa | 42 | 0 % | **~45 %** | `TipoCambio` y factura con TC manual; sin API de tasas, sin `Moneda` configurable y sin conversión en compras, CxP ni asientos |
| S6 F2 Inventario | 48 | 0 % | **~70 %** | Kardex, ajustes, lotes y FEFO conectados a ventas y compras; sin rotación ni alertas reales; defectos de lotes y de reversión |
| S6 F3 Proveedores | 44 | 0 % | **~70 %** | Proveedor, OC, recepción, CxP y pagos con asientos; sin evaluación, historial ni conciliación; sin reversión contable |
| S6 F4 Testing | 8 | 0 % | **~10 %** | Solo tests de `TipoCambio` |
| S6 F5 Documentación | 4 | 0 % | **0 %** | |
| **Sprint 6** | 146 | 0 % | **~58 %** | |
| S7 F1 Producción (opcional) | 52 | 0 % | **0 %** | |
| S7 F2 Mejoras técnicas | 38 | 0 % | **~60 %** | Sin Redis ni `@ExceptionHandler` global |
| S7 F3 Seguridad | 28 | 0 % | **~60 %** | Sin 2FA ni JWT; política de contraseñas mínima |
| S7 F4 Testing | 6 | 0 % | **~50 %** | Tests de seguridad existen, varios fallan y los `*IT` no se ejecutan |
| S7 F5 Documentación | 4 | 0 % | **0 %** | |
| **Sprint 7** | 128 (76 sin Producción) | 0 % | **~33 % (~56 % sin Producción)** | |
| S8 F1 RRHH | 48 | 0 % | **~60 %** | Sin asistencia, evaluaciones ni expediente |
| S8 F2 Nómina | 56 | 0 % | **~45 %** | Ordinario, CCSS, INS y renta; sin aguinaldo, cesantía, preaviso, extras, SICERE ni comprobantes; asiento defectuoso con solidarista o pensión |
| S8 F3 Reportes financieros | 32 | 0 % | **~35 %** | P&L y balance del Sprint 5 sin ventas ni cobros; sin flujo de caja (enlace a 404), ratios ni proyecciones |
| S8 F4 Testing | 8 | 0 % | **~60 %** | 16 clases de nómina y RRHH; test contable roto |
| S8 F5 Documentación | 4 | 0 % | **~10 %** | Solo la referencia normativa |
| **Sprint 8** | 148 | 0 % | **~48 %** | |
| S9 F1 UX/UI | 36 | 0 % | **~25 %** | Breadcrumbs y responsive; sin tema oscuro |
| S9 F2 PWA | 32 | 0 % | **0 %** | |
| S9 F3 Optimizaciones | 28 | 0 % | **~10 %** | |
| S9 F4 Testing | 5 | 0 % | **0 %** | |
| S9 F5 Documentación | 3 | 0 % | **0 %** | |
| **Sprint 9** | 104 | 0 % | **~11 %** | |

### Porcentaje por módulo (respecto a su alcance planificado)

| Módulo | Estimación | Principal carencia |
|---|---|---|
| `inventario` | ~70 % | Asientos de costo de ventas y ajustes; rotación; defectos de lotes y reversión |
| `proveedores` (+ CxP en `contabilidad`) | ~70 % | Reversión contable, asiento de CxP manual, conciliación, evaluación |
| Multi-divisa (`facturacion`) | ~45 % | API de tasas; conversión en compras, CxP y asientos |
| Producción | 0 % | Todo |
| `seguridad` (alcance S7) | ~60 % | 2FA, JWT, política de contraseñas, tests en rojo |
| `rrhh` | ~60 % | Asistencia, evaluaciones, expediente |
| `nomina` | ~45 % | Aguinaldo, cesantía, preaviso, extras, SICERE, comprobantes, asiento con solidarista o pensión |
| Reportes financieros (`contabilidad` + `reportes`) | ~35 % | Flujo de caja, ratios, exportación financiera; datos incompletos por falta de asientos de venta y cobro |
| UX/UI y PWA (`shared`) | ~15 % | Tema oscuro, PWA, optimizaciones de entrega |

### Defectos que bloquean la exactitud contable o la operación

**P1 (bloquean la exactitud contable o el arranque):**

1. **Ventas y cobros no se contabilizan** (heredado del Sprint 5). `generarAsientoVenta` (l. 359) y `generarAsientoPago` (l. 434) no tienen llamadores, y el bug `Long.getLong` (l. 364; `AsientoContableMapper` l. 54) sigue abierto. Consecuencia: el Estado de Resultados y el Balance General del Sprint 8 omiten ingresos, CxC y caja de cobros.
2. **Nómina con solidarista o pensión alimentaria no se puede contabilizar.** `generarAsientoNomina` no acredita esas deducciones y no existen cuentas en `V16__seed_nomina_parametros_contables.sql`. `NominaService.contabilizar` lanza `IllegalStateException` (diferencia = Σ solidarista + Σ pensión).
3. **Sin DDL versionado para las tablas de los Sprints 6 y 7 con `ddl-auto: validate`:** `movimientos_inventario`, `lotes_productos`, `ajustes_inventario`, `detalles_ajuste_inventario`, `proveedores`, `proveedor_item`, `ordenes_compra`, `detalle_ordenes_compra`, `pagos_proveedor`, `cuentas_por_pagar` y `tipos_cambio`. Un entorno nuevo no arranca con Flyway solo. RRHH y Nómina sí tienen migraciones (V3-V16).
4. **Asientos multi-moneda sin conversión.** `generarAsientoCompra` y `generarAsientoPagoProveedor` usan los montos nominales de la OC o del pago aunque `OrdenCompra.moneda` o `CuentaPorPagar.moneda` sean USD/EUR. El mayor mezcla colones con dólares.

**P2 (descuadres entre subledger y mayor, o errores de cantidades):**

5. `PagoProveedorServiceImpl.anularPago` (l. 78) no reversa el asiento (TODO l. 79). El mayor de CxP y Bancos diverge del subledger.
6. `CuentaPorPagarServiceImpl.crear` (CxP manual) no genera asiento, y `generarAsientoCompra` la omite (l. 507-510). Los pagos posteriores debitan una CxP nunca acreditada.
7. Lotes con cantidad duplicada al recibir una OC: `OrdenCompraServiceImpl.recibirParcial` l. 142 (`crearLote` con `aRecibir`) + `MovimientoInventarioServiceImpl.registrarEntrada` (suma `aRecibir` otra vez al lote).
8. Eliminar una factura no devuelve el stock: `FacturaServiceImpl.eliminarConValidacion` (l. 311) y `deleteById` no registran `ENTRADA_DEVOLUCION`.
9. Los movimientos de inventario no generan asiento: falta el costo de ventas y el ajuste de inventario. Las compras se debitan a "Compras" (periódico) mientras el kardex es perpetuo con costo promedio, así que el valor del inventario no está en el balance.
10. `AsientoNominaIntegrationTest`: el fixture usa `totalNeto = 1 150 000.00` en lugar de 881 700.00 (la carga patronal se acredita dos veces). Mantiene la suite en rojo. Es un error del test, no del servicio.
11. La tarjeta "Flujo de Caja" (`ContabilidadViewController` l. 179-185) enlaza a `/contabilidad/flujo-caja`, que no tiene mapping (404).

**P3 (funcionalidad incompleta sin impacto contable directo):**

12. No hay actualización automática del tipo de cambio (`FuenteTipoCambio.BCCR` y `API_EXTERNA` sin uso).
13. `AlertaInventarioService.verificarAlertas` solo escribe en el log; no notifica.
14. `PasswordUtil.esPasswordValida` sin llamadores; sin 2FA ni JWT; sin `@ExceptionHandler` global.
15. Nómina sin aguinaldo, cesantía, preaviso, horas extra o nocturnas, reporte CCSS ni comprobantes; `INS_RATE` fijo en el código (a validar con un contador).
16. Sin tests de `inventario` ni `proveedores`; sin JaCoCo; los `*IT` no se ejecutan.
17. Sprint 9 sin iniciar: el tema oscuro está comentado en `responsive.css` y no hay PWA, compresión ni caché HTTP.
18. Javadoc de `generarAsientoNomina` desactualizado. Los índices de los Sprints 6 a 9 declaran "0/N" con código implementado; faltan `FASE_4_TESTING.md` y `FASE_5_DOCUMENTACION.md` del Sprint 6.
