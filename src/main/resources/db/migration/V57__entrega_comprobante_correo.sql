-- T-055 (ADR-070): cola de entregas de comprobantes por correo (outbox) y permiso del reenvío manual.
--
-- entrega_comprobante: una fila AUTOMATICO nace en la misma transacción en que el comprobante pasa a ACEPTADO; el job
-- la envía con Resend. clave_automatica vale comprobante_id cuando origen = 'AUTOMATICO' y NULL cuando es MANUAL: su
-- índice único impide más de una entrega automática por comprobante (MySQL admite varios NULL, así que los reenvíos
-- manuales no chocan). Las horas las escribe la aplicación con su Clock (America/Costa_Rica): sin valores por defecto
-- de reloj en la base.

CREATE TABLE entrega_comprobante (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    comprobante_id   BIGINT       NOT NULL,
    tipo_documento   VARCHAR(20)  NOT NULL,
    origen           VARCHAR(12)  NOT NULL,
    usuario_id       INT          NULL,
    destinatario     VARCHAR(320) NULL,
    cco              VARCHAR(1000) NULL,
    estado           VARCHAR(20)  NOT NULL,
    intentos         INT          NOT NULL DEFAULT 0,
    proximo_intento  DATETIME     NULL,
    ultimo_error     VARCHAR(500) NULL,
    id_proveedor     VARCHAR(100) NULL,
    fecha_creacion   DATETIME     NOT NULL,
    fecha_envio      DATETIME     NULL,
    clave_automatica BIGINT       NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_entrega_clave_automatica UNIQUE (clave_automatica),
    CONSTRAINT fk_entrega_comprobante FOREIGN KEY (comprobante_id) REFERENCES comprobante_electronico (id),
    CONSTRAINT fk_entrega_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario),
    CONSTRAINT ck_entrega_tipo CHECK (tipo_documento IN ('FACTURA', 'NOTA_CREDITO')),
    CONSTRAINT ck_entrega_origen CHECK (origen IN ('AUTOMATICO', 'MANUAL')),
    CONSTRAINT ck_entrega_estado CHECK (estado IN ('PENDIENTE', 'ENVIADO', 'FALLIDO', 'OMITIDO_SIN_CORREO', 'OMITIDO_SANDBOX')),
    CONSTRAINT ck_entrega_clave CHECK ((origen = 'AUTOMATICO' AND clave_automatica = comprobante_id)
                                       OR (origen = 'MANUAL' AND clave_automatica IS NULL)),
    INDEX idx_entrega_estado_proximo (estado, proximo_intento),
    INDEX idx_entrega_comprobante (comprobante_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT IGNORE INTO permiso
    (codigo, nombre, descripcion, categoria, es_critico, activo, create_by, create_date, update_by, update_date)
VALUES
    ('COMPROBANTE_REENVIAR_CORREO', 'Reenviar comprobante por correo',
     'Reenviar por correo al cliente el PDF, el XML firmado y la respuesta de Hacienda de un comprobante aceptado',
     'Facturación', false, true, NULL, NOW(), NULL, NULL);

INSERT IGNORE INTO rol_permiso (id_rol, id_permiso)
    SELECT r.id_rol, p.id_permiso
    FROM rol r
    JOIN permiso p ON p.codigo = 'COMPROBANTE_REENVIAR_CORREO'
    WHERE r.codigo = 'ADMIN';
-- fin-permiso
