package api.astro.whats_orders_manager.modules.seguridad.config;

import api.astro.whats_orders_manager.modules.seguridad.model.Rol;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.repository.RolRepository;
import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Rol rolAdmin = rolRepository.findByCodigo("ADMIN").orElseGet(() -> {
            log.info("DataInitializer: creating ADMIN rol");
            Rol r = new Rol();
            r.setCodigo("ADMIN");
            r.setNombre("Administrador");
            r.setDescripcion("Rol con acceso total al sistema");
            r.setActivo(true);
            return rolRepository.save(r);
        });

        if (usuarioRepository.findByNombre("usuario prueba").isEmpty()) {
            log.info("DataInitializer: creating test user");
            Usuario u = new Usuario();
            u.setNombre("usuario prueba");
            u.setPassword(passwordEncoder.encode("JhfKHZ2%mJMI"));
            u.setRolEntity(rolAdmin);
            u.setActivo(true);
            u.setBloqueado(false);
            u.setIntentosFallidos(0);
            u.setRequireCambioPassword(false);
            u.setTelefono("1234567890");
            usuarioRepository.save(u);
        }
    }
}
