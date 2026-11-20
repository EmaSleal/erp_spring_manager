-- T-047 / ADR-051, ADR-052, ADR-054, ADR-055 §1: nota de crédito parcial.
--
-- 1. factura.monto_acreditado / factura.acreditada_totalmente: lo acreditado por notas parciales APLICADAS (en la
--    moneda de la factura, IVA incluido) y la marca «acreditada totalmente» (sin anular). Los actualiza
--    NotaFacturaEfectos en la misma transacción en que la nota pasa a APLICADA. Las facturas existentes quedan en 0 /
--    false (ninguna tiene notas parciales).
-- 2. Plan de cuentas (ADR-054): grupo 4.1.02 «Devoluciones y descuentos sobre ventas» (no acepta movimientos) bajo 4.1 y
--    sus hojas 4.1.02.001 y 4.1.02.002 (INGRESO, DEUDORA por ser contrapartida, aceptan movimientos). Idempotente: no
--    toca una cuenta cuyo código ya existe ni ninguna otra cuenta.
-- 3. Parámetros contables explícitos, sin valor por defecto en el código (ADR-024):
--    NOTA_CREDITO_DEVOLUCIONES (nota 06) y NOTA_CREDITO_ANULACIONES_DESCUENTOS (notas 01 y 02).
-- 4. ADR-055 §1: renombre de las cuentas de diferencial cambiario realizado. Solo cambia el nombre.
-- Los asientos ya registrados no se modifican.

ALTER TABLE factura
    ADD COLUMN monto_acreditado DECIMAL(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN acreditada_totalmente BOOLEAN NOT NULL DEFAULT FALSE;

INSERT INTO cuentas_contables
    (codigo, nombre, tipo, naturaleza, nivel, activa, acepta_movimientos, cuenta_padre_id,
     descripcion, creado_por, fecha_creacion, modificado_por, fecha_modificacion)
SELECT '4.1.02', 'Devoluciones y descuentos sobre ventas', 'INGRESO', 'DEUDORA', 3, true, false,
       (SELECT p.id FROM cuentas_contables p WHERE p.codigo = '4.1' LIMIT 1),
       'Cuentas de contrapartida de ingresos: notas de crédito sobre ventas',
       NULL, NOW(), NULL, NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cuentas_contables WHERE codigo = '4.1.02');

INSERT INTO cuentas_contables
    (codigo, nombre, tipo, naturaleza, nivel, activa, acepta_movimientos, cuenta_padre_id,
     descripcion, creado_por, fecha_creacion, modificado_por, fecha_modificacion)
SELECT '4.1.02.001', 'Devoluciones sobre ventas', 'INGRESO', 'DEUDORA', 4, true, true,
       (SELECT p.id FROM cuentas_contables p WHERE p.codigo = '4.1.02' LIMIT 1),
       'Débito por la base de la nota de crédito por devolución de mercancía (código 06)',
       NULL, NOW(), NULL, NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cuentas_contables WHERE codigo = '4.1.02.001');

INSERT INTO cuentas_contables
    (codigo, nombre, tipo, naturaleza, nivel, activa, acepta_movimientos, cuenta_padre_id,
     descripcion, creado_por, fecha_creacion, modificado_por, fecha_modificacion)
SELECT '4.1.02.002', 'Anulaciones, descuentos y rebajas sobre ventas', 'INGRESO', 'DEUDORA', 4, true, true,
       (SELECT p.id FROM cuentas_contables p WHERE p.codigo = '4.1.02' LIMIT 1),
       'Débito por la base de las notas de crédito por anulación (01) y corrección de monto (02)',
       NULL, NOW(), NULL, NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cuentas_contables WHERE codigo = '4.1.02.002');

INSERT INTO parametros_contables (clave, descripcion, cuenta_contable_id)
SELECT 'NOTA_CREDITO_DEVOLUCIONES', 'Débito por la base de la nota de crédito código 06 (ADR-054)',
       (SELECT c.id FROM cuentas_contables c WHERE c.codigo = '4.1.02.001')
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM parametros_contables WHERE clave = 'NOTA_CREDITO_DEVOLUCIONES');

INSERT INTO parametros_contables (clave, descripcion, cuenta_contable_id)
SELECT 'NOTA_CREDITO_ANULACIONES_DESCUENTOS',
       'Débito por la base de las notas de crédito códigos 01 y 02 (ADR-054)',
       (SELECT c.id FROM cuentas_contables c WHERE c.codigo = '4.1.02.002')
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM parametros_contables WHERE clave = 'NOTA_CREDITO_ANULACIONES_DESCUENTOS');

UPDATE cuentas_contables SET nombre = 'Diferencial Cambiario Ganado – realizado' WHERE codigo = '4.2.01.001';
UPDATE cuentas_contables SET nombre = 'Diferencial Cambiario Perdido – realizado' WHERE codigo = '5.2.01.001';
