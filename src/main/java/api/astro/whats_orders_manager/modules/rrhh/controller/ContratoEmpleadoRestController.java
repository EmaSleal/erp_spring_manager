package api.astro.whats_orders_manager.modules.rrhh.controller;

import api.astro.whats_orders_manager.modules.rrhh.dto.ContratoEmpleadoDTO;
import api.astro.whats_orders_manager.modules.rrhh.enums.CausaTerminacion;
import api.astro.whats_orders_manager.modules.rrhh.model.ContratoEmpleado;
import api.astro.whats_orders_manager.modules.rrhh.service.ContratoEmpleadoService;
import api.astro.whats_orders_manager.shared.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/rrhh/contratos")
@RequiredArgsConstructor
public class ContratoEmpleadoRestController {

    private final ContratoEmpleadoService contratoService;

    @PostMapping("/guardar")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_GESTIONAR_CONTRATOS')")
    public ResponseEntity<ResponseDTO> guardar(
            @RequestBody ContratoEmpleadoDTO dto,
            Authentication authentication) {
        try {
            ContratoEmpleado saved = contratoService.crear(dto);
            return ResponseEntity.ok(
                    ResponseDTO.success("Contrato creado exitosamente", saved.getId()));
        } catch (Exception e) {
            log.warn("Error al crear contrato: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/terminar")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_GESTIONAR_CONTRATOS')")
    public ResponseEntity<ResponseDTO> terminar(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        try {
            CausaTerminacion causa = CausaTerminacion.valueOf(body.get("causa"));
            LocalDate fecha = LocalDate.parse(body.get("fechaTerminacion"));
            String descripcion = body.get("descripcion");

            contratoService.terminar(id, causa, fecha, descripcion);
            return ResponseEntity.ok(ResponseDTO.success("Contrato terminado exitosamente"));
        } catch (Exception e) {
            log.warn("Error al terminar contrato {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_VER')")
    public ResponseEntity<ResponseDTO> listarTodos(Authentication authentication) {
        try {
            List<ContratoEmpleado> contratos = contratoService.findAll();
            return ResponseEntity.ok(ResponseDTO.success("OK", contratos));
        } catch (Exception e) {
            log.warn("Error al listar todos los contratos: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    @GetMapping("/empleado/{empleadoId}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_VER')")
    public ResponseEntity<ResponseDTO> listarPorEmpleado(
            @PathVariable Long empleadoId,
            Authentication authentication) {
        try {
            List<ContratoEmpleado> contratos = contratoService.findByEmpleado(empleadoId);
            return ResponseEntity.ok(ResponseDTO.success("OK", contratos));
        } catch (Exception e) {
            log.warn("Error al listar contratos del empleado {}: {}", empleadoId, e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }
}
