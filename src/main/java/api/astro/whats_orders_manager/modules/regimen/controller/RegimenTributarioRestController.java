package api.astro.whats_orders_manager.modules.regimen.controller;

import api.astro.whats_orders_manager.modules.regimen.dto.RegimenTributarioDTO;
import api.astro.whats_orders_manager.modules.regimen.dto.RegimenTributarioVistaDTO;
import api.astro.whats_orders_manager.modules.regimen.model.RegimenTributario;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenNoDefinidoException;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenTributarioService;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import api.astro.whats_orders_manager.shared.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Historial de tramos de régimen tributario (T-056, ADR-072). Crear, corregir y borrar exigen
 * REGIMEN_TRIBUTARIO_GESTIONAR (solo ADMIN); leer el historial y el régimen vigente también lo permite CONFIG_VER. Los
 * errores salen como 400, 404 y 409 con el mensaje fijo del servicio.
 */
@RestController
@RequestMapping("/api/regimen-tributario")
@RequiredArgsConstructor
public class RegimenTributarioRestController {

    private static final String LEER = "@permisoService.tienePermisoPorCodigo(#authentication.name, 'REGIMEN_TRIBUTARIO_GESTIONAR') "
        + "or @permisoService.tienePermisoPorCodigo(#authentication.name, 'CONFIG_VER')";
    private static final String GESTIONAR = "@permisoService.tienePermisoPorCodigo(#authentication.name, 'REGIMEN_TRIBUTARIO_GESTIONAR')";

    private final RegimenTributarioService servicio;
    private final UsuarioService usuarioService;

    @GetMapping
    @PreAuthorize(LEER)
    public ResponseEntity<ResponseDTO> listar(Authentication authentication) {
        List<RegimenTributarioVistaDTO> datos = servicio.historial().stream()
            .map(e -> RegimenTributarioVistaDTO.de(e, nombreDe(e.tramo()))).toList();
        return ResponseEntity.ok(ResponseDTO.success("OK", datos));
    }

    /** El tramo que rige hoy; si no hay ninguno, 200 con {@code data} nula. */
    @GetMapping("/vigente")
    @PreAuthorize(LEER)
    public ResponseEntity<ResponseDTO> vigente(Authentication authentication) {
        try {
            return ResponseEntity.ok(ResponseDTO.success("OK", vista(servicio.vigenteHoy())));
        } catch (RegimenNoDefinidoException e) {
            return ResponseEntity.ok(ResponseDTO.success(e.getMessage(), null));
        }
    }

    @PostMapping
    @PreAuthorize(GESTIONAR)
    public ResponseEntity<ResponseDTO> crear(@RequestBody RegimenTributarioDTO dto, Authentication authentication) {
        try {
            RegimenTributario creado = servicio.crear(dto, resolverUsuario(authentication));
            return ResponseEntity.ok(ResponseDTO.success("Tramo de régimen registrado", vista(creado)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ResponseDTO.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize(GESTIONAR)
    public ResponseEntity<ResponseDTO> corregir(@PathVariable Long id, @RequestBody RegimenTributarioDTO dto,
                                                Authentication authentication) {
        try {
            RegimenTributario corregido = servicio.corregir(id, dto, resolverUsuario(authentication));
            return ResponseEntity.ok(ResponseDTO.success("Tramo de régimen corregido", vista(corregido)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseDTO.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ResponseDTO.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(GESTIONAR)
    public ResponseEntity<ResponseDTO> eliminar(@PathVariable Long id, Authentication authentication) {
        try {
            servicio.eliminar(id, resolverUsuario(authentication));
            return ResponseEntity.ok(ResponseDTO.success("Tramo de régimen eliminado", null));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseDTO.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ResponseDTO.error(e.getMessage()));
        }
    }

    private RegimenTributarioVistaDTO vista(RegimenTributario t) {
        return RegimenTributarioVistaDTO.de(t, nombreDe(t));
    }

    private String nombreDe(RegimenTributario t) {
        return t.getUsuarioId() == null ? null
            : usuarioService.findById(t.getUsuarioId()).map(Usuario::getNombre).orElse(null);
    }

    private Usuario resolverUsuario(Authentication authentication) {
        return usuarioService.findByEmail(authentication.getName())
            .orElseThrow(() -> new IllegalStateException("Usuario no encontrado: " + authentication.getName()));
    }
}
