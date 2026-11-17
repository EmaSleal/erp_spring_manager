package api.astro.whats_orders_manager.modules.producto.controller;

import api.astro.whats_orders_manager.modules.configuracion.dto.CabysBusquedaDTO;
import api.astro.whats_orders_manager.modules.configuracion.dto.CabysDTO;
import api.astro.whats_orders_manager.modules.configuracion.service.HaciendaApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Búsqueda en el catálogo CAByS de Hacienda para el formulario del artículo maestro (ADR-040).
 * Disponible para quien puede crear o editar productos, no solo ADMIN.
 * Los mensajes de error son fijos; el detalle va solo al log (P-070).
 */
@Slf4j
@RestController
@RequestMapping("/api/articulos/cabys")
@RequiredArgsConstructor
public class CabysBusquedaRestController {

    static final String MSG_Q_REQUERIDO = "Ingrese un término de búsqueda";
    static final String MSG_HACIENDA = "No se pudo consultar el catálogo CAByS de Hacienda. Intente nuevamente más tarde.";
    static final String MSG_ERROR = "Error al buscar códigos CAByS";

    private final HaciendaApiService haciendaApiService;

    /**
     * GET /api/articulos/cabys/buscar?q=&top=
     * 200 con lista (posiblemente vacía), 400 sin término, 502 si Hacienda falla, 500 otro error.
     */
    @GetMapping("/buscar")
    @PreAuthorize("@permisoService.tieneAlgunPermisoPorCodigo(#authentication.name, 'PRODUCTO_CREAR', 'PRODUCTO_EDITAR')")
    public ResponseEntity<?> buscar(@RequestParam(value = "q", required = false) String q,
                                    @RequestParam(value = "top", defaultValue = "10") Integer top,
                                    Authentication authentication) {
        if (q == null || q.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", MSG_Q_REQUERIDO));
        }
        try {
            int limite = (top == null || top <= 0) ? 10 : Math.min(top, 50);
            CabysBusquedaDTO resultado = haciendaApiService.buscarCabys(q.trim(), limite);
            if (resultado == null || Boolean.TRUE.equals(resultado.getFallaServicio())) {
                log.warn("Búsqueda CAByS: la API de Hacienda no respondió o falló");
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("success", false, "message", MSG_HACIENDA));
            }
            List<CabysDTO> items = resultado.tieneResultados() ? resultado.getCabys() : List.of();
            return ResponseEntity.ok(Map.of("success", true, "data", items, "total", items.size()));
        } catch (Exception e) {
            log.error("Error al buscar códigos CAByS", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", MSG_ERROR));
        }
    }
}
