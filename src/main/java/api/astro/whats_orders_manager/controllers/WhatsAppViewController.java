package api.astro.whats_orders_manager.controllers;

import api.astro.whats_orders_manager.dto.whatsapp.PlantillaWhatsAppDTO;
import api.astro.whats_orders_manager.dto.whatsapp.WhatsAppMensajeDTO;
import api.astro.whats_orders_manager.models.MensajeWhatsApp.EstadoMensaje;
import api.astro.whats_orders_manager.models.MensajeWhatsApp.TipoMensaje;
import api.astro.whats_orders_manager.models.PlantillaWhatsApp;
import api.astro.whats_orders_manager.models.PlantillaWhatsApp.EstadoMeta;
import api.astro.whats_orders_manager.models.PlantillaWhatsApp.CategoriaPlantilla;
import api.astro.whats_orders_manager.services.MensajeWhatsAppService;
import api.astro.whats_orders_manager.services.PlantillaWhatsAppService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Controlador de vistas para WhatsApp
 * Renderiza las páginas HTML con Thymeleaf
 * 
 * @author EmaSleal
 * @version 1.0
 * @since Sprint 3 - Fase 1.6
 */
@Controller
@RequestMapping("/whatsapp")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
@Slf4j
public class WhatsAppViewController {
    
    private final MensajeWhatsAppService mensajeService;
    private final PlantillaWhatsAppService plantillaService;
    
    public WhatsAppViewController(
            MensajeWhatsAppService mensajeService,
            PlantillaWhatsAppService plantillaService) {
        this.mensajeService = mensajeService;
        this.plantillaService = plantillaService;
    }
    
    /**
     * GET /whatsapp/mensajes
     * Vista principal de mensajes WhatsApp
     */
    @GetMapping("/mensajes")
    public String mensajes(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String tipo,
            Model model) {
        
        log.info("Accediendo a vista de mensajes WhatsApp");
        
        try {
            // Obtener estadísticas generales
            MensajeWhatsAppService.EstadisticasMensajes stats = mensajeService.obtenerEstadisticas();
            model.addAttribute("estadisticas", stats);
            
            // Obtener mensajes según filtros
            List<WhatsAppMensajeDTO> mensajes;
            if (estado != null && !estado.isEmpty()) {
                mensajes = mensajeService.obtenerPorEstado(
                    EstadoMensaje.valueOf(estado.toUpperCase())
                );
            } else if (tipo != null && !tipo.isEmpty()) {
                mensajes = mensajeService.obtenerPorTipo(
                    TipoMensaje.valueOf(tipo.toUpperCase())
                );
            } else {
                // Por defecto, obtener todos los mensajes ordenados por fecha
                mensajes = mensajeService.obtenerTodos();
            }
            
            model.addAttribute("mensajes", mensajes);
            model.addAttribute("estadoFiltro", estado);
            model.addAttribute("tipoFiltro", tipo);
            
            return "whatsapp/mensajes";
            
        } catch (Exception e) {
            log.error("Error al cargar vista de mensajes", e);
            model.addAttribute("error", "Error al cargar mensajes: " + e.getMessage());
            return "whatsapp/mensajes";
        }
    }
    
    /**
     * GET /whatsapp/plantillas
     * Vista de gestión de plantillas
     */
    @GetMapping("/plantillas")
    public String plantillas(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String categoria,
            Model model) {
        
        log.info("Accediendo a vista de plantillas WhatsApp");
        
        try {
            // Obtener todas las plantillas para mostrar en la tabla
            List<PlantillaWhatsAppDTO> todasPlantillas = plantillaService.obtenerTodas();
            
            // Aplicar filtros si existen
            List<PlantillaWhatsAppDTO> plantillasFiltradas = todasPlantillas;
            if (estado != null && !estado.isEmpty()) {
                final String estadoFinal = estado.toUpperCase();
                plantillasFiltradas = todasPlantillas.stream()
                    .filter(p -> estadoFinal.equals(p.getEstadoMeta()))
                    .collect(java.util.stream.Collectors.toList());
            }
            if (categoria != null && !categoria.isEmpty()) {
                final String catFinal = categoria.toUpperCase();
                plantillasFiltradas = plantillasFiltradas.stream()
                    .filter(p -> catFinal.equals(p.getCategoria()))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            // Calcular estadísticas
            long aprobadas = todasPlantillas.stream()
                .filter(p -> "APPROVED".equals(p.getEstadoMeta()))
                .count();
            long pendientes = todasPlantillas.stream()
                .filter(p -> "PENDING".equals(p.getEstadoMeta()))
                .count();
            long activas = todasPlantillas.stream()
                .filter(PlantillaWhatsAppDTO::getActivo)
                .count();
            
            model.addAttribute("plantillas", plantillasFiltradas);
            model.addAttribute("aprobadas", aprobadas);
            model.addAttribute("pendientes", pendientes);
            model.addAttribute("activas", activas);
            model.addAttribute("estadoFiltro", estado);
            model.addAttribute("categoriaFiltro", categoria);
            
            return "whatsapp/plantillas";
            
        } catch (Exception e) {
            log.error("Error al cargar vista de plantillas", e);
            model.addAttribute("error", "Error al cargar plantillas: " + e.getMessage());
            return "whatsapp/plantillas";
        }
    }
    
    /**
     * GET /whatsapp/plantillas/{id}
     * Vista de detalle de una plantilla
     */
    @GetMapping("/plantillas/{id}")
    public String plantillaDetalle(@PathVariable Integer id, Model model) {
        log.info("Accediendo a detalle de plantilla {}", id);
        
        try {
            PlantillaWhatsApp plantilla = plantillaService.obtenerPorId(id)
                    .orElseThrow(() -> new RuntimeException("Plantilla no encontrada"));
            model.addAttribute("plantilla", plantilla);
            return "whatsapp/plantilla-detalle";
            
        } catch (Exception e) {
            log.error("Error al cargar detalle de plantilla", e);
            model.addAttribute("error", "Plantilla no encontrada");
            return "redirect:/whatsapp/plantillas";
        }
    }
    
    /**
     * GET /whatsapp/historial/{idUsuario}
     * Vista de historial de mensajes de un usuario
     */
    @GetMapping("/historial/{idUsuario}")
    public String historialUsuario(@PathVariable Integer idUsuario, Model model) {
        log.info("Accediendo a historial de usuario {}", idUsuario);
        
        try {
            List<WhatsAppMensajeDTO> mensajes = mensajeService.obtenerHistorialUsuario(idUsuario);
            MensajeWhatsAppService.EstadisticasMensajes stats = mensajeService.obtenerEstadisticasUsuario(idUsuario);
            
            model.addAttribute("mensajes", mensajes);
            model.addAttribute("estadisticas", stats);
            model.addAttribute("idUsuario", idUsuario);
            
            return "whatsapp/historial";
            
        } catch (Exception e) {
            log.error("Error al cargar historial de usuario", e);
            model.addAttribute("error", "Error al cargar historial: " + e.getMessage());
            return "whatsapp/historial";
        }
    }
}
