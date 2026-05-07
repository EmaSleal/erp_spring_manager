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

