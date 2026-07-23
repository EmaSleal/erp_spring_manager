-- ============================================================================
-- V16: Seed payroll accounting parameters
-- ============================================================================
-- Creates 6 leaf accounts for CR payroll and maps them to the NOMINA_*
-- parametros_contables keys required by NominaService.contabilizar().
-- INSERT IGNORE on both tables so re-running is safe.
-- ============================================================================

-- Payroll expense account
INSERT IGNORE INTO cuentas_contables
    (codigo, nombre, tipo, naturaleza, nivel, activa, acepta_movimientos, cuenta_padre_id, descripcion,
     creado_por, fecha_creacion, modificado_por, fecha_modificacion)
VALUES
    ('5.1.01.001', 'Gasto en Sueldos y Salarios', 'EGRESO', 'DEUDORA', 4, true, true, NULL,
     'Gasto patronal por planilla de salarios', NULL, NOW(), NULL, NULL);

-- Payroll liability accounts
INSERT IGNORE INTO cuentas_contables
    (codigo, nombre, tipo, naturaleza, nivel, activa, acepta_movimientos, cuenta_padre_id, descripcion,
     creado_por, fecha_creacion, modificado_por, fecha_modificacion)
VALUES
    ('2.1.01.001', 'Salarios por Pagar',          'PASIVO', 'ACREEDORA', 4, true, true, NULL,
     'Salarios netos pendientes de desembolso', NULL, NOW(), NULL, NULL),
    ('2.1.01.002', 'CCSS Patronal por Pagar',     'PASIVO', 'ACREEDORA', 4, true, true, NULL,
     'Cargas sociales patronales CCSS pendientes de entero', NULL, NOW(), NULL, NULL),
    ('2.1.01.003', 'CCSS Empleado por Pagar',     'PASIVO', 'ACREEDORA', 4, true, true, NULL,
     'Deducciones CCSS del empleado retenidas por enterar', NULL, NOW(), NULL, NULL),
    ('2.1.01.004', 'INS por Pagar',               'PASIVO', 'ACREEDORA', 4, true, true, NULL,
     'Prima de riesgos del trabajo INS pendiente', NULL, NOW(), NULL, NULL),
    ('2.1.01.005', 'Retención de Renta por Pagar','PASIVO', 'ACREEDORA', 4, true, true, NULL,
     'Impuesto sobre la renta retenido a empleados', NULL, NOW(), NULL, NULL);

-- Map NOMINA_* parametros to the seeded accounts
INSERT IGNORE INTO parametros_contables (clave, descripcion, cuenta_contable_id)
    SELECT 'NOMINA_GASTO_SUELDOS',          'Cuenta de gasto de sueldos y salarios', id
    FROM cuentas_contables WHERE codigo = '5.1.01.001';

INSERT IGNORE INTO parametros_contables (clave, descripcion, cuenta_contable_id)
    SELECT 'NOMINA_SALARIOS_POR_PAGAR',     'Cuenta de salarios netos por pagar',    id
    FROM cuentas_contables WHERE codigo = '2.1.01.001';

INSERT IGNORE INTO parametros_contables (clave, descripcion, cuenta_contable_id)
    SELECT 'NOMINA_CCSS_PATRONAL_POR_PAGAR','Cuenta CCSS patronal por pagar',        id
    FROM cuentas_contables WHERE codigo = '2.1.01.002';

INSERT IGNORE INTO parametros_contables (clave, descripcion, cuenta_contable_id)
    SELECT 'NOMINA_CCSS_POR_PAGAR',         'Cuenta CCSS empleado por pagar',        id
    FROM cuentas_contables WHERE codigo = '2.1.01.003';

INSERT IGNORE INTO parametros_contables (clave, descripcion, cuenta_contable_id)
    SELECT 'NOMINA_INS_POR_PAGAR',          'Cuenta INS por pagar',                  id
    FROM cuentas_contables WHERE codigo = '2.1.01.004';

INSERT IGNORE INTO parametros_contables (clave, descripcion, cuenta_contable_id)
    SELECT 'NOMINA_RENTA_POR_PAGAR',        'Cuenta retención de renta por pagar',   id
    FROM cuentas_contables WHERE codigo = '2.1.01.005';
