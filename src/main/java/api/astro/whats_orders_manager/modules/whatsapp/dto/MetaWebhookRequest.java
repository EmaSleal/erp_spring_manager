package api.astro.whats_orders_manager.modules.whatsapp.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * DTO para recibir webhooks de Meta WhatsApp Business API
 * Estructura completa según documentación oficial de Meta
 * 
 * @author EmaSleal
 * @version 1.0
 * @since Sprint 3 - Fase 1.2
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetaWebhookRequest {
    
    @NotNull
    private String object; // "whatsapp_business_account"
    
    @NotNull
    private List<Entry> entry;
    
    /**
     * Entrada del webhook (puede haber múltiples)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Entry {
        private String id; // WhatsApp Business Account ID
        private List<Change> changes;
    }
    
    /**
     * Cambio detectado (mensaje recibido, estado actualizado, etc.)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Change {
        private Value value;
        private String field; // "messages" o "message_status"
    }
    
    /**
     * Valor del cambio (contiene los datos del mensaje o estado)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Value {
        @JsonProperty("messaging_product")
        private String messagingProduct; // "whatsapp"
        
        private Metadata metadata;
        private List<Contact> contacts;
        private List<Message> messages;
        private List<Status> statuses;
        private List<Error> errors;
    }
    
    /**
     * Metadatos del webhook
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metadata {
        @JsonProperty("display_phone_number")
        private String displayPhoneNumber;
        
        @JsonProperty("phone_number_id")
        private String phoneNumberId;
    }
    
    /**
     * Información del contacto
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Contact {
        private Profile profile;
        @JsonProperty("wa_id")
        private String waId; // WhatsApp ID (teléfono)
    }
    
    /**
     * Perfil del contacto
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Profile {
        private String name;
    }
    
    /**
     * Mensaje recibido
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String from; // Número del remitente
        private String id; // ID del mensaje (wamid.xxx)
        private String timestamp;
        private Text text;
        private String type; // "text", "image", "document", etc.
        private Context context; // Si es respuesta a otro mensaje
    }
    
    /**
     * Contenido de texto del mensaje
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Text {
        private String body; // Contenido del mensaje
    }
    
    /**
     * Contexto del mensaje (respuesta a otro mensaje)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Context {
        private String from;
        private String id;
    }
    
    /**
     * Estado del mensaje (entregado, leído, etc.)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Status {
        private String id; // ID del mensaje
        private String status; // "sent", "delivered", "read", "failed"
        private String timestamp;
        @JsonProperty("recipient_id")
        private String recipientId;
        private Conversation conversation;
        private Pricing pricing;
        private List<Error> errors;
    }
    
    /**
     * Información de la conversación
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Conversation {
        private String id;
        @JsonProperty("expiration_timestamp")
        private String expirationTimestamp;
        private Origin origin;
    }
    
    /**
     * Origen de la conversación
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Origin {
        private String type; // "user_initiated", "business_initiated"
    }
    
    /**
     * Información de pricing
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pricing {
        private boolean billable;
        @JsonProperty("pricing_model")
        private String pricingModel;
        private String category;
    }
    
    /**
     * Error en el webhook
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Error {
        private Integer code;
        private String title;
        private String message;
        @JsonProperty("error_data")
        private ErrorData errorData;
    }
    
    /**
     * Datos del error
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorData {
        private String details;
    }
}
