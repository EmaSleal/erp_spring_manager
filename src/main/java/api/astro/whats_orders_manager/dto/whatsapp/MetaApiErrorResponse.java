package api.astro.whats_orders_manager.dto.whatsapp;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO para manejar respuestas de error de Meta WhatsApp Business API
 * 
 * @author EmaSleal
 * @version 1.0
 * @since Sprint 3 - Fase 1.2
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetaApiErrorResponse {
    
    @JsonProperty("error")
    private ErrorDetail error;
    
    /**
     * Detalle del error
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDetail {
        @JsonProperty("message")
        private String message;
        
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("code")
        private Integer code;
        
        @JsonProperty("error_data")
        private ErrorData errorData;
        
        @JsonProperty("fbtrace_id")
        private String fbtraceId;
    }
    
    /**
     * Datos adicionales del error
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorData {
        @JsonProperty("details")
        private String details;
    }
    
    /**
     * Verifica si el error es de límite de tasa (rate limit)
     */
    public boolean isRateLimitError() {
        return error != null && error.getCode() != null && 
               (error.getCode() == 80007 || error.getCode() == 130429);
    }
    
    /**
     * Verifica si el error es por número inválido
     */
    public boolean isInvalidPhoneError() {
        return error != null && error.getCode() != null && error.getCode() == 100;
    }
    
    /**
     * Verifica si el error es por plantilla inválida
     */
    public boolean isInvalidTemplateError() {
        return error != null && error.getCode() != null && error.getCode() == 132000;
    }
    
    /**
     * Obtiene el mensaje de error
     */
    public String getErrorMessage() {
        if (error != null) {
            return error.getMessage();
        }
        return "Error desconocido";
    }
}
