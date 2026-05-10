package api.astro.whats_orders_manager.modules.facturacion.controller;

import api.astro.whats_orders_manager.modules.facturacion.model.ConfiguracionFacturacion;
import api.astro.whats_orders_manager.modules.facturacion.service.ConfiguracionFacturacionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ============================================================================
 * CONFIGURACION FACTURACION REST CONTROLLER
 * ERP Orders Manager
 * ============================================================================
 * Controlador REST para gestión de configuración de facturación
 * Endpoints protegidos por autenticación
 * 
 * @author Astro Dev Team
 * @version 1.0
 * @since Sprint 4 - Fase 1.6
 * ============================================================================
 */
@RestController
@RequestMapping("/api/configuracion/facturacion")
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class ConfiguracionFacturacionRestController {

    @Autowired
    private ConfiguracionFacturacionService configuracionFacturacionService;

    /**
     * Obtiene la configuración de facturación
     * 
     * GET /api/configuracion/facturacion
     */
    @GetMapping
    public ResponseEntity<?> obtenerConfiguracion() {
        try {
            log.info("GET /api/configuracion/facturacion - Obteniendo configuración de facturación");
            
            ConfiguracionFacturacion configuracion = configuracionFacturacionService.getOrCreateConfiguracion();
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", configuracion
            ));
            
        } catch (Exception e) {
            log.error("Error al obtener configuración de facturación", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al obtener la configuración: " + e.getMessage()
                    ));
        }
    }

    /**
     * Crea una nueva configuración de facturación
     * 
     * POST /api/configuracion/facturacion
     */
    @PostMapping
    public ResponseEntity<?> crearConfiguracion(@RequestBody ConfiguracionFacturacion configuracion) {
        try {
            log.info("POST /api/configuracion/facturacion - Creando configuración de facturación");
            
            ConfiguracionFacturacion guardada = configuracionFacturacionService.saveOrUpdate(configuracion);
            log.info("Configuración de facturación guardada: ID {}", guardada.getId());
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Configuración guardada exitosamente",
                    "data", guardada
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación al crear configuración: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        } catch (IllegalStateException e) {
            log.error("Error de estado al crear configuración: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        } catch (Exception e) {
            log.error("Error al crear configuración de facturación", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al guardar la configuración: " + e.getMessage()
                    ));
        }
    }

    /**
     * Actualiza la configuración de facturación existente
     * 
     * PUT /api/configuracion/facturacion
     */
    @PutMapping
    public ResponseEntity<?> actualizarConfiguracion(@RequestBody ConfiguracionFacturacion configuracion) {
        try {
            log.info("PUT /api/configuracion/facturacion - Actualizando configuración de facturación");
            
            ConfiguracionFacturacion guardada = configuracionFacturacionService.saveOrUpdate(configuracion);
            log.info("Configuración de facturación actualizada: ID {}", guardada.getId());
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Configuración actualizada exitosamente",
                    "data", guardada
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación al actualizar configuración: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        } catch (IllegalStateException e) {
            log.error("Error de estado al actualizar configuración: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        } catch (Exception e) {
            log.error("Error al actualizar configuración de facturación", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al actualizar la configuración: " + e.getMessage()
                    ));
        }
    }

    /**
     * Genera y devuelve un preview del número de factura
     * 
     * GET /api/configuracion/facturacion/preview-numero
     */
    @GetMapping("/preview-numero")
    public ResponseEntity<?> generarPreviewNumero() {
        try {
            log.info("GET /api/configuracion/facturacion/preview-numero");
            
            ConfiguracionFacturacion configuracion = configuracionFacturacionService.getOrCreateConfiguracion();
            String preview = configuracion.generarNumeroFactura();
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "preview", preview
            ));
            
        } catch (Exception e) {
            log.error("Error al generar preview de número", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al generar preview: " + e.getMessage()
                    ));
        }
    }
}
