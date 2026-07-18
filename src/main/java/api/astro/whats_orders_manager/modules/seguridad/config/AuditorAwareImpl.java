package api.astro.whats_orders_manager.modules.seguridad.config;

import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioRepository;
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
            String email = null;

            if (principal instanceof UserDetails userDetails) {
                email = userDetails.getUsername(); // username is now the email
            } else if (principal instanceof String s) {
                email = s;
            }

            if (email == null || email.equals("anonymousUser")) {
                return Optional.empty();
            }

            // Use email-based lookup with COMMIT flush hint to prevent JPA flush recursion
            return usuarioRepository.findByEmailWithoutFlush(email)
                    .map(Usuario::getIdUsuario);
                    
        } catch (Exception e) {
            // En caso de cualquier error, retornamos Optional.empty()
            // Esto evita que falle la operación de guardado
            return Optional.empty();
        }
    }
}