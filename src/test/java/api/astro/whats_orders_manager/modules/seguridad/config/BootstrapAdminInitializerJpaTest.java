package api.astro.whats_orders_manager.modules.seguridad.config;

import api.astro.whats_orders_manager.config.startup.StartupConfigurationException;
import api.astro.whats_orders_manager.config.startup.StartupConfigurationFailureAnalyzer;
import api.astro.whats_orders_manager.modules.seguridad.model.Rol;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.repository.RolRepository;
import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * T-022 (ADR-026 §1) — Admin de bootstrap por variables de entorno, sin usuario de prueba ni contraseña fija
 * (H2, JPA). Cada prueba corre en la transacción de @DataJpaTest y se revierte.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("T-022 — admin de bootstrap")
class BootstrapAdminInitializerJpaTest {

    private static final String EMAIL = "admin.t022@example.invalid";
    private static final String TELEFONO = "50688887777";
    private static final String PASSWORD = "Contrasena-T022-Unica!";

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;

    private Rol rolAdmin;

    @BeforeEach
    void preparar() {
        Rol r = new Rol();
        r.setCodigo("ADMIN");
        r.setNombre("Administrador");
        r.setActivo(true);
        rolAdmin = rolRepository.save(r);
    }

    private BootstrapAdminInitializer initializer(String email, String telefono, String password) {
        return new BootstrapAdminInitializer(usuarioRepository, passwordEncoder, email, telefono, password);
    }

    private Usuario usuario(String nombre, String email, String telefono, String rol, boolean activo) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setEmail(email);
        u.setTelefono(telefono);
        u.setPassword("hash-existente");
        u.setRol(rol);
        u.setActivo(activo);
        return usuarioRepository.save(u);
    }

    @Test
    @DisplayName("(a) Sin ADMIN y con BOOTSTRAP_ADMIN_* → exactamente un ADMIN, cambio de contraseña obligatorio")
    void sinAdmin_creaUnoSolo() {
        Optional<Usuario> creado = initializer(EMAIL, TELEFONO, PASSWORD).crearSiNoHayAdminActivo(rolAdmin);

        assertThat(creado).isPresent();
        List<Usuario> admins = usuarioRepository.findByRol("ADMIN");
        assertThat(admins).hasSize(1);
        Usuario admin = admins.get(0);
        assertThat(admin.getNombre()).isEqualTo("Administrador");
        assertThat(admin.getEmail()).isEqualTo(EMAIL);
        assertThat(admin.getTelefono()).isEqualTo(TELEFONO);
        assertThat(admin.getActivo()).isTrue();
        assertThat(admin.getRequireCambioPassword()).isTrue();
        assertThat(admin.getRolEntity().getCodigo()).isEqualTo("ADMIN");
        assertThat(admin.getPassword()).isNotEqualTo(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, admin.getPassword())).isTrue();

        // un segundo arranque ya encuentra el ADMIN y no crea otro
        assertThat(initializer(EMAIL, TELEFONO, PASSWORD).crearSiNoHayAdminActivo(rolAdmin)).isEmpty();
        assertThat(usuarioRepository.findByRol("ADMIN")).hasSize(1);
    }

    @Test
    @DisplayName("(b) Sin ADMIN y sin BOOTSTRAP_ADMIN_PASSWORD → no arranca y nombra la variable")
    void sinAdmin_sinPassword_falla() {
        StartupConfigurationException e = catchThrowableOfType(StartupConfigurationException.class,
                () -> initializer(EMAIL, TELEFONO, "").crearSiNoHayAdminActivo(rolAdmin));

        assertThat(e).isNotNull();
        assertThat(e.getDescription()).contains("BOOTSTRAP_ADMIN_PASSWORD")
                .doesNotContain("BOOTSTRAP_ADMIN_EMAIL").doesNotContain("BOOTSTRAP_ADMIN_TELEFONO");
        FailureAnalysis analysis = new StartupConfigurationFailureAnalyzer().analyze(new IllegalStateException(e));
        assertThat(analysis.getDescription()).contains("BOOTSTRAP_ADMIN_PASSWORD");
        assertThat(analysis.getAction()).contains("BOOTSTRAP_ADMIN_EMAIL", "BOOTSTRAP_ADMIN_TELEFONO");
        assertThat(usuarioRepository.count()).isZero();
    }

    @Test
    @DisplayName("(b') Sin ADMIN y sin ninguna variable → nombra las tres")
    void sinAdmin_sinVariables_nombraLasTres() {
        StartupConfigurationException e = catchThrowableOfType(StartupConfigurationException.class,
                () -> initializer(null, " ", null).crearSiNoHayAdminActivo(rolAdmin));

        assertThat(e.getDescription())
                .contains("BOOTSTRAP_ADMIN_EMAIL", "BOOTSTRAP_ADMIN_TELEFONO", "BOOTSTRAP_ADMIN_PASSWORD");
    }

    @Test
    @DisplayName("(c) Con un ADMIN activo y sin BOOTSTRAP_ADMIN_* → arranca y no crea usuarios ni toca los existentes")
    void conAdminActivo_noCreaNada() {
        Usuario existente = usuario("admin actual", "actual@example.invalid", "50611112222", "ADMIN", true);
        long antes = usuarioRepository.count();

        assertThat(initializer("", "", "").crearSiNoHayAdminActivo(rolAdmin)).isEmpty();

        assertThat(usuarioRepository.count()).isEqualTo(antes);
        Usuario tras = usuarioRepository.findById(existente.getIdUsuario()).orElseThrow();
        assertThat(tras.getPassword()).isEqualTo("hash-existente");
        assertThat(tras.getNombre()).isEqualTo("admin actual");
    }

    @Test
    @DisplayName("Un ADMIN inactivo no cuenta: se crea el de bootstrap sin modificar el inactivo")
    void adminInactivo_creaBootstrap() {
        Usuario inactivo = usuario("admin viejo", "viejo@example.invalid", "50633334444", "ADMIN", false);

        assertThat(initializer(EMAIL, TELEFONO, PASSWORD).crearSiNoHayAdminActivo(rolAdmin)).isPresent();

        Usuario tras = usuarioRepository.findById(inactivo.getIdUsuario()).orElseThrow();
        assertThat(tras.getActivo()).isFalse();
        assertThat(tras.getPassword()).isEqualTo("hash-existente");
        assertThat(usuarioRepository.existsByRolAndActivoTrue("ADMIN")).isTrue();
    }

    @Test
    @DisplayName("(d) Contraseña de menos de 12 caracteres → no arranca, sin mostrar la contraseña")
    void passwordCorta_falla() {
        String corta = "Corta-11chr";

        StartupConfigurationException e = catchThrowableOfType(StartupConfigurationException.class,
                () -> initializer(EMAIL, TELEFONO, corta).crearSiNoHayAdminActivo(rolAdmin));

        assertThat(e).isNotNull();
        assertThat(e.getDescription()).contains("BOOTSTRAP_ADMIN_PASSWORD").contains("12").doesNotContain(corta);
        assertThat(e.getAction()).doesNotContain(corta);
        assertThat(usuarioRepository.count()).isZero();
    }

    @Test
    @DisplayName("Email ya usado por otro usuario → no arranca y no modifica ese usuario")
    void emailOcupado_falla() {
        Usuario otro = usuario("vendedor", EMAIL, "50655556666", "VENDEDOR", true);

        StartupConfigurationException e = catchThrowableOfType(StartupConfigurationException.class,
                () -> initializer(EMAIL, TELEFONO, PASSWORD).crearSiNoHayAdminActivo(rolAdmin));

        assertThat(e.getDescription()).contains("BOOTSTRAP_ADMIN_EMAIL").doesNotContain(EMAIL);
        assertThat(usuarioRepository.findById(otro.getIdUsuario()).orElseThrow().getRol()).isEqualTo("VENDEDOR");
        assertThat(usuarioRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("(e) Ningún log contiene la contraseña; solo 'admin de bootstrap creado (id=N)'")
    void logs_sinPassword(CapturedOutput output) {
        Usuario creado = initializer(EMAIL, TELEFONO, PASSWORD).crearSiNoHayAdminActivo(rolAdmin).orElseThrow();

        assertThat(output).contains("admin de bootstrap creado (id=" + creado.getIdUsuario() + ")")
                .doesNotContain(PASSWORD)
                .doesNotContain(PASSWORD.substring(0, 6))
                .doesNotContain(creado.getPassword());
    }
}
