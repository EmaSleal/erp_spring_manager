-- ============================================================================
-- V7: RRHH — Add fecha_aprobacion to ausencias
-- ============================================================================
-- Records the exact timestamp when an ausencia was approved.
-- ============================================================================

ALTER TABLE ausencias ADD COLUMN fecha_aprobacion DATETIME NULL;
