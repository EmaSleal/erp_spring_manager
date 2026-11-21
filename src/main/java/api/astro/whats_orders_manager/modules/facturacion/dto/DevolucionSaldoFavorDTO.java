package api.astro.whats_orders_manager.modules.facturacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Una devolución en dinero de un saldo a favor con su historial (T-049). Los campos de cada transición (usuario, fecha) son
 * null mientras no ocurra. Los nombres son los del usuario (null si no se pudo resolver). Los diferenciales están en CRC:
 * {@code diferencialNeto} positivo = ganancia, negativo = pérdida; solo hay TC del día en USD.
 */
public record DevolucionSaldoFavorDTO(Long id, Long idSaldoFavor, String moneda, BigDecimal monto, String estado,
                                      String medioSolicitud, LocalDate fechaSolicitud, String iban, String nombreTitular,
                                      String identificacionTitular,
                                      Integer solicitadoPor, String solicitadoPorNombre, LocalDateTime fechaCreacion,
                                      Integer aprobadoPor, String aprobadoPorNombre, LocalDateTime fechaAprobacion,
                                      boolean verificoCuenta, LocalDateTime fechaVerificacion,
                                      Integer rechazadoPor, String rechazadoPorNombre, LocalDateTime fechaRechazo,
                                      Integer anuladoPor, String anuladoPorNombre, LocalDateTime fechaAnulacion,
                                      String razon,
                                      Integer pagadoPor, String pagadoPorNombre, LocalDateTime fechaPago,
                                      Integer idCuentaBancaria, String cuentaBancaria, String numeroComprobante,
                                      LocalDate fechaComprobante, BigDecimal tipoCambioOrigen, LocalDate tcDiaFecha,
                                      BigDecimal tcDiaValor, String tcDiaTipo, String tcDiaFuente, String tcDiaOrigen,
                                      BigDecimal diferencialSaldo, BigDecimal diferencialNeto, Long idAsiento,
                                      String numeroAsiento) {
}
