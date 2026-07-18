package api.astro.whats_orders_manager.modules.seguridad;

import api.astro.whats_orders_manager.modules.seguridad.config.AuditorAwareImpl;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Spec scenario:
 *   "AuditorAware resolves without flush recursion"
 *   — AuditorAwareImpl MUST call findByEmailWithoutFlush (COMMIT hint) and NOT findByTelefonoWithoutFlush.
 *   The COMMIT hint itself is verified by checking which repository method is called.
 *
 * This test is a unit test (no DB needed), using Mockito to verify the call path.
 * A @DataJpaTest verifying no recursive flush at the JPA layer is the integration counterpart,
 * but the contract check (correct method invoked) is validated here.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditorAwareImpl — uses findByEmailWithoutFlush (COMMIT hint)")
class AuditorAwareImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private AuditorAwareImpl auditorAware;

    private Usuario usuario;

    @BeforeEach
    void setUpUser() {
        usuario = new Usuario();
        usuario.setIdUsuario(42);
        usuario.setEmail("auditor@example.com");
        usuario.setTelefono("88880000");
        usuario.setNombre("Auditor User");
        usuario.setActivo(true);
        usuario.setBloqueado(false);
    }

    @BeforeEach
    void setUpSecurityContext() {
        var userDetails = User.withUsername("auditor@example.com")
                .password("irrelevant")
                .roles("ADMIN")
                .build();
        var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getCurrentAuditor calls findByEmailWithoutFlush — COMMIT hint method")
    void getCurrentAuditor_callsEmailWithoutFlush() {
        when(usuarioRepository.findByEmailWithoutFlush("auditor@example.com"))
                .thenReturn(Optional.of(usuario));

        Optional<Integer> result = auditorAware.getCurrentAuditor();

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(42);
        verify(usuarioRepository).findByEmailWithoutFlush("auditor@example.com");
    }

    @Test
    @DisplayName("getCurrentAuditor does NOT call findByTelefonoWithoutFlush")
    void getCurrentAuditor_doesNotCallTelefonoWithoutFlush() {
        when(usuarioRepository.findByEmailWithoutFlush("auditor@example.com"))
                .thenReturn(Optional.of(usuario));

        auditorAware.getCurrentAuditor();

        verify(usuarioRepository, never()).findByTelefonoWithoutFlush(anyString());
    }
}
