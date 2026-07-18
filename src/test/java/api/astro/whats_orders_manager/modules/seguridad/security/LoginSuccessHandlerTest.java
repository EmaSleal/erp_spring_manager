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
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginSuccessHandler — unit tests")
class LoginSuccessHandlerTest {

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

    @Mock
    private Authentication authentication;

    private LoginSuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new LoginSuccessHandler(usuarioService, usuarioRepository, usuarioActividadService);
    }

    @Test
    @DisplayName("onAuthenticationSuccess resets intentos fallidos when user exists")
    void onAuthenticationSuccess_userExists_resetsFailedAttempts() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setTelefono("88887777");
        usuario.setIntentosFallidos(3);

        when(authentication.getName()).thenReturn("88887777");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(usuarioRepository.findByTelefono("88887777")).thenReturn(Optional.of(usuario));

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(usuarioService).resetearIntentosFallidos(1);
    }

    @Test
    @DisplayName("onAuthenticationSuccess does nothing when user not found")
    void onAuthenticationSuccess_userNotFound_doesNothing() throws Exception {
        when(authentication.getName()).thenReturn("unknown");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(usuarioRepository.findByTelefono("unknown")).thenReturn(Optional.empty());

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(usuarioService, never()).resetearIntentosFallidos(any());
    }
}
