-- ============================================================================
-- V29: Plan de cuentas coherente (ADR-017, T-014)
-- ============================================================================
-- Reorganiza el plan de cuentas existente en un árbol coherente:
--   R1. Solo las hojas (activas, con movimientos y sin hijas) reciben movimientos;
--       toda cuenta con hijas es agrupadora (acepta_movimientos = false) y un
--       parámetro contable solo puede apuntar a una hoja.
--   R2. Un medio de pago usa la misma cuenta para cobrar y para pagar
--       (CPP_BANCO_EFECTIVO -> 1.1.01.001; CPP_BANCO_* bancarios -> 1.1.01.002).
--   R3. cuenta_padre_id = cuenta cuyo código es el prefijo hasta el último punto;
--       nivel = número de segmentos del código.
--
-- Validada y con falla rápida (mismo estilo que V19/V27):
--   1. Precondiciones, antes de modificar nada:
--      * Las cuentas que se recodifican o cambian de rol se identifican por id
--        (1..11) y su código actual debe ser el del inventario de T-013.
--      * Ninguna de esas cuentas (ids 1, 2, 4, 5, 6, 7, 8, 9, 10, 11) puede tener
--        detalles_asiento.
--      * Ningún código destino de una recodificación puede existir con otro id.
--      * Las cuentas nuevas que ya existan por código deben tener el tipo y la
--        naturaleza esperados (se reutilizan tal cual).
--      * Las claves CPP_BANCO_* que se reasignan deben existir.
--   2. Agrupadoras nuevas (acepta_movimientos = false).
--   3. Hojas nuevas: 3.1.01, 3.2.01, 5.1.02.003.
--   4. Recodificación y renombre conservando los ids.
--   5. Recalculo de cuenta_padre_id y nivel para TODAS las cuentas.
--   6. acepta_movimientos = false en toda cuenta con hijas.
--   7. Parámetros: CPP_BANCO_*, nueva clave NOMINA_GASTO_CARGAS_SOCIALES y
--      descripciones con codificación rota.
--   8. Verificación final: ninguna clave apunta a una cuenta con hijas, inactiva
--      o sin movimientos; toda cuenta de varios segmentos tiene padre.
--      (La comprobación de claves del paso 6 se hace aquí, tras reasignar las
--      claves CPP_BANCO_* del paso 7, que antes apuntaban a 1.1.01 y 1.1.02.)
--
-- Todo el cambio de datos se ejecuta en una sola transacción dentro del
-- procedimiento: si una validación falla, se revierte y se relanza el error con
-- un mensaje que nombra la cuenta o la clave.
-- ============================================================================

DROP PROCEDURE IF EXISTS v29_fallar;
DROP PROCEDURE IF EXISTS v29_validar_cuenta_existente;
DROP PROCEDURE IF EXISTS v29_asegurar_cuenta;
DROP PROCEDURE IF EXISTS v29_plan_de_cuentas_coherente;

DELIMITER //

CREATE PROCEDURE v29_fallar(IN p_msg VARCHAR(512))
BEGIN
    DECLARE v_msg VARCHAR(128);
    SET v_msg = LEFT(p_msg, 128);
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
END //

-- Cuenta que se recodifica o cambia de rol: debe existir con el código del inventario,
-- sin detalles de asiento (si p_sin_detalles) y su código destino no puede existir con otro id.
CREATE PROCEDURE v29_validar_cuenta_existente(
    IN p_id             BIGINT,
    IN p_codigo_actual  VARCHAR(20),
    IN p_codigo_destino VARCHAR(20),
    IN p_sin_detalles   BOOLEAN
)
BEGIN
    DECLARE v_codigo   VARCHAR(20) DEFAULT NULL;
    DECLARE v_nombre   VARCHAR(200) DEFAULT NULL;
    DECLARE v_detalles BIGINT DEFAULT 0;
    DECLARE v_otro_id  BIGINT DEFAULT NULL;

    SELECT codigo, nombre INTO v_codigo, v_nombre FROM cuentas_contables WHERE id = p_id;

    IF v_codigo IS NULL THEN
        CALL v29_fallar(CONCAT('V29 cuenta id ', p_id, ' (', p_codigo_actual, '): no existe'));
    END IF;
    IF v_codigo <> p_codigo_actual THEN
        CALL v29_fallar(CONCAT('V29 cuenta id ', p_id, ': código actual ', v_codigo,
                               ', esperado ', p_codigo_actual));
    END IF;

    IF p_sin_detalles THEN
        SET v_detalles = (SELECT COUNT(*) FROM detalles_asiento WHERE cuenta_id = p_id);
        IF v_detalles > 0 THEN
            CALL v29_fallar(CONCAT('V29 cuenta ', v_codigo, ' (', LEFT(v_nombre, 30), ', id ', p_id, ') tiene ',
                                   v_detalles, ' detalle(s) de asiento; no se puede reorganizar'));
        END IF;
    END IF;

    IF p_codigo_destino IS NOT NULL AND p_codigo_destino <> p_codigo_actual THEN
        SET v_otro_id = (SELECT id FROM cuentas_contables WHERE codigo = p_codigo_destino AND id <> p_id);
        IF v_otro_id IS NOT NULL THEN
            CALL v29_fallar(CONCAT('V29 cuenta ', v_codigo, ' (id ', p_id, '): el código destino ',
                                   p_codigo_destino, ' ya existe con id ', v_otro_id));
        END IF;
    END IF;
END //

-- Cuenta nueva del árbol: si el código no existe se crea; si existe debe tener el
-- tipo/naturaleza esperados y se reutiliza tal cual.
CREATE PROCEDURE v29_asegurar_cuenta(
    IN p_codigo      VARCHAR(20),
    IN p_nombre      VARCHAR(200),
    IN p_tipo        VARCHAR(20),
    IN p_naturaleza  VARCHAR(20),
    IN p_acepta_mov  BOOLEAN,
    IN p_descripcion VARCHAR(500)
)
BEGIN
    DECLARE v_id         BIGINT DEFAULT NULL;
    DECLARE v_tipo       VARCHAR(20);
    DECLARE v_naturaleza VARCHAR(20);

    SELECT id, tipo, naturaleza INTO v_id, v_tipo, v_naturaleza
      FROM cuentas_contables WHERE codigo = p_codigo;

    IF v_id IS NULL THEN
        INSERT INTO cuentas_contables
            (codigo, nombre, tipo, naturaleza, nivel, activa, acepta_movimientos, cuenta_padre_id,
             descripcion, creado_por, fecha_creacion, modificado_por, fecha_modificacion)
        VALUES
            (p_codigo, p_nombre, p_tipo, p_naturaleza,
             LENGTH(p_codigo) - LENGTH(REPLACE(p_codigo, '.', '')) + 1,
             true, p_acepta_mov, NULL, p_descripcion, NULL, NOW(), NULL, NULL);
    ELSEIF v_tipo <> p_tipo OR v_naturaleza <> p_naturaleza THEN
        CALL v29_fallar(CONCAT('V29 cuenta ', p_codigo, ' ya existe como ', v_tipo, '/', v_naturaleza,
                               '; esperado ', p_tipo, '/', p_naturaleza));
    END IF;
END //

CREATE PROCEDURE v29_plan_de_cuentas_coherente()
BEGIN
    DECLARE v_codigo VARCHAR(20) DEFAULT NULL;
    DECLARE v_clave  VARCHAR(100) DEFAULT NULL;
    DECLARE v_motivo VARCHAR(40) DEFAULT NULL;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    -- ------------------------------------------------------------------------
    -- 1. Precondiciones (id, código actual del inventario, código destino)
    -- ------------------------------------------------------------------------
    CALL v29_validar_cuenta_existente(1,  '1.1.01',     '1.1.01',     true);
    CALL v29_validar_cuenta_existente(2,  '1.1.02',     '1.1.02',     true);
    CALL v29_validar_cuenta_existente(3,  '1.1.03',     '1.1.03',     false);
    CALL v29_validar_cuenta_existente(4,  '2.1.01',     '2.1.01',     true);
    CALL v29_validar_cuenta_existente(5,  '5.1.01',     '5.1.01',     true);
    CALL v29_validar_cuenta_existente(6,  '5.1.01.001', '5.1.02.002', true);
    CALL v29_validar_cuenta_existente(7,  '2.1.01.001', '2.1.04.003', true);
    CALL v29_validar_cuenta_existente(8,  '2.1.01.002', '2.1.04.004', true);
    CALL v29_validar_cuenta_existente(9,  '2.1.01.003', '2.1.04.005', true);
    CALL v29_validar_cuenta_existente(10, '2.1.01.004', '2.1.04.006', true);
    CALL v29_validar_cuenta_existente(11, '2.1.01.005', '2.1.02.002', true);

    SET v_clave = (SELECT k.clave
                     FROM (SELECT 'CPP_BANCO_EFECTIVO' AS clave UNION ALL SELECT 'CPP_BANCO_TRANSFERENCIA'
                           UNION ALL SELECT 'CPP_BANCO_SINPE' UNION ALL SELECT 'CPP_BANCO_DEPOSITO'
                           UNION ALL SELECT 'CPP_BANCO_CHEQUE') k
                    WHERE NOT EXISTS (SELECT 1 FROM parametros_contables p WHERE p.clave = k.clave)
                    ORDER BY k.clave LIMIT 1);
    IF v_clave IS NOT NULL THEN
        CALL v29_fallar(CONCAT('V29 clave ', v_clave, ': no existe en parametros_contables'));
    END IF;

    -- Las hojas destino de CPP_BANCO_* deben existir (las creó V19)
    IF NOT EXISTS (SELECT 1 FROM cuentas_contables WHERE codigo = '1.1.01.001') THEN
        CALL v29_fallar('V29 cuenta 1.1.01.001 (Caja General) no existe; requerida por CPP_BANCO_EFECTIVO');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM cuentas_contables WHERE codigo = '1.1.01.002') THEN
        CALL v29_fallar('V29 cuenta 1.1.01.002 (Bancos) no existe; requerida por CPP_BANCO_* bancarios');
    END IF;

    -- ------------------------------------------------------------------------
    -- 2. Agrupadoras nuevas (solo las que faltan)
    -- ------------------------------------------------------------------------
    CALL v29_asegurar_cuenta('1',      'Activo',                'ACTIVO',  'DEUDORA',   false, 'Bienes y derechos de la empresa');
    CALL v29_asegurar_cuenta('1.1',    'Activo Corriente',      'ACTIVO',  'DEUDORA',   false, 'Activos realizables en menos de un año');
    CALL v29_asegurar_cuenta('2.1.02', 'Impuestos por Pagar',   'PASIVO',  'ACREEDORA', false, 'Impuestos y retenciones por pagar');
    CALL v29_asegurar_cuenta('2.1.03', 'Anticipos',             'PASIVO',  'ACREEDORA', false, 'Anticipos recibidos');
    CALL v29_asegurar_cuenta('3',      'Capital',               'CAPITAL', 'ACREEDORA', false, 'Patrimonio de la empresa');
    CALL v29_asegurar_cuenta('3.1',    'Capital Social',        'CAPITAL', 'ACREEDORA', false, 'Aportes de los socios');
    CALL v29_asegurar_cuenta('3.2',    'Utilidades Retenidas',  'CAPITAL', 'ACREEDORA', false, 'Resultados acumulados de ejercicios anteriores');
    CALL v29_asegurar_cuenta('4',      'Ingresos',              'INGRESO', 'ACREEDORA', false, 'Ingresos de la empresa');
    CALL v29_asegurar_cuenta('4.1',    'Ingresos Operativos',   'INGRESO', 'ACREEDORA', false, 'Ingresos de la actividad principal');
    CALL v29_asegurar_cuenta('4.1.01', 'Ventas',                'INGRESO', 'ACREEDORA', false, 'Ingresos por ventas');
    CALL v29_asegurar_cuenta('4.2',    'Otros Ingresos',        'INGRESO', 'ACREEDORA', false, 'Ingresos no operativos');
    CALL v29_asegurar_cuenta('4.2.01', 'Diferencial Cambiario', 'INGRESO', 'ACREEDORA', false, 'Ganancias por diferencial cambiario');
    CALL v29_asegurar_cuenta('5.2',    'Gastos Financieros',    'EGRESO',  'DEUDORA',   false, 'Gastos financieros');
    CALL v29_asegurar_cuenta('5.2.01', 'Diferencial Cambiario', 'EGRESO',  'DEUDORA',   false, 'Pérdidas por diferencial cambiario');

    -- ------------------------------------------------------------------------
    -- 3. Hojas nuevas
    -- ------------------------------------------------------------------------
    CALL v29_asegurar_cuenta('3.1.01',     'Capital Social',             'CAPITAL', 'ACREEDORA', true, 'Capital social suscrito y pagado');
    CALL v29_asegurar_cuenta('3.2.01',     'Utilidades Retenidas',       'CAPITAL', 'ACREEDORA', true, 'Utilidades acumuladas de ejercicios anteriores');
    CALL v29_asegurar_cuenta('5.1.02.003', 'Cargas Sociales Patronales', 'EGRESO',  'DEUDORA',   true, 'Carga patronal CCSS de la planilla');

    -- ------------------------------------------------------------------------
    -- 4. Recodificación y renombre (se conservan los ids)
    -- ------------------------------------------------------------------------
    UPDATE cuentas_contables SET nombre = 'Efectivo y Equivalentes', acepta_movimientos = false,
           fecha_modificacion = NOW() WHERE id = 1;
    UPDATE cuentas_contables SET nombre = 'Cuentas por Cobrar', acepta_movimientos = false,
           fecha_modificacion = NOW() WHERE id = 2;
    UPDATE cuentas_contables SET nombre = 'IVA Crédito Fiscal', acepta_movimientos = true,
           fecha_modificacion = NOW() WHERE id = 3;
    UPDATE cuentas_contables SET acepta_movimientos = true, fecha_modificacion = NOW() WHERE id IN (4, 5);
    UPDATE cuentas_contables SET codigo = '2.1.04.003', fecha_modificacion = NOW() WHERE id = 7;
    UPDATE cuentas_contables SET codigo = '2.1.04.004', fecha_modificacion = NOW() WHERE id = 8;
    UPDATE cuentas_contables SET codigo = '2.1.04.005', fecha_modificacion = NOW() WHERE id = 9;
    UPDATE cuentas_contables SET codigo = '2.1.04.006', fecha_modificacion = NOW() WHERE id = 10;
    UPDATE cuentas_contables SET codigo = '2.1.02.002', fecha_modificacion = NOW() WHERE id = 11;
    UPDATE cuentas_contables SET codigo = '5.1.02.002', fecha_modificacion = NOW() WHERE id = 6;

    -- ------------------------------------------------------------------------
    -- 5. cuenta_padre_id (prefijo hasta el último punto) y nivel (segmentos)
    -- ------------------------------------------------------------------------
    UPDATE cuentas_contables c
    LEFT JOIN cuentas_contables p
           ON c.codigo LIKE '%.%'
          AND p.codigo = SUBSTRING(c.codigo, 1, LENGTH(c.codigo) - LENGTH(SUBSTRING_INDEX(c.codigo, '.', -1)) - 1)
    SET c.cuenta_padre_id = p.id,
        c.nivel = LENGTH(c.codigo) - LENGTH(REPLACE(c.codigo, '.', '')) + 1;

    SET v_codigo = (SELECT codigo FROM cuentas_contables
                     WHERE codigo LIKE '%.%' AND cuenta_padre_id IS NULL ORDER BY codigo LIMIT 1);
    IF v_codigo IS NOT NULL THEN
        CALL v29_fallar(CONCAT('V29 cuenta ', v_codigo, ' queda sin padre: no existe la cuenta ',
                               SUBSTRING(v_codigo, 1, LENGTH(v_codigo) - LENGTH(SUBSTRING_INDEX(v_codigo, '.', -1)) - 1)));
    END IF;

    -- ------------------------------------------------------------------------
    -- 6. Toda cuenta con hijas es agrupadora
    -- ------------------------------------------------------------------------
    DROP TEMPORARY TABLE IF EXISTS v29_padres;
    CREATE TEMPORARY TABLE v29_padres (id BIGINT PRIMARY KEY);
    INSERT INTO v29_padres (id)
        SELECT DISTINCT cuenta_padre_id FROM cuentas_contables WHERE cuenta_padre_id IS NOT NULL;
    UPDATE cuentas_contables c JOIN v29_padres p ON p.id = c.id
    SET c.acepta_movimientos = false;
    DROP TEMPORARY TABLE IF EXISTS v29_padres;

    -- ------------------------------------------------------------------------
    -- 7. Parámetros contables
    -- ------------------------------------------------------------------------
    UPDATE parametros_contables
       SET cuenta_contable_id = (SELECT id FROM cuentas_contables WHERE codigo = '1.1.01.001')
     WHERE clave = 'CPP_BANCO_EFECTIVO';
    UPDATE parametros_contables
       SET cuenta_contable_id = (SELECT id FROM cuentas_contables WHERE codigo = '1.1.01.002')
     WHERE clave IN ('CPP_BANCO_TRANSFERENCIA', 'CPP_BANCO_SINPE', 'CPP_BANCO_DEPOSITO', 'CPP_BANCO_CHEQUE');

    INSERT INTO parametros_contables (clave, descripcion, cuenta_contable_id)
    SELECT 'NOMINA_GASTO_CARGAS_SOCIALES', 'Gasto por cargas sociales patronales de la planilla (ADR-017)',
           (SELECT c.id FROM cuentas_contables c
             WHERE c.codigo = '5.1.02.003' AND c.tipo = 'EGRESO' AND c.naturaleza = 'DEUDORA')
    FROM DUAL
    WHERE NOT EXISTS (SELECT 1 FROM parametros_contables WHERE clave = 'NOMINA_GASTO_CARGAS_SOCIALES');

    UPDATE parametros_contables SET descripcion = 'Débito compras a proveedores'
     WHERE clave = 'CPP_CUENTA_COMPRAS';
    UPDATE parametros_contables SET descripcion = 'IVA crédito fiscal compras'
     WHERE clave = 'CPP_IVA_CREDITO_FISCAL';

    -- ------------------------------------------------------------------------
    -- 8. Verificación final
    -- ------------------------------------------------------------------------
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
        CALL v29_fallar(CONCAT('V29 clave ', v_clave, ' apunta a la cuenta ', v_codigo, ', que ', v_motivo,
                               '; solo puede apuntar a una hoja'));
    END IF;

    SET v_codigo = (SELECT c.codigo FROM cuentas_contables c
                    LEFT JOIN cuentas_contables p ON p.id = c.cuenta_padre_id
                     WHERE c.codigo LIKE '%.%'
                       AND (p.id IS NULL
                            OR p.codigo <> SUBSTRING(c.codigo, 1, LENGTH(c.codigo) - LENGTH(SUBSTRING_INDEX(c.codigo, '.', -1)) - 1))
                     ORDER BY c.codigo LIMIT 1);
    IF v_codigo IS NOT NULL THEN
        CALL v29_fallar(CONCAT('V29 cuenta ', v_codigo, ' no tiene el padre de su prefijo'));
    END IF;

    COMMIT;
END //

DELIMITER ;

CALL v29_plan_de_cuentas_coherente();

DROP PROCEDURE IF EXISTS v29_plan_de_cuentas_coherente;
DROP PROCEDURE IF EXISTS v29_asegurar_cuenta;
DROP PROCEDURE IF EXISTS v29_validar_cuenta_existente;
DROP PROCEDURE IF EXISTS v29_fallar;
