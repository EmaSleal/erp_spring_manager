package api.astro.whats_orders_manager.modules.facturacion.controller;

import api.astro.whats_orders_manager.modules.facturacion.dto.ComprobanteExternoDTO;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.service.ComprobanteExternoService;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import api.astro.whats_orders_manager.shared.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Clave del comprobante de una factura GENERAL emitido fuera del ERP (T-057, ADR-076 §3-4). Registrar, corregir y quitar
 * exigen FACTURA_EDITAR. Los errores salen con el mensaje fijo del servicio: 400 (clave o fecha inválidas), 404 (la factura
 * no existe) y 409 (SIMPLIFICADO, anulada, con comprobante del ERP no rechazado, clave duplicada, sin clave que quitar).
 */
@RestController
@RequestMapping("/api/facturas/{idFactura}/comprobante-externo")
@RequiredArgsConstructor
public class ComprobanteExternoRestController {

    private static final String EDITAR = "@permisoService.tienePermisoPorCodigo(#authentication.name, 'FACTURA_EDITAR')";

    private final ComprobanteExternoService servicio;
    private final UsuarioService usuarioService;

    @PutMapping
    @PreAuthorize(EDITAR)
    public ResponseEntity<ResponseDTO> registrar(@PathVariable Integer idFactura, @RequestBody ComprobanteExternoDTO dto,
                                                 Authentication authentication) {
        try {
            Factura f = servicio.registrar(idFactura, dto.getClave(), dto.getFecha(), resolverUsuario(authentication));
            return ResponseEntity.ok(ResponseDTO.success("Clave del comprobante externo registrada", datos(f)));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    @DeleteMapping
    @PreAuthorize(EDITAR)
    public ResponseEntity<ResponseDTO> quitar(@PathVariable Integer idFactura, Authentication authentication) {
        try {
            Factura f = servicio.quitar(idFactura, resolverUsuario(authentication));
            return ResponseEntity.ok(ResponseDTO.success("Clave del comprobante externo quitada", datos(f)));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    private static ResponseEntity<ResponseDTO> error(RuntimeException e) {
        if (e instanceof IllegalArgumentException) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
        if (e instanceof NoSuchElementException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseDTO.error(e.getMessage()));
        }
        if (e instanceof IllegalStateException) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ResponseDTO.error(e.getMessage()));
        }
        throw e;
    }

    private static Map<String, Object> datos(Factura f) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("idFactura", f.getIdFactura());
        datos.put("comprobanteExternoClave", f.getComprobanteExternoClave());
        datos.put("comprobanteExternoFecha", f.getComprobanteExternoFecha());
        return datos;
    }

    private Usuario resolverUsuario(Authentication authentication) {
        return usuarioService.findByEmail(authentication.getName())
            .orElseThrow(() -> new IllegalStateException("Usuario no encontrado: " + authentication.getName()));
    }
}
