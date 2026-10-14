package api.astro.whats_orders_manager.modules.proveedores.dto;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.proveedores.enums.MetodoPagoProveedor;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoProveedorDTO {

    private Long cuentaPorPagarId;
    private BigDecimal monto;
    /** Moneda del pago (ADR-011). Null → moneda de la cuenta por pagar. */
    private MonedaFE moneda;
    /** Tipo de cambio de la operación (ADR-011). Null → tasa de venta registrada para la fecha del pago. */
    private BigDecimal tipoCambio;
    private MetodoPagoProveedor metodoPago;
    /**
     * Cuenta bancaria propia de origen (ADR-018): obligatoria para TRANSFERENCIA, CHEQUE, SINPE y
     * DEPOSITO, ignorada en EFECTIVO. Debe estar activa y en la moneda del pago.
     */
    private Integer idCuentaBancariaOrigen;
    private String referencia;
    private LocalDate fecha;
    private String notas;
}
