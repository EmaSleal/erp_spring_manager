package api.astro.whats_orders_manager.modules.configuracion.controller;

import api.astro.whats_orders_manager.modules.configuracion.model.CuentaBancaria;
import api.astro.whats_orders_manager.modules.configuracion.model.Empresa;
import api.astro.whats_orders_manager.modules.configuracion.service.CuentaBancariaService;
import api.astro.whats_orders_manager.modules.configuracion.service.EmpresaService;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for PDF invoice configuration and bank accounts on Empresa entity.
 */
@RestController
@RequestMapping("/api/empresa")
@Slf4j
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EmpresaRestController {

    private final EmpresaService empresaService;
    private final UsuarioService usuarioService;
    private final CuentaBancariaService cuentaBancariaService;

    @Value("${app.sellos.directorio:/app/sellos}")
    private String sellosDir;
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024;
    private static final String[] ALLOWED_EXTENSIONS = {".png", ".jpg", ".jpeg", ".gif", ".webp"};

    // ==================== PDF CONFIG ====================

    /**
     * GET /api/empresa/pdf-config
     * Returns textoLegal, descripcionActividad, selloTipoEmpresa path and tieneSello flag.
     */
    @GetMapping("/pdf-config")
    public ResponseEntity<?> getPdfConfig() {
        try {
            Empresa empresa = empresaService.getEmpresaPrincipal();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                    "textoLegal",            empresa.getTextoLegal()            != null ? empresa.getTextoLegal()            : "",
                    "descripcionActividad",  empresa.getDescripcionActividad()  != null ? empresa.getDescripcionActividad()  : "",
                    "selloTipoEmpresa",      empresa.getSelloTipoEmpresa()      != null ? empresa.getSelloTipoEmpresa()      : "",
                    "tieneSello",            empresa.tieneSello()
                )
            ));
        } catch (Exception e) {
            log.error("Error al obtener PDF config de empresa", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * PUT /api/empresa/pdf-config
     * Body: { textoLegal, descripcionActividad }
     */
    @PutMapping("/pdf-config")
    public ResponseEntity<?> updatePdfConfig(@RequestBody Map<String, String> body,
                                             Authentication authentication) {
        try {
            Empresa empresa = empresaService.getEmpresaPrincipal();
            empresa.setTextoLegal(body.get("textoLegal"));
            empresa.setDescripcionActividad(body.get("descripcionActividad"));

            Integer usuarioId = resolveUsuarioId(authentication);
            empresaService.saveOrUpdate(empresa, usuarioId);

            return ResponseEntity.ok(Map.of("success", true, "message", "PDF config updated successfully"));
        } catch (Exception e) {
            log.error("Error al actualizar PDF config de empresa", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ==================== SELLO ====================

    /**
     * POST /api/empresa/sello
     * Multipart: sello (image file)
     */
    @PostMapping("/sello")
    public ResponseEntity<?> subirSello(@RequestParam("sello") MultipartFile sello,
                                        Authentication authentication) {
        try {
            validarArchivo(sello);

            Empresa empresa = empresaService.getEmpresaPrincipal();

            // Remove old sello file if present
            if (empresa.getSelloTipoEmpresa() != null) {
                eliminarArchivo(empresa.getSelloTipoEmpresa());
            }

            String nombreArchivo = guardarArchivo(sello, "sello_" + empresa.getIdEmpresa());
            empresa.setSelloTipoEmpresa(nombreArchivo);

            Integer usuarioId = resolveUsuarioId(authentication);
            empresaService.saveOrUpdate(empresa, usuarioId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Sello uploaded successfully",
                "data", Map.of("selloTipoEmpresa", nombreArchivo)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error al subir sello de empresa", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * DELETE /api/empresa/sello
     * Clears selloTipoEmpresa and deletes the physical file.
     */
    @DeleteMapping("/sello")
    public ResponseEntity<?> eliminarSello(Authentication authentication) {
        try {
            Empresa empresa = empresaService.getEmpresaPrincipal();

            if (empresa.getSelloTipoEmpresa() != null) {
                eliminarArchivo(empresa.getSelloTipoEmpresa());
                empresa.setSelloTipoEmpresa(null);
                Integer usuarioId = resolveUsuarioId(authentication);
                empresaService.saveOrUpdate(empresa, usuarioId);
            }

            return ResponseEntity.ok(Map.of("success", true, "message", "Sello removed successfully"));
        } catch (Exception e) {
            log.error("Error al eliminar sello de empresa", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ==================== CUENTAS BANCARIAS ====================

    /**
     * GET /api/empresa/cuentas-bancarias[?incluirInactivas=true]
     * Bank accounts of the main empresa (active only by default) with their accounting leaf (ADR-018).
     */
    @GetMapping("/cuentas-bancarias")
    public ResponseEntity<?> getCuentasBancarias(
            @RequestParam(name = "incluirInactivas", defaultValue = "false") boolean incluirInactivas) {
        try {
            List<Map<String, Object>> cuentas = cuentaBancariaService.listar(incluirInactivas)
                .stream()
                .map(this::cuentaToMap)
                .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("success", true, "data", cuentas));
        } catch (Exception e) {
            log.error("Error al obtener cuentas bancarias", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", mensaje(e)));
        }
    }

    /**
     * POST /api/empresa/cuentas-bancarias
     * Body: { entidad, cuentaIban, cuentaBanco, moneda }
     * Creates the bank account and its accounting leaf under 1.1.01.002 in one transaction (ADR-018).
     */
    @PostMapping("/cuentas-bancarias")
    public ResponseEntity<?> crearCuenta(@RequestBody Map<String, String> body,
                                         Authentication authentication) {
        try {
            CuentaBancaria guardada = cuentaBancariaService.crear(
                body.get("entidad"), body.get("cuentaIban"), body.get("cuentaBanco"), body.get("moneda"));

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Cuenta bancaria creada con su cuenta contable "
                    + guardada.getCuentaContable().getCodigo(),
                "data", cuentaToMap(guardada)
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return rechazo(e);
        } catch (Exception e) {
            log.error("Error al crear cuenta bancaria", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", mensaje(e)));
        }
    }

    /**
     * PUT /api/empresa/cuentas-bancarias/{id}
     * Body: { entidad, cuentaIban, cuentaBanco, moneda }
     * Renames the accounting leaf; a currency change is rejected if the leaf has movements (ADR-018).
     */
    @PutMapping("/cuentas-bancarias/{id}")
    public ResponseEntity<?> actualizarCuenta(@PathVariable Integer id,
                                              @RequestBody Map<String, String> body,
                                              Authentication authentication) {
        try {
            CuentaBancaria guardada = cuentaBancariaService.actualizar(id,
                body.get("entidad"), body.get("cuentaIban"), body.get("cuentaBanco"), body.get("moneda"));

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Cuenta bancaria actualizada",
                "data", cuentaToMap(guardada)
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return rechazo(e);
        } catch (Exception e) {
            log.error("Error al actualizar cuenta bancaria {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", mensaje(e)));
        }
    }

    /**
     * PATCH /api/empresa/cuentas-bancarias/{id}/estado
     * Body: { activa: true|false }
     * Activates/deactivates the bank account together with its accounting leaf (ADR-018).
     */
    @PatchMapping("/cuentas-bancarias/{id}/estado")
    public ResponseEntity<?> cambiarEstadoCuenta(@PathVariable Integer id,
                                                 @RequestBody Map<String, Object> body,
                                                 Authentication authentication) {
        try {
            Object activa = body.get("activa");
            if (activa == null) {
                throw new IllegalArgumentException("Indique 'activa' (true o false)");
            }
            boolean nuevaActiva = Boolean.parseBoolean(activa.toString());
            CuentaBancaria guardada = cuentaBancariaService.cambiarEstado(id, nuevaActiva);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", nuevaActiva ? "Cuenta bancaria activada" : "Cuenta bancaria desactivada",
                "data", cuentaToMap(guardada)
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return rechazo(e);
        } catch (Exception e) {
            log.error("Error al cambiar estado de cuenta bancaria {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", mensaje(e)));
        }
    }

    /**
     * DELETE /api/empresa/cuentas-bancarias/{id}
     * Deletes the bank account and its accounting leaf. Rejected (400) if a cobro or pago a proveedor
     * uses it or its leaf has movements: it must be deactivated instead (ADR-018).
     */
    @DeleteMapping("/cuentas-bancarias/{id}")
    public ResponseEntity<?> eliminarCuenta(@PathVariable Integer id,
                                            Authentication authentication) {
        try {
            cuentaBancariaService.eliminar(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Cuenta bancaria eliminada"));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return rechazo(e);
        } catch (Exception e) {
            log.error("Error al eliminar cuenta bancaria {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", mensaje(e)));
        }
    }

    // ==================== ASSETS ====================

    /**
     * GET /api/empresa/assets/{filename}
     * Serves uploaded sello files from sellosDir.
     */
    @GetMapping("/assets/{filename:.+}")
    public ResponseEntity<org.springframework.core.io.Resource> servirAsset(
            @PathVariable String filename) {
        if (filename.contains("..")) {
            return ResponseEntity.badRequest().build();
        }
        try {
            Path filePath = Paths.get(sellosDir, filename);
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
            log.error("Error sirviendo asset {}", filename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== PRIVATE HELPERS ====================

    private Integer resolveUsuarioId(Authentication authentication) {
        if (authentication == null) return null;
        return usuarioService.findByEmail(authentication.getName())
            .map(Usuario::getIdUsuario)
            .orElse(null);
    }

    private Map<String, Object> cuentaToMap(CuentaBancaria c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("idCuentaBancaria", c.getIdCuentaBancaria());
        m.put("entidad",          c.getEntidad()     != null ? c.getEntidad()     : "");
        m.put("cuentaIban",       c.getCuentaIban()  != null ? c.getCuentaIban()  : "");
        m.put("cuentaBanco",      c.getCuentaBanco() != null ? c.getCuentaBanco() : "");
        m.put("moneda",           c.getMoneda()      != null ? c.getMoneda()      : "");
        m.put("activa",           c.getActiva()      != null ? c.getActiva()      : true);
        m.put("orden",            c.getOrden()       != null ? c.getOrden()       : 0);
        CuentaContable hoja = c.getCuentaContable();
        m.put("codigoCuentaContable", hoja != null ? hoja.getCodigo() : "");
        m.put("nombreCuentaContable", hoja != null ? hoja.getNombre() : "");
        return m;
    }

    private ResponseEntity<?> rechazo(RuntimeException e) {
        log.warn("Cuenta bancaria rechazada: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("success", false, "message", mensaje(e)));
    }

    private static String mensaje(Exception e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }

    private void validarArchivo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File exceeds 2 MB limit");
        }
        String nombre = file.getOriginalFilename();
        if (nombre == null) {
            throw new IllegalArgumentException("Invalid filename");
        }
        boolean valida = false;
        for (String ext : ALLOWED_EXTENSIONS) {
            if (nombre.toLowerCase().endsWith(ext)) {
                valida = true;
                break;
            }
        }
        if (!valida) {
            throw new IllegalArgumentException("Invalid extension. Allowed: .png, .jpg, .jpeg, .gif, .webp");
        }
    }

    private String guardarArchivo(MultipartFile file, String prefijo) throws IOException {
        Path dirPath = Paths.get(sellosDir);
        Files.createDirectories(dirPath);
        String extension = getExtension(file.getOriginalFilename());
        String nombreArchivo = prefijo + extension;
        Path destino = dirPath.resolve(nombreArchivo);
        Files.copy(file.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
        log.info("Sello guardado: {}", destino.toAbsolutePath());
        return nombreArchivo;
    }

    private void eliminarArchivo(String nombreArchivo) throws IOException {
        if (nombreArchivo == null || nombreArchivo.isEmpty()) return;
        Path path = Paths.get(sellosDir, nombreArchivo);
        if (Files.exists(path)) {
            Files.delete(path);
            log.info("Sello eliminado: {}", path.toAbsolutePath());
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot == -1 ? "" : filename.substring(dot);
    }
}
