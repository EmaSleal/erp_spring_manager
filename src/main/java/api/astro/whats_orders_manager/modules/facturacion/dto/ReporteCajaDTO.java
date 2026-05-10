package api.astro.whats_orders_manager.modules.facturacion.dto;

import api.astro.whats_orders_manager.modules.facturacion.enums.MetodoPago;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para reportes de caja diaria.
 * Agrupa los pagos del día por método de pago.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteCajaDTO {
    
    private LocalDate fecha;
    private LocalDateTime fechaGeneracion;
    
    private BigDecimal totalIngresos;
    private Long cantidadPagos;
    
    private BigDecimal totalEfectivo;
    private Long cantidadEfectivo;
    
    private BigDecimal totalTarjeta;
    private Long cantidadTarjeta;
    
    private BigDecimal totalCheque;
    private Long cantidadCheque;
    
    private BigDecimal totalTransferencia;
    private Long cantidadTransferencia;
    
    private BigDecimal totalRecaudado;
    private Long cantidadRecaudado;
    
    private BigDecimal totalOtros;
    private Long cantidadOtros;
    
    private List<PagoDTO> pagosDetalle;
    
    /**
     * Calcula el total de ingresos sumando todos los métodos de pago.
     */
    public void calcularTotales() {
        this.totalIngresos = BigDecimal.ZERO
            .add(totalEfectivo != null ? totalEfectivo : BigDecimal.ZERO)
            .add(totalTarjeta != null ? totalTarjeta : BigDecimal.ZERO)
            .add(totalCheque != null ? totalCheque : BigDecimal.ZERO)
            .add(totalTransferencia != null ? totalTransferencia : BigDecimal.ZERO)
            .add(totalRecaudado != null ? totalRecaudado : BigDecimal.ZERO)
            .add(totalOtros != null ? totalOtros : BigDecimal.ZERO);
        
        this.cantidadPagos = 
            (cantidadEfectivo != null ? cantidadEfectivo : 0L) +
            (cantidadTarjeta != null ? cantidadTarjeta : 0L) +
            (cantidadCheque != null ? cantidadCheque : 0L) +
            (cantidadTransferencia != null ? cantidadTransferencia : 0L) +
            (cantidadRecaudado != null ? cantidadRecaudado : 0L) +
            (cantidadOtros != null ? cantidadOtros : 0L);
    }
    
    /**
     * Obtiene el total para un método de pago específico.
     * @param metodoPago Método de pago
     * @return Total del método de pago
     */
    public BigDecimal getTotalPorMetodo(MetodoPago metodoPago) {
        if (metodoPago == null) return BigDecimal.ZERO;
        
        return switch (metodoPago) {
            case EFECTIVO -> totalEfectivo != null ? totalEfectivo : BigDecimal.ZERO;
            case TARJETA -> totalTarjeta != null ? totalTarjeta : BigDecimal.ZERO;
            case CHEQUE -> totalCheque != null ? totalCheque : BigDecimal.ZERO;
            case TRANSFERENCIA -> totalTransferencia != null ? totalTransferencia : BigDecimal.ZERO;
            case RECAUDADO -> totalRecaudado != null ? totalRecaudado : BigDecimal.ZERO;
            case OTROS -> totalOtros != null ? totalOtros : BigDecimal.ZERO;
        };
    }
    
    /**
     * Obtiene la cantidad para un método de pago específico.
     * @param metodoPago Método de pago
     * @return Cantidad de pagos del método
     */
    public Long getCantidadPorMetodo(MetodoPago metodoPago) {
        if (metodoPago == null) return 0L;
        
        return switch (metodoPago) {
            case EFECTIVO -> cantidadEfectivo != null ? cantidadEfectivo : 0L;
            case TARJETA -> cantidadTarjeta != null ? cantidadTarjeta : 0L;
            case CHEQUE -> cantidadCheque != null ? cantidadCheque : 0L;
            case TRANSFERENCIA -> cantidadTransferencia != null ? cantidadTransferencia : 0L;
            case RECAUDADO -> cantidadRecaudado != null ? cantidadRecaudado : 0L;
            case OTROS -> cantidadOtros != null ? cantidadOtros : 0L;
        };
    }
}
