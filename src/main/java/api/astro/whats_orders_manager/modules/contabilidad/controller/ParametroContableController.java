package api.astro.whats_orders_manager.modules.contabilidad.controller;

import api.astro.whats_orders_manager.modules.contabilidad.model.ParametroContable;
import api.astro.whats_orders_manager.modules.contabilidad.service.ParametroContableService;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import api.astro.whats_orders_manager.shared.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles both the Thymeleaf view and REST CRUD for ParametroContable.
 *
 * View:  GET  /contabilidad/parametros
 * REST:  GET/POST/PUT/DELETE  /api/contabilidad/parametros[/{id}]
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ParametroContableController {

    private final ParametroContableService parametroService;
    private final PermisoService permisoService;

    // ── View ──────────────────────────────────────────────────────────────────

    @GetMapping("/contabilidad/parametros")
    public String listar(Model model, Authentication authentication) {
        if (!permisoService.tienePermisoPorCodigo(authentication.getName(), "CONTABILIDAD_VER")) {
            return "redirect:/acceso-denegado";
        }
        model.addAttribute("parametros", parametroService.listar());
        return "modules/contabilidad/parametros/listar";
    }

    // ── REST: list ────────────────────────────────────────────────────────────

    @GetMapping("/api/contabilidad/parametros")
    @ResponseBody
    public ResponseEntity<ResponseDTO> listarApi(Authentication authentication) {
        if (!permisoService.tienePermisoPorCodigo(authentication.getName(), "CONTABILIDAD_VER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseDTO.error("Sin permiso (CONTABILIDAD_VER)"));
        }
        try {
            List<Map<String, Object>> data = parametroService.listar().stream()
                    .map(this::toMap)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ResponseDTO.success("OK", data));
        } catch (Exception e) {
            log.warn("Error listing parametros contables: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDTO.error(e.getMessage()));
        }
    }

    // ── REST: create ──────────────────────────────────────────────────────────

    @PostMapping("/api/contabilidad/parametros")
    @ResponseBody
    public ResponseEntity<ResponseDTO> crear(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {

        if (!permisoService.tienePermisoPorCodigo(authentication.getName(), "CONTABILIDAD_EDITAR")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseDTO.error("Sin permiso (CONTABILIDAD_EDITAR)"));
        }
        try {
            String clave = (String) body.get("clave");
            String descripcion = (String) body.get("descripcion");
            Long cuentaContableId = parseLong(body.get("cuentaContableId"));

            ParametroContable created = parametroService.crear(clave, descripcion, cuentaContableId);
            return ResponseEntity.ok(ResponseDTO.success("Parámetro creado exitosamente", toMap(created)));
        } catch (IllegalArgumentException e) {
            log.warn("Validation error creating parametro: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            log.warn("Error creating parametro: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDTO.error(e.getMessage()));
        }
    }

    // ── REST: update ──────────────────────────────────────────────────────────

    @PutMapping("/api/contabilidad/parametros/{id}")
    @ResponseBody
    public ResponseEntity<ResponseDTO> actualizar(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication authentication) {

        if (!permisoService.tienePermisoPorCodigo(authentication.getName(), "CONTABILIDAD_EDITAR")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseDTO.error("Sin permiso (CONTABILIDAD_EDITAR)"));
        }
        try {
            String clave = (String) body.get("clave");
            String descripcion = (String) body.get("descripcion");
            Long cuentaContableId = parseLong(body.get("cuentaContableId"));

            ParametroContable updated = parametroService.actualizar(id, clave, descripcion, cuentaContableId);
            return ResponseEntity.ok(ResponseDTO.success("Parámetro actualizado exitosamente", toMap(updated)));
        } catch (IllegalArgumentException e) {
            log.warn("Validation error updating parametro {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            log.warn("Error updating parametro {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDTO.error(e.getMessage()));
        }
    }

    // ── REST: delete ──────────────────────────────────────────────────────────

    @DeleteMapping("/api/contabilidad/parametros/{id}")
    @ResponseBody
    public ResponseEntity<ResponseDTO> eliminar(
            @PathVariable Long id,
            Authentication authentication) {

        if (!permisoService.tienePermisoPorCodigo(authentication.getName(), "CONTABILIDAD_EDITAR")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseDTO.error("Sin permiso (CONTABILIDAD_EDITAR)"));
        }
        try {
            parametroService.eliminar(id);
            return ResponseEntity.ok(ResponseDTO.success("Parámetro eliminado"));
        } catch (IllegalArgumentException e) {
            log.warn("Validation error deleting parametro {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            log.warn("Error deleting parametro {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDTO.error(e.getMessage()));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Map<String, Object> toMap(ParametroContable p) {
        return Map.of(
                "id", p.getId(),
                "clave", p.getClave(),
                "descripcion", p.getDescripcion() != null ? p.getDescripcion() : "",
                "cuentaContableId", p.getCuentaContable().getIdCuenta(),
                "cuentaCodigo", p.getCuentaContable().getCodigo(),
                "cuentaNombre", p.getCuentaContable().getNombre()
        );
    }

    private Long parseLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }
}
