package api.astro.whats_orders_manager.modules.facturacion.electronica.controller;

import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ConfiguracionHaciendaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConfiguracionHaciendaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestión de configuración de Hacienda.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
@RestController
@RequestMapping("/api/facturas/configuracion-hacienda")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ConfiguracionHaciendaController {
    
    private final ConfiguracionHaciendaService configuracionService;
    
    /**
     * Obtiene configuración activa de una empresa.
     * GET /api/facturacion/configuracion-hacienda/empresa/{empresaId}/activa
     */
    @GetMapping("/empresa/{empresaId}/activa")
    public ResponseEntity<ConfiguracionHaciendaDTO> obtenerConfiguracionActiva(
            @PathVariable Integer empresaId) {
        log.info("Obteniendo configuración activa para empresa {}", empresaId);
        return configuracionService.obtenerConfiguracionActiva(empresaId.longValue())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Lista todas las configuraciones de una empresa.
     * GET /api/facturacion/configuracion-hacienda/empresa/{empresaId}
     */
    @GetMapping("/empresa/{empresaId}")
    public ResponseEntity<List<ConfiguracionHaciendaDTO>> listarConfiguraciones(
            @PathVariable Integer empresaId) {
        log.info("Listando configuraciones de Hacienda para empresa {}", empresaId);
        List<ConfiguracionHaciendaDTO> configuraciones = configuracionService.listarPorEmpresa(empresaId.longValue());
        return ResponseEntity.ok(configuraciones);
    }
    
    /**
     * Obtiene configuración por ambiente específico.
     * GET /api/facturacion/configuracion-hacienda/empresa/{empresaId}/ambiente/{ambiente}
     */
    @GetMapping("/empresa/{empresaId}/ambiente/{ambiente}")
    public ResponseEntity<ConfiguracionHaciendaDTO> obtenerPorAmbiente(
            @PathVariable Integer empresaId,
            @PathVariable AmbienteHacienda ambiente) {
        log.info("Obteniendo configuración {} para empresa {}", ambiente, empresaId);
        ConfiguracionHacienda config = configuracionService.obtenerPorEmpresaYAmbiente(empresaId.longValue(), ambiente)
                .orElse(null);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ConfiguracionHaciendaDTO.builder()
                .id(config.getId())
                .empresaId(config.getEmpresa().getIdEmpresa().longValue())
                .ambiente(config.getAmbiente())
                .usuarioHacienda(config.getUsuarioHacienda())
                .passwordHacienda(config.getPasswordHacienda())
                .rutaCertificado(config.getRutaCertificado())
                .pinCertificado(config.getPinCertificado())
                .activa(config.getActiva())
                .consecutivoFactura(config.getConsecutivoFactura())
                .consecutivoTiquete(config.getConsecutivoTiquete())
                .consecutivoNotaCredito(config.getConsecutivoNotaCredito())
                .consecutivoNotaDebito(config.getConsecutivoNotaDebito())
                .accessToken(config.getAccessToken())
                .tokenExpira(config.getTokenExpira())
                .build());
    }
    
    /**
     * Crea nueva configuración de Hacienda.
     * POST /api/facturacion/configuracion-hacienda
     */
    @PostMapping
    public ResponseEntity<ConfiguracionHaciendaDTO> crear(
            @Valid @RequestBody ConfiguracionHaciendaDTO dto) {
        log.info("Creando nueva configuración de Hacienda para empresa {}", dto.getEmpresaId());
        ConfiguracionHaciendaDTO creada = configuracionService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }
    
    /**
     * Actualiza configuración existente.
     * PUT /api/facturacion/configuracion-hacienda/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ConfiguracionHaciendaDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ConfiguracionHaciendaDTO dto) {
        log.info("Actualizando configuración de Hacienda {}", id);
        ConfiguracionHaciendaDTO actualizada = configuracionService.actualizar(id, dto);
        return ResponseEntity.ok(actualizada);
    }
    
    /**
     * Activa una configuración específica.
     * PUT /api/facturacion/configuracion-hacienda/{id}/activar
     */
    @PutMapping("/{id}/activar")
    public ResponseEntity<ConfiguracionHaciendaDTO> activar(@PathVariable Long id) {
        log.info("Activando configuración {}", id);
        ConfiguracionHaciendaDTO activada = configuracionService.activar(id);
        return ResponseEntity.ok(activada);
    }
    
    /**
     * Desactiva una configuración.
     * PUT /api/facturacion/configuracion-hacienda/{id}/desactivar
     */
    @PutMapping("/{id}/desactivar")
    public ResponseEntity<ConfiguracionHaciendaDTO> desactivar(@PathVariable Long id) {
        log.info("Desactivando configuración {}", id);
        ConfiguracionHaciendaDTO desactivada = configuracionService.desactivar(id);
        return ResponseEntity.ok(desactivada);
    }
    
    /**
     * Renueva el token de OAuth2 de Hacienda.
     * POST /api/facturacion/configuracion-hacienda/{id}/renovar-token
     */
    @PostMapping("/{id}/renovar-token")
    public ResponseEntity<Map<String, Object>> renovarToken(@PathVariable Long id) {
        log.info("Renovando token OAuth2 para configuración {}", id);
        try {
            configuracionService.renovarToken(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Token renovado exitosamente"
            ));
        } catch (Exception e) {
            log.error("Error renovando token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "success", false,
                        "message", "Error renovando token: " + e.getMessage()
                    ));
        }
    }
    
    /**
     * Verifica estado del token (si está vigente o necesita renovación).
     * GET /api/facturacion/configuracion-hacienda/{id}/estado-token
     */
    @GetMapping("/{id}/estado-token")
    public ResponseEntity<Map<String, Object>> verificarEstadoToken(@PathVariable Long id) {
        log.info("Verificando estado del token para configuración {}", id);
        Map<String, Object> payload = configuracionService.obtenerPorId(id)
        .<Map<String, Object>>map(cfg -> {
            boolean valido = cfg.getAccessToken() != null
                    && cfg.getTokenExpira() != null
                    && cfg.getTokenExpira().isAfter(LocalDateTime.now());

            return Map.<String, Object>of(
                "id", cfg.getId(),
                "tokenValido", valido,
                "expira", (cfg.getTokenExpira() != null) ? cfg.getTokenExpira().toString() : "N/A",
                "tieneToken", cfg.getAccessToken() != null
            );
        })
        .orElseGet(() -> Map.<String, Object>of(
            "error", "Configuración no encontrada"
        ));

        return ResponseEntity.ok(payload);
    }
    
    /**
     * Obtiene consecutivos actuales de una configuración.
     * GET /api/facturacion/configuracion-hacienda/{id}/consecutivos
     */
    @GetMapping("/{id}/consecutivos")
    public ResponseEntity<Map<String, Long>> obtenerConsecutivos(@PathVariable Long id) {
        log.info("Obteniendo consecutivos para configuración {}", id);
        return configuracionService.obtenerPorId(id)
                .map(config -> ResponseEntity.ok(Map.of(
                    "facturaElectronica", config.getConsecutivoFactura(),
                    "tiqueteElectronico", config.getConsecutivoTiquete(),
                    "notaCredito", config.getConsecutivoNotaCredito(),
                    "notaDebito", config.getConsecutivoNotaDebito()
                )))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Elimina una configuración (soft delete).
     * DELETE /api/facturacion/configuracion-hacienda/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.info("Eliminando configuración {}", id);
        configuracionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
