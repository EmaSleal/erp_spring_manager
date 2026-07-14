package api.astro.whats_orders_manager.modules.proveedores.controller;

import api.astro.whats_orders_manager.modules.proveedores.dto.ProveedorDTO;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import api.astro.whats_orders_manager.modules.proveedores.service.ProveedorService;
import api.astro.whats_orders_manager.shared.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/proveedores")
@RequiredArgsConstructor
public class ProveedorRestController {

    private final ProveedorService proveedorService;

    @GetMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PROVEEDOR_VER')")
    public ResponseEntity<ResponseDTO> listar(
            @RequestParam(defaultValue = "false") boolean soloActivos,
            Authentication authentication
    ) {
        List<Proveedor> lista = soloActivos
            ? proveedorService.findActivos()
            : proveedorService.findAll();
        return ResponseEntity.ok(ResponseDTO.success("OK", lista));
    }

    @GetMapping("/buscar")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PROVEEDOR_VER')")
    public ResponseEntity<ResponseDTO> buscar(
            @RequestParam String q,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ResponseDTO.success("OK", proveedorService.buscar(q)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PROVEEDOR_VER')")
    public ResponseEntity<ResponseDTO> findById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        try {
            return ResponseEntity.ok(ResponseDTO.success("OK", proveedorService.findById(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PROVEEDOR_CREAR')")
    public ResponseEntity<ResponseDTO> crear(
            @RequestBody ProveedorDTO dto,
            Authentication authentication
    ) {
        try {
            Proveedor p = proveedorService.crear(dto);
            return ResponseEntity.ok(ResponseDTO.success("Proveedor creado exitosamente", p));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PROVEEDOR_EDITAR')")
    public ResponseEntity<ResponseDTO> actualizar(
            @PathVariable Long id,
            @RequestBody ProveedorDTO dto,
            Authentication authentication
    ) {
        try {
            Proveedor p = proveedorService.actualizar(id, dto);
            return ResponseEntity.ok(ResponseDTO.success("Proveedor actualizado exitosamente", p));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PROVEEDOR_ELIMINAR')")
    public ResponseEntity<ResponseDTO> desactivar(
            @PathVariable Long id,
            Authentication authentication
    ) {
        try {
            proveedorService.desactivar(id);
            return ResponseEntity.ok(ResponseDTO.success("Proveedor desactivado exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }
}
