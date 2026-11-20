package api.astro.whats_orders_manager.modules.facturacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Detalle de una nota de crédito (T-042, ADR-047): datos, líneas, referencia, comprobante, historial y asiento (T-046: fecha contable, período fiscal de atribución y fecha del asiento). */
public record NotaCreditoDetalleDTO(Long id, String numero, String estado, String tipo, String codigoReferencia,
                                    String codigoNombre, String razon, LocalDateTime fechaEmision,
                                    LocalDateTime fechaAplicacion, String moneda, BigDecimal tipoCambio,
                                    BigDecimal subtotal, BigDecimal descuento, BigDecimal impuesto, BigDecimal total,
                                    String usuarioCreador, Integer idFactura, String numeroFactura,
                                    String clienteNombre, String claveReferencia,
                                    LocalDateTime fechaEmisionReferencia, List<LineaNotaCreditoDTO> lineas,
                                    ComprobanteNotaDTO comprobante, List<ComprobanteNotaDTO> historial,
                                    Long idAsientoReversion, String numeroAsientoReversion,
                                    boolean puedeEmitirSustituta,
                                    String motivoSustitutaNoDisponible,
                                    LocalDate fechaContable, String periodoFiscalAtribucion,
                                    LocalDate fechaAsientoReversion) {
}
