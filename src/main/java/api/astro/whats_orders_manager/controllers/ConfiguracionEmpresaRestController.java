package api.astro.whats_orders_manager.controllers;

import api.astro.whats_orders_manager.models.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.models.dto.ConfiguracionEmpresaDTO;
import api.astro.whats_orders_manager.services.ConfiguracionEmpresaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * ============================================================================
 * CONFIGURACION EMPRESA REST CONTROLLER
 * WhatsApp Orders Manager
 * ============================================================================
 * Controlador REST para gestión de configuración de empresa
 * Endpoints protegidos por autenticación
 * 
 * @author Astro Dev Team
 * @version 1.0
 * @since Sprint 4 - Fase 1.5
 * ============================================================================
 */
@RestController
@RequestMapping("/api/configuracion/empresa")
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class ConfiguracionEmpresaRestController {

    @Autowired
    private ConfiguracionEmpresaService configuracionEmpresaService;

    /**
     * Obtiene la configuración de la empresa
     * 
     * GET /api/configuracion/empresa
     */
    @GetMapping
    public ResponseEntity<?> obtenerConfiguracion() {
        try {
            log.info("GET /api/configuracion/empresa - Obteniendo configuración de empresa");
            
            ConfiguracionEmpresa configuracion = configuracionEmpresaService.obtenerOCrearConfiguracion();
            ConfiguracionEmpresaDTO dto = convertirADTO(configuracion);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", dto
            ));
            
        } catch (Exception e) {
            log.error("Error al obtener configuración de empresa", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al obtener la configuración: " + e.getMessage()
                    ));
        }
    }

    /**
     * Crea una nueva configuración de empresa
     * 
     * POST /api/configuracion/empresa
     */
    @PostMapping
    public ResponseEntity<?> crearConfiguracion(@RequestBody ConfiguracionEmpresaDTO dto) {
        try {
            log.info("POST /api/configuracion/empresa - Creando configuración de empresa");
            
            ConfiguracionEmpresa configuracion = convertirAEntidad(dto);
            ConfiguracionEmpresa guardada;
            
            if (configuracion.getIdConfiguracion() != null && configuracion.getIdConfiguracion() > 0) {
                guardada = configuracionEmpresaService.actualizarConfiguracion(configuracion);
            } else {
                guardada = configuracionEmpresaService.guardarConfiguracion(configuracion);
            }
            
            log.info("Configuración de empresa guardada: ID {}", guardada.getIdConfiguracion());
            
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
            log.error("Error al crear configuración de empresa", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al guardar la configuración: " + e.getMessage()
                    ));
        }
    }

    /**
     * Actualiza la configuración de empresa existente
     * 
     * PUT /api/configuracion/empresa
     */
    @PutMapping
    public ResponseEntity<?> actualizarConfiguracion(@RequestBody ConfiguracionEmpresaDTO dto) {
        try {
            log.info("PUT /api/configuracion/empresa - Actualizando configuración de empresa");
            
            ConfiguracionEmpresa configuracion = convertirAEntidad(dto);
            ConfiguracionEmpresa guardada;
            
            if (configuracion.getIdConfiguracion() != null && configuracion.getIdConfiguracion() > 0) {
                guardada = configuracionEmpresaService.actualizarConfiguracion(configuracion);
            } else {
                guardada = configuracionEmpresaService.guardarConfiguracion(configuracion);
            }
            
            log.info("Configuración de empresa actualizada: ID {}", guardada.getIdConfiguracion());
            
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
            log.error("Error al actualizar configuración de empresa", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al actualizar la configuración: " + e.getMessage()
                    ));
        }
    }

    /**
     * Valida los datos fiscales de la empresa
     * 
     * GET /api/configuracion/empresa/validar-fiscales
     */
    @GetMapping("/validar-fiscales")
    public ResponseEntity<?> validarDatosFiscales() {
        try {
            log.info("GET /api/configuracion/empresa/validar-fiscales");
            
            boolean validos = configuracionEmpresaService.validarDatosFiscales();
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "validos", validos,
                    "message", validos ? "Datos fiscales completos" : "Faltan datos fiscales"
            ));
            
        } catch (Exception e) {
            log.error("Error al validar datos fiscales", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al validar: " + e.getMessage()
                    ));
        }
    }

    // ==================== MÉTODOS PRIVADOS ====================

    /**
     * Convierte ConfiguracionEmpresa a DTO
     */
    private ConfiguracionEmpresaDTO convertirADTO(ConfiguracionEmpresa entidad) {
        ConfiguracionEmpresaDTO dto = new ConfiguracionEmpresaDTO();
        
        dto.setIdConfiguracion(entidad.getIdConfiguracion());
        dto.setRazonSocial(entidad.getRazonSocial());
        dto.setNombreComercial(entidad.getNombreComercial());
        dto.setRfc(entidad.getRfc());
        dto.setRegimenFiscal(entidad.getRegimenFiscal());
        dto.setDireccionCalle(entidad.getDireccionCalle());
        dto.setDireccionNumero(entidad.getDireccionNumero());
        dto.setDireccionColonia(entidad.getDireccionColonia());
        dto.setDireccionCiudad(entidad.getDireccionCiudad());
        dto.setDireccionEstado(entidad.getDireccionEstado());
        dto.setDireccionCodigoPostal(entidad.getDireccionCodigoPostal());
        dto.setDireccionPais(entidad.getDireccionPais());
        dto.setTelefono(entidad.getTelefono());
        dto.setEmail(entidad.getEmail());
        dto.setSitioWeb(entidad.getSitioWeb());
        dto.setLogoUrl(entidad.getLogoUrl());
        dto.setColorPrimario(entidad.getColorPrimario());
        dto.setColorSecundario(entidad.getColorSecundario());
        
        // Campos calculados
        dto.setDireccionCompleta(entidad.getDireccionCompleta());
        dto.setTieneLogoConfigurado(entidad.tieneLogoConfigurado());
        dto.setDatosFiscalesCompletos(entidad.tieneDatosFiscalesCompletos());
        
        return dto;
    }

    /**
     * Convierte DTO a ConfiguracionEmpresa
     */
    private ConfiguracionEmpresa convertirAEntidad(ConfiguracionEmpresaDTO dto) {
        return ConfiguracionEmpresa.builder()
                .idConfiguracion(dto.getIdConfiguracion())
                .razonSocial(dto.getRazonSocial())
                .nombreComercial(dto.getNombreComercial())
                .rfc(dto.getRfc())
                .regimenFiscal(dto.getRegimenFiscal())
                .direccionCalle(dto.getDireccionCalle())
                .direccionNumero(dto.getDireccionNumero())
                .direccionColonia(dto.getDireccionColonia())
                .direccionCiudad(dto.getDireccionCiudad())
                .direccionEstado(dto.getDireccionEstado())
                .direccionCodigoPostal(dto.getDireccionCodigoPostal())
                .direccionPais(dto.getDireccionPais())
                .telefono(dto.getTelefono())
                .email(dto.getEmail())
                .sitioWeb(dto.getSitioWeb())
                .logoUrl(dto.getLogoUrl())
                .colorPrimario(dto.getColorPrimario())
                .colorSecundario(dto.getColorSecundario())
                .build();
    }
}
