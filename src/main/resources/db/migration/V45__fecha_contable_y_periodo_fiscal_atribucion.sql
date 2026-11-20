-- T-046 / ADR-050: cada nota de crédito/débito y cada factura sustituta guarda por separado la fecha de su asiento
-- (fecha_contable) y el mes al que se atribuye su IVA (periodo_fiscal_atribucion, AAAA-MM).
--   nota_factura: ambas columnas NOT NULL.
--   factura: nullable; el servicio las exige cuando la factura es sustituta (sustituye_factura_id no nulo).
-- El relleno de los registros existentes NO modifica ningún asiento: la fecha contable de una nota toma la fecha de su
-- asiento de reversión (o la emisión de la nota si no hay), la de una sustituta la de su asiento de venta (o su
-- emisión). Si esa fecha cae en un mes distinto del que daría la regla nueva, solo se informa (no se re-fecha nada).

ALTER TABLE nota_factura
    ADD COLUMN fecha_contable DATE NULL,
    ADD COLUMN periodo_fiscal_atribucion CHAR(7) NULL;

UPDATE nota_factura n
SET n.fecha_contable = COALESCE(
        (SELECT MIN(a.fecha) FROM asientos_contables a
          WHERE a.nota_factura_id = n.id AND a.asiento_revertido_id IS NOT NULL),
        DATE(n.fecha_emision)),
    n.periodo_fiscal_atribucion = DATE_FORMAT(
        IF(n.codigo_referencia IN ('13', '14'), n.fecha_emision_referencia, n.fecha_emision), '%Y-%m');

ALTER TABLE nota_factura
    MODIFY COLUMN fecha_contable DATE NOT NULL,
    MODIFY COLUMN periodo_fiscal_atribucion CHAR(7) NOT NULL;

ALTER TABLE factura
    ADD COLUMN fecha_contable DATE NULL,
    ADD COLUMN periodo_fiscal_atribucion CHAR(7) NULL;

UPDATE factura f
SET f.fecha_contable = COALESCE(
        (SELECT MIN(a.fecha) FROM asientos_contables a
          WHERE a.factura_id = f.id_factura AND a.tipo = 'AUTOMATICO_VENTA'
            AND a.asiento_revertido_id IS NULL AND a.nota_factura_id IS NULL),
        DATE(f.create_date)),
    f.periodo_fiscal_atribucion = DATE_FORMAT(
        IF(f.codigo_sustitucion = '15',
           COALESCE((SELECT MIN(c.fecha_emision) FROM comprobante_electronico c
                      WHERE c.factura_id = f.sustituye_factura_id AND c.estado = 'ACEPTADO'),
                    f.create_date),
           f.create_date), '%Y-%m')
WHERE f.sustituye_factura_id IS NOT NULL;
