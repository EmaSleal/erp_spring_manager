-- ============================================================================
-- V37: estado ERROR_HACIENDA y contador de consultas con ind-estado "error"
-- (ADR-030, T-025 revisión 2)
-- ============================================================================
-- La documentación del API de recepción enumera para GET /recepcion/{clave} los
-- ind-estado recibido, procesando, aceptado, rechazado y error. "error" se mapea
-- a un estado propio, bloqueado: Hacienda tiene el documento, pero no lo resolvió.
-- El sondeo vuelve a consultar y, tras N consultas seguidas con "error", deja de
-- hacerlo; el contador lo lleva `consultas_error_hacienda`.
--
-- Esta migración:
--   1) agrega `consultas_error_hacienda` (0 en todas las filas existentes);
--   2) amplía el ENUM de `estado` con ERROR_HACIENDA.
-- No modifica ningún valor existente.
-- ============================================================================

ALTER TABLE comprobante_electronico
    ADD COLUMN consultas_error_hacienda INT NOT NULL DEFAULT 0 AFTER intentos_envio;

ALTER TABLE comprobante_electronico
    MODIFY COLUMN estado ENUM('ACEPTADO', 'ANULADO', 'ENVIADO', 'ENVIO_INCIERTO', 'ERROR', 'ERROR_HACIENDA',
                              'FIRMADO', 'GENERADO', 'NO_TRANSMITIDO', 'PROCESANDO', 'RECHAZADO') NOT NULL;
