package api.astro.whats_orders_manager.modules.contabilidad.controller;

import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoCuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.service.CuentaPorPagarService;
import api.astro.whats_orders_manager.modules.seguridad.dto.ModuloDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador de vistas para el módulo de Contabilidad.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
@Controller
@RequestMapping("/contabilidad")
@RequiredArgsConstructor
@Slf4j
public class ContabilidadViewController {

    private final CuentaPorPagarService cuentaPorPagarService;


    /**
     * Muestra la página principal de Contabilidad.
     * GET /contabilidad
     */
    @GetMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_VER')")
    public String mostrarContabilidad(Model model, Authentication authentication) {
        log.info("Accediendo a módulo de Contabilidad");
        
        try {
            // Agregar información del usuario actual si es necesario
            if (authentication != null) {
                model.addAttribute("userName", authentication.getName());
            }
            
            model.addAttribute("title", "Contabilidad");
            model.addAttribute("currentPage", "contabilidad");
            
            // Cargar submódulos de contabilidad
            List<ModuloDTO> modulos = cargarSubmodulosContabilidad();
            model.addAttribute("modulos", modulos);
            
            return "modules/contabilidad/contabilidad";
            
        } catch (Exception e) {
            log.error("Error al cargar módulo de Contabilidad: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al cargar el módulo de Contabilidad");
            return "error/error";
        }
    }
    
    /**
     * Displays the Cuentas por Pagar list view with summary statistics.
     * GET /contabilidad/cuentas-pagar
     */
    @GetMapping("/cuentas-pagar")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CUENTA_PAGAR_VER')")
    public String cuentasPagar(Model model, Authentication authentication) {
        log.info("Accessing Cuentas por Pagar view");

        List<CuentaPorPagar> cuentas = cuentaPorPagarService.findAll();

        BigDecimal totalSaldoPendiente = cuentas.stream()
            .filter(c -> c.getEstado() != EstadoCuentaPorPagar.PAGADA)
            .map(CuentaPorPagar::getSaldoPendiente)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long countVencidas = cuentas.stream()
            .filter(c -> c.getEstado() == EstadoCuentaPorPagar.VENCIDA)
            .count();

        BigDecimal totalPagado = cuentas.stream()
            .filter(c -> c.getEstado() == EstadoCuentaPorPagar.PAGADA)
            .map(CuentaPorPagar::getMonto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("cuentas", cuentas);
        model.addAttribute("estados", EstadoCuentaPorPagar.values());
        model.addAttribute("totalSaldoPendiente", totalSaldoPendiente);
        model.addAttribute("countVencidas", countVencidas);
        model.addAttribute("totalPagado", totalPagado);

        return "modules/contabilidad/cuentas-pagar";
    }

    /**
     * Carga los submódulos disponibles del módulo de Contabilidad
     */
    private List<ModuloDTO> cargarSubmodulosContabilidad() {
        List<ModuloDTO> modulos = new ArrayList<>();
        
        // Cuentas
        modulos.add(new ModuloDTO(
            "Cuentas",
            "Plan de cuentas contable",
            "fas fa-list-alt",
            "#667eea",
            "/contabilidad/cuentas",
            true, // Próximamente
            true   // Visible
        ));
        
        // Asientos Contables
        modulos.add(new ModuloDTO(
            "Asientos",
            "Registro de asientos contables",
            "fas fa-book-open",
            "#f093fb",
            "/contabilidad/asientos",
            true, // Próximamente
            true   // Visible
        ));
        
        // Cuentas por Cobrar
        modulos.add(new ModuloDTO(
            "Cuentas por Cobrar",
            "Gestión de cobros y saldos",
            "fas fa-hand-holding-usd",
            "#4facfe",
            "/contabilidad/cuentas-cobrar",
            false,
            true
        ));
        
        // Cuentas por Pagar
        modulos.add(new ModuloDTO(
            "Cuentas por Pagar",
            "Control de pagos a proveedores",
            "fas fa-file-invoice-dollar",
            "#fa709a",
            "/contabilidad/cuentas-pagar",
            true,
            true
        ));
        
        // Libro Mayor
        modulos.add(new ModuloDTO(
            "Libro Mayor",
            "Registro de transacciones",
            "fas fa-book",
            "#a8edea",
            "/contabilidad/libro-mayor",
            false,
            true
        ));
        
        // Reportes Contables
        modulos.add(new ModuloDTO(
            "Reportes",
            "Balance, estado de resultados",
            "fas fa-chart-line",
            "#fbc2eb",
            "/contabilidad/reportes",
            true,
            true
        ));
        
        // Balance General
        modulos.add(new ModuloDTO(
            "Balance General",
            "Estado financiero de activos",
            "fas fa-balance-scale",
            "#fdcbf1",
            "/contabilidad/balance",
            false,
            true
        ));
        
        // Estado de Resultados
        modulos.add(new ModuloDTO(
            "Estado de Resultados",
            "Ingresos, costos y utilidad",
            "fas fa-chart-bar",
            "#e0c3fc",
            "/contabilidad/estado-resultados",
            false,
            true
        ));
        
        // Flujo de Caja
        modulos.add(new ModuloDTO(
            "Flujo de Caja",
            "Movimientos de efectivo",
            "fas fa-money-bill-wave",
            "#8ec5fc",
            "/contabilidad/flujo-caja",
            false,
            true
        ));
        
        log.debug("Submódulos de contabilidad cargados: {}", modulos.size());
        return modulos;
    }
}
