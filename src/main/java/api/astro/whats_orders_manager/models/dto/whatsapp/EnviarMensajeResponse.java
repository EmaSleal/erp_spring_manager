package api.astro.whats_orders_manager.models.dto.whatsapp;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO para la respuesta al enviar un mensaje por WhatsApp
 * Estructura según respuesta de Meta WhatsApp Business API
 * 
 * @author EmaSleal
 * @version 1.0
 * @since Sprint 3 - Fase 1.2
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnviarMensajeResponse {
    
    @JsonProperty("messaging_product")
    private String messagingProduct; // "whatsapp"
    
    @JsonProperty("contacts")
    private List<Contact> contacts;
    
    @JsonProperty("messages")
    private List<Message> messages;
    
    /**
     * Información del contacto al que se envió el mensaje
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Contact {
        @JsonProperty("input")
        private String input; // Número al que se intentó enviar
        
        @JsonProperty("wa_id")
        private String waId; // WhatsApp ID del contacto
    }
    
    /**
     * Información del mensaje enviado
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Message {
        @JsonProperty("id")
        private String id; // ID del mensaje (wamid.xxx)
        
        @JsonProperty("message_status")
        private String messageStatus; // Estado inicial: "accepted", "sent"
    }
    
    /**
     * Verifica si el mensaje fue aceptado exitosamente
     */
    public boolean isExitoso() {
        return messages != null && !messages.isEmpty() && messages.get(0).getId() != null;
    }
    
    /**
     * Obtiene el ID del primer mensaje enviado
     */
    public String getMessageId() {
        if (messages != null && !messages.isEmpty()) {
            return messages.get(0).getId();
        }
        return null;
    }
    
    /**
     * Obtiene el WhatsApp ID del destinatario
     */
    public String getWaId() {
        if (contacts != null && !contacts.isEmpty()) {
            return contacts.get(0).getWaId();
        }
        return null;
    }
}
