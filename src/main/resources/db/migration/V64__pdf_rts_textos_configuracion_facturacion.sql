-- ============================================================================
-- V64: textos del PDF de la venta del régimen simplificado (T-059, ADR-078)
-- ============================================================================
-- El título y la leyenda del PDF de las facturas selladas SIMPLIFICADO dejan de salir de la propiedad
-- facturacion.pdf.titulo-rts y pasan a configuracion_facturacion (se editan en Configuración, pestaña Facturación):
--   pdf_rts_titulo   VARCHAR(120) NOT NULL  título del comprobante; el guardado exige la palabra «Factura».
--                                           DEFAULT = el texto inicial: una fila nueva lo recibe de la base, sin texto fijo en Java.
--   pdf_rts_leyenda  VARCHAR(300) NULL      leyenda opcional junto al total; NULL = sin leyenda (sin DEFAULT a propósito).
-- Idempotente: las columnas se agregan solo si faltan, el UPDATE solo toca filas con el título vacío (una fila que ya
-- tiene título conserva título y leyenda, y una leyenda que el usuario dejó en NULL no se vuelve a sembrar) y el
-- MODIFY final converge al mismo tipo. Los textos de siembra solo existen en este script.
-- ============================================================================

SET @t059_falta_titulo := (SELECT COUNT(*) = 0 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'configuracion_facturacion' AND column_name = 'pdf_rts_titulo');
SET @t059_sql := IF(@t059_falta_titulo, 'ALTER TABLE configuracion_facturacion ADD COLUMN pdf_rts_titulo VARCHAR(120) NULL', 'DO 0');
PREPARE t059_stmt FROM @t059_sql;
EXECUTE t059_stmt;
DEALLOCATE PREPARE t059_stmt;

SET @t059_falta_leyenda := (SELECT COUNT(*) = 0 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'configuracion_facturacion' AND column_name = 'pdf_rts_leyenda');
SET @t059_sql := IF(@t059_falta_leyenda, 'ALTER TABLE configuracion_facturacion ADD COLUMN pdf_rts_leyenda VARCHAR(300) NULL', 'DO 0');
PREPARE t059_stmt FROM @t059_sql;
EXECUTE t059_stmt;
DEALLOCATE PREPARE t059_stmt;

-- Siembra inicial: solo filas sin título. La leyenda se siembra en esa misma pasada y no se toca si ya traía un valor.
UPDATE configuracion_facturacion
   SET pdf_rts_leyenda = CASE WHEN pdf_rts_leyenda IS NULL OR TRIM(pdf_rts_leyenda) = ''
                              THEN 'Contribuyente del Régimen de Tributación Simplificada. No cobra IVA.'
                              ELSE pdf_rts_leyenda END,
       pdf_rts_titulo  = 'Factura — Régimen de Tributación Simplificada'
 WHERE pdf_rts_titulo IS NULL OR TRIM(pdf_rts_titulo) = '';

ALTER TABLE configuracion_facturacion
    MODIFY COLUMN pdf_rts_titulo VARCHAR(120) NOT NULL DEFAULT 'Factura — Régimen de Tributación Simplificada';
