package api.astro.whats_orders_manager.modules.seguridad;

import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioRepository;
import api.astro.whats_orders_manager.modules.seguridad.security.AppLogoutSuccessHandler;
import api.astro.whats_orders_manager.modules.seguridad.security.LoginFailureHandler;
import api.astro.whats_orders_manager.modules.seguridad.security.LoginSuccessHandler;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Integration-style tests asserting that UsuarioActividad audit rows are
 * triggered for LOGIN_SUCCESS, LOGIN_FAILURE, and LOGOUT events with IP
 * and the correct service method calls.
 *
 * Uses Mockito instead of a live DB to verify the wiring between handlers
 * and UsuarioActividadService without requiring MySQL connectivity.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioActividad audit wiring — handler → service integration")
class UsuarioActividadAuditIT {

    @Mock
    private UsuarioActividadService usuarioActividadService;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private static final String TEST_IP = "192.168.1.50";
    private static final String TEST_USERNAME = "user@test.com";
    private static final Integer TEST_USER_ID = 42;

    private Usuario testUser;

    @BeforeEach
    void setUp() {
        testUser = new Usuario();
        testUser.setIdUsuario(TEST_USER_ID);
        testUser.setTelefono(TEST_USERNAME);
        testUser.setIntentosFallidos(0);

        // Use lenient to avoid UnnecessaryStubbing when logout tests don't call getRemoteAddr()
        lenient().when(request.getRemoteAddr()).thenReturn(TEST_IP);
    }

    // ==================== LOGIN SUCCESS ====================

    @Test
    @DisplayName("LoginSuccessHandler calls registrarLogin with userId and IP on success")
    void loginSuccess_callsRegistrarLogin_withUserIdAndIp() throws Exception {
        when(usuarioRepository.findByTelefono(TEST_USERNAME)).thenReturn(Optional.of(testUser));

        LoginSuccessHandler handler = new LoginSuccessHandler(
                usuarioService, usuarioRepository, usuarioActividadService);

        Authentication auth = new UsernamePasswordAuthenticationToken(TEST_USERNAME, null);

        handler.onAuthenticationSuccess(request, response, auth);

        verify(usuarioActividadService).registrarLogin(eq(TEST_USER_ID), eq(TEST_IP), any());
    }

    @Test
    @DisplayName("LoginSuccessHandler passes non-null IP to registrarLogin")
    void loginSuccess_ipAddress_isNotNull() throws Exception {
        when(usuarioRepository.findByTelefono(TEST_USERNAME)).thenReturn(Optional.of(testUser));

        LoginSuccessHandler handler = new LoginSuccessHandler(
                usuarioService, usuarioRepository, usuarioActividadService);

        Authentication auth = new UsernamePasswordAuthenticationToken(TEST_USERNAME, null);

        handler.onAuthenticationSuccess(request, response, auth);

        verify(usuarioActividadService).registrarLogin(
                eq(TEST_USER_ID),
                argThat(ip -> ip != null && !ip.isBlank()),
                any()
        );
    }

    // ==================== LOGIN FAILURE ====================

    @Test
    @DisplayName("LoginFailureHandler calls registrarLoginFallido with identifier and IP on failure")
    void loginFailure_callsRegistrarLoginFallido_withIdentifierAndIp() throws Exception {
        when(request.getParameter("username")).thenReturn(TEST_USERNAME);
        when(usuarioRepository.findByTelefono(TEST_USERNAME)).thenReturn(Optional.of(testUser));

        LoginFailureHandler handler = new LoginFailureHandler(
                usuarioService, usuarioRepository, usuarioActividadService);

        AuthenticationException exception = new BadCredentialsException("Bad credentials");

        handler.onAuthenticationFailure(request, response, exception);

        verify(usuarioActividadService).registrarLoginFallido(eq(TEST_USERNAME), eq(TEST_IP), any());
    }

    @Test
    @DisplayName("LoginFailureHandler calls registrarLoginFallido even when user not found in DB")
    void loginFailure_userNotFound_stillCallsRegistrarLoginFallido() throws Exception {
        when(request.getParameter("username")).thenReturn("unknown@test.com");
        when(usuarioRepository.findByTelefono("unknown@test.com")).thenReturn(Optional.empty());

        LoginFailureHandler handler = new LoginFailureHandler(
                usuarioService, usuarioRepository, usuarioActividadService);

        AuthenticationException exception = new BadCredentialsException("Bad credentials");

        handler.onAuthenticationFailure(request, response, exception);

        verify(usuarioActividadService).registrarLoginFallido(eq("unknown@test.com"), eq(TEST_IP), any());
    }

    // ==================== LOGOUT ====================

    @Test
    @DisplayName("AppLogoutSuccessHandler calls registrarLogout with userId and IP on logout")
    void logout_callsRegistrarLogout_withUserIdAndIp() throws Exception {
        when(usuarioRepository.findByTelefono(TEST_USERNAME)).thenReturn(Optional.of(testUser));

        AppLogoutSuccessHandler handler = new AppLogoutSuccessHandler(
                usuarioActividadService, usuarioRepository);

        Authentication auth = new UsernamePasswordAuthenticationToken(TEST_USERNAME, null);

        handler.onLogoutSuccess(request, response, auth);

        verify(usuarioActividadService).registrarLogout(eq(TEST_USER_ID));
    }

    @Test
    @DisplayName("AppLogoutSuccessHandler handles null authentication gracefully")
    void logout_nullAuthentication_doesNotThrow() throws Exception {
        AppLogoutSuccessHandler handler = new AppLogoutSuccessHandler(
                usuarioActividadService, usuarioRepository);

        handler.onLogoutSuccess(request, response, null);

        // No exception thrown; registrarLogout not called when user is anonymous
        verify(usuarioActividadService, never()).registrarLogout(any());
    }
}
