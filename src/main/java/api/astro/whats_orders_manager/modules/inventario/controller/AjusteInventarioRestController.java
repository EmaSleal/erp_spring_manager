package api.astro.whats_orders_manager.modules.inventario.controller;

import api.astro.whats_orders_manager.modules.inventario.dto.AjusteRequest;
import api.astro.whats_orders_manager.modules.inventario.model.AjusteInventario;
import api.astro.whats_orders_manager.modules.inventario.service.AjusteInventarioService;
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
@RequestMapping("/api/inventario/ajustes")
@RequiredArgsConstructor
public class AjusteInventarioRestController {

    private final AjusteInventarioService ajusteService;
    private final UsuarioService usuarioService;

    @PostMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'INVENTARIO_AJUSTAR')")
    public ResponseEntity<ResponseDTO> crear(@RequestBody AjusteRequest dto, Authentication authentication) {
        try {
            AjusteInventario ajuste = ajusteService.crear(dto, resolverUsuario(authentication));
            return ResponseEntity.ok(ResponseDTO.success("Ajuste " + ajuste.getNumero() + " creado en estado PENDIENTE", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/aprobar")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'INVENTARIO_AJUSTAR')")
    public ResponseEntity<ResponseDTO> aprobar(@PathVariable Long id, Authentication authentication) {
        try {
            AjusteInventario ajuste = ajusteService.aprobar(id, resolverUsuario(authentication));
            return ResponseEntity.ok(ResponseDTO.success("Ajuste " + ajuste.getNumero() + " aprobado — stock actualizado", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'INVENTARIO_AJUSTAR')")
    public ResponseEntity<ResponseDTO> rechazar(@PathVariable Long id, Authentication authentication) {
        try {
            AjusteInventario ajuste = ajusteService.rechazar(id, resolverUsuario(authentication));
            return ResponseEntity.ok(ResponseDTO.success("Ajuste " + ajuste.getNumero() + " rechazado", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    private Usuario resolverUsuario(Authentication authentication) {
        return usuarioService.findByEmail(authentication.getName())
            .orElseThrow(() -> new IllegalStateException("Usuario no encontrado: " + authentication.getName()));
    }
}
