-- ============================================================================
-- V8: RRHH — Add justificacion_temporalidad to contratos_empleado
-- ============================================================================
-- CR labor law requires a written justification for fixed-term contracts
-- (PLAZO_FIJO and OBRA_DETERMINADA).
-- ============================================================================

ALTER TABLE contratos_empleado ADD COLUMN justificacion_temporalidad VARCHAR(300) NULL;
