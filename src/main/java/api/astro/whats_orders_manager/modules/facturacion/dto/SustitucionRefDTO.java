package api.astro.whats_orders_manager.modules.facturacion.dto;

/** Referencia a la otra factura de una sustitución (ADR-045): id y número; numeroNota solo en «sustituye a». */
public record SustitucionRefDTO(Integer idFactura, String numeroFactura, String numeroNota) {
}
