package api.astro.whats_orders_manager.modules.producto.controller;

import api.astro.whats_orders_manager.modules.configuracion.model.Presentacion;
import api.astro.whats_orders_manager.modules.configuracion.service.PresentacionService;
import api.astro.whats_orders_manager.modules.producto.model.ArticuloMaestro;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.service.ArticuloMaestroService;
import api.astro.whats_orders_manager.modules.producto.service.ProductoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for ArticuloMaestro (master article) CRUD and variant management.
 * All endpoints return a Map.of("success", ...) envelope following project conventions.
 *
 * Base URL: /api/articulos
 */
@Slf4j
@RestController
@RequestMapping("/api/articulos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('VENDEDOR')")
public class ArticuloMaestroRestController {

    private final ArticuloMaestroService articuloMaestroService;
    private final ProductoService productoService;
    private final PresentacionService presentacionService;

    // =========================================================================
    // Master CRUD
    // =========================================================================

    /**
     * GET /api/articulos
     * Returns all master articles.
     */
    @GetMapping
    public ResponseEntity<?> listarTodos() {
        try {
            log.info("GET /api/articulos - listando todos los artículos maestro");
            List<ArticuloMaestro> maestros = articuloMaestroService.findAll();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", maestros,
                    "total", maestros.size()
            ));
        } catch (Exception e) {
            log.error("Error al listar artículos maestro", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * GET /api/articulos/{id}
     * Returns one master article by ID, or 404 if not found.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Integer id) {
        try {
            log.info("GET /api/articulos/{}", id);
            return articuloMaestroService.findById(id)
                    .map(m -> ResponseEntity.ok(Map.of("success", true, "data", m)))
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("success", false, "message", "Artículo no encontrado: " + id)));
        } catch (Exception e) {
            log.error("Error al obtener artículo maestro {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * POST /api/articulos
     * Creates a new master article. Returns 201 on success.
     */
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody ArticuloMaestro maestro) {
        try {
            log.info("POST /api/articulos - creando artículo maestro: {}", maestro.getDescripcion());
            ArticuloMaestro saved = articuloMaestroService.save(maestro);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("success", true, "data", saved, "message", "Artículo creado exitosamente"));
        } catch (Exception e) {
            log.error("Error al crear artículo maestro", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * PUT /api/articulos/{id}
     * Updates an existing master article.
     * Sets the ID from the path and delegates to save(), which triggers write-through to variants.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody ArticuloMaestro maestro) {
        try {
            log.info("PUT /api/articulos/{} - actualizando artículo maestro", id);
            maestro.setIdArticuloMaestro(id);
            ArticuloMaestro saved = articuloMaestroService.save(maestro);
            return ResponseEntity.ok(Map.of("success", true, "data", saved, "message", "Artículo actualizado exitosamente"));
        } catch (Exception e) {
            log.error("Error al actualizar artículo maestro {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * DELETE /api/articulos/{id}
     * Deletes a master article.
     * Returns 409 Conflict when the master still has variants linked to it.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        try {
            log.info("DELETE /api/articulos/{}", id);
            articuloMaestroService.deleteById(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Artículo eliminado exitosamente"));
        } catch (IllegalStateException e) {
            log.warn("Intento de eliminar artículo maestro {} con variantes activas: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error al eliminar artículo maestro {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // =========================================================================
    // Activation / deactivation
    // =========================================================================

    /**
     * PATCH /api/articulos/{id}/desactivar
     * Deactivates the master and all its Producto variants in a single transaction.
     */
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<?> desactivar(@PathVariable Integer id) {
        try {
            log.info("PATCH /api/articulos/{}/desactivar", id);
            articuloMaestroService.deactivate(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Artículo desactivado correctamente"));
        } catch (Exception e) {
            log.error("Error al desactivar artículo maestro {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * PATCH /api/articulos/{id}/reactivar
     * Reactivates the master and all its Producto variants in a single transaction.
     */
    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<?> reactivar(@PathVariable Integer id) {
        try {
            log.info("PATCH /api/articulos/{}/reactivar", id);
            articuloMaestroService.reactivate(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Artículo reactivado correctamente"));
        } catch (Exception e) {
            log.error("Error al reactivar artículo maestro {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // =========================================================================
    // Variants
    // =========================================================================

    /**
     * GET /api/articulos/{id}/variantes
     * Returns all Producto variants linked to the given master.
     */
    @GetMapping("/{id}/variantes")
    public ResponseEntity<?> listarVariantes(@PathVariable Integer id) {
        try {
            log.info("GET /api/articulos/{}/variantes", id);
            List<Producto> variantes = articuloMaestroService.findVariantes(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", variantes,
                    "total", variantes.size()
            ));
        } catch (Exception e) {
            log.error("Error al listar variantes del artículo {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * POST /api/articulos/{id}/variantes
     * Creates a new Producto variant linked to the given master article.
     * Only variant-specific fields (codigo, presentacion, precioInstitucional, precioMayorista) are accepted.
     */
    @PostMapping("/{id}/variantes")
    @SuppressWarnings("deprecation")
    public ResponseEntity<?> crearVariante(@PathVariable Integer id, @RequestBody Producto variante) {
        try {
            log.info("POST /api/articulos/{}/variantes - creando variante: {}", id, variante.getCodigo());
            ArticuloMaestro maestro = articuloMaestroService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Artículo maestro no encontrado: " + id));
            variante.setArticuloMaestro(maestro);
            // Propagate master common/FE fields to variant deprecated columns (write-through safety)
            if (maestro.getDescripcion() != null) variante.setDescripcion(maestro.getDescripcion());
            if (maestro.getActive() != null)      variante.setActive(maestro.getActive());
            if (maestro.getGravado() != null)     variante.setGravado(maestro.getGravado());
            if (maestro.getPorcentajeImpuesto() != null) variante.setPorcentajeImpuesto(maestro.getPorcentajeImpuesto());
            if (maestro.getAplicaOtroImpuesto() != null) variante.setAplicaOtroImpuesto(maestro.getAplicaOtroImpuesto());
            if (maestro.getCodigoCabys() != null)        variante.setCodigoCabys(maestro.getCodigoCabys());
            if (maestro.getDescripcionCabys() != null)   variante.setDescripcionCabys(maestro.getDescripcionCabys());

            Producto saved = productoService.save(variante);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("success", true, "data", saved, "message", "Variante creada exitosamente"));
        } catch (IllegalArgumentException e) {
            log.warn("Artículo maestro {} no encontrado al crear variante: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error al crear variante para artículo {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * PUT /api/articulos/{id}/variantes/{vId}
     * Updates only variant-specific fields: codigo, precios, presentacion.
     * Master common/FE fields are NOT updated here — use PUT /api/articulos/{id} instead.
     */
    @PutMapping("/{id}/variantes/{vId}")
    public ResponseEntity<?> actualizarVariante(
            @PathVariable Integer id,
            @PathVariable Integer vId,
            @RequestBody Producto varianteData) {
        try {
            log.info("PUT /api/articulos/{}/variantes/{} - actualizando variante", id, vId);
            Producto existente = productoService.findById(vId)
                    .orElseThrow(() -> new IllegalArgumentException("Variante no encontrada: " + vId));

            // Only update variant-specific fields
            if (varianteData.getCodigo() != null) {
                existente.setCodigo(varianteData.getCodigo());
            }
            if (varianteData.getPrecioInstitucional() != null) {
                existente.setPrecioInstitucional(varianteData.getPrecioInstitucional());
            }
            if (varianteData.getPrecioMayorista() != null) {
                existente.setPrecioMayorista(varianteData.getPrecioMayorista());
            }
            if (varianteData.getPresentacion() != null && varianteData.getPresentacion().getIdPresentacion() != null) {
                Presentacion presentacion = presentacionService.findById(varianteData.getPresentacion().getIdPresentacion())
                        .orElse(varianteData.getPresentacion());
                existente.setPresentacion(presentacion);
            }

            Producto saved = productoService.save(existente);
            return ResponseEntity.ok(Map.of("success", true, "data", saved, "message", "Variante actualizada exitosamente"));
        } catch (IllegalArgumentException e) {
            log.warn("Variante {} no encontrada: {}", vId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error al actualizar variante {} del artículo {}", vId, id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * DELETE /api/articulos/{id}/variantes/{vId}
     * Deletes a variant by its Producto ID.
     * Returns 409 Conflict when attempting to delete the last variant of a master article.
     */
    @DeleteMapping("/{id}/variantes/{vId}")
    public ResponseEntity<?> eliminarVariante(@PathVariable Integer id, @PathVariable Integer vId) {
        try {
            log.info("DELETE /api/articulos/{}/variantes/{}", id, vId);
            articuloMaestroService.deleteVariante(id, vId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Variante eliminada exitosamente"));
        } catch (IllegalStateException e) {
            log.warn("Intento de eliminar la última variante del artículo maestro {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error al eliminar variante {} del artículo {}", vId, id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
