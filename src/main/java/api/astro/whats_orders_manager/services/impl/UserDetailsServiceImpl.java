package api.astro.whats_orders_manager.services.impl;

import api.astro.whats_orders_manager.models.Usuario;
import api.astro.whats_orders_manager.repositories.UsuarioRepository;

import api.astro.whats_orders_manager.services.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Optional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Override
    public UserDetails loadUserByUsername(String nombre) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByNombre(nombre)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + nombre));

        // Actualizar último acceso
        actualizarUltimoAcceso(usuario);

        return User.withUsername(usuario.getTelefono())
                .password(usuario.getPassword()) // La contraseña debe estar encriptada en la BD
                .roles(usuario.getRol()) // Se usa el rol almacenado en la BD
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
            usuario.setUltimoAcceso(new Timestamp(System.currentTimeMillis()));
            usuarioRepository.save(usuario);
        } catch (Exception e) {
            // Log del error pero no interrumpir el login
            System.err.println("Error al actualizar último acceso para usuario " + usuario.getTelefono() + ": " + e.getMessage());
        }
    }

    public Optional<Integer> getCurrentUserID(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        String telefono = null;

        if (principal instanceof UserDetails userDetails) {
            //log.info("UserDetails: {}", userDetails);
            telefono = userDetails.getUsername(); // el username aquí puede ser tu número de teléfono
        } else if (principal instanceof String s) {
            telefono = s;
        }

        if (telefono == null) return Optional.empty();

        // Buscar el ID de usuario usando el teléfono

        Integer userId = usuarioService.findByTelefono(telefono)
                .map(usuario -> usuario.getIdUsuario())
                .orElse(null);

        if (userId == null) return Optional.empty();

        return userId.toString().isEmpty() ? Optional.empty() : Optional.of(userId);
    }
}