package api.astro.whats_orders_manager.modules.contabilidad.controller;

import api.astro.whats_orders_manager.modules.contabilidad.dto.ProrrataIvaDTO;
import api.astro.whats_orders_manager.modules.contabilidad.dto.ProrrataIvaVistaDTO;
import api.astro.whats_orders_manager.modules.contabilidad.model.ProrrataIva;
import api.astro.whats_orders_manager.modules.contabilidad.service.ProrrataIvaService;
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
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Porcentaje provisional de prorrata del IVA por año (T-054, ADR-067 §4). Listar, dar de alta y corregir exigen
 * PRORRATA_IVA_GESTIONAR; consultar el porcentaje de un año también lo permite CUENTA_PAGAR_CREAR, porque el formulario de
 * CxP lo necesita para avisar cuando falta. Corregir un porcentaje no recalcula las CxP ya registradas.
 */
@RestController
@RequestMapping("/api/prorrata-iva")
@RequiredArgsConstructor
public class ProrrataIvaRestController {

    private final ProrrataIvaService prorrataIvaService;
    private final UsuarioService usuarioService;

    @GetMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PRORRATA_IVA_GESTIONAR')")
    public ResponseEntity<ResponseDTO> listar(Authentication authentication) {
        List<ProrrataIvaVistaDTO> datos = prorrataIvaService.listar().stream().map(this::vista).toList();
        return ResponseEntity.ok(ResponseDTO.success("OK", datos));
    }

    /** {@code configurado} indica si el año tiene porcentaje; sin él, solo vienen el año y {@code configurado=false}. */
    @GetMapping("/{anio}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PRORRATA_IVA_GESTIONAR') "
        + "or @permisoService.tienePermisoPorCodigo(#authentication.name, 'CUENTA_PAGAR_CREAR')")
    public ResponseEntity<ResponseDTO> porAnio(@PathVariable Integer anio, Authentication authentication) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("anio", anio);
        var prorrata = prorrataIvaService.buscar(anio);
        data.put("configurado", prorrata.isPresent());
        prorrata.ifPresent(p -> data.put("porcentaje", p.getPorcentaje()));
        return ResponseEntity.ok(ResponseDTO.success("OK", data));
    }

    @PostMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PRORRATA_IVA_GESTIONAR')")
    public ResponseEntity<ResponseDTO> crear(@RequestBody ProrrataIvaDTO dto, Authentication authentication) {
        try {
            ProrrataIva creada = prorrataIvaService.crear(dto, resolverUsuario(authentication));
            return ResponseEntity.ok(ResponseDTO.success("Prorrata del IVA de " + creada.getAnio() + " registrada", vista(creada)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ResponseDTO.error(e.getMessage()));
        }
    }

    @PutMapping("/{anio}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PRORRATA_IVA_GESTIONAR')")
    public ResponseEntity<ResponseDTO> corregir(@PathVariable Integer anio, @RequestBody ProrrataIvaDTO dto,
                                                Authentication authentication) {
        try {
            ProrrataIva corregida = prorrataIvaService.corregir(anio, dto, resolverUsuario(authentication));
            return ResponseEntity.ok(ResponseDTO.success("Prorrata del IVA de " + anio + " corregida", vista(corregida)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseDTO.error(e.getMessage()));
        }
    }

    private ProrrataIvaVistaDTO vista(ProrrataIva p) {
        String nombre = p.getUsuarioId() == null ? null
            : usuarioService.findById(p.getUsuarioId()).map(Usuario::getNombre).orElse(null);
        return ProrrataIvaVistaDTO.de(p, nombre);
    }

    private Usuario resolverUsuario(Authentication authentication) {
        return usuarioService.findByEmail(authentication.getName())
            .orElseThrow(() -> new IllegalStateException("Usuario no encontrado: " + authentication.getName()));
    }
}
