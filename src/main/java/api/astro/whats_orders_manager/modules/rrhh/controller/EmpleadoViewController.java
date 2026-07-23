package api.astro.whats_orders_manager.modules.rrhh.controller;

import api.astro.whats_orders_manager.modules.rrhh.service.DepartamentoService;
import api.astro.whats_orders_manager.modules.rrhh.service.EmpleadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/rrhh/empleados")
@RequiredArgsConstructor
public class EmpleadoViewController {

    private final EmpleadoService empleadoService;
    private final DepartamentoService departamentoService;

    @GetMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_VER')")
    public String lista(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model,
            Authentication authentication) {
        Pageable pageable = PageRequest.of(page, size);
        model.addAttribute("empleados", empleadoService.findAll(pageable));
        model.addAttribute("departamentos", departamentoService.findActivos());
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        return "modules/rrhh/empleados/lista";
    }

    @GetMapping("/nuevo")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_GESTIONAR_EMPLEADOS')")
    public String nuevo(Model model, Authentication authentication) {
        model.addAttribute("departamentos", departamentoService.findActivos());
        model.addAttribute("usuariosDisponibles", empleadoService.findUsuariosDisponibles(null));
        model.addAttribute("empleado", null);
        return "modules/rrhh/empleados/form";
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_VER')")
    public String detalle(
            @PathVariable Long id,
            Model model,
            Authentication authentication) {
        model.addAttribute("empleado", empleadoService.findById(id));
        return "modules/rrhh/empleados/detalle";
    }

    @GetMapping("/{id}/editar")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_GESTIONAR_EMPLEADOS')")
    public String editar(
            @PathVariable Long id,
            Model model,
            Authentication authentication) {
        model.addAttribute("empleado", empleadoService.findById(id));
        model.addAttribute("departamentos", departamentoService.findActivos());
        model.addAttribute("usuariosDisponibles", empleadoService.findUsuariosDisponibles(id));
        return "modules/rrhh/empleados/form";
    }
}
