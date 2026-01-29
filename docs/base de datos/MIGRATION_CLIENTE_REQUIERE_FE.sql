-- =====================================================
-- MIGRACIÓN: Agregar campo requiere_factura_electronica
-- Fecha: 24 de enero de 2026
-- Descripción: Permite configurar si un cliente requiere
--              facturación electrónica de Costa Rica
-- =====================================================

-- 1. Agregar columna
ALTER TABLE cliente 
ADD COLUMN requiere_factura_electronica BOOLEAN DEFAULT TRUE 
COMMENT 'Indica si el cliente requiere facturación electrónica (Hacienda CR)';

-- 2. Actualizar todos los clientes existentes a TRUE por defecto
UPDATE cliente 
SET requiere_factura_electronica = TRUE 
WHERE requiere_factura_electronica IS NULL;

-- 3. Verificar cambios
SELECT 
    idCliente,
    nombre,
    identificacion,
    requiere_factura_electronica
FROM cliente
LIMIT 10;

-- 4. Estadísticas
SELECT 
    requiere_factura_electronica,
    COUNT(*) as cantidad
FROM cliente
GROUP BY requiere_factura_electronica;

-- =====================================================
-- NOTAS DE USO
-- =====================================================
-- 
-- Para DESACTIVAR la facturación electrónica de un cliente:
-- UPDATE cliente SET requiere_factura_electronica = FALSE WHERE idCliente = X;
--
-- Para ACTIVAR la facturación electrónica de un cliente:
-- UPDATE cliente SET requiere_factura_electronica = TRUE WHERE idCliente = X;
--
-- COMPORTAMIENTO DEL SISTEMA:
-- - TRUE (default): El cliente aparece en lista de facturas pendientes
-- - FALSE: El cliente NO aparece en lista de facturas pendientes
-- - NULL: Se trata como FALSE por seguridad
--
-- VALIDACIONES:
-- - El sistema valida antes de generar comprobante
-- - Si requiere_factura_electronica = FALSE, lanza excepción
-- - El filtro en /api/facturas/comprobantes/empresa/{id}/pendientes
--   excluye automáticamente clientes con FALSE
-- =====================================================
