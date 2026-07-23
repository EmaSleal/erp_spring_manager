package api.astro.whats_orders_manager.modules.rrhh.controller;

import api.astro.whats_orders_manager.modules.rrhh.dto.DepartamentoDTO;
import api.astro.whats_orders_manager.modules.rrhh.model.Departamento;
import api.astro.whats_orders_manager.modules.rrhh.service.DepartamentoService;
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
@RequestMapping("/api/rrhh/departamentos")
@RequiredArgsConstructor
public class DepartamentoRestController {

    private final DepartamentoService departamentoService;

    @GetMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_VER')")
    public ResponseEntity<ResponseDTO> listar(
            @RequestParam(defaultValue = "false") boolean soloActivos,
            Authentication authentication) {
        List<Departamento> lista = soloActivos
                ? departamentoService.findActivos()
                : departamentoService.findAll();
        return ResponseEntity.ok(ResponseDTO.success("OK", lista));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_VER')")
    public ResponseEntity<ResponseDTO> findById(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            return ResponseEntity.ok(ResponseDTO.success("OK", departamentoService.findById(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_GESTIONAR_EMPLEADOS')")
    public ResponseEntity<ResponseDTO> crear(
            @RequestBody DepartamentoDTO dto,
            Authentication authentication) {
        try {
            Departamento saved = departamentoService.crear(dto);
            return ResponseEntity.ok(ResponseDTO.success("Departamento creado exitosamente", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_GESTIONAR_EMPLEADOS')")
    public ResponseEntity<ResponseDTO> actualizar(
            @PathVariable Long id,
            @RequestBody DepartamentoDTO dto,
            Authentication authentication) {
        try {
            Departamento saved = departamentoService.actualizar(id, dto);
            return ResponseEntity.ok(ResponseDTO.success("Departamento actualizado exitosamente", saved));
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
            departamentoService.desactivar(id);
            return ResponseEntity.ok(ResponseDTO.success("Departamento desactivado exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }
}
