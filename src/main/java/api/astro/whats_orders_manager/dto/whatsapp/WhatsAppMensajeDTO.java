package api.astro.whats_orders_manager.dto.whatsapp;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * DTO para transferir datos de mensajes WhatsApp entre capas de la aplicación
 * Simplificado para uso interno (no relacionado con Meta API)
 * 
 * @author EmaSleal
 * @version 1.0
 * @since Sprint 3 - Fase 1.2
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsAppMensajeDTO {
    
    private Long idMensaje;
    private String telefono;
    private String mensaje;
    private String tipo; // ENVIADO, RECIBIDO
    private String estado; // PENDIENTE, ENVIADO, ENTREGADO, LEIDO, FALLIDO
    private Integer idUsuario;
    private String nombreUsuario;
    private String idMensajeWhatsapp; // wamid.xxx
    private String nombrePlantilla; // Nombre de la plantilla si se usó una
    private LocalDateTime fechaEnvio;
    private LocalDateTime fechaEntrega;
    private LocalDateTime fechaLectura;
    private String error;
    private String metadata;
    
    /**
     * Verifica si el mensaje fue exitoso
     */
    public boolean esExitoso() {
        return "ENTREGADO".equals(estado) || "LEIDO".equals(estado);
    }
    
    /**
     * Verifica si es un mensaje enviado por el sistema
     */
    public boolean esEnviado() {
        return "ENVIADO".equals(tipo);
    }
    
    /**
     * Verifica si es un mensaje recibido de un cliente
     */
    public boolean esRecibido() {
        return "RECIBIDO".equals(tipo);
    }
    
    /**
     * Verifica si el mensaje falló
     */
    public boolean esFallido() {
        return "FALLIDO".equals(estado);
    }
    
    /**
     * Verifica si el mensaje está pendiente
     */
    public boolean esPendiente() {
        return "PENDIENTE".equals(estado);
    }
}
