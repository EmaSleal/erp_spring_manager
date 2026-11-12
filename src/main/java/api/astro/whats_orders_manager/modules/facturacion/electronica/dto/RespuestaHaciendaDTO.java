package api.astro.whats_orders_manager.modules.facturacion.electronica.dto;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MensajeHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.ResultadoEnvioHacienda;
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

    private String xmlRespuesta;

    private String indicadorEstado;
    
    private Boolean exitoso;
    
    private String detalles;
    
    private Integer codigoHttp;
    
    private Long tiempoRespuestaMs;
    
    private Boolean debeReintentar;
    /** Envío (POST /recepcion): clasificación en origen que fija el estado del comprobante (ADR-030 §3). */
    private ResultadoEnvioHacienda resultadoEnvio;
    /** Envío: valor de X-Error-Cause cuando Hacienda rechaza la recepción. */
    private String causaError;
    /** Consulta (GET /recepcion/{clave}): true si Hacienda respondió 404, es decir, no tiene el documento. */
    private Boolean documentoNoExiste;

    /**
     * Constructor de la proyección JPQL de RespuestaHaciendaRepository.findByComprobanteIdAsDTO (historial de
     * respuestas guardadas), sin los campos de clasificación del envío y la consulta.
     */
    public RespuestaHaciendaDTO(Long id, Long comprobanteId, String claveNumerica, LocalDateTime fechaRespuesta,
                                MensajeHacienda codigoMensaje, String mensaje, String xmlRespuesta,
                                String indicadorEstado, Boolean exitoso, String detalles, Integer codigoHttp,
                                Long tiempoRespuestaMs, Boolean debeReintentar, LocalDateTime createdAt) {
        this(id, comprobanteId, claveNumerica, fechaRespuesta, codigoMensaje, mensaje, xmlRespuesta, indicadorEstado,
                exitoso, detalles, codigoHttp, tiempoRespuestaMs, debeReintentar, null, null, null, createdAt);
    }
    
    private LocalDateTime createdAt;

    // public String getIndicadorEstado() {
    //     return exitoso != null && exitoso ? "✅" : "❌";
    // }
}
