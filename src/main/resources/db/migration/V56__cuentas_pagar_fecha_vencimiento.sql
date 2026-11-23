-- ============================================================================
-- V56: fecha de vencimiento de las cuentas por pagar existentes (T-053, revisión 1, criterio 15)
-- ============================================================================
-- Desde T-052 la CxP (manual y desde una orden de compra) quedaba con fecha_vencimiento NULL si la petición no la traía,
-- y sin ella no funcionan /vencidas ni la programación de pagos. A partir de esta versión el servidor la calcula al crear
-- la CxP: fecha del comprobante + días de crédito del proveedor (sin días, o 0: de contado, la fecha del comprobante).
-- Esta migración aplica la misma regla, una sola vez, a las CxP que quedaron con NULL. Para las anteriores a V53, que no
-- tienen fecha_comprobante, se usa fecha_emision. No toca las que ya tienen fecha; repetirla no cambia nada.
-- En el dump de producción (facturas_monrachem_20261118_205706) hay 0 CxP, así que no actualiza ninguna fila.
-- ============================================================================

UPDATE cuentas_por_pagar c
    JOIN proveedores p ON p.id = c.proveedor_id
SET c.fecha_vencimiento = DATE_ADD(COALESCE(c.fecha_comprobante, c.fecha_emision),
                                   INTERVAL GREATEST(COALESCE(p.dias_credito, 0), 0) DAY)
WHERE c.fecha_vencimiento IS NULL;
