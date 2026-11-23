package api.astro.whats_orders_manager.modules.contabilidad.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Pantalla de alta y corrección de la prorrata provisional del IVA por año (T-054, ADR-067 §4). */
@Controller
@RequestMapping("/contabilidad/prorrata-iva")
public class ProrrataIvaViewController {

    @GetMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PRORRATA_IVA_GESTIONAR')")
    public String prorrataIva(Model model, Authentication authentication) {
        model.addAttribute("title", "Prorrata del IVA");
        model.addAttribute("currentPage", "contabilidad");
        return "modules/contabilidad/prorrata-iva";
    }
}
