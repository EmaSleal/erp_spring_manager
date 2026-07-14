# 📋 CLASIFICACIÓN DE SPRINTS FUTUROS (6-9)

**Proyecto:** WhatsApp Orders Manager - ERP Spring Boot  
**Fecha:** 16 de enero de 2026  
**Base:** Análisis de código actual + MEJORAS_FUTURAS.md + Referencias  
**Objetivo:** Evitar duplicidad y maximizar sinergia entre módulos

---

## 🎯 SPRINT 5: CONTABILIDAD + FACTURACIÓN ELECTRÓNICA CR + PAGOS

**Estado:** 📋 Propuesta aprobada pendiente  
**Duración:** 24-32 días  
**Prioridad:** ⭐⭐⭐ CRÍTICA  

### Módulos Incluidos
1. ✅ Módulo de Pagos (45 tareas)
2. ✅ Contabilidad de Doble Partida (58 tareas)
3. ✅ Facturación Electrónica CR v4.4 (52 tareas)
4. ✅ Testing Automatizado (32 tareas)
5. ✅ Documentación (12 tareas)

**Total:** 199 tareas | **Documento:** `PROPUESTA_SPRINT_5.md`

---

## 🚀 SPRINT 6: MULTI-DIVISA + INVENTARIO + PROVEEDORES

**Duración estimada:** 20-26 días  
**Prioridad:** ⭐⭐⭐ ALTA  
**Dependencias:** Sprint 5 (Pagos y Contabilidad)

### 🎯 Objetivos Estratégicos
Implementar sistema multi-divisa para internacionalización + control de inventario completo + módulo de proveedores (cuentas por pagar).

---

### FASE 6.1: Sistema Multi-Divisa (NUEVA)

**Duración:** 5-7 días | **Tareas:** 42 tareas  
**Prioridad:** ⭐⭐⭐ CRÍTICA

#### ¿Por qué ahora?
- ✅ Pagos ya implementados (Sprint 5)
- ✅ Contabilidad preparada para multi-divisa
- ✅ Base para facturación internacional
- ⚠️ **DETECTADO:** Ya existe `formatearMoneda()` en código actual

#### Tareas Principales (42)

**Base de Datos (8 tareas)**
- [ ] Crear tabla `divisa` (código, nombre, símbolo, es_maestra, decimales)
- [ ] Crear tabla `tipo_cambio` (origen, destino, fecha, tasa)
- [ ] Alterar `factura` (id_divisa, tipo_cambio_registrado, total_divisa_maestra)
- [ ] Alterar `pago` (moneda, tipo_cambio)
- [ ] Trigger: Auto-convertir a divisa maestra
- [ ] Función SQL: Generar conversión histórica
- [ ] Datos iniciales: CRC, USD, EUR (Costa Rica)
- [ ] Script de rollback completo

**Backend (12 tareas)**
- [ ] Entidad `Divisa.java`
- [ ] Entidad `TipoCambio.java`
- [ ] `DivisaRepository` + queries
- [ ] `TipoCambioRepository` + queries
- [ ] `DivisaService` + Impl (CRUD, validar única maestra)
- [ ] `TipoCambioService` + Impl (registro, consulta histórica)
- [ ] `ConversionService` (convertir entre divisas)
- [ ] Actualizar `FacturaService` (manejo multi-divisa)
- [ ] Actualizar `PagoService` (conversión automática)
- [ ] Actualizar `ContabilidadService` (asientos en divisa maestra)
- [ ] `DivisaController` (vistas)
- [ ] `DivisaRestController` (API)

**Frontend (8 tareas)**
- [ ] Vista: `configuracion/divisas.html` (CRUD divisas)
- [ ] Vista: `configuracion/tipos-cambio.html` (registro TC)
- [ ] Actualizar: `facturacion/form.html` (selector divisa)
- [ ] Actualizar: `facturacion/detalle.html` (mostrar conversión)
- [ ] Actualizar: `pagos/registrar.html` (conversión automática)
- [ ] JavaScript: `divisas.js` (CRUD + validaciones)
- [ ] JavaScript: `tipos-cambio.js` (registro + histórico)
- [ ] Actualizar: `reportes/` (consolidación en divisa maestra)

**Integraciones (6 tareas)**
- [ ] Integrar con API externa de tipos de cambio (opcional)
- [ ] Actualizar reportes (conversión automática)
- [ ] Actualizar exportaciones PDF/Excel (multi-divisa)
- [ ] Notificación: Variación significativa en TC (>5%)
- [ ] Migración de facturas existentes a CRC
- [ ] Scheduler: Actualizar TC diarios (si API externa)

**Testing (6 tareas)**
- [ ] `DivisaServiceTest` - CRUD y validaciones
- [ ] `TipoCambioServiceTest` - Registro y consultas
- [ ] `ConversionServiceTest` - Conversiones correctas
- [ ] Test: Factura en USD → Conversión a CRC
- [ ] Test: Pago en USD → Asiento en CRC
- [ ] Test: Reporte consolidado en divisa maestra

**Documentación (2 tareas)**
- [ ] `SPRINT_6_FASE_1_MULTI_DIVISA.md`
- [ ] `MANUAL_GESTION_DIVISAS.md`

---

### FASE 6.2: Inventario Avanzado (AMPLIAR EXISTENTE)

**Duración:** 7-9 días | **Tareas:** 48 tareas  
**Prioridad:** ⭐⭐⭐ ALTA

#### Estado Actual Detectado
- ⚠️ **Producto** solo tiene campos básicos
- ⚠️ **NO** tiene control de stock
- ⚠️ **NO** tiene almacenes
- ⚠️ Enum `Permiso.PRODUCTO_AJUSTAR_INVENTARIO` existe pero no se usa
- ✅ Reportes ya tienen filtro `stockBajo` (comentado como TODO)

#### Tareas Principales (48)

**Base de Datos (10 tareas)**
- [ ] Alterar `producto` (agregar: stock_actual, stock_minimo, stock_maximo, unidad_medida)
- [ ] Crear tabla `almacen` (código, nombre, dirección, activo)
- [ ] Crear tabla `stock_por_almacen` (id_producto, id_almacen, cantidad, ubicacion)
- [ ] Crear tabla `movimiento_inventario` (tipo, id_producto, id_almacen, cantidad, referencia)
- [ ] Crear tabla `ajuste_inventario` (motivo, observaciones, usuario, fecha)
- [ ] Crear tabla `transferencia_almacen` (origen, destino, productos, estado)
- [ ] Trigger: Actualizar stock al crear factura
- [ ] Trigger: Revertir stock al anular factura
- [ ] SP: `sp_kardex_producto(id_producto, fecha_inicio, fecha_fin)`
- [ ] SP: `sp_productos_bajo_stock()`

**Backend (14 tareas)**
- [ ] Actualizar entidad `Producto.java` (agregar campos de stock)
- [ ] Crear entidad `Almacen.java`
- [ ] Crear entidad `StockPorAlmacen.java`
- [ ] Crear entidad `MovimientoInventario.java`
- [ ] Crear enum `TipoMovimiento` (ENTRADA, SALIDA, AJUSTE, TRANSFERENCIA)
- [ ] `AlmacenRepository` + queries
- [ ] `MovimientoInventarioRepository` + queries
- [ ] `InventarioService` + Impl (ajustes, transferencias, kardex)
- [ ] `AlmacenService` + Impl (CRUD almacenes)
- [ ] Actualizar `ProductoService` (validar stock antes de facturar)
- [ ] Actualizar `FacturaService` (descontar stock automáticamente)
- [ ] `InventarioController` (vistas)
- [ ] `InventarioRestController` (API)
- [ ] Integración con `NotificacionService` (alertas stock bajo)

**Frontend (10 tareas)**
- [ ] Vista: `inventario/almacenes.html` (CRUD almacenes)
- [ ] Vista: `inventario/stock-por-almacen.html` (consulta stock)
- [ ] Vista: `inventario/ajustes.html` (ajustes manuales)
- [ ] Vista: `inventario/transferencias.html` (entre almacenes)
- [ ] Vista: `inventario/kardex.html` (movimientos por producto)
- [ ] Vista: `inventario/alertas.html` (productos bajo stock)
- [ ] Actualizar: `productos/form.html` (gestión de stock)
- [ ] JavaScript: `inventario.js` (ajustes y transferencias)
- [ ] JavaScript: `alertas-stock.js` (notificaciones en tiempo real)
- [ ] Dashboard: Widget de stock bajo

**Integraciones (6 tareas)**
- [ ] Actualizar reportes: Productos bajo stock
- [ ] Actualizar reportes: Valorización de inventario
- [ ] Exportar kardex a Excel
- [ ] Código de barras/QR para productos
- [ ] Lectura de código de barras (opcional)
- [ ] Integración con balanza/báscula (opcional)

**Testing (6 tareas)**
- [ ] `InventarioServiceTest` - Ajustes y kardex
- [ ] Test: Descontar stock al facturar
- [ ] Test: Revertir stock al anular factura
- [ ] Test: Transferencia entre almacenes
- [ ] Test: Validar stock insuficiente
- [ ] Test: Cálculo de stock bajo

**Documentación (2 tareas)**
- [ ] `SPRINT_6_FASE_2_INVENTARIO.md`
- [ ] `MANUAL_GESTION_INVENTARIO.md`

---

### FASE 6.3: Módulo de Proveedores (NUEVA)

**Duración:** 6-8 días | **Tareas:** 44 tareas  
**Prioridad:** ⭐⭐⭐ ALTA

#### ¿Por qué ahora?
- ✅ Complementa módulo de Pagos (Sprint 5)
- ✅ Completa ciclo: Compras → Pagos → Contabilidad
- ✅ Base para órdenes de compra
- ⚠️ **DETECTADO:** NO existe modelo Proveedor actual

#### Tareas Principales (44)

**Base de Datos (8 tareas)**
- [ ] Crear tabla `proveedor` (similar a cliente)
- [ ] Crear tabla `compra` (similar a factura)
- [ ] Crear tabla `linea_compra` (detalle de compras)
- [ ] Crear tabla `pago_proveedor` (cuentas por pagar)
- [ ] Crear tabla `estado_cuenta_proveedor`
- [ ] Vista: `v_deuda_proveedores`
- [ ] SP: `sp_estado_cuenta_proveedor(id_proveedor)`
- [ ] Script de rollback

**Backend (12 tareas)**
- [ ] Entidad `Proveedor.java`
- [ ] Entidad `Compra.java`
- [ ] Entidad `LineaCompra.java`
- [ ] Entidad `PagoProveedor.java`
- [ ] `ProveedorRepository` + queries
- [ ] `CompraRepository` + queries
- [ ] `PagoProveedorRepository` + queries
- [ ] `ProveedorService` + Impl
- [ ] `CompraService` + Impl (registro, afectar inventario)
- [ ] `PagoProveedorService` + Impl
- [ ] `ProveedorController` (vistas)
- [ ] `ProveedorRestController` (API)

**Frontend (10 tareas)**
- [ ] Vista: `proveedores/lista.html`
- [ ] Vista: `proveedores/form.html` (CRUD)
- [ ] Vista: `proveedores/detalle.html`
- [ ] Vista: `compras/lista.html`
- [ ] Vista: `compras/form.html` (registro de compras)
- [ ] Vista: `compras/detalle.html`
- [ ] Vista: `proveedores/pagos.html` (pagos a proveedores)
- [ ] Vista: `proveedores/estado-cuenta.html`
- [ ] JavaScript: `proveedores.js`
- [ ] JavaScript: `compras.js`

**Integraciones (8 tareas)**
- [ ] Integrar con Inventario (entrada de stock por compra)
- [ ] Integrar con Contabilidad (asientos de compras)
- [ ] Integrar con Pagos (pagos a proveedores)
- [ ] Reportes: Compras por proveedor
- [ ] Reportes: Cuentas por pagar
- [ ] Exportar estado de cuenta a PDF
- [ ] Notificaciones: Pagos próximos a vencer
- [ ] Dashboard: Saldo proveedores

**Testing (4 tareas)**
- [ ] `ProveedorServiceTest`
- [ ] `CompraServiceTest` - Registro y stock
- [ ] Test: Asiento contable de compra
- [ ] Test: Pago a proveedor

**Documentación (2 tareas)**
- [ ] `SPRINT_6_FASE_3_PROVEEDORES.md`
- [ ] `MANUAL_GESTION_PROVEEDORES.md`

---

### 📊 Resumen Sprint 6

```
┌─────────────────────────────────────────────────────────────┐
│                    SPRINT 6 - RESUMEN                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  FASE 6.1: Multi-Divisa           [42 tareas]  5-7 días  ⭐⭐⭐│
│  FASE 6.2: Inventario             [48 tareas]  7-9 días  ⭐⭐⭐│
│  FASE 6.3: Proveedores            [44 tareas]  6-8 días  ⭐⭐⭐│
│  Testing + Documentación          [12 tareas]  2-3 días  ⭐⭐ │
│                                                              │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│  TOTAL SPRINT 6                   [146 tareas] 20-27 días   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

**Valor de Negocio Sprint 6:**
- ✅ Sistema internacional (multi-divisa)
- ✅ Control total de inventario
- ✅ Gestión de proveedores y compras
- ✅ Ciclo completo: Compras → Inventario → Ventas → Contabilidad

---

## 🔧 SPRINT 7: PRODUCCIÓN + MEJORAS SISTEMA + SEGURIDAD

**Duración estimada:** 18-24 días  
**Prioridad:** ⭐⭐ MEDIA-ALTA  
**Dependencias:** Sprint 6 (Inventario)

### 🎯 Objetivos Estratégicos
Módulo de producción básico + mejoras técnicas del sistema + seguridad avanzada.

---

### FASE 7.1: Módulo de Producción (NUEVA)

**Duración:** 8-10 días | **Tareas:** 52 tareas  
**Prioridad:** ⭐⭐ MEDIA

#### Aplicabilidad
- ⚠️ **Solo para empresas manufactureras**
- ✅ Puede omitirse si solo es comercio/servicios

#### Tareas Principales (52)

**Base de Datos (10 tareas)**
- [ ] Crear tabla `producto_compuesto` (productos que se fabrican)
- [ ] Crear tabla `formula_produccion` (BOM - Bill of Materials)
- [ ] Crear tabla `linea_formula` (componentes del producto)
- [ ] Crear tabla `orden_produccion` (órdenes de fabricación)
- [ ] Crear tabla `proceso_produccion` (etapas del proceso)
- [ ] Crear tabla `consumo_materiales` (materiales usados)
- [ ] Crear tabla `produccion_terminada` (productos fabricados)
- [ ] SP: `sp_explosionar_formula(id_producto, cantidad)`
- [ ] SP: `sp_costo_produccion(id_orden)`
- [ ] Trigger: Descontar materiales al producir

**Backend (16 tareas)**
- [ ] Entidad `ProductoCompuesto.java`
- [ ] Entidad `FormulaProduccion.java`
- [ ] Entidad `LineaFormula.java`
- [ ] Entidad `OrdenProduccion.java`
- [ ] Enum `EstadoOrden` (PLANIFICADA, EN_PROCESO, FINALIZADA, CANCELADA)
- [ ] `FormulaRepository` + queries
- [ ] `OrdenProduccionRepository` + queries
- [ ] `FormulaService` + Impl (CRUD fórmulas, explosión BOM)
- [ ] `OrdenProduccionService` + Impl (planificar, ejecutar)
- [ ] `ProduccionService` + Impl (consumir materiales, producir)
- [ ] Integrar con Inventario (descontar materiales, agregar producto terminado)
- [ ] Integrar con Contabilidad (costeo de producción)
- [ ] `ProduccionController` (vistas)
- [ ] `ProduccionRestController` (API)
- [ ] Cálculo de costos estándar vs reales
- [ ] Reportes de eficiencia de producción

**Frontend (10 tareas)**
- [ ] Vista: `produccion/formulas.html` (CRUD fórmulas/BOM)
- [ ] Vista: `produccion/ordenes.html` (lista de órdenes)
- [ ] Vista: `produccion/crear-orden.html`
- [ ] Vista: `produccion/ejecutar-orden.html` (registrar producción)
- [ ] Vista: `produccion/reportes/costos.html`
- [ ] Vista: `produccion/reportes/eficiencia.html`
- [ ] JavaScript: `formulas.js`
- [ ] JavaScript: `ordenes-produccion.js`
- [ ] Dashboard: Widget de órdenes activas
- [ ] Calculadora de costos de producción

**Integraciones (8 tareas)**
- [ ] Validar disponibilidad de materiales antes de producir
- [ ] Actualizar stock de materias primas (salida)
- [ ] Actualizar stock de producto terminado (entrada)
- [ ] Generar asiento contable de producción
- [ ] Reportes: Costo de producción por orden
- [ ] Reportes: Materiales más usados
- [ ] Exportar órdenes a PDF
- [ ] Notificaciones: Orden finalizada

**Testing (6 tareas)**
- [ ] `FormulaServiceTest` - Explosión de BOM
- [ ] `OrdenProduccionServiceTest`
- [ ] Test: Consumir materiales correctamente
- [ ] Test: Generar producto terminado
- [ ] Test: Cálculo de costos
- [ ] Test: Stock insuficiente impide producción

**Documentación (2 tareas)**
- [ ] `SPRINT_7_FASE_1_PRODUCCION.md`
- [ ] `MANUAL_GESTION_PRODUCCION.md`

---

### FASE 7.2: Mejoras Técnicas del Sistema (IMPORTANTE)

**Duración:** 6-8 días | **Tareas:** 38 tareas  
**Prioridad:** ⭐⭐⭐ ALTA

#### Estado Actual Detectado (MEJORAS_FUTURAS.md)
- ⚠️ Username es teléfono (debería ser username independiente)
- ⚠️ Uso de `Timestamp` (debería ser `LocalDateTime`)
- ⚠️ Falta "Remember Me" en login
- ⚠️ Sin auditoría completa de cambios

#### Tareas Principales (38)

**Migración Username (13 tareas)**
- [ ] Alterar tabla `usuario` (agregar campo username UNIQUE)
- [ ] Migración de datos (generar usernames temporales)
- [ ] Actualizar `Usuario.java` (agregar campo username)
- [ ] Actualizar `UsuarioRepository` (findByUsername, existsByUsername)
- [ ] Actualizar `UserDetailsServiceImpl` (usar username en lugar de teléfono)
- [ ] Actualizar `SecurityConfig` (usernameParameter = "username")
- [ ] Actualizar `auth/login.html` (campo username)
- [ ] Actualizar `auth/register.html` (campo username + validación)
- [ ] Actualizar `perfil/ver.html` (mostrar username)
- [ ] Actualizar `perfil/editar.html` (editar username)
- [ ] JavaScript: Validar username en tiempo real
- [ ] Testing: Login con username
- [ ] Documentación: Guía de migración para usuarios

**Migración LocalDateTime (8 tareas)**
- [ ] Actualizar `Usuario.java` (Timestamp → LocalDateTime)
- [ ] Actualizar `Factura.java` (si aplica)
- [ ] Actualizar todas las entidades con fechas
- [ ] Actualizar templates (usar #temporals en lugar de #dates)
- [ ] Actualizar servicios (usar LocalDateTime.now())
- [ ] Testing: Fechas se guardan correctamente
- [ ] Verificar compatibilidad con MySQL
- [ ] Documentación de cambio

**Remember Me (4 tareas)**
- [ ] Configurar en `SecurityConfig` (rememberMe + token validity)
- [ ] Crear tabla `persistent_logins` para tokens
- [ ] Agregar checkbox en `auth/login.html`
- [ ] Testing: Remember me funciona

**Auditoría Avanzada (8 tareas)**
- [ ] Crear tabla `auditoria_cambios` (entidad, acción, usuario, antes, después)
- [ ] Crear `AuditoriaService` (registrar cambios)
- [ ] Aspect: Interceptar cambios automáticamente (@Auditable)
- [ ] Vista: `admin/auditoria.html` (consultar cambios)
- [ ] Filtros: Por entidad, usuario, fecha
- [ ] Exportar auditoría a Excel
- [ ] Testing: Auditoría se registra
- [ ] Documentación

**Otras Mejoras (5 tareas)**
- [ ] Rate limiting en endpoints críticos
- [ ] Configuración de CORS adecuada
- [ ] Compresión de respuestas HTTP (Gzip)
- [ ] Caché de recursos estáticos
- [ ] Documentación API con OpenAPI/Swagger

---

### FASE 7.3: Seguridad Avanzada (CRÍTICA)

**Duración:** 4-6 días | **Tareas:** 28 tareas  
**Prioridad:** ⭐⭐⭐ ALTA

#### Tareas Principales (28)

**Autenticación 2FA (10 tareas)**
- [ ] Agregar campo `secret_2fa` en tabla usuario
- [ ] Agregar dependencia Google Authenticator
- [ ] Servicio: Generar QR code para 2FA
- [ ] Servicio: Validar código 2FA
- [ ] Vista: Configurar 2FA en perfil
- [ ] Vista: Login con 2FA
- [ ] Permitir códigos de backup
- [ ] Testing: 2FA completo
- [ ] Documentación para usuarios
- [ ] Notificación: 2FA activado/desactivado

**Bloqueo de Cuenta (8 tareas)**
- [ ] Tabla: `intentos_login_fallidos`
- [ ] Servicio: Contar intentos fallidos
- [ ] Bloquear cuenta después de 5 intentos
- [ ] Desbloqueo automático después de 30 minutos
- [ ] Desbloqueo manual por admin
- [ ] Notificación: Cuenta bloqueada
- [ ] Vista: Gestión de cuentas bloqueadas (admin)
- [ ] Testing

**Tokens de Sesión (6 tareas)**
- [ ] Migrar a JWT para API REST
- [ ] Token de refresh
- [ ] Expiración configurable
- [ ] Revocar tokens
- [ ] Vista: Sesiones activas por usuario
- [ ] Testing

**Otras Seguridades (4 tareas)**
- [ ] Content Security Policy (CSP) headers
- [ ] HTTPS obligatorio en producción
- [ ] Sanitización de inputs (prevenir XSS)
- [ ] SQL Injection protection review

---

### 📊 Resumen Sprint 7

```
┌─────────────────────────────────────────────────────────────┐
│                    SPRINT 7 - RESUMEN                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  FASE 7.1: Producción (opcional) [52 tareas]  8-10 días ⭐⭐ │
│  FASE 7.2: Mejoras Técnicas       [38 tareas]  6-8 días ⭐⭐⭐│
│  FASE 7.3: Seguridad Avanzada     [28 tareas]  4-6 días ⭐⭐⭐│
│  Testing + Documentación          [10 tareas]  2-3 días ⭐⭐ │
│                                                              │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│  TOTAL SPRINT 7                   [128 tareas] 20-27 días   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

**Nota:** Producción puede omitirse si no aplica, reduciendo a 76 tareas / 12-17 días

---

## 👥 SPRINT 8: RECURSOS HUMANOS + NÓMINA + REPORTES AVANZADOS

**Duración estimada:** 22-28 días  
**Prioridad:** ⭐⭐ MEDIA  
**Dependencias:** Sprint 5 (Contabilidad)

### 🎯 Objetivos Estratégicos
Gestión completa de RRHH + Nómina + Reportes financieros avanzados.

---

### FASE 8.1: Módulo de Recursos Humanos (NUEVA)

**Duración:** 8-10 días | **Tareas:** 48 tareas  
**Prioridad:** ⭐⭐ MEDIA

#### Tareas Principales (48)

**Base de Datos (10 tareas)**
- [ ] Crear tabla `empleado` (separada de usuario)
- [ ] Crear tabla `departamento`
- [ ] Crear tabla `puesto`
- [ ] Crear tabla `contrato_empleado`
- [ ] Crear tabla `asistencia`
- [ ] Crear tabla `ausencia` (vacaciones, permisos, bajas)
- [ ] Crear tabla `evaluacion_desempeno`
- [ ] Crear tabla `capacitacion`
- [ ] Vista: `v_empleados_activos`
- [ ] SP: `sp_reporte_asistencias(mes, anio)`

**Backend (14 tareas)**
- [ ] Entidad `Empleado.java`
- [ ] Entidad `Departamento.java`
- [ ] Entidad `Puesto.java`
- [ ] Entidad `ContratoEmpleado.java`
- [ ] Entidad `Asistencia.java`
- [ ] Entidad `Ausencia.java`
- [ ] Enum `TipoContrato` (INDEFINIDO, PLAZO_FIJO, EVENTUAL)
- [ ] Enum `TipoAusencia` (VACACIONES, PERMISO, BAJA_MEDICA, INCAPACIDAD)
- [ ] `EmpleadoRepository` + queries
- [ ] `AsistenciaRepository` + queries
- [ ] `EmpleadoService` + Impl
- [ ] `AsistenciaService` + Impl
- [ ] `EmpleadoController` (vistas)
- [ ] `EmpleadoRestController` (API)

**Frontend (10 tareas)**
- [ ] Vista: `rrhh/empleados/lista.html`
- [ ] Vista: `rrhh/empleados/form.html`
- [ ] Vista: `rrhh/empleados/detalle.html` (ficha completa)
- [ ] Vista: `rrhh/asistencias/registro.html`
- [ ] Vista: `rrhh/asistencias/reporte.html`
- [ ] Vista: `rrhh/ausencias/gestionar.html`
- [ ] Vista: `rrhh/departamentos.html`
- [ ] Vista: `rrhh/puestos.html`
- [ ] JavaScript: `empleados.js`
- [ ] JavaScript: `asistencias.js`

**Integraciones (8 tareas)**
- [ ] Integrar con Usuario (un empleado puede tener usuario)
- [ ] Portal del empleado (ver recibos, solicitar vacaciones)
- [ ] Reportes: Empleados por departamento
- [ ] Reportes: Contratos próximos a vencer
- [ ] Reportes: Ausencias por tipo
- [ ] Exportar ficha de empleado a PDF
- [ ] Notificaciones: Contrato por vencer
- [ ] Dashboard: KPIs de RRHH

**Testing (4 tareas)**
- [ ] `EmpleadoServiceTest`
- [ ] `AsistenciaServiceTest`
- [ ] Test: Registro de asistencias
- [ ] Test: Gestión de ausencias

**Documentación (2 tareas)**
- [ ] `SPRINT_8_FASE_1_RRHH.md`
- [ ] `MANUAL_RECURSOS_HUMANOS.md`

---

### FASE 8.2: Módulo de Nómina (NUEVA)

**Duración:** 10-12 días | **Tareas:** 56 tareas  
**Prioridad:** ⭐⭐⭐ ALTA (si aplica)

#### Tareas Principales (56)

**Base de Datos (12 tareas)**
- [ ] Crear tabla `nomina` (periodo, estado, total)
- [ ] Crear tabla `concepto_nomina` (catálogo: sueldo, bono, deducción)
- [ ] Crear tabla `detalle_nomina` (empleado, concepto, monto)
- [ ] Crear tabla `regla_calculo` (fórmulas de nómina)
- [ ] Crear tabla `retencion` (impuestos, seguridad social)
- [ ] Crear tabla `prestamo_empleado`
- [ ] Crear tabla `anticipo_salario`
- [ ] Vista: `v_nomina_actual`
- [ ] SP: `sp_calcular_nomina(id_periodo)`
- [ ] SP: `sp_generar_recibos(id_nomina)`
- [ ] SP: `sp_reporte_costos_nomina(anio)`
- [ ] Trigger: Registrar asiento contable de nómina

**Backend (18 tareas)**
- [ ] Entidad `Nomina.java`
- [ ] Entidad `ConceptoNomina.java`
- [ ] Entidad `DetalleNomina.java`
- [ ] Entidad `ReglaCalculo.java`
- [ ] Entidad `Retencion.java`
- [ ] Enum `TipoConcepto` (PERCEPCION, DEDUCCION)
- [ ] Enum `EstadoNomina` (BORRADOR, CALCULADA, PAGADA, CERRADA)
- [ ] `NominaRepository` + queries
- [ ] `ConceptoNominaRepository`
- [ ] `NominaService` + Impl (cálculo, generación)
- [ ] `CalculoNominaService` (motor de cálculo)
- [ ] Integrar con RRHH (obtener empleados activos)
- [ ] Integrar con Asistencia (horas trabajadas, extras)
- [ ] Integrar con Contabilidad (asientos de nómina)
- [ ] Integrar con Pagos (pago de nómina)
- [ ] `NominaController` (vistas)
- [ ] `NominaRestController` (API)
- [ ] Generador de recibos de pago (PDF)

**Frontend (12 tareas)**
- [ ] Vista: `nomina/periodos.html` (gestión de periodos)
- [ ] Vista: `nomina/calcular.html` (cálculo de nómina)
- [ ] Vista: `nomina/revision.html` (revisar antes de pagar)
- [ ] Vista: `nomina/recibos.html` (generar recibos)
- [ ] Vista: `nomina/conceptos.html` (CRUD conceptos)
- [ ] Vista: `nomina/retenciones.html` (configurar retenciones)
- [ ] Vista: `nomina/reportes/costos.html`
- [ ] Vista: `nomina/reportes/deducciones.html`
- [ ] Vista: `portal-empleado/mi-recibo.html`
- [ ] JavaScript: `nomina.js`
- [ ] JavaScript: `calculo-nomina.js`
- [ ] Dashboard: Widget de nómina actual

**Integraciones (8 tareas)**
- [ ] Depósito directo a bancos (archivo SEPA/ACH)
- [ ] Envío de recibos por email
- [ ] Cálculo de impuestos según tablas
- [ ] Reportes legales (libro de remuneraciones)
- [ ] Exportar nómina a Excel
- [ ] Integración con banco (pago electrónico)
- [ ] Notificaciones: Nómina lista
- [ ] Archivo para declaraciones fiscales

**Testing (4 tareas)**
- [ ] `NominaServiceTest`
- [ ] `CalculoNominaServiceTest`
- [ ] Test: Cálculo correcto de nómina
- [ ] Test: Asiento contable generado

**Documentación (2 tareas)**
- [ ] `SPRINT_8_FASE_2_NOMINA.md`
- [ ] `MANUAL_NOMINA.md`

---

### FASE 8.3: Reportes Financieros Avanzados (AMPLIAR)

**Duración:** 4-6 días | **Tareas:** 32 tareas  
**Prioridad:** ⭐⭐⭐ ALTA

#### Estado Actual
- ✅ Reportes básicos implementados (Sprint 4)
- ⚠️ Faltan estados financieros completos
- ⚠️ Falta análisis financiero

#### Tareas Principales (32)

**Base de Datos (6 tareas)**
- [ ] SP: `sp_estado_resultados(fecha_inicio, fecha_fin)`
- [ ] SP: `sp_balance_general(fecha_corte)`
- [ ] SP: `sp_flujo_efectivo(periodo)`
- [ ] SP: `sp_razones_financieras(periodo)`
- [ ] SP: `sp_punto_equilibrio()`
- [ ] Vista: `v_kpis_financieros`

**Backend (8 tareas)**
- [ ] `EstadosFinancierosService` + Impl
- [ ] DTO: `EstadoResultadosDTO`
- [ ] DTO: `BalanceGeneralDTO`
- [ ] DTO: `FlujoEfectivoDTO`
- [ ] DTO: `RazonesFinancierasDTO`
- [ ] `EstadosFinancierosController`
- [ ] Exportar estados financieros a PDF
- [ ] Exportar estados financieros a Excel

**Frontend (10 tareas)**
- [ ] Vista: `reportes/financieros/estado-resultados.html`
- [ ] Vista: `reportes/financieros/balance-general.html`
- [ ] Vista: `reportes/financieros/flujo-efectivo.html`
- [ ] Vista: `reportes/financieros/razones-financieras.html`
- [ ] Vista: `reportes/financieros/punto-equilibrio.html`
- [ ] Dashboard financiero ejecutivo
- [ ] Gráficas: Tendencias financieras
- [ ] Gráficas: Comparativas periodo a periodo
- [ ] JavaScript: `estados-financieros.js`
- [ ] Exportación personalizada

**Análisis Avanzado (4 tareas)**
- [ ] Proyecciones financieras (básicas)
- [ ] Análisis de tendencias
- [ ] Comparativas año vs año
- [ ] Alertas de salud financiera

**Testing (2 tareas)**
- [ ] `EstadosFinancierosServiceTest`
- [ ] Test: Cálculo correcto de estados

**Documentación (2 tareas)**
- [ ] `SPRINT_8_FASE_3_REPORTES_AVANZADOS.md`
- [ ] `MANUAL_ESTADOS_FINANCIEROS.md`

---

### 📊 Resumen Sprint 8

```
┌─────────────────────────────────────────────────────────────┐
│                    SPRINT 8 - RESUMEN                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  FASE 8.1: RRHH                   [48 tareas]  8-10 días ⭐⭐ │
│  FASE 8.2: Nómina                 [56 tareas] 10-12 días ⭐⭐⭐│
│  FASE 8.3: Reportes Avanzados     [32 tareas]  4-6 días ⭐⭐⭐│
│  Testing + Documentación          [12 tareas]  2-3 días ⭐⭐ │
│                                                              │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│  TOTAL SPRINT 8                   [148 tareas] 24-31 días   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 🎨 SPRINT 9: UX/UI + PWA + OPTIMIZACIONES

**Duración estimada:** 14-18 días  
**Prioridad:** ⭐ BAJA (mejora de experiencia)  
**Dependencias:** Ninguna crítica

### 🎯 Objetivos Estratégicos
Mejorar experiencia de usuario + aplicación progresiva + optimizaciones de rendimiento.

---

### FASE 9.1: Mejoras de UX/UI

**Duración:** 5-7 días | **Tareas:** 36 tareas  

**Tema Oscuro/Claro (12 tareas)**
- [ ] CSS: Variables globales para colores
- [ ] Crear `dark-theme.css`
- [ ] Toggle de tema en navbar
- [ ] Guardar preferencia en localStorage
- [ ] Detectar preferencia del sistema
- [ ] Actualizar todos los templates
- [ ] Iconos y gráficas modo oscuro
- [ ] Testing en ambos modos
- [ ] Accesibilidad (contraste WCAG)
- [ ] Animación de transición
- [ ] Persistencia en BD (opcional)
- [ ] Documentación

**Mejoras de Accesibilidad (10 tareas)**
- [ ] ARIA labels en formularios
- [ ] Navegación por teclado completa
- [ ] Skip to content links
- [ ] Focus visible en todos los elementos
- [ ] Lectores de pantalla (screen readers)
- [ ] Textos alternativos en imágenes
- [ ] Tamaño de fuente ajustable
- [ ] Alto contraste para discapacidad visual
- [ ] Testing con herramientas de accesibilidad
- [ ] Cumplimiento WCAG 2.1 AA

**Responsive Design Avanzado (8 tareas)**
- [ ] Optimizar para tablets
- [ ] Menú hamburguesa en móviles
- [ ] Tablas responsivas (scroll horizontal)
- [ ] Modales adaptables
- [ ] Formularios optimizados para móvil
- [ ] Touch gestures
- [ ] Testing en múltiples dispositivos
- [ ] Breakpoints optimizados

**Otras Mejoras UI (6 tareas)**
- [ ] Animaciones y transiciones suaves
- [ ] Loading skeletons
- [ ] Infinite scroll en listas largas
- [ ] Drag and drop donde aplique
- [ ] Tooltips informativos
- [ ] Mejoras en feedback visual

---

### FASE 9.2: Progressive Web App (PWA)

**Duración:** 5-7 días | **Tareas:** 32 tareas  

**PWA Base (12 tareas)**
- [ ] Crear `manifest.json` completo
- [ ] Service Worker para cacheo
- [ ] Estrategia de caché (network-first, cache-first)
- [ ] Offline fallback page
- [ ] Íconos PWA (múltiples tamaños)
- [ ] Splash screens
- [ ] Instalable en móviles
- [ ] Instalable en desktop
- [ ] Update prompt para nueva versión
- [ ] Testing de instalación
- [ ] HTTPS obligatorio
- [ ] Documentación de PWA

**Funcionalidad Offline (10 tareas)**
- [ ] Caché de vistas principales
- [ ] IndexedDB para datos offline
- [ ] Sincronización cuando vuelve online
- [ ] Indicador de estado offline
- [ ] Formularios offline (guardar en local)
- [ ] Cola de acciones pendientes
- [ ] Conflicto de sincronización
- [ ] Background sync
- [ ] Testing de modo offline
- [ ] Documentación

**Push Notifications (10 tareas)**
- [ ] Configurar Web Push API
- [ ] Solicitar permisos de notificación
- [ ] Backend: Enviar push notifications
- [ ] Personalizar notificaciones
- [ ] Notificaciones de recordatorio
- [ ] Notificaciones de alertas
- [ ] Gestionar suscripciones
- [ ] Desuscribirse de notificaciones
- [ ] Testing de push
- [ ] Documentación

---

### FASE 9.3: Optimizaciones de Rendimiento

**Duración:** 4-6 días | **Tareas:** 28 tareas  

**Backend (12 tareas)**
- [ ] Perfilado con JProfiler/VisualVM
- [ ] Optimizar queries N+1
- [ ] Caché de queries frecuentes
- [ ] Lazy loading de relaciones
- [ ] Connection pooling optimizado
- [ ] Índices de BD adicionales
- [ ] Batch processing
- [ ] Async processing mejorado
- [ ] Compresión de respuestas
- [ ] Rate limiting global
- [ ] Monitoreo con Actuator
- [ ] Métricas con Micrometer

**Frontend (10 tareas)**
- [ ] Minificación de CSS/JS
- [ ] Lazy loading de imágenes
- [ ] Code splitting
- [ ] Tree shaking
- [ ] Optimizar bundle size
- [ ] Preload de recursos críticos
- [ ] Defer de scripts no críticos
- [ ] Optimizar Chart.js (solo cargar necesario)
- [ ] CDN para librerías
- [ ] Testing de performance (Lighthouse)

**Base de Datos (6 tareas)**
- [ ] Análisis de slow queries
- [ ] Optimizar índices
- [ ] Particionado de tablas grandes
- [ ] Archivado de datos históricos
- [ ] Vacuum/optimize tables
- [ ] Monitoreo de rendimiento

---

### 📊 Resumen Sprint 9

```
┌─────────────────────────────────────────────────────────────┐
│                    SPRINT 9 - RESUMEN                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  FASE 9.1: UX/UI Mejoras          [36 tareas]  5-7 días  ⭐  │
│  FASE 9.2: PWA                    [32 tareas]  5-7 días  ⭐  │
│  FASE 9.3: Optimizaciones         [28 tareas]  4-6 días  ⭐⭐ │
│  Testing + Documentación          [8 tareas]   2-3 días  ⭐  │
│                                                              │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│  TOTAL SPRINT 9                   [104 tareas] 16-23 días   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 📊 RESUMEN EJECUTIVO DE SPRINTS FUTUROS

```
╔═══════════════════════════════════════════════════════════════════╗
║                 ROADMAP SPRINTS 5-9 (2026)                        ║
╠═══════════════════════════════════════════════════════════════════╣
║                                                                    ║
║  SPRINT 5: Contabilidad + FE CR + Pagos                           ║
║  ├─ Duración: 24-32 días                                          ║
║  ├─ Tareas: 199                                                   ║
║  ├─ Prioridad: ⭐⭐⭐ CRÍTICA                                        ║
║  └─ Valor: Sistema financiero completo + cumplimiento CR         ║
║                                                                    ║
║  SPRINT 6: Multi-Divisa + Inventario + Proveedores                ║
║  ├─ Duración: 20-27 días                                          ║
║  ├─ Tareas: 146                                                   ║
║  ├─ Prioridad: ⭐⭐⭐ ALTA                                           ║
║  └─ Valor: Internacionalización + control operativo completo     ║
║                                                                    ║
║  SPRINT 7: Producción + Mejoras + Seguridad                       ║
║  ├─ Duración: 20-27 días (12-17 sin producción)                  ║
║  ├─ Tareas: 128 (76 sin producción)                              ║
║  ├─ Prioridad: ⭐⭐⭐ ALTA (mejoras) / ⭐⭐ MEDIA (producción)        ║
║  └─ Valor: Sistema robusto y seguro + manufactura (opcional)     ║
║                                                                    ║
║  SPRINT 8: RRHH + Nómina + Reportes Avanzados                     ║
║  ├─ Duración: 24-31 días                                          ║
║  ├─ Tareas: 148                                                   ║
║  ├─ Prioridad: ⭐⭐ MEDIA-ALTA                                      ║
║  └─ Valor: Gestión de personal + análisis financiero avanzado    ║
║                                                                    ║
║  SPRINT 9: UX/UI + PWA + Optimizaciones                           ║
║  ├─ Duración: 16-23 días                                          ║
║  ├─ Tareas: 104                                                   ║
║  ├─ Prioridad: ⭐ BAJA                                             ║
║  └─ Valor: Experiencia de usuario premium + rendimiento          ║
║                                                                    ║
║  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  ║
║  TOTAL: 725 tareas | 104-140 días (~5-7 meses)                   ║
║                                                                    ║
╚═══════════════════════════════════════════════════════════════════╝
```

---

## 🎯 RECOMENDACIONES ESTRATÉGICAS

### Orden Sugerido de Implementación

1. **SPRINT 5** (OBLIGATORIO) - Base financiera y legal
2. **SPRINT 6** (ALTAMENTE RECOMENDADO) - Operaciones completas
3. **SPRINT 7** (RECOMENDADO) - Seguridad y estabilidad
   - Omitir Fase 7.1 (Producción) si no aplica
4. **SPRINT 8** (CONDICIONAL) - Solo si se requiere gestión de personal
5. **SPRINT 9** (OPCIONAL) - Mejoras de experiencia

### Sprints Modulares (Pueden dividirse)

- **Sprint 6:** Puede hacerse en 2 partes (6A: Multi-Divisa + Inventario, 6B: Proveedores)
- **Sprint 7:** Producción es completamente opcional
- **Sprint 8:** RRHH y Nómina pueden separarse
- **Sprint 9:** Totalmente divisible por fase

### Criterios de Priorización

| Sprint | Omitible | Condicional | Crítico |
|--------|----------|-------------|---------|
| Sprint 5 | ❌ | - | ✅ |
| Sprint 6 Fase 1 (Multi-Divisa) | ✅ | Solo si facturación internacional | - |
| Sprint 6 Fase 2 (Inventario) | - | ✅ | Si maneja stock |
| Sprint 6 Fase 3 (Proveedores) | - | ✅ | Si tiene compras |
| Sprint 7 Fase 1 (Producción) | ✅ | Solo manufactura | - |
| Sprint 7 Fase 2 (Mejoras) | - | - | ✅ |
| Sprint 7 Fase 3 (Seguridad) | - | - | ✅ |
| Sprint 8 (RRHH/Nómina) | ✅ | Solo si gestiona empleados | - |
| Sprint 9 (UX/PWA) | ✅ | - | - |

---

## 📝 PRÓXIMOS PASOS

1. ✅ **Revisar clasificación de sprints**
2. ⏳ **Aprobar Sprint 5** (si procede)
3. ⏳ **Crear documentos detallados** por sprint
4. ⏳ **Definir criterios de aceptación** por fase
5. ⏳ **Estimar recursos y tiempo** disponible

---

**Documento generado:** 16 de enero de 2026  
**Creado por:** GitHub Copilot  
**Estado:** 📋 CLASIFICACIÓN PRELIMINAR  

---

¿Deseas que proceda a crear los documentos de propuesta detallados para cada sprint?
