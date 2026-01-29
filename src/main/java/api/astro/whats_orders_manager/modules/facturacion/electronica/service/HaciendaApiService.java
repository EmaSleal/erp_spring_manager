package api.astro.whats_orders_manager.modules.facturacion.electronica.service;

import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.RespuestaHaciendaDTO;

import java.util.List;

/**
 * Servicio para integración con API de Hacienda de Costa Rica.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
public interface HaciendaApiService {
    
    /**
     * Autentica y obtiene token OAuth2 de Hacienda.
     */
    TokenResponse autenticar(Long configuracionId);
    
    /**
     * Renueva token OAuth2 usando refresh token.
     */
    TokenResponse renovarToken(String refreshToken, Long configuracionId);
    
    /**
     * Envía comprobante a Hacienda.
     */
    RespuestaHaciendaDTO enviarComprobante(String xml, String claveNumerica, Long configuracionId);
    
    /**
     * Consulta estado de un comprobante en Hacienda.
     */
    RespuestaHaciendaDTO consultarComprobante(String claveNumerica, Long configuracionId);
    
    /**
     * Consulta si Hacienda tiene mensajes de respuesta pendientes.
     */
    List<MensajeHaciendaResponse> consultarMensajes(Long configuracionId);
    
    /**
     * Respuesta de autenticación OAuth2.
     */
    record TokenResponse(
        String accessToken,
        String refreshToken,
        Integer expiresIn,
        String tokenType
    ) {}
    
    /**
     * Mensaje de respuesta de Hacienda.
     */
    record MensajeHaciendaResponse(
        String claveNumerica,
        String mensaje,
        String detalle,
        String estado
    ) {}
}
