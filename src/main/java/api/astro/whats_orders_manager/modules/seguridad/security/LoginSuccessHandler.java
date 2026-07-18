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
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioActividadService usuarioActividadService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        String username = authentication.getName();
        log.info("Authentication success for user: {}", username);

        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        Optional<Usuario> optUsuario = usuarioRepository.findByEmail(username);
        if (optUsuario.isPresent()) {
            Integer userId = optUsuario.get().getIdUsuario();
            usuarioService.resetearIntentosFallidos(userId);
            usuarioActividadService.registrarLogin(userId, ip, userAgent);
        }

        response.sendRedirect("/dashboard");
    }
}
