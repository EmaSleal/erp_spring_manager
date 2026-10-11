-- ============================================================================
-- V23: Datos fiscales de productos -> gravado 13 % (T-006, revisión 1)
-- ============================================================================
-- gravado = false era el valor por defecto de la columna, no una decisión
-- fiscal: todos los productos tributan IVA 13 %. La lógica sigue respetando
-- las marcas gravado/porcentaje por producto (ADR-009); se corrigen los DATOS.
--
-- Resolución (CalculadoraImpuestoService.resolveGravado/resolvePorcentajeImpuesto):
--   articulo_maestro es la fuente de verdad; los campos deprecados de producto
--   solo se usan cuando el maestro no existe o no define el valor.
-- Por eso se actualizan las dos tablas:
--   1. articulo_maestro: fuente de verdad del valor resuelto.
--   2. producto (campos deprecados): respaldo para productos sin maestro o con
--      maestro sin valor; se corrige para que ningún camino resuelva "exento".
-- Criterio en ambas tablas: gravado = false y porcentaje NULL o 0.
-- No hay productos excluidos.
--
-- Re-ejecución: NO es idempotente a propósito, FALLA de forma explícita.
-- CREATE TABLE (sin IF NOT EXISTS) aborta con "Table 'auditoria_producto_iva'
-- already exists" antes de tocar datos. Así un re-run nunca reconvierte en
-- gravados los productos marcados explícitamente como exentos después de esta
-- migración (gravado = false, porcentaje 0), y la auditoría original se conserva.
-- ============================================================================

CREATE TABLE auditoria_producto_iva (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    tabla_origen           VARCHAR(30)   NOT NULL COMMENT 'articulo_maestro | producto',
    id_articulo_maestro    INT           NULL     COMMENT 'Maestro modificado o maestro del producto modificado',
    id_producto            INT           NULL     COMMENT 'Producto modificado (NULL en filas de articulo_maestro)',
    gravado_anterior       BIT(1)        NULL,
    porcentaje_anterior    DECIMAL(38,2) NULL,
    gravado_nuevo          BIT(1)        NOT NULL,
    porcentaje_nuevo       DECIMAL(38,2) NOT NULL,
    migracion              VARCHAR(10)   NOT NULL DEFAULT 'V23',
    fecha                  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_auditoria_producto_iva_producto (id_producto),
    KEY idx_auditoria_producto_iva_maestro (id_articulo_maestro)
) ENGINE=InnoDB;

-- 1. Auditoría y corrección de articulo_maestro (fuente de verdad)
INSERT INTO auditoria_producto_iva
    (tabla_origen, id_articulo_maestro, id_producto, gravado_anterior, porcentaje_anterior,
     gravado_nuevo, porcentaje_nuevo)
SELECT 'articulo_maestro', am.id_articulo_maestro, NULL, am.gravado, am.porcentaje_impuesto,
       b'1', 13.00
FROM articulo_maestro am
WHERE am.gravado = b'0'
  AND (am.porcentaje_impuesto IS NULL OR am.porcentaje_impuesto = 0);

UPDATE articulo_maestro
SET gravado = b'1',
    porcentaje_impuesto = 13.00
WHERE gravado = b'0'
  AND (porcentaje_impuesto IS NULL OR porcentaje_impuesto = 0);

-- 2. Auditoría y corrección de los campos deprecados de producto (respaldo)
INSERT INTO auditoria_producto_iva
    (tabla_origen, id_articulo_maestro, id_producto, gravado_anterior, porcentaje_anterior,
     gravado_nuevo, porcentaje_nuevo)
SELECT 'producto', p.id_articulo_maestro, p.id_producto, p.gravado, p.porcentaje_impuesto,
       b'1', 13.00
FROM producto p
WHERE p.gravado = b'0'
  AND (p.porcentaje_impuesto IS NULL OR p.porcentaje_impuesto = 0);

UPDATE producto
SET gravado = b'1',
    porcentaje_impuesto = 13.00
WHERE gravado = b'0'
  AND (porcentaje_impuesto IS NULL OR porcentaje_impuesto = 0);
