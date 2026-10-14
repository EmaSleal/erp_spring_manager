-- ============================================================================
-- V25: Multimoneda contable — TC propio en pagos y compras, diferencial (ADR-011, T-007)
-- ============================================================================
-- 1. New columns (all NULL-able; backfilled below so no existing row keeps NULL):
--      pagos            : moneda, tipo_cambio DECIMAL(18,5), monto_aplicado DECIMAL(12,2)
--      ordenes_compra   : tipo_cambio DECIMAL(18,5)
--      cuentas_por_pagar: tipo_cambio DECIMAL(18,5)
--      pagos_proveedor  : moneda, tipo_cambio DECIMAL(18,5), monto_aplicado DECIMAL(19,2)
--    These tables have no CREATE TABLE in Flyway (created historically by
--    Hibernate), so only ALTER TABLE ... ADD COLUMN is used.
-- 2. Accounting keys DIFERENCIAL_CAMBIARIO_INGRESO / DIFERENCIAL_CAMBIARIO_GASTO
--    (same rules as V19, without stored procedures):
--      * the leaf account is created when its code does not exist;
--      * the key is linked only to an account with the expected tipo/naturaleza;
--        if the code exists with another tipo/naturaleza the subquery yields NULL
--        and the INSERT fails (cuenta_contable_id is NOT NULL + FK) — fail fast;
--      * an already configured key (manual setup) is kept as is.
-- 3. Backfill (ADR-011):
--      pagos: moneda = factura.moneda_fe (CRC for advances or NULL currency),
--             tipo_cambio = factura.tipo_cambio (1 for CRC / advances),
--             monto_aplicado = monto.
--             A foreign-currency invoice without tipo_cambio (pre-ADR-004 data)
--             falls back to the sale rate registered for the payment date and,
--             as a last resort, 1.
--      ordenes_compra / cuentas_por_pagar: 1 when CRC (or NULL currency).
--             Foreign currency: a CxP created from an OC inherits the OC rate;
--             otherwise the active sale rate in tipos_cambio on or before the
--             document date; as a last resort 1, which is the effective rate at
--             which the pre-ADR-011 purchase entries were posted (unconverted).
--      pagos_proveedor: moneda = CxP.moneda (CRC if NULL),
--             tipo_cambio = CxP.tipo_cambio (1 for CRC), monto_aplicado = monto
--             (historical supplier payments were posted unconverted, without
--             differential).
--    Posted asientos are NOT modified (historical foreign-currency entries are
--    only reported; see T-007).
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. Columns
-- ---------------------------------------------------------------------------

ALTER TABLE pagos
    ADD COLUMN moneda         ENUM('CRC','EUR','USD') NULL,
    ADD COLUMN tipo_cambio    DECIMAL(18,5)           NULL,
    ADD COLUMN monto_aplicado DECIMAL(12,2)           NULL;

ALTER TABLE ordenes_compra
    ADD COLUMN tipo_cambio DECIMAL(18,5) NULL;

ALTER TABLE cuentas_por_pagar
    ADD COLUMN tipo_cambio DECIMAL(18,5) NULL;

ALTER TABLE pagos_proveedor
    ADD COLUMN moneda         ENUM('CRC','EUR','USD') NULL,
    ADD COLUMN tipo_cambio    DECIMAL(18,5)           NULL,
    ADD COLUMN monto_aplicado DECIMAL(19,2)           NULL;

-- ---------------------------------------------------------------------------
-- 2. Accounting keys for the realized exchange differential
-- ---------------------------------------------------------------------------

INSERT INTO cuentas_contables
    (codigo, nombre, tipo, naturaleza, nivel, activa, acepta_movimientos, cuenta_padre_id,
     descripcion, creado_por, fecha_creacion, modificado_por, fecha_modificacion)
SELECT '4.2.01.001', 'Diferencial Cambiario Ganado', 'INGRESO', 'ACREEDORA', 4, true, true, NULL,
       'Ganancia por diferencial cambiario realizado en cobros y pagos en moneda extranjera',
       NULL, NOW(), NULL, NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cuentas_contables WHERE codigo = '4.2.01.001');

INSERT INTO cuentas_contables
    (codigo, nombre, tipo, naturaleza, nivel, activa, acepta_movimientos, cuenta_padre_id,
     descripcion, creado_por, fecha_creacion, modificado_por, fecha_modificacion)
SELECT '5.2.01.001', 'Diferencial Cambiario Perdido', 'EGRESO', 'DEUDORA', 4, true, true, NULL,
       'Pérdida por diferencial cambiario realizado en cobros y pagos en moneda extranjera',
       NULL, NOW(), NULL, NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cuentas_contables WHERE codigo = '5.2.01.001');

INSERT INTO parametros_contables (clave, descripcion, cuenta_contable_id)
SELECT 'DIFERENCIAL_CAMBIARIO_INGRESO', 'Ingreso por diferencial cambiario realizado (ADR-011)',
       (SELECT c.id FROM cuentas_contables c
         WHERE c.codigo = '4.2.01.001' AND c.tipo = 'INGRESO' AND c.naturaleza = 'ACREEDORA')
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM parametros_contables WHERE clave = 'DIFERENCIAL_CAMBIARIO_INGRESO');

INSERT INTO parametros_contables (clave, descripcion, cuenta_contable_id)
SELECT 'DIFERENCIAL_CAMBIARIO_GASTO', 'Gasto por diferencial cambiario realizado (ADR-011)',
       (SELECT c.id FROM cuentas_contables c
         WHERE c.codigo = '5.2.01.001' AND c.tipo = 'EGRESO' AND c.naturaleza = 'DEUDORA')
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM parametros_contables WHERE clave = 'DIFERENCIAL_CAMBIARIO_GASTO');

-- ---------------------------------------------------------------------------
-- 3. Backfill
-- ---------------------------------------------------------------------------

-- 3.1 Customer payments
UPDATE pagos p
LEFT JOIN factura f ON f.id_factura = p.id_factura
SET p.moneda = CASE
        WHEN f.id_factura IS NULL OR f.moneda_fe IS NULL THEN 'CRC'
        ELSE f.moneda_fe
    END,
    p.tipo_cambio = CASE
        WHEN f.id_factura IS NULL OR f.moneda_fe IS NULL OR f.moneda_fe = 'CRC' THEN 1
        ELSE COALESCE(
            NULLIF(f.tipo_cambio, 0),
            (SELECT tc.tasa_venta FROM tipos_cambio tc
              WHERE tc.moneda_destino = f.moneda_fe AND tc.activo = 1
                AND tc.fecha <= DATE(p.fecha_pago)
              ORDER BY tc.fecha DESC, tc.id DESC LIMIT 1),
            1)
    END,
    p.monto_aplicado = p.monto
WHERE p.moneda IS NULL OR p.tipo_cambio IS NULL OR p.monto_aplicado IS NULL;

-- 3.2 Purchase orders
UPDATE ordenes_compra oc
SET oc.tipo_cambio = 1
WHERE oc.tipo_cambio IS NULL AND (oc.moneda IS NULL OR oc.moneda = 'CRC');

UPDATE ordenes_compra oc
SET oc.tipo_cambio = COALESCE(
        (SELECT tc.tasa_venta FROM tipos_cambio tc
          WHERE tc.moneda_destino = oc.moneda AND tc.activo = 1 AND tc.fecha <= oc.fecha
          ORDER BY tc.fecha DESC, tc.id DESC LIMIT 1),
        1)
WHERE oc.tipo_cambio IS NULL;

-- 3.3 Accounts payable
UPDATE cuentas_por_pagar c
SET c.tipo_cambio = 1
WHERE c.tipo_cambio IS NULL AND (c.moneda IS NULL OR c.moneda = 'CRC');

UPDATE cuentas_por_pagar c
JOIN ordenes_compra oc ON oc.id = c.orden_compra_id
SET c.tipo_cambio = oc.tipo_cambio
WHERE c.tipo_cambio IS NULL AND oc.moneda = c.moneda AND oc.tipo_cambio IS NOT NULL;

UPDATE cuentas_por_pagar c
SET c.tipo_cambio = COALESCE(
        (SELECT tc.tasa_venta FROM tipos_cambio tc
          WHERE tc.moneda_destino = c.moneda AND tc.activo = 1 AND tc.fecha <= c.fecha_emision
          ORDER BY tc.fecha DESC, tc.id DESC LIMIT 1),
        1)
WHERE c.tipo_cambio IS NULL;

-- 3.4 Supplier payments
UPDATE pagos_proveedor pp
JOIN cuentas_por_pagar c ON c.id = pp.cuenta_por_pagar_id
SET pp.moneda = COALESCE(c.moneda, 'CRC'),
    pp.tipo_cambio = CASE WHEN c.moneda IS NULL OR c.moneda = 'CRC' THEN 1 ELSE c.tipo_cambio END,
    pp.monto_aplicado = pp.monto
WHERE pp.moneda IS NULL OR pp.tipo_cambio IS NULL OR pp.monto_aplicado IS NULL;
