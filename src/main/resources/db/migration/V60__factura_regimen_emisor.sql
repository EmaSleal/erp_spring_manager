-- T-057 (ADR-073 Opción A, ADR-076): régimen tributario sellado en la factura. regimen_emisor se fija al crear la factura con
-- el régimen vigente en su fecha (RegimenTributarioService.vigenteEn) y no se recalcula nunca, ni al editarla.
--
-- Las facturas existentes se rellenan con GENERAL: es como se calcularon (IVA explícito, asiento con IVA por pagar). Eso
-- incluye las de julio y agosto de 2026, que la semilla de V58 (SIMPLIFICADO desde 2000-01-01, GENERAL desde 2026-09-01) ubicaría
-- en el tramo SIMPLIFICADO: el sello describe cómo se calculó cada factura, no el tramo histórico. Quedan cubiertas, desde el
-- primer tramo SIMPLIFICADO, por el bloqueo de emisión de ADR-076 §2.
--
-- Sin valor por defecto después del relleno: toda factura nueva debe traer su régimen (lo fija FacturaServiceImpl).
ALTER TABLE factura
    ADD COLUMN regimen_emisor VARCHAR(12) NOT NULL DEFAULT 'GENERAL',
    ADD CONSTRAINT ck_factura_regimen_emisor CHECK (regimen_emisor IN ('GENERAL', 'SIMPLIFICADO'));

ALTER TABLE factura ALTER COLUMN regimen_emisor DROP DEFAULT;
