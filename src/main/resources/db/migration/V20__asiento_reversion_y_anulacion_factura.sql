-- ============================================================================
-- V20: Reversal reference on asientos and factura cancellation (ADR-008)
-- ============================================================================
-- 1. asientos_contables.asiento_revertido_id: a reversal entry points to the
--    posted entry it mirrors. Posted entries are never edited or deleted.
-- 2. factura.anulada / fecha_anulacion / motivo_anulacion: a factura with a
--    posted sales entry is cancelled (and its entry reversed) instead of
--    being physically deleted.
-- ============================================================================

ALTER TABLE asientos_contables
    ADD COLUMN asiento_revertido_id BIGINT NULL,
    ADD CONSTRAINT fk_asiento_revertido
        FOREIGN KEY (asiento_revertido_id) REFERENCES asientos_contables(id);

CREATE INDEX idx_asiento_revertido ON asientos_contables (asiento_revertido_id);

ALTER TABLE factura
    ADD COLUMN anulada          BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN fecha_anulacion  DATETIME(6)  NULL,
    ADD COLUMN motivo_anulacion VARCHAR(500) NULL;
