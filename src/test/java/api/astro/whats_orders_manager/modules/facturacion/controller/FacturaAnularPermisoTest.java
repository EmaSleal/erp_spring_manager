package api.astro.whats_orders_manager.modules.facturacion.controller;

import api.astro.whats_orders_manager.modules.cliente.service.ClienteService;
import api.astro.whats_orders_manager.modules.facturacion.dto.mapper.FacturaMapper;
import api.astro.whats_orders_manager.modules.facturacion.dto.mapper.PagoMapper;
import api.astro.whats_orders_manager.modules.facturacion.service.FacturaPdfService;
import api.astro.whats_orders_manager.modules.facturacion.service.FacturaService;
import api.astro.whats_orders_manager.modules.facturacion.service.PagoService;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import api.astro.whats_orders_manager.shared.service.EmailService;
import api.astro.whats_orders_manager.shared.service.MonedaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST /facturas/anular/{id} está protegido por el permiso propio FACTURA_ANULAR
 * (ADR-008), no por FACTURA_ELIMINAR.
 *
 * Contexto mínimo: FacturaController real con @PreAuthorize activo (method security)
 * y un bean "permisoService" simulado que decide por código de permiso.
 */
@SpringJUnitWebConfig(FacturaAnularPermisoTest.Config.class)
@DisplayName("FacturaController.anularFactura — permiso FACTURA_ANULAR")
class FacturaAnularPermisoTest {

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class Config {

        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .csrf(csrf -> csrf.disable());
            return http.build();
        }

        @Bean(name = "permisoService")
        PermisoService permisoService() {
            return mock(PermisoService.class);
        }

        @Bean
        FacturaService facturaService() {
            return mock(FacturaService.class);
        }

        @Bean
        FacturaController facturaController(FacturaService facturaService) {
            return new FacturaController(
                    facturaService,
                    mock(ClienteService.class),
                    mock(PagoService.class),
                    mock(EmailService.class),
                    mock(MonedaService.class),
                    mock(FacturaMapper.class),
                    mock(FacturaPdfService.class),
                    mock(PagoMapper.class));
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private PermisoService permisoService;

    @Autowired
    private FacturaService facturaService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(permisoService, facturaService);
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Rol con FACTURA_ELIMINAR pero sin FACTURA_ANULAR recibe 403 y no se anula")
    void sinFacturaAnular_devuelve403() throws Exception {
        when(permisoService.tienePermisoPorCodigo(eq("vendedor"), anyString())).thenReturn(false);
        when(permisoService.tienePermisoPorCodigo("vendedor", "FACTURA_ELIMINAR")).thenReturn(true);

        mockMvc.perform(post("/facturas/anular/7").param("motivo", "prueba")
                        .with(user("vendedor").roles("ADMIN")))
                .andExpect(status().isForbidden());

        verify(permisoService).tienePermisoPorCodigo("vendedor", "FACTURA_ANULAR");
        verify(facturaService, never()).anular(anyInt(), anyString());
    }

    @Test
    @DisplayName("Rol con FACTURA_ANULAR anula la factura y redirige al listado")
    void conFacturaAnular_anulaYRedirige() throws Exception {
        when(permisoService.tienePermisoPorCodigo("gerente", "FACTURA_ANULAR")).thenReturn(true);

        mockMvc.perform(post("/facturas/anular/7").param("motivo", "prueba")
                        .with(user("gerente").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/facturas"));

        verify(facturaService).anular(7, "prueba");
        verify(permisoService, never()).tienePermisoPorCodigo("gerente", "FACTURA_ELIMINAR");
    }
}
