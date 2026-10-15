package api.astro.whats_orders_manager.modules.configuracion.controller;

import api.astro.whats_orders_manager.modules.configuracion.dto.mapper.ConfiguracionEmpresaMapper;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmpresaRepository;
import api.astro.whats_orders_manager.modules.configuracion.service.HaciendaApiService;
import api.astro.whats_orders_manager.modules.configuracion.service.impl.ConfiguracionEmpresaServiceImpl;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioActividadService;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import org.junit.jupiter.api.AfterEach;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-017 (ADR-020) — PUT /api/configuracion/empresa con la autoconfiguración real de Spring MVC y
 * Jackson (@WebMvcTest): un campo desconocido en el cuerpo ya no produce un 400 antes de llegar al
 * controlador, y el cambio se persiste (JPA sobre H2) sin crear filas nuevas.
 */
@WebMvcTest(value = ConfiguracionEmpresaRestController.class, properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureDataJpa
@AutoConfigureTestDatabase
@Import({ConfiguracionEmpresaServiceImpl.class, ConfiguracionEmpresaMapper.class})
@DisplayName("T-017 — PUT /api/configuracion/empresa con campos desconocidos")
class ConfiguracionEmpresaPutJacksonTest {

    @MockitoBean private HaciendaApiService haciendaApiService;
    @MockitoBean private UsuarioActividadService usuarioActividadService;
    @MockitoBean private UsuarioService usuarioService;

    @Autowired private MockMvc mockMvc;
    @Autowired private ConfiguracionEmpresaRepository repository;

    @AfterEach
    void limpiar() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("un cuerpo con faviconUrl responde 200, success=true y persiste razón social y actividad")
    void put_conCampoDesconocido_guardaCambios() throws Exception {
        ConfiguracionEmpresa existente = repository.save(ConfiguracionEmpresa.builder()
                .razonSocial("MONRACHEM S.A")
                .nombreComercial("MONRACHEM")
                .codigoActividad("20104")
                .build());

        String cuerpo = """
                {"idConfiguracion": %d,
                 "razonSocial": "EMANUEL SOTO LEAL",
                 "codigoActividad": "4773.0",
                 "faviconUrl": "https://ejemplo.com/favicon.ico",
                 "campoQueNoExiste": 123}
                """.formatted(existente.getIdConfiguracion());

        mockMvc.perform(put("/api/configuracion/empresa")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.razonSocial").value("EMANUEL SOTO LEAL"));

        assertThat(repository.count()).isEqualTo(1);
        ConfiguracionEmpresa guardada = repository.findById(existente.getIdConfiguracion()).orElseThrow();
        assertThat(guardada.getRazonSocial()).isEqualTo("EMANUEL SOTO LEAL");
        assertThat(guardada.getCodigoActividad()).isEqualTo("4773.0");
        assertThat(guardada.getNombreComercial()).isEqualTo("MONRACHEM");
    }
}
