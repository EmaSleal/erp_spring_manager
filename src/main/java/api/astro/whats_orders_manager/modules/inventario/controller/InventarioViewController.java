package api.astro.whats_orders_manager.modules.inventario.controller;

import api.astro.whats_orders_manager.modules.inventario.enums.EstadoAjuste;
import api.astro.whats_orders_manager.modules.inventario.enums.TipoAjuste;
import api.astro.whats_orders_manager.modules.inventario.service.AjusteInventarioService;
import api.astro.whats_orders_manager.modules.inventario.service.ReporteInventarioService;
import api.astro.whats_orders_manager.modules.producto.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/inventario")
@RequiredArgsConstructor
public class InventarioViewController {

    private final ProductoRepository productoRepository;
    private final ReporteInventarioService reporteService;
    private final AjusteInventarioService ajusteService;

    @GetMapping("/kardex")
    @PreAuthorize("isAuthenticated()")
    public String kardex(Model model) {
        model.addAttribute("productos", productoRepository.findAll());
        return "modules/inventario/kardex";
    }

    @GetMapping("/stock")
    @PreAuthorize("isAuthenticated()")
    public String stock(Model model) {
        model.addAttribute("resumen", reporteService.getResumen());
        model.addAttribute("criticos", reporteService.getStockCritico());
        model.addAttribute("bajos", reporteService.getStockBajo());
        model.addAttribute("lotesPorVencer", reporteService.getLotesPorVencer(30));
        return "modules/inventario/stock";
    }

    @GetMapping("/ajustes")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'INVENTARIO_AJUSTAR')")
    public String ajustes(Model model, Authentication authentication) {
        model.addAttribute("ajustes", ajusteService.findAll());
        model.addAttribute("productos", productoRepository.findAll());
        model.addAttribute("tiposAjuste", TipoAjuste.values());
        model.addAttribute("estadosAjuste", EstadoAjuste.values());
        return "modules/inventario/ajustes";
    }
}
