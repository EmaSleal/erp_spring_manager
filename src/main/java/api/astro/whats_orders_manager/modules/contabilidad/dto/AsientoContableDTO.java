package api.astro.whats_orders_manager.modules.contabilidad.dto;

import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoAsiento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO para transferencia de datos de AsientoContable.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsientoContableDTO {
    
    private Long idAsiento;
    private String numero;
    private LocalDate fecha;
    private String concepto;
    private TipoAsiento tipo;
    private EstadoAsiento estado;
    
    // Relaciones
    private Long facturaId;
    private String facturaNumero;
    
    private Long pagoId;
    private String pagoReferencia;
    
    // Detalles del asiento
    @Builder.Default
    private List<DetalleAsientoDTO> detalles = new ArrayList<>();
    
    // Campos de auditoría
    private Integer creadoPor;
    private Integer modificadoPor;
    private LocalDate fechaCreacion;
    private LocalDate fechaModificacion;
    
    // Totales calculados
    private BigDecimal totalDebe;
    private BigDecimal totalHaber;
    private BigDecimal diferencia;
    private Boolean estaCuadrado;
    
    // Información de estado
    private Boolean puedeModificarse;
    private Boolean puedeContabilizarse;
    private Boolean esAutomatico;
    private Boolean esEditable;
}
