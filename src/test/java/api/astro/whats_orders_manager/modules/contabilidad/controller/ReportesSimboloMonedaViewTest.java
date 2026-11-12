package api.astro.whats_orders_manager.modules.contabilidad.controller;

import api.astro.whats_orders_manager.modules.contabilidad.service.CalculoContableService;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import api.astro.whats_orders_manager.shared.service.MonedaService;
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
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-031 (auditoría 7d #9) — el balance general y el estado de resultados muestran el símbolo de la moneda
 * funcional (el mismo mecanismo de los asientos: modelo simboloMoneda, por defecto ₡) y no un «$» fijo.
 */
@SpringJUnitWebConfig(ReportesSimboloMonedaViewTest.Config.class)
@DisplayName("ReporteContableController — símbolo de moneda funcional en los reportes")
class ReportesSimboloMonedaViewTest {

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
        CalculoContableService calculoContableService() {
            return mock(CalculoContableService.class);
        }

        @Bean
        MonedaService monedaService() {
            return mock(MonedaService.class);
        }

        @Bean
        ReporteContableController reporteContableController(CalculoContableService calculo, MonedaService moneda) {
            return new ReporteContableController(calculo, moneda);
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
    private CalculoContableService calculo;

    @Autowired
    private MonedaService monedaService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(permisoService, calculo, monedaService);
        when(permisoService.tienePermisoPorCodigo(anyString(), anyString())).thenReturn(true);
        when(monedaService.obtenerSimboloMoneda()).thenReturn("₡");
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static Map<String, Object> linea(String codigo, String saldo) {
        Map<String, Object> cuenta = new HashMap<>();
        cuenta.put("codigo", codigo);
        cuenta.put("nombre", "Cuenta " + codigo);
        Map<String, Object> linea = new HashMap<>();
        linea.put("cuenta", cuenta);
        linea.put("saldo", new BigDecimal(saldo));
        return linea;
    }

    private String render(String url) throws Exception {
        return mockMvc.perform(get(url).with(user("conta")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("balance general: montos con ₡, ninguno con prefijo «$»")
    void balanceGeneral_usaSimboloFuncional() throws Exception {
        Map<String, Object> balance = new HashMap<>();
        balance.put("activos", List.of(linea("1.1", "1500.50")));
        balance.put("pasivos", List.of(linea("2.1", "300.00")));
        balance.put("capital", List.of(linea("3.1", "1200.50")));
        balance.put("totalActivos", new BigDecimal("1500.50"));
        balance.put("totalPasivos", new BigDecimal("300.00"));
        balance.put("totalCapital", new BigDecimal("1200.50"));
        when(calculo.generarBalanceGeneral(any())).thenReturn(balance);

        String html = render("/contabilidad/reportes/balance-general?fecha=2026-10-31");

        assertThat(html).contains("₡1,500.50").contains("₡300.00").contains("₡1,200.50");
        assertThat(html).doesNotContainPattern("\\$\\s*[0-9]");
    }

    @Test
    @DisplayName("estado de resultados: montos con ₡, ninguno con prefijo «$»")
    void estadoResultados_usaSimboloFuncional() throws Exception {
        Map<String, Object> estado = new HashMap<>();
        estado.put("ingresos", List.of(linea("4.1", "900.00")));
        estado.put("egresos", List.of(linea("5.1", "400.00")));
        estado.put("totalIngresos", new BigDecimal("900.00"));
        estado.put("totalEgresos", new BigDecimal("400.00"));
        when(calculo.generarEstadoResultados(any(), any())).thenReturn(estado);

        String html = render("/contabilidad/reportes/estado-resultados?fechaInicio=2026-10-01&fechaFin=2026-10-31");

        assertThat(html).contains("₡900.00").contains("₡400.00").contains("₡500.00");
        assertThat(html).doesNotContainPattern("\\$\\s*[0-9]");
    }

    @Test
    @DisplayName("el símbolo sale de MonedaService (moneda funcional configurada), no de un valor fijo")
    void simboloConfigurado_seMuestra() throws Exception {
        when(monedaService.obtenerSimboloMoneda()).thenReturn("€");
        Map<String, Object> estado = new HashMap<>();
        estado.put("ingresos", List.of());
        estado.put("egresos", List.of());
        estado.put("totalIngresos", new BigDecimal("10.00"));
        estado.put("totalEgresos", BigDecimal.ZERO);
        when(calculo.generarEstadoResultados(any(), any())).thenReturn(estado);

        String html = render("/contabilidad/reportes/estado-resultados?fechaInicio=2026-10-01&fechaFin=2026-10-31");

        assertThat(html).contains("€10.00").doesNotContain("₡10.00");
    }
}
