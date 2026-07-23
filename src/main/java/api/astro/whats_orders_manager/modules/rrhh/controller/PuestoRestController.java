package api.astro.whats_orders_manager.modules.rrhh.controller;

import api.astro.whats_orders_manager.modules.rrhh.dto.PuestoDTO;
import api.astro.whats_orders_manager.modules.rrhh.model.Puesto;
import api.astro.whats_orders_manager.modules.rrhh.service.PuestoService;
import api.astro.whats_orders_manager.shared.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/rrhh/puestos")
@RequiredArgsConstructor
public class PuestoRestController {

    private final PuestoService puestoService;

    /**
     * GET /api/rrhh/puestos — list all, or filter by departamentoId.
     * Used by the cascade select in the empleado form (PR3).
     */
    @GetMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_VER')")
    public ResponseEntity<ResponseDTO> listar(
            @RequestParam(required = false) Long departamentoId,
            Authentication authentication) {
        List<Puesto> lista = departamentoId != null
                ? puestoService.findByDepartamento(departamentoId)
                : puestoService.findAll();
        return ResponseEntity.ok(ResponseDTO.success("OK", lista));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_VER')")
    public ResponseEntity<ResponseDTO> findById(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            return ResponseEntity.ok(ResponseDTO.success("OK", puestoService.findById(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_GESTIONAR_EMPLEADOS')")
    public ResponseEntity<ResponseDTO> crear(
            @RequestBody PuestoDTO dto,
            Authentication authentication) {
        try {
            Puesto saved = puestoService.crear(dto);
            return ResponseEntity.ok(ResponseDTO.success("Puesto creado exitosamente", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_GESTIONAR_EMPLEADOS')")
    public ResponseEntity<ResponseDTO> actualizar(
            @PathVariable Long id,
            @RequestBody PuestoDTO dto,
            Authentication authentication) {
        try {
            Puesto saved = puestoService.actualizar(id, dto);
            return ResponseEntity.ok(ResponseDTO.success("Puesto actualizado exitosamente", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_GESTIONAR_EMPLEADOS')")
    public ResponseEntity<ResponseDTO> desactivar(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            puestoService.desactivar(id);
            return ResponseEntity.ok(ResponseDTO.success("Puesto desactivado exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }
}
