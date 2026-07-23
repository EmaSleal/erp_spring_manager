package api.astro.whats_orders_manager.modules.rrhh.controller;

import api.astro.whats_orders_manager.modules.rrhh.repository.EmpleadoRepository;
import api.astro.whats_orders_manager.modules.rrhh.service.DepartamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/rrhh/departamentos")
@RequiredArgsConstructor
public class DepartamentoViewController {

    private final DepartamentoService departamentoService;
    private final EmpleadoRepository empleadoRepository;

    @GetMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_VER')")
    public String lista(Model model, Authentication authentication) {
        model.addAttribute("departamentos", departamentoService.findAll());
        model.addAttribute("empleadosActivos", empleadoRepository.findByActivoTrue());
        return "modules/rrhh/departamentos/lista";
    }
}
