package api.astro.whats_orders_manager.controllers;

import api.astro.whats_orders_manager.models.dto.NotificacionDTO;
import api.astro.whats_orders_manager.services.NotificacionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * ============================================================================
 * NOTIFICACIÓN WEBSOCKET CONTROLLER
 * WhatsApp Orders Manager - Sprint 4 Fase 3.5
 * ============================================================================
 * Controller WebSocket para envío de notificaciones en tiempo real.
 * 
 * Endpoints WebSocket:
 * - /app/notificaciones/enviar: Enviar notificación (admin)
 * - /app/notificaciones/marcar-leida: Marcar como leída
 * - /topic/notificaciones: Broadcast a todos los usuarios
 * - /queue/notificaciones: Mensaje privado a usuario específico
 * 
 * Flujo:
 * 1. Cliente se conecta a /ws-notificaciones
 * 2. Cliente se suscribe a /user/queue/notificaciones
 * 3. Servidor envía notificaciones por SimpMessagingTemplate
 * 4. Cliente recibe y muestra notificación en UI
 * 
 * @author EmaSleal
 * @since Sprint 4 - Fase 3.5
 * ============================================================================
 */
@Slf4j
@Controller
public class NotificacionWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private NotificacionService notificacionService;

    /**
     * Envía una notificación broadcast a todos los usuarios conectados
     * 
     * Endpoint: /app/notificaciones/broadcast
     * Destino: /topic/notificaciones
     * 
     * @param notificacion DTO de la notificación
     * @return NotificacionDTO procesada
     */
    @MessageMapping("/notificaciones/broadcast")
    @SendTo("/topic/notificaciones")
    public NotificacionDTO enviarBroadcast(@Payload NotificacionDTO notificacion) {
        log.info("📡 Broadcasting notificación: {}", notificacion.getTitulo());
        return notificacion;
    }

    /**
     * Marca una notificación como leída
     * 
     * Endpoint: /app/notificaciones/marcar-leida
     * 
     * @param idNotificacion ID de la notificación
     * @param authentication Usuario autenticado
     */
    @MessageMapping("/notificaciones/marcar-leida")
    @SendToUser("/queue/notificaciones/leida")
    public void marcarComoLeida(@Payload Integer idNotificacion, Authentication authentication) {
        log.info("✅ Marcando notificación {} como leída para usuario {}", 
            idNotificacion, authentication.getName());
        
        boolean actualizada = notificacionService.marcarComoLeida(idNotificacion);
        
        if (actualizada) {
            log.debug("Notificación {} marcada como leída", idNotificacion);
        }
    }

    /**
     * Obtiene notificaciones no leídas del usuario
     * 
     * Endpoint: /app/notificaciones/no-leidas
     * 
     * @param authentication Usuario autenticado
     * @return Lista de notificaciones no leídas
     */
    @MessageMapping("/notificaciones/no-leidas")
    @SendToUser("/queue/notificaciones/lista")
    public List<NotificacionDTO> obtenerNoLeidas(Authentication authentication) {
        log.debug("📬 Obteniendo notificaciones no leídas para {}", authentication.getName());
        
        // Extraer ID del usuario del nombre (asumir que es el ID)
        try {
            Integer idUsuario = Integer.parseInt(authentication.getName());
            var notificaciones = notificacionService.findNoLeidasByUsuarioId(idUsuario);
            return notificacionService.convertirADTOs(notificaciones);
        } catch (NumberFormatException e) {
            log.error("Error al parsear ID de usuario: {}", authentication.getName());
            return List.of();
        }
    }

    /**
     * Envía una notificación a un usuario específico por su ID
     * 
     * Método helper para uso interno desde services
     * 
     * @param idUsuario ID del usuario destinatario
     * @param notificacion DTO de la notificación
     */
    public void enviarNotificacionAUsuario(Integer idUsuario, NotificacionDTO notificacion) {
        log.info("📨 Enviando notificación WebSocket a usuario {}: {}", 
            idUsuario, notificacion.getTitulo());
        
        messagingTemplate.convertAndSendToUser(
            idUsuario.toString(),
            "/queue/notificaciones",
            notificacion
        );
        
        log.debug("✅ Notificación WebSocket enviada a usuario {}", idUsuario);
    }

    /**
     * Envía notificación broadcast a todos los usuarios conectados
     * 
     * Método helper para uso interno desde services
     * 
     * @param notificacion DTO de la notificación
     */
    public void enviarNotificacionBroadcast(NotificacionDTO notificacion) {
        log.info("📡 Enviando notificación broadcast: {}", notificacion.getTitulo());
        
        messagingTemplate.convertAndSend(
            "/topic/notificaciones",
            notificacion
        );
        
        log.debug("✅ Notificación broadcast enviada");
    }

    /**
     * Notifica actualización de contador de no leídas a usuario específico
     * 
     * @param idUsuario ID del usuario
     * @param cantidadNoLeidas Cantidad de notificaciones no leídas
     */
    public void notificarContadorNoLeidas(Integer idUsuario, Long cantidadNoLeidas) {
        log.debug("🔢 Actualizando contador de no leídas para usuario {}: {}", 
            idUsuario, cantidadNoLeidas);
        
        messagingTemplate.convertAndSendToUser(
            idUsuario.toString(),
            "/queue/notificaciones/contador",
            cantidadNoLeidas
        );
    }
}
