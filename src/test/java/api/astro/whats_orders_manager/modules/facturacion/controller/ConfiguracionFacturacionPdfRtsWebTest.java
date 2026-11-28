package api.astro.whats_orders_manager.modules.facturacion.controller;

import api.astro.whats_orders_manager.modules.configuracion.repository.ProvinciaCostaRicaRepository;
import api.astro.whats_orders_manager.modules.facturacion.model.ConfiguracionFacturacion;
import api.astro.whats_orders_manager.modules.facturacion.repository.ConfiguracionFacturacionRepository;
import api.astro.whats_orders_manager.modules.facturacion.service.impl.ConfiguracionFacturacionServiceImpl;
import api.astro.whats_orders_manager.modules.seguridad.security.ApiSecurityErrorHandler;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioActividadService;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-059 criterio 4: PUT /api/configuracion/facturacion con el servicio real y el repositorio simulado. Un título inválido
 * devuelve 400 con el mensaje fijo y no toca la base; la regla ADMIN de la clase sigue vigente (VENDEDOR 403).
 */
@WebMvcTest(ConfiguracionFacturacionRestController.class)
@Import({ConfiguracionFacturacionPdfRtsWebTest.SeguridadConfig.class, ApiSecurityErrorHandler.class,
        ConfiguracionFacturacionServiceImpl.class})
@DisplayName("T-059 — PUT /api/configuracion/facturacion: título y leyenda del RTS")
class ConfiguracionFacturacionPdfRtsWebTest {

    private static final String URL = "/api/configuracion/facturacion";

    @TestConfiguration
    @EnableMethodSecurity
    static class SeguridadConfig {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http, ApiSecurityErrorHandler api) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .exceptionHandling(e -> e.accessDeniedHandler(api).authenticationEntryPoint(api));
            return http.build();
        }
    }

    @MockitoBean private ConfiguracionFacturacionRepository repositorio;
    @MockitoBean private UsuarioActividadService usuarioActividadService;
    @MockitoBean private UsuarioService usuarioService;
    @MockitoBean private ProvinciaCostaRicaRepository provinciaRepository;

    @Autowired private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        ConfiguracionFacturacion existente = new ConfiguracionFacturacion();
        existente.setId(1);
        existente.setSerieFactura("FA01");
        existente.setNumeroInicial(1);
        existente.setNumeroActual(1);
        when(repositorio.findById(1)).thenReturn(Optional.of(existente));
        when(repositorio.save(any(ConfiguracionFacturacion.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static String cuerpo(String titulo, String leyenda) {
        return "{\"id\":1,\"serieFactura\":\"FA01\",\"numeroInicial\":1,\"numeroActual\":1,\"moneda\":\"CRC\","
                + "\"simboloMoneda\":\"₡\",\"decimales\":2,\"formatoNumero\":\"{serie}-{numero}\",\"activo\":true,"
                + "\"pdfRtsTitulo\":" + json(titulo) + ",\"pdfRtsLeyenda\":" + json(leyenda) + "}";
    }

    private static String json(String s) {
        return s == null ? "null" : "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    @Test
    @DisplayName("título válido y leyenda: 200 y se guardan recortados")
    void valido() throws Exception {
        mockMvc.perform(put(URL).with(user("admin").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("  Factura de venta RTS  ", "  No cobra IVA.  ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pdfRtsTitulo").value("Factura de venta RTS"))
                .andExpect(jsonPath("$.data.pdfRtsLeyenda").value("No cobra IVA."));
        verify(repositorio).save(any(ConfiguracionFacturacion.class));
    }

    @Test
    @DisplayName("leyenda vacía: 200 y se guarda NULL")
    void leyendaVacia() throws Exception {
        mockMvc.perform(put(URL).with(user("admin").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Factura RTS", "   ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pdfRtsLeyenda").doesNotExist());
    }

    @Test
    @DisplayName("título sin «Factura»: 400 con el mensaje fijo y sin guardar")
    void sinFactura() throws Exception {
        mockMvc.perform(put(URL).with(user("admin").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Comprobante de venta", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("El título del comprobante del régimen simplificado debe contener la palabra «Factura»"));
        verify(repositorio, never()).save(any(ConfiguracionFacturacion.class));
    }

    @Test
    @DisplayName("título vacío: 400 obligatorio")
    void vacio() throws Exception {
        mockMvc.perform(put(URL).with(user("admin").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("  ", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El título del comprobante del régimen simplificado es obligatorio"));
    }

    @Test
    @DisplayName("título de 121 caracteres: 400; leyenda de 301: 400")
    void largos() throws Exception {
        mockMvc.perform(put(URL).with(user("admin").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Factura " + "x".repeat(113), null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("El título del comprobante del régimen simplificado no puede exceder 120 caracteres"));
        mockMvc.perform(put(URL).with(user("admin").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Factura RTS", "a".repeat(301))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("La leyenda del comprobante del régimen simplificado no puede exceder 300 caracteres"));
        verify(repositorio, never()).save(any(ConfiguracionFacturacion.class));
    }

    @Test
    @DisplayName("la regla ADMIN sigue: VENDEDOR recibe 403 y no se guarda nada")
    void soloAdmin() throws Exception {
        mockMvc.perform(put(URL).with(user("vendedor").roles("VENDEDOR")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Factura RTS", null)))
                .andExpect(status().isForbidden());
        verify(repositorio, never()).save(any(ConfiguracionFacturacion.class));
    }
}
