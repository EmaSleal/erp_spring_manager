package api.astro.whats_orders_manager.modules.seguridad.service;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmpresaRepository;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.model.UsuarioActividad;
import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioActividadRepository;
import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioRepository;
import api.astro.whats_orders_manager.modules.seguridad.service.impl.UsuarioActividadServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * T-017 — el registro de un cambio de configuración o de permiso guarda la actividad a nombre del
 * usuario autenticado (usuario_actividad.id_usuario es obligatorio) y, si el registro falla, no revierte
 * ni rompe la operación del llamador (antes: UnexpectedRollbackException y 500 al guardar
 * Configuración → Empresa). H2, JPA; sin transacción de prueba para que cada paso confirme.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import({UsuarioActividadServiceImpl.class, RegistroCambioConfiguracionJpaTest.AuditoriaConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("T-017 — registro de cambios de configuración y permisos")
class RegistroCambioConfiguracionJpaTest {

    /** Como PersistenceConfig en la app: llena fecha_actividad (@CreatedDate). */
    @TestConfiguration
    @EnableJpaAuditing
    static class AuditoriaConfig {}

    @Autowired private UsuarioActividadService service;
    @Autowired private UsuarioActividadRepository actividadRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ConfiguracionEmpresaRepository configuracionRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private TransactionTemplate tx;
    private Usuario admin;

    @BeforeEach
    void preparar() {
        tx = new TransactionTemplate(transactionManager);
        admin = new Usuario();
        admin.setNombre("Administrador");
        admin.setEmail("admin@t017.test");
        admin = usuarioRepository.save(admin);
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
        actividadRepository.deleteAll();
        configuracionRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    private void autenticarComo(String email) {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                email, null, AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
    }

    private ConfiguracionEmpresa nuevaConfiguracion(String razonSocial) {
        return ConfiguracionEmpresa.builder().razonSocial(razonSocial).nombreComercial("MONRACHEM").build();
    }

    @Test
    @DisplayName("con usuario autenticado, el cambio de configuración queda registrado a su nombre")
    void cambioConfiguracion_registraAlUsuarioAutenticado() {
        autenticarComo("admin@t017.test");

        service.registrarCambioConfiguracion("configuracion_empresa", "Configuración de empresa actualizada");

        List<UsuarioActividad> actividades = actividadRepository.findByUsuario_IdUsuario(admin.getIdUsuario());
        assertThat(actividades).singleElement().satisfies(a -> {
            assertThat(a.getTipoActividad()).isEqualTo("CONFIG_CHANGE");
            assertThat(a.getDescripcion()).contains("[configuracion_empresa]");
        });
    }

    @Test
    @DisplayName("con usuario autenticado, el cambio de permiso queda registrado a su nombre")
    void cambioPermiso_registraAlUsuarioAutenticado() {
        autenticarComo("admin@t017.test");

        service.registrarCambioPermiso("CONFIG_EDITAR_EMPRESA", "Permiso asignado");

        assertThat(actividadRepository.findByUsuario_IdUsuario(admin.getIdUsuario()))
                .singleElement()
                .satisfies(a -> assertThat(a.getTipoActividad()).isEqualTo("PERMISSION_CHANGE"));
    }

    @Test
    @DisplayName("sin usuario autenticado, no registra nada y la transacción del llamador confirma")
    void sinUsuario_noRegistraYElLlamadorConfirma() {
        assertThatCode(() -> tx.executeWithoutResult(s -> {
            configuracionRepository.save(nuevaConfiguracion("EMANUEL SOTO LEAL"));
            service.registrarCambioConfiguracion("configuracion_empresa", "sin usuario");
        })).doesNotThrowAnyException();

        assertThat(actividadRepository.count()).isZero();
        assertThat(configuracionRepository.findAll())
                .extracting(ConfiguracionEmpresa::getRazonSocial).containsExactly("EMANUEL SOTO LEAL");
    }

    @Test
    @DisplayName("si guardar la actividad falla en la BD, la transacción del llamador confirma igual")
    void fallaAlGuardarActividad_noRevierteAlLlamador() {
        autenticarComo("admin@t017.test");
        // user_agent admite 500 caracteres: este insert falla de verdad en la base
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "x".repeat(600));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThatCode(() -> tx.executeWithoutResult(s -> {
            configuracionRepository.save(nuevaConfiguracion("EMANUEL SOTO LEAL"));
            service.registrarCambioConfiguracion("configuracion_empresa", "falla al registrar");
        })).doesNotThrowAnyException();

        assertThat(actividadRepository.count()).isZero();
        assertThat(configuracionRepository.findAll())
                .extracting(ConfiguracionEmpresa::getRazonSocial).containsExactly("EMANUEL SOTO LEAL");
    }
}
