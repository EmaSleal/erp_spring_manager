-- ============================================================================
-- Migration: Backfill articulo_maestro from existing producto rows
-- Run AFTER the app starts once (JPA creates articulo_maestro and adds
-- idArticuloMaestro column to producto via ddl-auto: update).
-- Safe to re-run: INSERT IGNORE deduplicates by uk_descripcion.
-- linea_factura.idProducto is NOT touched — FK integrity preserved.
-- ============================================================================

-- Step 1: Insert one master per distinct descripcion.
INSERT IGNORE INTO articulo_maestro
    (descripcion, active, gravado, porcentaje_impuesto, aplica_otro_impuesto, codigo_cabys, descripcion_cabys)
SELECT DISTINCT
    p.descripcion,
    COALESCE(p.active, 1),
    COALESCE(p.gravado, 0),
    p.porcentaje_impuesto,
    COALESCE(p.aplica_otro_impuesto, 0),
    p.codigo_cabys,
    p.descripcion_cabys
FROM producto p
WHERE p.descripcion IS NOT NULL;

-- Step 2: Link each producto to its master.
UPDATE producto p
    JOIN articulo_maestro am ON am.descripcion = p.descripcion
SET p.idArticuloMaestro = am.idArticuloMaestro
WHERE p.idArticuloMaestro IS NULL;

-- Validation query (run after applying to confirm zero unlinked rows):
--   SELECT COUNT(*) FROM producto WHERE idArticuloMaestro IS NULL;

-- ============================================================================
-- SP updates required (DBA action — bodies live in DB, not in the repo):
--   ObtenerProductos              → JOIN articulo_maestro; read descripcion + FE fields from master
--   sp_desactivar_producto        → cascade deactivation to all variants of the row's master
--   sp_obtener_productos_mas_vendidos → JOIN articulo_maestro so grouping reflects the master name
-- ============================================================================
