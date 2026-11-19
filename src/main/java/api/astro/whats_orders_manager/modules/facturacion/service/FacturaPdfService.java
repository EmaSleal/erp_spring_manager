package api.astro.whats_orders_manager.modules.facturacion.service;

public interface FacturaPdfService {

    byte[] generarPdfFactura(Integer facturaId);

    /** PDF de una nota de crédito (T-042): mismas reglas de estado que la factura, con el bloque «Referencia». */
    byte[] generarPdfNotaCredito(Long notaId);
}
