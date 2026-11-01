-- ============================================================================
-- V38: INS riesgos del trabajo como carga patronal opcional (ADR-033, T-027)
-- ============================================================================
-- 1. parametros_nomina_empresa.porcentaje_ins_riesgos_trabajo: tasa versionada del INS RT
--    (póliza de riesgos del trabajo, a cargo del patrono). NULL = no configurada: no se
--    calcula ni se provisiona. Sin DEFAULT y sin poblar las filas existentes (ADR-024/026).
-- 2. detalles_nomina: la columna ins (deducción al empleado) se reemplaza por
--    ins_riesgos_patronal (carga patronal, no se resta del empleado). Producción tiene
--    0 nóminas: no se migran datos.
-- No se crean rutinas ni triggers (ADR-028).
-- ============================================================================

ALTER TABLE parametros_nomina_empresa
    ADD COLUMN porcentaje_ins_riesgos_trabajo DECIMAL(5,4) NULL;

ALTER TABLE detalles_nomina
    DROP COLUMN ins,
    ADD COLUMN ins_riesgos_patronal DECIMAL(15,2) NOT NULL DEFAULT 0.00;
