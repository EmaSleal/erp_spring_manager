package api.astro.whats_orders_manager.modules.facturacion.correo;

import api.astro.whats_orders_manager.modules.facturacion.correo.controller.EntregaComprobanteRestController;
import api.astro.whats_orders_manager.modules.facturacion.correo.dto.EntregaDTO;
import api.astro.whats_orders_manager.modules.facturacion.correo.service.EntregaComprobanteService;
import api.astro.whats_orders_manager.modules.facturacion.correo.service.EntregaCorreoException;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-055: reenvío manual y consulta de entregas. El permiso nuevo COMPROBANTE_REENVIAR_CORREO protege el POST (403 sin
 * él, 201 con él); FACTURA_VER protege el historial. 400 y 409 vienen del servicio con el cuerpo {success, message}.
 */
@SpringJUnitWebConfig(EntregaComprobanteRestControllerTest.Config.class)
@DisplayName("T-055 — EntregaComprobanteRestController: permisos y errores")
class EntregaComprobanteRestControllerTest {

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class Config {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated()).csrf(csrf -> csrf.disable());
            return http.build();
        }

        @Bean(name = "permisoService")
        PermisoService permisoService() {
            return mock(PermisoService.class);
        }

        @Bean
        EntregaComprobanteService entregaService() {
            return mock(EntregaComprobanteService.class);
        }

        @Bean
        EntregaComprobanteRestController controller(EntregaComprobanteService servicio) {
            return new EntregaComprobanteRestController(servicio);
        }
    }

    @Autowired private WebApplicationContext context;
    @Autowired private PermisoService permisoService;
    @Autowired private EntregaComprobanteService servicio;
    private MockMvc mockMvc;

    private static final String RUTA = "/api/facturas/electronica/comprobantes/7/entregas";

    @BeforeEach
    void setUp() {
        reset(permisoService, servicio);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static EntregaDTO entrega() {
        return new EntregaDTO(5L, "MANUAL", "otro@example.test", "PENDIENTE", 0, LocalDateTime.of(2026, 11, 3, 10, 0),
                null, null);
    }

    @Test
    @DisplayName("POST sin COMPROBANTE_REENVIAR_CORREO → 403 y no se crea la entrega")
    void sinPermiso403() throws Exception {
        when(permisoService.tienePermisoPorCodigo("vendedor", "FACTURA_VER")).thenReturn(true);

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("{\"destinatario\":\"a@b.co\"}")
                        .with(user("vendedor").roles("ADMIN")))
                .andExpect(status().isForbidden());

        verify(permisoService).tienePermisoPorCodigo("vendedor", "COMPROBANTE_REENVIAR_CORREO");
        verify(servicio, never()).registrarManual(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("POST con el permiso → 201 con la entrega MANUAL PENDIENTE")
    void conPermiso201() throws Exception {
        when(permisoService.tienePermisoPorCodigo("admin", "COMPROBANTE_REENVIAR_CORREO")).thenReturn(true);
        when(servicio.registrarManual(7L, "otro@example.test", "admin")).thenReturn(entrega());

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinatario\":\"otro@example.test\"}").with(user("admin").roles("ADMIN")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.origen").value("MANUAL"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.destinatario").value("otro@example.test"));

        verify(servicio).registrarManual(7L, "otro@example.test", "admin");
    }

    @Test
    @DisplayName("correo inválido → 400 {success:false, message}; comprobante no ACEPTADO → 409; inexistente → 404")
    void erroresDelServicio() throws Exception {
        when(permisoService.tienePermisoPorCodigo("admin", "COMPROBANTE_REENVIAR_CORREO")).thenReturn(true);
        when(servicio.registrarManual(eq(7L), eq("no-es-correo"), anyString()))
                .thenThrow(new EntregaCorreoException(HttpStatus.BAD_REQUEST, "El destinatario no es un correo válido"));
        when(servicio.registrarManual(eq(8L), anyString(), anyString()))
                .thenThrow(new EntregaCorreoException(HttpStatus.CONFLICT, "Solo se reenvía un comprobante ACEPTADO"));
        when(servicio.registrarManual(eq(9L), anyString(), anyString()))
                .thenThrow(new EntregaCorreoException(HttpStatus.NOT_FOUND, "Comprobante no encontrado"));

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("{\"destinatario\":\"no-es-correo\"}")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("El destinatario no es un correo válido"));
        mockMvc.perform(post("/api/facturas/electronica/comprobantes/8/entregas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinatario\":\"a@b.co\"}").with(user("admin").roles("ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Solo se reenvía un comprobante ACEPTADO"));
        mockMvc.perform(post("/api/facturas/electronica/comprobantes/9/entregas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinatario\":\"a@b.co\"}").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST sin cuerpo → el servicio recibe destinatario nulo (usa el del receptor)")
    void sinCuerpoUsaElDelReceptor() throws Exception {
        when(permisoService.tienePermisoPorCodigo("admin", "COMPROBANTE_REENVIAR_CORREO")).thenReturn(true);
        when(servicio.registrarManual(eq(7L), any(), eq("admin"))).thenReturn(entrega());

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("{}").with(user("admin").roles("ADMIN")))
                .andExpect(status().isCreated());

        verify(servicio).registrarManual(7L, null, "admin");
    }

    @Test
    @DisplayName("GET historial: 403 sin FACTURA_VER; con FACTURA_VER lista las entregas")
    void historial() throws Exception {
        mockMvc.perform(get(RUTA).with(user("x").roles("ADMIN"))).andExpect(status().isForbidden());

        when(permisoService.tienePermisoPorCodigo("lector", "FACTURA_VER")).thenReturn(true);
        when(servicio.listar(7L)).thenReturn(List.of(entrega()));
        mockMvc.perform(get(RUTA).with(user("lector").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$[0].intentos").value(0));
    }
}
