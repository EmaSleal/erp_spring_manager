-- ============================================================================
-- V24: Elimina configuracion_facturacion.igv (T-006, revisión 1)
-- ============================================================================
-- Columna heredada del IGV peruano (valor por defecto 18 en la entidad). Ningún
-- cálculo la usa: la tarifa general de IVA vive solo en el parámetro de sistema
-- FACTURA_IVA_PORCENTAJE (13) y el desglose por línea en CalculadoraImpuestoService
-- (ADR-009). Se elimina porque es NOT NULL sin DEFAULT: tras quitar el campo de la
-- entidad, cualquier INSERT de una nueva configuración fallaría; además una tasa
-- muerta en la configuración invita a reutilizarla por error.
-- ============================================================================

ALTER TABLE configuracion_facturacion DROP COLUMN igv;
