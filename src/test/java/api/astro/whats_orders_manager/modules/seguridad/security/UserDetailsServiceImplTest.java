package api.astro.whats_orders_manager.modules.seguridad.security;

import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioRepository;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import api.astro.whats_orders_manager.modules.seguridad.service.impl.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDetailsServiceImpl — unit tests")
class UserDetailsServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private Usuario activeUser;
    private Usuario blockedUser;
    private Usuario inactiveUser;

    @BeforeEach
    void setUp() {
        activeUser = new Usuario();
        activeUser.setIdUsuario(1);
        activeUser.setEmail("juan@example.com");
        activeUser.setTelefono("88887777");
        activeUser.setNombre("Juan");
        activeUser.setPassword("$2a$10$encryptedPassword");
        activeUser.setRol("USER");
        activeUser.setActivo(true);
        activeUser.setBloqueado(false);
        activeUser.setIntentosFallidos(0);

        blockedUser = new Usuario();
        blockedUser.setIdUsuario(2);
        blockedUser.setEmail("ana@example.com");
        blockedUser.setTelefono("99998888");
        blockedUser.setNombre("Ana");
        blockedUser.setPassword("$2a$10$encryptedPassword2");
        blockedUser.setRol("USER");
        blockedUser.setActivo(true);
        blockedUser.setBloqueado(true);
        blockedUser.setIntentosFallidos(6);

        inactiveUser = new Usuario();
        inactiveUser.setIdUsuario(3);
        inactiveUser.setEmail("pedro@example.com");
        inactiveUser.setTelefono("77776666");
        inactiveUser.setNombre("Pedro");
        inactiveUser.setPassword("$2a$10$encryptedPassword3");
        inactiveUser.setRol("USER");
        inactiveUser.setActivo(false);
        inactiveUser.setBloqueado(false);
    }

    @Test
    @DisplayName("loadUserByUsername throws LockedException when user is bloqueado=true")
    void loadUserByUsername_bloqueado_throwsLockedException() {
        when(usuarioRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(blockedUser));

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("ana@example.com"))
                .isInstanceOf(LockedException.class);
    }

    @Test
    @DisplayName("loadUserByUsername throws UsernameNotFoundException when user is inactive")
    void loadUserByUsername_inactivo_throwsUsernameNotFoundException() {
        when(usuarioRepository.findByEmail("pedro@example.com")).thenReturn(Optional.of(inactiveUser));

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("pedro@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("loadUserByUsername returns UserDetails when user is active and not blocked — username is email")
    void loadUserByUsername_activeAndNotBlocked_returnsUserDetails() {
        when(usuarioRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(activeUser));

        var result = userDetailsService.loadUserByUsername("juan@example.com");

        org.assertj.core.api.Assertions.assertThat(result).isNotNull();
        org.assertj.core.api.Assertions.assertThat(result.getUsername()).isEqualTo("juan@example.com");
    }
}
