package api.astro.whats_orders_manager.modules.rrhh.controller;

import api.astro.whats_orders_manager.modules.rrhh.repository.AusenciaRepository;
import api.astro.whats_orders_manager.modules.rrhh.repository.ContratoEmpleadoRepository;
import api.astro.whats_orders_manager.modules.rrhh.repository.DepartamentoRepository;
import api.astro.whats_orders_manager.modules.rrhh.repository.EmpleadoRepository;
import api.astro.whats_orders_manager.modules.rrhh.repository.PuestoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/rrhh")
@RequiredArgsConstructor
@Slf4j
public class RrhhDashboardController {

    private final EmpleadoRepository empleadoRepository;
    private final DepartamentoRepository departamentoRepository;
    private final PuestoRepository puestoRepository;
    private final ContratoEmpleadoRepository contratoRepository;
    private final AusenciaRepository ausenciaRepository;

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("totalEmpleados", empleadoRepository.countByActivoTrue());
        model.addAttribute("totalDepartamentos", departamentoRepository.countByActivoTrue());
        model.addAttribute("totalPuestos", puestoRepository.countByActivoTrue());
        model.addAttribute("contratosActivos", contratoRepository.countByActivoTrue());
        model.addAttribute("ausenciasPendientes", ausenciaRepository.countByAprobadaFalse());
        return "modules/rrhh/dashboard";
    }
}
