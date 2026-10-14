-- ============================================================================
-- V30: Cuentas bancarias con hoja contable propia (ADR-018, T-015)
-- ============================================================================
-- Cada cuenta bancaria de la empresa tiene su propia cuenta hoja bajo
-- 1.1.01.002 Bancos, que pasa a ser agrupadora:
--   * 1.1.01.002 -> acepta_movimientos = false (agrupadora).
--   * Una hoja por fila de cuenta_bancaria, ordenadas por id_cuenta_bancaria:
--     1.1.01.002.001, 1.1.01.002.002, ... ACTIVO/DEUDORA, nivel 5, nombre
--     "<entidad> <moneda>", padre 1.1.01.002, activa = la de la cuenta bancaria.
--   * cuenta_bancaria.cuenta_contable_id (FK, UNIQUE, NOT NULL tras enlazar).
--   * pagos.cuenta_bancaria_id (cuenta de destino de un cobro por transferencia)
--     y pagos_proveedor.cuenta_bancaria_id (cuenta de origen de un pago bancario):
--     FK a cuenta_bancaria, NULL.
--   * Se eliminan las claves COBRO_CAJA_TRANSFERENCIA y CPP_BANCO_TRANSFERENCIA,
--     CPP_BANCO_SINPE, CPP_BANCO_DEPOSITO, CPP_BANCO_CHEQUE: esos medios usan la
--     hoja de la cuenta bancaria elegida en el pago, sin respaldo por clave.
--
-- DDL e implicit commit: en MySQL cada ALTER TABLE confirma la transacción en
-- curso, así que el DDL no puede ir dentro de la transacción de datos. Orden:
--   1. v30_validar: todas las precondiciones, sin modificar nada. Si una falla,
--      la migración se detiene antes de cualquier cambio (datos intactos).
--   2. DDL idempotente (v30_agregar_columna / v30_agregar_restriccion revisan
--      information_schema): solo agrega columnas NULL y restricciones, que no
--      pueden fallar por los datos existentes; si el paso 3 fallara, reintentar
--      la migración no choca con columnas ya creadas.
--   3. v30_cuentas_bancarias_contables: todo el cambio de datos en UNA
--      transacción (EXIT HANDLER -> ROLLBACK + RESIGNAL), con la verificación
--      final dentro de la misma transacción.
--   4. cuenta_bancaria.cuenta_contable_id pasa a NOT NULL (solo se llega aquí si
--      la verificación del paso 3 confirmó que todas las filas quedaron enlazadas).
-- ============================================================================

DROP PROCEDURE IF EXISTS v30_fallar;
DROP PROCEDURE IF EXISTS v30_validar;
DROP PROCEDURE IF EXISTS v30_agregar_columna;
DROP PROCEDURE IF EXISTS v30_agregar_restriccion;
DROP PROCEDURE IF EXISTS v30_cuentas_bancarias_contables;

DELIMITER //

CREATE PROCEDURE v30_fallar(IN p_msg VARCHAR(512))
BEGIN
    DECLARE v_msg VARCHAR(128);
    SET v_msg = LEFT(p_msg, 128);
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
END //

-- ----------------------------------------------------------------------------
-- 1. Precondiciones (no modifica nada)
-- ----------------------------------------------------------------------------
CREATE PROCEDURE v30_validar()
BEGIN
    DECLARE v_bancos_id BIGINT DEFAULT NULL;
    DECLARE v_tipo      VARCHAR(20) DEFAULT NULL;
    DECLARE v_detalles  BIGINT DEFAULT 0;
    DECLARE v_texto     VARCHAR(200) DEFAULT NULL;

    SELECT id, tipo INTO v_bancos_id, v_tipo FROM cuentas_contables WHERE codigo = '1.1.01.002';
    IF v_bancos_id IS NULL THEN
        CALL v30_fallar('V30 cuenta 1.1.01.002 (Bancos) no existe');
    END IF;
    IF v_tipo <> 'ACTIVO' THEN
        CALL v30_fallar(CONCAT('V30 cuenta 1.1.01.002 es de tipo ', v_tipo, '; se esperaba ACTIVO'));
    END IF;

    SET v_detalles = (SELECT COUNT(*) FROM detalles_asiento WHERE cuenta_id = v_bancos_id);
    IF v_detalles > 0 THEN
        CALL v30_fallar(CONCAT('V30 cuenta 1.1.01.002 (Bancos) tiene ', v_detalles,
                               ' detalle(s) de asiento; no puede pasar a agrupadora'));
    END IF;

    SET v_texto = (SELECT codigo FROM cuentas_contables
                    WHERE cuenta_padre_id = v_bancos_id OR codigo LIKE '1.1.01.002.%'
                    ORDER BY codigo LIMIT 1);
    IF v_texto IS NOT NULL THEN
        CALL v30_fallar(CONCAT('V30 la cuenta ', v_texto, ' ya existe bajo 1.1.01.002; se esperaba sin subcuentas'));
    END IF;

    -- Ninguna clave que se conserva puede apuntar a 1.1.01.002 (quedaría apuntando a una agrupadora)
    SET v_texto = (SELECT clave FROM parametros_contables
                    WHERE cuenta_contable_id = v_bancos_id
                      AND clave NOT IN ('COBRO_CAJA_TRANSFERENCIA', 'CPP_BANCO_TRANSFERENCIA', 'CPP_BANCO_SINPE',
                                        'CPP_BANCO_DEPOSITO', 'CPP_BANCO_CHEQUE')
                    ORDER BY clave LIMIT 1);
    IF v_texto IS NOT NULL THEN
        CALL v30_fallar(CONCAT('V30 clave ', v_texto, ' apunta a 1.1.01.002, que pasa a agrupadora'));
    END IF;

    -- Toda cuenta bancaria necesita entidad y moneda para nombrar su hoja
    SET v_texto = (SELECT CAST(id_cuenta_bancaria AS CHAR) FROM cuenta_bancaria
                    WHERE entidad IS NULL OR TRIM(entidad) = '' OR moneda IS NULL OR TRIM(moneda) = ''
                    ORDER BY id_cuenta_bancaria LIMIT 1);
    IF v_texto IS NOT NULL THEN
        CALL v30_fallar(CONCAT('V30 cuenta bancaria id ', v_texto, ' sin entidad o sin moneda'));
    END IF;

    IF (SELECT COUNT(*) FROM cuenta_bancaria) > 999 THEN
        CALL v30_fallar('V30 hay más de 999 cuentas bancarias; el código de hoja admite 3 dígitos');
    END IF;
END //

-- ----------------------------------------------------------------------------
-- 2. DDL idempotente
-- ----------------------------------------------------------------------------
CREATE PROCEDURE v30_agregar_columna(IN p_tabla VARCHAR(64), IN p_columna VARCHAR(64), IN p_definicion VARCHAR(200))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_tabla AND COLUMN_NAME = p_columna) THEN
        SET @v30_sql = CONCAT('ALTER TABLE `', p_tabla, '` ADD COLUMN `', p_columna, '` ', p_definicion);
        PREPARE v30_stmt FROM @v30_sql;
        EXECUTE v30_stmt;
        DEALLOCATE PREPARE v30_stmt;
    END IF;
END //

CREATE PROCEDURE v30_agregar_restriccion(IN p_tabla VARCHAR(64), IN p_nombre VARCHAR(64), IN p_definicion VARCHAR(300))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_tabla AND CONSTRAINT_NAME = p_nombre) THEN
        SET @v30_sql = CONCAT('ALTER TABLE `', p_tabla, '` ADD CONSTRAINT `', p_nombre, '` ', p_definicion);
        PREPARE v30_stmt FROM @v30_sql;
        EXECUTE v30_stmt;
        DEALLOCATE PREPARE v30_stmt;
    END IF;
END //

-- ----------------------------------------------------------------------------
-- 3. Cambio de datos en una sola transacción
-- ----------------------------------------------------------------------------
CREATE PROCEDURE v30_cuentas_bancarias_contables()
BEGIN
    DECLARE v_bancos_id BIGINT DEFAULT NULL;
    DECLARE v_clave     VARCHAR(100) DEFAULT NULL;
    DECLARE v_codigo    VARCHAR(20) DEFAULT NULL;
    DECLARE v_motivo    VARCHAR(40) DEFAULT NULL;
    DECLARE v_texto     VARCHAR(200) DEFAULT NULL;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    SET v_bancos_id = (SELECT id FROM cuentas_contables WHERE codigo = '1.1.01.002');

    -- 3.1 Bancos pasa a agrupadora
    UPDATE cuentas_contables
       SET acepta_movimientos = false, fecha_modificacion = NOW()
     WHERE id = v_bancos_id;

    -- 3.2 Una hoja por cuenta bancaria, en orden de id
    DROP TEMPORARY TABLE IF EXISTS v30_hojas;
    CREATE TEMPORARY TABLE v30_hojas (
        id_cuenta_bancaria INT PRIMARY KEY,
        codigo             VARCHAR(20) NOT NULL
    );
    INSERT INTO v30_hojas (id_cuenta_bancaria, codigo)
        SELECT id_cuenta_bancaria,
               CONCAT('1.1.01.002.', LPAD(ROW_NUMBER() OVER (ORDER BY id_cuenta_bancaria), 3, '0'))
          FROM cuenta_bancaria;

    INSERT INTO cuentas_contables
        (codigo, nombre, tipo, naturaleza, nivel, activa, acepta_movimientos, cuenta_padre_id,
         descripcion, creado_por, fecha_creacion, modificado_por, fecha_modificacion)
    SELECT h.codigo,
           LEFT(CONCAT(TRIM(cb.entidad), ' ', TRIM(cb.moneda)), 200),
           'ACTIVO', 'DEUDORA', 5, cb.activa, true, v_bancos_id,
           LEFT(CONCAT('Cuenta bancaria ', TRIM(cb.entidad),
                       COALESCE(CONCAT(' — ', NULLIF(TRIM(cb.cuenta_iban), '')), ''), ' (ADR-018)'), 500),
           NULL, NOW(), NULL, NULL
      FROM v30_hojas h
      JOIN cuenta_bancaria cb ON cb.id_cuenta_bancaria = h.id_cuenta_bancaria
     ORDER BY h.codigo;

    -- 3.3 Enlace cuenta bancaria -> hoja
    UPDATE cuenta_bancaria cb
      JOIN v30_hojas h ON h.id_cuenta_bancaria = cb.id_cuenta_bancaria
      JOIN cuentas_contables c ON c.codigo = h.codigo
       SET cb.cuenta_contable_id = c.id;

    DROP TEMPORARY TABLE IF EXISTS v30_hojas;

    -- 3.4 Claves de medios bancarios: sin respaldo, el medio usa la hoja de su cuenta bancaria
    DELETE FROM parametros_contables
     WHERE clave IN ('COBRO_CAJA_TRANSFERENCIA', 'CPP_BANCO_TRANSFERENCIA', 'CPP_BANCO_SINPE',
                     'CPP_BANCO_DEPOSITO', 'CPP_BANCO_CHEQUE');

    -- ------------------------------------------------------------------------
    -- 3.5 Verificación final
    -- ------------------------------------------------------------------------
    -- Ninguna clave apunta a una cuenta con hijas, inactiva o sin movimientos
    SELECT p.clave, c.codigo,
           CASE WHEN EXISTS (SELECT 1 FROM cuentas_contables h WHERE h.cuenta_padre_id = c.id) THEN 'tiene hijas'
                WHEN c.activa = false THEN 'está inactiva'
                ELSE 'no acepta movimientos' END
      INTO v_clave, v_codigo, v_motivo
      FROM parametros_contables p
      JOIN cuentas_contables c ON c.id = p.cuenta_contable_id
     WHERE c.activa = false
        OR c.acepta_movimientos = false
        OR EXISTS (SELECT 1 FROM cuentas_contables h WHERE h.cuenta_padre_id = c.id)
     ORDER BY p.clave
     LIMIT 1;
    IF v_clave IS NOT NULL THEN
        CALL v30_fallar(CONCAT('V30 clave ', v_clave, ' apunta a la cuenta ', v_codigo, ', que ', v_motivo,
                               '; solo puede apuntar a una hoja'));
    END IF;

    -- Toda cuenta bancaria tiene su propia hoja bajo 1.1.01.002
    SET v_texto = (SELECT CAST(cb.id_cuenta_bancaria AS CHAR)
                     FROM cuenta_bancaria cb
                     LEFT JOIN cuentas_contables c ON c.id = cb.cuenta_contable_id
                    WHERE c.id IS NULL
                       OR c.cuenta_padre_id <> v_bancos_id
                       OR c.acepta_movimientos = false
                       OR EXISTS (SELECT 1 FROM cuentas_contables h WHERE h.cuenta_padre_id = c.id)
                    ORDER BY cb.id_cuenta_bancaria LIMIT 1);
    IF v_texto IS NOT NULL THEN
        CALL v30_fallar(CONCAT('V30 cuenta bancaria id ', v_texto, ' no quedó enlazada a una hoja propia bajo 1.1.01.002'));
    END IF;
    SET v_texto = (SELECT CAST(cuenta_contable_id AS CHAR) FROM cuenta_bancaria
                    GROUP BY cuenta_contable_id HAVING COUNT(*) > 1 LIMIT 1);
    IF v_texto IS NOT NULL THEN
        CALL v30_fallar(CONCAT('V30 la cuenta contable id ', v_texto, ' quedó enlazada a varias cuentas bancarias'));
    END IF;

    COMMIT;
END //

DELIMITER ;

-- 1. Precondiciones
CALL v30_validar();

-- 2. DDL (columnas NULL y restricciones)
CALL v30_agregar_columna('cuenta_bancaria', 'cuenta_contable_id', 'BIGINT NULL');
CALL v30_agregar_restriccion('cuenta_bancaria', 'uk_cuenta_bancaria_cuenta_contable', 'UNIQUE (cuenta_contable_id)');
CALL v30_agregar_restriccion('cuenta_bancaria', 'fk_cuenta_bancaria_cuenta_contable',
                             'FOREIGN KEY (cuenta_contable_id) REFERENCES cuentas_contables (id)');
CALL v30_agregar_columna('pagos', 'cuenta_bancaria_id', 'INT NULL');
CALL v30_agregar_restriccion('pagos', 'fk_pagos_cuenta_bancaria',
                             'FOREIGN KEY (cuenta_bancaria_id) REFERENCES cuenta_bancaria (id_cuenta_bancaria)');
CALL v30_agregar_columna('pagos_proveedor', 'cuenta_bancaria_id', 'INT NULL');
CALL v30_agregar_restriccion('pagos_proveedor', 'fk_pagos_proveedor_cuenta_bancaria',
                             'FOREIGN KEY (cuenta_bancaria_id) REFERENCES cuenta_bancaria (id_cuenta_bancaria)');

-- 3. Datos en una transacción
CALL v30_cuentas_bancarias_contables();

-- 4. Toda cuenta bancaria debe tener hoja (verificado en el paso 3)
ALTER TABLE cuenta_bancaria MODIFY cuenta_contable_id BIGINT NOT NULL;

DROP PROCEDURE IF EXISTS v30_cuentas_bancarias_contables;
DROP PROCEDURE IF EXISTS v30_agregar_restriccion;
DROP PROCEDURE IF EXISTS v30_agregar_columna;
DROP PROCEDURE IF EXISTS v30_validar;
DROP PROCEDURE IF EXISTS v30_fallar;
