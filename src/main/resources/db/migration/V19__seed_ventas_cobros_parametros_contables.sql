-- ============================================================================
-- V19: Seed sales/collection accounting parameters (ADR-008)
-- ============================================================================
-- Creates the minimum leaf accounts for sales and customer collections and
-- maps them to the parametros_contables keys required by
-- AsientoContableService.generarAsientoVenta / generarAsientoPago:
--   VENTA_CXC_CLIENTES, VENTA_INGRESOS, VENTA_IVA_POR_PAGAR,
--   COBRO_ANTICIPOS_CLIENTES, COBRO_CAJA_<MetodoPago>
-- INSERT IGNORE on both tables: an existing account code or key is kept
-- untouched, so re-running is safe and manual configuration is preserved.
-- ============================================================================

-- Cash and bank accounts (one per collection channel)
INSERT IGNORE INTO cuentas_contables
    (codigo, nombre, tipo, naturaleza, nivel, activa, acepta_movimientos, cuenta_padre_id, descripcion,
     creado_por, fecha_creacion, modificado_por, fecha_modificacion)
VALUES
    ('1.1.01.001', 'Caja General',                'ACTIVO', 'DEUDORA', 4, true, true, NULL,
     'Cobros recibidos en efectivo', NULL, NOW(), NULL, NULL),
    ('1.1.01.002', 'Bancos',                      'ACTIVO', 'DEUDORA', 4, true, true, NULL,
     'Cobros por transferencia o depósito bancario', NULL, NOW(), NULL, NULL),
    ('1.1.01.003', 'Tarjetas por Liquidar',       'ACTIVO', 'DEUDORA', 4, true, true, NULL,
     'Cobros con tarjeta pendientes de liquidación del datáfono', NULL, NOW(), NULL, NULL),
    ('1.1.01.004', 'Cheques por Depositar',       'ACTIVO', 'DEUDORA', 4, true, true, NULL,
     'Cheques recibidos pendientes de depósito', NULL, NOW(), NULL, NULL),
    ('1.1.01.005', 'Recaudación por Terceros',    'ACTIVO', 'DEUDORA', 4, true, true, NULL,
     'Cobros recaudados por terceros pendientes de traslado', NULL, NOW(), NULL, NULL);

-- Receivables
INSERT IGNORE INTO cuentas_contables
    (codigo, nombre, tipo, naturaleza, nivel, activa, acepta_movimientos, cuenta_padre_id, descripcion,
     creado_por, fecha_creacion, modificado_por, fecha_modificacion)
VALUES
    ('1.1.02.001', 'Cuentas por Cobrar Clientes', 'ACTIVO', 'DEUDORA', 4, true, true, NULL,
     'Saldos por cobrar a clientes por ventas a crédito', NULL, NOW(), NULL, NULL);

-- Liabilities: VAT payable and customer advances
INSERT IGNORE INTO cuentas_contables
    (codigo, nombre, tipo, naturaleza, nivel, activa, acepta_movimientos, cuenta_padre_id, descripcion,
     creado_por, fecha_creacion, modificado_por, fecha_modificacion)
VALUES
    ('2.1.02.001', 'IVA por Pagar',               'PASIVO', 'ACREEDORA', 4, true, true, NULL,
     'IVA trasladado en ventas pendiente de declarar', NULL, NOW(), NULL, NULL),
    ('2.1.03.001', 'Anticipos de Clientes',       'PASIVO', 'ACREEDORA', 4, true, true, NULL,
     'Pagos recibidos de clientes sin factura asignada', NULL, NOW(), NULL, NULL);

-- Revenue
INSERT IGNORE INTO cuentas_contables
    (codigo, nombre, tipo, naturaleza, nivel, activa, acepta_movimientos, cuenta_padre_id, descripcion,
     creado_por, fecha_creacion, modificado_por, fecha_modificacion)
VALUES
    ('4.1.01.001', 'Ingresos por Ventas',         'INGRESO', 'ACREEDORA', 4, true, true, NULL,
     'Ingresos por venta de mercadería y servicios', NULL, NOW(), NULL, NULL);

-- Map sales keys
INSERT IGNORE INTO parametros_contables (clave, descripcion, cuenta_contable_id)
    SELECT 'VENTA_CXC_CLIENTES',       'Cuenta por cobrar a clientes (ventas y cobros)', id
    FROM cuentas_contables WHERE codigo = '1.1.02.001';

INSERT IGNORE INTO parametros_contables (clave, descripcion, cuenta_contable_id)
    SELECT 'VENTA_INGRESOS',           'Cuenta de ingresos por ventas',                  id
    FROM cuentas_contables WHERE codigo = '4.1.01.001';

INSERT IGNORE INTO parametros_contables (clave, descripcion, cuenta_contable_id)
    SELECT 'VENTA_IVA_POR_PAGAR',      'Cuenta de IVA por pagar en ventas',              id
    FROM cuentas_contables WHERE codigo = '2.1.02.001';

-- Map collection keys
INSERT IGNORE INTO parametros_contables (clave, descripcion, cuenta_contable_id)
    SELECT 'COBRO_ANTICIPOS_CLIENTES', 'Cuenta de anticipos de clientes (pagos sin factura)', id
    FROM cuentas_contables WHERE codigo = '2.1.03.001';

INSERT IGNORE INTO parametros_contables (clave, descripcion, cuenta_contable_id)
    SELECT 'COBRO_CAJA_EFECTIVO',      'Caja/banco para cobros en efectivo',             id
    FROM cuentas_contables WHERE codigo = '1.1.01.001';

INSERT IGNORE INTO parametros_contables (clave, descripcion, cuenta_contable_id)
    SELECT 'COBRO_CAJA_TARJETA',       'Caja/banco para cobros con tarjeta',             id
    FROM cuentas_contables WHERE codigo = '1.1.01.003';

INSERT IGNORE INTO parametros_contables (clave, descripcion, cuenta_contable_id)
    SELECT 'COBRO_CAJA_CHEQUE',        'Caja/banco para cobros con cheque',              id
    FROM cuentas_contables WHERE codigo = '1.1.01.004';

INSERT IGNORE INTO parametros_contables (clave, descripcion, cuenta_contable_id)
    SELECT 'COBRO_CAJA_TRANSFERENCIA', 'Caja/banco para cobros por transferencia',       id
    FROM cuentas_contables WHERE codigo = '1.1.01.002';

INSERT IGNORE INTO parametros_contables (clave, descripcion, cuenta_contable_id)
    SELECT 'COBRO_CAJA_RECAUDADO',     'Caja/banco para cobros recaudados por terceros', id
    FROM cuentas_contables WHERE codigo = '1.1.01.005';

INSERT IGNORE INTO parametros_contables (clave, descripcion, cuenta_contable_id)
    SELECT 'COBRO_CAJA_OTROS',         'Caja/banco para cobros por otros medios',        id
    FROM cuentas_contables WHERE codigo = '1.1.01.001';
