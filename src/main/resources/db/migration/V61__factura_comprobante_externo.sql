-- T-057 (ADR-076 §3-4): clave del comprobante que se emitió fuera del ERP (en la herramienta de Hacienda) para una factura
-- del régimen general. La clave es de 50 dígitos y única entre facturas (MySQL permite varios NULL en un índice único);
-- la fecha es la de emisión de ese comprobante. Ambas son NULL mientras la factura no tenga un comprobante externo.
ALTER TABLE factura
    ADD COLUMN comprobante_externo_clave VARCHAR(50) NULL,
    ADD COLUMN comprobante_externo_fecha DATE NULL,
    ADD CONSTRAINT uk_factura_comprobante_externo_clave UNIQUE (comprobante_externo_clave);
