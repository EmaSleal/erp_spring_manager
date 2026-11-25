package api.astro.whats_orders_manager.modules.facturacion.controller;

import api.astro.whats_orders_manager.modules.regimen.panel.RegimenPanel;
import api.astro.whats_orders_manager.modules.regimen.panel.RegimenPanelService;
import api.astro.whats_orders_manager.modules.regimen.tope.EstadoTope;
import api.astro.whats_orders_manager.modules.regimen.tope.NivelTope;
import api.astro.whats_orders_manager.modules.regimen.tope.TopeComprasRtsService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.ConfiguracionHaciendaNoDisponibleException;
import api.astro.whats_orders_manager.modules.facturacion.service.PanelFacturacionService;
import api.astro.whats_orders_manager.modules.facturacion.service.PanelFacturacionService.Contadores;
import api.astro.whats_orders_manager.modules.facturacion.service.PanelFacturacionService.Panel;
import api.astro.whats_orders_manager.modules.facturacion.service.PanelFacturacionService.TarjetaHacienda;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-043 (criterios 1, 2, 6, 7 y 9): GET /facturas/inicio con la plantilla real. 200 con FACTURA_VER y 403 sin él;
 * sin montos ni gráficos; enlaces filtrados; tarjeta de Hacienda (legible, sin activa, ilegible) siempre con 200; la
 * tarjeta de Configuración de Hacienda solo para ADMIN.
 */
@SpringJUnitWebConfig(FacturacionInicioViewTest.Config.class)
@DisplayName("T-043 — página de inicio de Facturación")
class FacturacionInicioViewTest {

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
        PanelFacturacionService panelService() {
            return mock(PanelFacturacionService.class);
        }

        @Bean
        RegimenPanelService regimenPanelService() {
            return mock(RegimenPanelService.class);
        }

        @Bean
        FacturacionInicioController facturacionInicioController(PanelFacturacionService panel, RegimenPanelService regimen) {
            return new FacturacionInicioController(panel, regimen);
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
    @Autowired private PanelFacturacionService panelService;
    @Autowired private RegimenPanelService regimenPanelService;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        reset(permisoService, panelService, regimenPanelService);
        when(permisoService.tienePermisoPorCodigo(anyString(), anyString())).thenReturn(false);
        when(permisoService.tienePermisoPorCodigo(eq("gerente"), eq("FACTURA_VER"))).thenReturn(true);
        when(permisoService.tienePermisoPorCodigo(eq("admin"), eq("FACTURA_VER"))).thenReturn(true);
        when(panelService.obtener()).thenReturn(panel(tarjetaOk()));
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static Panel panel(TarjetaHacienda tarjeta) {
        return new Panel(new Contadores(3, 0, 12, 1, 4, 2, 5, 7, 6, 8, 9), tarjeta,
            tarjeta.disponible() ? tarjeta.ambienteEnum() : null);
    }

    private static TarjetaHacienda tarjetaOk() {
        return new TarjetaHacienda(null, "SANDBOX", false, "Vence el 2027-03-01 (en 102 días)", 102L, false, true,
            LocalDateTime.of(2026, 11, 19, 14, 30));
    }

    private String html(String usuario, String rol) throws Exception {
        MvcResult r = mvc.perform(get("/facturas/inicio").with(user(usuario).roles(rol)))
            .andExpect(status().isOk()).andReturn();
        return r.getResponse().getContentAsString();
    }

    @Test
    @DisplayName("sin FACTURA_VER responde 403")
    void sinPermiso() throws Exception {
        mvc.perform(get("/facturas/inicio").with(user("otro").roles("USER"))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("con FACTURA_VER responde 200 con los contadores y sus enlaces filtrados")
    void conPermiso() throws Exception {
        String html = html("gerente", "USER");

        assertThat(html).contains("Comprobantes con error", "Comprobantes con envío incierto", "Facturas sin comprobante",
            "Artículos activos sin CAByS", "Clientes con datos FE inválidos");
        assertThat(html).contains("/facturas/comprobantes?estado=ERROR&amp;vigente=true&amp;ambiente=SANDBOX",
            "/facturas/comprobantes?estado=NO_TRANSMITIDO&amp;vigente=true&amp;ambiente=SANDBOX",
            "/facturas/comprobantes?estado=ENVIO_INCIERTO&amp;vigente=true&amp;ambiente=SANDBOX",
            "/facturas/comprobantes?estado=RECHAZADO&amp;vigente=true&amp;ambiente=SANDBOX",
            "/facturas/notas-credito?estado=EN_TRAMITE&amp;ambiente=SANDBOX",
            "/facturas?sustitutaPendiente=true", "/facturas?estadoFE=sin_fe", "/articulos?sinCabys=true",
            "/clientes?filtro=feInvalidos");
        // los números del panel salen en h3.stat-number con data-contador
        assertThat(html).containsPattern("data-contador[^>]*>12<");
    }

    @Test
    @DisplayName("ninguna tarjeta lleva montos, símbolos de moneda ni gráficos (ADR-048 §3)")
    void sinMontosNiGraficos() throws Exception {
        String cuerpo = html("gerente", "USER");
        // solo el contenido del <main>: la plantilla compartida (navbar, layout) no es parte del panel
        String main = cuerpo.substring(cuerpo.indexOf("<main"), cuerpo.indexOf("</main>"));

        assertThat(main).doesNotContain("₡", "$", "US$", "CRC", "USD", "<canvas", "chart", "Chart");
        assertThat(main).doesNotContainPattern("\\d[\\d.,]*\\s*(colones|dólares)");
        assertThat(cuerpo).doesNotContain("<canvas"); // chart.js lo carga el layout compartido; el panel no dibuja nada
    }

    @Test
    @DisplayName("tarjeta de Hacienda legible: ambiente, certificado y token con hora; sin datos sensibles")
    void tarjetaLegible() throws Exception {
        String html = html("gerente", "USER");

        assertThat(html).contains("id=\"hacienda-ambiente\"", "SANDBOX", "Vence el 2027-03-01 (en 102 días)",
            "Sí, expira 19/11/2026 14:30");
        assertThat(html).doesNotContain("id=\"hacienda-aviso\"", "bg-danger\">SANDBOX");
        assertThat(html).doesNotContainIgnoringCase("password").doesNotContain("pin-");
    }

    @Test
    @DisplayName("PRODUCCION se destaca y el certificado por vencer o vencido se marca en alerta")
    void produccionYAlerta() throws Exception {
        when(panelService.obtener()).thenReturn(panel(new TarjetaHacienda(null, "PRODUCCION", true,
            "Vencido el 2026-10-01", -49L, true, false, null)));

        String html = html("gerente", "USER");

        assertThat(html).containsPattern("badge bg-danger\"[^>]*>\\s*PRODUCCION");
        assertThat(html).containsPattern("text-danger fw-bold\"[^>]*>Vencido el 2026-10-01");
        assertThat(html).containsPattern("id=\"hacienda-token\">\\s*<span>No</span>");
    }

    @Test
    @DisplayName("certificado ilegible: «No se pudo leer el certificado» y la página responde 200")
    void certificadoIlegible() throws Exception {
        when(panelService.obtener()).thenReturn(panel(new TarjetaHacienda(null, "SANDBOX", false,
            PanelFacturacionService.MENSAJE_CERTIFICADO_ILEGIBLE, null, true, false, null)));

        assertThat(html("gerente", "USER")).contains("No se pudo leer el certificado");
    }

    @Test
    @DisplayName("sin configuración activa: aviso fijo de T-041, 200")
    void sinActiva() throws Exception {
        when(panelService.obtener()).thenReturn(panel(
            TarjetaHacienda.conAviso(ConfiguracionHaciendaNoDisponibleException.MENSAJE_NO_VALIDA)));

        String html = html("gerente", "USER");

        assertThat(html).contains("id=\"hacienda-aviso\"")
            .contains(ConfiguracionHaciendaNoDisponibleException.MENSAJE_NO_VALIDA)
            .doesNotContain("id=\"hacienda-ambiente\"");
    }

    @Test
    @DisplayName("credenciales ilegibles: aviso fijo de T-041, 200")
    void ilegible() throws Exception {
        when(panelService.obtener()).thenReturn(panel(
            TarjetaHacienda.conAviso(ConfiguracionHaciendaNoDisponibleException.MENSAJE_ILEGIBLE)));

        String html = html("gerente", "USER");

        assertThat(html).contains("id=\"hacienda-aviso\"")
            .contains(ConfiguracionHaciendaNoDisponibleException.MENSAJE_ILEGIBLE);
    }

    @Test
    @DisplayName("accesos: Facturas, Notas de crédito y Comprobantes con FACTURA_VER; Configuración de Hacienda solo ADMIN")
    void accesos() throws Exception {
        String usuario = html("gerente", "USER");
        String admin = html("admin", "ADMIN");

        for (String h : new String[]{usuario, admin}) {
            assertThat(h).contains("href=\"/facturas\"", "href=\"/facturas/notas-credito\"", "href=\"/facturas/comprobantes\"");
        }
        assertThat(usuario).doesNotContain("/facturas/configuracion-hacienda");
        assertThat(admin).contains("href=\"/facturas/configuracion-hacienda\"");
    }

    @Test
    @DisplayName("review 1: el encabezado dice el ambiente de los contadores y los enlaces lo llevan (PRODUCCION)")
    void ambienteDeLosContadores() throws Exception {
        when(panelService.obtener()).thenReturn(panel(new TarjetaHacienda(null, "PRODUCCION", true,
            "Vence el 2027-03-01 (en 102 días)", 102L, false, true, LocalDateTime.of(2026, 11, 19, 14, 30))));

        String html = html("gerente", "USER");

        assertThat(html).contains("Contadores del ambiente PRODUCCION")
            .doesNotContain(PanelFacturacionService.LEYENDA_SIN_AMBIENTE);
        assertThat(html).contains("/facturas/comprobantes?estado=RECHAZADO&amp;vigente=true&amp;ambiente=PRODUCCION",
            "/facturas/notas-credito?estado=EN_TRAMITE&amp;ambiente=PRODUCCION");
    }

    @Test
    @DisplayName("review 1: sin ambiente activo o con credenciales ilegibles, leyenda fija y enlaces sin ambiente, 200")
    void sinAmbienteActivo() throws Exception {
        for (String mensaje : new String[]{ConfiguracionHaciendaNoDisponibleException.MENSAJE_NO_VALIDA,
            ConfiguracionHaciendaNoDisponibleException.MENSAJE_ILEGIBLE}) {
            when(panelService.obtener()).thenReturn(panel(TarjetaHacienda.conAviso(mensaje)));

            String html = html("gerente", "USER");

            assertThat(html).contains("Sin ambiente activo: los contadores incluyen todos los ambientes")
                .doesNotContain("Contadores del ambiente");
            assertThat(html).contains("/facturas/comprobantes?estado=RECHAZADO&amp;vigente=true\"")
                .doesNotContain("ambiente=");
        }
    }

    // ── T-056: bloque de régimen ─────────────────────────────────────────────

    @Test
    @DisplayName("T-056: sin parámetros del año el panel dice «Tope de 2027 sin configurar» y que solo cuenta lo registrado en el ERP")
    void regimen_topeSinConfigurar() throws Exception {
        when(regimenPanelService.obtener()).thenReturn(new RegimenPanel(null, EstadoTope.sinConfigurar(2027),
                TopeComprasRtsService.ADVERTENCIA_SOLO_ERP));
        String html = html("gerente", "GERENTE");
        assertThat(html).contains("id=\"regimen-tope\"").contains("Tope de 2027 sin configurar")
                .contains(TopeComprasRtsService.ADVERTENCIA_SOLO_ERP).doesNotContain("id=\"regimen-verificacion-aviso\"");
    }

    @Test
    @DisplayName("T-056: aviso de verificación y tope en AVISO se muestran con texto escapado")
    void regimen_avisos() throws Exception {
        EstadoTope tope = new EstadoTope(2026, true, new BigDecimal("85969200.00"), new BigDecimal("70000000.00"),
                new BigDecimal("81.43"), NivelTope.AVISO, new BigDecimal("80.00"), new BigDecimal("100.00"),
                "Compras al 81,43 % del tope");
        when(regimenPanelService.obtener()).thenReturn(new RegimenPanel("Hacienda <b>difiere</b>", tope,
                TopeComprasRtsService.ADVERTENCIA_SOLO_ERP));
        String html = html("gerente", "GERENTE");
        assertThat(html).contains("id=\"regimen-verificacion-aviso\"").contains("Hacienda &lt;b&gt;difiere&lt;/b&gt;")
                .doesNotContain("<b>difiere</b>").contains("alert-warning").contains("70.000.000,00").contains("81,43");
    }

    @Test
    @DisplayName("T-056: si el bloque de régimen no está disponible (nulo) el panel igual responde 200")
    void regimen_nulo() throws Exception {
        when(regimenPanelService.obtener()).thenReturn(null);
        assertThat(html("gerente", "GERENTE")).doesNotContain("id=\"regimen-tope\"");
    }
}
