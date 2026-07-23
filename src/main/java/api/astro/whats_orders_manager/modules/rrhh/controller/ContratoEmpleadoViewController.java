package api.astro.whats_orders_manager.modules.rrhh.controller;

import api.astro.whats_orders_manager.modules.rrhh.enums.TipoContrato;
import api.astro.whats_orders_manager.modules.rrhh.enums.TipoJornada;
import api.astro.whats_orders_manager.modules.rrhh.service.ContratoEmpleadoService;
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
@RequestMapping("/rrhh/contratos")
@RequiredArgsConstructor
public class ContratoEmpleadoViewController {

    private final ContratoEmpleadoService contratoService;
    private final EmpleadoService empleadoService;

    @GetMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_VER')")
    public String lista(Model model, Authentication authentication) {
        model.addAttribute("empleados", empleadoService.findActivos());
        return "modules/rrhh/contratos/lista";
    }

    @GetMapping("/nuevo")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_GESTIONAR_CONTRATOS')")
    public String nuevo(Model model, Authentication authentication) {
        model.addAttribute("empleados", empleadoService.findActivos());
        model.addAttribute("tiposContrato", TipoContrato.values());
        model.addAttribute("tiposJornada", TipoJornada.values());
        return "modules/rrhh/contratos/form";
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_VER')")
    public String detalle(
            @PathVariable Long id,
            Model model,
            Authentication authentication) {
        model.addAttribute("contrato", contratoService.findByEmpleado(id));
        return "modules/rrhh/contratos/detalle";
    }
}
