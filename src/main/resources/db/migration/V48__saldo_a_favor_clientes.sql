-- T-048 / ADR-053, ADR-054, ADR-055, ADR-056, ADR-057: saldo a favor del cliente (fase B1).
--
-- 1. Plan de cuentas (ADR-054): grupo 2.1.05 «Saldos a favor de clientes» (no acepta movimientos) bajo 2.1 y sus hojas
--    2.1.05.001 (CRC) y 2.1.05.002 (USD): PASIVO, ACREEDORA, nivel 4, aceptan movimientos. Idempotente: no toca una
--    cuenta cuyo código ya existe ni ninguna otra cuenta. 2.1.03.001 «Anticipos de Clientes» es otra cosa y no se usa.
-- 2. Parámetros contables explícitos, uno por moneda y sin valor por defecto en el código (ADR-024):
--    SALDO_FAVOR_CLIENTES_CRC y SALDO_FAVOR_CLIENTES_USD. Esta migración los deja configurados con las hojas anteriores.
-- 3. saldos_favor_cliente: el auxiliar de un saldo a favor (ADR-053); el saldo vivo nunca es negativo (CHECK).
-- 4. aplicaciones_saldo_favor: cada aplicación manual del saldo a otra factura del mismo cliente y moneda, con el
--    diferencial por tramos (ADR-055, ADR-057), el tipo de cambio del día (ADR-056 §3) y, si se reversa, los datos de la
--    reversión. Nada se borra.
-- 5. nota_factura.monto_credito_cxc / monto_saldo_favor: el reparto de la nota al aplicarse. Quedan NULL hasta que la
--    nota pasa a APLICADA; las notas existentes no se modifican.
-- Los asientos y pagos ya registrados no se modifican.

INSERT INTO cuentas_contables
    (codigo, nombre, tipo, naturaleza, nivel, activa, acepta_movimientos, cuenta_padre_id,
     descripcion, creado_por, fecha_creacion, modificado_por, fecha_modificacion)
SELECT '2.1.05', 'Saldos a favor de clientes', 'PASIVO', 'ACREEDORA', 3, true, false,
       (SELECT p.id FROM cuentas_contables p WHERE p.codigo = '2.1' LIMIT 1),
       'Excedente de las notas de crédito sobre el saldo pendiente de la factura, por moneda (ADR-053)',
       NULL, NOW(), NULL, NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cuentas_contables WHERE codigo = '2.1.05');

INSERT INTO cuentas_contables
    (codigo, nombre, tipo, naturaleza, nivel, activa, acepta_movimientos, cuenta_padre_id,
     descripcion, creado_por, fecha_creacion, modificado_por, fecha_modificacion)
SELECT '2.1.05.001', 'Saldos a favor de clientes – CRC', 'PASIVO', 'ACREEDORA', 4, true, true,
       (SELECT p.id FROM cuentas_contables p WHERE p.codigo = '2.1.05' LIMIT 1),
       'Saldos a favor de clientes en colones: crédito por el excedente de la nota, débito al aplicarlos',
       NULL, NOW(), NULL, NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cuentas_contables WHERE codigo = '2.1.05.001');

INSERT INTO cuentas_contables
    (codigo, nombre, tipo, naturaleza, nivel, activa, acepta_movimientos, cuenta_padre_id,
     descripcion, creado_por, fecha_creacion, modificado_por, fecha_modificacion)
SELECT '2.1.05.002', 'Saldos a favor de clientes – USD', 'PASIVO', 'ACREEDORA', 4, true, true,
       (SELECT p.id FROM cuentas_contables p WHERE p.codigo = '2.1.05' LIMIT 1),
       'Saldos a favor de clientes en dólares: crédito por el excedente de la nota, débito al aplicarlos',
       NULL, NOW(), NULL, NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cuentas_contables WHERE codigo = '2.1.05.002');

INSERT INTO parametros_contables (clave, descripcion, cuenta_contable_id)
SELECT 'SALDO_FAVOR_CLIENTES_CRC', 'Saldos a favor de clientes en colones (ADR-054)',
       (SELECT c.id FROM cuentas_contables c WHERE c.codigo = '2.1.05.001')
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM parametros_contables WHERE clave = 'SALDO_FAVOR_CLIENTES_CRC');

INSERT INTO parametros_contables (clave, descripcion, cuenta_contable_id)
SELECT 'SALDO_FAVOR_CLIENTES_USD', 'Saldos a favor de clientes en dólares (ADR-054)',
       (SELECT c.id FROM cuentas_contables c WHERE c.codigo = '2.1.05.002')
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM parametros_contables WHERE clave = 'SALDO_FAVOR_CLIENTES_USD');

ALTER TABLE nota_factura
    ADD COLUMN monto_credito_cxc DECIMAL(12,2) NULL,
    ADD COLUMN monto_saldo_favor DECIMAL(12,2) NULL;

CREATE TABLE saldos_favor_cliente (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    cliente_id          INT           NOT NULL,
    moneda              VARCHAR(3)    NOT NULL,
    monto_original      DECIMAL(12,2) NOT NULL,
    tipo_cambio_origen  DECIMAL(18,5) NOT NULL,
    fecha_origen        DATE          NOT NULL,
    saldo_vivo          DECIMAL(12,2) NOT NULL,
    nota_factura_id     BIGINT        NOT NULL,
    clave_nota          VARCHAR(50)   NOT NULL,
    factura_origen_id   INT           NOT NULL,
    estado              VARCHAR(20)   NOT NULL,
    creado_por          INT           NULL,
    fecha_creacion      DATETIME(6)   NULL,
    modificado_por      INT           NULL,
    fecha_modificacion  DATETIME(6)   NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_saldo_favor_nota UNIQUE (nota_factura_id),
    CONSTRAINT ck_saldo_favor_saldo_vivo CHECK (saldo_vivo >= 0),
    CONSTRAINT fk_saldo_favor_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id_cliente),
    CONSTRAINT fk_saldo_favor_nota FOREIGN KEY (nota_factura_id) REFERENCES nota_factura (id),
    CONSTRAINT fk_saldo_favor_factura_origen FOREIGN KEY (factura_origen_id) REFERENCES factura (id_factura),
    KEY idx_saldo_favor_cliente_moneda_estado (cliente_id, moneda, estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aplicaciones_saldo_favor (
    id                      BIGINT        NOT NULL AUTO_INCREMENT,
    saldo_favor_id          BIGINT        NOT NULL,
    factura_id              INT           NOT NULL,
    pago_id                 BIGINT        NOT NULL,
    monto                   DECIMAL(12,2) NOT NULL,
    tipo_cambio_origen      DECIMAL(18,5) NOT NULL,
    tipo_cambio_factura     DECIMAL(18,5) NOT NULL,
    tc_dia_fecha            DATE          NULL,
    tc_dia_valor            DECIMAL(18,5) NULL,
    tc_dia_tipo             VARCHAR(10)   NULL,
    tc_dia_fuente           VARCHAR(20)   NULL,
    tc_dia_origen           VARCHAR(10)   NULL,
    diferencial_saldo       DECIMAL(14,2) NOT NULL,
    diferencial_cxc         DECIMAL(14,2) NOT NULL,
    diferencial_neto        DECIMAL(14,2) NOT NULL,
    asiento_id              BIGINT        NOT NULL,
    estado                  VARCHAR(20)   NOT NULL,
    usuario_id              INT           NOT NULL,
    fecha_aplicacion        DATETIME(6)   NOT NULL,
    razon_reversion         VARCHAR(180)  NULL,
    reversado_por           INT           NULL,
    fecha_reversion         DATETIME(6)   NULL,
    asiento_reversion_id    BIGINT        NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_aplicacion_saldo FOREIGN KEY (saldo_favor_id) REFERENCES saldos_favor_cliente (id),
    CONSTRAINT fk_aplicacion_factura FOREIGN KEY (factura_id) REFERENCES factura (id_factura),
    CONSTRAINT fk_aplicacion_pago FOREIGN KEY (pago_id) REFERENCES pagos (id_pago),
    CONSTRAINT fk_aplicacion_asiento FOREIGN KEY (asiento_id) REFERENCES asientos_contables (id),
    CONSTRAINT fk_aplicacion_asiento_reversion FOREIGN KEY (asiento_reversion_id) REFERENCES asientos_contables (id),
    KEY idx_aplicacion_saldo (saldo_favor_id),
    KEY idx_aplicacion_factura (factura_id),
    KEY idx_aplicacion_pago (pago_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
