package api.astro.whats_orders_manager.controllers;

import api.astro.whats_orders_manager.models.dto.whatsapp.MetaWebhookRequest;
import api.astro.whats_orders_manager.models.dto.WebhookValidationDTO;
import api.astro.whats_orders_manager.services.WebhookWhatsAppService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * Controlador para manejar el webhook de WhatsApp Meta Business API
 * 
 * Este endpoint debe ser PÚBLICO (sin autenticación) para que Meta pueda:
 * 1. Verificar el webhook (GET request)
 * 2. Enviar notificaciones (POST request)
 * 
 * @author EmaSleal
 * @version 2.0
 * @since Sprint 3 - Fase 1.4 (Actualizado)
 */
@Slf4j
@RestController
@RequestMapping("/api/whatsapp/webhook")
public class WhatsAppWebhookController {
    
    private final WebhookWhatsAppService webhookService;

    @Value("${meta.webhook.verify-token:}")
    private String webhookVerifyToken;
    
    public WhatsAppWebhookController(WebhookWhatsAppService webhookService) {
        this.webhookService = webhookService;
    }

    /**
     * Endpoint GET para verificación del webhook por Meta
     * 
     * Meta enviará:
     * - hub.mode=subscribe
     * - hub.verify_token=[tu token]
     * - hub.challenge=[código aleatorio]
     * 
     * Debemos responder con el hub.challenge si el verify_token es correcto
     * 
     * GET /api/whatsapp/webhook?hub.mode=subscribe&hub.verify_token=TOKEN&hub.challenge=CHALLENGE
     */
    @GetMapping
    public ResponseEntity<?> verifyWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {

        log.info("📞 Webhook verification request received");
        log.info("Mode: {}, Token: {}, Challenge: {}", mode, token != null ? "***" : "null", challenge);

        // Validar parámetros
        if (mode == null || token == null || challenge == null) {
            log.error("❌ Missing required parameters");
            return ResponseEntity.badRequest().body("Missing required parameters");
        }

        // Usar DTO para validación
        WebhookValidationDTO validation = new WebhookValidationDTO(mode, token, challenge);

        // Validar modo
        if (!validation.isSubscribeMode()) {
            log.error("❌ Invalid mode: {}", mode);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid mode");
        }

        // Validar token
        if (webhookVerifyToken.isEmpty()) {
            log.error("❌ Webhook verify token not configured in application.yml");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Webhook verify token not configured");
        }

        if (!validation.isTokenValid(webhookVerifyToken)) {
            log.error("❌ Invalid verify token");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid verify token");
        }

        // ✅ Todo correcto, devolver el challenge
        log.info("✅ Webhook verification successful! Responding with challenge");
        return ResponseEntity.ok(challenge);
    }

    /**
     * Endpoint POST para recibir notificaciones de WhatsApp
     * 
     * Meta enviará notificaciones cuando:
     * - Se recibe un mensaje
     * - Cambia el estado de un mensaje (enviado, entregado, leído)
     * - Ocurre un error
     * 
     * POST /api/whatsapp/webhook
     */
    @PostMapping
    public ResponseEntity<String> receiveNotification(@Valid @RequestBody MetaWebhookRequest webhookRequest) {
        
        log.info("📨 Webhook notification received - Object: {}", 
                webhookRequest.getObject() != null ? webhookRequest.getObject() : "N/A");

        try {
            // Validar que sea un webhook de WhatsApp Business
            if (!"whatsapp_business_account".equals(webhookRequest.getObject())) {
                log.warn("⚠️ Webhook ignorado - Object no es whatsapp_business_account: {}", 
                        webhookRequest.getObject());
                return ResponseEntity.ok("IGNORED");
            }
            
            // Procesar webhook usando el servicio
            webhookService.procesarWebhook(webhookRequest);
            
            log.info("✅ Webhook procesado exitosamente");
            return ResponseEntity.ok("EVENT_RECEIVED");
            
        } catch (Exception e) {
            log.error("❌ Error al procesar webhook", e);
            // Devolver 200 para que Meta no reintente indefinidamente
            return ResponseEntity.ok("ERROR_PROCESSING");
        }
    }
    
    /**
     * Endpoint para probar el webhook manualmente (solo para desarrollo/testing)
     * 
     * POST /api/whatsapp/webhook/test
     */
    @PostMapping("/test")
    public ResponseEntity<String> testWebhook(@RequestBody MetaWebhookRequest webhookRequest) {
        
        log.info("🧪 Webhook de prueba recibido");
        
        try {
            webhookService.procesarWebhook(webhookRequest);
            return ResponseEntity.ok("Webhook de prueba procesado exitosamente");
        } catch (Exception e) {
            log.error("❌ Error en webhook de prueba", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Health check del servicio webhook
     * 
     * GET /api/whatsapp/webhook/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("✅ Webhook service is running");
    }
}
