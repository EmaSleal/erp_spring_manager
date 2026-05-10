-- =====================================================
-- MIGRACIÓN: MÓDULO DE PAGOS
-- Sistema: ERP Orders Manager
-- Fecha: 18 de enero de 2026
-- Sprint: 5 - Fase 1
-- Descripción: Creación de tabla de pagos con integración a facturación y contabilidad
-- =====================================================

-- =====================================================
-- TABLA: pagos
-- =====================================================
-- Gestiona los pagos de clientes aplicados a facturas
-- Integración automática con contabilidad (asientos contables)
-- Soporte para múltiples métodos de pago según catálogo de Hacienda CR
-- =====================================================

CREATE TABLE IF NOT EXISTS pagos (
    -- Identificación
    id_pago BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero_pago VARCHAR(20) NOT NULL UNIQUE COMMENT 'Número consecutivo: PAG-YYYYMMDD-0001',
    
    -- Relaciones
    cliente_id BIGINT NOT NULL COMMENT 'Cliente que realiza el pago',
    factura_id INT NULL COMMENT 'Factura a la que se aplica (NULL para adelantos)',
    
    -- Datos del Pago
    monto DECIMAL(12,2) NOT NULL COMMENT 'Monto del pago',
    fecha_pago DATE NOT NULL COMMENT 'Fecha en que se recibe el pago',
    metodo_pago VARCHAR(30) NOT NULL COMMENT 'EFECTIVO, TARJETA, CHEQUE, TRANSFERENCIA_DEPOSITO, RECAUDADO_TERCEROS, OTROS',
    tipo_pago VARCHAR(20) NOT NULL COMMENT 'TOTAL, PARCIAL, ADELANTO, NOTA_CREDITO',
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE' COMMENT 'PENDIENTE, CONFIRMADO, RECHAZADO, ANULADO, CONCILIADO',
    
    -- Referencia bancaria/documentos
    referencia_bancaria VARCHAR(100) NULL COMMENT 'Número de cheque, referencia de transferencia, etc.',
    banco VARCHAR(100) NULL COMMENT 'Nombre del banco',
    cuenta_bancaria VARCHAR(50) NULL COMMENT 'Últimos dígitos de cuenta',
    
    -- Notas y observaciones
    observaciones TEXT NULL COMMENT 'Observaciones adicionales del pago',
    comprobante_url VARCHAR(255) NULL COMMENT 'URL del comprobante digitalizado',
    
    -- Auditoría: Creación
    creado_por VARCHAR(100) NOT NULL COMMENT 'Usuario que registra el pago',
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de registro',
    
    -- Auditoría: Modificación
    modificado_por VARCHAR(100) NULL COMMENT 'Usuario que modifica',
    modificado_en TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha de última modificación',
    
    -- Auditoría: Anulación
    anulado_por VARCHAR(100) NULL COMMENT 'Usuario que anula el pago',
    anulado_en TIMESTAMP NULL COMMENT 'Fecha de anulación',
    motivo_anulacion TEXT NULL COMMENT 'Motivo de la anulación',
    
    -- Claves foráneas
    CONSTRAINT fk_pago_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id_cliente) ON DELETE RESTRICT,
    CONSTRAINT fk_pago_factura FOREIGN KEY (factura_id) REFERENCES factura(id_factura) ON DELETE RESTRICT,
    
    -- Validaciones
    CONSTRAINT chk_pago_monto_positivo CHECK (monto > 0),
    CONSTRAINT chk_pago_metodo_valido CHECK (metodo_pago IN (
        'EFECTIVO', 'TARJETA', 'CHEQUE', 'TRANSFERENCIA_DEPOSITO', 'RECAUDADO_TERCEROS', 'OTROS'
    )),
    CONSTRAINT chk_pago_tipo_valido CHECK (tipo_pago IN (
        'TOTAL', 'PARCIAL', 'ADELANTO', 'NOTA_CREDITO'
    )),
    CONSTRAINT chk_pago_estado_valido CHECK (estado IN (
        'PENDIENTE', 'CONFIRMADO', 'RECHAZADO', 'ANULADO', 'CONCILIADO'
    ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Gestión de pagos de clientes con integración contable';

-- =====================================================
-- ÍNDICES PARA OPTIMIZACIÓN
-- =====================================================

-- Búsqueda por número de pago (único)
CREATE UNIQUE INDEX idx_pago_numero ON pagos(numero_pago);

-- Búsqueda por cliente
CREATE INDEX idx_pago_cliente ON pagos(cliente_id);

-- Búsqueda por factura
CREATE INDEX idx_pago_factura ON pagos(factura_id);

-- Búsqueda por fecha
CREATE INDEX idx_pago_fecha ON pagos(fecha_pago);

-- Búsqueda por estado
CREATE INDEX idx_pago_estado ON pagos(estado);

-- Búsqueda por método de pago
CREATE INDEX idx_pago_metodo ON pagos(metodo_pago);

-- Búsqueda combinada: cliente + estado (para adelantos disponibles)
CREATE INDEX idx_pago_cliente_estado ON pagos(cliente_id, estado);

-- Búsqueda combinada: factura + estado (para saldos)
CREATE INDEX idx_pago_factura_estado ON pagos(factura_id, estado);

-- =====================================================
-- DATOS INICIALES (OPCIONAL)
-- =====================================================

-- Ejemplo de pago en efectivo aplicado a factura
-- INSERT INTO pagos (
--     numero_pago, cliente_id, factura_id, monto, fecha_pago, 
--     metodo_pago, tipo_pago, estado, creado_por
-- ) VALUES (
--     'PAG-20260118-0001', 1, 1, 50000.00, '2026-01-18',
--     'EFECTIVO', 'TOTAL', 'CONFIRMADO', 'ADMIN'
-- );

-- =====================================================
-- VALIDACIONES POST-MIGRACIÓN
-- =====================================================

-- Verificar estructura de la tabla
-- DESCRIBE pagos;

-- Verificar índices creados
-- SHOW INDEXES FROM pagos;

-- Contar registros
-- SELECT COUNT(*) as total_pagos FROM pagos;

-- =====================================================
-- CONSULTAS DE PRUEBA ÚTILES
-- =====================================================

-- Pagos pendientes de confirmar
-- SELECT * FROM pagos WHERE estado = 'PENDIENTE' ORDER BY fecha_pago DESC;

-- Total pagado por cliente
-- SELECT cliente_id, SUM(monto) as total_pagado
-- FROM pagos
-- WHERE estado = 'CONFIRMADO'
-- GROUP BY cliente_id;

-- Total pagado por factura
-- SELECT factura_id, SUM(monto) as total_pagado
-- FROM pagos
-- WHERE factura_id IS NOT NULL AND estado = 'CONFIRMADO'
-- GROUP BY factura_id;

-- Adelantos disponibles por cliente
-- SELECT cliente_id, SUM(monto) as adelantos_disponibles
-- FROM pagos
-- WHERE tipo_pago = 'ADELANTO' AND estado = 'CONFIRMADO' AND factura_id IS NULL
-- GROUP BY cliente_id;

-- Pagos por método en un período
-- SELECT metodo_pago, COUNT(*) as cantidad, SUM(monto) as total
-- FROM pagos
-- WHERE fecha_pago BETWEEN '2026-01-01' AND '2026-01-31'
-- AND estado = 'CONFIRMADO'
-- GROUP BY metodo_pago;

-- =====================================================
-- NOTAS IMPORTANTES
-- =====================================================
-- 1. El campo factura_id es NULL para adelantos (pagos sin factura asignada)
-- 2. Los métodos de pago siguen el catálogo de Hacienda CR (Anexo 4.4)
-- 3. Al confirmar un pago, se genera automáticamente un asiento contable
-- 4. Los pagos anulados no se eliminan, solo cambian de estado
-- 5. El estado CONCILIADO indica que el pago fue validado contra extracto bancario
-- 6. La referencia bancaria es obligatoria para CHEQUE, TRANSFERENCIA_DEPOSITO
-- 7. El número de pago es único y se genera automáticamente: PAG-YYYYMMDD-0001
-- =====================================================

-- FIN DE LA MIGRACIÓN
