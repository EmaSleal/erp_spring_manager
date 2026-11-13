-- ============================================================================
-- V39: nota de crédito (y, a futuro, de débito) sobre una factura (ADR-037, T-032)
-- ============================================================================
-- Esta migración:
--   1) crea nota_factura: la nota, con su código de referencia (nota 9 del Anexo
--      v4.4), razón, estado, la clave y fecha del comprobante ACEPTADO que
--      referencia, moneda, tipo de cambio y totales. Como máximo una nota
--      EN_TRAMITE por factura: columna generada factura_en_tramite_id con UNIQUE
--      (mismo patrón que V36);
--   2) crea linea_nota_factura: foto de cada línea de la factura al emitir la
--      nota. La referencia a la línea de origen y al producto pasa a NULL si se
--      borran (sin cascada de borrado);
--   3) comprobante_electronico: FK nullable a la nota; factura_id nullable; CHECK
--      que exige exactamente un documento de origen (factura o nota); como máximo
--      un comprobante vigente por nota (columna generada + UNIQUE, como V36). La
--      unicidad de vigente por factura de V36 no cambia;
--   4) asientos_contables: FK nullable a la nota (reversión al aplicarse la nota).
-- Sin procedimientos ni triggers (ADR-028). Producción no tiene comprobantes ni
-- notas: no modifica ninguna fila existente.
-- ============================================================================

-- 1) Nota
CREATE TABLE nota_factura (
    id                        BIGINT        NOT NULL AUTO_INCREMENT,
    factura_id                INT           NOT NULL,
    tipo                      ENUM('CREDITO', 'DEBITO') NOT NULL,
    codigo_referencia         VARCHAR(2)    NOT NULL,
    razon                     VARCHAR(180)  NOT NULL,
    estado                    ENUM('APLICADA', 'EN_TRAMITE', 'RECHAZADA') NOT NULL,
    fecha_emision             DATETIME(6)   NOT NULL,
    fecha_aplicacion          DATETIME(6)   NULL,
    comprobante_referencia_id BIGINT        NOT NULL,
    clave_referencia          VARCHAR(50)   NOT NULL,
    fecha_emision_referencia  DATETIME(6)   NOT NULL,
    moneda                    VARCHAR(3)    NOT NULL,
    tipo_cambio               DECIMAL(18,5) NOT NULL,
    subtotal                  DECIMAL(12,2) NOT NULL,
    descuento                 DECIMAL(12,2) NOT NULL,
    impuesto                  DECIMAL(12,2) NOT NULL,
    total                     DECIMAL(12,2) NOT NULL,
    usuario_creador           VARCHAR(100)  NOT NULL,
    create_date               DATETIME(6)   NOT NULL,
    update_date               DATETIME(6)   NULL,
    factura_en_tramite_id     INT GENERATED ALWAYS AS (IF(estado = 'EN_TRAMITE', factura_id, NULL)) STORED,
    PRIMARY KEY (id),
    KEY idx_nota_factura_factura (factura_id),
    KEY idx_nota_factura_estado (estado),
    UNIQUE KEY uk_nota_en_tramite_por_factura (factura_en_tramite_id),
    CONSTRAINT fk_nota_factura_factura FOREIGN KEY (factura_id) REFERENCES factura (id_factura),
    CONSTRAINT fk_nota_factura_comprobante_ref FOREIGN KEY (comprobante_referencia_id)
        REFERENCES comprobante_electronico (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 2) Líneas de la nota (foto de la línea de factura)
CREATE TABLE linea_nota_factura (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    nota_factura_id  BIGINT        NOT NULL,
    linea_factura_id INT           NULL,
    producto_id      INT           NULL,
    numero_linea     INT           NOT NULL,
    descripcion      VARCHAR(200)  NOT NULL,
    codigo_cabys     VARCHAR(13)   NOT NULL,
    unidad_medida    VARCHAR(15)   NOT NULL,
    cantidad         DECIMAL(16,3) NOT NULL,
    precio_unitario  DECIMAL(18,5) NOT NULL,
    descuento        DECIMAL(12,2) NOT NULL,
    tarifa_iva       DECIMAL(5,2)  NOT NULL,
    monto_iva        DECIMAL(12,2) NOT NULL,
    subtotal         DECIMAL(12,2) NOT NULL,
    total            DECIMAL(12,2) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_linea_nota_nota (nota_factura_id),
    KEY idx_linea_nota_linea_factura (linea_factura_id),
    KEY idx_linea_nota_producto (producto_id),
    CONSTRAINT fk_linea_nota_nota FOREIGN KEY (nota_factura_id) REFERENCES nota_factura (id),
    CONSTRAINT fk_linea_nota_linea_factura FOREIGN KEY (linea_factura_id) REFERENCES linea_factura (id_linea_factura)
        ON DELETE SET NULL,
    CONSTRAINT fk_linea_nota_producto FOREIGN KEY (producto_id) REFERENCES producto (id_producto)
        ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 3) Comprobante: documento de origen factura o nota
ALTER TABLE comprobante_electronico
    MODIFY COLUMN factura_id INT NULL,
    ADD COLUMN nota_factura_id BIGINT NULL AFTER factura_id,
    ADD COLUMN nota_vigente_id BIGINT GENERATED ALWAYS AS (IF(vigente = b'1', nota_factura_id, NULL)) STORED,
    ADD KEY idx_comprobante_nota_factura (nota_factura_id),
    ADD UNIQUE KEY uk_comprobante_un_vigente_por_nota (nota_vigente_id),
    ADD CONSTRAINT fk_comprobante_nota_factura FOREIGN KEY (nota_factura_id) REFERENCES nota_factura (id),
    ADD CONSTRAINT chk_comprobante_un_documento_origen
        CHECK ((factura_id IS NULL) <> (nota_factura_id IS NULL));

-- 4) Asiento ligado a la nota
ALTER TABLE asientos_contables
    ADD COLUMN nota_factura_id BIGINT NULL,
    ADD KEY idx_asiento_nota_factura (nota_factura_id),
    ADD CONSTRAINT fk_asiento_nota_factura FOREIGN KEY (nota_factura_id) REFERENCES nota_factura (id);
