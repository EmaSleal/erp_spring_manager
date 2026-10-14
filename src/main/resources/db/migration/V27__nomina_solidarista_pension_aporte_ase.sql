-- ============================================================================
-- V27: Nómina — solidarista, pensión alimentaria y aporte patronal ASE (ADR-013, T-010)
-- ============================================================================
-- 1. Tabla versionada parametros_nomina_empresa (mismo patrón que parametros_ccss):
--    porcentaje patronal a la Asociación Solidarista (convenio de empresa, no tasa
--    legal). Se siembra un registro vigente desde 2026-01-01 con 0.0533.
-- 2. Columnas del aporte patronal ASE en la línea de nómina y en el total de la nómina
--    (DEFAULT 0.00: las nóminas existentes quedan con aporte 0).
-- 3. Cuentas contables (P-012: nacen con cuenta padre, nunca NULL):
--      2.1.04.001 Asociación solidarista por pagar      (PASIVO / ACREEDORA)
--      2.1.04.002 Pensiones alimentarias por pagar      (PASIVO / ACREEDORA)
--      5.1.02.001 Aporte patronal asociación solidarista (EGRESO / DEUDORA)
--    Padres: 2.1.04 (Obligaciones laborales por pagar) y 5.1.02 (Gastos de personal),
--    con sus ancestros 2 / 2.1 y 5 / 5.1. No se usan 2.1.01 ni 5.1.01 porque en la base
--    existente son cuentas de movimiento ("Cuentas por Pagar Proveedores" y "Compras").
--    Cada padre se crea solo si su código no
--    existe; si ya existe se reutiliza tal cual. Las cuentas de nivel 1 (2 y 5) son
--    raíz del plan y por definición no tienen padre.
--    Si alguna de las tres hojas ya existía sin padre, se le asigna el padre.
-- 4. Claves de parámetro contable (mismas reglas que V25: la clave solo se enlaza a una
--    cuenta con el tipo/naturaleza esperados; si el código existe con otro
--    tipo/naturaleza la subconsulta da NULL y el INSERT falla — fail fast; una clave
--    ya configurada manualmente se respeta):
--      NOMINA_SOLIDARISTA_POR_PAGAR         -> 2.1.04.001
--      NOMINA_PENSION_ALIMENTARIA_POR_PAGAR -> 2.1.04.002
--      NOMINA_APORTE_PATRONAL_ASE_GASTO     -> 5.1.02.001
--      NOMINA_APORTE_PATRONAL_ASE_POR_PAGAR -> 2.1.04.001 (mismo pasivo que el ahorro obrero)
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. parametros_nomina_empresa
-- ---------------------------------------------------------------------------

CREATE TABLE parametros_nomina_empresa (
    id                              BIGINT AUTO_INCREMENT PRIMARY KEY,
    porcentaje_aporte_patronal_ase  DECIMAL(5,4) NOT NULL,
    vigencia_desde                  DATE         NOT NULL,
    vigencia_hasta                  DATE,
    created_at                      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_nomina_empresa_vigencia (vigencia_desde, vigencia_hasta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Aporte patronal ASE vigente desde 1-ene-2026, sin fecha de fin: 5.33 %
INSERT INTO parametros_nomina_empresa (porcentaje_aporte_patronal_ase, vigencia_desde, vigencia_hasta)
VALUES (0.0533, '2026-01-01', NULL);

-- ---------------------------------------------------------------------------
-- 2. Columnas del aporte patronal ASE
-- ---------------------------------------------------------------------------

ALTER TABLE detalles_nomina
    ADD COLUMN aporte_patronal_solidarista DECIMAL(15,2) DEFAULT 0.00;

ALTER TABLE nominas
    ADD COLUMN total_aporte_patronal_solidarista DECIMAL(15,2) DEFAULT 0.00;

-- ---------------------------------------------------------------------------
-- 3. Cuentas contables
-- ---------------------------------------------------------------------------

-- 3.1 Ancestros de pasivo: 2 > 2.1 > 2.1.04
INSERT INTO cuentas_contables
    (codigo, nombre, tipo, naturaleza, nivel, activa, acepta_movimientos, cuenta_padre_id,
     descripcion, creado_por, fecha_creacion, modificado_por, fecha_modificacion)
SELECT '2', 'Pasivo', 'PASIVO', 'ACREEDORA', 1, true, false, NULL,
       'Obligaciones de la empresa', NULL, NOW(), NULL, NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cuentas_contables WHERE codigo = '2');

INSERT INTO cuentas_contables
    (codigo, nombre, tipo, naturaleza, nivel, activa, acepta_movimientos, cuenta_padre_id,
     descripcion, creado_por, fecha_creacion, modificado_por, fecha_modificacion)
SELECT '2.1', 'Pasivo Corriente', 'PASIVO', 'ACREEDORA', 2, true, false,
       (SELECT p.id FROM cuentas_contables p WHERE p.codigo = '2'),
       'Obligaciones con vencimiento menor a un año', NULL, NOW(), NULL, NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cuentas_contables WHERE codigo = '2.1');

INSERT INTO cuentas_contables
    (codigo, nombre, tipo, naturaleza, nivel, activa, acepta_movimientos, cuenta_padre_id,
     descripcion, creado_por, fecha_creacion, modificado_por, fecha_modificacion)
SELECT '2.1.04', 'Obligaciones Laborales por Pagar', 'PASIVO', 'ACREEDORA', 3, true, false,
       (SELECT p.id FROM cuentas_contables p WHERE p.codigo = '2.1'),
       'Salarios, cargas sociales y deducciones de nómina por pagar', NULL, NOW(), NULL, NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cuentas_contables WHERE codigo = '2.1.04');

-- 3.2 Ancestros de gasto: 5 > 5.1 > 5.1.02
INSERT INTO cuentas_contables
    (codigo, nombre, tipo, naturaleza, nivel, activa, acepta_movimientos, cuenta_padre_id,
     descripcion, creado_por, fecha_creacion, modificado_por, fecha_modificacion)
SELECT '5', 'Gastos', 'EGRESO', 'DEUDORA', 1, true, false, NULL,
       'Gastos de la empresa', NULL, NOW(), NULL, NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cuentas_contables WHERE codigo = '5');

INSERT INTO cuentas_contables
    (codigo, nombre, tipo, naturaleza, nivel, activa, acepta_movimientos, cuenta_padre_id,
     descripcion, creado_por, fecha_creacion, modificado_por, fecha_modificacion)
SELECT '5.1', 'Gastos de Operación', 'EGRESO', 'DEUDORA', 2, true, false,
       (SELECT p.id FROM cuentas_contables p WHERE p.codigo = '5'),
       'Gastos operativos de la empresa', NULL, NOW(), NULL, NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cuentas_contables WHERE codigo = '5.1');

INSERT INTO cuentas_contables
    (codigo, nombre, tipo, naturaleza, nivel, activa, acepta_movimientos, cuenta_padre_id,
     descripcion, creado_por, fecha_creacion, modificado_por, fecha_modificacion)
SELECT '5.1.02', 'Gastos de Personal', 'EGRESO', 'DEUDORA', 3, true, false,
       (SELECT p.id FROM cuentas_contables p WHERE p.codigo = '5.1'),
       'Sueldos, cargas sociales y aportes patronales', NULL, NOW(), NULL, NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cuentas_contables WHERE codigo = '5.1.02');

-- 3.3 Hojas nuevas (con padre)
INSERT INTO cuentas_contables
    (codigo, nombre, tipo, naturaleza, nivel, activa, acepta_movimientos, cuenta_padre_id,
     descripcion, creado_por, fecha_creacion, modificado_por, fecha_modificacion)
SELECT '2.1.04.001', 'Asociación Solidarista por Pagar', 'PASIVO', 'ACREEDORA', 4, true, true,
       (SELECT p.id FROM cuentas_contables p WHERE p.codigo = '2.1.04'),
       'Ahorro obrero y aporte patronal a la Asociación Solidarista pendientes de traslado',
       NULL, NOW(), NULL, NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cuentas_contables WHERE codigo = '2.1.04.001');

INSERT INTO cuentas_contables
    (codigo, nombre, tipo, naturaleza, nivel, activa, acepta_movimientos, cuenta_padre_id,
     descripcion, creado_por, fecha_creacion, modificado_por, fecha_modificacion)
SELECT '2.1.04.002', 'Pensiones Alimentarias por Pagar', 'PASIVO', 'ACREEDORA', 4, true, true,
       (SELECT p.id FROM cuentas_contables p WHERE p.codigo = '2.1.04'),
       'Pensiones alimentarias retenidas a empleados pendientes de depósito',
       NULL, NOW(), NULL, NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cuentas_contables WHERE codigo = '2.1.04.002');

INSERT INTO cuentas_contables
    (codigo, nombre, tipo, naturaleza, nivel, activa, acepta_movimientos, cuenta_padre_id,
     descripcion, creado_por, fecha_creacion, modificado_por, fecha_modificacion)
SELECT '5.1.02.001', 'Aporte Patronal Asociación Solidarista', 'EGRESO', 'DEUDORA', 4, true, true,
       (SELECT p.id FROM cuentas_contables p WHERE p.codigo = '5.1.02'),
       'Aporte patronal a la Asociación Solidarista de los empleados afiliados',
       NULL, NOW(), NULL, NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cuentas_contables WHERE codigo = '5.1.02.001');

-- 3.4 Hojas que ya existían sin padre: se les asigna el padre (solo estas tres)
UPDATE cuentas_contables c
JOIN cuentas_contables p ON p.codigo = '2.1.04'
SET c.cuenta_padre_id = p.id
WHERE c.codigo IN ('2.1.04.001', '2.1.04.002') AND c.cuenta_padre_id IS NULL;

UPDATE cuentas_contables c
JOIN cuentas_contables p ON p.codigo = '5.1.02'
SET c.cuenta_padre_id = p.id
WHERE c.codigo = '5.1.02.001' AND c.cuenta_padre_id IS NULL;

-- ---------------------------------------------------------------------------
-- 4. Claves de parámetro contable
-- ---------------------------------------------------------------------------

INSERT INTO parametros_contables (clave, descripcion, cuenta_contable_id)
SELECT 'NOMINA_SOLIDARISTA_POR_PAGAR', 'Ahorro obrero solidarista por pagar (ADR-013)',
       (SELECT c.id FROM cuentas_contables c
         WHERE c.codigo = '2.1.04.001' AND c.tipo = 'PASIVO' AND c.naturaleza = 'ACREEDORA')
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM parametros_contables WHERE clave = 'NOMINA_SOLIDARISTA_POR_PAGAR');

INSERT INTO parametros_contables (clave, descripcion, cuenta_contable_id)
SELECT 'NOMINA_PENSION_ALIMENTARIA_POR_PAGAR', 'Pensiones alimentarias retenidas por pagar (ADR-013)',
       (SELECT c.id FROM cuentas_contables c
         WHERE c.codigo = '2.1.04.002' AND c.tipo = 'PASIVO' AND c.naturaleza = 'ACREEDORA')
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM parametros_contables WHERE clave = 'NOMINA_PENSION_ALIMENTARIA_POR_PAGAR');

INSERT INTO parametros_contables (clave, descripcion, cuenta_contable_id)
SELECT 'NOMINA_APORTE_PATRONAL_ASE_GASTO', 'Gasto por aporte patronal a la Asociación Solidarista (ADR-013)',
       (SELECT c.id FROM cuentas_contables c
         WHERE c.codigo = '5.1.02.001' AND c.tipo = 'EGRESO' AND c.naturaleza = 'DEUDORA')
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM parametros_contables WHERE clave = 'NOMINA_APORTE_PATRONAL_ASE_GASTO');

INSERT INTO parametros_contables (clave, descripcion, cuenta_contable_id)
SELECT 'NOMINA_APORTE_PATRONAL_ASE_POR_PAGAR', 'Aporte patronal ASE por pagar — mismo pasivo que el ahorro obrero (ADR-013)',
       (SELECT c.id FROM cuentas_contables c
         WHERE c.codigo = '2.1.04.001' AND c.tipo = 'PASIVO' AND c.naturaleza = 'ACREEDORA')
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM parametros_contables WHERE clave = 'NOMINA_APORTE_PATRONAL_ASE_POR_PAGAR');
