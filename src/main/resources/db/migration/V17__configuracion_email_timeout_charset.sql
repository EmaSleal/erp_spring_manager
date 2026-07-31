-- ============================================================================
-- V17: Configuración Email — Add smtp_timeout and charset
-- ============================================================================
-- The frontend form already exposes timeout (ms) and charset inputs, but the
-- backend never persisted them, so PUT/POST requests were silently rejected
-- with UnrecognizedPropertyException. Adds the missing columns.
-- ============================================================================

ALTER TABLE configuracion_email ADD COLUMN smtp_timeout INT NULL DEFAULT 5000;
ALTER TABLE configuracion_email ADD COLUMN charset VARCHAR(20) NULL DEFAULT 'UTF-8';
