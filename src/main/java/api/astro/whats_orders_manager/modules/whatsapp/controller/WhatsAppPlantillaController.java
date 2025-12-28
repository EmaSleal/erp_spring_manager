package api.astro.whats_orders_manager.modules.whatsapp.controller;


import api.astro.whats_orders_manager.modules.whatsapp.model.PlantillaWhatsApp;
import api.astro.whats_orders_manager.modules.whatsapp.dto.PlantillaWhatsAppDTO;
import api.astro.whats_orders_manager.modules.whatsapp.service.PlantillaWhatsAppService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador REST para gestión de plantillas de WhatsApp
 * Endpoints protegidos por autenticación
 * 
 * @author EmaSleal
 * @version 1.0
 * @since Sprint 3 - Fase 1.4
 */
@RestController
@RequestMapping("/api/whatsapp/plantillas")
@Slf4j
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
public class WhatsAppPlantillaController {
    
    private final PlantillaWhatsAppService plantillaService;
    
    public WhatsAppPlantillaController(PlantillaWhatsAppService plantillaService) {
        this.plantillaService = plantillaService;
    }
    
    /**
     * Obtiene todas las plantillas activas
     * 
     * GET /api/whatsapp/plantillas
     */
    @GetMapping
    public ResponseEntity<List<PlantillaWhatsAppDTO>> obtenerPlantillasActivas() {
        log.info("Consultando plantillas activas");
        List<PlantillaWhatsAppDTO> plantillas = plantillaService.obtenerPlantillasActivas();
        return ResponseEntity.ok(plantillas);
    }
    
    /**
     * Obtiene todas las plantillas (incluyendo inactivas)
     * 
     * GET /api/whatsapp/plantillas/todas
     */
    @GetMapping("/todas")
    public ResponseEntity<List<PlantillaWhatsAppDTO>> obtenerTodasPlantillas() {
        log.info("Consultando todas las plantillas");
        List<PlantillaWhatsAppDTO> plantillas = plantillaService.obtenerTodas();
        return ResponseEntity.ok(plantillas);
    }
    
    /**
     * Obtiene plantillas aprobadas por Meta
     * 
     * GET /api/whatsapp/plantillas/aprobadas
     */
    @GetMapping("/aprobadas")
    public ResponseEntity<List<PlantillaWhatsAppDTO>> obtenerPlantillasAprobadas() {
        log.info("Consultando plantillas aprobadas");
        List<PlantillaWhatsAppDTO> plantillas = plantillaService.obtenerPlantillasAprobadas();
        return ResponseEntity.ok(plantillas);
    }
    
    /**
     * Obtiene una plantilla por ID
     * 
     * GET /api/whatsapp/plantillas/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Integer id) {
        log.info("Consultando plantilla ID: {}", id);
        
        Optional<PlantillaWhatsApp> plantilla = plantillaService.obtenerPorId(id);
        
        if (plantilla.isPresent()) {
            return ResponseEntity.ok(plantilla.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Plantilla no encontrada"));
        }
    }
    
    /**
     * Obtiene una plantilla por nombre
     * 
     * GET /api/whatsapp/plantillas/nombre/{nombre}
     */
    @GetMapping("/nombre/{nombre}")
    public ResponseEntity<?> obtenerPorNombre(@PathVariable String nombre) {
        log.info("Consultando plantilla por nombre: {}", nombre);
        
        Optional<PlantillaWhatsApp> plantilla = plantillaService.obtenerPorNombre(nombre);
        
        if (plantilla.isPresent()) {
            return ResponseEntity.ok(plantilla.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Plantilla no encontrada"));
        }
    }
    
    /**
     * Obtiene plantillas por categoría
     * 
     * GET /api/whatsapp/plantillas/categoria/{categoria}
     */
    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<List<PlantillaWhatsAppDTO>> obtenerPorCategoria(
            @PathVariable PlantillaWhatsApp.CategoriaPlantilla categoria) {
        
        log.info("Consultando plantillas de categoría: {}", categoria);
        List<PlantillaWhatsAppDTO> plantillas = plantillaService.obtenerPorCategoria(categoria);
        return ResponseEntity.ok(plantillas);
    }
    
    /**
     * Obtiene plantillas por estado en Meta
     * 
     * GET /api/whatsapp/plantillas/estado/{estado}
     */
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<PlantillaWhatsAppDTO>> obtenerPorEstado(
            @PathVariable PlantillaWhatsApp.EstadoMeta estado) {
        
        log.info("Consultando plantillas con estado: {}", estado);
        List<PlantillaWhatsAppDTO> plantillas = plantillaService.obtenerPorEstado(estado);
        return ResponseEntity.ok(plantillas);
    }
    
    /**
     * Crea una nueva plantilla
     * 
     * POST /api/whatsapp/plantillas
     */
    @PostMapping
    public ResponseEntity<?> crearPlantilla(@Valid @RequestBody PlantillaWhatsApp plantilla) {
        log.info("Creando nueva plantilla: {}", plantilla.getNombre());
        
        try {
            PlantillaWhatsApp creada = plantillaService.crear(plantilla);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "success", true,
                            "message", "Plantilla creada exitosamente",
                            "plantilla", creada
                    ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        } catch (Exception e) {
            log.error("Error al crear plantilla", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al crear plantilla: " + e.getMessage()
                    ));
        }
    }
    
    /**
     * Actualiza una plantilla existente
     * 
     * PUT /api/whatsapp/plantillas/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarPlantilla(
            @PathVariable Integer id,
            @Valid @RequestBody PlantillaWhatsApp plantilla) {
        
        log.info("Actualizando plantilla ID: {}", id);
        
        try {
            Optional<PlantillaWhatsApp> actualizada = plantillaService.actualizar(id, plantilla);
            
            if (actualizada.isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Plantilla actualizada exitosamente",
                        "plantilla", actualizada.get()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "success", false,
                                "message", "Plantilla no encontrada"
                        ));
            }
            
        } catch (Exception e) {
            log.error("Error al actualizar plantilla", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al actualizar plantilla: " + e.getMessage()
                    ));
        }
    }
    
    /**
     * Marca una plantilla como aprobada por Meta
     * 
     * POST /api/whatsapp/plantillas/{id}/aprobar
     */
    @PostMapping("/{id}/aprobar")
    public ResponseEntity<?> marcarComoAprobada(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        
        String templateId = body.get("templateId");
        log.info("Marcando plantilla {} como aprobada con template ID: {}", id, templateId);
        
        try {
            Optional<PlantillaWhatsApp> plantilla = plantillaService.marcarComoAprobada(id, templateId);
            
            if (plantilla.isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Plantilla marcada como aprobada",
                        "plantilla", plantilla.get()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "success", false,
                                "message", "Plantilla no encontrada"
                        ));
            }
            
        } catch (Exception e) {
            log.error("Error al aprobar plantilla", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al aprobar plantilla: " + e.getMessage()
                    ));
        }
    }
    
    /**
     * Marca una plantilla como rechazada por Meta
     * 
     * POST /api/whatsapp/plantillas/{id}/rechazar
     */
    @PostMapping("/{id}/rechazar")
    public ResponseEntity<?> marcarComoRechazada(@PathVariable Integer id) {
        log.info("Marcando plantilla {} como rechazada", id);
        
        try {
            Optional<PlantillaWhatsApp> plantilla = plantillaService.marcarComoRechazada(id);
            
            if (plantilla.isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Plantilla marcada como rechazada",
                        "plantilla", plantilla.get()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "success", false,
                                "message", "Plantilla no encontrada"
                        ));
            }
            
        } catch (Exception e) {
            log.error("Error al rechazar plantilla", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al rechazar plantilla: " + e.getMessage()
                    ));
        }
    }
    
    /**
     * Activa una plantilla
     * 
     * POST /api/whatsapp/plantillas/{id}/activar
     */
    @PostMapping("/{id}/activar")
    public ResponseEntity<?> activarPlantilla(@PathVariable Integer id) {
        log.info("Activando plantilla ID: {}", id);
        
        try {
            Optional<PlantillaWhatsApp> plantilla = plantillaService.activar(id);
            
            if (plantilla.isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Plantilla activada exitosamente"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "success", false,
                                "message", "Plantilla no encontrada"
                        ));
            }
            
        } catch (Exception e) {
            log.error("Error al activar plantilla", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al activar plantilla: " + e.getMessage()
                    ));
        }
    }
    
    /**
     * Desactiva una plantilla
     * 
     * POST /api/whatsapp/plantillas/{id}/desactivar
     */
    @PostMapping("/{id}/desactivar")
    public ResponseEntity<?> desactivarPlantilla(@PathVariable Integer id) {
        log.info("Desactivando plantilla ID: {}", id);
        
        try {
            Optional<PlantillaWhatsApp> plantilla = plantillaService.desactivar(id);
            
            if (plantilla.isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Plantilla desactivada exitosamente"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "success", false,
                                "message", "Plantilla no encontrada"
                        ));
            }
            
        } catch (Exception e) {
            log.error("Error al desactivar plantilla", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al desactivar plantilla: " + e.getMessage()
                    ));
        }
    }
    
    /**
     * Elimina una plantilla
     * 
     * DELETE /api/whatsapp/plantillas/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarPlantilla(@PathVariable Integer id) {
        log.info("Eliminando plantilla ID: {}", id);
        
        try {
            plantillaService.eliminar(id);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Plantilla eliminada exitosamente"
            ));
            
        } catch (Exception e) {
            log.error("Error al eliminar plantilla", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al eliminar plantilla: " + e.getMessage()
                    ));
        }
    }
    
    /**
     * Valida parámetros de una plantilla
     * 
     * POST /api/whatsapp/plantillas/validar-parametros
     */
    @PostMapping("/validar-parametros")
    public ResponseEntity<?> validarParametros(@RequestBody ValidarParametrosRequest request) {
        log.info("Validando parámetros para plantilla: {}", request.getNombrePlantilla());
        
        boolean valido = plantillaService.validarParametros(
                request.getNombrePlantilla(),
                request.getParametros()
        );
        
        return ResponseEntity.ok(Map.of(
                "valido", valido,
                "message", valido ? "Parámetros válidos" : "Parámetros inválidos"
        ));
    }
    
    /**
     * Limpia el caché de plantillas
     * 
     * POST /api/whatsapp/plantillas/limpiar-cache
     */
    @PostMapping("/limpiar-cache")
    public ResponseEntity<?> limpiarCache() {
        log.info("Limpiando caché de plantillas");
        plantillaService.limpiarCache();
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Caché limpiado exitosamente"
        ));
    }
    
    // ========================================
    // CLASES INTERNAS - REQUEST BODIES
    // ========================================
    
    @lombok.Data
    public static class ValidarParametrosRequest {
        private String nombrePlantilla;
        private List<String> parametros;
    }
}
