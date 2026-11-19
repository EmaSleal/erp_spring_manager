package api.astro.whats_orders_manager.modules.facturacion;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmpresaRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.ConfiguracionHaciendaNoDisponibleException;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ConfiguracionHaciendaRepository;
import api.astro.whats_orders_manager.modules.facturacion.service.PanelFacturacionService;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-043 (criterios 4, 7 y 8) — GET /facturas/inicio contra MySQL real: las consultas de conteo (incluida la de
 * sustitutas) corren en MySQL, y la tarjeta de Hacienda responde 200 con la configuración legible, sin configuración
 * activa y con las credenciales que no se descifran (mensajes fijos de T-041).
 *
 * <p>Base: MySQL 8 en Testcontainers (t043-it-mysql, tmpfs, sin volúmenes).
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect"
})
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("T-043 — panel de inicio de Facturación (MySQL)")
class PanelFacturacionIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.32")
            .withDatabaseName("t043")
            .withTmpFs(Map.of("/var/lib/mysql", "rw"))
            .withCreateContainerCmdModifier(cmd -> cmd.withName("t043-it-mysql"))
            .withCopyFileToContainer(
                    MountableFile.forHostPath(Path.of("database/routines/stored_procedures.sql")),
                    "/docker-entrypoint-initdb.d/01-routines.sql");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @MockitoBean(name = "permisoService") private PermisoService permisoService;

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PanelFacturacionService panelService;
    @Autowired private ConfiguracionEmpresaRepository empresaRepository;
    @Autowired private ConfiguracionHaciendaRepository configuracionRepository;

    @BeforeEach
    void preparar() {
        when(permisoService.tienePermisoPorCodigo(anyString(), anyString())).thenReturn(false);
        when(permisoService.tienePermisoPorCodigo(eq("gerente"), eq("FACTURA_VER"))).thenReturn(true);
        jdbc.update("DELETE FROM configuracion_hacienda");
        ConfiguracionEmpresa empresa = empresaRepository.findFirstByOrderByIdConfiguracionAsc()
                .orElseGet(() -> empresaRepository.save(ConfiguracionEmpresa.builder().razonSocial("Empresa T043").build()));
        configuracionRepository.save(ConfiguracionHacienda.builder().empresa(empresa).ambiente(AmbienteHacienda.SANDBOX)
                .usuarioHacienda("usuario-t043").passwordHacienda("clave-t043").activa(true)
                .rutaCertificado(CertificadoDePrueba.ruta()).pinCertificado(CertificadoDePrueba.PIN).build());
    }

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM configuracion_hacienda");
    }

    private String pagina() throws Exception {
        return mockMvc.perform(get("/facturas/inicio").with(user("gerente").roles("USER")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("los contadores (COUNT en MySQL) son 0 sin datos y la configuración legible se muestra")
    void legible() throws Exception {
        var c = panelService.contadores();
        assertThat(c.comprobantesError() + c.comprobantesNoTransmitido() + c.comprobantesEnvioIncierto()
                + c.comprobantesRechazado() + c.notasEnTramite() + c.anuladasSinSustituta() + c.sinComprobante())
                .isZero();

        String html = pagina();

        assertThat(html).contains("id=\"hacienda-ambiente\"", "SANDBOX", "Vence el ");
        assertThat(html).doesNotContain("id=\"hacienda-aviso\"", "usuario-t043", "clave-t043", CertificadoDePrueba.PIN);
    }

    @Test
    @DisplayName("sin configuración activa: 200 con el aviso fijo")
    void sinActiva() throws Exception {
        jdbc.update("UPDATE configuracion_hacienda SET activa = b'0'");

        assertThat(pagina()).contains("id=\"hacienda-aviso\"")
                .contains(ConfiguracionHaciendaNoDisponibleException.MENSAJE_NO_VALIDA);
    }

    @Test
    @DisplayName("credenciales que no se descifran: 200 con el aviso fijo, nunca 500")
    void ilegible() throws Exception {
        String original = jdbc.queryForObject("SELECT password_hacienda FROM configuracion_hacienda WHERE activa = b'1'",
                String.class);
        assertThat(original).startsWith("enc:v1:");
        String alterado = original.substring(0, original.length() - 6)
                + (original.endsWith("AAAAAA") ? "BBBBBB" : "AAAAAA");
        jdbc.update("UPDATE configuracion_hacienda SET password_hacienda = ? WHERE activa = b'1'", alterado);

        assertThat(pagina()).contains("id=\"hacienda-aviso\"")
                .contains(ConfiguracionHaciendaNoDisponibleException.MENSAJE_ILEGIBLE);
    }
}
