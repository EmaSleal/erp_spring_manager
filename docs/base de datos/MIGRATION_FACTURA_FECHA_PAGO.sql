-- ============================================================================
-- MIGRATION: Eliminar campo 'fecha' y agregar 'fechaPago' en tabla factura
-- Fecha: 13 de octubre de 2025
-- Sprint: Sprint 2 - Fase 5 (Notificaciones)
-- Punto: 5.3.3 (Recordatorio de pago)
-- ============================================================================

-- IMPORTANTE: Los stored procedures que usan el campo 'fecha' ya fueron modificados manualmente
-- Esta migración debe ejecutarse DESPUÉS de actualizar los SPs

USE whats_orders_manager;

-- ============================================================================
-- PASO 1: Agregar el nuevo campo fechaPago
-- ============================================================================

ALTER TABLE factura
ADD COLUMN fecha_pago DATE NULL COMMENT 'Fecha límite de pago de la factura'
AFTER fecha_entrega;

-- ============================================================================
-- PASO 2: Migrar datos existentes (OPCIONAL)
-- ============================================================================
-- Si quieres calcular la fecha de pago basándote en la fecha de entrega + 7 días:

UPDATE factura
SET fecha_pago = DATE_ADD(fecha_entrega, INTERVAL 7 DAY)
WHERE fecha_entrega IS NOT NULL
  AND fecha_pago IS NULL;

-- Para facturas sin fecha de entrega, usar createDate + 7 días:
UPDATE factura
SET fecha_pago = DATE_ADD(DATE(create_date), INTERVAL 7 DAY)
WHERE fecha_entrega IS NULL
  AND fecha_pago IS NULL
  AND create_date IS NOT NULL;

-- ============================================================================
-- PASO 3: Eliminar el campo 'fecha' antiguo
-- ============================================================================

ALTER TABLE factura
DROP COLUMN fecha;

-- ============================================================================
-- PASO 4: Verificación
-- ============================================================================

-- Verificar la estructura de la tabla:
DESCRIBE factura;

-- Contar facturas con fecha_pago NULL (deberían ser pocas o ninguna):
SELECT COUNT(*) AS facturas_sin_fecha_pago
FROM factura
WHERE fecha_pago IS NULL;

-- Ver algunos ejemplos de facturas con la nueva estructura:
SELECT 
    id_factura,
    numero_factura,
    fecha_entrega,
    fecha_pago,
    tipo_factura,
    total,
    create_date
FROM factura
ORDER BY id_factura DESC
LIMIT 10;

-- ============================================================================
-- ROLLBACK (En caso de error)
-- ============================================================================
-- Si necesitas revertir los cambios:

-- ALTER TABLE factura DROP COLUMN fecha_pago;
-- ALTER TABLE factura ADD COLUMN fecha TIMESTAMP NULL AFTER igv;

-- ============================================================================
-- NOTAS IMPORTANTES
-- ============================================================================

/*
1. STORED PROCEDURES AFECTADOS (ya modificados manualmente):
   - CrearFactura: Ya no inserta el campo 'fecha'
   - ObtenerFacturaCompleta: Ya no selecciona 'fecha'

2. LÓGICA DE NEGOCIO:
   - fecha_pago = fecha_entrega + días de crédito (por defecto 7 días)
   - Si no hay fecha_entrega, usar create_date + días de crédito

3. RECORDATORIOS DE PAGO (Punto 5.3.3):
   - El scheduler buscará facturas donde:
     * tipo_factura = 'PENDIENTE'
     * fecha_pago < CURDATE()
     * entregado = true

4. CONSIDERACIONES:
   - El campo fecha_pago puede ser NULL para facturas muy antiguas
   - En el formulario de crear factura, calcular automáticamente:
     fecha_pago = fecha_entrega + 7 días (o configurable)

5. ÍNDICES RECOMENDADOS (opcional, para optimizar consultas):
*/

-- Índice para búsquedas de facturas vencidas:
CREATE INDEX idx_factura_pago_vencido 
ON factura(tipo_factura, fecha_pago, entregado);

-- Índice para búsquedas por fecha de pago:
CREATE INDEX idx_factura_fecha_pago 
ON factura(fecha_pago);

-- ============================================================================
-- FIN DE LA MIGRACIÓN
-- ============================================================================

/*
RESULTADO ESPERADO:
✅ Campo 'fecha' eliminado
✅ Campo 'fecha_pago' agregado
✅ Datos migrados (fecha_pago calculada basándose en fecha_entrega)
✅ Índices creados para optimizar consultas de recordatorios
✅ Estructura lista para implementar Punto 5.3.3
*/
