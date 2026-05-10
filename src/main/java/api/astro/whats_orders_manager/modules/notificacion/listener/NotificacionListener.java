package api.astro.whats_orders_manager.modules.notificacion.listener;

import api.astro.whats_orders_manager.modules.notificacion.event.NotificacionEvent;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.notificacion.enums.CanalNotificacion;
import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioRepository;
import api.astro.whats_orders_manager.modules.notificacion.service.NotificacionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * ============================================================================
 * NOTIFICACIÓN LISTENER
 * ERP Orders Manager - Sprint 4 Fase 3.6
 * ============================================================================
 * Listener que captura eventos de notificación y los procesa.
 * 
 * Responsabilidades:
 * - Escuchar eventos NotificacionEvent
 * - Validar datos del evento
 * - Resolver usuario destinatario (si solo se pasa ID)
 * - Delegar envío a NotificacionService
 * - Procesar de forma asíncrona para no bloquear el hilo principal
 * 
 * Procesamiento:
 * - @Async: Ejecuta en thread separado
 * - @EventListener: Captura eventos publicados
 * - Validación de preferencias en NotificacionService
 * - Envío por canales configurados
 * 
 * @author EmaSleal
 * @since Sprint 4 - Fase 3.6
 * ============================================================================
 */
@Slf4j
@Component
public class NotificacionListener {

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Procesa eventos de notificación
     * 
     * @Async: Ejecuta en thread separado para no bloquear
     * @EventListener: Se activa cuando se publica NotificacionEvent
     */
    @Async
    @EventListener
    public void procesarNotificacion(NotificacionEvent event) {
        log.info("🎯 Evento de notificación recibido: {}", event);

        try {
            // Resolver usuario destinatario
            Usuario usuario = resolverUsuario(event);
            
            if (usuario == null && event.getIdUsuario() == null) {
                log.warn("⚠️ No se pudo resolver usuario destinatario, evento ignorado");
                return;
            }

            // Determinar canales de envío
            CanalNotificacion[] canales = determinarCanales(event);

            // Enviar notificación por cada canal
            for (CanalNotificacion canal : canales) {
                enviarPorCanal(event, usuario, canal);
            }

        } catch (Exception e) {
            log.error("❌ Error al procesar evento de notificación: {}", e.getMessage(), e);
        }
    }

    /**
     * Resuelve el usuario destinatario del evento
     */
    private Usuario resolverUsuario(NotificacionEvent event) {
        // Si ya tiene el objeto Usuario, usarlo
        if (event.getUsuario() != null) {
            return event.getUsuario();
        }

        // Si tiene ID de usuario, buscarlo
        if (event.getIdUsuario() != null) {
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(event.getIdUsuario());
            if (usuarioOpt.isPresent()) {
                return usuarioOpt.get();
            } else {
                log.warn("⚠️ Usuario ID {} no encontrado", event.getIdUsuario());
            }
        }

        return null;
    }

    /**
     * Determina los canales por los que se debe enviar la notificación
     */
    private CanalNotificacion[] determinarCanales(NotificacionEvent event) {
        // Si el evento especifica un canal, usar solo ese
        if (event.getCanal() != null) {
            return new CanalNotificacion[]{event.getCanal()};
        }

        // Si no especifica canal, intentar por todos los disponibles
        // El NotificacionService validará preferencias del usuario
        return CanalNotificacion.getCanalesDisponibles();
    }

    /**
     * Envía la notificación por un canal específico
     */
    private void enviarPorCanal(NotificacionEvent event, Usuario usuario, CanalNotificacion canal) {
        try {
            log.debug("📤 Enviando notificación por canal {}: {}", canal, event.getTitulo());

            // Si usa plantilla
            if (event.getIdPlantilla() != null && event.getVariablesPlantilla() != null) {
                notificacionService.enviarNotificacionConPlantilla(
                    usuario,
                    event.getTipo(),
                    canal,
                    event.getIdPlantilla(),
                    event.getVariablesPlantilla()
                );
            }
            // Si tiene URL de acción
            else if (event.getUrlAccion() != null) {
                notificacionService.enviarNotificacionConAccion(
                    usuario,
                    event.getTipo(),
                    canal,
                    event.getTitulo(),
                    event.getMensaje(),
                    event.getUrlAccion(),
                    event.getTextoBoton()
                );
            }
            // Notificación simple
            else {
                notificacionService.enviarNotificacion(
                    usuario,
                    event.getTipo(),
                    canal,
                    event.getTitulo(),
                    event.getMensaje()
                );
            }

            log.debug("✅ Notificación enviada por canal {}", canal);

        } catch (Exception e) {
            log.error("❌ Error al enviar notificación por canal {}: {}", 
                canal, e.getMessage());
        }
    }

    /**
     * Procesa eventos de notificación broadcast (a todos los usuarios)
     * 
     * Uso: Notificaciones del sistema que todos deben ver
     */
    @Async
    @EventListener(condition = "#event.usuario == null && #event.idUsuario == null")
    public void procesarNotificacionBroadcast(NotificacionEvent event) {
        log.info("📡 Evento de notificación broadcast recibido: {}", event);

        try {
            // Para broadcasts, enviar solo por canal WEB
            // Evitar spam por email/WhatsApp a todos los usuarios
            notificacionService.enviarNotificacion(
                null,  // Sin usuario específico
                event.getTipo(),
                CanalNotificacion.WEB,
                event.getTitulo(),
                event.getMensaje()
            );

            log.info("✅ Notificación broadcast procesada");

        } catch (Exception e) {
            log.error("❌ Error al procesar notificación broadcast: {}", e.getMessage(), e);
        }
    }
}
