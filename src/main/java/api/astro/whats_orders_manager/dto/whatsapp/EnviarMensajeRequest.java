package api.astro.whats_orders_manager.dto.whatsapp;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/**
 * DTO para enviar mensajes a través de Meta WhatsApp Business API
 * 
 * @author EmaSleal
 * @version 1.0
 * @since Sprint 3 - Fase 1.2
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnviarMensajeRequest {
    
    @JsonProperty("messaging_product")
    @Builder.Default
    private String messagingProduct = "whatsapp";
    
    @JsonProperty("recipient_type")
    @Builder.Default
    private String recipientType = "individual";
    
    @NotBlank(message = "El número de teléfono es obligatorio")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Formato de teléfono inválido")
    @JsonProperty("to")
    private String to; // Número en formato internacional: +525512345678
    
    @NotBlank(message = "El tipo de mensaje es obligatorio")
    @JsonProperty("type")
    private String type; // "text", "template", "document", etc.
    
    // Para mensajes de texto simple
    @JsonProperty("text")
    private TextContent text;
    
    // Para mensajes con plantilla
    @JsonProperty("template")
    private TemplateContent template;
    
    // Para envío de documentos
    @JsonProperty("document")
    private DocumentContent document;
    
    /**
     * Contenido de mensaje de texto
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TextContent {
        @JsonProperty("preview_url")
        private Boolean previewUrl;
        
        @JsonProperty("body")
        private String body;
    }
    
    /**
     * Contenido de plantilla
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TemplateContent {
        @JsonProperty("name")
        private String name; // Nombre de la plantilla
        
        @JsonProperty("language")
        private Language language;
        
        @JsonProperty("components")
        private List<Component> components;
    }
    
    /**
     * Idioma de la plantilla
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Language {
        @JsonProperty("code")
        private String code; // "es_MX", "en_US", etc.
    }
    
    /**
     * Componente de la plantilla (header, body, button)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Component {
        @JsonProperty("type")
        private String type; // "header", "body", "button"
        
        @JsonProperty("parameters")
        private List<Parameter> parameters;
    }
    
    /**
     * Parámetro del componente
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Parameter {
        @JsonProperty("type")
        private String type; // "text", "currency", "date_time", "document"
        
        @JsonProperty("text")
        private String text;
        
        @JsonProperty("document")
        private DocumentParameter document;
    }
    
    /**
     * Parámetro de tipo documento
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DocumentParameter {
        @JsonProperty("link")
        private String link; // URL del documento
        
        @JsonProperty("filename")
        private String filename;
    }
    
    /**
     * Contenido de documento
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DocumentContent {
        @JsonProperty("link")
        private String link; // URL del documento
        
        @JsonProperty("caption")
        private String caption;
        
        @JsonProperty("filename")
        private String filename;
    }
}
