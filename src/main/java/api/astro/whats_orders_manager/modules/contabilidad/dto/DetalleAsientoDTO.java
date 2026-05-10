package api.astro.whats_orders_manager.modules.contabilidad.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para transferencia de datos de DetalleAsiento.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleAsientoDTO {
    
    private Long idDetalle;
    
    // Relación con asiento
    private Long asientoId;
    private String asientoNumero;
    
    // Cuenta contable
    private Long cuentaId;
    private String cuentaCodigo;
    private String cuentaNombre;
    
    // Montos (debe XOR haber)
    @Builder.Default
    private BigDecimal debe = BigDecimal.ZERO;
    
    @Builder.Default
    private BigDecimal haber = BigDecimal.ZERO;
    
    private String descripcion;
    
    // Campos de auditoría
    private Integer creadoPor;
    private Integer modificadoPor;
    
    // Información calculada
    private BigDecimal monto;
    private Boolean esDebe;
    private Boolean esHaber;
}
