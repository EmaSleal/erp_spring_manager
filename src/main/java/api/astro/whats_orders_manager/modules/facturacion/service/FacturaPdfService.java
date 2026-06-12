package api.astro.whats_orders_manager.modules.facturacion.service;

public interface FacturaPdfService {

    byte[] generarPdfFactura(Integer facturaId);
}
