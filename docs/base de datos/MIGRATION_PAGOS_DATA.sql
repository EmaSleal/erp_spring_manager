-- ==========================================================================
-- MIGRACIÓN DE DATOS EXISTENTES EN TABLA PAGOS
-- ==========================================================================
-- Descripción: Pobla los campos nuevos (cliente_id, numero_pago, tipo_pago)
--              para los registros existentes antes de agregar constraints
-- Autor: Sistema
-- Fecha: 2026-01-19
-- ==========================================================================

USE facturas_monrachem;

-- Paso 1: Poblar cliente_id desde la factura relacionada
UPDATE pagos p
INNER JOIN factura f ON p.id_factura = f.id_factura
SET p.cliente_id = f.idCliente
WHERE p.cliente_id IS NULL OR p.cliente_id = 0;

-- Verificar que todos los pagos tengan cliente
SELECT COUNT(*) AS pagos_sin_cliente 
FROM pagos 
WHERE cliente_id IS NULL OR cliente_id = 0;

-- Paso 2: Generar numero_pago para registros existentes
-- Formato: PAG-YYYYMMDD-NNNN (basado en fecha_pago)
SET @counter = 0;

UPDATE pagos 
SET numero_pago = CONCAT(
    'PAG-', 
    DATE_FORMAT(fecha_pago, '%Y%m%d'), 
    '-',
    LPAD(@counter := @counter + 1, 4, '0')
)
WHERE numero_pago IS NULL OR numero_pago = ''
ORDER BY fecha_pago, id_pago;

-- Verificar que todos tengan número
SELECT COUNT(*) AS pagos_sin_numero 
FROM pagos 
WHERE numero_pago IS NULL OR numero_pago = '';

-- Paso 3: Establecer tipo_pago por defecto (TOTAL)
UPDATE pagos 
SET tipo_pago = 'TOTAL' 
WHERE tipo_pago IS NULL;

-- Verificar que todos tengan tipo
SELECT COUNT(*) AS pagos_sin_tipo 
FROM pagos 
WHERE tipo_pago IS NULL;

-- Paso 4: Copiar campos renombrados (si hay datos)
-- referencia → referencia_bancaria (solo si referencia tiene valor)
UPDATE pagos 
SET referencia_bancaria = referencia 
WHERE referencia IS NOT NULL 
  AND referencia != ''
  AND (referencia_bancaria IS NULL OR referencia_bancaria = '');

-- notas → observaciones (solo si notas tiene valor)
UPDATE pagos 
SET observaciones = notas 
WHERE notas IS NOT NULL 
  AND notas != ''
  AND (observaciones IS NULL OR observaciones = '');

-- ==========================================================================
-- VERIFICACIÓN FINAL
-- ==========================================================================

SELECT 
    'Datos migrados correctamente' AS status,
    COUNT(*) AS total_pagos,
    SUM(CASE WHEN cliente_id IS NOT NULL AND cliente_id != 0 THEN 1 ELSE 0 END) AS con_cliente,
    SUM(CASE WHEN numero_pago IS NOT NULL AND numero_pago != '' THEN 1 ELSE 0 END) AS con_numero,
    SUM(CASE WHEN tipo_pago IS NOT NULL THEN 1 ELSE 0 END) AS con_tipo
FROM pagos;

-- Mostrar los pagos actualizados
SELECT 
    id_pago,
    numero_pago,
    fecha_pago,
    cliente_id,
    tipo_pago,
    monto,
    estado
FROM pagos
ORDER BY fecha_pago DESC;

-- ==========================================================================
-- PASO 5: AGREGAR FOREIGN KEY CONSTRAINT (ejecutar después de verificar)
-- ==========================================================================

-- Ahora sí se puede agregar la FK constraint sin errores
ALTER TABLE pagos 
ADD CONSTRAINT fk_pago_cliente 
FOREIGN KEY (cliente_id) REFERENCES cliente(id_cliente)
ON DELETE RESTRICT 
ON UPDATE CASCADE;

-- Agregar índice único para numero_pago
ALTER TABLE pagos 
ADD UNIQUE INDEX uk_numero_pago (numero_pago);

-- Verificar constraints
SELECT 
    CONSTRAINT_NAME,
    TABLE_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'facturas_monrachem'
  AND TABLE_NAME = 'pagos'
  AND REFERENCED_TABLE_NAME IS NOT NULL;

-- Verificar índices
SHOW INDEXES FROM pagos;

-- ==========================================================================
-- FIN DE MIGRACIÓN
-- ==========================================================================
