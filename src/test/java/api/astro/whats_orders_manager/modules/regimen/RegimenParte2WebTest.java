package api.astro.whats_orders_manager.modules.regimen;

import api.astro.whats_orders_manager.modules.regimen.tope.EstadoTope;
import api.astro.whats_orders_manager.modules.regimen.tope.NivelTope;
import api.astro.whats_orders_manager.modules.regimen.tope.TopeComprasRts;
import api.astro.whats_orders_manager.modules.regimen.tope.TopeComprasRtsRestController;
import api.astro.whats_orders_manager.modules.regimen.tope.TopeComprasRtsService;
import api.astro.whats_orders_manager.modules.regimen.tope.TopeComprasRtsViewController;
import api.astro.whats_orders_manager.modules.regimen.verificacion.ResultadoVerificacion;
import api.astro.whats_orders_manager.modules.regimen.verificacion.VerificacionRegimen;
import api.astro.whats_orders_manager.modules.regimen.verificacion.VerificacionRegimenRestController;
import api.astro.whats_orders_manager.modules.regimen.verificacion.VerificacionRegimenService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
 * T-056 (criterios 5 y 7): API y pantalla del tope de compras del RTS (REGIMEN_TOPE_COMPRAS_GESTIONAR) y API de la
 * verificación con Hacienda (ejecutarla exige REGIMEN_TRIBUTARIO_GESTIONAR; leer la última también lo permite CONFIG_VER).
 */
@SpringJUnitWebConfig(RegimenParte2WebTest.Config.class)
@DisplayName("T-056 — API y pantalla del tope de compras del RTS y API de la verificación (permisos y errores)")
class RegimenParte2WebTest {

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
        TopeComprasRtsService topeService() {
            return mock(TopeComprasRtsService.class);
        }

        @Bean
        VerificacionRegimenService verificacionService() {
            return mock(VerificacionRegimenService.class);
        }

        @Bean
        UsuarioService usuarioService() {
            return mock(UsuarioService.class);
        }

        @Bean
        TopeComprasRtsRestController topeRest(TopeComprasRtsService s, UsuarioService u) {
            return new TopeComprasRtsRestController(s, u);
        }

        @Bean
        java.time.Clock clock() {
            return java.time.Clock.fixed(java.time.Instant.parse("2026-11-25T16:00:00Z"), java.time.ZoneId.of("America/Costa_Rica"));
        }

        @Bean
        TopeComprasRtsViewController topeView(java.time.Clock clock) {
            return new TopeComprasRtsViewController(clock);
        }

        @Bean
        VerificacionRegimenRestController verificacionRest(VerificacionRegimenService s, UsuarioService u) {
            return new VerificacionRegimenRestController(s, u);
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
    @Autowired private TopeComprasRtsService tope;
    @Autowired private VerificacionRegimenService verificacion;
    @Autowired private UsuarioService usuarioService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(permisoService, tope, verificacion, usuarioService);
        when(permisoService.tienePermisoPorCodigo(anyString(), anyString())).thenReturn(false);
        when(permisoService.tienePermisoPorCodigo(eq("admin"), eq("REGIMEN_TOPE_COMPRAS_GESTIONAR"))).thenReturn(true);
        when(permisoService.tienePermisoPorCodigo(eq("admin"), eq("REGIMEN_TRIBUTARIO_GESTIONAR"))).thenReturn(true);
        when(permisoService.tienePermisoPorCodigo(eq("lector"), eq("CONFIG_VER"))).thenReturn(true);
        Usuario admin = mock(Usuario.class);
        when(admin.getIdUsuario()).thenReturn(7);
        when(admin.getNombre()).thenReturn("Ana Admin");
        when(usuarioService.findByEmail("admin")).thenReturn(Optional.of(admin));
        when(usuarioService.findById(7)).thenReturn(Optional.of(admin));
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static TopeComprasRts parametros(int anio) {
        return TopeComprasRts.builder().id(1L).anio(anio).salarioBase(new BigDecimal("462200.00"))
                .multiplicador(new BigDecimal("186.00")).umbralAviso(new BigDecimal("80.00")).umbralTope(new BigDecimal("100.00"))
                .nota("n").usuarioId(7).fechaRegistro(LocalDateTime.of(2026, 11, 1, 8, 0))
                .fechaActualizacion(LocalDateTime.of(2026, 11, 1, 8, 0)).build();
    }

    // ── Tope: permisos ───────────────────────────────────────────────────────

    @Test
    @DisplayName("sin REGIMEN_TOPE_COMPRAS_GESTIONAR: listar, estado, crear, corregir y la pantalla responden 403")
    void tope_sinPermiso_403() throws Exception {
        for (String usuario : new String[] {"contador", "lector"}) {
            mockMvc.perform(get("/api/tope-compras-rts").with(user(usuario))).andExpect(status().isForbidden());
            mockMvc.perform(get("/api/tope-compras-rts/2026/estado").with(user(usuario))).andExpect(status().isForbidden());
            mockMvc.perform(post("/api/tope-compras-rts").with(user(usuario)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"anio\":2026,\"salarioBase\":462200}")).andExpect(status().isForbidden());
            mockMvc.perform(put("/api/tope-compras-rts/2026").with(user(usuario)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"salarioBase\":462200}")).andExpect(status().isForbidden());
            mockMvc.perform(get("/configuracion/tope-compras-rts").with(user(usuario))).andExpect(status().isForbidden());
        }
        verifyNoInteractions(tope);
    }

    @Test
    @DisplayName("con el permiso: lista, crea (200), 400/409 con el mensaje fijo, corrige y 404; la pantalla es 200")
    void tope_conPermiso() throws Exception {
        when(tope.listar()).thenReturn(List.of(parametros(2026)));
        mockMvc.perform(get("/api/tope-compras-rts").with(user("admin"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].anio").value(2026))
                .andExpect(jsonPath("$.data[0].usuarioNombre").value("Ana Admin"))
                .andExpect(jsonPath("$.data[0].tope").value(85969200.00));

        when(tope.crear(any(), any())).thenReturn(parametros(2026));
        mockMvc.perform(post("/api/tope-compras-rts").with(user("admin")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"anio\":2026,\"salarioBase\":462200}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        doThrow(new IllegalArgumentException(TopeComprasRtsService.MSG_SALARIO)).when(tope).crear(any(), any());
        mockMvc.perform(post("/api/tope-compras-rts").with(user("admin")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"anio\":2026}")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(TopeComprasRtsService.MSG_SALARIO));
        doThrow(new IllegalStateException("Ya existe un tope para 2026")).when(tope).crear(any(), any());
        mockMvc.perform(post("/api/tope-compras-rts").with(user("admin")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"anio\":2026,\"salarioBase\":1}")).andExpect(status().isConflict());

        doThrow(new NoSuchElementException("No hay tope para 2030")).when(tope).corregir(eq(2030), any(), any());
        mockMvc.perform(put("/api/tope-compras-rts/2030").with(user("admin")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"salarioBase\":1}")).andExpect(status().isNotFound());

        mockMvc.perform(get("/configuracion/tope-compras-rts").with(user("admin"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("tope-compras-rts.js")));
    }

    @Test
    @DisplayName("estado del año: acumulado, tope, porcentaje y la advertencia de que solo cuenta lo registrado en el ERP")
    void tope_estado() throws Exception {
        when(tope.estado(2026)).thenReturn(new EstadoTope(2026, true, new BigDecimal("85969200.00"),
                new BigDecimal("70000000.00"), new BigDecimal("81.43"), NivelTope.AVISO, new BigDecimal("80.00"),
                new BigDecimal("100.00"), "aviso"));
        mockMvc.perform(get("/api/tope-compras-rts/2026/estado").with(user("admin"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nivel").value("AVISO"))
                .andExpect(jsonPath("$.data.advertencia").value(TopeComprasRtsService.ADVERTENCIA_SOLO_ERP));
    }

    // ── Verificación ─────────────────────────────────────────────────────────

    private static VerificacionRegimen verificacionGuardada() {
        return VerificacionRegimen.builder().id(5L).fechaHora(LocalDateTime.of(2026, 11, 25, 10, 0))
                .regimenLocal("GENERAL").codigoHacienda(2).descripcionHacienda("Régimen Simplificado")
                .regimenHacienda("SIMPLIFICADO").resultado(ResultadoVerificacion.DIFIERE).build();
    }

    @Test
    @DisplayName("verificar a mano exige REGIMEN_TRIBUTARIO_GESTIONAR; sin él, 403 y no consulta")
    void verificar_sinPermiso_403() throws Exception {
        mockMvc.perform(post("/api/regimen/verificacion").with(user("lector")).with(csrf())).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/regimen/verificacion").with(user("contador"))).andExpect(status().isForbidden());
        verifyNoInteractions(verificacion);
    }

    @Test
    @DisplayName("verificar a mano devuelve el resultado con los dos valores; CONFIG_VER solo lee la última")
    void verificar_conPermiso() throws Exception {
        when(verificacion.verificar(7)).thenReturn(verificacionGuardada());
        mockMvc.perform(post("/api/regimen/verificacion").with(user("admin")).with(csrf())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resultado").value("DIFIERE"))
                .andExpect(jsonPath("$.data.regimenLocal").value("GENERAL"))
                .andExpect(jsonPath("$.data.codigoHacienda").value(2))
                .andExpect(jsonPath("$.data.descripcionHacienda").value("Régimen Simplificado"));
        verify(verificacion).verificar(7);

        when(verificacion.ultima()).thenReturn(Optional.of(verificacionGuardada()));
        mockMvc.perform(get("/api/regimen/verificacion").with(user("lector"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resultado").value("DIFIERE"));
        when(verificacion.ultima()).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/regimen/verificacion").with(user("lector"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
