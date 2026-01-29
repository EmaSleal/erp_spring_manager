package api.astro.whats_orders_manager.modules.contabilidad.dto;

import api.astro.whats_orders_manager.modules.contabilidad.enums.NaturalezaCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoCuenta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO para transferencia de datos de CuentaContable.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuentaContableDTO {
    
    private Long idCuenta;
    private String codigo;
    private String nombre;
    private String descripcion;
    private TipoCuenta tipo;
    private NaturalezaCuenta naturaleza;
    private Integer nivel;
    
    @Builder.Default
    private Boolean activa = true;
    
    @Builder.Default
    private Boolean aceptaMovimientos = true;
    
    // Relación jerárquica
    private Long cuentaPadreId;
    private String cuentaPadreCodigo;
    private String cuentaPadreNombre;
    
    @Builder.Default
    private List<CuentaContableDTO> subcuentas = new ArrayList<>();
    
    // Campos de auditoría
    private Integer creadoPor;
    private LocalDateTime fechaCreacion;
    private Integer modificadoPor;
    private LocalDateTime fechaModificacion;
    
    // Información adicional
    private Long cantidadSubcuentas;
    private Boolean tieneMovimientos;
}
