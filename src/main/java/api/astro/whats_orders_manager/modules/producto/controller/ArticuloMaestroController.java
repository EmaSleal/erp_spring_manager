package api.astro.whats_orders_manager.modules.producto.controller;

import api.astro.whats_orders_manager.modules.configuracion.service.PresentacionService;
import api.astro.whats_orders_manager.modules.producto.model.ArticuloMaestro;
import api.astro.whats_orders_manager.modules.producto.service.ArticuloMaestroService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Thymeleaf controller for the ArticuloMaestro (master article) management views.
 * Handles the list, create, and edit pages.
 *
 * GET  /articulos           — master list view
 * GET  /articulos/nuevo     — create master + first variant
 * GET  /articulos/{id}/editar — edit master fields and manage variants sub-table
 */
@Slf4j
@Controller
@RequestMapping("/articulos")
@RequiredArgsConstructor
public class ArticuloMaestroController {

    private final ArticuloMaestroService articuloMaestroService;
    private final PresentacionService presentacionService;

    /**
     * List all master articles, or only the active ones without CAByS with ?sinCabys=true.
     *
     * GET /articulos[?sinCabys=true]
     */
    @GetMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PRODUCTO_VER')")
    public String listar(@RequestParam(name = "sinCabys", defaultValue = "false") boolean sinCabys,
                         Model model, Authentication authentication) {
        log.info("GET /articulos - listando artículos maestro (sinCabys={})", sinCabys);
        List<ArticuloMaestro> maestros = sinCabys
                ? articuloMaestroService.findActivosSinCabys()
                : articuloMaestroService.findAll();
        model.addAttribute("sinCabys", sinCabys);
        model.addAttribute("totalSinCabys", articuloMaestroService.countActivosSinCabys());
        Map<Integer, Long> varianteCounts = articuloMaestroService.getVarianteCounts();
        model.addAttribute("maestros", maestros);
        model.addAttribute("varianteCounts", varianteCounts);
        return "modules/producto/articulos";
    }

    /**
     * Show the create-new form (master fields only; first variant section is in the template).
     *
     * GET /articulos/nuevo
     */
    @GetMapping("/nuevo")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PRODUCTO_CREAR')")
    public String nuevo(Model model, Authentication authentication) {
        log.info("GET /articulos/nuevo - formulario de nuevo artículo maestro");
        model.addAttribute("maestro", new ArticuloMaestro());
        model.addAttribute("presentaciones", presentacionService.findAll());
        return "modules/producto/articulo-form";
    }

    /**
     * Show the edit form for an existing master article.
     * The variants sub-table is loaded via JS (fetch /api/articulos/{id}/variantes).
     *
     * GET /articulos/{id}/editar
     */
    @GetMapping("/{id}/editar")
    @PreAuthorize("@permisoService.tieneAlgunPermisoPorCodigo(#authentication.name, 'PRODUCTO_CREAR', 'PRODUCTO_EDITAR')")
    public String editar(@PathVariable Integer id, Model model, Authentication authentication) {
        log.info("GET /articulos/{}/editar - cargando artículo maestro", id);
        ArticuloMaestro maestro = articuloMaestroService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Artículo no encontrado: " + id));
        model.addAttribute("maestro", maestro);
        model.addAttribute("presentaciones", presentacionService.findAll());
        return "modules/producto/articulo-form";
    }
}
