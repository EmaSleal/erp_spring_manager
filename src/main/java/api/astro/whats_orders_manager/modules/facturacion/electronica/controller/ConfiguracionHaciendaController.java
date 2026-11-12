package api.astro.whats_orders_manager.modules.facturacion.electronica.controller;

import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ConfiguracionHaciendaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.mapper.ConfiguracionHaciendaMapper;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConfiguracionHaciendaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
public class ConfiguracionHaciendaController {

    /** T-030 (ADR-035): toda escritura de la configuración de Hacienda y del certificado es solo para ADMIN. */
    static final String SOLO_ADMIN = "hasRole('ADMIN')";

    private static final List<String> EXTENSIONES_CERTIFICADO = List.of(".p12", ".pfx");
    private static final DateTimeFormatter MARCA_TIEMPO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final ConfiguracionHaciendaService configuracionService;
    private final ConfiguracionHaciendaMapper mapper;

    @Value("${app.certificados.directorio:/app/certificados}")
    private String certificadosDir;

    @Value("${app.certificados.tamano-maximo-bytes:262144}")
    private long tamanoMaximoCertificado;
    
    /**
     * Obtiene configuración activa de una empresa.
     * GET /api/facturacion/configuracion-hacienda/empresa/{empresaId}/activa
     */
    @GetMapping("/empresa/{empresaId}/activa")
    public ResponseEntity<ConfiguracionHaciendaDTO> obtenerConfiguracionActiva(
            @PathVariable Integer empresaId) {
        log.info("Obteniendo configuración activa para empresa {}", empresaId);
        return configuracionService.obtenerConfiguracionActiva(empresaId)
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
        List<ConfiguracionHaciendaDTO> configuraciones = configuracionService.listarPorEmpresa(empresaId);
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
        // T-021: el mapper enmascara contraseña y PIN y nunca incluye los tokens
        return configuracionService.obtenerPorEmpresaYAmbiente(empresaId, ambiente)
                .map(mapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Crea nueva configuración de Hacienda.
     * POST /api/facturacion/configuracion-hacienda
     */
    @PreAuthorize(SOLO_ADMIN)
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
    @PreAuthorize(SOLO_ADMIN)
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
    @PreAuthorize(SOLO_ADMIN)
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
    @PreAuthorize(SOLO_ADMIN)
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
    @PreAuthorize(SOLO_ADMIN)
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
            boolean tieneToken = Boolean.TRUE.equals(cfg.getTieneToken());
            boolean valido = tieneToken
                    && cfg.getTokenExpira() != null
                    && cfg.getTokenExpira().isAfter(LocalDateTime.now());

            return Map.<String, Object>of(
                "id", cfg.getId(),
                "tokenValido", valido,
                "expira", (cfg.getTokenExpira() != null) ? cfg.getTokenExpira().toString() : "N/A",
                "tieneToken", tieneToken
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
    @PreAuthorize(SOLO_ADMIN)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.info("Eliminando configuración {}", id);
        configuracionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Sube un certificado .p12/.pfx (T-030, P-035 parcial). El nombre lo genera el servidor (ambiente, marca de
     * tiempo y UUID); el del cliente solo aporta la extensión. Nunca sobrescribe un archivo existente y la ruta final
     * debe quedar dentro de app.certificados.directorio. El PIN no llega a este endpoint: su validación queda en P-035.
     */
    @PreAuthorize(SOLO_ADMIN)
    @PostMapping("/upload-certificado")
    public ResponseEntity<Map<String, String>> uploadCertificado(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(value = "ambiente", required = false) AmbienteHacienda ambiente) {
        Optional<String> extension = extensionPermitida(archivo.getOriginalFilename());
        if (extension.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Solo se permiten archivos .p12 o .pfx"));
        }
        if (archivo.isEmpty() || archivo.getSize() > tamanoMaximoCertificado) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "El certificado debe tener contenido y no superar " + tamanoMaximoCertificado + " bytes"));
        }
        try {
            Path dir = Path.of(certificadosDir).toAbsolutePath().normalize();
            String prefijo = ambiente != null ? ambiente.name().toLowerCase(Locale.ROOT) : "certificado";
            String nombre = prefijo + "-" + LocalDateTime.now().format(MARCA_TIEMPO) + "-" + UUID.randomUUID()
                    + extension.get();
            Path destino = dir.resolve(nombre).normalize();
            if (!destino.getParent().equals(dir)) {
                log.warn("Ruta de certificado fuera del directorio rechazada: {}", destino);
                return ResponseEntity.badRequest().body(Map.of("error", "Ruta de destino inválida"));
            }
            Files.createDirectories(dir); // idempotent — no-op if already exists
            try (InputStream contenido = archivo.getInputStream()) {
                Files.copy(contenido, destino); // sin REPLACE_EXISTING: falla antes que sobrescribir
            }
            String ruta = destino.toString();
            log.info("Certificado guardado en: {}", ruta);
            return ResponseEntity.ok(Map.of("ruta", ruta));
        } catch (IOException e) {
            log.error("Error al guardar certificado", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "No se pudo guardar el certificado"));
        }
    }

    private static Optional<String> extensionPermitida(String nombreOriginal) {
        if (nombreOriginal == null) {
            return Optional.empty();
        }
        String nombre = nombreOriginal.toLowerCase(Locale.ROOT);
        return EXTENSIONES_CERTIFICADO.stream().filter(nombre::endsWith).findFirst();
    }
}
