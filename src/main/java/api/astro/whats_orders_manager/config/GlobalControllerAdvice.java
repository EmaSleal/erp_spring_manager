package api.astro.whats_orders_manager.config;

import api.astro.whats_orders_manager.models.Usuario;
import api.astro.whats_orders_manager.services.UsuarioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpSession;

/**
 * ============================================================================
 * GLOBAL CONTROLLER ADVICE
 * WhatsApp Orders Manager
 * ============================================================================
 * Clase global que agrega automáticamente datos del usuario al modelo
 * en todas las vistas del sistema.
 * 
 * Datos agregados:
 * - userName: Nombre completo del usuario
 * - userRole: Rol del usuario (ADMIN, USER, VENDEDOR, VISUALIZADOR)
 * - userInitials: Iniciales del usuario (para avatar fallback)
 * - userAvatar: URL del avatar del usuario (si existe)
 * - usuarioActual: Objeto Usuario completo
 * 
 * Esto evita tener que agregar estos datos manualmente en cada controlador.
 * ============================================================================
 */
@Slf4j
@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private UsuarioService usuarioService;

    /**
     * Agrega automáticamente los datos del usuario actual al modelo
     * en todas las vistas del sistema.
     * 
     * @param model El modelo de Spring MVC
     * @param session La sesión HTTP actual
     * @param authentication El objeto Authentication de Spring Security
     */
    @ModelAttribute
    public void addGlobalAttributes(Model model, HttpSession session, Authentication authentication) {
        try {
            // Si el usuario está autenticado
            if (authentication != null && authentication.isAuthenticated() 
                && authentication.getPrincipal() instanceof UserDetails) {
                
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                String username = userDetails.getUsername();
                
                log.debug("Agregando datos globales del usuario: {}", username);
                
                // Buscar el usuario completo
                Usuario usuario = usuarioService.findByTelefono(username).orElse(null);
                
                if (usuario != null) {
                    // Nombre del usuario
                    String userName = usuario.getNombre();
                    model.addAttribute("userName", userName);
                    
                    // Rol del usuario
                    String userRole = usuario.getRol() != null ? usuario.getRol() : "USER";
                    model.addAttribute("userRole", userRole);
                    
                    // Iniciales del usuario (para avatar fallback)
                    String userInitials = obtenerIniciales(userName);
                    model.addAttribute("userInitials", userInitials);
                    
                    // Avatar del usuario (si existe)
                    String userAvatar = usuario.getAvatar();
                    model.addAttribute("userAvatar", userAvatar != null ? userAvatar : "");
                    
                    // Usuario completo
                    model.addAttribute("usuarioActual", usuario);
                    
                    // También agregar a la sesión para acceso rápido
                    session.setAttribute("userName", userName);
                    session.setAttribute("userRole", userRole);
                    session.setAttribute("userInitials", userInitials);
                    session.setAttribute("userAvatar", userAvatar != null ? userAvatar : "");
                    
                    log.debug("Datos globales agregados - Usuario: {}, Rol: {}, Iniciales: {}", 
                             userName, userRole, userInitials);
                } else {
                    log.warn("No se encontró el usuario con username: {}", username);
                    agregarDatosPorDefecto(model);
                }
            } else {
                log.debug("Usuario no autenticado o principal no es UserDetails");
                agregarDatosPorDefecto(model);
            }
        } catch (Exception e) {
            log.error("Error al agregar datos globales del usuario al modelo: {}", e.getMessage(), e);
            agregarDatosPorDefecto(model);
        }
    }
    
    /**
     * Obtiene las iniciales del nombre del usuario.
     * Ejemplo: "Juan Pérez" -> "JP"
     * 
     * @param nombre Nombre completo del usuario
     * @return Iniciales del usuario (máximo 2 caracteres)
     */
    private String obtenerIniciales(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return "U";
        }
        
        String[] partes = nombre.trim().split("\\s+");
        
        if (partes.length == 1) {
            // Solo un nombre: usar primera letra
            return partes[0].substring(0, 1).toUpperCase();
        } else {
            // Múltiples nombres: usar primera letra de los dos primeros
            String iniciales = partes[0].substring(0, 1).toUpperCase() + 
                             partes[1].substring(0, 1).toUpperCase();
            return iniciales;
        }
    }
    
    /**
     * Agrega valores por defecto cuando no se puede obtener el usuario.
     * 
     * @param model El modelo de Spring MVC
     */
    private void agregarDatosPorDefecto(Model model) {
        model.addAttribute("userName", "Usuario");
        model.addAttribute("userRole", "USER");
        model.addAttribute("userInitials", "U");
        model.addAttribute("userAvatar", "");
    }
}
