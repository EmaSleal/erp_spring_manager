package api.astro.whats_orders_manager.modules.facturacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Vistas del saldo a favor del cliente para la interfaz (T-048). */
public final class SaldoFavorDTO {

    private SaldoFavorDTO() {
    }

    /** Un saldo a favor con su nota de origen. {@code numeroNota} es el consecutivo del comprobante de la nota. */
    public record Saldo(Long id, Integer idCliente, String moneda, BigDecimal montoOriginal, BigDecimal saldoVivo,
                        LocalDate fechaOrigen, BigDecimal tipoCambioOrigen, String estado, Long idNota,
                        String numeroNota, String claveNota, Integer idFacturaOrigen, String numeroFacturaOrigen,
                        BigDecimal reservado, BigDecimal disponible) {
    }

    /**
     * Una aplicación (VIGENTE o REVERSADA). Los diferenciales están en CRC; {@code diferencialNeto} positivo es ganancia.
     * Los campos del TC del día y de la reversión son null cuando no aplican.
     */
    public record Aplicacion(Long id, Long idSaldoFavor, Long idNota, String numeroNota, String claveNota,
                             String estado, BigDecimal monto,
                             String moneda, Integer idFactura, String numeroFactura, Long idPago, String numeroPago,
                             LocalDateTime fechaAplicacion, Integer usuarioId, BigDecimal tipoCambioOrigen,
                             BigDecimal tipoCambioFactura, LocalDate tcDiaFecha, BigDecimal tcDiaValor,
                             String tcDiaTipo, String tcDiaFuente, String tcDiaOrigen, BigDecimal diferencialSaldo,
                             BigDecimal diferencialCxc, BigDecimal diferencialNeto, String razonReversion,
                             Integer reversadoPor, LocalDateTime fechaReversion, Long idAsientoReversion) {
    }

    /** Respuesta para el modal de pago de una factura. */
    public record ParaFactura(Integer idFactura, String numeroFactura, String moneda, BigDecimal saldoPendiente,
                              boolean admiteAplicacion, List<Saldo> saldos, List<Aplicacion> aplicaciones) {
    }

    /**
     * Un saldo con sus aplicaciones y sus devoluciones en dinero con su historial (estado de cuenta del cliente).
     * {@code saldo.reservado} es lo reservado por devoluciones SOLICITADAS o APROBADAS y {@code saldo.disponible} es
     * saldo vivo − reservado.
     */
    public record ConAplicaciones(Saldo saldo, List<Aplicacion> aplicaciones, List<DevolucionSaldoFavorDTO> devoluciones) {
    }
}
