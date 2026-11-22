-- T-051 / ADR-061 §3: nota de crédito 06 (devolución de mercancía), total o parcial.
--
-- La modalidad de la nota (TOTAL: acredita todo el comprobante y anula la factura; PARCIAL: acredita un monto por línea
-- y no la anula) salía del código de referencia (01 y 13 totales; 02 y 14 parciales). El 06 no tiene modalidad fija: la
-- fija el pedido (sin líneas es total, con líneas es parcial). Por eso la modalidad se persiste en la nota al emitirla y
-- toda la lógica (topes, elegibilidad, efectos al aceptarse, PDF) lee esta columna, no el código.
--
-- Las notas existentes se rellenan desde su código: 02 y 14 → PARCIAL; el resto (01, 13) → TOTAL. Ninguna nota 06 existe
-- (el código no estaba habilitado), así que el relleno es exacto. Los asientos y demás datos no se tocan.

ALTER TABLE nota_factura
    ADD COLUMN modalidad ENUM('TOTAL', 'PARCIAL') NULL;

UPDATE nota_factura
SET modalidad = IF(codigo_referencia IN ('02', '14'), 'PARCIAL', 'TOTAL')
WHERE modalidad IS NULL;

ALTER TABLE nota_factura
    MODIFY COLUMN modalidad ENUM('TOTAL', 'PARCIAL') NOT NULL;
