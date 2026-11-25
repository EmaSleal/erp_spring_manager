package api.astro.whats_orders_manager.modules.regimen;

import api.astro.whats_orders_manager.modules.regimen.controller.RegimenTributarioRestController;
import api.astro.whats_orders_manager.modules.regimen.controller.RegimenTributarioViewController;
import api.astro.whats_orders_manager.modules.regimen.dto.RegimenTributarioDTO;
import api.astro.whats_orders_manager.modules.regimen.enums.Regimen;
import api.astro.whats_orders_manager.modules.regimen.model.RegimenTributario;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenNoDefinidoException;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenTributarioService;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenTributarioService.TramoEstado;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-056 (criterio 3 y 4): /api/regimen-tributario y la pantalla /configuracion/regimen-tributario. Escribir exige
 * REGIMEN_TRIBUTARIO_GESTIONAR (solo ADMIN); leer también lo permite CONFIG_VER. Los errores salen como 400, 404 y 409
 * con el mensaje fijo del servicio.
 */
@SpringJUnitWebConfig(RegimenTributarioWebTest.Config.class)
@DisplayName("T-056 — API y pantalla del régimen tributario (permisos y errores)")
class RegimenTributarioWebTest {

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class Config {

        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
            return http.build();
        }

        @Bean(name = "permisoService")
        PermisoService permisoService() {
            return mock(PermisoService.class);
        }

        @Bean
        RegimenTributarioService regimenService() {
            return mock(RegimenTributarioService.class);
        }

        @Bean
        UsuarioService usuarioService() {
            return mock(UsuarioService.class);
        }

        @Bean
        RegimenTributarioRestController regimenRestController(RegimenTributarioService servicio, UsuarioService usuarios) {
            return new RegimenTributarioRestController(servicio, usuarios);
        }

        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-11-25T16:00:00Z"), ZoneId.of("America/Costa_Rica"));
        }

        @Bean
        RegimenTributarioViewController regimenViewController(Clock clock) {
            return new RegimenTributarioViewController(clock);
        }

        @Bean
        SpringResourceTemplateResolver templateResolver(ApplicationContext context) {
            SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
            resolver.setApplicationContext(context);
            resolver.setPrefix("classpath:/templates/");
            resolver.setSuffix(".html");
            resolver.setCharacterEncoding("UTF-8");
            resolver.setCacheable(false);
            return resolver;
        }

        @Bean
        SpringTemplateEngine templateEngine(SpringResourceTemplateResolver resolver) {
            SpringTemplateEngine engine = new SpringTemplateEngine();
            engine.setTemplateResolver(resolver);
            engine.addDialect(new SpringSecurityDialect());
            return engine;
        }

        @Bean
        ThymeleafViewResolver viewResolver(SpringTemplateEngine engine) {
            ThymeleafViewResolver resolver = new ThymeleafViewResolver();
            resolver.setTemplateEngine(engine);
            resolver.setCharacterEncoding("UTF-8");
            return resolver;
        }
    }

    @Autowired private WebApplicationContext context;
    @Autowired private PermisoService permisoService;
    @Autowired private RegimenTributarioService servicio;
    @Autowired private UsuarioService usuarioService;

    private MockMvc mockMvc;
    private Usuario admin;

    @BeforeEach
    void setUp() {
        reset(permisoService, servicio, usuarioService);
        when(permisoService.tienePermisoPorCodigo(anyString(), anyString())).thenReturn(false);
        when(permisoService.tienePermisoPorCodigo(eq("admin"), eq("REGIMEN_TRIBUTARIO_GESTIONAR"))).thenReturn(true);
        when(permisoService.tienePermisoPorCodigo(eq("lector"), eq("CONFIG_VER"))).thenReturn(true);
        admin = mock(Usuario.class);
        when(admin.getIdUsuario()).thenReturn(7);
        when(admin.getNombre()).thenReturn("Ana Admin");
        when(usuarioService.findByEmail("admin")).thenReturn(Optional.of(admin));
        when(usuarioService.findById(7)).thenReturn(Optional.of(admin));
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }


    private static RegimenTributario tramo(long id, Regimen r, String desde) {
        return RegimenTributario.builder().id(id).regimen(r).vigenteDesde(LocalDate.parse(desde)).nota("n").usuarioId(7)
                .fechaRegistro(LocalDateTime.of(2026, 11, 1, 8, 0)).build();
    }

    // ── Permisos ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("sin REGIMEN_TRIBUTARIO_GESTIONAR: crear, corregir y eliminar responden 403 y no tocan el servicio")
    void sinPermisoEscritura_403() throws Exception {
        for (String usuario : new String[] {"contador", "lector"}) {
            mockMvc.perform(post("/api/regimen-tributario").with(user(usuario)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"regimen\":\"SIMPLIFICADO\",\"vigenteDesde\":\"2027-01-01\"}")).andExpect(status().isForbidden());
            mockMvc.perform(put("/api/regimen-tributario/3").with(user(usuario)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"regimen\":\"SIMPLIFICADO\",\"vigenteDesde\":\"2027-01-01\"}")).andExpect(status().isForbidden());
            mockMvc.perform(delete("/api/regimen-tributario/3").with(user(usuario)).with(csrf())).andExpect(status().isForbidden());
        }
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("sin ningún permiso de régimen ni CONFIG_VER: listar, vigente y pantalla responden 403")
    void sinPermisoLectura_403() throws Exception {
        mockMvc.perform(get("/api/regimen-tributario").with(user("contador"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/regimen-tributario/vigente").with(user("contador"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/configuracion/regimen-tributario").with(user("contador"))).andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("con CONFIG_VER se puede leer el historial, el vigente y la pantalla, pero no escribir")
    void configVer_lee() throws Exception {
        when(servicio.historial()).thenReturn(List.of());
        mockMvc.perform(get("/api/regimen-tributario").with(user("lector"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/regimen-tributario/vigente").with(user("lector"))).andExpect(status().isOk());
        mockMvc.perform(get("/configuracion/regimen-tributario").with(user("lector"))).andExpect(status().isOk());
        mockMvc.perform(delete("/api/regimen-tributario/3").with(user("lector")).with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("historial: tramos con vigente, próximo, editable y el nombre de quien registró")
    void listar_ok() throws Exception {
        when(servicio.historial()).thenReturn(List.of(
                new TramoEstado(tramo(1, Regimen.SIMPLIFICADO, "2000-01-01"), false, false, false, "Un tramo que ya empezó no se edita ni se borra"),
                new TramoEstado(tramo(2, Regimen.GENERAL, "2026-09-01"), true, false, false, "Un tramo que ya empezó no se edita ni se borra"),
                new TramoEstado(tramo(3, Regimen.SIMPLIFICADO, "2027-01-01"), false, true, true, null)));
        mockMvc.perform(get("/api/regimen-tributario").with(user("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[1].regimen").value("GENERAL"))
                .andExpect(jsonPath("$.data[1].vigenteDesde").value("2026-09-01"))
                .andExpect(jsonPath("$.data[1].vigente").value(true))
                .andExpect(jsonPath("$.data[2].proximo").value(true))
                .andExpect(jsonPath("$.data[2].editable").value(true))
                .andExpect(jsonPath("$.data[0].editable").value(false))
                .andExpect(jsonPath("$.data[0].bloqueo").value("Un tramo que ya empezó no se edita ni se borra"))
                .andExpect(jsonPath("$.data[2].usuarioNombre").value("Ana Admin"));
    }

    @Test
    @DisplayName("vigente: el tramo de hoy con vigente=true y el siguiente con proximo=true; sin siguiente, nulo; sin vigente, data nula")
    void vigente() throws Exception {
        String bloqueo = "Un tramo que ya empezó no se edita ni se borra";
        when(servicio.historial()).thenReturn(List.of(
                new TramoEstado(tramo(1, Regimen.SIMPLIFICADO, "2000-01-01"), false, false, false, bloqueo),
                new TramoEstado(tramo(2, Regimen.GENERAL, "2026-09-01"), true, false, false, bloqueo),
                new TramoEstado(tramo(3, Regimen.SIMPLIFICADO, "2027-01-01"), false, true, true, null)));
        mockMvc.perform(get("/api/regimen-tributario/vigente").with(user("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.regimen").value("GENERAL"))
                .andExpect(jsonPath("$.data.vigenteDesde").value("2026-09-01"))
                .andExpect(jsonPath("$.data.vigente").value(true))
                .andExpect(jsonPath("$.data.proximo").value(false))
                .andExpect(jsonPath("$.data.siguiente.regimen").value("SIMPLIFICADO"))
                .andExpect(jsonPath("$.data.siguiente.proximo").value(true))
                .andExpect(jsonPath("$.data.siguiente.vigente").value(false))
                .andExpect(jsonPath("$.data.siguiente.editable").value(true));

        when(servicio.historial()).thenReturn(List.of(
                new TramoEstado(tramo(2, Regimen.GENERAL, "2026-09-01"), true, false, false, bloqueo)));
        mockMvc.perform(get("/api/regimen-tributario/vigente").with(user("admin")))
                .andExpect(jsonPath("$.data.vigente").value(true))
                .andExpect(jsonPath("$.data.siguiente").doesNotExist());

        when(servicio.historial()).thenReturn(List.of());
        mockMvc.perform(get("/api/regimen-tributario/vigente").with(user("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("crear y corregir: la respuesta lleva el estado real del tramo (vigente, próximo, editable)")
    void escrituraConEstadoReal() throws Exception {
        RegimenTributario creado = tramo(3, Regimen.SIMPLIFICADO, "2026-11-24");
        when(servicio.crear(any(), eq(admin))).thenReturn(creado);
        when(servicio.historial()).thenReturn(List.of(
                new TramoEstado(creado, true, false, false, "Un tramo que ya empezó no se edita ni se borra")));
        mockMvc.perform(post("/api/regimen-tributario").with(user("admin")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"regimen\":\"SIMPLIFICADO\",\"vigenteDesde\":\"2026-11-24\",\"nota\":\"n\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.vigente").value(true))
                .andExpect(jsonPath("$.data.editable").value(false));
    }

    @Test
    @DisplayName("crear: 200 con el tramo y el usuario autenticado; 400 y 409 con el mensaje del servicio")
    void crear() throws Exception {
        when(servicio.crear(any(), eq(admin))).thenReturn(tramo(3, Regimen.SIMPLIFICADO, "2027-01-01"));
        mockMvc.perform(post("/api/regimen-tributario").with(user("admin")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"regimen\":\"SIMPLIFICADO\",\"vigenteDesde\":\"2027-01-01\",\"nota\":\"n\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.vigenteDesde").value("2027-01-01"));
        verify(servicio).crear(any(RegimenTributarioDTO.class), eq(admin));

        when(servicio.crear(any(), any())).thenThrow(new IllegalArgumentException("Indique el régimen (GENERAL o SIMPLIFICADO)"));
        mockMvc.perform(post("/api/regimen-tributario").with(user("admin")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vigenteDesde\":\"2027-01-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Indique el régimen (GENERAL o SIMPLIFICADO)"));
        reset(servicio);
        when(servicio.crear(any(), any())).thenThrow(new IllegalStateException("Dos tramos seguidos no pueden tener el mismo régimen"));
        mockMvc.perform(post("/api/regimen-tributario").with(user("admin")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"regimen\":\"GENERAL\",\"vigenteDesde\":\"2027-01-01\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Dos tramos seguidos no pueden tener el mismo régimen"));
    }

    @Test
    @DisplayName("corregir: 200; 404 si no existe; 409 si no se puede")
    void corregir() throws Exception {
        when(servicio.corregir(eq(3L), any(), eq(admin))).thenReturn(tramo(3, Regimen.SIMPLIFICADO, "2027-02-01"));
        mockMvc.perform(put("/api/regimen-tributario/3").with(user("admin")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"regimen\":\"SIMPLIFICADO\",\"vigenteDesde\":\"2027-02-01\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.vigenteDesde").value("2027-02-01"));
        when(servicio.corregir(eq(99L), any(), any())).thenThrow(new NoSuchElementException("El tramo no existe"));
        mockMvc.perform(put("/api/regimen-tributario/99").with(user("admin")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"regimen\":\"SIMPLIFICADO\",\"vigenteDesde\":\"2027-02-01\"}"))
                .andExpect(status().isNotFound());
        when(servicio.corregir(eq(2L), any(), any())).thenThrow(new IllegalStateException("Un tramo que ya empezó no se edita ni se borra"));
        mockMvc.perform(put("/api/regimen-tributario/2").with(user("admin")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"regimen\":\"SIMPLIFICADO\",\"vigenteDesde\":\"2027-02-01\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("eliminar: 200; 404 si no existe; 409 si no se puede")
    void eliminar() throws Exception {
        mockMvc.perform(delete("/api/regimen-tributario/3").with(user("admin")).with(csrf())).andExpect(status().isOk());
        verify(servicio).eliminar(3L, admin);
        org.mockito.Mockito.doThrow(new NoSuchElementException("El tramo no existe")).when(servicio).eliminar(eq(99L), any());
        mockMvc.perform(delete("/api/regimen-tributario/99").with(user("admin")).with(csrf())).andExpect(status().isNotFound());
        org.mockito.Mockito.doThrow(new IllegalStateException("Un tramo que ya empezó no se edita ni se borra")).when(servicio).eliminar(eq(2L), any());
        mockMvc.perform(delete("/api/regimen-tributario/2").with(user("admin")).with(csrf())).andExpect(status().isConflict());
    }

    // ── Pantalla ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("la pantalla tiene la tabla del historial, el formulario, el script propio y no usa innerHTML")
    void pantalla() throws Exception {
        mockMvc.perform(get("/configuracion/regimen-tributario").with(user("admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Régimen tributario")))
                .andExpect(content().string(containsString("id=\"tablaRegimen\"")))
                .andExpect(content().string(containsString("id=\"formRegimen\"")))
                .andExpect(content().string(containsString("regimen-tributario.js")))
                .andExpect(content().string(containsString("data-puede-gestionar=\"true\"")))
                .andExpect(content().string(containsString("data-hoy=\"2026-11-25\"")))
                .andExpect(content().string(not(containsString("innerHTML"))));
        mockMvc.perform(get("/configuracion/regimen-tributario").with(user("lector")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-puede-gestionar=\"false\"")));
    }
}
