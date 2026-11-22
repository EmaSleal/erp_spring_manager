package api.astro.whats_orders_manager.modules.contabilidad.controller;

import api.astro.whats_orders_manager.modules.contabilidad.dto.AnularCuentaPorPagarDTO;
import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarCreadaDTO;
import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarDTO;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoCuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.contabilidad.service.ComprobanteProveedorDuplicadoException;
import api.astro.whats_orders_manager.modules.contabilidad.service.CuentaPorPagarService;
import api.astro.whats_orders_manager.modules.proveedores.dto.PagoProveedorDTO;
import api.astro.whats_orders_manager.modules.proveedores.service.PagoProveedorService;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import api.astro.whats_orders_manager.shared.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/cuentas-pagar")
@RequiredArgsConstructor
public class CuentaPorPagarRestController {

    private final CuentaPorPagarService cuentaPorPagarService;
    private final PagoProveedorService pagoProveedorService;
    private final UsuarioService usuarioService;
    private final AsientoContableService asientoContableService;

    @GetMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CUENTA_PAGAR_VER')")
    public ResponseEntity<ResponseDTO> listar(
            @RequestParam(required = false) EstadoCuentaPorPagar estado,
            Authentication authentication
    ) {
        // T-052 (P-023 §1): DTO armados en una transacción de lectura, nunca la entidad con sus proxies perezosos
        return ResponseEntity.ok(ResponseDTO.success("OK", cuentaPorPagarService.listarVista(estado)));
    }

    @GetMapping("/vencidas")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CUENTA_PAGAR_VER')")
    public ResponseEntity<ResponseDTO> vencidas(Authentication authentication) {
        return ResponseEntity.ok(ResponseDTO.success("OK", cuentaPorPagarService.vencidasVista()));
    }

    /**
     * Cuentas elegibles como contrapartida de una CxP manual (ADR-016), ordenadas por código.
     * cuentaComprasId es la cuenta de CPP_CUENTA_COMPRAS (null si no está configurada); la cuenta
     * correspondiente de la lista viene con predeterminada = true para preseleccionarla.
     */
    @GetMapping("/cuentas-contrapartida")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CUENTA_PAGAR_CREAR')")
    public ResponseEntity<ResponseDTO> cuentasContrapartida(Authentication authentication) {
        Long cuentaComprasId = asientoContableService.obtenerIdCuentaComprasCpp().orElse(null);
        var cuentas = asientoContableService.obtenerCuentasContrapartidaCxpManual().stream()
            .map(c -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",             c.getIdCuenta());
                m.put("codigo",         c.getCodigo());
                m.put("nombre",         c.getNombre());
                m.put("tipo",           c.getTipo() != null ? c.getTipo().name() : null);
                m.put("predeterminada", c.getIdCuenta().equals(cuentaComprasId));
                return m;
            })
            .toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("cuentaComprasId", cuentaComprasId);
        data.put("cuentas", cuentas);
        return ResponseEntity.ok(ResponseDTO.success("OK", data));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CUENTA_PAGAR_VER')")
    public ResponseEntity<ResponseDTO> findById(@PathVariable Long id, Authentication authentication) {
        try {
            return ResponseEntity.ok(ResponseDTO.success("OK", cuentaPorPagarService.detalleVista(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}/pagos")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CUENTA_PAGAR_VER')")
    public ResponseEntity<ResponseDTO> pagosDeCuenta(@PathVariable Long id, Authentication authentication) {
        var pagos = pagoProveedorService.findByCuentaVista(id);
        return ResponseEntity.ok(ResponseDTO.success("OK", pagos));
    }

    @GetMapping("/pagos")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CUENTA_PAGAR_VER')")
    public ResponseEntity<ResponseDTO> pagosPorPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ResponseDTO.success("OK", pagoProveedorService.findByPeriodoVista(desde, hasta)));
    }

    @PostMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CUENTA_PAGAR_CREAR')")
    public ResponseEntity<ResponseDTO> crear(
            @RequestBody CuentaPorPagarDTO dto,
            Authentication authentication
    ) {
        try {
            var cpp = cuentaPorPagarService.crear(dto);
            var creada = new CuentaPorPagarCreadaDTO(cuentaPorPagarService.detalleVista(cpp.getId()),
                cuentaPorPagarService.advertencias(dto));
            return ResponseEntity.ok(ResponseDTO.success("Cuenta por pagar creada: " + cpp.getNumero(), creada));
        } catch (ComprobanteProveedorDuplicadoException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    /**
     * Anula una CxP sin pagos activos (ADR-066 §3). 400 con motivo inválido, 404 si no existe, 409 si ya está anulada
     * o tiene pagos activos.
     */
    @PostMapping("/{id}/anular")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CUENTA_PAGAR_ANULAR')")
    public ResponseEntity<ResponseDTO> anular(
            @PathVariable Long id,
            @RequestBody AnularCuentaPorPagarDTO dto,
            Authentication authentication
    ) {
        try {
            Usuario usuario = resolverUsuario(authentication);
            var cpp = cuentaPorPagarService.anular(id, dto.getMotivo(), usuario);
            return ResponseEntity.ok(ResponseDTO.success("Cuenta por pagar " + cpp.getNumero() + " anulada"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseDTO.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ResponseDTO.error(e.getMessage()));
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
            return ResponseEntity.ok(ResponseDTO.success("Pago " + pago.getNumero() + " registrado"));
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
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CUENTA_PAGAR_REGISTRAR_PAGO')")
    public ResponseEntity<ResponseDTO> actualizarVencidas(Authentication authentication) {
        int marcadas = cuentaPorPagarService.actualizarVencidas();
        return ResponseEntity.ok(ResponseDTO.success("Cuentas marcadas como vencidas: " + marcadas));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Usuario resolverUsuario(Authentication authentication) {
        return usuarioService.findByEmail(authentication.getName())
            .orElseThrow(() -> new IllegalStateException("Usuario no encontrado: " + authentication.getName()));
    }
}
