-- ============================================================================
-- V14: Nómina — DB-level unique constraint on nominas(periodo_inicio, periodo_fin, tipo)
-- ============================================================================
-- Depends on: V12 (nominas table)
-- ============================================================================

ALTER TABLE nominas
    ADD CONSTRAINT uk_nomina_periodo UNIQUE (periodo_inicio, periodo_fin, tipo);
