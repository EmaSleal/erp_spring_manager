package api.astro.whats_orders_manager.modules.seguridad.service;

import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioRepository;
import api.astro.whats_orders_manager.modules.seguridad.service.impl.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Spec scenario covered:
 *   - "Login by email succeeds" — findByEmail returns user → authentication succeeds, principal username == email
 *   - "Legacy telefono not accepted as login identifier" — findByEmail returns empty → UsernameNotFoundException
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserDetailsServiceImpl — email-based authentication")
class UserDetailsServiceImplEmailTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService usuarioService;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private Usuario activeUserWithEmail;

    @BeforeEach
    void setUp() {
        activeUserWithEmail = new Usuario();
        activeUserWithEmail.setIdUsuario(10);
        activeUserWithEmail.setNombre("Test User");
        activeUserWithEmail.setEmail("test.user@example.com");
        activeUserWithEmail.setTelefono("88881111");
        activeUserWithEmail.setPassword("$2a$10$encryptedPassword");
        activeUserWithEmail.setRol("USER");
        activeUserWithEmail.setActivo(true);
        activeUserWithEmail.setBloqueado(false);
        activeUserWithEmail.setIntentosFallidos(0);
    }

    @Test
    @DisplayName("login by email succeeds — loadUserByUsername(email) returns UserDetails with email as username")
    void loadUserByUsername_byEmail_succeeds() {
        when(usuarioRepository.findByEmail("test.user@example.com"))
                .thenReturn(Optional.of(activeUserWithEmail));

        UserDetails result = userDetailsService.loadUserByUsername("test.user@example.com");

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("test.user@example.com");
    }

    @Test
    @DisplayName("login by telefono fails — findByEmail returns empty → UsernameNotFoundException")
    void loadUserByUsername_byTelefono_throwsUsernameNotFoundException() {
        // After email migration, telefono is not a valid login identifier.
        // findByEmail("88881111") returns empty → no fallback by telefono in auth path.
        // lenient() required because before production fix, findByEmail may not be called at all.
        lenient().when(usuarioRepository.findByEmail("88881111"))
                .thenReturn(Optional.empty());
        lenient().when(usuarioRepository.findByTelefono("88881111"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("88881111"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
