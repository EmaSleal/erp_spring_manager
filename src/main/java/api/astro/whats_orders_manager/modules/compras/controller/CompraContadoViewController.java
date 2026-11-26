package api.astro.whats_orders_manager.modules.compras.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Clock;
import java.time.LocalDate;

/** Formulario rápido «Compra de contado» (T-058, ADR-077 §1-2): exige crear CxP y registrar pagos. */
@Controller
@RequestMapping("/compras")
@RequiredArgsConstructor
public class CompraContadoViewController {

    private final Clock clock;

    @Value("${app.compras.adjunto.tamano-maximo:5MB}")
    private String tamanoMaximoAdjunto;

    @GetMapping("/contado")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CUENTA_PAGAR_CREAR')"
        + " and @permisoService.tienePermisoPorCodigo(#authentication.name, 'CUENTA_PAGAR_REGISTRAR_PAGO')")
    public String formulario(Model model, Authentication authentication) {
        model.addAttribute("hoy", LocalDate.now(clock).toString());
        model.addAttribute("tamanoMaximoAdjunto", DataSize.parse(tamanoMaximoAdjunto).toBytes());
        return "modules/proveedores/compra-contado";
    }
}
