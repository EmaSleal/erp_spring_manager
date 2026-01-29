package api.astro.whats_orders_manager.modules.facturacion.electronica.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO para estadísticas de tendencia de envíos de comprobantes electrónicos.
 * 
 * <p>Contiene datos para generar gráficos de tendencia mostrando
 * envíos por día durante un período específico.</p>
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TendenciaEnviosDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Fechas del período (eje X del gráfico).
     * Formato: "2026-01-24"
     */
    @Builder.Default
    private List<String> fechas = new ArrayList<>();
    
    /**
     * Cantidad de comprobantes aceptados por día.
     */
    @Builder.Default
    private List<Integer> aceptados = new ArrayList<>();
    
    /**
     * Cantidad de comprobantes rechazados por día.
     */
    @Builder.Default
    private List<Integer> rechazados = new ArrayList<>();
    
    /**
     * Cantidad de comprobantes pendientes por día.
     */
    @Builder.Default
    private List<Integer> pendientes = new ArrayList<>();
    
    /**
     * Cantidad de comprobantes con error por día.
     */
    @Builder.Default
    private List<Integer> errores = new ArrayList<>();
    
    /**
     * Total de comprobantes por día.
     */
    @Builder.Default
    private List<Integer> total = new ArrayList<>();
    
    /**
     * Agrega estadísticas de un día específico.
     * 
     * @param fecha Fecha en formato "yyyy-MM-dd"
     * @param aceptados Cantidad de aceptados
     * @param rechazados Cantidad de rechazados
     * @param pendientes Cantidad de pendientes
     * @param errores Cantidad de errores
     */
    public void agregarDia(String fecha, int aceptados, int rechazados, int pendientes, int errores) {
        this.fechas.add(fecha);
        this.aceptados.add(aceptados);
        this.rechazados.add(rechazados);
        this.pendientes.add(pendientes);
        this.errores.add(errores);
        this.total.add(aceptados + rechazados + pendientes + errores);
    }
}
