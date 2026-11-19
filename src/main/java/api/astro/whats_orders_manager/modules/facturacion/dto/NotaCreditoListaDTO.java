package api.astro.whats_orders_manager.modules.facturacion.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fila del listado de notas de crédito (T-042, ADR-047 §1). Sin entidades JPA: todo lo que la pantalla pinta, incluido
 * el estado del comprobante vigente y lo que habilita «Reenviar» y «Consultar estado».
 */
public record NotaCreditoListaDTO(Long id, String numero, Integer idFactura, String numeroFactura,
                                  String clienteNombre, String codigoReferencia, String codigoNombre,
                                  BigDecimal total, String moneda, LocalDateTime fechaEmision, String estado,
                                  Long idComprobante, String estadoComprobante, LocalDateTime fechaEnvioComprobante,
                                  boolean puedeReenviar, boolean consultableEnHacienda) {
}
