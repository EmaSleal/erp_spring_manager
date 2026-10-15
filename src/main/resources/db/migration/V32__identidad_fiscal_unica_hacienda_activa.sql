-- ============================================================================
-- V32: Identidad fiscal única y una sola configuración de Hacienda activa
--      (ADR-021, T-018)
-- ============================================================================
-- Estado de producción: configuracion_empresa tiene las filas 1 y 7, y las dos
-- configuraciones de Hacienda están activas (3 SANDBOX -> empresa 7, 4 PRODUCCION
-- -> empresa 1). La emisión tomaba una cualquiera. Esta migración:
--   * valida las precondiciones (si una falla, no modifica nada):
--       - configuracion_empresa tiene exactamente las ids 1 y 7;
--       - configuracion_hacienda tiene exactamente la id 3 (SANDBOX, empresa 7)
--         y la id 4 (PRODUCCION, empresa 1);
--       - las únicas FK hacia configuracion_empresa son
--         configuracion_hacienda.empresa_id y comprobante_electronico.empresa_id;
--         si apareciera otra, la migración no sabría reapuntarla;
--   * reapunta a la id 1 todo lo que apunta a la id 7 y elimina la id 7;
--   * deja activa la id 3 (SANDBOX) e inactiva la id 4 (PRODUCCION);
--   * verifica: 1 fila en configuracion_empresa, 1 configuración de Hacienda
--     activa (id 3, SANDBOX) y ninguna FK huérfana.
-- No toca los datos de identidad de la id 1 (los corrige el usuario desde la UI)
-- ni las credenciales, rutas o consecutivos de configuracion_hacienda.
--
-- Sin DDL sobre tablas: validación, cambios y verificación van en UNA transacción
-- dentro de v32_identidad_fiscal_unica (EXIT HANDLER -> ROLLBACK + RESIGNAL). Si la
-- migración falla, pueden quedar los procedimientos v32_*; el DROP IF EXISTS
-- inicial los recrea en el reintento.
-- ============================================================================

DROP PROCEDURE IF EXISTS v32_fallar;
DROP PROCEDURE IF EXISTS v32_identidad_fiscal_unica;

DELIMITER //

CREATE PROCEDURE v32_fallar(IN p_msg VARCHAR(512))
BEGIN
    DECLARE v_msg VARCHAR(128);
    SET v_msg = LEFT(p_msg, 128);
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
END //

CREATE PROCEDURE v32_identidad_fiscal_unica()
BEGIN
    DECLARE v_empresas      BIGINT DEFAULT 0;
    DECLARE v_empresa_1     BIGINT DEFAULT 0;
    DECLARE v_empresa_7     BIGINT DEFAULT 0;
    DECLARE v_haciendas     BIGINT DEFAULT 0;
    DECLARE v_ambiente_3    VARCHAR(20) DEFAULT NULL;
    DECLARE v_empresa_de_3  BIGINT DEFAULT NULL;
    DECLARE v_ambiente_4    VARCHAR(20) DEFAULT NULL;
    DECLARE v_empresa_de_4  BIGINT DEFAULT NULL;
    DECLARE v_texto         VARCHAR(200) DEFAULT NULL;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    -- ------------------------------------------------------------------------
    -- 1. Precondiciones (con las filas bloqueadas; no modifica nada)
    -- ------------------------------------------------------------------------
    SELECT COUNT(*), COALESCE(SUM(id_configuracion = 1), 0), COALESCE(SUM(id_configuracion = 7), 0)
      INTO v_empresas, v_empresa_1, v_empresa_7
      FROM configuracion_empresa FOR UPDATE;

    IF v_empresas <> 2 OR v_empresa_1 <> 1 OR v_empresa_7 <> 1 THEN
        CALL v32_fallar(CONCAT('V32 configuracion_empresa debe tener exactamente las ids 1 y 7; tiene ',
                               v_empresas, ' fila(s); la corrección ya se aplicó o el estado cambió'));
    END IF;

    SELECT COUNT(*) INTO v_haciendas FROM configuracion_hacienda FOR UPDATE;
    SELECT ambiente, empresa_id INTO v_ambiente_3, v_empresa_de_3 FROM configuracion_hacienda WHERE id = 3;
    SELECT ambiente, empresa_id INTO v_ambiente_4, v_empresa_de_4 FROM configuracion_hacienda WHERE id = 4;

    IF v_haciendas <> 2 OR v_ambiente_3 IS NULL OR v_ambiente_4 IS NULL THEN
        CALL v32_fallar(CONCAT('V32 configuracion_hacienda debe tener exactamente las ids 3 y 4; tiene ',
                               v_haciendas, ' fila(s)'));
    END IF;
    IF v_ambiente_3 <> 'SANDBOX' OR v_empresa_de_3 <> 7 THEN
        CALL v32_fallar(CONCAT('V32 configuracion_hacienda id 3 debe ser SANDBOX de la empresa 7; es ',
                               v_ambiente_3, ' de la empresa ', v_empresa_de_3));
    END IF;
    IF v_ambiente_4 <> 'PRODUCCION' OR v_empresa_de_4 <> 1 THEN
        CALL v32_fallar(CONCAT('V32 configuracion_hacienda id 4 debe ser PRODUCCION de la empresa 1; es ',
                               v_ambiente_4, ' de la empresa ', v_empresa_de_4));
    END IF;

    SET v_texto = (SELECT CONCAT(TABLE_NAME, '.', COLUMN_NAME)
                     FROM information_schema.KEY_COLUMN_USAGE
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND REFERENCED_TABLE_SCHEMA = DATABASE()
                      AND REFERENCED_TABLE_NAME = 'configuracion_empresa'
                      AND NOT (TABLE_NAME = 'configuracion_hacienda' AND COLUMN_NAME = 'empresa_id')
                      AND NOT (TABLE_NAME = 'comprobante_electronico' AND COLUMN_NAME = 'empresa_id')
                    ORDER BY TABLE_NAME, COLUMN_NAME LIMIT 1);
    IF v_texto IS NOT NULL THEN
        CALL v32_fallar(CONCAT('V32 FK no prevista hacia configuracion_empresa: ', v_texto));
    END IF;

    -- ------------------------------------------------------------------------
    -- 2. Reapuntar a la id 1, eliminar la id 7 y dejar activa solo SANDBOX
    -- ------------------------------------------------------------------------
    UPDATE configuracion_hacienda SET empresa_id = 1 WHERE empresa_id = 7;
    UPDATE comprobante_electronico SET empresa_id = 1 WHERE empresa_id = 7;

    DELETE FROM configuracion_empresa WHERE id_configuracion = 7;

    UPDATE configuracion_hacienda SET activa = b'1', updated_at = NOW(6) WHERE id = 3;
    UPDATE configuracion_hacienda SET activa = b'0', updated_at = NOW(6) WHERE id = 4;

    -- ------------------------------------------------------------------------
    -- 3. Verificación final (dentro de la transacción)
    -- ------------------------------------------------------------------------
    IF (SELECT COUNT(*) FROM configuracion_empresa) <> 1
       OR (SELECT COUNT(*) FROM configuracion_empresa WHERE id_configuracion = 1) <> 1 THEN
        CALL v32_fallar('V32 verificación: configuracion_empresa no quedó con la única fila id 1');
    END IF;

    IF (SELECT COUNT(*) FROM configuracion_hacienda WHERE activa = b'1') <> 1
       OR (SELECT COUNT(*) FROM configuracion_hacienda WHERE activa = b'1' AND id = 3 AND ambiente = 'SANDBOX') <> 1 THEN
        CALL v32_fallar('V32 verificación: no quedó exactamente una configuración de Hacienda activa (id 3, SANDBOX)');
    END IF;

    IF EXISTS (SELECT 1 FROM configuracion_hacienda h
                LEFT JOIN configuracion_empresa e ON e.id_configuracion = h.empresa_id
               WHERE e.id_configuracion IS NULL)
       OR EXISTS (SELECT 1 FROM comprobante_electronico c
                   LEFT JOIN configuracion_empresa e ON e.id_configuracion = c.empresa_id
                  WHERE c.empresa_id IS NOT NULL AND e.id_configuracion IS NULL) THEN
        CALL v32_fallar('V32 verificación: hay referencias a una configuracion_empresa inexistente');
    END IF;

    COMMIT;
END //

DELIMITER ;

CALL v32_identidad_fiscal_unica();

DROP PROCEDURE IF EXISTS v32_identidad_fiscal_unica;
DROP PROCEDURE IF EXISTS v32_fallar;
