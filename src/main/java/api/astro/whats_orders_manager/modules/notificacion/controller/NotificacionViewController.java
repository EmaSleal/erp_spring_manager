package api.astro.whats_orders_manager.modules.notificacion.controller;

import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioRepository;
import api.astro.whats_orders_manager.modules.notificacion.service.NotificacionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

/**
 * ============================================================================
 * NOTIFICACIÓN VIEW CONTROLLER
 * ERP Orders Manager - Sprint 4 Fase 3.7
 * ============================================================================
 * Controller para las vistas web de notificaciones (no API REST).
 * 
 * Endpoints:
 * - GET /notificaciones - Página de lista de notificaciones
 * - GET /notificaciones/preferencias - Configuración de preferencias
 * 
 * Las operaciones CRUD se manejan en NotificacionRestController.
 * 
 * @author EmaSleal
 * @since Sprint 4 - Fase 3.7
 * ============================================================================
 */
@Slf4j
@Controller
@RequestMapping("/notificaciones")
public class NotificacionViewController {

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Página principal de notificaciones
     * Muestra la lista completa de notificaciones con paginación y filtros
     * 
     * @param model Modelo para pasar datos a la vista
     * @param authentication Información del usuario autenticado
     * @return Vista de lista de notificaciones
     */
    @GetMapping
    public String listaNotificaciones(Model model, Authentication authentication) {
        // Obtener ID del usuario autenticado
        Integer idUsuario = obtenerIdUsuario(authentication);
        
        if (idUsuario != null) {
            // Obtener contador de no leídas
            long contadorNoLeidas = notificacionService.countNoLeidasByUsuarioId(idUsuario);
            model.addAttribute("contadorNoLeidas", contadorNoLeidas);
        } else {
            model.addAttribute("contadorNoLeidas", 0L);
        }

        return "modules/notificacion/lista";
    }

    /**
     * Página de preferencias de notificaciones
     * Permite configurar qué canales usar para cada tipo de notificación
     *
     * @return Vista de preferencias
     */
    @GetMapping("/preferencias")
    public String preferenciasNotificaciones() {
        return "modules/notificacion/preferencias";
    }

    /**
     * Obtiene el ID del usuario autenticado
     * 
     * @param authentication Información del usuario
     * @return ID del usuario o null si no se puede obtener
     */
    private Integer obtenerIdUsuario(Authentication authentication) {
        if (authentication == null) {
            return null;
        }

        try {
            String username = authentication.getName();
            
            // Buscar usuario por nombre o teléfono
            Optional<Usuario> usuarioOpt = usuarioRepository.findByNombre(username);
            
            if (!usuarioOpt.isPresent()) {
                usuarioOpt = usuarioRepository.findByEmail(username);
            }
            
            return usuarioOpt.map(Usuario::getIdUsuario).orElse(null);
            
        } catch (Exception e) {
            log.error("Error obteniendo ID de usuario: {}", e.getMessage());
            return null;
        }
    }
}
