package api.astro.whats_orders_manager.modules.proveedores.controller;

import api.astro.whats_orders_manager.modules.proveedores.enums.EstadoOrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.model.DetalleOrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.model.OrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import api.astro.whats_orders_manager.modules.proveedores.service.OrdenCompraService;
import api.astro.whats_orders_manager.modules.proveedores.service.ProveedorService;
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
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-053: la plantilla real de órdenes de compra. El botón de generar CPP ya no existe; cada orden tiene su detalle con las
 * facturas del proveedor; «Registrar factura del proveedor» y su formulario solo con CUENTA_PAGAR_CREAR; el aviso de la
 * recepción depende del control de inventario.
 */
@SpringJUnitWebConfig(OrdenesCompraVistaTest.Config.class)
@DisplayName("T-053 — vista de órdenes de compra (detalle, facturas del proveedor y recepción)")
class OrdenesCompraVistaTest {

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
        OrdenCompraService ordenCompraService() {
            return mock(OrdenCompraService.class);
        }

        @Bean
        ProveedorService proveedorService() {
            return mock(ProveedorService.class);
        }

        @Bean
        MonedaService monedaService() {
            return mock(MonedaService.class);
        }

        @Bean
        ProveedorViewController proveedorViewController(ProveedorService proveedores, OrdenCompraService ordenes) {
            return new ProveedorViewController(proveedores, ordenes);
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
    @Autowired private OrdenCompraService ordenCompraService;
    @Autowired private ProveedorService proveedorService;
    @Autowired private MonedaService monedaService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(permisoService, ordenCompraService, proveedorService, monedaService);
        when(monedaService.obtenerSimboloMoneda()).thenReturn("₡");
        when(permisoService.tienePermisoPorCodigo(anyString(), anyString())).thenReturn(false);
        for (String u : List.of("lector", "contador")) {
            when(permisoService.tienePermisoPorCodigo(eq(u), eq("ORDEN_COMPRA_VER"))).thenReturn(true);
        }
        when(permisoService.tienePermisoPorCodigo(eq("contador"), eq("CUENTA_PAGAR_CREAR"))).thenReturn(true);
        Proveedor proveedor = new Proveedor();
        proveedor.setId(7L);
        proveedor.setNombre("Distribuidora S.A.");
        OrdenCompra oc = OrdenCompra.builder().id(9L).numero("OC-2026-00009").proveedor(proveedor)
                .estado(EstadoOrdenCompra.RECIBIDA).fecha(LocalDate.of(2026, 10, 1)).total(new BigDecimal("113000.00")).build();
        oc.getDetalles().add(DetalleOrdenCompra.builder().id(1L).ordenCompra(oc).descripcion("Flete").cantidad(1)
                .precioUnitario(new BigDecimal("500.00")).build());
        when(ordenCompraService.findAll()).thenReturn(List.of(oc));
        when(ordenCompraService.recepcionMueveInventario()).thenReturn(false);
        when(proveedorService.findActivos()).thenReturn(List.of(proveedor));
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("sin CUENTA_PAGAR_CREAR: botón de detalle sí; sin «Registrar factura del proveedor» ni su formulario; sin botón de generar CPP")
    void sinPermisoCrear() throws Exception {
        mockMvc.perform(get("/proveedores/ordenes-compra").with(user("lector")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("btn-detalle-oc")))
                .andExpect(content().string(containsString("id=\"modalDetalleOC\"")))
                .andExpect(content().string(not(containsString("id=\"btnRegistrarFacturaOc\""))))
                .andExpect(content().string(not(containsString("id=\"modalFacturaOC\""))))
                .andExpect(content().string(not(containsString("btn-generar-cpp"))))
                .andExpect(content().string(not(containsString("generar-cpp"))));
    }

    @Test
    @DisplayName("con CUENTA_PAGAR_CREAR: la acción «Registrar factura del proveedor» y el formulario de comprobante y líneas con el proveedor fijo")
    void conPermisoCrear() throws Exception {
        mockMvc.perform(get("/proveedores/ordenes-compra").with(user("contador")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btnRegistrarFacturaOc\"")))
                .andExpect(content().string(containsString("Registrar factura del proveedor")))
                .andExpect(content().string(containsString("id=\"modalFacturaOC\"")))
                .andExpect(content().string(containsString("id=\"facProveedor\"")))
                .andExpect(content().string(containsString("id=\"facNumeroComprobante\"")))
                .andExpect(content().string(containsString("id=\"facClaveComprobante\"")))
                .andExpect(content().string(containsString("id=\"facFechaComprobante\"")))
                .andExpect(content().string(containsString("id=\"facLineas\"")))
                .andExpect(content().string(containsString("/modules/contabilidad/js/cxp-lineas.js")))
                .andExpect(content().string(containsString("/modules/proveedores/js/oc-facturacion.js")))
                .andExpect(content().string(not(containsString("<select id=\"facProveedor\""))));
    }

    @Test
    @DisplayName("la nota de la recepción dice que sin control de inventario no se mueve el stock; con control, que solo se registran líneas con producto")
    void notaDeRecepcion() throws Exception {
        mockMvc.perform(get("/proveedores/ordenes-compra").with(user("lector")))
                .andExpect(content().string(containsString("no mueve el stock")))
                .andExpect(content().string(not(containsString("Solo se registran líneas con producto de catálogo"))));
        when(ordenCompraService.recepcionMueveInventario()).thenReturn(true);
        mockMvc.perform(get("/proveedores/ordenes-compra").with(user("lector")))
                .andExpect(content().string(containsString("Solo se registran líneas con producto de catálogo")))
                .andExpect(content().string(not(containsString("no mueve el stock"))));
    }
}
