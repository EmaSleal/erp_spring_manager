package api.astro.whats_orders_manager.dto.whatsapp;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO para validar webhooks de Meta WhatsApp Business API
 * Meta envía este request al registrar el webhook por primera vez
 * 
 * @author EmaSleal
 * @version 1.0
 * @since Sprint 3 - Fase 1.2
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebhookValidationDTO {
    
    @NotBlank(message = "El modo de validación es obligatorio")
    private String mode; // "subscribe"
    
    @NotBlank(message = "El token de verificación es obligatorio")
    private String verify_token;
    
    @NotBlank(message = "El challenge es obligatorio")
    private String challenge; // Código que debemos devolver para validar
    
    /**
     * Verifica si el token coincide con el configurado
     * 
     * @param expectedToken Token esperado configurado en el sistema
     * @return true si coincide
     */
    public boolean isTokenValid(String expectedToken) {
        return expectedToken != null && expectedToken.equals(verify_token);
    }
    
    /**
     * Verifica si el modo es de suscripción
     */
    public boolean isSubscribeMode() {
        return "subscribe".equals(mode);
    }
}
