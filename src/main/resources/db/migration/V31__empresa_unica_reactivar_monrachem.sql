-- ============================================================================
-- V31: Empresa única — reactivar Monrachem y eliminar el placeholder (ADR-019, T-016)
-- ============================================================================
-- En producción la empresa activa es la id 2 "Mi Empresa S.A.C.", creada en
-- silencio por getEmpresaPrincipal() el 2026-07-03 porque la id 1 (Monrachem)
-- había quedado inactiva. Esta migración:
--   * valida las precondiciones (si una falla, no modifica nada):
--       - existen exactamente las filas de empresa 1 y 2;
--       - solo la 2 está activa;
--       - la 2 es el placeholder (nombre_empresa = 'Mi Empresa S.A.C.');
--       - la única FK hacia empresa es cuenta_bancaria.empresa_id (diagnóstico
--         T-016); si apareciera otra, la migración no sabría reapuntarla;
--   * copia de la id 2 a la id 1 sello_tipo_empresa, texto_legal,
--     descripcion_actividad, logo y favicon cuando la id 2 tiene un valor no
--     vacío (la id 2 gana: es lo último que cargó el usuario); si no, conserva el
--     de la id 1. La identidad (nombre, identificación, dirección) es la de la id 1;
--   * reapunta a la id 1 las cuentas bancarias de la id 2;
--   * activa la id 1 y elimina la id 2;
--   * verifica: exactamente una empresa, activa, con id 1, y ninguna cuenta
--     bancaria apuntando a una empresa inexistente.
--
-- Sin DDL sobre tablas: validación, cambios y verificación van en UNA transacción
-- dentro de v31_empresa_unica (EXIT HANDLER -> ROLLBACK + RESIGNAL). Los CREATE/DROP
-- PROCEDURE confirman implícitamente, pero ocurren antes y después de esa
-- transacción y no tocan datos. Si la migración falla, pueden quedar los
-- procedimientos v31_*; el DROP IF EXISTS inicial los recrea en el reintento.
--
-- Después de desplegar, reiniciar la aplicación: la caché "empresa" guarda el
-- placeholder.
-- ============================================================================

DROP PROCEDURE IF EXISTS v31_fallar;
DROP PROCEDURE IF EXISTS v31_empresa_unica;

DELIMITER //

CREATE PROCEDURE v31_fallar(IN p_msg VARCHAR(512))
BEGIN
    DECLARE v_msg VARCHAR(128);
    SET v_msg = LEFT(p_msg, 128);
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
END //

CREATE PROCEDURE v31_empresa_unica()
BEGIN
    DECLARE v_total     BIGINT DEFAULT 0;
    DECLARE v_activas   BIGINT DEFAULT 0;
    DECLARE v_activa_1  BIGINT DEFAULT NULL;
    DECLARE v_activa_2  BIGINT DEFAULT NULL;
    DECLARE v_nombre_2  VARCHAR(200) DEFAULT NULL;
    DECLARE v_texto     VARCHAR(200) DEFAULT NULL;

    DECLARE v_sello_2   VARCHAR(255) DEFAULT NULL;
    DECLARE v_legal_2   TEXT DEFAULT NULL;
    DECLARE v_desc_2    VARCHAR(200) DEFAULT NULL;
    DECLARE v_logo_2    VARCHAR(255) DEFAULT NULL;
    DECLARE v_favicon_2 VARCHAR(255) DEFAULT NULL;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    -- ------------------------------------------------------------------------
    -- 1. Precondiciones (con las filas bloqueadas; no modifica nada)
    -- ------------------------------------------------------------------------
    SELECT COUNT(*), COALESCE(SUM(activo = b'1'), 0)
      INTO v_total, v_activas
      FROM empresa FOR UPDATE;

    SELECT activo + 0 INTO v_activa_1 FROM empresa WHERE id_empresa = 1;
    SELECT activo + 0, nombre_empresa INTO v_activa_2, v_nombre_2 FROM empresa WHERE id_empresa = 2;

    IF v_activa_1 IS NULL OR v_activa_2 IS NULL THEN
        CALL v31_fallar('V31 no existen las empresas id 1 e id 2; no se aplica la corrección');
    END IF;
    IF v_total <> 2 THEN
        CALL v31_fallar(CONCAT('V31 la tabla empresa tiene ', v_total, ' filas; se esperaban exactamente 2 (ids 1 y 2)'));
    END IF;
    IF v_activa_1 <> 0 THEN
        CALL v31_fallar('V31 la empresa id 1 ya está activa; la corrección ya se aplicó o el estado cambió');
    END IF;
    IF v_activa_2 <> 1 OR v_activas <> 1 THEN
        CALL v31_fallar(CONCAT('V31 se esperaba solo la empresa id 2 activa; activas = ', v_activas));
    END IF;
    IF v_nombre_2 <> 'Mi Empresa S.A.C.' THEN
        CALL v31_fallar(CONCAT('V31 la empresa id 2 no es el placeholder: ', LEFT(v_nombre_2, 60)));
    END IF;

    SET v_texto = (SELECT CONCAT(TABLE_NAME, '.', COLUMN_NAME)
                     FROM information_schema.KEY_COLUMN_USAGE
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND REFERENCED_TABLE_SCHEMA = DATABASE()
                      AND REFERENCED_TABLE_NAME = 'empresa'
                      AND NOT (TABLE_NAME = 'cuenta_bancaria' AND COLUMN_NAME = 'empresa_id')
                    ORDER BY TABLE_NAME, COLUMN_NAME LIMIT 1);
    IF v_texto IS NOT NULL THEN
        CALL v31_fallar(CONCAT('V31 FK no prevista hacia empresa: ', v_texto));
    END IF;

    -- ------------------------------------------------------------------------
    -- 2. Configuración cargada en el placeholder: la id 2 gana si tiene valor
    -- ------------------------------------------------------------------------
    SELECT sello_tipo_empresa, texto_legal, descripcion_actividad, logo, favicon
      INTO v_sello_2, v_legal_2, v_desc_2, v_logo_2, v_favicon_2
      FROM empresa WHERE id_empresa = 2;

    UPDATE empresa
       SET sello_tipo_empresa    = IF(NULLIF(TRIM(v_sello_2), '')   IS NOT NULL, v_sello_2,   sello_tipo_empresa),
           texto_legal           = IF(NULLIF(TRIM(v_legal_2), '')   IS NOT NULL, v_legal_2,   texto_legal),
           descripcion_actividad = IF(NULLIF(TRIM(v_desc_2), '')    IS NOT NULL, v_desc_2,    descripcion_actividad),
           logo                  = IF(NULLIF(TRIM(v_logo_2), '')    IS NOT NULL, v_logo_2,    logo),
           favicon               = IF(NULLIF(TRIM(v_favicon_2), '') IS NOT NULL, v_favicon_2, favicon)
     WHERE id_empresa = 1;

    -- ------------------------------------------------------------------------
    -- 3. Reapuntar referencias, activar la id 1 y eliminar el placeholder
    -- ------------------------------------------------------------------------
    UPDATE cuenta_bancaria SET empresa_id = 1 WHERE empresa_id = 2;

    UPDATE empresa SET activo = b'1', update_date = NOW(6) WHERE id_empresa = 1;

    DELETE FROM empresa WHERE id_empresa = 2;

    -- ------------------------------------------------------------------------
    -- 4. Verificación final (dentro de la transacción)
    -- ------------------------------------------------------------------------
    IF (SELECT COUNT(*) FROM empresa) <> 1
       OR (SELECT COUNT(*) FROM empresa WHERE activo = b'1') <> 1
       OR (SELECT COUNT(*) FROM empresa WHERE id_empresa = 1 AND activo = b'1') <> 1 THEN
        CALL v31_fallar('V31 verificación: no quedó exactamente una empresa activa con id 1');
    END IF;

    IF EXISTS (SELECT 1 FROM cuenta_bancaria cb
                LEFT JOIN empresa e ON e.id_empresa = cb.empresa_id
               WHERE cb.empresa_id IS NOT NULL AND e.id_empresa IS NULL) THEN
        CALL v31_fallar('V31 verificación: hay cuentas bancarias apuntando a una empresa inexistente');
    END IF;

    COMMIT;
END //

DELIMITER ;

CALL v31_empresa_unica();

DROP PROCEDURE IF EXISTS v31_empresa_unica;
DROP PROCEDURE IF EXISTS v31_fallar;
