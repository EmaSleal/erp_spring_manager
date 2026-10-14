package api.astro.whats_orders_manager.modules.configuracion.controller;

import api.astro.whats_orders_manager.modules.configuracion.model.CuentaBancaria;
import api.astro.whats_orders_manager.modules.configuracion.service.CuentaBancariaService;
import api.astro.whats_orders_manager.shared.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only list of the company's active bank accounts for the payment screens (ADR-018): the
 * "Cuenta bancaria de destino" of a cobro and the "Cuenta bancaria de origen" of a pago a proveedor.
 *
 * Bank account management stays under /api/empresa/cuentas-bancarias (ADMIN only). This endpoint is
 * open to whoever registers payments (PAGO_CREAR or CUENTA_PAGAR_REGISTRAR_PAGO) and only exposes
 * what the selector needs; /api/** already requires authentication in SecurityConfig, so no change
 * there is needed.
 */
@RestController
@RequestMapping("/api/cuentas-bancarias")
@RequiredArgsConstructor
public class CuentaBancariaRestController {

    private final CuentaBancariaService cuentaBancariaService;

    /**
     * GET /api/cuentas-bancarias/activas?moneda=CRC
     * @param moneda currency of the payment (CRC, USD, EUR); omitted → all active accounts
     */
    @GetMapping("/activas")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PAGO_CREAR')"
        + " or @permisoService.tienePermisoPorCodigo(#authentication.name, 'CUENTA_PAGAR_REGISTRAR_PAGO')")
    public ResponseEntity<ResponseDTO> activas(@RequestParam(required = false) String moneda,
                                               Authentication authentication) {
        try {
            List<Map<String, Object>> cuentas = cuentaBancariaService.listarActivas(moneda).stream()
                .map(CuentaBancariaRestController::toMap)
                .toList();
            return ResponseEntity.ok(ResponseDTO.success("OK", cuentas));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    private static Map<String, Object> toMap(CuentaBancaria c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("idCuentaBancaria", c.getIdCuentaBancaria());
        m.put("entidad", c.getEntidad());
        m.put("moneda", c.getMoneda());
        m.put("cuentaBanco", c.getCuentaBanco());
        m.put("cuentaIban", c.getCuentaIban());
        m.put("codigoCuentaContable", c.getCuentaContable() != null ? c.getCuentaContable().getCodigo() : null);
        return m;
    }
}
