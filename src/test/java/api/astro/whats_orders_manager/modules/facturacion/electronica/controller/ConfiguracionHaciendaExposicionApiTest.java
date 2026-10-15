package api.astro.whats_orders_manager.modules.facturacion.electronica.controller;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmpresaRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.mapper.ConfiguracionHaciendaMapper;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ConfiguracionHaciendaRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.HaciendaApiService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl.ConfiguracionHaciendaServiceImpl;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioActividadService;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-021 — Los GET de configuración de Hacienda (lista, activa, por ambiente, estado del token) nunca devuelven
 * la contraseña, el PIN ni los tokens: contraseña y PIN salen enmascarados y los tokens no salen. MVC + Jackson
 * reales, JPA sobre H2 (credenciales cifradas con la clave de prueba).
 */
@WebMvcTest(value = ConfiguracionHaciendaController.class, properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureDataJpa
@AutoConfigureTestDatabase
@Import({ConfiguracionHaciendaServiceImpl.class, ConfiguracionHaciendaMapper.class})
@DisplayName("T-021 — la API de configuración de Hacienda no expone credenciales")
class ConfiguracionHaciendaExposicionApiTest {

    private static final String PASSWORD = "clave-secreta-de-prueba";
    private static final String PIN = "pin-secreto-0042";
    private static final String ACCESS = "access-token-secreto";
    private static final String REFRESH = "refresh-token-secreto";

    @MockitoBean private HaciendaApiService haciendaApiService;
    @MockitoBean private UsuarioActividadService usuarioActividadService;
    @MockitoBean private UsuarioService usuarioService;

    @Autowired private MockMvc mockMvc;
    @Autowired private ConfiguracionHaciendaRepository haciendaRepository;
    @Autowired private ConfiguracionEmpresaRepository empresaRepository;

    private ConfiguracionEmpresa empresa;
    private ConfiguracionHacienda config;

    @BeforeEach
    void preparar() {
        empresa = empresaRepository.save(ConfiguracionEmpresa.builder()
                .razonSocial("EMPRESA DE PRUEBA").nombreComercial("PRUEBA").build());
        config = haciendaRepository.save(ConfiguracionHacienda.builder()
                .empresa(empresa).ambiente(AmbienteHacienda.SANDBOX).usuarioHacienda("usuario-prueba")
                .passwordHacienda(PASSWORD).pinCertificado(PIN).accessToken(ACCESS).refreshToken(REFRESH)
                .tokenExpira(LocalDateTime.now().plusHours(1)).activa(true).build());
    }

    @AfterEach
    void limpiar() {
        haciendaRepository.deleteAll();
        empresaRepository.deleteAll();
    }

    private String getJson(String url) throws Exception {
        return mockMvc.perform(get(url).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static void sinSecretos(String json) {
        assertThat(json).doesNotContain(PASSWORD, PIN, ACCESS, REFRESH, "enc:v1:")
                .doesNotContain("accessToken").doesNotContain("refreshToken");
    }

    @Test
    @DisplayName("GET /empresa/{id} (lista): contraseña y PIN enmascarados, sin tokens")
    void lista() throws Exception {
        String json = getJson("/api/facturas/configuracion-hacienda/empresa/" + empresa.getIdConfiguracion());

        sinSecretos(json);
        mockMvc.perform(get("/api/facturas/configuracion-hacienda/empresa/" + empresa.getIdConfiguracion())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(jsonPath("$[0].passwordHacienda").value("********"))
                .andExpect(jsonPath("$[0].pinCertificado").value("********"))
                .andExpect(jsonPath("$[0].tieneToken").value(true));
    }

    @Test
    @DisplayName("GET /empresa/{id}/activa: enmascarada, sin tokens")
    void activa() throws Exception {
        String url = "/api/facturas/configuracion-hacienda/empresa/" + empresa.getIdConfiguracion() + "/activa";

        sinSecretos(getJson(url));
        mockMvc.perform(get(url).with(user("admin").roles("ADMIN")))
                .andExpect(jsonPath("$.passwordHacienda").value("********"))
                .andExpect(jsonPath("$.pinCertificado").value("********"));
    }

    @Test
    @DisplayName("GET /empresa/{id}/ambiente/{ambiente}: ya no devuelve la contraseña, el PIN ni el token en claro")
    void porAmbiente() throws Exception {
        String url = "/api/facturas/configuracion-hacienda/empresa/" + empresa.getIdConfiguracion()
                + "/ambiente/SANDBOX";

        sinSecretos(getJson(url));
        mockMvc.perform(get(url).with(user("admin").roles("ADMIN")))
                .andExpect(jsonPath("$.passwordHacienda").value("********"))
                .andExpect(jsonPath("$.pinCertificado").value("********"))
                .andExpect(jsonPath("$.usuarioHacienda").value("usuario-prueba"));
    }

    @Test
    @DisplayName("Sin PIN guardado → pinCertificado null")
    void sinPin_null() throws Exception {
        config.setPinCertificado(null);
        haciendaRepository.save(config);

        mockMvc.perform(get("/api/facturas/configuracion-hacienda/empresa/" + empresa.getIdConfiguracion()
                        + "/ambiente/SANDBOX").with(user("admin").roles("ADMIN")))
                .andExpect(jsonPath("$.pinCertificado").doesNotExist());
    }

    @Test
    @DisplayName("GET /{id}/estado-token: informa vigencia sin exponer el token")
    void estadoToken() throws Exception {
        String url = "/api/facturas/configuracion-hacienda/" + config.getId() + "/estado-token";

        sinSecretos(getJson(url));
        mockMvc.perform(get(url).with(user("admin").roles("ADMIN")))
                .andExpect(jsonPath("$.tieneToken").value(true))
                .andExpect(jsonPath("$.tokenValido").value(true));
    }
}
