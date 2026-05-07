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

