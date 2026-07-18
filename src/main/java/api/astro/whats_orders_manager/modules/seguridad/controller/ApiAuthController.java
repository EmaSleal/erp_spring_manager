package api.astro.whats_orders_manager.modules.seguridad.controller;

import api.astro.whats_orders_manager.modules.seguridad.dto.LoginRequest;
import api.astro.whats_orders_manager.modules.seguridad.dto.LoginResponse;
import api.astro.whats_orders_manager.modules.seguridad.enums.Permiso;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioRepository;
import api.astro.whats_orders_manager.modules.seguridad.security.LoginFailureHandler;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class ApiAuthController {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final PermisoService permisoService;
    private final SecurityContextRepository securityContextRepository;
    private final LoginFailureHandler loginFailureHandler;

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, httpRequest, httpResponse);

            String email = authentication.getName(); // username is now email
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado tras autenticación"));

            List<String> permisos = permisoService.obtenerPermisos(usuario)
                    .stream()
                    .map(Permiso::name)
                    .sorted()
                    .toList();

            log.info("API login exitoso para usuario: {}", email);

            return ResponseEntity.ok(new LoginResponse(
                    usuario.getIdUsuario(),
                    usuario.getNombre(),
                    usuario.getEmail(),
                    usuario.getRol(),
                    permisos
            ));

        } catch (BadCredentialsException e) {
            log.warn("API login fallido para usuario: {} - credenciales inválidas", request.username());
            try {
                // null response: REST path returns its own ResponseEntity — LoginFailureHandler is null-safe (no redirect issued)
                loginFailureHandler.onAuthenticationFailure(httpRequest, null, e);
            } catch (Exception handlerEx) {
                log.debug("Failure handler processing for REST path: {}", handlerEx.getMessage());
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas"));

        } catch (LockedException e) {
            log.warn("API login fallido para usuario: {} - cuenta bloqueada", request.username());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Cuenta bloqueada"));

        } catch (DisabledException e) {
            log.warn("API login fallido para usuario: {} - usuario inactivo", request.username());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Usuario inactivo o bloqueado"));

        } catch (Exception e) {
            log.error("Error en API login: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al procesar el login"));
        }
    }
}
