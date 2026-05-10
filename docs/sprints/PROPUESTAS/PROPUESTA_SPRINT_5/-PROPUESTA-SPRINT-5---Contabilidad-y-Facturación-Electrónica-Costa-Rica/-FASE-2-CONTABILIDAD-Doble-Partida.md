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

