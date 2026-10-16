package api.astro.whats_orders_manager.modules.seguridad.config;

import api.astro.whats_orders_manager.config.startup.StartupConfigurationException;
import api.astro.whats_orders_manager.modules.seguridad.model.Rol;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * T-022 (ADR-026 §1) — Admin de bootstrap: si no existe ningún usuario activo con rol ADMIN, crea uno con los
 * datos de BOOTSTRAP_ADMIN_EMAIL, BOOTSTRAP_ADMIN_TELEFONO y BOOTSTRAP_ADMIN_PASSWORD, nombre "Administrador",
 * activo y con cambio de contraseña obligatorio en el primer ingreso.
 *
 * <ul>
 *   <li>Sin ADMIN activo y con alguna variable ausente, o con una contraseña de menos de
 *       {@value #LONGITUD_MINIMA_PASSWORD} caracteres: la aplicación no arranca
 *       ({@link StartupConfigurationException}, presentada por el FailureAnalyzer de T-021).</li>
 *   <li>Con un ADMIN activo: las variables se ignoran y no son necesarias.</li>
 *   <li>Nunca modifica filas de usuario existentes. Si el email o el teléfono ya pertenecen a otro usuario
 *       (p. ej. inactivo), no arranca y lo indica.</li>
 * </ul>
 * La contraseña nunca se registra: solo se registra el id del usuario creado. Los mensajes nombran variables,
 * nunca valores.
 */
@Slf4j
@Component
public class BootstrapAdminInitializer {

    static final int LONGITUD_MINIMA_PASSWORD = 12;
    static final String NOMBRE = "Administrador";
    static final String ROL_ADMIN = "ADMIN";

    static final String VAR_EMAIL = "BOOTSTRAP_ADMIN_EMAIL";
    static final String VAR_TELEFONO = "BOOTSTRAP_ADMIN_TELEFONO";
    static final String VAR_PASSWORD = "BOOTSTRAP_ADMIN_PASSWORD";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String telefono;
    private final String password;

    public BootstrapAdminInitializer(UsuarioRepository usuarioRepository,
                                     PasswordEncoder passwordEncoder,
                                     @Value("${bootstrap.admin.email:}") String email,
                                     @Value("${bootstrap.admin.telefono:}") String telefono,
                                     @Value("${bootstrap.admin.password:}") String password) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.telefono = telefono;
        this.password = password;
    }

    /**
     * @param rolAdmin rol ADMIN ya existente (lo garantiza DataInitializer)
     * @return el admin creado, o vacío si ya había un ADMIN activo
     * @throws StartupConfigurationException si hace falta crear el admin y la configuración no lo permite
     */
    @Transactional
    public Optional<Usuario> crearSiNoHayAdminActivo(Rol rolAdmin) {
        if (usuarioRepository.existsByRolAndActivoTrue(ROL_ADMIN)) {
            return Optional.empty();
        }

        validarVariables();

        Usuario admin = new Usuario();
        admin.setNombre(NOMBRE);
        admin.setEmail(email.trim());
        admin.setTelefono(telefono.trim());
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRol(ROL_ADMIN);
        admin.setRolEntity(rolAdmin);
        admin.setActivo(true);
        admin.setBloqueado(false);
        admin.setIntentosFallidos(0);
        admin.setRequireCambioPassword(true);
        Usuario creado = usuarioRepository.save(admin);

        log.info("admin de bootstrap creado (id={})", creado.getIdUsuario());
        return Optional.of(creado);
    }

    private void validarVariables() {
        List<String> faltantes = new ArrayList<>();
        if (vacio(email)) {
            faltantes.add(VAR_EMAIL);
        }
        if (vacio(telefono)) {
            faltantes.add(VAR_TELEFONO);
        }
        if (vacio(password)) {
            faltantes.add(VAR_PASSWORD);
        }
        if (!faltantes.isEmpty()) {
            throw new StartupConfigurationException(
                    "No hay ningún usuario activo con rol ADMIN y faltan variables de entorno para crear el admin "
                            + "de bootstrap: " + String.join(", ", faltantes) + ".",
                    "Defina " + VAR_EMAIL + ", " + VAR_TELEFONO + " y " + VAR_PASSWORD + " (mínimo "
                            + LONGITUD_MINIMA_PASSWORD + " caracteres) en el entorno o en el archivo .env. Al "
                            + "ingresar por primera vez se pedirá cambiar la contraseña. Con un ADMIN activo estas "
                            + "variables no son necesarias.");
        }
        if (password.length() < LONGITUD_MINIMA_PASSWORD) {
            throw new StartupConfigurationException(
                    VAR_PASSWORD + " debe tener al menos " + LONGITUD_MINIMA_PASSWORD + " caracteres.",
                    "Defina en " + VAR_PASSWORD + " una contraseña de " + LONGITUD_MINIMA_PASSWORD
                            + " caracteres o más.");
        }
        if (usuarioRepository.findByEmail(email.trim()).isPresent()) {
            throw new StartupConfigurationException(
                    "No hay ningún usuario activo con rol ADMIN y el email de " + VAR_EMAIL + " ya pertenece a "
                            + "otro usuario.",
                    "Use en " + VAR_EMAIL + " un email que no esté registrado, o reactive desde la base un ADMIN "
                            + "existente. Los usuarios existentes no se modifican al arrancar.");
        }
        if (usuarioRepository.findByTelefono(telefono.trim()).isPresent()) {
            throw new StartupConfigurationException(
                    "No hay ningún usuario activo con rol ADMIN y el teléfono de " + VAR_TELEFONO + " ya "
                            + "pertenece a otro usuario.",
                    "Use en " + VAR_TELEFONO + " un teléfono que no esté registrado, o reactive desde la base un "
                            + "ADMIN existente. Los usuarios existentes no se modifican al arrancar.");
        }
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
