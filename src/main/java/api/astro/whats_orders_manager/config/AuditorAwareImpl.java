package api.astro.whats_orders_manager.config;

import api.astro.whats_orders_manager.models.Usuario;
import api.astro.whats_orders_manager.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("auditorAwareImpl")
public class AuditorAwareImpl implements AuditorAware<Integer> {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public Optional<Integer> getCurrentAuditor() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }

            Object principal = authentication.getPrincipal();
            String telefono = null;

            if (principal instanceof UserDetails userDetails) {
                telefono = userDetails.getUsername(); // el username es el número de teléfono
            } else if (principal instanceof String s) {
                telefono = s;
            }

            if (telefono == null || telefono.equals("anonymousUser")) {
                return Optional.empty();
            }

            // Usar método especial que evita auto-flush y previene recursión infinita
            return usuarioRepository.findByTelefonoWithoutFlush(telefono)
                    .map(Usuario::getIdUsuario);
                    
        } catch (Exception e) {
            // En caso de cualquier error, retornamos Optional.empty()
            // Esto evita que falle la operación de guardado
            return Optional.empty();
        }
    }
}