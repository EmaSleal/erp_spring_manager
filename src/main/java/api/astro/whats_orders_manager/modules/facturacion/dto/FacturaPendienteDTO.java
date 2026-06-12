package api.astro.whats_orders_manager.modules.facturacion.dto;

import java.math.BigDecimal;

public record FacturaPendienteDTO(
    Integer idFactura,
    String numeroFactura,
    BigDecimal totalFactura,
    BigDecimal saldoPendiente) {}
