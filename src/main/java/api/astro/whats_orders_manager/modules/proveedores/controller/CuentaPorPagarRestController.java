package api.astro.whats_orders_manager.modules.proveedores.controller;

import api.astro.whats_orders_manager.modules.proveedores.dto.CuentaPorPagarDTO;
import api.astro.whats_orders_manager.modules.proveedores.dto.PagoProveedorDTO;
import api.astro.whats_orders_manager.modules.proveedores.enums.EstadoCuentaPorPagar;
import api.astro.whats_orders_manager.modules.proveedores.service.CuentaPorPagarService;
import api.astro.whats_orders_manager.modules.proveedores.service.PagoProveedorService;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import api.astro.whats_orders_manager.shared.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/cuentas-pagar")
@RequiredArgsConstructor
public class CuentaPorPagarRestController {

    private final CuentaPorPagarService cuentaPorPagarService;
    private final PagoProveedorService pagoProveedorService;
    private final UsuarioService usuarioService;

    @GetMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CUENTA_PAGAR_VER')")
    public ResponseEntity<ResponseDTO> listar(
            @RequestParam(required = false) EstadoCuentaPorPagar estado,
            Authentication authentication
    ) {
        var lista = estado != null
            ? cuentaPorPagarService.findByEstado(estado)
            : cuentaPorPagarService.findAll();
        return ResponseEntity.ok(ResponseDTO.success("OK", lista));
    }

    @GetMapping("/vencidas")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CUENTA_PAGAR_VER')")
    public ResponseEntity<ResponseDTO> vencidas(Authentication authentication) {
        return ResponseEntity.ok(ResponseDTO.success("OK", cuentaPorPagarService.findVencidas()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CUENTA_PAGAR_VER')")
    public ResponseEntity<ResponseDTO> findById(@PathVariable Long id, Authentication authentication) {
        try {
            return ResponseEntity.ok(ResponseDTO.success("OK", cuentaPorPagarService.findById(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}/pagos")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CUENTA_PAGAR_VER')")
    public ResponseEntity<ResponseDTO> pagosDeCuenta(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(ResponseDTO.success("OK", pagoProveedorService.findByCuenta(id)));
    }

    @GetMapping("/pagos")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CUENTA_PAGAR_VER')")
    public ResponseEntity<ResponseDTO> pagosPorPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ResponseDTO.success("OK", pagoProveedorService.findByPeriodo(desde, hasta)));
    }

    @PostMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CUENTA_PAGAR_VER')")
    public ResponseEntity<ResponseDTO> crear(
            @RequestBody CuentaPorPagarDTO dto,
            Authentication authentication
    ) {
        try {
            var cpp = cuentaPorPagarService.crear(dto);
            return ResponseEntity.ok(ResponseDTO.success("Cuenta por pagar creada: " + cpp.getNumero(), cpp));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/pagar")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CUENTA_PAGAR_REGISTRAR_PAGO')")
    public ResponseEntity<ResponseDTO> registrarPago(
            @PathVariable Long id,
            @RequestBody PagoProveedorDTO dto,
            Authentication authentication
    ) {
        try {
            dto.setCuentaPorPagarId(id);
            Usuario usuario = resolverUsuario(authentication);
            var pago = pagoProveedorService.registrarPago(dto, usuario);
            return ResponseEntity.ok(ResponseDTO.success("Pago " + pago.getNumero() + " registrado", pago));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    @PostMapping("/pagos/{pagoId}/anular")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CUENTA_PAGAR_REGISTRAR_PAGO')")
    public ResponseEntity<ResponseDTO> anularPago(
            @PathVariable Long pagoId,
            Authentication authentication
    ) {
        try {
            Usuario usuario = resolverUsuario(authentication);
            pagoProveedorService.anularPago(pagoId, usuario);
            return ResponseEntity.ok(ResponseDTO.success("Pago anulado exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    @PostMapping("/actualizar-vencidas")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CUENTA_PAGAR_VER')")
    public ResponseEntity<ResponseDTO> actualizarVencidas(Authentication authentication) {
        int marcadas = cuentaPorPagarService.actualizarVencidas();
        return ResponseEntity.ok(ResponseDTO.success("Cuentas marcadas como vencidas: " + marcadas));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Usuario resolverUsuario(Authentication authentication) {
        return usuarioService.findByTelefono(authentication.getName())
            .orElseThrow(() -> new IllegalStateException("Usuario no encontrado: " + authentication.getName()));
    }
}
