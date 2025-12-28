package api.astro.whats_orders_manager.modules.whatsapp.controller;


import api.astro.whats_orders_manager.modules.whatsapp.model.MensajeWhatsApp.EstadoMensaje;
import api.astro.whats_orders_manager.modules.whatsapp.model.MensajeWhatsApp.TipoMensaje;
import api.astro.whats_orders_manager.modules.whatsapp.model.PlantillaWhatsApp;
import api.astro.whats_orders_manager.modules.whatsapp.model.PlantillaWhatsApp.EstadoMeta;
import api.astro.whats_orders_manager.modules.whatsapp.dto.PlantillaWhatsAppDTO;
import api.astro.whats_orders_manager.modules.whatsapp.dto.WhatsAppMensajeDTO;
import api.astro.whats_orders_manager.modules.whatsapp.model.PlantillaWhatsApp.CategoriaPlantilla;
import api.astro.whats_orders_manager.modules.whatsapp.service.MensajeWhatsAppService;
import api.astro.whats_orders_manager.modules.whatsapp.service.PlantillaWhatsAppService;
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
     * Vista principal de mensajes WhatsApp - Muestra conversaciones agrupadas
     */
    @GetMapping("/mensajes")
    public String mensajes(Model model) {
        
        log.info("Accediendo a vista de conversaciones WhatsApp");
        
        try {
            // Obtener estadísticas generales
            MensajeWhatsAppService.EstadisticasMensajes stats = mensajeService.obtenerEstadisticas();
            if (stats == null) {
                stats = new MensajeWhatsAppService.EstadisticasMensajes(0L, 0L, 0L, 0L, 0L);
            }
            model.addAttribute("estadisticas", stats);
            
            // Obtener conversaciones agrupadas por teléfono
            List<MensajeWhatsAppService.Conversacion> conversaciones = mensajeService.obtenerConversaciones();
            
            if (conversaciones == null) {
                conversaciones = new java.util.ArrayList<>();
            }
            
            log.info("Se encontraron {} conversaciones", conversaciones.size());
            model.addAttribute("conversaciones", conversaciones);
            
            return "whatsapp/mensajes";
            
        } catch (Exception e) {
            log.error("Error al cargar vista de conversaciones", e);
            model.addAttribute("error", "Error al cargar conversaciones: " + e.getMessage());
            model.addAttribute("conversaciones", new java.util.ArrayList<>());
            model.addAttribute("estadisticas", new MensajeWhatsAppService.EstadisticasMensajes(0L, 0L, 0L, 0L, 0L));
            return "whatsapp/mensajes";
        }
    }
    
    /**
     * GET /whatsapp/conversacion/{telefono}
     * Vista de detalle de una conversación específica
     */
    @GetMapping("/conversacion/{telefono}")
    public String conversacionDetalle(@PathVariable String telefono, Model model) {
        log.info("Accediendo a conversación con teléfono: {}", telefono);
        
        try {
            // Obtener todos los mensajes del teléfono
            List<WhatsAppMensajeDTO> mensajes = mensajeService.obtenerMensajesRecientes(telefono);
            
            if (mensajes == null || mensajes.isEmpty()) {
                mensajes = new java.util.ArrayList<>();
            }
            
            model.addAttribute("telefono", telefono);
            model.addAttribute("mensajes", mensajes);
            model.addAttribute("totalMensajes", mensajes.size());
            
            // Obtener nombre de usuario si existe
            String nombreUsuario = mensajes.isEmpty() ? telefono : 
                (mensajes.get(0).getNombreUsuario() != null ? mensajes.get(0).getNombreUsuario() : telefono);
            model.addAttribute("nombreUsuario", nombreUsuario);
            
            // Calcular estadísticas de la conversación
            long mensajesEnviados = mensajes.stream()
                .filter(m -> "ENVIADO".equals(m.getTipo()))
                .count();
            long mensajesRecibidos = mensajes.stream()
                .filter(m -> "RECIBIDO".equals(m.getTipo()))
                .count();
            
            model.addAttribute("mensajesEnviados", mensajesEnviados);
            model.addAttribute("mensajesRecibidos", mensajesRecibidos);
            
            // Fecha del primer mensaje
            if (!mensajes.isEmpty()) {
                model.addAttribute("primerMensaje", mensajes.get(mensajes.size() - 1).getFechaEnvio());
            }
            
            return "whatsapp/conversacion-detalle";
            
        } catch (Exception e) {
            log.error("Error al cargar conversación", e);
            model.addAttribute("error", "Error al cargar conversación: " + e.getMessage());
            return "redirect:/whatsapp/mensajes";
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
