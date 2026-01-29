package api.astro.whats_orders_manager.modules.facturacion.electronica.controller;

import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.HaciendaCallbackDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ComprobanteElectronicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para recibir callbacks de Hacienda de Costa Rica.
 * 
 * <p>Este endpoint recibe notificaciones asíncronas cuando Hacienda procesa
 * un comprobante electrónico y cambia su estado.</p>
 * 
 * <p><strong>Seguridad:</strong></p>
 * <ul>
 *   <li>Valida token de autenticación en header X-Webhook-Token</li>
 *   <li>Registra todos los callbacks recibidos en logs</li>
 *   <li>Maneja errores y devuelve códigos HTTP apropiados</li>
 * </ul>
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
@RestController
@RequestMapping("/api/hacienda")
@RequiredArgsConstructor
@Slf4j
public class HaciendaWebhookController {
    
    private final ComprobanteElectronicoService comprobanteService;
    
    /**
     * Token de seguridad para validar callbacks de Hacienda.
     * Si está vacío, no se valida el token.
     */
    @Value("${facturacion.hacienda.webhook.token:}")
    private String webhookToken;
    
    /**
     * Recibe callback de Hacienda cuando el estado de un comprobante cambia.
     * 
     * <p><strong>Ejemplo de llamada:</strong></p>
     * <pre>
     * POST /api/hacienda/callback HTTP/1.1
     * Host: tu-dominio.com
     * Content-Type: application/json
     * X-Webhook-Token: tu-token-secreto
     * 
     * {
     *   "claveNumerica": "50612202600100111234567890123456789012345678901234",
     *   "estado": "aceptado",
     *   "codigoRespuesta": "1",
     *   "mensaje": "Comprobante aceptado",
     *   "xmlRespuesta": "PD94bWwgdmVyc2lvbj0iMS4wIi...",
     *   "fechaRespuesta": "2026-01-24T15:30:00"
     * }
     * </pre>
     * 
     * @param callback Datos del callback enviados por Hacienda
     * @param authToken Token de autenticación (header X-Webhook-Token)
     * @return ResponseEntity con resultado del procesamiento
     */
    @PostMapping("/callback")
    public ResponseEntity<Map<String, Object>> recibirCallback(
            @Valid @RequestBody HaciendaCallbackDTO callback,
            @RequestHeader(value = "X-Webhook-Token", required = false) String authToken
    ) {
        log.info("📥 Callback recibido de Hacienda para clave: {}", callback.getClaveNumerica());
        log.debug("📋 Datos del callback: estado={}, código={}, mensaje={}", 
            callback.getEstado(), callback.getCodigoRespuesta(), callback.getMensaje());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 1. Validar token de autenticación (si está configurado)
            if (!webhookToken.isEmpty()) {
                if (authToken == null || authToken.trim().isEmpty()) {
                    log.warn("⚠️ Token de autenticación no proporcionado");
                    response.put("success", false);
                    response.put("message", "Token de autenticación requerido");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
                }
                
                if (!webhookToken.equals(authToken)) {
                    log.warn("⚠️ Token de autenticación inválido. Recibido: {}", 
                        authToken.substring(0, Math.min(10, authToken.length())) + "...");
                    response.put("success", false);
                    response.put("message", "Token de autenticación inválido");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
                }
            }
            
            // 2. Procesar callback
            log.info("🔄 Procesando callback para clave: {}", callback.getClaveNumerica());
            comprobanteService.procesarCallbackHacienda(callback);
            
            log.info("✅ Callback procesado exitosamente para clave: {}", callback.getClaveNumerica());
            
            response.put("success", true);
            response.put("message", "Callback procesado exitosamente");
            response.put("claveNumerica", callback.getClaveNumerica());
            response.put("estado", callback.getEstado());
            
            return ResponseEntity.ok(response);
                
        } catch (jakarta.persistence.EntityNotFoundException e) {
            log.error("❌ Comprobante no encontrado: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Comprobante no encontrado: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            
        } catch (IllegalArgumentException e) {
            log.error("❌ Datos inválidos en callback: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Datos inválidos: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            
        } catch (Exception e) {
            log.error("❌ Error procesando callback de Hacienda: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error interno procesando callback: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Endpoint de salud para verificar que el webhook está activo.
     * 
     * @return Estado del webhook
     */
    @GetMapping("/callback/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("webhook", "active");
        response.put("tokenRequired", !webhookToken.isEmpty());
        return ResponseEntity.ok(response);
    }
}
