package api.astro.whats_orders_manager.modules.rrhh.controller;

import api.astro.whats_orders_manager.modules.rrhh.dto.EmpleadoDTO;
import api.astro.whats_orders_manager.modules.rrhh.model.Empleado;
import api.astro.whats_orders_manager.modules.rrhh.service.EmpleadoService;
import api.astro.whats_orders_manager.shared.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/rrhh/empleados")
@RequiredArgsConstructor
public class EmpleadoRestController {

    private final EmpleadoService empleadoService;

    @PostMapping("/guardar")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_GESTIONAR_EMPLEADOS')")
    public ResponseEntity<ResponseDTO> guardar(
            @RequestBody EmpleadoDTO dto,
            Authentication authentication) {
        try {
            Empleado saved = empleadoService.crear(dto);
            return ResponseEntity.ok(
                    ResponseDTO.success("Empleado creado exitosamente", saved.getId()));
        } catch (Exception e) {
            log.warn("Error al crear empleado: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/actualizar")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_GESTIONAR_EMPLEADOS')")
    public ResponseEntity<ResponseDTO> actualizar(
            @PathVariable Long id,
            @RequestBody EmpleadoDTO dto,
            Authentication authentication) {
        try {
            empleadoService.actualizar(id, dto);
            return ResponseEntity.ok(ResponseDTO.success("Empleado actualizado exitosamente"));
        } catch (Exception e) {
            log.warn("Error al actualizar empleado {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/baja")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_GESTIONAR_EMPLEADOS')")
    public ResponseEntity<ResponseDTO> darDeBaja(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        try {
            String fechaStr = body.get("fechaSalida");
            String motivo = body.get("motivo");

            LocalDate fecha = (fechaStr != null && !fechaStr.isBlank())
                    ? LocalDate.parse(fechaStr)
                    : LocalDate.now();

            empleadoService.darDeBaja(id, fecha, motivo);
            return ResponseEntity.ok(ResponseDTO.success("Empleado dado de baja exitosamente"));
        } catch (Exception e) {
            log.warn("Error al dar de baja empleado {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }
}
