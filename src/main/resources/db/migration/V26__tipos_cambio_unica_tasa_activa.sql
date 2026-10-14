-- ============================================================================
-- V26: tipos_cambio — una sola tasa ACTIVA por fecha y par de monedas (T-008, ADR-004 / ADR-011)
-- ============================================================================
-- The original unique key uk_tipos_cambio_fecha_origen_destino
-- (fecha, moneda_origen, moneda_destino) also covered inactive rows, so
-- TipoCambioService.registrar() — which deactivates the active row and inserts
-- the new one to keep the history — failed for any day that already had a rate
-- (e.g. a manual correction of a day loaded by the daily sync).
--
-- New rule: at most one ACTIVE row per (fecha, moneda_origen, moneda_destino);
-- any number of inactive rows (history) is allowed.
--   * activo_unico: generated column, 1 when activo, NULL otherwise.
--   * Unique key on (fecha, moneda_origen, moneda_destino, activo_unico):
--     NULLs never collide in a MySQL unique index, so only active rows compete.
-- The table has no CREATE TABLE in Flyway (created historically by Hibernate);
-- only ALTER TABLE is used. No stored procedures. Existing data already
-- satisfies the new key (the old key was stricter).
-- ============================================================================

ALTER TABLE tipos_cambio
    ADD COLUMN activo_unico TINYINT GENERATED ALWAYS AS (CASE WHEN activo THEN 1 END) VIRTUAL,
    ADD CONSTRAINT uk_tipos_cambio_fecha_origen_destino_activo
        UNIQUE (fecha, moneda_origen, moneda_destino, activo_unico),
    DROP INDEX uk_tipos_cambio_fecha_origen_destino;
