package api.astro.whats_orders_manager.modules.proveedores.controller;

import api.astro.whats_orders_manager.modules.proveedores.dto.OrdenCompraDTO;
import api.astro.whats_orders_manager.modules.proveedores.dto.RecepcionDTO;
import api.astro.whats_orders_manager.modules.proveedores.enums.EstadoOrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.model.OrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.service.CuentaPorPagarService;
import api.astro.whats_orders_manager.modules.proveedores.service.OrdenCompraService;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import api.astro.whats_orders_manager.shared.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/ordenes-compra")
@RequiredArgsConstructor
public class OrdenCompraRestController {

    private final OrdenCompraService ordenCompraService;
    private final CuentaPorPagarService cuentaPorPagarService;
    private final UsuarioService usuarioService;

    @GetMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'ORDEN_COMPRA_VER')")
    public ResponseEntity<ResponseDTO> listar(
            @RequestParam(required = false) EstadoOrdenCompra estado,
            Authentication authentication
    ) {
        var lista = estado != null
            ? ordenCompraService.findByEstado(estado)
            : ordenCompraService.findAll();
        return ResponseEntity.ok(ResponseDTO.success("OK", lista));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'ORDEN_COMPRA_VER')")
    public ResponseEntity<ResponseDTO> findById(@PathVariable Long id, Authentication authentication) {
        try {
            return ResponseEntity.ok(ResponseDTO.success("OK", ordenCompraService.findById(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'ORDEN_COMPRA_CREAR')")
    public ResponseEntity<ResponseDTO> crear(
            @RequestBody OrdenCompraDTO dto,
            Authentication authentication
    ) {
        try {
            Usuario usuario = resolverUsuario(authentication);
            OrdenCompra oc = ordenCompraService.crear(dto, usuario);
            return ResponseEntity.ok(ResponseDTO.success("Orden de compra creada: " + oc.getNumero(), oc));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/aprobar")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'ORDEN_COMPRA_APROBAR')")
    public ResponseEntity<ResponseDTO> aprobar(@PathVariable Long id, Authentication authentication) {
        try {
            Usuario usuario = resolverUsuario(authentication);
            OrdenCompra oc = ordenCompraService.aprobar(id, usuario);
            return ResponseEntity.ok(ResponseDTO.success("OC " + oc.getNumero() + " aprobada", oc));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'ORDEN_COMPRA_CANCELAR')")
    public ResponseEntity<ResponseDTO> cancelar(@PathVariable Long id, Authentication authentication) {
        try {
            Usuario usuario = resolverUsuario(authentication);
            OrdenCompra oc = ordenCompraService.cancelar(id, usuario);
            return ResponseEntity.ok(ResponseDTO.success("OC " + oc.getNumero() + " cancelada", oc));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/recibir")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'ORDEN_COMPRA_APROBAR')")
    public ResponseEntity<ResponseDTO> recibirParcial(
            @PathVariable Long id,
            @RequestBody RecepcionDTO dto,
            Authentication authentication
    ) {
        try {
            dto.setOrdenCompraId(id);
            Usuario usuario = resolverUsuario(authentication);
            OrdenCompra oc = ordenCompraService.recibirParcial(dto, usuario);
            return ResponseEntity.ok(ResponseDTO.success("Recepción registrada — OC en estado: " + oc.getEstado(), oc));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/generar-cpp")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CUENTA_PAGAR_CREAR')")
    public ResponseEntity<ResponseDTO> generarCuentaPorPagar(
            @PathVariable Long id,
            Authentication authentication
    ) {
        try {
            var cpp = cuentaPorPagarService.crearDesdeOrdenCompra(id);
            return ResponseEntity.ok(ResponseDTO.success("Cuenta por pagar generada: " + cpp.getNumero(), cpp));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Usuario resolverUsuario(Authentication authentication) {
        return usuarioService.findByTelefono(authentication.getName())
            .orElseThrow(() -> new IllegalStateException("Usuario no encontrado: " + authentication.getName()));
    }
}
