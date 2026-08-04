package api.astro.whats_orders_manager.modules.rrhh.controller;

import api.astro.whats_orders_manager.modules.rrhh.enums.TipoAusencia;
import api.astro.whats_orders_manager.modules.rrhh.service.AusenciaService;
import api.astro.whats_orders_manager.modules.rrhh.service.EmpleadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/rrhh/ausencias")
@RequiredArgsConstructor
public class AusenciaViewController {

    private final AusenciaService ausenciaService;
    private final EmpleadoService empleadoService;

    @GetMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_VER')")
    public String lista(Model model, Authentication authentication) {
        model.addAttribute("empleados", empleadoService.findActivos());
        return "modules/rrhh/ausencias/lista";
    }

    @GetMapping("/nueva")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_GESTIONAR_AUSENCIAS')")
    public String nueva(Model model, Authentication authentication) {
        model.addAttribute("empleados", empleadoService.findActivos());
        model.addAttribute("tiposAusencia", TipoAusencia.values());
        return "modules/rrhh/ausencias/form";
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_VER')")
    public String detalle(
            @PathVariable Long id,
            Model model,
            Authentication authentication) {
        model.addAttribute("ausencia", ausenciaService.findByEmpleado(id));
        return "modules/rrhh/ausencias/detalle";
    }
}
