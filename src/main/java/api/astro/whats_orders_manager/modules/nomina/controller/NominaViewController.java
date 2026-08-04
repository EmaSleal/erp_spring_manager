package api.astro.whats_orders_manager.modules.nomina.controller;

import api.astro.whats_orders_manager.modules.nomina.service.NominaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * View controller for the Nómina (payroll) module.
 *
 * Routes are nested under /rrhh/nomina to keep nómina inside the RRHH section.
 * All endpoints are gated with NOMINA_VER via @PreAuthorize — same pattern as
 * AusenciaViewController.
 */
@Controller
@RequestMapping("/rrhh/nomina")
@RequiredArgsConstructor
public class NominaViewController {

    private final NominaService nominaService;

    /**
     * Lists all nóminas as a summary table.
     *
     * @permission NOMINA_VER
     */
    @GetMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'NOMINA_VER')")
    public String lista(Model model, Authentication authentication) {
        model.addAttribute("nominas", nominaService.listar());
        return "modules/nomina/lista";
    }

    /**
     * Detail view for a single nómina including employee breakdown lines.
     *
     * @permission NOMINA_VER
     */
    @GetMapping("/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'NOMINA_VER')")
    public String ver(
            @PathVariable Long id,
            Model model,
            Authentication authentication) {
        model.addAttribute("nomina", nominaService.obtener(id));
        return "modules/nomina/ver";
    }
}
