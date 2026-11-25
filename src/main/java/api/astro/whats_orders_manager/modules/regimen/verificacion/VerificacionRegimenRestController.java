package api.astro.whats_orders_manager.modules.regimen.verificacion;

import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import api.astro.whats_orders_manager.shared.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Verificación del régimen con Hacienda (T-056, ADR-072). Ejecutarla a mano exige REGIMEN_TRIBUTARIO_GESTIONAR; leer la
 * última también lo permite CONFIG_VER. Solo avisa: nunca cambia el régimen.
 */
@RestController
@RequestMapping("/api/regimen/verificacion")
@RequiredArgsConstructor
public class VerificacionRegimenRestController {

    private static final String LEER = "@permisoService.tienePermisoPorCodigo(#authentication.name, 'REGIMEN_TRIBUTARIO_GESTIONAR') "
        + "or @permisoService.tienePermisoPorCodigo(#authentication.name, 'CONFIG_VER')";
    private static final String GESTIONAR = "@permisoService.tienePermisoPorCodigo(#authentication.name, 'REGIMEN_TRIBUTARIO_GESTIONAR')";

    private final VerificacionRegimenService servicio;
    private final UsuarioService usuarioService;

    /** La última verificación; si no hay ninguna, 200 con {@code data} nula. */
    @GetMapping
    @PreAuthorize(LEER)
    public ResponseEntity<ResponseDTO> ultima(Authentication authentication) {
        return ResponseEntity.ok(ResponseDTO.success("OK", servicio.ultima().map(this::vista).orElse(null)));
    }

    @PostMapping
    @PreAuthorize(GESTIONAR)
    public ResponseEntity<ResponseDTO> verificar(Authentication authentication) {
        Integer idUsuario = usuarioService.findByEmail(authentication.getName()).map(Usuario::getIdUsuario).orElse(null);
        VerificacionRegimen v = servicio.verificar(idUsuario);
        return ResponseEntity.ok(ResponseDTO.success("Verificación con Hacienda realizada", vista(v)));
    }

    private VerificacionRegimenVistaDTO vista(VerificacionRegimen v) {
        String nombre = v.getUsuarioId() == null ? null
            : usuarioService.findById(v.getUsuarioId()).map(Usuario::getNombre).orElse(null);
        return VerificacionRegimenVistaDTO.de(v, nombre);
    }
}
