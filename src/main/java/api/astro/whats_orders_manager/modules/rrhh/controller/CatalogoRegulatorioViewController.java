package api.astro.whats_orders_manager.modules.rrhh.controller;

import api.astro.whats_orders_manager.modules.rrhh.service.ParametroCCSSService;
import api.astro.whats_orders_manager.modules.rrhh.service.SalarioMinimoService;
import api.astro.whats_orders_manager.modules.rrhh.service.TramoImpuestoSalarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;

/**
 * MVC view controller for regulatory catalogs (read-only).
 * No mutations — these catalogs are managed via Flyway migrations.
 */
@Controller
@RequestMapping("/rrhh/catalogos")
@RequiredArgsConstructor
public class CatalogoRegulatorioViewController {

    private final ParametroCCSSService parametroCCSSService;
    private final TramoImpuestoSalarioService tramoImpuestoSalarioService;
    private final SalarioMinimoService salarioMinimoService;

    @GetMapping("/parametros-ccss")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_VER_CATALOGO_SALARIAL')")
    public String parametrosCcss(Model model, Authentication authentication) {
        try {
            model.addAttribute("parametroCcss",
                    parametroCCSSService.findVigenteByFecha(LocalDate.now()));
        } catch (Exception e) {
            model.addAttribute("parametroCcss", null);
        }
        model.addAttribute("historial", parametroCCSSService.findAll());
        model.addAttribute("tramosActuales",
                tramoImpuestoSalarioService.findByAnioVigencia(LocalDate.now().getYear()));
        return "modules/rrhh/catalogos/parametros-ccss";
    }

    @GetMapping("/salarios-minimos")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_VER_CATALOGO_SALARIAL')")
    public String salariosMinimos(Model model, Authentication authentication) {
        model.addAttribute("salarios", salarioMinimoService.findAll());
        return "modules/rrhh/catalogos/salarios-minimos";
    }
}
