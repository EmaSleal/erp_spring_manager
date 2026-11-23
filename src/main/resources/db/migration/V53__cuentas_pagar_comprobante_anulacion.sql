-- ============================================================================
-- V53: cuentas por pagar con datos del comprobante del proveedor, desglose de IVA, anulación
--      y saldo del proveedor en CRC (T-052, ADR-066 §1-3, P-021, P-023 §1/§2/§4)
-- ============================================================================
-- 1. cuentas_por_pagar: columnas nuevas, todas NULL-ables. Las CxP existentes conservan sus valores y quedan con
--    estos campos en NULL (no se inventa el número, la clave ni la fecha del comprobante, ADR-024):
--      numero_comprobante_proveedor  VARCHAR(50)  número o consecutivo del comprobante del proveedor
--      clave_comprobante             VARCHAR(50)  clave numérica de 50 dígitos (opcional)
--      fecha_comprobante             DATE         fecha de emisión del comprobante del proveedor
--      comprobante_normalizado       VARCHAR(50)  número sin espacios y en mayúsculas, NULL si la CxP está anulada o no
--                                                 tiene comprobante. La mantiene el servicio. Respalda en la base la
--                                                 regla «un proveedor no repite el comprobante en CxP no anuladas»:
--                                                 el índice único trata los NULL como distintos, así que las
--                                                 anuladas y las heredadas no chocan.
--      anulada_por_id / fecha_anulacion / motivo_anulacion   anulación (ADR-066 §3)
--
-- 2. cuenta_pagar_iva: desglose de IVA por tarifa de la CxP (base e IVA en la moneda de la CxP).
--    codigo_tarifa es el CodigoTarifaIVA de Hacienda v4.4 (el mismo catálogo que las líneas de la factura de venta);
--    NULL significa «sin tarifa».
--    Desglose de las CxP existentes: una sola fila derivada de sus propios datos, sin inventar:
--      base = subtotal, o monto - impuesto si el subtotal es NULL (monto si ambos son NULL), iva = impuesto (0 si NULL).
--      codigo_tarifa = '08' (13 %) solo si subtotal e impuesto existen, impuesto > 0 e impuesto = ROUND(subtotal x 0.13, 2).
--      En cualquier otro caso queda NULL («sin tarifa»): un impuesto 0 es ambiguo (exento o no sujeto), una tasa
--      distinta de 13 % no se puede identificar con un código y la CxP de una orden de compra no guarda subtotal ni
--      impuesto propios.
--
-- 3. Permiso CUENTA_PAGAR_ANULAR (crítico), asignado al rol ADMIN. Idempotente: INSERT IGNORE.
--    El DataInitializer también lo crea desde el enum, y MatrizPermisos lo da a ADMIN.
--
-- 4. proveedores.saldo_pendiente se recalcula una vez con la fórmula nueva (ADR-011): suma, por proveedor, del saldo de
--    cada CxP no anulada x su tipo de cambio (1 si la moneda es CRC o falta el tipo de cambio), cada producto
--    redondeado a 2 decimales. El saldo anterior sumaba montos sin convertir.
--
-- 5. Cuenta de IVA crédito fiscal (código 1.1.03): si su nombre quedó con la codificación rota («IVA Crdito Fiscal»)
--    se corrige a «IVA Crédito Fiscal». Se identifica por código, no por nombre, y el UPDATE es un no-op si ya está bien.
--    El é se arma con sus bytes UTF-8 (0xC3A9) para no depender de la codificación con que se lea este archivo.
-- ============================================================================

ALTER TABLE cuentas_por_pagar
    ADD COLUMN numero_comprobante_proveedor VARCHAR(50)  NULL,
    ADD COLUMN clave_comprobante            VARCHAR(50)  NULL,
    ADD COLUMN fecha_comprobante            DATE         NULL,
    ADD COLUMN comprobante_normalizado      VARCHAR(50)  NULL,
    ADD COLUMN anulada_por_id               INT          NULL,
    ADD COLUMN fecha_anulacion              DATETIME     NULL,
    ADD COLUMN motivo_anulacion             VARCHAR(180) NULL;

ALTER TABLE cuentas_por_pagar
    ADD CONSTRAINT uk_cpp_proveedor_comprobante UNIQUE (proveedor_id, comprobante_normalizado);

CREATE TABLE cuenta_pagar_iva (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    cuenta_por_pagar_id BIGINT        NOT NULL,
    codigo_tarifa       VARCHAR(2)    NULL,
    base                DECIMAL(15,2) NOT NULL,
    iva                 DECIMAL(15,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_cpi_cuenta FOREIGN KEY (cuenta_por_pagar_id) REFERENCES cuentas_por_pagar (id),
    KEY idx_cpi_cuenta (cuenta_por_pagar_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO cuenta_pagar_iva (cuenta_por_pagar_id, codigo_tarifa, base, iva)
    SELECT c.id,
           CASE WHEN c.subtotal IS NOT NULL AND c.impuesto IS NOT NULL AND c.impuesto > 0
                     AND c.impuesto = ROUND(c.subtotal * 0.13, 2)
                THEN '08' ELSE NULL END,
           COALESCE(c.subtotal, c.monto - COALESCE(c.impuesto, 0)),
           COALESCE(c.impuesto, 0)
    FROM cuentas_por_pagar c;

INSERT IGNORE INTO permiso
    (codigo, nombre, descripcion, categoria, es_critico, activo, create_by, create_date, update_by, update_date)
VALUES
    ('CUENTA_PAGAR_ANULAR', 'Anular cuentas por pagar',
     'Anular cuentas por pagar sin pagos activos (operación crítica)',
     'Cuentas por Pagar', true, true, NULL, NOW(), NULL, NULL);

INSERT IGNORE INTO rol_permiso (id_rol, id_permiso)
    SELECT r.id_rol, p.id_permiso
    FROM rol r
    JOIN permiso p ON p.codigo = 'CUENTA_PAGAR_ANULAR'
    WHERE r.codigo = 'ADMIN';
-- fin-permiso

UPDATE proveedores p
SET p.saldo_pendiente = COALESCE((
    SELECT SUM(ROUND(c.saldo_pendiente *
                     CASE WHEN c.moneda IS NULL OR c.moneda = 'CRC' OR c.tipo_cambio IS NULL OR c.tipo_cambio <= 0
                          THEN 1 ELSE c.tipo_cambio END, 2))
    FROM cuentas_por_pagar c
    WHERE c.proveedor_id = p.id AND c.estado <> 'ANULADA'), 0);

UPDATE cuentas_contables
SET nombre = CONCAT('IVA Cr', CONVERT(0xC3A9 USING utf8mb4), 'dito Fiscal')
WHERE codigo = '1.1.03'
  AND nombre LIKE 'IVA Cr%dito Fiscal'
  AND nombre <> CONCAT('IVA Cr', CONVERT(0xC3A9 USING utf8mb4), 'dito Fiscal');
