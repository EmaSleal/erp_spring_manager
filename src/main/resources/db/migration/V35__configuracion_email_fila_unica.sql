-- ============================================================================
-- V35: configuracion_email de fila única (ADR-027, T-022, P-045)
-- ============================================================================
-- Estado de producción: 4 filas, todas inactivas y nunca probadas. 1, 2 y 3 son
-- iguales (smtp.gmail.com, misma cuenta); la 4 tiene datos de ejemplo (el host es
-- un correo y la contraseña es la máscara "********"). La lectura por "primera
-- por id" y por "primera activa" podía elegir filas distintas. Esta migración:
--   * valida que ninguna FK apunte a configuracion_email (no hay ninguna; si
--     apareciera una, la migración no sabría reapuntarla y no modifica nada);
--   * conserva la fila válida de menor id: smtp_host sin "@" y smtp_password
--     distinta de "********" (en producción, la id 1);
--   * si ninguna es válida, conserva la de menor id y la deja inactiva;
--   * elimina las demás. Con la tabla vacía no hace nada;
--   * no modifica smtp_password (la cifra la aplicación al arrancar,
--     SmtpPasswordCifradoRunner);
--   * verifica: como mucho una fila.
-- Después, fuera del procedimiento (el DDL confirma implícitamente), agrega la
-- restricción de fila única: columna fila_unica (siempre 1) con CHECK y UNIQUE.
-- Un segundo INSERT falla por clave duplicada en uk_configuracion_email_fila_unica.
--
-- Mecanismo de configuracion_empresa (ADR-021, V32): migración que deja una fila
-- + el servicio no crea una segunda. Se aplica igual aquí
-- (ConfiguracionEmailServiceImpl) y, además, la restricción en la base. No se usa
-- CHECK sobre id_configuracion porque MySQL no admite CHECK sobre columnas
-- AUTO_INCREMENT, y la fila conservada podría no ser la id 1.
--
-- Si la migración falla, pueden quedar los procedimientos v35_*; el DROP IF
-- EXISTS inicial los recrea en el reintento.
-- ============================================================================

DROP PROCEDURE IF EXISTS v35_fallar;
DROP PROCEDURE IF EXISTS v35_configuracion_email_fila_unica;

DELIMITER //

CREATE PROCEDURE v35_fallar(IN p_msg VARCHAR(512))
BEGIN
    DECLARE v_msg VARCHAR(128);
    SET v_msg = LEFT(p_msg, 128);
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
END //

CREATE PROCEDURE v35_configuracion_email_fila_unica()
BEGIN
    DECLARE v_total     BIGINT DEFAULT 0;
    DECLARE v_conservar INT DEFAULT NULL;
    DECLARE v_texto     VARCHAR(200) DEFAULT NULL;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    -- ------------------------------------------------------------------------
    -- 1. Precondiciones (con las filas bloqueadas; no modifica nada)
    -- ------------------------------------------------------------------------
    SELECT COUNT(*) INTO v_total FROM configuracion_email FOR UPDATE;

    SET v_texto = (SELECT CONCAT(TABLE_NAME, '.', COLUMN_NAME)
                     FROM information_schema.KEY_COLUMN_USAGE
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND REFERENCED_TABLE_SCHEMA = DATABASE()
                      AND REFERENCED_TABLE_NAME = 'configuracion_email'
                    ORDER BY TABLE_NAME, COLUMN_NAME LIMIT 1);
    IF v_texto IS NOT NULL THEN
        CALL v35_fallar(CONCAT('V35 FK no prevista hacia configuracion_email: ', v_texto));
    END IF;

    -- ------------------------------------------------------------------------
    -- 2. Elegir la fila que se conserva y eliminar las demás
    -- ------------------------------------------------------------------------
    IF v_total > 0 THEN
        SELECT MIN(id_configuracion) INTO v_conservar
          FROM configuracion_email
         WHERE LOCATE('@', smtp_host) = 0
           AND smtp_password <> '********';

        IF v_conservar IS NULL THEN
            SELECT MIN(id_configuracion) INTO v_conservar FROM configuracion_email;
            UPDATE configuracion_email SET activo = 0 WHERE id_configuracion = v_conservar;
        END IF;

        DELETE FROM configuracion_email WHERE id_configuracion <> v_conservar;
    END IF;

    -- ------------------------------------------------------------------------
    -- 3. Verificación final (dentro de la transacción)
    -- ------------------------------------------------------------------------
    IF (SELECT COUNT(*) FROM configuracion_email) > 1 THEN
        CALL v35_fallar('V35 verificación: configuracion_email quedó con más de una fila');
    END IF;
    IF v_conservar IS NOT NULL
       AND (SELECT COUNT(*) FROM configuracion_email WHERE id_configuracion = v_conservar) <> 1 THEN
        CALL v35_fallar('V35 verificación: no quedó la fila elegida');
    END IF;

    COMMIT;
END //

DELIMITER ;

CALL v35_configuracion_email_fila_unica();

DROP PROCEDURE IF EXISTS v35_configuracion_email_fila_unica;
DROP PROCEDURE IF EXISTS v35_fallar;

-- ----------------------------------------------------------------------------
-- 4. Restricción de fila única: toda fila tiene fila_unica = 1 (DEFAULT + CHECK)
--    y fila_unica es UNIQUE, así que no puede haber dos filas. La aplicación no
--    mapea esta columna: los INSERT de JPA toman el DEFAULT.
-- ----------------------------------------------------------------------------
ALTER TABLE configuracion_email
    ADD COLUMN fila_unica TINYINT NOT NULL DEFAULT 1
        COMMENT 'Siempre 1: con uk_configuracion_email_fila_unica limita la tabla a una fila (ADR-027)',
    ADD CONSTRAINT ck_configuracion_email_fila_unica CHECK (fila_unica = 1),
    ADD CONSTRAINT uk_configuracion_email_fila_unica UNIQUE (fila_unica);
