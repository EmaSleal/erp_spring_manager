package api.astro.whats_orders_manager.modules.rrhh.controller;

import api.astro.whats_orders_manager.modules.rrhh.enums.TipoJornada;
import api.astro.whats_orders_manager.modules.rrhh.service.DepartamentoService;
import api.astro.whats_orders_manager.modules.rrhh.service.PuestoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/rrhh/puestos")
@RequiredArgsConstructor
public class PuestoViewController {

    private final PuestoService puestoService;
    private final DepartamentoService departamentoService;

    @GetMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_VER')")
    public String lista(Model model, Authentication authentication) {
        model.addAttribute("puestos", puestoService.findAll());
        model.addAttribute("departamentos", departamentoService.findActivos());
        model.addAttribute("tiposJornada", TipoJornada.values());
        return "modules/rrhh/puestos/lista";
    }
}
