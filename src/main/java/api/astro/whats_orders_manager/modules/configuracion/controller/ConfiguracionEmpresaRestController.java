package api.astro.whats_orders_manager.modules.configuracion.controller;

import api.astro.whats_orders_manager.modules.configuracion.dto.CabysBusquedaDTO;
import api.astro.whats_orders_manager.modules.configuracion.dto.ConfiguracionEmpresaDTO;
import api.astro.whats_orders_manager.modules.configuracion.dto.HaciendaConsultaDTO;
import api.astro.whats_orders_manager.modules.configuracion.dto.mapper.ConfiguracionEmpresaMapper;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.service.ConfiguracionEmpresaService;
import api.astro.whats_orders_manager.modules.configuracion.service.HaciendaApiService;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioActividadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.util.Map;

/**
 * ============================================================================
 * CONFIGURACION EMPRESA REST CONTROLLER
 * ERP Orders Manager
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
@RequiredArgsConstructor
public class ConfiguracionEmpresaRestController {

    private final ConfiguracionEmpresaService configuracionEmpresaService;
    private final HaciendaApiService haciendaConsultaService;
    private final ConfiguracionEmpresaMapper mapper;
    private final UsuarioActividadService usuarioActividadService;

    @Value("${app.uploads.directorio:/app/uploads}")
    private String uploadsDir;

    private static final String EMPRESA_SUBDIR = "empresa";
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2 MB
    private static final String[] ALLOWED_IMG_EXT = {".png", ".jpg", ".jpeg", ".gif", ".webp", ".ico"};

    /**
     * Busca códigos CABYS por descripción o palabra clave
     *
     * GET /api/configuracion/empresa/hacienda/cabys/buscar?q={termino}&top={cantidad}
     */
    @GetMapping("/hacienda/cabys/buscar")
    public ResponseEntity<?> buscarCabys(
            @RequestParam("q") String termino,
            @RequestParam(value = "top", defaultValue = "10") Integer top) {
        try {
            log.info("GET /api/configuracion/empresa/hacienda/cabys/buscar?q={}&top={}", termino, top);

            CabysBusquedaDTO resultado = haciendaConsultaService.buscarCabys(termino, top);

            if (resultado.getExitosa() && resultado.tieneResultados()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", resultado
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                        "success", false,
                        "message", resultado.getMensajeError() != null ?
                            resultado.getMensajeError() : "No se encontraron resultados"
                    ));
            }

        } catch (Exception e) {
            log.error("Error al buscar códigos CABYS", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "message", "Error al buscar códigos CABYS: " + e.getMessage()
                ));
        }
    }

    /**
     * Consulta datos de un contribuyente en la API de Hacienda Costa Rica
     *
     * GET /api/configuracion/empresa/hacienda/consultar/{numeroIdentificacion}
     */
    @GetMapping("/hacienda/consultar/{numeroIdentificacion}")
    public ResponseEntity<?> consultarHacienda(@PathVariable String numeroIdentificacion) {
        try {
            log.info("GET /api/configuracion/empresa/hacienda/consultar/{} - Consultando API Hacienda",
                numeroIdentificacion);

            HaciendaConsultaDTO resultado = haciendaConsultaService.consultarContribuyente(numeroIdentificacion);

            log.info("Resultado de consulta Hacienda: {}", resultado);

            if (resultado.getExitosa()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", resultado
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                        "success", false,
                        "message", resultado.getMensajeError()
                    ));
            }

        } catch (Exception e) {
            log.error("Error al consultar API de Hacienda", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "message", "Error al consultar Hacienda: " + e.getMessage()
                ));
        }
    }

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

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", mapper.toDTO(configuracion)
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

            ConfiguracionEmpresa configuracion = mapper.toEntity(dto);
            ConfiguracionEmpresa guardada = configuracionEmpresaService.saveOrUpdate(configuracion);

            log.info("Configuración de empresa guardada: ID {}", guardada.getIdConfiguracion());
            usuarioActividadService.registrarCambioConfiguracion(
                    "configuracion_empresa", "Configuración de empresa creada/actualizada");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Configuración guardada exitosamente",
                    "data", mapper.toDTO(guardada)
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

            ConfiguracionEmpresa configuracion = mapper.toEntity(dto);
            ConfiguracionEmpresa guardada = configuracionEmpresaService.saveOrUpdate(configuracion);

            log.info("Configuración de empresa actualizada: ID {}", guardada.getIdConfiguracion());
            usuarioActividadService.registrarCambioConfiguracion(
                    "configuracion_empresa", "Configuración de empresa actualizada");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Configuración actualizada exitosamente",
                    "data", mapper.toDTO(guardada)
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

    // ==================== LOGO ====================

    /**
     * POST /api/configuracion/empresa/logo
     * Multipart: logo (image file)
     */
    @PostMapping("/logo")
    public ResponseEntity<?> subirLogo(@RequestParam("logo") MultipartFile logo) {
        try {
            validarImagenCE(logo);
            ConfiguracionEmpresa config = configuracionEmpresaService.obtenerOCrearConfiguracion();
            if (config.getLogoPath() != null) eliminarArchivoCE(config.getLogoPath());
            String nombre = guardarArchivoCE(logo, "logo_" + config.getIdConfiguracion());
            config.setLogoPath(nombre);
            configuracionEmpresaService.saveOrUpdate(config);
            return ResponseEntity.ok(Map.of("success", true, "message", "Logo uploaded",
                    "data", Map.of("logoPath", nombre)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error al subir logo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * DELETE /api/configuracion/empresa/logo
     * Clears logoPath and deletes the physical file.
     */
    @DeleteMapping("/logo")
    public ResponseEntity<?> eliminarLogo() {
        try {
            ConfiguracionEmpresa config = configuracionEmpresaService.obtenerOCrearConfiguracion();
            if (config.getLogoPath() != null) {
                eliminarArchivoCE(config.getLogoPath());
                config.setLogoPath(null);
                configuracionEmpresaService.saveOrUpdate(config);
            }
            return ResponseEntity.ok(Map.of("success", true, "message", "Logo removed"));
        } catch (Exception e) {
            log.error("Error al eliminar logo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ==================== FAVICON ====================

    /**
     * POST /api/configuracion/empresa/favicon
     * Multipart: favicon (image/ico file)
     */
    @PostMapping("/favicon")
    public ResponseEntity<?> subirFavicon(@RequestParam("favicon") MultipartFile favicon) {
        try {
            validarImagenCE(favicon);
            ConfiguracionEmpresa config = configuracionEmpresaService.obtenerOCrearConfiguracion();
            if (config.getFaviconPath() != null) eliminarArchivoCE(config.getFaviconPath());
            String nombre = guardarArchivoCE(favicon, "favicon_" + config.getIdConfiguracion());
            config.setFaviconPath(nombre);
            configuracionEmpresaService.saveOrUpdate(config);
            return ResponseEntity.ok(Map.of("success", true, "message", "Favicon uploaded",
                    "data", Map.of("faviconPath", nombre)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error al subir favicon", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * DELETE /api/configuracion/empresa/favicon
     * Clears faviconPath and deletes the physical file.
     */
    @DeleteMapping("/favicon")
    public ResponseEntity<?> eliminarFavicon() {
        try {
            ConfiguracionEmpresa config = configuracionEmpresaService.obtenerOCrearConfiguracion();
            if (config.getFaviconPath() != null) {
                eliminarArchivoCE(config.getFaviconPath());
                config.setFaviconPath(null);
                configuracionEmpresaService.saveOrUpdate(config);
            }
            return ResponseEntity.ok(Map.of("success", true, "message", "Favicon removed"));
        } catch (Exception e) {
            log.error("Error al eliminar favicon", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ==================== ASSETS (serve uploaded files) ====================

    /**
     * GET /api/configuracion/empresa/assets/{subdir}/{filename}
     * Serves uploaded files (logo, favicon). Protected by @PreAuthorize on the class.
     */
    @GetMapping("/assets/{subdir}/{filename:.+}")
    public ResponseEntity<org.springframework.core.io.Resource> servirAsset(
            @PathVariable String subdir,
            @PathVariable String filename) {
        if (subdir.contains("..") || filename.contains("..")) {
            return ResponseEntity.badRequest().build();
        }
        try {
            Path filePath = Paths.get(uploadsDir, subdir, filename);
            org.springframework.core.io.Resource resource =
                    new org.springframework.core.io.UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) contentType = "application/octet-stream";
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (Exception e) {
            log.error("Error sirviendo asset {}/{}", subdir, filename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== PRIVATE HELPERS ====================

    private void validarImagenCE(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("File cannot be empty");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("File exceeds 2 MB limit");
        String nombre = file.getOriginalFilename();
        if (nombre == null) throw new IllegalArgumentException("Invalid filename");
        boolean ok = false;
        for (String ext : ALLOWED_IMG_EXT) {
            if (nombre.toLowerCase().endsWith(ext)) { ok = true; break; }
        }
        if (!ok) throw new IllegalArgumentException("Extension not allowed. Allowed: .png .jpg .jpeg .gif .webp .ico");
    }

    private String guardarArchivoCE(MultipartFile file, String prefijo) throws IOException {
        Path dir = Paths.get(uploadsDir, EMPRESA_SUBDIR);
        Files.createDirectories(dir);
        String ext = getExtCE(file.getOriginalFilename());
        String nombre = prefijo + ext;
        Files.copy(file.getInputStream(), dir.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);
        log.info("Asset guardado: {}", dir.resolve(nombre).toAbsolutePath());
        return nombre;
    }

    private void eliminarArchivoCE(String nombre) throws IOException {
        if (nombre == null || nombre.isEmpty()) return;
        Path p = Paths.get(uploadsDir, EMPRESA_SUBDIR, nombre);
        if (Files.exists(p)) {
            Files.delete(p);
            log.info("Asset eliminado: {}", p.toAbsolutePath());
        }
    }

    private String getExtCE(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot == -1 ? "" : filename.substring(dot).toLowerCase();
    }
}
