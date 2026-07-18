package api.astro.whats_orders_manager.modules.seguridad.security;

import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioRepository;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioActividadService;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginFailureHandler implements AuthenticationFailureHandler {

    static final int FAILURE_THRESHOLD = 6;

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioActividadService usuarioActividadService;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {

        String identifier = request.getParameter("username");
        log.warn("Authentication failure for user: {}", identifier);

        String ip = (request != null) ? request.getRemoteAddr() : "UNKNOWN";
        String motivo = exception.getMessage();

        if (identifier == null || identifier.isBlank()) {
            usuarioActividadService.registrarLoginFallido("", ip, motivo);
            redirectIfPresent(response, "/auth/login?error=true");
            return;
        }

        usuarioActividadService.registrarLoginFallido(identifier, ip, motivo);

        if (exception instanceof LockedException) {
            log.warn("Login attempt on locked account: {}", identifier);
            redirectIfPresent(response, "/auth/login?locked=true");
            return;
        }

        Optional<Usuario> optUsuario = usuarioRepository.findByEmail(identifier)
                .or(() -> usuarioRepository.findByNombre(identifier));
        if (optUsuario.isEmpty()) {
            redirectIfPresent(response, "/auth/login?error=true");
            return;
        }

        Usuario usuario = optUsuario.get();

        // Capture count before increment to compute post-increment value without a second DB read
        int priorCount = (usuario.getIntentosFallidos() == null ? 0 : usuario.getIntentosFallidos());
        usuarioService.incrementarIntentosFallidos(usuario.getIdUsuario());
        int postIncrementCount = priorCount + 1;

        if (postIncrementCount >= FAILURE_THRESHOLD) {
            log.warn("User {} reached failure threshold ({}) — locking account",
                    usuario.getNombre(), postIncrementCount);
            usuario.bloquear("Superó intentos", null);
            usuarioRepository.save(usuario);
        }

        redirectIfPresent(response, "/auth/login?error=true");
    }

    private void redirectIfPresent(HttpServletResponse response, String url) throws IOException {
        if (response != null) {
            response.sendRedirect(url);
        }
    }
}
