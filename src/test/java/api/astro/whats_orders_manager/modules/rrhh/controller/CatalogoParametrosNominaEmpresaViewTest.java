package api.astro.whats_orders_manager.modules.rrhh.controller;

import api.astro.whats_orders_manager.modules.rrhh.model.ParametroNominaEmpresa;
import api.astro.whats_orders_manager.modules.rrhh.service.ParametroCCSSService;
import api.astro.whats_orders_manager.modules.rrhh.service.ParametroNominaEmpresaService;
import api.astro.whats_orders_manager.modules.rrhh.service.SalarioMinimoService;
import api.astro.whats_orders_manager.modules.rrhh.service.TramoImpuestoSalarioService;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-010 (ADR-013) — /rrhh/catalogos/parametros-ccss shows the read-only section
 * "Parámetros de nómina de la empresa" (current ASE employer rate and vigencia history),
 * protected by RRHH_VER_CATALOGO_SALARIAL, rendered with the real Thymeleaf template.
 */
@SpringJUnitWebConfig(CatalogoParametrosNominaEmpresaViewTest.Config.class)
@DisplayName("CatalogoRegulatorioViewController — parámetros de nómina de la empresa")
class CatalogoParametrosNominaEmpresaViewTest {

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
        ParametroCCSSService parametroCCSSService() {
            return mock(ParametroCCSSService.class);
        }

        @Bean
        ParametroNominaEmpresaService parametroNominaEmpresaService() {
            return mock(ParametroNominaEmpresaService.class);
        }

        @Bean
        TramoImpuestoSalarioService tramoImpuestoSalarioService() {
            return mock(TramoImpuestoSalarioService.class);
        }

        @Bean
        CatalogoRegulatorioViewController catalogoRegulatorioViewController(
                ParametroCCSSService ccss, ParametroNominaEmpresaService empresa, TramoImpuestoSalarioService tramos) {
            return new CatalogoRegulatorioViewController(ccss, empresa, tramos, mock(SalarioMinimoService.class));
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

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private PermisoService permisoService;

    @Autowired
    private ParametroCCSSService parametroCCSSService;

    @Autowired
    private ParametroNominaEmpresaService parametroNominaEmpresaService;

    @Autowired
    private TramoImpuestoSalarioService tramoImpuestoSalarioService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(permisoService, parametroCCSSService, parametroNominaEmpresaService, tramoImpuestoSalarioService);
        when(permisoService.tienePermisoPorCodigo(anyString(), anyString())).thenReturn(false);
        when(permisoService.tienePermisoPorCodigo(eq("rrhh"), eq("RRHH_VER_CATALOGO_SALARIAL"))).thenReturn(true);
        when(parametroCCSSService.findVigenteByFecha(any())).thenThrow(new NoSuchElementException("sin CCSS"));
        when(parametroCCSSService.findAll()).thenReturn(List.of());
        when(tramoImpuestoSalarioService.findByAnioVigencia(anyInt())).thenReturn(List.of());
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("user with RRHH_VER_CATALOGO_SALARIAL sees the current ASE rate (5,33 %) and the vigencia history")
    void conPermiso_muestraTasaVigenteEHistorial() throws Exception {
        ParametroNominaEmpresa anterior = build("0.0500", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
        ParametroNominaEmpresa vigente = build("0.0533", LocalDate.of(2026, 1, 1), null);
        when(parametroNominaEmpresaService.findVigenteByFecha(any())).thenReturn(vigente);
        when(parametroNominaEmpresaService.findAll()).thenReturn(List.of(anterior, vigente));

        mockMvc.perform(get("/rrhh/catalogos/parametros-ccss").with(user("rrhh")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Parámetros de nómina de la empresa")))
                .andExpect(content().string(containsString("5,33 %")))
                .andExpect(content().string(containsString("5,00 %")))
                .andExpect(content().string(containsString("01/01/2025")))
                .andExpect(content().string(containsString("31/12/2025")))
                .andExpect(content().string(containsString("Historial de parámetros de nómina de la empresa")))
                .andExpect(content().string(not(containsString("<form"))));
    }

    @Test
    @DisplayName("page still renders when there is no current company payroll record")
    void sinRegistroVigente_renderizaConAviso() throws Exception {
        when(parametroNominaEmpresaService.findVigenteByFecha(any()))
                .thenThrow(new NoSuchElementException("sin registro"));
        when(parametroNominaEmpresaService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/rrhh/catalogos/parametros-ccss").with(user("rrhh")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Parámetros de nómina de la empresa")))
                .andExpect(content().string(containsString(
                        "No se encontró un registro de parámetros de nómina de la empresa vigente")));
    }

    @Test
    @DisplayName("user without RRHH_VER_CATALOGO_SALARIAL gets 403")
    void sinPermiso_403() throws Exception {
        mockMvc.perform(get("/rrhh/catalogos/parametros-ccss").with(user("vendedor")))
                .andExpect(status().isForbidden());

        verify(parametroNominaEmpresaService, never()).findAll();
    }

    @Test
    @DisplayName("the catalog controller exposes only GET endpoints (no write endpoint)")
    void soloEndpointsGet() {
        RequestMappingHandlerMapping mapping = context.getBean(RequestMappingHandlerMapping.class);
        List<RequestMappingInfo> infos = mapping.getHandlerMethods().entrySet().stream()
                .filter(e -> e.getValue().getBeanType().equals(CatalogoRegulatorioViewController.class))
                .map(java.util.Map.Entry::getKey)
                .toList();

        assertThat(infos).isNotEmpty();
        assertThat(infos).allSatisfy(info ->
                assertThat(info.getMethodsCondition().getMethods()).isEqualTo(Set.of(RequestMethod.GET)));
    }

    private ParametroNominaEmpresa build(String tasa, LocalDate desde, LocalDate hasta) {
        ParametroNominaEmpresa p = new ParametroNominaEmpresa();
        p.setPorcentajeAportePatronalAse(new BigDecimal(tasa));
        p.setVigenciaDesde(desde);
        p.setVigenciaHasta(hasta);
        return p;
    }
}
