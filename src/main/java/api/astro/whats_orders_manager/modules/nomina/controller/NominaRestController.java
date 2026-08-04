package api.astro.whats_orders_manager.modules.nomina.controller;

import api.astro.whats_orders_manager.modules.nomina.dto.NominaDTO;
import api.astro.whats_orders_manager.modules.nomina.dto.NominaResumenDTO;
import api.astro.whats_orders_manager.modules.nomina.enums.TipoNomina;
import api.astro.whats_orders_manager.modules.nomina.service.NominaService;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import api.astro.whats_orders_manager.shared.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST controller for payroll (Nómina) lifecycle endpoints.
 *
 * Permission model: each action requires the corresponding NOMINA_* permission via
 * {@code permisoService.tienePermisoPorCodigo} — mirrors the pattern used in
 * {@code AusenciaRestController} and other module controllers.
 *
 * Error handling: all exceptions are caught and returned as {@link ResponseDTO#error}
 * with HTTP 400, except missing permissions which return HTTP 403.
 */
@Slf4j
@RestController
@RequestMapping("/api/nomina")
@RequiredArgsConstructor
public class NominaRestController {

    private final NominaService nominaService;
    private final PermisoService permisoService;

    // ── POST /api/nomina ──────────────────────────────────────────────────────

    /**
     * Creates a new payroll run in BORRADOR state.
     *
     * <p>Request body: {@code { "periodoInicio": "2026-07-01", "periodoFin": "2026-07-31",
     * "fechaPago": "2026-08-05", "tipo": "MENSUAL" }}
     *
     * @permission NOMINA_CREAR
     */
    @PostMapping
    public ResponseEntity<ResponseDTO> crear(
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        if (!permisoService.tienePermisoPorCodigo(authentication.getName(), "NOMINA_CREAR")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseDTO.error("No tiene permiso para crear nóminas (NOMINA_CREAR)"));
        }

        try {
            LocalDate periodoInicio = LocalDate.parse(body.get("periodoInicio"));
            LocalDate periodoFin = LocalDate.parse(body.get("periodoFin"));
            LocalDate fechaPago = LocalDate.parse(body.get("fechaPago"));
            TipoNomina tipo = TipoNomina.valueOf(body.get("tipo"));

            NominaDTO dto = nominaService.crear(periodoInicio, periodoFin, fechaPago, tipo);
            return ResponseEntity.ok(ResponseDTO.success("Nómina creada exitosamente", dto));
        } catch (Exception e) {
            log.warn("Error al crear nómina: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    // ── GET /api/nomina ───────────────────────────────────────────────────────

    /**
     * Lists all payrolls as summary projections.
     *
     * @permission NOMINA_VER
     */
    @GetMapping
    public ResponseEntity<ResponseDTO> listar(Authentication authentication) {
        if (!permisoService.tienePermisoPorCodigo(authentication.getName(), "NOMINA_VER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseDTO.error("No tiene permiso para ver nóminas (NOMINA_VER)"));
        }

        try {
            List<NominaResumenDTO> nominas = nominaService.listar();
            return ResponseEntity.ok(ResponseDTO.success("OK", nominas));
        } catch (Exception e) {
            log.warn("Error al listar nóminas: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    // ── GET /api/nomina/{id} ──────────────────────────────────────────────────

    /**
     * Returns a full nomina with detail lines.
     *
     * @permission NOMINA_VER
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO> obtener(
            @PathVariable Long id,
            Authentication authentication) {

        if (!permisoService.tienePermisoPorCodigo(authentication.getName(), "NOMINA_VER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseDTO.error("No tiene permiso para ver nóminas (NOMINA_VER)"));
        }

        try {
            NominaDTO dto = nominaService.obtener(id);
            return ResponseEntity.ok(ResponseDTO.success("OK", dto));
        } catch (Exception e) {
            log.warn("Error al obtener nómina {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    // ── POST /api/nomina/{id}/calcular ────────────────────────────────────────

    /**
     * Executes gross-to-net calculation for all active employees.
     *
     * @permission NOMINA_CALCULAR
     */
    @PostMapping("/{id}/calcular")
    public ResponseEntity<ResponseDTO> calcular(
            @PathVariable Long id,
            Authentication authentication) {

        if (!permisoService.tienePermisoPorCodigo(authentication.getName(), "NOMINA_CALCULAR")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseDTO.error("No tiene permiso para calcular nóminas (NOMINA_CALCULAR)"));
        }

        try {
            NominaDTO dto = nominaService.calcular(id);
            return ResponseEntity.ok(ResponseDTO.success("Nómina calculada exitosamente", dto));
        } catch (Exception e) {
            log.warn("Error al calcular nómina {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    // ── POST /api/nomina/{id}/aprobar ─────────────────────────────────────────

    /**
     * Transitions nomina from CALCULADA to APROBADA.
     *
     * @permission NOMINA_APROBAR
     */
    @PostMapping("/{id}/aprobar")
    public ResponseEntity<ResponseDTO> aprobar(
            @PathVariable Long id,
            Authentication authentication) {

        if (!permisoService.tienePermisoPorCodigo(authentication.getName(), "NOMINA_APROBAR")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseDTO.error("No tiene permiso para aprobar nóminas (NOMINA_APROBAR)"));
        }

        try {
            NominaDTO dto = nominaService.aprobar(id);
            return ResponseEntity.ok(ResponseDTO.success("Nómina aprobada exitosamente", dto));
        } catch (Exception e) {
            log.warn("Error al aprobar nómina {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    // ── POST /api/nomina/{id}/contabilizar ────────────────────────────────────

    /**
     * Validates accounting parameters and transitions nomina from APROBADA to CONTABILIZADA.
     *
     * @permission NOMINA_CONTABILIZAR
     */
    @PostMapping("/{id}/contabilizar")
    public ResponseEntity<ResponseDTO> contabilizar(
            @PathVariable Long id,
            Authentication authentication) {

        if (!permisoService.tienePermisoPorCodigo(authentication.getName(), "NOMINA_CONTABILIZAR")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseDTO.error("No tiene permiso para contabilizar nóminas (NOMINA_CONTABILIZAR)"));
        }

        try {
            NominaDTO dto = nominaService.contabilizar(id);
            return ResponseEntity.ok(ResponseDTO.success("Nómina contabilizada exitosamente", dto));
        } catch (Exception e) {
            log.warn("Error al contabilizar nómina {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    // ── DELETE /api/nomina/{id} ───────────────────────────────────────────────

    /**
     * Cancels (anula) a nomina in BORRADOR or CALCULADA state.
     *
     * <p>Request body: {@code { "motivo": "descripcion del motivo" }}
     *
     * @permission NOMINA_ANULAR
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO> anular(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {

        if (!permisoService.tienePermisoPorCodigo(authentication.getName(), "NOMINA_ANULAR")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseDTO.error("No tiene permiso para anular nóminas (NOMINA_ANULAR)"));
        }

        try {
            String motivo = (body != null) ? body.getOrDefault("motivo", "") : "";
            NominaDTO dto = nominaService.anular(id, motivo);
            return ResponseEntity.ok(ResponseDTO.success("Nómina anulada exitosamente", dto));
        } catch (Exception e) {
            log.warn("Error al anular nómina {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }
}
