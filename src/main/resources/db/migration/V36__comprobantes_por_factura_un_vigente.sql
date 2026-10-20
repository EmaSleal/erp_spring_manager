-- ============================================================================
-- V36: comprobante_electronico 1:N con un solo vigente por factura y estados de
-- transmisión NO_TRANSMITIDO / ENVIO_INCIERTO (ADR-030, T-025)
-- ============================================================================
-- Estado previo (dump de producción): la relación factura → comprobante era 1:1
-- por el UNIQUE KEY `UKmhj4c2o5tfm11ddetubtu7fq4` (factura_id), que Hibernate
-- creó a partir del @OneToOne; ninguna migración lo había declarado. Producción
-- no tiene comprobantes (consulta del 2026-10-17), pero la migración es segura
-- con datos: como el UNIQUE previo garantizaba como mucho un comprobante por
-- factura, todos los existentes quedan vigentes sin conflicto.
--
-- Esta migración:
--   1) asegura el índice simple idx_factura (lo usa la FK hacia factura) y quita
--      el UNIQUE de Hibernate sobre factura_id, si existe;
--   2) agrega `vigente` (todos los existentes quedan vigentes);
--   3) garantiza en la BD como máximo un vigente por factura: columna generada
--      factura_vigente_id = factura_id si vigente, NULL si no, con UNIQUE (MySQL
--      admite varios NULL en un índice único, así que el historial no choca);
--   4) amplía el ENUM de `estado` con NO_TRANSMITIDO y ENVIO_INCIERTO.
-- Sin procedimientos (ADR-028): los pasos condicionales usan sentencias preparadas.
-- No modifica ninguna fila existente salvo el valor por defecto de `vigente`.
-- ============================================================================

-- 1) Índice simple sobre factura_id (por si un entorno no tuviera idx_factura)
SET @falta_idx := (SELECT COUNT(*) = 0 FROM information_schema.statistics
                   WHERE table_schema = DATABASE() AND table_name = 'comprobante_electronico'
                     AND index_name = 'idx_factura');
SET @sql := IF(@falta_idx, 'CREATE INDEX idx_factura ON comprobante_electronico (factura_id)', 'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 1b) UNIQUE de Hibernate sobre factura_id (nombre verificado en el dump)
SET @hay_uk := (SELECT COUNT(*) > 0 FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = 'comprobante_electronico'
                  AND index_name = 'UKmhj4c2o5tfm11ddetubtu7fq4');
SET @sql := IF(@hay_uk, 'ALTER TABLE comprobante_electronico DROP INDEX `UKmhj4c2o5tfm11ddetubtu7fq4`', 'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) y 3) Comprobante vigente, con unicidad por factura en la BD
ALTER TABLE comprobante_electronico
    ADD COLUMN vigente BIT(1) NOT NULL DEFAULT b'1' AFTER factura_id,
    ADD COLUMN factura_vigente_id INT GENERATED ALWAYS AS (IF(vigente = b'1', factura_id, NULL)) STORED,
    ADD UNIQUE KEY uk_comprobante_un_vigente_por_factura (factura_vigente_id);

-- 4) Estados de transmisión de ADR-030
ALTER TABLE comprobante_electronico
    MODIFY COLUMN estado ENUM('ACEPTADO', 'ANULADO', 'ENVIADO', 'ENVIO_INCIERTO', 'ERROR', 'FIRMADO',
                              'GENERADO', 'NO_TRANSMITIDO', 'PROCESANDO', 'RECHAZADO') NOT NULL;
