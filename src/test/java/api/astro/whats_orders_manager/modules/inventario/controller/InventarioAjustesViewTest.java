package api.astro.whats_orders_manager.modules.inventario.controller;

import api.astro.whats_orders_manager.modules.inventario.service.AjusteInventarioService;
import api.astro.whats_orders_manager.modules.inventario.service.ReporteInventarioService;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.repository.ProductoRepository;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-031 (P-053) — /inventario/ajustes se renderiza completa con la plantilla Thymeleaf real:
 * la secuencia «[/» dentro del script inline rompía el parser de Thymeleaf (TemplateInputException).
 */
@SpringJUnitWebConfig(InventarioAjustesViewTest.Config.class)
@DisplayName("InventarioViewController — página de ajustes")
class InventarioAjustesViewTest {

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
        ProductoRepository productoRepository() {
            return mock(ProductoRepository.class);
        }

        @Bean
        AjusteInventarioService ajusteInventarioService() {
            return mock(AjusteInventarioService.class);
        }

        @Bean
        InventarioViewController inventarioViewController(ProductoRepository repo, AjusteInventarioService ajustes) {
            return new InventarioViewController(repo, mock(ReporteInventarioService.class), ajustes);
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
    private ProductoRepository productoRepository;

    @Autowired
    private AjusteInventarioService ajusteInventarioService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(permisoService, productoRepository, ajusteInventarioService);
        when(permisoService.tienePermisoPorCodigo(anyString(), anyString())).thenReturn(true);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private Producto producto(int id, String descripcion, int stock) {
        Producto p = new Producto();
        p.setIdProducto(id);
        p.setDescripcion(descripcion);
        p.setStock(stock);
        return p;
    }

    @Test
    @DisplayName("/inventario/ajustes renderiza completa (termina en </html>) y productosData lleva cada producto")
    void ajustes_renderizaCompletaConProductosData() throws Exception {
        when(ajusteInventarioService.findAll()).thenReturn(List.of());
        when(productoRepository.findAll()).thenReturn(List.of(producto(7, "Arroz", 42), producto(9, "Frijol", 5)));

        String html = mockMvc.perform(get("/inventario/ajustes").with(user("inv")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html.trim()).endsWith("</html>");
        assertThat(html).contains("const productosData = {};");
        assertThat(html).containsPattern("productosData\\[\\s*7\\s*\\] = \\{");
        assertThat(html).containsPattern("productosData\\[\\s*9\\s*\\] = \\{");
        assertThat(html).contains("stock: 42").contains("stock: 5");
        assertThat(html).contains("\"Arroz\"").contains("\"Frijol\"");
    }
}
