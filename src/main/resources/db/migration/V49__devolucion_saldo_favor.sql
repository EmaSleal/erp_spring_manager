-- T-049 / ADR-058 (fase B2 de ADR-053): devolución en dinero de un saldo a favor.
--
-- 1. saldos_favor_cliente.monto_reservado: lo reservado por devoluciones SOLICITADAS o APROBADAS. El disponible es
--    saldo_vivo - monto_reservado y nunca es negativo (CHECK); la aplicación de B1 y las solicitudes usan solo el
--    disponible. Los saldos existentes quedan con 0 reservado.
-- 2. devoluciones_saldo_favor: una fila por devolución (SOLICITADA, APROBADA, PAGADA, RECHAZADA, ANULADA), con el medio y
--    la fecha de la solicitud del cliente, el IBAN, el titular, el usuario y la fecha de cada transición, la razón del
--    rechazo o la anulación, la declaración del aprobador, la cuenta bancaria de la empresa (ADR-018) y el comprobante
--    bancario (número y fecha: el sistema no tiene almacenamiento de adjuntos), el TC de origen y el del día (ADR-056),
--    el diferencial realizado por tramo (ADR-055) y el asiento del pago. Nada se borra.
-- 3. Permiso SALDO_FAVOR_DEVOLUCION_APROBAR (aprobar, rechazar y pagar devoluciones): se crea si no existe y se asigna al
--    rol ADMIN. Los roles existentes son ADMIN, GERENTE, VENDEDOR, USER, VISUALIZADOR y CLIENTE: no hay rol de
--    contabilidad, así que no se asigna a ningún otro. Idempotente (INSERT IGNORE; el código del permiso es único).
-- Los asientos y pagos ya registrados no se modifican.

ALTER TABLE saldos_favor_cliente
    ADD COLUMN monto_reservado DECIMAL(12,2) NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_saldo_favor_reservado CHECK (monto_reservado >= 0 AND saldo_vivo - monto_reservado >= 0);

CREATE TABLE devoluciones_saldo_favor (
    id                      BIGINT        NOT NULL AUTO_INCREMENT,
    saldo_favor_id          BIGINT        NOT NULL,
    monto                   DECIMAL(12,2) NOT NULL,
    estado                  VARCHAR(20)   NOT NULL,
    medio_solicitud         VARCHAR(20)   NOT NULL,
    fecha_solicitud         DATE          NOT NULL,
    iban                    VARCHAR(34)   NOT NULL,
    nombre_titular          VARCHAR(100)  NOT NULL,
    identificacion_titular  VARCHAR(20)   NOT NULL,
    solicitado_por          INT           NOT NULL,
    fecha_creacion          DATETIME(6)   NOT NULL,
    aprobado_por            INT           NULL,
    fecha_aprobacion        DATETIME(6)   NULL,
    verifico_cuenta         BIT(1)        NOT NULL DEFAULT b'0',
    fecha_verificacion      DATETIME(6)   NULL,
    rechazado_por           INT           NULL,
    fecha_rechazo           DATETIME(6)   NULL,
    anulado_por             INT           NULL,
    fecha_anulacion         DATETIME(6)   NULL,
    razon                   VARCHAR(180)  NULL,
    pagado_por              INT           NULL,
    fecha_pago              DATETIME(6)   NULL,
    cuenta_bancaria_id      INT           NULL,
    numero_comprobante      VARCHAR(50)   NULL,
    fecha_comprobante       DATE          NULL,
    tipo_cambio_origen      DECIMAL(18,5) NULL,
    tc_dia_fecha            DATE          NULL,
    tc_dia_valor            DECIMAL(18,5) NULL,
    tc_dia_tipo             VARCHAR(10)   NULL,
    tc_dia_fuente           VARCHAR(20)   NULL,
    tc_dia_origen           VARCHAR(10)   NULL,
    diferencial_saldo       DECIMAL(14,2) NULL,
    diferencial_neto        DECIMAL(14,2) NULL,
    asiento_id              BIGINT        NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_devolucion_monto CHECK (monto > 0),
    CONSTRAINT fk_devolucion_saldo FOREIGN KEY (saldo_favor_id) REFERENCES saldos_favor_cliente (id),
    CONSTRAINT fk_devolucion_cuenta_bancaria FOREIGN KEY (cuenta_bancaria_id)
        REFERENCES cuenta_bancaria (id_cuenta_bancaria),
    CONSTRAINT fk_devolucion_asiento FOREIGN KEY (asiento_id) REFERENCES asientos_contables (id),
    KEY idx_devolucion_saldo (saldo_favor_id, estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT IGNORE INTO permiso
    (codigo, nombre, descripcion, categoria, es_critico, activo, create_by, create_date, update_by, update_date)
VALUES
    ('SALDO_FAVOR_DEVOLUCION_APROBAR', 'Aprobar devoluciones de saldo a favor',
     'Aprobar, rechazar y pagar devoluciones en dinero de saldos a favor de clientes (operación crítica)',
     'Pagos', true, true, NULL, NOW(), NULL, NULL);

INSERT IGNORE INTO rol_permiso (id_rol, id_permiso)
    SELECT r.id_rol, p.id_permiso
    FROM rol r
    JOIN permiso p ON p.codigo = 'SALDO_FAVOR_DEVOLUCION_APROBAR'
    WHERE r.codigo = 'ADMIN';
