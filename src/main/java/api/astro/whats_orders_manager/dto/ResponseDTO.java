package api.astro.whats_orders_manager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO genérico para respuestas de API REST
 * Proporciona una estructura consistente para todas las respuestas
 * 
 * @version 1.0
 * @since 26/10/2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDTO {
    
    /**
     * Indica si la operación fue exitosa
     */
    private boolean success;
    
    /**
     * Mensaje descriptivo del resultado
     */
    private String message;
    
    /**
     * Datos adicionales de la respuesta (opcional)
     */
    private Object data;
    
    /**
     * Constructor para respuestas sin datos adicionales
     */
    public ResponseDTO(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.data = null;
    }
    
    /**
     * Crea una respuesta de éxito
     */
    public static ResponseDTO success(String message) {
        return new ResponseDTO(true, message);
    }
    
    /**
     * Crea una respuesta de éxito con datos
     */
    public static ResponseDTO success(String message, Object data) {
        return new ResponseDTO(true, message, data);
    }
    
    /**
     * Crea una respuesta de error
     */
    public static ResponseDTO error(String message) {
        return new ResponseDTO(false, message);
    }
    
    /**
     * Crea una respuesta de error con datos
     */
    public static ResponseDTO error(String message, Object data) {
        return new ResponseDTO(false, message, data);
    }
    
    /**
     * Convierte el DTO a un Map para compatibilidad con código legacy
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("success", success);
        map.put("message", message);
        if (data != null) {
            map.put("data", data);
        }
        return map;
    }
}
