package api.astro.whats_orders_manager.modules.notificacion.dto;

import api.astro.whats_orders_manager.modules.notificacion.enums.CanalNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.enums.TipoNotificacion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * ============================================================================
 * NOTIFICACIÓN DTO
 * ERP Orders Manager - Sprint 4 Fase 3
 * ============================================================================
 * Data Transfer Object para transferir datos de notificaciones
 * entre las capas de la aplicación.
 * 
 * Usado para:
 * - Respuestas de API REST
 * - Envío de datos al frontend
 * - Recepción de datos desde el frontend
 * - WebSocket (notificaciones en tiempo real)
 * 
 * ============================================================================
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificacionDTO {

    // ==================== IDENTIFICACIÓN ====================
    
    private Integer idNotificacion;
    
    // ==================== TIPO Y CANAL ====================
    
    private TipoNotificacion tipo;
    private String tipoNombre;
    private String tipoDescripcion;
    private String tipoIcono;
    private String tipoColor;
    
    private CanalNotificacion canal;
    private String canalNombre;
    private String canalIcono;
    
    // ==================== DESTINATARIO ====================
    
    private Integer idUsuario;
    private String nombreUsuario;
    private String emailDestinatario;
    private String telefonoDestinatario;
    
    // ==================== CONTENIDO ====================
    
    private String titulo;
    private String mensaje;
    private String urlAccion;
    private String textoBoton;
    
    // ==================== RELACIONES ====================
    
    private Integer idFacturaRelacionada;
    private String numeroFactura;
    
    private Integer idClienteRelacionado;
    private String nombreCliente;
    
    private Integer idProductoRelacionado;
    private String nombreProducto;
    
    private Integer entidadRelacionadaId;
    private String entidadRelacionadaTipo;
    
    // ==================== ESTADO ====================
    
    private Boolean enviada;
    private Boolean leida;
    private LocalDateTime fechaEnvio;
    private LocalDateTime fechaLectura;
    private String errorMensaje;
    private Integer intentosEnvio;
    
    // ==================== PLANTILLA ====================
    
    private Integer idPlantilla;
    private String nombrePlantilla;
    
    // ==================== AUDITORÍA ====================
    
    private Integer generadaPor;
    private String nombreGenerador;
    
    // ==================== MÉTODOS DE UTILIDAD ====================
    
    /**
     * Verifica si la notificación está pendiente de lectura
     */
    public boolean estaPendiente() {
        return Boolean.TRUE.equals(enviada) 
            && Boolean.FALSE.equals(leida) 
            && canal == CanalNotificacion.WEB;
    }

    /**
     * Verifica si el envío falló
     */
    public boolean tieneFallo() {
        return Boolean.FALSE.equals(enviada) && errorMensaje != null;
    }

    /**
     * Obtiene el tiempo transcurrido desde el envío
     * Retorna formato "hace X minutos", "hace X horas", etc.
     */
    public String getTiempoTranscurrido() {
        if (fechaEnvio == null) {
            return "";
        }

        Duration duracion = Duration.between(fechaEnvio, LocalDateTime.now());
        long segundos = duracion.getSeconds();
        long minutos = segundos / 60;
        long horas = minutos / 60;
        long dias = horas / 24;

        if (dias > 0) {
            return "hace " + dias + (dias == 1 ? " día" : " días");
        } else if (horas > 0) {
            return "hace " + horas + (horas == 1 ? " hora" : " horas");
        } else if (minutos > 0) {
            return "hace " + minutos + (minutos == 1 ? " minuto" : " minutos");
        } else {
            return "hace unos segundos";
        }
    }

    /**
     * Obtiene un resumen corto del mensaje (primeros 100 caracteres)
     */
    public String getMensajeResumido() {
        if (mensaje == null || mensaje.isEmpty()) {
            return "";
        }
        
        if (mensaje.length() <= 100) {
            return mensaje;
        }
        
        return mensaje.substring(0, 97) + "...";
    }

    @Override
    public String toString() {
        return "NotificacionDTO{" +
                "id=" + idNotificacion +
                ", tipo=" + tipo +
                ", canal=" + canal +
                ", titulo='" + titulo + '\'' +
                ", enviada=" + enviada +
                ", leida=" + leida +
                '}';
    }
}
