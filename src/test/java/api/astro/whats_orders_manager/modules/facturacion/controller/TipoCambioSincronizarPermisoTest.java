package api.astro.whats_orders_manager.modules.facturacion.controller;

import api.astro.whats_orders_manager.modules.facturacion.dto.ResultadoSincronizacionTipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoSincronizacionTipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.service.TipoCambioService;
import api.astro.whats_orders_manager.modules.facturacion.service.TipoCambioSyncService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-008 criterio 7 — POST /api/tipos-cambio/sincronizar (sincronización manual) solo para
 * administradores (hasRole('ADMIN'), igual que el registro manual de tipos de cambio).
 */
@SpringJUnitWebConfig(TipoCambioSincronizarPermisoTest.Config.class)
@DisplayName("TipoCambioRestController.sincronizar — solo ADMIN")
class TipoCambioSincronizarPermisoTest {

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

        @Bean
        TipoCambioSyncService tipoCambioSyncService() {
            return mock(TipoCambioSyncService.class);
        }

        @Bean
        TipoCambioRestController tipoCambioRestController(TipoCambioSyncService syncService) {
            return new TipoCambioRestController(mock(TipoCambioService.class), syncService);
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TipoCambioSyncService syncService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(syncService);
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("usuario sin rol ADMIN recibe 403 y no se sincroniza")
    void sinAdmin_403() throws Exception {
        mockMvc.perform(post("/api/tipos-cambio/sincronizar").with(user("vendedor").roles("USER", "VENDEDOR")))
                .andExpect(status().isForbidden());

        verify(syncService, never()).sincronizar();
    }

    @Test
    @DisplayName("ADMIN ejecuta la sincronización y recibe el resultado por moneda")
    void admin_sincroniza() throws Exception {
        when(syncService.sincronizar()).thenReturn(List.of(new ResultadoSincronizacionTipoCambio(
                "USD", EstadoSincronizacionTipoCambio.CREADO, LocalDate.of(2026, 10, 11),
                new BigDecimal("449.86"), new BigDecimal("455.6"), "Tipo de cambio registrado")));

        mockMvc.perform(post("/api/tipos-cambio/sincronizar").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].moneda").value("USD"))
                .andExpect(jsonPath("$[0].estado").value("CREADO"));

        verify(syncService).sincronizar();
    }
}
