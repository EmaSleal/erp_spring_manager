# 🚀 PROPUESTA SPRINT 5 - Contabilidad y Facturación Electrónica Costa Rica

**Proyecto:** WhatsApp Orders Manager - ERP Spring Boot  
**Sprint:** 5 - Contabilidad + Facturación Electrónica CR  
**Fecha de creación:** 16 de enero de 2026  
**Duración estimada:** 3-4 semanas (20-28 días)  
**Estado:** 📋 PROPUESTA PARA APROBACIÓN

---

## 📊 ANÁLISIS DE FACTIBILIDAD

### ✅ Sprint 5 Original (Propuesto en TAREAS_COMPLETADAS)

| Prioridad | Módulo | Factibilidad | Recomendación |
|-----------|--------|--------------|---------------|
| **Alta** | Módulo de Pagos | ✅ 85% | Integrar en Fase 1 (básico) |
| **Alta** | Conciliación bancaria | 🟡 60% | Posponer a Sprint 6 |
| **Alta** | Métodos de pago múltiples | ✅ 90% | Integrar en Fase 1 |
| **Alta** | Historial de transacciones | ✅ 95% | Integrar en Fase 2 (contabilidad) |
| **Media** | Testing automatizado | ✅ 100% | Integrar en todas las fases |
| **Media** | Integration tests | ✅ 90% | Fase de testing dedicada |
| **Baja** | Tema oscuro/claro | ⭕ - | Sprint 7+ |
| **Baja** | PWA | ⭕ - | Sprint 8+ |

**Conclusión:** El Sprint 5 original es factible, pero puede optimizarse integrándolo con el módulo de contabilidad de Costa Rica.

---

## 🎯 PROPUESTA MEJORADA: SPRINT 5 HÍBRIDO

### Visión General

Combinar **Contabilidad Costa Rica + Facturación Electrónica + Pagos** en un sprint cohesivo que:

1. ✅ Cumpla con normativa fiscal de CR (Hacienda)
2. ✅ Implemente contabilidad de doble partida
3. ✅ Gestione pagos y conciliación básica
4. ✅ Integre facturación electrónica v4.4
5. ✅ Mantenga compatibilidad con sistema actual

### Ventajas de este Enfoque

✅ **Sinergia técnica:** Pagos → Asientos contables → Facturación electrónica  
✅ **Cumplimiento fiscal:** Todo listo para operar en Costa Rica  
✅ **Valor de negocio:** Sistema completo de gestión financiera  
✅ **Reutilización:** Facturas actuales + nuevos campos FE  
✅ **Extensible:** Base sólida para multi-divisa (Sprint 6)  

---

## 📋 ESTRUCTURA DEL SPRINT 5

```
┌─────────────────────────────────────────────────────────────┐
│                    SPRINT 5 - ROADMAP                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  FASE 1: Módulo de Pagos (Base)          [5-7 días]  ⭐⭐⭐  │
│  FASE 2: Contabilidad (Doble Partida)    [7-9 días]  ⭐⭐⭐  │
│  FASE 3: Facturación Electrónica CR       [6-8 días]  ⭐⭐⭐  │
│  FASE 4: Testing Automatizado             [4-5 días]  ⭐⭐   │
│  FASE 5: Documentación y Manuales         [2-3 días]  ⭐⭐   │
│                                                              │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│  TOTAL: 24-32 días (~4-5 semanas)                           │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 🏗️ FASE 1: MÓDULO DE PAGOS (Base Financiera)

**Duración:** 5-7 días  
**Prioridad:** ⭐⭐⭐ CRÍTICA  
**Progreso estimado:** 0/45 tareas

### Objetivos

- Gestionar pagos de facturas (parciales/totales)
- Soportar múltiples métodos de pago
- Registrar transacciones financieras
- Conciliación básica (sin integración bancaria)

### 1.1 Base de Datos (8 tareas)

#### Tabla: `metodo_pago`
```sql
CREATE TABLE metodo_pago (
    id_metodo_pago INT PRIMARY KEY AUTO_INCREMENT,
    codigo VARCHAR(2) NOT NULL UNIQUE COMMENT 'Código Hacienda CR: 01-08',
    nombre VARCHAR(50) NOT NULL COMMENT 'Efectivo, Tarjeta, Transferencia',
    descripcion TEXT,
    requiere_referencia BOOLEAN DEFAULT FALSE COMMENT 'Si requiere número de referencia/cheque',
    activo BOOLEAN DEFAULT TRUE,
    icono VARCHAR(50) COMMENT 'Clase CSS para icono',
    
    -- Auditoría
    create_by INT,
    create_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by INT,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='Catálogo de métodos de pago (alineado a Hacienda CR)';

-- Datos iniciales (catálogo Hacienda CR)
INSERT INTO metodo_pago (codigo, nombre, descripcion, requiere_referencia, icono) VALUES
('01', 'Efectivo', 'Pago en efectivo', FALSE, 'bi-cash'),
('02', 'Tarjeta', 'Tarjeta de crédito/débito', TRUE, 'bi-credit-card'),
('03', 'Cheque', 'Cheque bancario', TRUE, 'bi-clipboard-check'),
('04', 'Transferencia', 'Transferencia bancaria', TRUE, 'bi-bank'),
('05', 'Recaudado por terceros', 'Cobro por terceros', TRUE, 'bi-people'),
('99', 'Otros', 'Otros métodos de pago', FALSE, 'bi-question-circle');
```

#### Tabla: `pago`
```sql
CREATE TABLE pago (
    id_pago INT PRIMARY KEY AUTO_INCREMENT,
    numero_pago VARCHAR(20) UNIQUE NOT NULL COMMENT 'Número consecutivo: PG-00001',
    id_factura INT NOT NULL COMMENT 'Factura que se está pagando',
    id_metodo_pago INT NOT NULL COMMENT 'Método de pago utilizado',
    
    -- Montos
    monto DECIMAL(10, 2) NOT NULL COMMENT 'Monto del pago',
    moneda VARCHAR(3) DEFAULT 'CRC' COMMENT 'CRC, USD, etc.',
    tipo_cambio DECIMAL(10, 4) DEFAULT 1.0000 COMMENT 'Tipo de cambio al momento del pago',
    
    -- Detalles
    fecha_pago DATE NOT NULL COMMENT 'Fecha en que se realizó el pago',
    referencia VARCHAR(100) COMMENT 'Número de cheque, referencia de transferencia, etc.',
    observaciones TEXT,
    
    -- Estado
    estado ENUM('PENDIENTE', 'CONFIRMADO', 'ANULADO') DEFAULT 'CONFIRMADO',
    fecha_anulacion DATE COMMENT 'Fecha de anulación si aplica',
    motivo_anulacion TEXT,
    
    -- Auditoría
    create_by INT,
    create_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by INT,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (id_factura) REFERENCES factura(idFactura) ON DELETE RESTRICT,
    FOREIGN KEY (id_metodo_pago) REFERENCES metodo_pago(id_metodo_pago),
    
    INDEX idx_pago_factura (id_factura),
    INDEX idx_pago_fecha (fecha_pago DESC),
    INDEX idx_pago_metodo (id_metodo_pago)
) ENGINE=InnoDB COMMENT='Registro de pagos realizados a facturas';
```

#### Tabla: `estado_cuenta_cliente`
```sql
CREATE TABLE estado_cuenta_cliente (
    id_estado_cuenta INT PRIMARY KEY AUTO_INCREMENT,
    id_cliente INT NOT NULL,
    
    -- Saldos acumulados
    total_facturado DECIMAL(12, 2) DEFAULT 0.00 COMMENT 'Total de facturas emitidas',
    total_pagado DECIMAL(12, 2) DEFAULT 0.00 COMMENT 'Total pagado por el cliente',
    saldo_pendiente DECIMAL(12, 2) DEFAULT 0.00 COMMENT 'Saldo pendiente de pago',
    
    -- Crédito
    limite_credito DECIMAL(10, 2) DEFAULT 0.00 COMMENT 'Límite de crédito autorizado',
    credito_disponible DECIMAL(10, 2) DEFAULT 0.00 COMMENT 'Crédito disponible',
    
    -- Estadísticas
    ultima_factura DATE COMMENT 'Fecha de última factura',
    ultimo_pago DATE COMMENT 'Fecha de último pago',
    dias_credito_promedio INT DEFAULT 0 COMMENT 'Promedio de días para pagar',
    
    -- Auditoría
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (id_cliente) REFERENCES cliente(idCliente) ON DELETE CASCADE,
    UNIQUE KEY uk_cliente_estado_cuenta (id_cliente)
) ENGINE=InnoDB COMMENT='Estado de cuenta consolidado por cliente';
```

**Tareas BD:**
- [ ] 1.1.1 - Crear tabla `metodo_pago` con datos iniciales
- [ ] 1.1.2 - Crear tabla `pago` con relaciones
- [ ] 1.1.3 - Crear tabla `estado_cuenta_cliente`
- [ ] 1.1.4 - Crear trigger para actualizar estado cuenta al insertar pago
- [ ] 1.1.5 - Crear stored procedure `sp_estado_cuenta_cliente(id_cliente)`
- [ ] 1.1.6 - Crear vista `v_pagos_factura` (join pago + factura + cliente)
- [ ] 1.1.7 - Migración: alterar tabla `factura` para agregar campo `saldo_pendiente`
- [ ] 1.1.8 - Script de migración y rollback completo

### 1.2 Backend - Modelos (6 tareas)

- [ ] 1.2.1 - Crear entidad `MetodoPago.java`
- [ ] 1.2.2 - Crear entidad `Pago.java` con validaciones
- [ ] 1.2.3 - Crear entidad `EstadoCuentaCliente.java`
- [ ] 1.2.4 - Crear DTO `PagoDTO.java`
- [ ] 1.2.5 - Crear DTO `EstadoCuentaDTO.java`
- [ ] 1.2.6 - Crear enum `EstadoPago.java` (PENDIENTE, CONFIRMADO, ANULADO)

### 1.3 Backend - Repositories (4 tareas)

- [ ] 1.3.1 - Crear `MetodoPagoRepository.java` con queries
- [ ] 1.3.2 - Crear `PagoRepository.java` con búsquedas avanzadas
- [ ] 1.3.3 - Crear `EstadoCuentaRepository.java`
- [ ] 1.3.4 - Queries derivadas: `findByFactura`, `findByFechaPagoBetween`, `findByMetodoPago`

### 1.4 Backend - Services (8 tareas)

- [ ] 1.4.1 - Crear `PagoService.java` (interfaz)
- [ ] 1.4.2 - Crear `PagoServiceImpl.java` con lógica de negocio
- [ ] 1.4.3 - Implementar método `registrarPago(PagoDTO)` con validaciones
- [ ] 1.4.4 - Implementar método `anularPago(Integer idPago, String motivo)`
- [ ] 1.4.5 - Implementar método `calcularSaldoFactura(Integer idFactura)`
- [ ] 1.4.6 - Crear `EstadoCuentaService.java` para cálculos
- [ ] 1.4.7 - Implementar `actualizarEstadoCuenta(Integer idCliente)`
- [ ] 1.4.8 - Integrar con `NotificacionService` (pago recibido)

### 1.5 Backend - Controllers (4 tareas)

- [ ] 1.5.1 - Crear `PagoRestController.java` con endpoints CRUD
- [ ] 1.5.2 - Endpoint: `POST /api/pagos` - Registrar pago
- [ ] 1.5.3 - Endpoint: `PUT /api/pagos/{id}/anular` - Anular pago
- [ ] 1.5.4 - Endpoint: `GET /api/clientes/{id}/estado-cuenta` - Estado de cuenta

### 1.6 Frontend - Vistas (6 tareas)

- [ ] 1.6.1 - Crear `templates/modules/pagos/lista.html` (listado de pagos)
- [ ] 1.6.2 - Crear `templates/modules/pagos/registrar.html` (formulario de pago)
- [ ] 1.6.3 - Crear `templates/modules/pagos/estado-cuenta.html` (por cliente)
- [ ] 1.6.4 - Actualizar `facturacion/detalle.html` (agregar sección de pagos)
- [ ] 1.6.5 - Modal de pago rápido en listado de facturas
- [ ] 1.6.6 - Crear badge de estado de pago en facturas

### 1.7 Frontend - JavaScript (4 tareas)

- [ ] 1.7.1 - Crear `static/modules/pagos/js/pagos.js`
- [ ] 1.7.2 - Implementar validación de monto (no mayor al saldo)
- [ ] 1.7.3 - Implementar cálculo automático de saldo restante
- [ ] 1.7.4 - Integrar con DataTables para historial de pagos

### 1.8 Testing (5 tareas)

- [ ] 1.8.1 - Tests unitarios `PagoServiceTest`
- [ ] 1.8.2 - Test: Registrar pago parcial
- [ ] 1.8.3 - Test: Registrar pago completo (factura PAGADA)
- [ ] 1.8.4 - Test: Anular pago y revertir saldo
- [ ] 1.8.5 - Test: Validación de montos negativos/cero

---

## 📊 FASE 2: CONTABILIDAD (Doble Partida)

**Duración:** 7-9 días  
**Prioridad:** ⭐⭐⭐ CRÍTICA  
**Progreso estimado:** 0/58 tareas

### Objetivos

- Implementar contabilidad de doble partida
- Catálogo de cuentas contables (Plan de cuentas CR)
- Registro automático de asientos desde facturas/pagos
- Libro diario y mayor
- Balance de comprobación
- Estados financieros básicos

### 2.1 Base de Datos (10 tareas)

#### Tabla: `cuenta_contable`
```sql
CREATE TABLE cuenta_contable (
    id_cuenta INT PRIMARY KEY AUTO_INCREMENT,
    codigo VARCHAR(20) NOT NULL UNIQUE COMMENT 'Código contable: 1.1.01.001',
    nombre VARCHAR(200) NOT NULL,
    descripcion TEXT,
    
    -- Clasificación
    tipo_cuenta ENUM('ACTIVO', 'PASIVO', 'CAPITAL', 'INGRESO', 'GASTO') NOT NULL,
    naturaleza ENUM('DEUDORA', 'ACREEDORA') NOT NULL COMMENT 'ACTIVO/GASTO=DEUDORA, PASIVO/CAPITAL/INGRESO=ACREEDORA',
    nivel INT NOT NULL COMMENT '1=Mayor, 2=Sub-cuenta, 3=Auxiliar, etc.',
    id_cuenta_padre INT COMMENT 'Cuenta padre en jerarquía',
    
    -- Control
    acepta_movimientos BOOLEAN DEFAULT TRUE COMMENT 'FALSE si es solo agrupadora',
    requiere_auxiliar BOOLEAN DEFAULT FALSE COMMENT 'Si requiere detalle adicional (ej. cliente, proveedor)',
    activa BOOLEAN DEFAULT TRUE,
    
    -- Saldos acumulados (actualización trigger/batch)
    saldo_actual DECIMAL(15, 2) DEFAULT 0.00,
    saldo_inicial DECIMAL(15, 2) DEFAULT 0.00 COMMENT 'Saldo al inicio del ejercicio',
    
    -- Auditoría
    create_by INT,
    create_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by INT,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (id_cuenta_padre) REFERENCES cuenta_contable(id_cuenta),
    INDEX idx_tipo_cuenta (tipo_cuenta),
    INDEX idx_codigo_cuenta (codigo)
) ENGINE=InnoDB COMMENT='Catálogo de cuentas contables (Plan de cuentas)';
```

#### Tabla: `asiento_contable`
```sql
CREATE TABLE asiento_contable (
    id_asiento INT PRIMARY KEY AUTO_INCREMENT,
    numero_asiento VARCHAR(20) UNIQUE NOT NULL COMMENT 'Consecutivo: ASI-00001',
    
    -- Clasificación
    tipo_asiento ENUM('APERTURA', 'OPERACION', 'AJUSTE', 'CIERRE') DEFAULT 'OPERACION',
    fecha_asiento DATE NOT NULL,
    periodo VARCHAR(7) NOT NULL COMMENT 'YYYY-MM para agrupación',
    
    -- Origen del asiento (trazabilidad)
    origen ENUM('MANUAL', 'FACTURA', 'PAGO', 'NOTA_CREDITO', 'NOTA_DEBITO', 'AUTOMATICO') DEFAULT 'MANUAL',
    id_origen INT COMMENT 'ID de factura/pago que originó el asiento',
    
    -- Detalles
    concepto TEXT NOT NULL COMMENT 'Descripción del asiento',
    observaciones TEXT,
    
    -- Control
    estado ENUM('BORRADOR', 'REGISTRADO', 'ANULADO') DEFAULT 'BORRADOR',
    fecha_anulacion DATE,
    motivo_anulacion TEXT,
    
    -- Validación contable
    total_debe DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    total_haber DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    cuadrado BOOLEAN GENERATED ALWAYS AS (total_debe = total_haber) STORED COMMENT 'Debe = Haber',
    
    -- Auditoría
    create_by INT,
    create_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by INT,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_fecha_asiento (fecha_asiento DESC),
    INDEX idx_periodo_asiento (periodo),
    INDEX idx_estado_asiento (estado),
    INDEX idx_origen_asiento (origen, id_origen)
) ENGINE=InnoDB COMMENT='Encabezado de asientos contables';
```

#### Tabla: `detalle_asiento`
```sql
CREATE TABLE detalle_asiento (
    id_detalle_asiento INT PRIMARY KEY AUTO_INCREMENT,
    id_asiento INT NOT NULL,
    id_cuenta INT NOT NULL COMMENT 'Cuenta contable afectada',
    
    -- Movimientos
    debe DECIMAL(15, 2) DEFAULT 0.00,
    haber DECIMAL(15, 2) DEFAULT 0.00,
    
    -- Auxiliares (opcional)
    id_cliente INT COMMENT 'Si la cuenta es de clientes',
    id_proveedor INT COMMENT 'Si la cuenta es de proveedores',
    referencia VARCHAR(100) COMMENT 'Referencia adicional',
    
    -- Descripción
    detalle TEXT COMMENT 'Detalle específico de esta línea',
    
    FOREIGN KEY (id_asiento) REFERENCES asiento_contable(id_asiento) ON DELETE CASCADE,
    FOREIGN KEY (id_cuenta) REFERENCES cuenta_contable(id_cuenta),
    FOREIGN KEY (id_cliente) REFERENCES cliente(idCliente),
    
    INDEX idx_detalle_asiento (id_asiento),
    INDEX idx_detalle_cuenta (id_cuenta)
) ENGINE=InnoDB COMMENT='Detalle de movimientos por asiento (debe y haber)';
```

**Plan de Cuentas Inicial (Costa Rica):**
```sql
-- Script de inserción de plan de cuentas básico CR
-- Nivel 1: Cuentas principales
INSERT INTO cuenta_contable (codigo, nombre, tipo_cuenta, naturaleza, nivel, acepta_movimientos) VALUES
('1', 'ACTIVO', 'ACTIVO', 'DEUDORA', 1, FALSE),
('2', 'PASIVO', 'PASIVO', 'ACREEDORA', 1, FALSE),
('3', 'CAPITAL', 'CAPITAL', 'ACREEDORA', 1, FALSE),
('4', 'INGRESOS', 'INGRESO', 'ACREEDORA', 1, FALSE),
('5', 'GASTOS', 'GASTO', 'DEUDORA', 1, FALSE);

-- Nivel 2: Sub-cuentas de ACTIVO
INSERT INTO cuenta_contable (codigo, nombre, tipo_cuenta, naturaleza, nivel, id_cuenta_padre, acepta_movimientos) VALUES
('1.1', 'ACTIVO CIRCULANTE', 'ACTIVO', 'DEUDORA', 2, 1, FALSE),
('1.2', 'ACTIVO FIJO', 'ACTIVO', 'DEUDORA', 2, 1, FALSE);

-- Nivel 3: Cuentas de detalle (las que aceptan movimientos)
INSERT INTO cuenta_contable (codigo, nombre, tipo_cuenta, naturaleza, nivel, id_cuenta_padre, acepta_movimientos, requiere_auxiliar) VALUES
('1.1.01', 'Caja', 'ACTIVO', 'DEUDORA', 3, 2, TRUE, FALSE),
('1.1.02', 'Bancos', 'ACTIVO', 'DEUDORA', 3, 2, TRUE, FALSE),
('1.1.03', 'Cuentas por Cobrar', 'ACTIVO', 'DEUDORA', 3, 2, TRUE, TRUE),
('1.1.04', 'Inventario', 'ACTIVO', 'DEUDORA', 3, 2, TRUE, FALSE),
('2.1.01', 'Cuentas por Pagar', 'PASIVO', 'ACREEDORA', 3, 6, TRUE, TRUE),
('4.1.01', 'Ventas', 'INGRESO', 'ACREEDORA', 3, 9, TRUE, FALSE),
('4.1.02', 'IVA por Pagar (13%)', 'PASIVO', 'ACREEDORA', 3, 6, TRUE, FALSE),
('5.1.01', 'Costo de Ventas', 'GASTO', 'DEUDORA', 3, 10, TRUE, FALSE);
```

**Tareas BD:**
- [ ] 2.1.1 - Crear tabla `cuenta_contable` con jerarquía
- [ ] 2.1.2 - Crear tabla `asiento_contable`
- [ ] 2.1.3 - Crear tabla `detalle_asiento`
- [ ] 2.1.4 - Script de plan de cuentas inicial CR (50+ cuentas)
- [ ] 2.1.5 - Crear trigger para actualizar saldos de cuentas
- [ ] 2.1.6 - Crear trigger para validar cuadre de asiento (debe = haber)
- [ ] 2.1.7 - Crear SP `sp_libro_diario(fecha_inicio, fecha_fin)`
- [ ] 2.1.8 - Crear SP `sp_libro_mayor(id_cuenta, fecha_inicio, fecha_fin)`
- [ ] 2.1.9 - Crear SP `sp_balance_comprobacion(fecha)`
- [ ] 2.1.10 - Crear vista `v_saldos_cuentas` (saldos actualizados)

### 2.2 Backend - Modelos (8 tareas)

- [ ] 2.2.1 - Crear entidad `CuentaContable.java` con jerarquía
- [ ] 2.2.2 - Crear entidad `AsientoContable.java`
- [ ] 2.2.3 - Crear entidad `DetalleAsiento.java`
- [ ] 2.2.4 - Crear enum `TipoCuenta.java` (ACTIVO, PASIVO, etc.)
- [ ] 2.2.5 - Crear enum `Naturaleza.java` (DEUDORA, ACREEDORA)
- [ ] 2.2.6 - Crear enum `TipoAsiento.java`
- [ ] 2.2.7 - Crear DTO `AsientoContableDTO.java`
- [ ] 2.2.8 - Crear DTO `BalanceComprobacionDTO.java`

### 2.3 Backend - Repositories (6 tareas)

- [ ] 2.3.1 - Crear `CuentaContableRepository.java`
- [ ] 2.3.2 - Crear `AsientoContableRepository.java`
- [ ] 2.3.3 - Crear `DetalleAsientoRepository.java`
- [ ] 2.3.4 - Query: `findCuentasByTipo(TipoCuenta tipo)`
- [ ] 2.3.5 - Query: `findAsientosByPeriodo(String periodo)`
- [ ] 2.3.6 - Query: `findCuentasHabilitadasParaMovimiento()`

### 2.4 Backend - Services (12 tareas)

- [ ] 2.4.1 - Crear `ContabilidadService.java` (interfaz)
- [ ] 2.4.2 - Crear `ContabilidadServiceImpl.java`
- [ ] 2.4.3 - Método: `registrarAsiento(AsientoContableDTO)` con validación de cuadre
- [ ] 2.4.4 - Método: `anularAsiento(Integer idAsiento, String motivo)`
- [ ] 2.4.5 - Método: `generarAsientoDesdeFactura(Integer idFactura)` - Automático
- [ ] 2.4.6 - Método: `generarAsientoDesdePago(Integer idPago)` - Automático
- [ ] 2.4.7 - Método: `obtenerLibroDiario(LocalDate inicio, LocalDate fin)`
- [ ] 2.4.8 - Método: `obtenerLibroMayor(Integer idCuenta, LocalDate inicio, LocalDate fin)`
- [ ] 2.4.9 - Método: `generarBalanceComprobacion(LocalDate fecha)`
- [ ] 2.4.10 - Crear `PlanCuentasService.java` para gestionar cuentas
- [ ] 2.4.11 - Integración con `FacturaService` (evento de factura creada)
- [ ] 2.4.12 - Integración con `PagoService` (evento de pago registrado)

### 2.5 Backend - Controllers (5 tareas)

- [ ] 2.5.1 - Crear `ContabilidadController.java` (vistas)
- [ ] 2.5.2 - Crear `ContabilidadRestController.java` (API)
- [ ] 2.5.3 - Endpoint: `POST /api/asientos` - Registrar asiento manual
- [ ] 2.5.4 - Endpoint: `GET /api/reportes/libro-diario` - Libro diario
- [ ] 2.5.5 - Endpoint: `GET /api/reportes/balance-comprobacion` - Balance

### 2.6 Frontend - Vistas (8 tareas)

- [ ] 2.6.1 - Crear `templates/modules/contabilidad/plan-cuentas.html`
- [ ] 2.6.2 - Crear `templates/modules/contabilidad/asientos/lista.html`
- [ ] 2.6.3 - Crear `templates/modules/contabilidad/asientos/registrar.html`
- [ ] 2.6.4 - Crear `templates/modules/contabilidad/reportes/libro-diario.html`
- [ ] 2.6.5 - Crear `templates/modules/contabilidad/reportes/libro-mayor.html`
- [ ] 2.6.6 - Crear `templates/modules/contabilidad/reportes/balance-comprobacion.html`
- [ ] 2.6.7 - Crear modal de consulta rápida de cuenta
- [ ] 2.6.8 - Dashboard de contabilidad (KPIs financieros)

### 2.7 Frontend - JavaScript (4 tareas)

- [ ] 2.7.1 - Crear `static/modules/contabilidad/js/asientos.js`
- [ ] 2.7.2 - Implementar validación de cuadre debe=haber en tiempo real
- [ ] 2.7.3 - Autocompletar cuentas contables con búsqueda
- [ ] 2.7.4 - Generador de asientos desde plantillas

### 2.8 Testing (5 tareas)

- [ ] 2.8.1 - Tests unitarios `ContabilidadServiceTest`
- [ ] 2.8.2 - Test: Asiento manual cuadrado
- [ ] 2.8.3 - Test: Asiento automático desde factura
- [ ] 2.8.4 - Test: Asiento automático desde pago
- [ ] 2.8.5 - Test: Validación debe ≠ haber (debe fallar)

---

## 🇨🇷 FASE 3: FACTURACIÓN ELECTRÓNICA COSTA RICA

**Duración:** 6-8 días  
**Prioridad:** ⭐⭐⭐ CRÍTICA  
**Progreso estimado:** 0/52 tareas

### Objetivos

- Integración con Hacienda CR (API v4.4)
- Generación de XML según anexo v4.4
- Firma digital XAdES-EPES
- Envío a Hacienda (RECEPCION API)
- Consulta de estado y callbacks
- Almacenamiento de comprobantes y respuestas

### 3.1 Base de Datos (8 tareas)

#### Actualizar tabla `factura`
```sql
-- Agregar campos para facturación electrónica CR
ALTER TABLE factura ADD COLUMN (
    -- Consecutivo Hacienda (20 dígitos)
    consecutivo_hacienda VARCHAR(20) UNIQUE COMMENT 'SSS-TTTTT-TT-NNNNNNNNNN',
    
    -- Clave numérica (50 dígitos)
    clave_hacienda VARCHAR(50) UNIQUE COMMENT 'Clave única de 50 dígitos',
    
    -- Datos adicionales para FE
    condicion_venta VARCHAR(2) DEFAULT '01' COMMENT '01=Contado, 02=Crédito, etc.',
    plazo_credito INT COMMENT 'Días de crédito si aplica',
    medio_pago VARCHAR(2) DEFAULT '01' COMMENT 'Código catálogo Hacienda',
    
    -- Estado Hacienda
    estado_hacienda ENUM(
        'BORRADOR', 
        'FIRMADO', 
        'ENVIADO', 
        'RECIBIDO', 
        'ACEPTADO', 
        'RECHAZADO', 
        'ERROR'
    ) DEFAULT 'BORRADOR',
    
    fecha_envio_hacienda DATETIME COMMENT 'Cuándo se envió a Hacienda',
    fecha_respuesta_hacienda DATETIME COMMENT 'Cuándo respondió Hacienda',
    
    -- Almacenamiento de XMLs
    xml_sin_firma LONGTEXT COMMENT 'XML generado antes de firmar',
    xml_firmado LONGTEXT COMMENT 'XML firmado (el que se envía)',
    xml_respuesta LONGTEXT COMMENT 'Respuesta de Hacienda (base64)',
    
    -- Trazabilidad
    location_hacienda VARCHAR(255) COMMENT 'URL para consultar estado',
    codigo_error VARCHAR(10) COMMENT 'Código de error de Hacienda si aplica',
    mensaje_error TEXT COMMENT 'Mensaje de error detallado',
    
    -- Reintentos
    intentos_envio INT DEFAULT 0,
    ultimo_intento DATETIME
);

-- Índices para búsquedas
CREATE INDEX idx_factura_clave_hacienda ON factura(clave_hacienda);
CREATE INDEX idx_factura_estado_hacienda ON factura(estado_hacienda);
CREATE INDEX idx_factura_consecutivo_hacienda ON factura(consecutivo_hacienda);
```

#### Actualizar tabla `linea_factura`
```sql
ALTER TABLE linea_factura ADD COLUMN (
    -- CABYS (13 dígitos obligatorio v4.4)
    cabys VARCHAR(13) COMMENT 'Código de bienes y servicios',
    
    -- Unidad de medida (catálogo Hacienda)
    unidad_medida VARCHAR(5) DEFAULT 'Unid' COMMENT 'Sp, Kg, m, etc.',
    
    -- Impuestos detallados
    codigo_impuesto VARCHAR(2) DEFAULT '01' COMMENT '01=IVA, 02=Selectivo, etc.',
    codigo_tarifa VARCHAR(2) DEFAULT '08' COMMENT '08=13%, 01=0%, etc.',
    tarifa_impuesto DECIMAL(5, 2) DEFAULT 13.00 COMMENT 'Porcentaje de impuesto',
    monto_impuesto DECIMAL(10, 2) COMMENT 'Monto calculado del impuesto',
    
    -- Naturaleza de la línea
    naturaleza_descuento VARCHAR(100) COMMENT 'Razón del descuento si aplica'
);
```

#### Tabla: `configuracion_hacienda`
```sql
CREATE TABLE configuracion_hacienda (
    id_configuracion INT PRIMARY KEY AUTO_INCREMENT,
    
    -- Configuración única (Singleton)
    ambiente ENUM('SANDBOX', 'PRODUCCION') DEFAULT 'SANDBOX',
    
    -- URLs
    url_token VARCHAR(255) NOT NULL COMMENT 'URL del IdP para obtener token',
    url_recepcion VARCHAR(255) NOT NULL COMMENT 'URL de recepción de comprobantes',
    
    -- Credenciales (encriptadas)
    client_id VARCHAR(100) DEFAULT 'api-prod',
    username VARCHAR(100) COMMENT 'Usuario de ATV',
    password_encrypted VARCHAR(255) COMMENT 'Contraseña ATV encriptada',
    
    -- Certificado de firma
    ruta_certificado VARCHAR(255) COMMENT 'Ruta al archivo .p12',
    password_certificado_encrypted VARCHAR(255),
    
    -- Configuración de emisor
    tipo_identificacion_emisor VARCHAR(2) DEFAULT '02' COMMENT '01=Física, 02=Jurídica',
    numero_identificacion_emisor VARCHAR(12) NOT NULL COMMENT 'Cédula padded a 12',
    nombre_comercial_emisor VARCHAR(80),
    
    -- Consecutivos
    sucursal VARCHAR(3) DEFAULT '001' COMMENT 'Código de sucursal (3 dígitos)',
    terminal VARCHAR(5) DEFAULT '00001' COMMENT 'Código de terminal (5 dígitos)',
    consecutivo_actual INT DEFAULT 0 COMMENT 'Último consecutivo utilizado',
    
    -- Control
    activo BOOLEAN DEFAULT TRUE,
    modo_contingencia BOOLEAN DEFAULT FALSE COMMENT 'Activar si no hay conexión con Hacienda',
    
    -- Callback
    url_callback VARCHAR(255) COMMENT 'URL donde Hacienda enviará respuestas',
    
    -- Auditoría
    update_by INT,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='Configuración para integración con Hacienda CR';

-- Datos iniciales (SANDBOX)
INSERT INTO configuracion_hacienda (
    ambiente, 
    url_token, 
    url_recepcion,
    tipo_identificacion_emisor,
    numero_identificacion_emisor,
    nombre_comercial_emisor
) VALUES (
    'SANDBOX',
    'https://idp.comprobanteselectronicos.go.cr/auth/realms/rut-stag/protocol/openid-connect/token',
    'https://api.comprobanteselectronicos.go.cr/recepcion-sandbox/v1/recepcion',
    '02',
    '000000000000',
    'EMPRESA DEMO CR'
);
```

**Tareas BD:**
- [ ] 3.1.1 - Migración: Alterar `factura` con campos FE
- [ ] 3.1.2 - Migración: Alterar `linea_factura` con CABYS e impuestos
- [ ] 3.1.3 - Crear tabla `configuracion_hacienda` con datos SANDBOX
- [ ] 3.1.4 - Crear tabla `log_envio_hacienda` (auditoría completa)
- [ ] 3.1.5 - Crear función SQL para generar consecutivo (20 dígitos)
- [ ] 3.1.6 - Crear función SQL para generar clave (50 dígitos)
- [ ] 3.1.7 - Crear trigger para auto-generar consecutivo y clave
- [ ] 3.1.8 - Script de rollback completo

### 3.2 Backend - Modelos (6 tareas)

- [ ] 3.2.1 - Crear entidad `ConfiguracionHacienda.java`
- [ ] 3.2.2 - Crear DTO `FacturaElectronicaDTO.java` (para XML)
- [ ] 3.2.3 - Crear enum `EstadoHacienda.java`
- [ ] 3.2.4 - Crear enum `CondicionVenta.java` (catálogo CR)
- [ ] 3.2.5 - Crear enum `MedioPago.java` (catálogo CR)
- [ ] 3.2.6 - Actualizar `Factura.java` con nuevos campos

### 3.3 Backend - Generación de XML (8 tareas)

- [ ] 3.3.1 - Crear `XmlGeneratorService.java` para construir XML v4.4
- [ ] 3.3.2 - Implementar método `generarXmlFactura(Factura factura)`
- [ ] 3.3.3 - Agregar dependencia JAXB para marshalling XML
- [ ] 3.3.4 - Crear clases JAXB según schema v4.4 (o usar JAXBContext manual)
- [ ] 3.3.5 - Implementar validación contra XSD v4.4
- [ ] 3.3.6 - Calcular campos automáticos (resumen impuestos, totales)
- [ ] 3.3.7 - Generar estructura de Emisor desde `ConfiguracionEmpresa`
- [ ] 3.3.8 - Generar estructura de Receptor desde `Cliente`

### 3.4 Backend - Firma Digital (5 tareas)

- [ ] 3.4.1 - Crear `FirmaDigitalService.java` para XAdES-EPES
- [ ] 3.4.2 - Agregar dependencia `xmlsec` para firma XML
- [ ] 3.4.3 - Implementar método `firmarXml(String xml)` → XML firmado
- [ ] 3.4.4 - Cargar certificado .p12 desde configuración
- [ ] 3.4.5 - Validar certificado (fecha de expiración, cadena de confianza)

### 3.5 Backend - Integración API Hacienda (10 tareas)

- [ ] 3.5.1 - Crear `HaciendaApiService.java` para comunicación HTTP
- [ ] 3.5.2 - Implementar `TokenManager` (cachea token 5 min, renueva antes de expirar)
- [ ] 3.5.3 - Método: `obtenerToken()` usando OAuth2 Resource Owner Password
- [ ] 3.5.4 - Configurar `WebClient` con interceptores (token, UTF-8, logging)
- [ ] 3.5.5 - Método: `enviarComprobante(String clave, String xmlBase64)` → POST /recepcion
- [ ] 3.5.6 - Método: `consultarEstado(String clave)` → GET /recepcion/{clave}
- [ ] 3.5.7 - Implementar callback endpoint para recibir respuesta de Hacienda
- [ ] 3.5.8 - Manejo de errores HTTP (401, 429 rate limit, 500)
- [ ] 3.5.9 - Circuit breaker con Resilience4j (si Hacienda cae)
- [ ] 3.5.10 - Implementar cola de reintentos con exponential backoff

### 3.6 Backend - Workflow Completo (8 tareas)

- [ ] 3.6.1 - Crear `FacturaElectronicaService.java` (orquestador)
- [ ] 3.6.2 - Método: `firmarYEnviarFactura(Integer idFactura)`
  1. Generar consecutivo y clave
  2. Generar XML v4.4
  3. Firmar XML
  4. Enviar a Hacienda
  5. Guardar XMLs y respuesta
  6. Actualizar estado factura
- [ ] 3.6.3 - Método: `consultarYActualizarEstado(Integer idFactura)`
- [ ] 3.6.4 - Método: `reprocesarFacturasRechazadas()`
- [ ] 3.6.5 - Scheduled task para consultar estados pendientes
- [ ] 3.6.6 - Integración con `NotificacionService` (factura aceptada/rechazada)
- [ ] 3.6.7 - Generar PDF de factura con código QR (clave Hacienda)
- [ ] 3.6.8 - Enviar email al cliente con XML + PDF adjuntos

### 3.7 Frontend - Vistas (4 tareas)

- [ ] 3.7.1 - Crear `templates/modules/facturacion/configuracion-hacienda.html`
- [ ] 3.7.2 - Actualizar `facturacion/detalle.html` con badge de estado Hacienda
- [ ] 3.7.3 - Botón "Firmar y Enviar a Hacienda" en detalle de factura
- [ ] 3.7.4 - Modal de historial de envíos y reintentos

### 3.8 Testing (3 tareas)

- [ ] 3.8.1 - Tests unitarios `XmlGeneratorServiceTest`
- [ ] 3.8.2 - Test de generación de XML válido contra XSD
- [ ] 3.8.3 - Integration test con SANDBOX de Hacienda

---

## 🧪 FASE 4: TESTING AUTOMATIZADO

**Duración:** 4-5 días  
**Prioridad:** ⭐⭐ ALTA  
**Progreso estimado:** 0/32 tareas

### Objetivos

- Implementar testing automatizado completo
- Cobertura mínima del 80%
- Tests unitarios con JUnit 5 + Mockito
- Tests de integración con TestContainers
- CI/CD básico con GitHub Actions

### 4.1 Configuración de Testing (6 tareas)

- [ ] 4.1.1 - Actualizar `pom.xml` con dependencias de testing
  - JUnit 5 (Jupiter)
  - Mockito
  - AssertJ
  - TestContainers (MySQL)
  - Spring Boot Test
  - H2 para tests
- [ ] 4.1.2 - Crear `application-test.yml` para perfil de testing
- [ ] 4.1.3 - Configurar TestContainers para MySQL
- [ ] 4.1.4 - Crear clase base `BaseIntegrationTest`
- [ ] 4.1.5 - Configurar Jacoco para cobertura de código
- [ ] 4.1.6 - Configurar Maven Surefire para ejecución de tests

### 4.2 Tests Unitarios - Pagos (5 tareas)

- [ ] 4.2.1 - `PagoServiceTest` - Registrar pago válido
- [ ] 4.2.2 - `PagoServiceTest` - Validar monto mayor a saldo (debe fallar)
- [ ] 4.2.3 - `PagoServiceTest` - Anular pago y verificar reversión de saldo
- [ ] 4.2.4 - `PagoServiceTest` - Múltiples pagos parciales
- [ ] 4.2.5 - `EstadoCuentaServiceTest` - Cálculo de saldo correcto

### 4.3 Tests Unitarios - Contabilidad (6 tareas)

- [ ] 4.3.1 - `ContabilidadServiceTest` - Asiento cuadrado (debe = haber)
- [ ] 4.3.2 - `ContabilidadServiceTest` - Asiento descuadrado (debe fallar)
- [ ] 4.3.3 - `ContabilidadServiceTest` - Asiento desde factura automático
- [ ] 4.3.4 - `ContabilidadServiceTest` - Asiento desde pago automático
- [ ] 4.3.5 - `ContabilidadServiceTest` - Anular asiento
- [ ] 4.3.6 - `CuentaContableServiceTest` - Validar jerarquía de cuentas

### 4.4 Tests Unitarios - Facturación Electrónica (5 tareas)

- [ ] 4.4.1 - `XmlGeneratorServiceTest` - Generar XML válido
- [ ] 4.4.2 - `XmlGeneratorServiceTest` - Validar contra XSD v4.4
- [ ] 4.4.3 - `FirmaDigitalServiceTest` - Firmar XML (mock de certificado)
- [ ] 4.4.4 - `HaciendaApiServiceTest` - Obtener token (mock)
- [ ] 4.4.5 - `FacturaElectronicaServiceTest` - Workflow completo (mocks)

### 4.5 Tests de Integración (6 tareas)

- [ ] 4.5.1 - `PagoIntegrationTest` - CRUD completo con DB real (TestContainers)
- [ ] 4.5.2 - `ContabilidadIntegrationTest` - Asientos con DB real
- [ ] 4.5.3 - `FacturaElectronicaIntegrationTest` - Envío a SANDBOX Hacienda
- [ ] 4.5.4 - `ReporteIntegrationTest` - Generación de reportes
- [ ] 4.5.5 - `NotificacionIntegrationTest` - Envío de notificaciones
- [ ] 4.5.6 - Test E2E: Factura → Pago → Asiento → Hacienda

### 4.6 CI/CD con GitHub Actions (4 tareas)

- [ ] 4.6.1 - Crear `.github/workflows/maven.yml`
- [ ] 4.6.2 - Configurar pipeline: build → test → coverage
- [ ] 4.6.3 - Integrar Jacoco report en GitHub Actions
- [ ] 4.6.4 - Badge de coverage en README.md

---

## 📚 FASE 5: DOCUMENTACIÓN Y MANUALES

**Duración:** 2-3 días  
**Prioridad:** ⭐⭐ MEDIA  
**Progreso estimado:** 0/12 tareas

### 5.1 Documentación Técnica (6 tareas)

- [ ] 5.1.1 - `SPRINT_5_FASE_1_PAGOS.md`
- [ ] 5.1.2 - `SPRINT_5_FASE_2_CONTABILIDAD.md`
- [ ] 5.1.3 - `SPRINT_5_FASE_3_FACTURACION_ELECTRONICA_CR.md`
- [ ] 5.1.4 - `SPRINT_5_FASE_4_TESTING.md`
- [ ] 5.1.5 - `SPRINT_5_RESUMEN_FINAL.md`
- [ ] 5.1.6 - Actualizar `ESTADO_PROYECTO.md`

### 5.2 Manuales de Usuario (6 tareas)

- [ ] 5.2.1 - `MANUAL_PAGOS.md` (500+ líneas)
- [ ] 5.2.2 - `MANUAL_CONTABILIDAD.md` (800+ líneas)
- [ ] 5.2.3 - `MANUAL_FACTURACION_ELECTRONICA_CR.md` (1,000+ líneas)
- [ ] 5.2.4 - `GUIA_CONFIGURACION_HACIENDA.md`
- [ ] 5.2.5 - `GUIA_OBTENCION_CERTIFICADO_FIRMA.md`
- [ ] 5.2.6 - `FAQ_FACTURACION_ELECTRONICA.md`

---

## 📊 RESUMEN EJECUTIVO DEL SPRINT 5

### Estadísticas del Sprint

```
┌─────────────────────────────────────────────────────────────┐
│                  SPRINT 5 - MÉTRICAS                         │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Total de tareas:                     199 tareas            │
│  Duración estimada:                   24-32 días            │
│  Velocidad requerida:                 6-8 tareas/día        │
│                                                              │
│  Nuevas tablas BD:                    8 tablas              │
│  Entidades Java:                      12 entidades          │
│  Services:                            8 servicios           │
│  Controllers:                         6 controllers         │
│  Templates:                           15 vistas             │
│                                                              │
│  Tests unitarios:                     30+ tests             │
│  Tests integración:                   6 tests               │
│  Cobertura objetivo:                  80%+                  │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Valor de Negocio

✅ **Sistema completo de pagos** - Control financiero total  
✅ **Contabilidad de doble partida** - Cumplimiento contable  
✅ **Facturación electrónica CR** - Cumplimiento fiscal  
✅ **Testing automatizado** - Calidad asegurada  
✅ **Listo para producción** - Sistema robusto y confiable  

### Prioridades por Fase

| Fase | Prioridad | Bloqueante | Puede posponerse |
|------|-----------|------------|------------------|
| Fase 1: Pagos | ⭐⭐⭐ | SÍ | NO |
| Fase 2: Contabilidad | ⭐⭐⭐ | SÍ | Parcialmente* |
| Fase 3: FE Costa Rica | ⭐⭐⭐ | NO** | SÍ (Sprint 6) |
| Fase 4: Testing | ⭐⭐ | NO | SÍ (Sprint 6) |
| Fase 5: Documentación | ⭐⭐ | NO | SÍ (Sprint 6) |

\* La contabilidad básica es crítica, pero reportes avanzados pueden posponerse  
\** Solo bloqueante si se requiere facturar en Costa Rica inmediatamente

---

## 🎯 RECOMENDACIONES ESTRATÉGICAS

### Opción A: Sprint 5 Completo (Recomendado)
**Duración:** 24-32 días  
**Enfoque:** Implementar todas las fases  
**Ventaja:** Sistema completo y listo para CR  
**Riesgo:** Sprint largo, posible burnout  

### Opción B: Sprint 5 Dividido (Conservador)
**Sprint 5A:** Pagos + Contabilidad (14-18 días)  
**Sprint 5B:** FE Costa Rica + Testing (10-14 días)  
**Ventaja:** Sprints más manejables  
**Riesgo:** Mayor tiempo total por contexto switching  

### Opción C: Sprint 5 MVP (Rápido)
**Duración:** 16-20 días  
**Enfoque:** Solo Fase 1 + Fase 2 + Testing básico  
**Ventaja:** Entrega rápida de valor  
**Riesgo:** FE Costa Rica queda pendiente  

---

## 📝 PRÓXIMOS PASOS

1. **Revisar y aprobar** esta propuesta
2. **Decidir opción estratégica** (A, B o C)
3. **Obtener certificado de firma** (si se elige FE Costa Rica)
4. **Configurar credenciales ATV** (si se elige FE Costa Rica)
5. **Crear branch** `feature/sprint-5-contabilidad-fe-cr`
6. **Iniciar Fase 1: Módulo de Pagos**

---

## 🔗 REFERENCIAS

- [Documentación Hacienda CR - v4.4](https://www.hacienda.go.cr/contenido/14509-factura-electronica)
- [Anexo de Estructuras v4.4](https://tribunet.hacienda.go.cr/docs/esquemas/2017/v4.4/)
- [API Comprobantes Electrónicos](https://api.comprobanteselectronicos.go.cr/docs/)
- [Guía BCCR - Certificados Digitales](https://www.bccr.fi.cr/firmadigital/)
- [Archivo de referencia local](../referencias/facturacion%20cr.txt)

---

**Documento creado:** 16 de enero de 2026  
**Creado por:** GitHub Copilot  
**Estado:** 📋 PROPUESTA PARA APROBACIÓN  

---

¿Deseas proceder con alguna de las opciones propuestas?
