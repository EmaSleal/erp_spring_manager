package api.astro.whats_orders_manager.controllers;


import api.astro.whats_orders_manager.models.MensajeWhatsApp;
import api.astro.whats_orders_manager.models.dto.WhatsAppMensajeDTO;
import api.astro.whats_orders_manager.services.MensajeWhatsAppService;
import api.astro.whats_orders_manager.services.WhatsAppService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestión de mensajes de WhatsApp
 * Endpoints protegidos por autenticación
 * 
 * @author EmaSleal
 * @version 1.0
 * @since Sprint 3 - Fase 1.4
 */
@RestController
@RequestMapping("/api/whatsapp/mensajes")
@Slf4j
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
public class WhatsAppMensajeController {
    
    private final WhatsAppService whatsAppService;
    private final MensajeWhatsAppService mensajeService;
    
    public WhatsAppMensajeController(
            WhatsAppService whatsAppService,
            MensajeWhatsAppService mensajeService) {
        this.whatsAppService = whatsAppService;
        this.mensajeService = mensajeService;
    }
    
    /**
     * Envía un mensaje de texto simple
     * 
     * POST /api/whatsapp/mensajes/enviar
     * Body: { "telefono": "+525512345678", "mensaje": "Hola", "idUsuario": 1 }
     */
    @PostMapping("/enviar")
    public ResponseEntity<?> enviarMensajeTexto(@RequestBody EnviarMensajeRequest request) {
        
        log.info("Solicitud de envío de mensaje a: {}", request.getTelefono());
        
        try {
            MensajeWhatsApp mensaje = whatsAppService.enviarMensajeTexto(
                    request.getTelefono(),
                    request.getMensaje(),
                    request.getIdUsuario()
            );
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Mensaje enviado exitosamente",
                    "idMensaje", mensaje.getIdMensaje(),
                    "estado", mensaje.getEstado().name()
            ));
            
        } catch (Exception e) {
            log.error("Error al enviar mensaje", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al enviar mensaje: " + e.getMessage()
                    ));
        }
    }
    
    /**
     * Envía un mensaje usando una plantilla
     * 
     * POST /api/whatsapp/mensajes/enviar-plantilla
     * Body: { "telefono": "+525512345678", "nombrePlantilla": "factura_nueva", 
     *         "parametros": ["Juan", "12345"], "idUsuario": 1 }
     */
    @PostMapping("/enviar-plantilla")
    public ResponseEntity<?> enviarMensajePlantilla(@RequestBody EnviarPlantillaRequest request) {
        
        log.info("Solicitud de envío de plantilla '{}' a: {}", 
                request.getNombrePlantilla(), request.getTelefono());
        
        try {
            MensajeWhatsApp mensaje = whatsAppService.enviarMensajePlantilla(
                    request.getTelefono(),
                    request.getNombrePlantilla(),
                    request.getParametros(),
                    request.getIdUsuario()
            );
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Plantilla enviada exitosamente",
                    "idMensaje", mensaje.getIdMensaje(),
                    "estado", mensaje.getEstado().name()
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        } catch (Exception e) {
            log.error("Error al enviar plantilla", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al enviar plantilla: " + e.getMessage()
                    ));
        }
    }
    
    /**
     * Envía un documento PDF
     * 
     * POST /api/whatsapp/mensajes/enviar-documento
     * Body: { "telefono": "+525512345678", "urlDocumento": "https://...", 
     *         "nombreArchivo": "factura.pdf", "caption": "Tu factura", "idUsuario": 1 }
     */
    @PostMapping("/enviar-documento")
    public ResponseEntity<?> enviarDocumento(@RequestBody EnviarDocumentoRequest request) {
        
        log.info("Solicitud de envío de documento '{}' a: {}", 
                request.getNombreArchivo(), request.getTelefono());
        
        try {
            MensajeWhatsApp mensaje = whatsAppService.enviarDocumento(
                    request.getTelefono(),
                    request.getUrlDocumento(),
                    request.getNombreArchivo(),
                    request.getCaption(),
                    request.getIdUsuario()
            );
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Documento enviado exitosamente",
                    "idMensaje", mensaje.getIdMensaje(),
                    "estado", mensaje.getEstado().name()
            ));
            
        } catch (Exception e) {
            log.error("Error al enviar documento", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al enviar documento: " + e.getMessage()
                    ));
        }
    }
    
    /**
     * Obtiene el historial de mensajes de un usuario
     * 
     * GET /api/whatsapp/mensajes/usuario/{idUsuario}
     */
    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<List<WhatsAppMensajeDTO>> obtenerHistorialUsuario(
            @PathVariable Integer idUsuario) {
        
        log.info("Consultando historial de usuario: {}", idUsuario);
        List<WhatsAppMensajeDTO> mensajes = mensajeService.obtenerHistorialUsuario(idUsuario);
        return ResponseEntity.ok(mensajes);
    }
    
    /**
     * Obtiene un mensaje específico por su ID
     * 
     * GET /api/whatsapp/mensajes/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerMensajePorId(@PathVariable Long id) {
        try {
            log.info("Consultando mensaje por ID: {}", id);
            WhatsAppMensajeDTO mensaje = mensajeService.obtenerPorIdDTO(id);
            
            if (mensaje == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "success", false,
                                "message", "Mensaje no encontrado"
                        ));
            }
            
            return ResponseEntity.ok(mensaje);
            
        } catch (Exception e) {
            log.error("Error al obtener mensaje", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al obtener mensaje: " + e.getMessage()
                    ));
        }
    }
    
    /**
     * Obtiene mensajes recientes de un teléfono
     * 
     * GET /api/whatsapp/mensajes/telefono/{telefono}
     */
    @GetMapping("/telefono/{telefono}")
    public ResponseEntity<List<WhatsAppMensajeDTO>> obtenerMensajesTelefono(
            @PathVariable String telefono) {
        
        log.info("Consultando mensajes de teléfono: {}", telefono);
        List<WhatsAppMensajeDTO> mensajes = mensajeService.obtenerMensajesRecientes(telefono);
        return ResponseEntity.ok(mensajes);
    }
    
    /**
     * Obtiene mensajes por estado
     * 
     * GET /api/whatsapp/mensajes/estado/{estado}
     */
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<WhatsAppMensajeDTO>> obtenerPorEstado(
            @PathVariable MensajeWhatsApp.EstadoMensaje estado) {
        
        log.info("Consultando mensajes con estado: {}", estado);
        List<WhatsAppMensajeDTO> mensajes = mensajeService.obtenerPorEstado(estado);
        return ResponseEntity.ok(mensajes);
    }
    
    /**
     * Obtiene mensajes fallidos recientes
     * 
     * GET /api/whatsapp/mensajes/fallidos?horas=24
     */
    @GetMapping("/fallidos")
    public ResponseEntity<List<WhatsAppMensajeDTO>> obtenerMensajesFallidos(
            @RequestParam(defaultValue = "24") int horas) {
        
        log.info("Consultando mensajes fallidos de las últimas {} horas", horas);
        List<WhatsAppMensajeDTO> mensajes = mensajeService.obtenerMensajesFallidos(horas);
        return ResponseEntity.ok(mensajes);
    }
    
    /**
     * Obtiene estadísticas generales de mensajes
     * 
     * GET /api/whatsapp/mensajes/estadisticas
     */
    @GetMapping("/estadisticas")
    public ResponseEntity<MensajeWhatsAppService.EstadisticasMensajes> obtenerEstadisticas() {
        
        log.info("Consultando estadísticas generales de mensajes");
        MensajeWhatsAppService.EstadisticasMensajes stats = mensajeService.obtenerEstadisticas();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Obtiene estadísticas de mensajes de un usuario
     * 
     * GET /api/whatsapp/mensajes/estadisticas/usuario/{idUsuario}
     */
    @GetMapping("/estadisticas/usuario/{idUsuario}")
    public ResponseEntity<MensajeWhatsAppService.EstadisticasMensajes> obtenerEstadisticasUsuario(
            @PathVariable Integer idUsuario) {
        
        log.info("Consultando estadísticas de usuario: {}", idUsuario);
        MensajeWhatsAppService.EstadisticasMensajes stats = mensajeService.obtenerEstadisticasUsuario(idUsuario);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Reintenta enviar mensajes fallidos
     * 
     * POST /api/whatsapp/mensajes/reintentar
     */
    @PostMapping("/reintentar")
    public ResponseEntity<?> reintentarMensajesFallidos() {
        
        log.info("Reintentando envío de mensajes fallidos");
        
        try {
            int exitosos = whatsAppService.reintentarMensajesFallidos();
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Reintentos completados",
                    "mensajesExitosos", exitosos
            ));
            
        } catch (Exception e) {
            log.error("Error al reintentar mensajes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al reintentar mensajes: " + e.getMessage()
                    ));
        }
    }
    
    // ========================================
    // CLASES INTERNAS - REQUEST BODIES
    // ========================================
    
    @lombok.Data
    public static class EnviarMensajeRequest {
        @NotBlank(message = "El teléfono es obligatorio")
        private String telefono;
        
        @NotBlank(message = "El mensaje es obligatorio")
        private String mensaje;
        
        @NotNull(message = "El ID de usuario es obligatorio")
        private Integer idUsuario;
    }
    
    @lombok.Data
    public static class EnviarPlantillaRequest {
        @NotBlank(message = "El teléfono es obligatorio")
        private String telefono;
        
        @NotBlank(message = "El nombre de la plantilla es obligatorio")
        private String nombrePlantilla;
        
        private List<String> parametros;
        
        @NotNull(message = "El ID de usuario es obligatorio")
        private Integer idUsuario;
    }
    
    @lombok.Data
    public static class EnviarDocumentoRequest {
        @NotBlank(message = "El teléfono es obligatorio")
        private String telefono;
        
        @NotBlank(message = "La URL del documento es obligatoria")
        private String urlDocumento;
        
        @NotBlank(message = "El nombre del archivo es obligatorio")
        private String nombreArchivo;
        
        private String caption;
        
        @NotNull(message = "El ID de usuario es obligatorio")
        private Integer idUsuario;
    }
}
