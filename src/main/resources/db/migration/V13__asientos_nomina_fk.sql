-- ============================================================================
-- V13: Nómina — columna nomina_id en asientos_contables
-- ============================================================================
-- Depends on: V12 (nominas table)
-- ============================================================================

ALTER TABLE asientos_contables
    ADD COLUMN nomina_id BIGINT NULL,
    ADD CONSTRAINT fk_asiento_nomina
        FOREIGN KEY (nomina_id) REFERENCES nominas(id);

CREATE INDEX idx_asiento_nomina ON asientos_contables (nomina_id);
