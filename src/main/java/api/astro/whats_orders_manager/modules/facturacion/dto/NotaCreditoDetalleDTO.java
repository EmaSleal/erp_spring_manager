package api.astro.whats_orders_manager.modules.facturacion.dto;

import api.astro.whats_orders_manager.modules.facturacion.correo.dto.CorreoDetalleDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Detalle de una nota de crédito (T-042, ADR-047): datos, líneas, referencia, comprobante, historial y asiento (T-046: fecha contable, período fiscal de atribución y fecha del asiento). T-048: reparto del total de la nota al aplicarse: {@code montoCreditoCxc} a cuentas por cobrar y {@code montoSaldoFavor} a saldo a favor ({@code idSaldoFavor}); null hasta que la nota está APLICADA. */
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
                                    LocalDate fechaAsientoReversion, boolean parcial,
                                    BigDecimal montoCreditoCxc, BigDecimal montoSaldoFavor, Long idSaldoFavor,
                                    CorreoDetalleDTO correo) {

    /** Sin el campo de correo (T-055): el controlador lo completa con {@link #conCorreo}. */
    public NotaCreditoDetalleDTO(Long id, String numero, String estado, String tipo, String codigoReferencia,
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
                                 LocalDate fechaAsientoReversion, boolean parcial,
                                 BigDecimal montoCreditoCxc, BigDecimal montoSaldoFavor, Long idSaldoFavor) {
        this(id, numero, estado, tipo, codigoReferencia, codigoNombre, razon, fechaEmision, fechaAplicacion, moneda,
            tipoCambio, subtotal, descuento, impuesto, total, usuarioCreador, idFactura, numeroFactura, clienteNombre,
            claveReferencia, fechaEmisionReferencia, lineas, comprobante, historial, idAsientoReversion,
            numeroAsientoReversion, puedeEmitirSustituta, motivoSustitutaNoDisponible, fechaContable,
            periodoFiscalAtribucion, fechaAsientoReversion, parcial, montoCreditoCxc, montoSaldoFavor, idSaldoFavor, null);
    }

    public NotaCreditoDetalleDTO conCorreo(CorreoDetalleDTO nuevo) {
        return new NotaCreditoDetalleDTO(id, numero, estado, tipo, codigoReferencia, codigoNombre, razon, fechaEmision,
            fechaAplicacion, moneda, tipoCambio, subtotal, descuento, impuesto, total, usuarioCreador, idFactura,
            numeroFactura, clienteNombre, claveReferencia, fechaEmisionReferencia, lineas, comprobante, historial,
            idAsientoReversion, numeroAsientoReversion, puedeEmitirSustituta, motivoSustitutaNoDisponible, fechaContable,
            periodoFiscalAtribucion, fechaAsientoReversion, parcial, montoCreditoCxc, montoSaldoFavor, idSaldoFavor, nuevo);
    }
}
