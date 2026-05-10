package api.astro.whats_orders_manager.modules.configuracion.controller;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmail;
import api.astro.whats_orders_manager.modules.configuracion.dto.ConfiguracionEmailDTO;
import api.astro.whats_orders_manager.modules.configuracion.service.ConfiguracionEmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ============================================================================
 * CONFIGURACION EMAIL REST CONTROLLER
 * WhatsApp Orders Manager
 * ============================================================================
 * Controlador REST para gestión de configuración de email/SMTP
 * Endpoints protegidos por autenticación
 * 
 * @author Astro Dev Team
 * @version 1.0
 * @since Sprint 4 - Fase 1.5
 * ============================================================================
 */
@RestController
@RequestMapping("/api/configuracion/email")
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class ConfiguracionEmailRestController {

    @Autowired
    private ConfiguracionEmailService configuracionEmailService;

    /**
     * Obtiene la configuración de email
     * 
     * GET /api/configuracion/email
     */
    @GetMapping
    public ResponseEntity<?> obtenerConfiguracion() {
        try {
            log.info("GET /api/configuracion/email - Obteniendo configuración de email");
            
            ConfiguracionEmail configuracion = configuracionEmailService.obtenerOCrearConfiguracion();
            ConfiguracionEmailDTO dto = convertirADTO(configuracion);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", dto
            ));
            
        } catch (Exception e) {
            log.error("Error al obtener configuración de email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al obtener la configuración: " + e.getMessage()
                    ));
        }
    }

    /**
     * Crea una nueva configuración de email
     * 
     * POST /api/configuracion/email
     */
    @PostMapping
    public ResponseEntity<?> crearConfiguracion(@RequestBody ConfiguracionEmailDTO dto) {
        try {
            log.info("POST /api/configuracion/email - Creando configuración de email");
            
            ConfiguracionEmail configuracion = convertirAEntidad(dto);
            ConfiguracionEmail guardada;
            
            if (configuracion.getIdConfiguracion() != null && configuracion.getIdConfiguracion() > 0) {
                guardada = configuracionEmailService.actualizarConfiguracion(configuracion);
            } else {
                guardada = configuracionEmailService.guardarConfiguracion(configuracion);
            }
            
            log.info("Configuración de email guardada: ID {}", guardada.getIdConfiguracion());
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Configuración guardada exitosamente",
                    "data", convertirADTO(guardada)
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación al crear configuración: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        } catch (Exception e) {
            log.error("Error al crear configuración de email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al guardar la configuración: " + e.getMessage()
                    ));
        }
    }

    /**
     * Actualiza la configuración de email existente
     * 
     * PUT /api/configuracion/email
     */
    @PutMapping
    public ResponseEntity<?> actualizarConfiguracion(@RequestBody ConfiguracionEmailDTO dto) {
        try {
            log.info("PUT /api/configuracion/email - Actualizando configuración de email");
            
            ConfiguracionEmail configuracion = convertirAEntidad(dto);
            ConfiguracionEmail guardada;
            
            if (configuracion.getIdConfiguracion() != null && configuracion.getIdConfiguracion() > 0) {
                guardada = configuracionEmailService.actualizarConfiguracion(configuracion);
            } else {
                guardada = configuracionEmailService.guardarConfiguracion(configuracion);
            }
            
            log.info("Configuración de email actualizada: ID {}", guardada.getIdConfiguracion());
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Configuración actualizada exitosamente",
                    "data", convertirADTO(guardada)
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación al actualizar configuración: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        } catch (Exception e) {
            log.error("Error al actualizar configuración de email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al actualizar la configuración: " + e.getMessage()
                    ));
        }
    }

    /**
     * Prueba la configuración enviando un email de prueba
     * 
     * POST /api/configuracion/email/probar
     */
    @PostMapping("/probar")
    public ResponseEntity<?> probarConfiguracion(@RequestBody Map<String, String> request) {
        try {
            String emailDestino = request.get("emailDestino");
            
            if (emailDestino == null || emailDestino.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "success", false,
                                "message", "El email de destino es obligatorio"
                        ));
            }
            
            log.info("POST /api/configuracion/email/probar - Probando configuración con email: {}", emailDestino);
            
            boolean exitoso = configuracionEmailService.probarConfiguracion(emailDestino);
            
            if (exitoso) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Email de prueba enviado exitosamente a " + emailDestino
                ));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of(
                                "success", false,
                                "message", "No se pudo enviar el email de prueba"
                        ));
            }
            
        } catch (Exception e) {
            log.error("Error al probar configuración de email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al enviar email de prueba: " + e.getMessage()
                    ));
        }
    }

    /**
     * Cambia el estado de la configuración (activo/inactivo)
     * 
     * PATCH /api/configuracion/email/estado
     */
    @PatchMapping("/estado")
    public ResponseEntity<?> cambiarEstado(@RequestBody Map<String, Boolean> request) {
        try {
            Boolean activo = request.get("activo");
            
            if (activo == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "success", false,
                                "message", "El parámetro 'activo' es obligatorio"
                        ));
            }
            
            log.info("PATCH /api/configuracion/email/estado - Cambiando estado a: {}", activo);
            
            ConfiguracionEmail actualizada = configuracionEmailService.cambiarEstado(activo);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Estado actualizado a " + (activo ? "activo" : "inactivo"),
                    "data", convertirADTO(actualizada)
            ));
            
        } catch (Exception e) {
            log.error("Error al cambiar estado de configuración", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al cambiar estado: " + e.getMessage()
                    ));
        }
    }

    /**
     * Valida que la configuración esté completa
     * 
     * GET /api/configuracion/email/validar
     */
    @GetMapping("/validar")
    public ResponseEntity<?> validarConfiguracion() {
        try {
            log.info("GET /api/configuracion/email/validar");
            
            boolean valida = configuracionEmailService.validarConfiguracion();
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "valida", valida,
                    "message", valida ? "Configuración completa" : "Falta información de configuración"
            ));
            
        } catch (Exception e) {
            log.error("Error al validar configuración", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al validar: " + e.getMessage()
                    ));
        }
    }

    // ==================== MÉTODOS PRIVADOS ====================

    /**
     * Convierte ConfiguracionEmail a DTO
     */
    private ConfiguracionEmailDTO convertirADTO(ConfiguracionEmail entidad) {
        ConfiguracionEmailDTO dto = new ConfiguracionEmailDTO();
        
        dto.setIdConfiguracion(entidad.getIdConfiguracion());
        dto.setSmtpHost(entidad.getSmtpHost());
        dto.setSmtpPort(entidad.getSmtpPort());
        dto.setSmtpUsuario(entidad.getSmtpUsuario());
        dto.setSmtpPassword("********"); // No exponer password real
        dto.setSmtpSsl(entidad.getSmtpSsl());
        dto.setSmtpTls(entidad.getSmtpTls());
        dto.setSmtpAuth(entidad.getSmtpAuth());
        dto.setEmailRemitente(entidad.getEmailRemitente());
        dto.setNombreRemitente(entidad.getNombreRemitente());
        dto.setEmailCopia(entidad.getEmailCopia());
        dto.setEmailCopiaOculta(entidad.getEmailCopiaOculta());
        dto.setActivo(entidad.getActivo());
        dto.setUltimoTest(entidad.getUltimoTest());
        dto.setEstadoUltimoTest(entidad.getEstadoUltimoTest());
        
        // Campos calculados
        dto.setProtocoloSeguridad(entidad.getProtocoloSeguridad());
        dto.setConfiguracionCompleta(entidad.isConfiguracionCompleta());
        dto.setUltimoTestExitoso(entidad.isUltimoTestExitoso());
        
        return dto;
    }

    /**
     * Convierte DTO a ConfiguracionEmail
     */
    private ConfiguracionEmail convertirAEntidad(ConfiguracionEmailDTO dto) {
        return ConfiguracionEmail.builder()
                .idConfiguracion(dto.getIdConfiguracion())
                .smtpHost(dto.getSmtpHost())
                .smtpPort(dto.getSmtpPort())
                .smtpUsuario(dto.getSmtpUsuario())
                .smtpPassword(dto.getSmtpPassword())
                .smtpSsl(dto.getSmtpSsl())
                .smtpTls(dto.getSmtpTls())
                .smtpAuth(dto.getSmtpAuth())
                .emailRemitente(dto.getEmailRemitente())
                .nombreRemitente(dto.getNombreRemitente())
                .emailCopia(dto.getEmailCopia())
                .emailCopiaOculta(dto.getEmailCopiaOculta())
                .activo(dto.getActivo())
                .build();
    }
}
