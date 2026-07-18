package api.astro.whats_orders_manager.modules.configuracion.controller;

import api.astro.whats_orders_manager.modules.configuracion.model.CuentaBancaria;
import api.astro.whats_orders_manager.modules.configuracion.model.Empresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.CuentaBancariaRepository;
import api.astro.whats_orders_manager.modules.configuracion.service.EmpresaService;
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
    private final CuentaBancariaRepository cuentaBancariaRepository;

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
     * GET /api/empresa/cuentas-bancarias
     * Returns active bank accounts for the main empresa.
     */
    @GetMapping("/cuentas-bancarias")
    public ResponseEntity<?> getCuentasBancarias() {
        try {
            Empresa empresa = empresaService.getEmpresaPrincipal();
            List<Map<String, Object>> cuentas = cuentaBancariaRepository
                .findByEmpresaIdEmpresaAndActivaTrue(empresa.getIdEmpresa())
                .stream()
                .map(this::cuentaToMap)
                .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("success", true, "data", cuentas));
        } catch (Exception e) {
            log.error("Error al obtener cuentas bancarias", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * POST /api/empresa/cuentas-bancarias
     * Body: { entidad, cuentaIban, cuentaBanco, moneda }
     */
    @PostMapping("/cuentas-bancarias")
    public ResponseEntity<?> crearCuenta(@RequestBody Map<String, String> body,
                                         Authentication authentication) {
        try {
            Empresa empresa = empresaService.getEmpresaPrincipal();

            CuentaBancaria cuenta = new CuentaBancaria();
            cuenta.setEmpresa(empresa);
            cuenta.setEntidad(body.get("entidad"));
            cuenta.setCuentaIban(body.get("cuentaIban"));
            cuenta.setCuentaBanco(body.get("cuentaBanco"));
            cuenta.setMoneda(body.get("moneda"));
            cuenta.setActiva(true);
            cuenta.setOrden(0);

            CuentaBancaria guardada = cuentaBancariaRepository.save(cuenta);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Bank account created successfully",
                "data", cuentaToMap(guardada)
            ));
        } catch (Exception e) {
            log.error("Error al crear cuenta bancaria", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * PUT /api/empresa/cuentas-bancarias/{id}
     * Body: { entidad, cuentaIban, cuentaBanco, moneda }
     */
    @PutMapping("/cuentas-bancarias/{id}")
    public ResponseEntity<?> actualizarCuenta(@PathVariable Integer id,
                                              @RequestBody Map<String, String> body,
                                              Authentication authentication) {
        try {
            CuentaBancaria cuenta = cuentaBancariaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bank account not found: " + id));

            cuenta.setEntidad(body.get("entidad"));
            cuenta.setCuentaIban(body.get("cuentaIban"));
            cuenta.setCuentaBanco(body.get("cuentaBanco"));
            cuenta.setMoneda(body.get("moneda"));

            CuentaBancaria guardada = cuentaBancariaRepository.save(cuenta);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Bank account updated successfully",
                "data", cuentaToMap(guardada)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error al actualizar cuenta bancaria {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * DELETE /api/empresa/cuentas-bancarias/{id}
     * Logical delete (activa = false).
     */
    @DeleteMapping("/cuentas-bancarias/{id}")
    public ResponseEntity<?> eliminarCuenta(@PathVariable Integer id,
                                            Authentication authentication) {
        try {
            CuentaBancaria cuenta = cuentaBancariaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bank account not found: " + id));

            cuenta.setActiva(false);
            cuentaBancariaRepository.save(cuenta);

            return ResponseEntity.ok(Map.of("success", true, "message", "Bank account removed successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error al eliminar cuenta bancaria {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
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
        return Map.of(
            "idCuentaBancaria", c.getIdCuentaBancaria(),
            "entidad",          c.getEntidad()     != null ? c.getEntidad()     : "",
            "cuentaIban",       c.getCuentaIban()  != null ? c.getCuentaIban()  : "",
            "cuentaBanco",      c.getCuentaBanco() != null ? c.getCuentaBanco() : "",
            "moneda",           c.getMoneda()      != null ? c.getMoneda()      : "",
            "activa",           c.getActiva()      != null ? c.getActiva()      : true,
            "orden",            c.getOrden()       != null ? c.getOrden()       : 0
        );
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
