-- ============================================================================
-- V63: adjunto del comprobante de la cuenta por pagar (T-058, ADR-077 §7)
-- ============================================================================
-- La compra de contado permite adjuntar el comprobante del proveedor (XML, PDF, JPG o PNG). El archivo vive en el directorio
-- de uploads (cxp/<id>/<UUID>.<ext>, nombre no adivinable) y la CxP guarda solo la referencia:
--   adjunto_ruta            ruta relativa al directorio de uploads; NULL si la CxP no tiene adjunto.
--   adjunto_nombre_original nombre con que se subió el archivo (solo para mostrarlo y nombrar la descarga).
--   adjunto_tipo            tipo de contenido derivado de la extensión permitida (no del cliente).
-- Las CxP existentes quedan sin adjunto (NULL). Sin CHECK ni índices: se leen siempre por el id de la CxP.
-- ============================================================================

ALTER TABLE cuentas_por_pagar
    ADD COLUMN adjunto_ruta            VARCHAR(120) NULL,
    ADD COLUMN adjunto_nombre_original VARCHAR(255) NULL,
    ADD COLUMN adjunto_tipo            VARCHAR(100) NULL;
