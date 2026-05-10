package api.astro.whats_orders_manager.modules.facturacion.electronica.controller;

import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmpresaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controlador MVC para vistas de facturación electrónica.
 *
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
@Controller
@RequestMapping("/facturas")
@RequiredArgsConstructor
@Slf4j
public class FacturacionElectronicaViewController {

    private final ConfiguracionEmpresaRepository configuracionEmpresaRepository;

    /**
     * Vista de comprobantes electrónicos.
     * GET /facturacion/comprobantes
     */
    @GetMapping("/comprobantes")
    public String comprobantes(Model model) {
        log.info("Accediendo a vista de comprobantes electrónicos");
        Integer empresaId = configuracionEmpresaRepository.findFirstByOrderByIdConfiguracionAsc()
                .map(e -> e.getIdConfiguracion())
                .orElse(1);
        model.addAttribute("empresaId", empresaId);
        return "modules/facturacion/electronica/comprobantes";
    }

    /**
     * Vista de configuración de Hacienda.
     * GET /facturacion/configuracion-hacienda
     */
    @GetMapping("/configuracion-hacienda")
    public String configuracionHacienda(Model model) {
        log.info("Accediendo a vista de configuración de Hacienda");
        Integer empresaId = configuracionEmpresaRepository.findFirstByOrderByIdConfiguracionAsc()
                .map(e -> e.getIdConfiguracion())
                .orElse(1);
        model.addAttribute("empresaId", empresaId);
        return "modules/facturacion/electronica/configuracion-hacienda";
    }
}
