package api.astro.whats_orders_manager.modules.facturacion.electronica.dto;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MensajeHacienda;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO para respuesta de Hacienda.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespuestaHaciendaDTO {
    
    private Long id;
    
    private Long comprobanteId;
    
    private String claveNumerica;
    
    private LocalDateTime fechaRespuesta;
    
    private MensajeHacienda codigoMensaje;
    
    private String mensaje;

    private String indicadorEstado;
    
    private Boolean exitoso;
    
    private String detalles;
    
    private Integer codigoHttp;
    
    private Long tiempoRespuestaMs;
    
    private Boolean debeReintentar;
    
    private LocalDateTime createdAt;

    // public String getIndicadorEstado() {
    //     return exitoso != null && exitoso ? "✅" : "❌";
    // }
}
