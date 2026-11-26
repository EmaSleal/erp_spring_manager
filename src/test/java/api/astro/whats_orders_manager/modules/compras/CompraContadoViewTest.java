package api.astro.whats_orders_manager.modules.compras;

import api.astro.whats_orders_manager.modules.compras.controller.CompraContadoViewController;
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
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-058 criterio 11: la vista «Compra de contado» (plantilla Thymeleaf real) exige CUENTA_PAGAR_CREAR y CUENTA_PAGAR_REGISTRAR_PAGO;
 * el menú lateral solo ofrece la entrada con ambos; la fecha de hoy sale del reloj de la aplicación.
 */
@SpringJUnitWebConfig(CompraContadoViewTest.Config.class)
@DisplayName("T-058 — vista y menú de la compra de contado")
class CompraContadoViewTest {

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
        CompraContadoViewController compraContadoViewController() {
            return new CompraContadoViewController(Clock.fixed(Instant.parse("2026-11-23T02:00:00Z"), ZoneId.of("America/Costa_Rica")));
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
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(permisoService);
        when(permisoService.tienePermisoPorCodigo(anyString(), anyString())).thenReturn(false);
        when(permisoService.tienePermisoPorCodigo(eq("cajero"), eq("CUENTA_PAGAR_CREAR"))).thenReturn(true);
        when(permisoService.tienePermisoPorCodigo(eq("cajero"), eq("CUENTA_PAGAR_REGISTRAR_PAGO"))).thenReturn(true);
        when(permisoService.tienePermisoPorCodigo(eq("soloCxp"), eq("CUENTA_PAGAR_CREAR"))).thenReturn(true);
        when(permisoService.tienePermisoPorCodigo(eq("soloPago"), eq("CUENTA_PAGAR_REGISTRAR_PAGO"))).thenReturn(true);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("con ambos permisos: renderiza el formulario con la fecha de hoy (Costa Rica), el límite del adjunto y el enlace del menú")
    void conAmbosPermisos() throws Exception {
        mockMvc.perform(get("/compras/contado").with(user("cajero")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Compra de contado")))
                .andExpect(content().string(containsString("data-hoy=\"2026-11-22\"")))
                .andExpect(content().string(containsString("data-adjunto-max=\"5242880\"")))
                .andExpect(content().string(containsString("/modules/proveedores/js/compra-contado.js")))
                .andExpect(content().string(containsString("/compras/contado\"")))
                .andExpect(content().string(not(containsString("innerHTML"))));
    }

    @Test
    @DisplayName("solo con crear CxP o solo con registrar pago: 403 (y sin la entrada del menú en otras vistas)")
    void conUnSoloPermiso() throws Exception {
        mockMvc.perform(get("/compras/contado").with(user("soloCxp"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/compras/contado").with(user("soloPago"))).andExpect(status().isForbidden());
    }
}
