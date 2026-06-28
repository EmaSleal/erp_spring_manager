package api.astro.whats_orders_manager.modules.configuracion.controller;

import api.astro.whats_orders_manager.modules.configuracion.model.Presentacion;
import api.astro.whats_orders_manager.modules.configuracion.service.PresentacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/configuracion/presentaciones")
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasRole('VENDEDOR')")
@RequiredArgsConstructor
public class PresentacionRestController {

    private final PresentacionService presentacionService;

    @GetMapping
    public ResponseEntity<?> findAll() {
        try {
            log.info("GET /api/configuracion/presentaciones");
            List<Presentacion> presentaciones = presentacionService.findAll();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", presentaciones,
                    "total", presentaciones.size()
            ));
        } catch (Exception e) {
            log.error("Error al obtener presentaciones", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable Integer id) {
        try {
            log.info("GET /api/configuracion/presentaciones/{}", id);
            return presentacionService.findById(id)
                    .map(p -> ResponseEntity.ok(Map.of("success", true, "data", p)))
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("success", false, "message", "Presentacion not found: " + id)));
        } catch (Exception e) {
            log.error("Error al obtener presentacion {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Presentacion presentacion) {
        try {
            log.info("POST /api/configuracion/presentaciones - nombre: {}", presentacion.getNombre());
            Presentacion saved = presentacionService.save(presentacion);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("success", true, "data", saved));
        } catch (Exception e) {
            log.error("Error al crear presentacion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody Presentacion presentacion) {
        try {
            log.info("PUT /api/configuracion/presentaciones/{}", id);
            if (presentacionService.findById(id).isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Presentacion not found: " + id));
            }
            presentacion.setIdPresentacion(id);
            Presentacion updated = presentacionService.save(presentacion);
            return ResponseEntity.ok(Map.of("success", true, "data", updated));
        } catch (Exception e) {
            log.error("Error al actualizar presentacion {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteById(@PathVariable Integer id) {
        try {
            log.info("DELETE /api/configuracion/presentaciones/{}", id);
            if (presentacionService.findById(id).isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Presentacion not found: " + id));
            }
            presentacionService.deleteById(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Presentacion deleted"));
        } catch (Exception e) {
            log.error("Error al eliminar presentacion {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
