package api.astro.whats_orders_manager.modules.facturacion.controller;

import api.astro.whats_orders_manager.modules.facturacion.service.FacturaPdfService;
import api.astro.whats_orders_manager.shared.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.NoSuchElementException;

/**
 * Páginas de las notas de crédito en Facturación (ADR-047, T-042): listado, detalle y PDF. Consultar exige FACTURA_VER;
 * emitir (FACTURA_ANULAR) se hace desde el detalle de la factura. Los datos los pide la página a la API
 * ({@link NotaCreditoRestController}).
 */
@Controller
@RequiredArgsConstructor
public class NotaCreditoViewController {

    private final FacturaPdfService facturaPdfService;

    @GetMapping("/facturas/notas-credito")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'FACTURA_VER')")
    public String listado(Authentication authentication) {
        return "modules/facturacion/notas-credito";
    }

    @GetMapping("/facturas/notas-credito/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'FACTURA_VER')")
    public String detalle(@PathVariable Long id, Authentication authentication, Model model) {
        model.addAttribute("notaId", id);
        return "modules/facturacion/notas-credito-detalle";
    }

    @GetMapping("/facturas/notas-credito/{id}/pdf")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'FACTURA_VER')")
    @ResponseBody
    public ResponseEntity<byte[]> pdf(@PathVariable Long id, Authentication authentication) {
        try {
            return ResponseUtil.pdf(facturaPdfService.generarPdfNotaCredito(id), "nota-credito-" + id + ".pdf");
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
