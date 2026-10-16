package api.astro.whats_orders_manager.modules.configuracion.controller;

import api.astro.whats_orders_manager.config.crypto.CredencialCipherConfig;
import api.astro.whats_orders_manager.modules.configuracion.dto.mapper.ConfiguracionEmailMapper;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmail;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmailRepository;
import api.astro.whats_orders_manager.modules.configuracion.service.impl.ConfiguracionEmailServiceImpl;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-022 (ADR-026 §2, ADR-027) — API de configuración de email: el JSON nunca trae la contraseña (ni la
 * máscara: el campo es de solo escritura) y POST/PUT con "********" o vacío conservan la guardada. MVC + Jackson
 * reales, JPA sobre H2 con la contraseña cifrada con la clave de prueba.
 */
@WebMvcTest(value = ConfiguracionEmailRestController.class, properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureDataJpa
@AutoConfigureTestDatabase
@Import({ConfiguracionEmailServiceImpl.class, ConfiguracionEmailMapper.class, CredencialCipherConfig.class})
@DisplayName("T-022 — la API de configuración de email no expone ni pisa la contraseña")
class ConfiguracionEmailApiTest {

    private static final String PASSWORD = "clave-smtp-secreta-19";

    @MockitoBean private UsuarioActividadService usuarioActividadService;
    @MockitoBean private UsuarioService usuarioService;

    @Autowired private MockMvc mockMvc;
    @Autowired private ConfiguracionEmailRepository repository;

    private ConfiguracionEmail existente;

    @BeforeEach
    void preparar() {
        existente = repository.save(ConfiguracionEmail.builder()
                .smtpHost("smtp.example.invalid").smtpPort(587).smtpUsuario("cuenta@example.invalid")
                .smtpPassword(PASSWORD).emailRemitente("cuenta@example.invalid").activo(false).build());
    }

    @AfterEach
    void limpiar() {
        repository.deleteAll();
    }

    private String cuerpo(String password) {
        return """
                {"smtpHost":"smtp2.example.invalid","smtpPort":465,"smtpUsuario":"cuenta@example.invalid",
                 "smtpPassword":%s,"emailRemitente":"cuenta@example.invalid"}
                """.formatted(password == null ? "null" : "\"" + password + "\"");
    }

    private String enviar(MockHttpServletRequestBuilder request, String password) throws Exception {
        return mockMvc.perform(request.with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo(password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String passwordGuardada() {
        return repository.findById(existente.getIdConfiguracion()).orElseThrow().getSmtpPassword();
    }

    @Test
    @DisplayName("GET: sin contraseña, ni cifrada ni enmascarada")
    void get_sinPassword() throws Exception {
        String json = mockMvc.perform(get("/api/configuracion/email").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(json).doesNotContain(PASSWORD).doesNotContain("enc:v1:").doesNotContain("smtpPassword");
    }

    @Test
    @DisplayName("PUT y POST con '********' o vacío conservan la contraseña; la respuesta no la incluye")
    void putPost_conMascara_conservan() throws Exception {
        assertThat(enviar(put("/api/configuracion/email"), "********")).doesNotContain(PASSWORD);
        assertThat(passwordGuardada()).isEqualTo(PASSWORD);

        assertThat(enviar(post("/api/configuracion/email"), "********")).doesNotContain(PASSWORD);
        assertThat(passwordGuardada()).isEqualTo(PASSWORD);

        enviar(put("/api/configuracion/email"), "");
        assertThat(passwordGuardada()).isEqualTo(PASSWORD);

        enviar(post("/api/configuracion/email"), null);
        assertThat(passwordGuardada()).isEqualTo(PASSWORD);

        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findById(existente.getIdConfiguracion()).orElseThrow().getSmtpHost())
                .isEqualTo("smtp2.example.invalid");
    }

    @Test
    @DisplayName("PUT con una contraseña nueva la reemplaza")
    void put_passwordNueva() throws Exception {
        enviar(put("/api/configuracion/email"), "clave-nueva-de-prueba");

        assertThat(passwordGuardada()).isEqualTo("clave-nueva-de-prueba");
    }
}
