-- ============================================================================
-- V22: Producto.precio_incluye_iva (ADR-009, T-006)
-- ============================================================================
-- Sale prices include IVA. Every existing product is left with
-- precio_incluye_iva = TRUE (NOT NULL DEFAULT TRUE fills existing rows).
-- The IVA rate itself is NOT duplicated here: it is resolved per product from
-- articulo_maestro/producto (gravado, porcentaje_impuesto) with fallback to the
-- system parameter FACTURA_IVA_PORCENTAJE (13).
-- ============================================================================

ALTER TABLE producto
    ADD COLUMN precio_incluye_iva BIT(1) NOT NULL DEFAULT b'1';

UPDATE producto SET precio_incluye_iva = b'1' WHERE precio_incluye_iva <> b'1';

-- Ensure the single configurable global rate exists (seeded historically by
-- database/seeds/04_parametro_sistema.sql). Existing values are not touched.
INSERT INTO parametro_sistema (categoria, clave, descripcion, editable, tipo_dato, valor)
SELECT 'FACTURACION', 'FACTURA_IVA_PORCENTAJE', 'Tarifa general de IVA (Costa Rica, código Hacienda 08)',
       b'1', 'DECIMAL', '13'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM parametro_sistema WHERE clave = 'FACTURA_IVA_PORCENTAJE');
