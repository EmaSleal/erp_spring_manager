package api.astro.whats_orders_manager.modules.seguridad.security;

import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioRepository;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioActividadService;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginFailureHandler — unit tests")
class LoginFailureHandlerTest {

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioActividadService usuarioActividadService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private LoginFailureHandler handler;
    private AuthenticationException exception;

    @BeforeEach
    void setUp() {
        handler = new LoginFailureHandler(usuarioService, usuarioRepository, usuarioActividadService);
        exception = new BadCredentialsException("Bad credentials");
    }

    @Test
    @DisplayName("onAuthenticationFailure increments intentos fallidos when user exists")
    void onAuthenticationFailure_userExists_incrementsFailedAttempts() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setTelefono("88887777");
        usuario.setIntentosFallidos(0);

        when(request.getParameter("username")).thenReturn("88887777");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(usuarioRepository.findByTelefono("88887777")).thenReturn(Optional.of(usuario));

        handler.onAuthenticationFailure(request, response, exception);

        verify(usuarioService).incrementarIntentosFallidos(1);
    }

    @Test
    @DisplayName("onAuthenticationFailure does not call incrementarIntentosFallidos when user not found")
    void onAuthenticationFailure_userNotFound_doesNothing() throws Exception {
        when(request.getParameter("username")).thenReturn("unknown");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(usuarioRepository.findByTelefono("unknown")).thenReturn(Optional.empty());

        handler.onAuthenticationFailure(request, response, exception);

        verify(usuarioService, never()).incrementarIntentosFallidos(any());
    }

    @Test
    @DisplayName("onAuthenticationFailure calls bloquear on 6th consecutive failed attempt")
    void onAuthenticationFailure_sixthAttempt_blocksUser() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(2);
        usuario.setTelefono("99998888");
        usuario.setIntentosFallidos(5); // already has 5 — next call brings count to 6

        when(request.getParameter("username")).thenReturn("99998888");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(usuarioRepository.findByTelefono("99998888")).thenReturn(Optional.of(usuario));

        handler.onAuthenticationFailure(request, response, exception);

        verify(usuarioService).incrementarIntentosFallidos(2);
        // bloquear() is called on the entity; handler saves via repository
        verify(usuarioRepository).save(argThat(u -> u.getBloqueado() && "Superó intentos".equals(u.getRazonBloqueo())));
    }

    @Test
    @DisplayName("onAuthenticationFailure does NOT block user before the 6th attempt")
    void onAuthenticationFailure_beforeSixthAttempt_doesNotBlock() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(3);
        usuario.setTelefono("77776666");
        usuario.setIntentosFallidos(4); // 4 already — after increment -> 5 (not yet threshold)

        when(request.getParameter("username")).thenReturn("77776666");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(usuarioRepository.findByTelefono("77776666")).thenReturn(Optional.of(usuario));

        handler.onAuthenticationFailure(request, response, exception);

        verify(usuarioService).incrementarIntentosFallidos(3);
        verify(usuarioRepository, never()).save(any());
    }
}
