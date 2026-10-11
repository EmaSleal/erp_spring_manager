-- ============================================================================
-- V19: Seed sales/collection accounting parameters (ADR-008)
-- ============================================================================
-- Creates the minimum leaf accounts for sales and customer collections and
-- maps them to the parametros_contables keys required by
-- AsientoContableService.generarAsientoVenta / generarAsientoPago:
--   VENTA_CXC_CLIENTES, VENTA_INGRESOS, VENTA_IVA_POR_PAGAR,
--   COBRO_ANTICIPOS_CLIENTES, COBRO_CAJA_<MetodoPago>
--
-- Validating seed (no silent linking):
--   * If the account with the expected code does not exist, it is created.
--   * If it exists, its tipo/naturaleza must match the expected ones;
--     otherwise the migration FAILS naming the key and the account.
--   * If the key already exists (manual configuration), the mapping is kept
--     only when the mapped account has the expected tipo/naturaleza;
--     otherwise the migration FAILS naming the key and the account.
-- Re-running after a failure is safe: existing valid rows are kept.
-- ============================================================================

DROP PROCEDURE IF EXISTS v19_asegurar_parametro_contable;

DELIMITER //

CREATE PROCEDURE v19_asegurar_parametro_contable(
    IN p_clave        VARCHAR(100),
    IN p_param_desc   VARCHAR(300),
    IN p_codigo       VARCHAR(20),
    IN p_nombre       VARCHAR(200),
    IN p_tipo         VARCHAR(20),
    IN p_naturaleza   VARCHAR(20),
    IN p_cuenta_desc  VARCHAR(500)
)
BEGIN
    DECLARE v_cuenta_id    BIGINT DEFAULT NULL;
    DECLARE v_param_cuenta BIGINT DEFAULT NULL;
    DECLARE v_codigo       VARCHAR(20);
    DECLARE v_nombre       VARCHAR(200);
    DECLARE v_tipo         VARCHAR(20);
    DECLARE v_naturaleza   VARCHAR(20);
    DECLARE v_msg          VARCHAR(512);

    SET v_param_cuenta = (SELECT cuenta_contable_id FROM parametros_contables WHERE clave = p_clave);

    IF v_param_cuenta IS NOT NULL THEN
        -- Key already configured: validate the mapped account, keep the mapping
        SELECT codigo, nombre, tipo, naturaleza
          INTO v_codigo, v_nombre, v_tipo, v_naturaleza
          FROM cuentas_contables WHERE id = v_param_cuenta;

        IF v_tipo <> p_tipo OR v_naturaleza <> p_naturaleza THEN
            SET v_msg = LEFT(CONCAT('V19 clave ', p_clave, ': cuenta ', v_codigo, ' (', LEFT(v_nombre, 24), ') es ',
                                    v_tipo, '/', v_naturaleza, '; esperado ', p_tipo, '/', p_naturaleza), 128);
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
        END IF;
    ELSE
        SET v_cuenta_id = (SELECT id FROM cuentas_contables WHERE codigo = p_codigo);

        IF v_cuenta_id IS NULL THEN
            INSERT INTO cuentas_contables
                (codigo, nombre, tipo, naturaleza, nivel, activa, acepta_movimientos, cuenta_padre_id,
                 descripcion, creado_por, fecha_creacion, modificado_por, fecha_modificacion)
            VALUES
                (p_codigo, p_nombre, p_tipo, p_naturaleza, 4, true, true, NULL,
                 p_cuenta_desc, NULL, NOW(), NULL, NULL);
            SET v_cuenta_id = LAST_INSERT_ID();
        ELSE
            -- Account code already exists: link it only if tipo/naturaleza match
            SELECT nombre, tipo, naturaleza
              INTO v_nombre, v_tipo, v_naturaleza
              FROM cuentas_contables WHERE id = v_cuenta_id;

            IF v_tipo <> p_tipo OR v_naturaleza <> p_naturaleza THEN
                SET v_msg = LEFT(CONCAT('V19 clave ', p_clave, ': cuenta ', p_codigo, ' (', LEFT(v_nombre, 24), ') es ',
                                        v_tipo, '/', v_naturaleza, '; esperado ', p_tipo, '/', p_naturaleza), 128);
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
            END IF;
        END IF;

        INSERT INTO parametros_contables (clave, descripcion, cuenta_contable_id)
        VALUES (p_clave, p_param_desc, v_cuenta_id);
    END IF;
END //

DELIMITER ;

-- Sales keys
CALL v19_asegurar_parametro_contable('VENTA_CXC_CLIENTES', 'Cuenta por cobrar a clientes (ventas y cobros)',
    '1.1.02.001', 'Cuentas por Cobrar Clientes', 'ACTIVO', 'DEUDORA',
    'Saldos por cobrar a clientes por ventas a crédito');

CALL v19_asegurar_parametro_contable('VENTA_INGRESOS', 'Cuenta de ingresos por ventas',
    '4.1.01.001', 'Ingresos por Ventas', 'INGRESO', 'ACREEDORA',
    'Ingresos por venta de mercadería y servicios');

CALL v19_asegurar_parametro_contable('VENTA_IVA_POR_PAGAR', 'Cuenta de IVA por pagar en ventas',
    '2.1.02.001', 'IVA por Pagar', 'PASIVO', 'ACREEDORA',
    'IVA trasladado en ventas pendiente de declarar');

-- Collection keys
CALL v19_asegurar_parametro_contable('COBRO_ANTICIPOS_CLIENTES', 'Cuenta de anticipos de clientes (pagos sin factura)',
    '2.1.03.001', 'Anticipos de Clientes', 'PASIVO', 'ACREEDORA',
    'Pagos recibidos de clientes sin factura asignada');

CALL v19_asegurar_parametro_contable('COBRO_CAJA_EFECTIVO', 'Caja/banco para cobros en efectivo',
    '1.1.01.001', 'Caja General', 'ACTIVO', 'DEUDORA',
    'Cobros recibidos en efectivo');

CALL v19_asegurar_parametro_contable('COBRO_CAJA_TRANSFERENCIA', 'Caja/banco para cobros por transferencia',
    '1.1.01.002', 'Bancos', 'ACTIVO', 'DEUDORA',
    'Cobros por transferencia o depósito bancario');

CALL v19_asegurar_parametro_contable('COBRO_CAJA_TARJETA', 'Caja/banco para cobros con tarjeta',
    '1.1.01.003', 'Tarjetas por Liquidar', 'ACTIVO', 'DEUDORA',
    'Cobros con tarjeta pendientes de liquidación del datáfono');

CALL v19_asegurar_parametro_contable('COBRO_CAJA_CHEQUE', 'Caja/banco para cobros con cheque',
    '1.1.01.004', 'Cheques por Depositar', 'ACTIVO', 'DEUDORA',
    'Cheques recibidos pendientes de depósito');

CALL v19_asegurar_parametro_contable('COBRO_CAJA_RECAUDADO', 'Caja/banco para cobros recaudados por terceros',
    '1.1.01.005', 'Recaudación por Terceros', 'ACTIVO', 'DEUDORA',
    'Cobros recaudados por terceros pendientes de traslado');

CALL v19_asegurar_parametro_contable('COBRO_CAJA_OTROS', 'Caja/banco para cobros por otros medios',
    '1.1.01.001', 'Caja General', 'ACTIVO', 'DEUDORA',
    'Cobros recibidos en efectivo');

DROP PROCEDURE IF EXISTS v19_asegurar_parametro_contable;
