package api.astro.whats_orders_manager.modules.facturacion.dto;

import java.math.BigDecimal;

/**
 * Factura con saldo pendiente para el selector del formulario de pagos.
 * monedaFE y tipoCambio (ADR-011) permiten mostrar el saldo en su moneda y
 * habilitar el tipo de cambio del pago cuando hay moneda extranjera.
 */
public record FacturaPendienteDTO(
    Integer idFactura,
    String numeroFactura,
    BigDecimal totalFactura,
    BigDecimal saldoPendiente,
    String monedaFE,
    BigDecimal tipoCambio) {}
