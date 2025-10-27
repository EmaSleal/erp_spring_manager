package api.astro.whats_orders_manager.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para manejar el webhook de WhatsApp Meta Business API
 * 
 * Este endpoint debe ser PÚBLICO (sin autenticación) para que Meta pueda:
 * 1. Verificar el webhook (GET request)
 * 2. Enviar notificaciones (POST request)
 * 
 * @author EmaSleal
 * @since Sprint 3 - Fase 0
 */
@Slf4j
@RestController
@RequestMapping("/api/whatsapp/webhook")
public class WhatsAppWebhookController {

    @Value("${meta.webhook.verify-token:}")
    private String webhookVerifyToken;

    /**
     * Endpoint GET para verificación del webhook por Meta
     * 
     * Meta enviará:
     * - hub.mode=subscribe
     * - hub.verify_token=[tu token]
     * - hub.challenge=[código aleatorio]
     * 
     * Debemos responder con el hub.challenge si el verify_token es correcto
     */
    @GetMapping
    public ResponseEntity<?> verifyWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {

        log.info("📞 Webhook verification request received");
        log.info("Mode: {}", mode);
        log.info("Token received: {}", token);
        log.info("Challenge: {}", challenge);

        // Validar parámetros
        if (mode == null || token == null || challenge == null) {
            log.error("❌ Missing required parameters");
            return ResponseEntity.badRequest().body("Missing required parameters");
        }

        // Validar modo
        if (!"subscribe".equals(mode)) {
            log.error("❌ Invalid mode: {}", mode);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid mode");
        }

        // Validar token
        if (webhookVerifyToken.isEmpty()) {
            log.error("❌ Webhook verify token not configured in application.yml");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Webhook verify token not configured");
        }

        if (!webhookVerifyToken.equals(token)) {
            log.error("❌ Invalid verify token. Expected: {}, Received: {}", 
                    webhookVerifyToken, token);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid verify token");
        }

        // ✅ Todo correcto, devolver el challenge
        log.info("✅ Webhook verification successful! Responding with challenge: {}", challenge);
        return ResponseEntity.ok(challenge);
    }

    /**
     * Endpoint POST para recibir notificaciones de WhatsApp
     * 
     * Meta enviará notificaciones cuando:
     * - Se recibe un mensaje
     * - Cambia el estado de un mensaje (enviado, entregado, leído)
     * - Etc.
     */
    @PostMapping
    public ResponseEntity<?> receiveNotification(@RequestBody String payload) {
        log.info("📨 Webhook notification received");
        log.info("Payload: {}", payload);

        // TODO: Implementar lógica de procesamiento de mensajes (Sprint 3 - Fase 1)
        // Por ahora solo loggeamos y respondemos 200 OK

        return ResponseEntity.ok().build();
    }
}
