package api.astro.whats_orders_manager.modules.regimen.tope;

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
 * Parámetros por año del tope de compras del RTS y su estado (T-056, ADR-072). Todo exige REGIMEN_TOPE_COMPRAS_GESTIONAR
 * (solo ADMIN). Los errores salen como 400, 404 y 409 con el mensaje fijo del servicio.
 */
@RestController
@RequestMapping("/api/tope-compras-rts")
@RequiredArgsConstructor
public class TopeComprasRtsRestController {

    private static final String GESTIONAR = "@permisoService.tienePermisoPorCodigo(#authentication.name, 'REGIMEN_TOPE_COMPRAS_GESTIONAR')";

    private final TopeComprasRtsService servicio;
    private final UsuarioService usuarioService;

    @GetMapping
    @PreAuthorize(GESTIONAR)
    public ResponseEntity<ResponseDTO> listar(Authentication authentication) {
        List<TopeComprasRtsVistaDTO> datos = servicio.listar().stream().map(this::vista).toList();
        return ResponseEntity.ok(ResponseDTO.success("OK", datos));
    }

    /** Acumulado, tope y porcentaje del año, con la advertencia de que solo cuenta lo registrado en el ERP. */
    @GetMapping("/{anio}/estado")
    @PreAuthorize(GESTIONAR)
    public ResponseEntity<ResponseDTO> estado(@PathVariable Integer anio, Authentication authentication) {
        EstadoTope e = servicio.estado(anio);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("anio", e.anio());
        data.put("configurado", e.configurado());
        data.put("tope", e.tope());
        data.put("acumulado", e.acumulado());
        data.put("porcentaje", e.porcentaje());
        data.put("nivel", e.nivel());
        data.put("umbralAviso", e.umbralAviso());
        data.put("umbralTope", e.umbralTope());
        data.put("mensaje", e.mensaje());
        data.put("advertencia", TopeComprasRtsService.ADVERTENCIA_SOLO_ERP);
        return ResponseEntity.ok(ResponseDTO.success("OK", data));
    }

    @PostMapping
    @PreAuthorize(GESTIONAR)
    public ResponseEntity<ResponseDTO> crear(@RequestBody TopeComprasRtsDTO dto, Authentication authentication) {
        try {
            TopeComprasRts creado = servicio.crear(dto, resolverUsuario(authentication));
            return ResponseEntity.ok(ResponseDTO.success("Tope de compras del RTS de " + creado.getAnio() + " registrado", vista(creado)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ResponseDTO.error(e.getMessage()));
        }
    }

    @PutMapping("/{anio}")
    @PreAuthorize(GESTIONAR)
    public ResponseEntity<ResponseDTO> corregir(@PathVariable Integer anio, @RequestBody TopeComprasRtsDTO dto,
                                                Authentication authentication) {
        try {
            TopeComprasRts corregido = servicio.corregir(anio, dto, resolverUsuario(authentication));
            return ResponseEntity.ok(ResponseDTO.success("Tope de compras del RTS de " + anio + " corregido", vista(corregido)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseDTO.error(e.getMessage()));
        }
    }

    private TopeComprasRtsVistaDTO vista(TopeComprasRts t) {
        String nombre = t.getUsuarioId() == null ? null
            : usuarioService.findById(t.getUsuarioId()).map(Usuario::getNombre).orElse(null);
        return TopeComprasRtsVistaDTO.de(t, nombre);
    }

    private Usuario resolverUsuario(Authentication authentication) {
        return usuarioService.findByEmail(authentication.getName())
            .orElseThrow(() -> new IllegalStateException("Usuario no encontrado: " + authentication.getName()));
    }
}
