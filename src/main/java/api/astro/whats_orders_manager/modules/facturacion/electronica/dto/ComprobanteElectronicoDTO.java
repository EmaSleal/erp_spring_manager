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
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private String xmlComprobante;
    
    private String xmlRespuesta;
}
