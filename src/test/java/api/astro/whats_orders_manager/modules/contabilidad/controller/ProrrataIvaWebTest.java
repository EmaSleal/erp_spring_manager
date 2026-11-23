package api.astro.whats_orders_manager.modules.contabilidad.controller;

import api.astro.whats_orders_manager.modules.contabilidad.dto.ProrrataIvaDTO;
import api.astro.whats_orders_manager.modules.contabilidad.model.ProrrataIva;
import api.astro.whats_orders_manager.modules.contabilidad.service.ProrrataIvaService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-054 (criterio 7): /api/prorrata-iva y la pantalla /contabilidad/prorrata-iva. La gestión (alta, corrección, listado y
 * pantalla) exige PRORRATA_IVA_GESTIONAR; consultar el porcentaje de un año también lo permite CUENTA_PAGAR_CREAR (el
 * formulario de CxP lo necesita para avisar). Los errores salen como 400, 404 y 409 con el mensaje del servicio.
 */
@SpringJUnitWebConfig(ProrrataIvaWebTest.Config.class)
@DisplayName("T-054 — API y pantalla de la prorrata del IVA (permisos y errores)")
class ProrrataIvaWebTest {

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
        ProrrataIvaService prorrataIvaService() {
            return mock(ProrrataIvaService.class);
        }

        @Bean
        UsuarioService usuarioService() {
            return mock(UsuarioService.class);
        }

        @Bean
        ProrrataIvaRestController prorrataIvaRestController(ProrrataIvaService servicio, UsuarioService usuarios) {
            return new ProrrataIvaRestController(servicio, usuarios);
        }

        @Bean
        ProrrataIvaViewController prorrataIvaViewController() {
            return new ProrrataIvaViewController();
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
    @Autowired private ProrrataIvaService servicio;
    @Autowired private UsuarioService usuarioService;

    private MockMvc mockMvc;
    private Usuario admin;

    @BeforeEach
    void setUp() {
        reset(permisoService, servicio, usuarioService);
        when(permisoService.tienePermisoPorCodigo(anyString(), anyString())).thenReturn(false);
        when(permisoService.tienePermisoPorCodigo(eq("admin"), eq("PRORRATA_IVA_GESTIONAR"))).thenReturn(true);
        when(permisoService.tienePermisoPorCodigo(eq("contador"), eq("CUENTA_PAGAR_CREAR"))).thenReturn(true);
        admin = mock(Usuario.class);
        when(admin.getIdUsuario()).thenReturn(7);
        when(admin.getNombre()).thenReturn("Ana Admin");
        when(usuarioService.findByEmail("admin")).thenReturn(Optional.of(admin));
        when(usuarioService.findById(7)).thenReturn(Optional.of(admin));
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static ProrrataIva prorrata(int anio, String pct) {
        return ProrrataIva.builder().id((long) anio).anio(anio).porcentaje(new BigDecimal(pct)).nota("n").usuarioId(7)
                .fechaRegistro(LocalDateTime.of(2026, 1, 5, 10, 0)).fechaActualizacion(LocalDateTime.of(2026, 1, 5, 10, 0)).build();
    }

    // ── Permisos ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("sin PRORRATA_IVA_GESTIONAR: listar, crear, corregir y la pantalla responden 403 y no tocan el servicio")
    void sinPermiso_403() throws Exception {
        for (String usuario : new String[] {"contador", "lector"}) {
            mockMvc.perform(get("/api/prorrata-iva").with(user(usuario))).andExpect(status().isForbidden());
            mockMvc.perform(post("/api/prorrata-iva").with(user(usuario)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"anio\":2026,\"porcentaje\":80}")).andExpect(status().isForbidden());
            mockMvc.perform(put("/api/prorrata-iva/2026").with(user(usuario)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"porcentaje\":80}")).andExpect(status().isForbidden());
            mockMvc.perform(get("/contabilidad/prorrata-iva").with(user(usuario))).andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/prorrata-iva/2026").with(user("lector"))).andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("con PRORRATA_IVA_GESTIONAR: listado con el nombre de quien registró")
    void listar_ok() throws Exception {
        when(servicio.listar()).thenReturn(List.of(prorrata(2026, "80.00")));
        mockMvc.perform(get("/api/prorrata-iva").with(user("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].anio").value(2026))
                .andExpect(jsonPath("$.data[0].porcentaje").value(80.0))
                .andExpect(jsonPath("$.data[0].nota").value("n"))
                .andExpect(jsonPath("$.data[0].usuarioNombre").value("Ana Admin"));
    }

    @Test
    @DisplayName("crear: 200 con el registro y el usuario autenticado; el servicio recibe el DTO")
    void crear_ok() throws Exception {
        when(servicio.crear(any(), eq(admin))).thenReturn(prorrata(2026, "85.50"));
        mockMvc.perform(post("/api/prorrata-iva").with(user("admin")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"anio\":2026,\"porcentaje\":85.5,\"nota\":\"n\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.anio").value(2026));
        verify(servicio).crear(any(ProrrataIvaDTO.class), eq(admin));
    }

    @Test
    @DisplayName("crear: 400 con el mensaje de validación y 409 si el año ya existe")
    void crear_errores() throws Exception {
        when(servicio.crear(any(), any())).thenThrow(new IllegalArgumentException("El porcentaje de prorrata debe estar entre 0 y 100"));
        mockMvc.perform(post("/api/prorrata-iva").with(user("admin")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"anio\":2026,\"porcentaje\":101}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El porcentaje de prorrata debe estar entre 0 y 100"));
        reset(servicio);
        when(servicio.crear(any(), any())).thenThrow(new IllegalStateException("Ya existe un porcentaje de prorrata para 2026; corríjalo en lugar de crear otro"));
        mockMvc.perform(post("/api/prorrata-iva").with(user("admin")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"anio\":2026,\"porcentaje\":80}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("corregir: 200; 404 si el año no existe; 400 con porcentaje inválido")
    void corregir() throws Exception {
        when(servicio.corregir(eq(2026), any(), eq(admin))).thenReturn(prorrata(2026, "72.25"));
        mockMvc.perform(put("/api/prorrata-iva/2026").with(user("admin")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"porcentaje\":72.25}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.porcentaje").value(72.25));
        when(servicio.corregir(eq(2030), any(), any())).thenThrow(new NoSuchElementException("No hay porcentaje de prorrata configurado para 2030"));
        mockMvc.perform(put("/api/prorrata-iva/2030").with(user("admin")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"porcentaje\":50}"))
                .andExpect(status().isNotFound());
        when(servicio.corregir(eq(2027), any(), any())).thenThrow(new IllegalArgumentException("El porcentaje de prorrata debe estar entre 0 y 100"));
        mockMvc.perform(put("/api/prorrata-iva/2027").with(user("admin")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"porcentaje\":-1}"))
                .andExpect(status().isBadRequest());
    }

    // ── Consulta por año (formulario de CxP) ─────────────────────────────────

    @Test
    @DisplayName("consultar un año: lo puede hacer CUENTA_PAGAR_CREAR; configurado=true con el porcentaje o configurado=false")
    void consultarPorAnio() throws Exception {
        when(servicio.buscar(2026)).thenReturn(Optional.of(prorrata(2026, "80.00")));
        when(servicio.buscar(2031)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/prorrata-iva/2026").with(user("contador")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configurado").value(true))
                .andExpect(jsonPath("$.data.anio").value(2026))
                .andExpect(jsonPath("$.data.porcentaje").value(80.0));
        mockMvc.perform(get("/api/prorrata-iva/2031").with(user("contador")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configurado").value(false))
                .andExpect(jsonPath("$.data.anio").value(2031))
                .andExpect(jsonPath("$.data.porcentaje").doesNotExist());
        mockMvc.perform(get("/api/prorrata-iva/2026").with(user("admin"))).andExpect(status().isOk());
    }

    // ── Pantalla ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("la pantalla indica que corregir el porcentaje no recalcula las CxP registradas y que la liquidación anual es un asiento manual")
    void pantalla() throws Exception {
        mockMvc.perform(get("/contabilidad/prorrata-iva").with(user("admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Prorrata del IVA")))
                .andExpect(content().string(containsString("no recalcula las cuentas por pagar ya registradas")))
                .andExpect(content().string(containsString("liquidación anual")))
                .andExpect(content().string(containsString("asiento manual")))
                .andExpect(content().string(containsString("id=\"formProrrata\"")))
                .andExpect(content().string(containsString("id=\"tablaProrrata\"")))
                .andExpect(content().string(not(containsString("innerHTML"))));
    }
}
