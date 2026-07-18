package api.astro.whats_orders_manager.modules.seguridad.service.impl;

import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioRepository;

import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // Authenticate by email (primary identity). Fallback to nombre for compatibility.
        Usuario usuario = usuarioRepository.findByEmail(usernameOrEmail)
                .or(() -> usuarioRepository.findByNombre(usernameOrEmail))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + usernameOrEmail));

        // Verificar si el usuario está activo
        if (usuario.getActivo() == null || !usuario.getActivo()) {
            throw new UsernameNotFoundException("Usuario inactivo: " + usernameOrEmail);
        }

        boolean bloqueado = usuario.getBloqueado() != null && usuario.getBloqueado();

        if (!bloqueado) {
            actualizarUltimoAcceso(usuario);
        }

        return User.withUsername(usuario.getEmail())
                .password(usuario.getPassword())
                .roles(usuario.getRol())
                .accountLocked(bloqueado)
                .build();
    }

    /**
     * Actualiza el campo ultimo_acceso del usuario con la fecha y hora actual.
     * Este método se ejecuta cada vez que el usuario inicia sesión exitosamente.
     * 
     * @param usuario El usuario que acaba de autenticarse
     */
    private void actualizarUltimoAcceso(Usuario usuario) {
        try {
            LocalDateTime now = LocalDateTime.now();
            usuario.setUltimoAcceso(now);
            usuarioRepository.save(usuario);
            log.info("Último acceso actualizado para usuario: {} (ID: {}) - Timestamp: {}", 
                    usuario.getNombre(), usuario.getIdUsuario(), now);
        } catch (Exception e) {
            // Log del error pero no interrumpir el login
            log.error("Error al actualizar último acceso para usuario {} (ID: {}): {}", 
                    usuario.getNombre(), usuario.getIdUsuario(), e.getMessage(), e);
        }
    }

    public Optional<Integer> getCurrentUserID(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        String email = null;

        if (principal instanceof UserDetails userDetails) {
            email = userDetails.getUsername(); // username is now the email
        } else if (principal instanceof String s) {
            email = s;
        }

        if (email == null) return Optional.empty();

        // Resolve user ID by email
        Integer userId = usuarioService.findByEmail(email)
                .map(usuario -> usuario.getIdUsuario())
                .orElse(null);

        if (userId == null) return Optional.empty();

        return userId.toString().isEmpty() ? Optional.empty() : Optional.of(userId);
    }
}