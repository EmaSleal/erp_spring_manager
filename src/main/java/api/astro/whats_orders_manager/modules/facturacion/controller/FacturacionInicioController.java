package api.astro.whats_orders_manager.modules.facturacion.controller;

import api.astro.whats_orders_manager.modules.facturacion.service.PanelFacturacionService;
import api.astro.whats_orders_manager.modules.regimen.panel.RegimenPanelService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * T-043 (ADR-048): página de inicio del módulo de Facturación (patrón de RrhhDashboardController). Solo lo que
 * requiere acción, el estado de Hacienda y los accesos; sin montos de ventas ni gráficos.
 */
@Controller
@RequestMapping("/facturas")
@RequiredArgsConstructor
public class FacturacionInicioController {

    private final PanelFacturacionService panelService;
    /** T-056: aviso de verificación del régimen y tope de compras del RTS. */
    private final RegimenPanelService regimenPanelService;

    @GetMapping("/inicio")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'FACTURA_VER')")
    public String inicio(Model model, Authentication authentication) {
        PanelFacturacionService.Panel panel = panelService.obtener();
        model.addAttribute("contadores", panel.contadores());
        model.addAttribute("hacienda", panel.hacienda());
        // T-043 (review 1): ambiente de los contadores (null = sin ambiente activo) y la leyenda fija en ese caso
        String ambiente = panel.ambienteContadores() != null ? panel.ambienteContadores().name() : null;
        model.addAttribute("ambienteContadores", ambiente);
        // parámetro que los enlaces de comprobantes y notas añaden para listar solo ese ambiente (vacío = sin filtro)
        model.addAttribute("sufijoAmbiente", ambiente != null ? "&ambiente=" + ambiente : "");
        model.addAttribute("leyendaSinAmbiente", PanelFacturacionService.LEYENDA_SIN_AMBIENTE);
        model.addAttribute("regimenPanel", regimenPanelService.obtener());
        return "modules/facturacion/inicio";
    }
}
