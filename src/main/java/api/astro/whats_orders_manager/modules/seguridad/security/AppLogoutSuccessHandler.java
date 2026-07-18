package api.astro.whats_orders_manager.modules.seguridad.security;

import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioRepository;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioActividadService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Logout success handler — records a LOGOUT audit entry via UsuarioActividadService.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppLogoutSuccessHandler implements LogoutSuccessHandler {

    private final UsuarioActividadService usuarioActividadService;
    private final UsuarioRepository usuarioRepository;

    @Override
    public void onLogoutSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        if (authentication != null) {
            String email = authentication.getName(); // username is now email
            log.info("Logout for user: {}", email);

            usuarioRepository.findByEmail(email).ifPresent(usuario ->
                    usuarioActividadService.registrarLogout(usuario.getIdUsuario())
            );
        } else {
            log.info("Logout for anonymous user");
        }

        response.sendRedirect("/auth/login?logout");
    }
}
