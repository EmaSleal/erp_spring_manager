-- ============================================================================
-- V14: Nómina — DB-level unique constraint on nominas(periodo_inicio, periodo_fin, tipo)
-- ============================================================================
-- Depends on: V12 (nominas table)
-- Idempotent: skips ADD CONSTRAINT if uk_nomina_periodo already exists.
-- ============================================================================

SET @constraint_count = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME        = 'nominas'
      AND CONSTRAINT_NAME   = 'uk_nomina_periodo'
);

SET @sql = IF(
    @constraint_count = 0,
    'ALTER TABLE nominas ADD CONSTRAINT uk_nomina_periodo UNIQUE (periodo_inicio, periodo_fin, tipo)',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
