package api.astro.whats_orders_manager.modules.rrhh.controller;

import api.astro.whats_orders_manager.modules.rrhh.dto.AusenciaDTO;
import api.astro.whats_orders_manager.modules.rrhh.model.Ausencia;
import api.astro.whats_orders_manager.modules.rrhh.model.Empleado;
import api.astro.whats_orders_manager.modules.rrhh.repository.AusenciaRepository;
import api.astro.whats_orders_manager.modules.rrhh.repository.EmpleadoRepository;
import api.astro.whats_orders_manager.modules.rrhh.service.AusenciaService;
import api.astro.whats_orders_manager.shared.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/rrhh/ausencias")
@RequiredArgsConstructor
public class AusenciaRestController {

    private final AusenciaService ausenciaService;
    private final AusenciaRepository ausenciaRepository;
    private final EmpleadoRepository empleadoRepository;

    @PostMapping("/registrar")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_GESTIONAR_AUSENCIAS')")
    public ResponseEntity<ResponseDTO> registrar(
            @RequestBody AusenciaDTO dto,
            Authentication authentication) {
        try {
            Ausencia saved = ausenciaService.registrar(dto);
            return ResponseEntity.ok(
                    ResponseDTO.success("Ausencia registrada exitosamente", saved.getId()));
        } catch (Exception e) {
            log.warn("Error al registrar ausencia: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/aprobar")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_APROBAR_AUSENCIAS')")
    public ResponseEntity<ResponseDTO> aprobar(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body,
            Authentication authentication) {
        try {
            Integer usuarioId = body.get("usuarioAprobadorId");
            ausenciaService.aprobar(id, usuarioId);
            return ResponseEntity.ok(ResponseDTO.success("Ausencia aprobada exitosamente"));
        } catch (Exception e) {
            log.warn("Error al aprobar ausencia {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/aprobar-jefe")
    public ResponseEntity<ResponseDTO> aprobarComoJefe(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            Empleado jefe = empleadoRepository.findByUsuarioEmail(authentication.getName())
                    .orElse(null);
            if (jefe == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ResponseDTO.error("No tenés un empleado vinculado a tu usuario."));
            }

            Ausencia ausencia = ausenciaRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Ausencia no encontrada"));

            var dept = ausencia.getEmpleado().getDepartamento();
            if (dept == null || dept.getJefe() == null || !dept.getJefe().getId().equals(jefe.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ResponseDTO.error("No sos el jefe del departamento de este empleado."));
            }

            ausenciaService.aprobar(id, jefe.getUsuario().getIdUsuario());
            return ResponseEntity.ok(ResponseDTO.success("Ausencia aprobada exitosamente"));
        } catch (Exception e) {
            log.warn("Error al aprobar ausencia como jefe {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_VER')")
    public ResponseEntity<ResponseDTO> listarTodos(Authentication authentication) {
        try {
            List<Ausencia> ausencias = ausenciaService.findAll();
            return ResponseEntity.ok(ResponseDTO.success("OK", ausencias));
        } catch (Exception e) {
            log.warn("Error al listar todas las ausencias: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    @GetMapping("/empleado/{empleadoId}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_VER')")
    public ResponseEntity<ResponseDTO> listarPorEmpleado(
            @PathVariable Long empleadoId,
            Authentication authentication) {
        try {
            List<Ausencia> ausencias = ausenciaService.findByEmpleado(empleadoId);
            return ResponseEntity.ok(ResponseDTO.success("OK", ausencias));
        } catch (Exception e) {
            log.warn("Error al listar ausencias del empleado {}: {}", empleadoId, e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }
}
