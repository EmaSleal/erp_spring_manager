package api.astro.whats_orders_manager.modules.facturacion.electronica.dto;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO para comprobante electrónico.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComprobanteElectronicoDTO {
    
    private Long id;
    
    private Long facturaId;
    
    private String facturaNumero;
    
    private Long empresaId;
    
    private String empresaNombre;
    
    private TipoComprobanteElectronico tipoComprobante;
    
    private String tipoComprobanteDescripcion;
    
    private String claveNumerica;
    
    private String consecutivo;
    
    private LocalDateTime fechaEmision;
    
    private EstadoComprobante estado;
    
    private String estadoDescripcion;
    
    private String codigoRespuesta;
    
    private String mensajeRespuesta;
    
    private LocalDateTime fechaEnvio;
    
    private LocalDateTime fechaRespuesta;
    
    private Integer intentosEnvio;
    
    private String ultimoError;
    
    private Boolean enviadoEmail;
    
    private LocalDateTime fechaEnvioEmail;
    
    private String urlPdf;
    
    private Boolean puedeReenviar;
    
    private Boolean listoParaEnviar;

    /** Comprobante vigente de su factura (los demás son historial, p. ej. un RECHAZADO reemitido). */
    private Boolean vigente;

    /**
     * ENVIO_INCIERTO: momento a partir del cual un 404 de "Consultar estado en Hacienda" lo marca NO_TRANSMITIDO
     * (ADR-031 §4); antes se puede consultar igual, pero un 404 no cambia el estado.
     */
    private LocalDateTime verificableDesde;

    /** Se puede "Consultar estado en Hacienda": ENVIADO, PROCESANDO, ENVIO_INCIERTO o ERROR_HACIENDA (ADR-031 §4). */
    private Boolean consultableEnHacienda;

    /** Consultas seguidas en que Hacienda informó ind-estado "error". */
    private Integer consultasErrorHacienda;

    /** ERROR_HACIENDA tras el máximo de consultas seguidas con "error": el sondeo ya no lo consulta; revisar en ATV. */
    private Boolean revisarEnAtv;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private String xmlComprobante;
    
    private String xmlRespuesta;
}
