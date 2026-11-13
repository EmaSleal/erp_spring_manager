package api.astro.whats_orders_manager.modules.facturacion.controller;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ComprobanteElectronicoService;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.enums.TipoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.exception.NotaFacturaConflictoException;
import api.astro.whats_orders_manager.modules.facturacion.exception.NotaFacturaPedidoInvalidoException;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.NotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.service.NotaFacturaService;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-032 (criterio 6): POST /api/facturas/notas-credito exige FACTURA_ANULAR (el permiso de anular una factura) y
 * responde 201, 400, 403, 404 o 409 sin exponer mensajes de excepciones inesperadas.
 */
@SpringJUnitWebConfig(NotaCreditoRestControllerTest.Config.class)
@DisplayName("T-032 — API de notas de crédito: permisos y respuestas")
class NotaCreditoRestControllerTest {

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
        NotaFacturaService notaFacturaService() {
            return mock(NotaFacturaService.class);
        }

        @Bean
        ComprobanteElectronicoService comprobanteElectronicoService() {
            return mock(ComprobanteElectronicoService.class);
        }

        @Bean
        NotaCreditoRestController notaCreditoRestController(NotaFacturaService n, ComprobanteElectronicoService c) {
            return new NotaCreditoRestController(n, c);
        }
    }

    private static final String CUERPO =
            "{\"idFactura\":7,\"codigoReferencia\":\"01\",\"razon\":\"Anulación total\"}";

    @Autowired private WebApplicationContext context;
    @Autowired private PermisoService permisoService;
    @Autowired private NotaFacturaService notaService;
    @Autowired private ComprobanteElectronicoService comprobanteService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(permisoService, notaService, comprobanteService);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        when(permisoService.tienePermisoPorCodigo("gerente", "FACTURA_ANULAR")).thenReturn(true);
        when(permisoService.tienePermisoPorCodigo("gerente", "FACTURA_VER")).thenReturn(true);
    }

    @Test
    @DisplayName("sin FACTURA_ANULAR → 403 y no se emite nada")
    void sinPermiso_403() throws Exception {
        when(permisoService.tienePermisoPorCodigo(eq("vendedor"), anyString())).thenReturn(false);
        when(permisoService.tienePermisoPorCodigo("vendedor", "FACTURA_VER")).thenReturn(true);

        mockMvc.perform(post("/api/facturas/notas-credito").contentType(MediaType.APPLICATION_JSON).content(CUERPO)
                        .with(user("vendedor").roles("ADMIN")))
                .andExpect(status().isForbidden());

        verify(permisoService).tienePermisoPorCodigo("vendedor", "FACTURA_ANULAR");
        verifyNoInteractions(notaService);
    }

    @Test
    @DisplayName("con FACTURA_ANULAR → 201 con id, estado y clave; el comprobante firmado se envía")
    void emite_201() throws Exception {
        NotaFactura nota = nota();
        ComprobanteElectronico firmado = ComprobanteElectronico.builder().id(30L).claveNumerica("506CLAVE")
                .estado(EstadoComprobante.FIRMADO).build();
        ComprobanteElectronico enviado = ComprobanteElectronico.builder().id(30L).claveNumerica("506CLAVE")
                .estado(EstadoComprobante.ENVIADO).build();
        when(notaService.emitirNotaCreditoTotal(7, null, "01", "Anulación total", "gerente")).thenReturn(nota);
        when(notaService.obtener(5L)).thenReturn(Optional.of(new NotaFacturaService.NotaConComprobante(nota, firmado)),
                Optional.of(new NotaFacturaService.NotaConComprobante(nota, enviado)));

        mockMvc.perform(post("/api/facturas/notas-credito").contentType(MediaType.APPLICATION_JSON).content(CUERPO)
                        .with(user("gerente").roles("USER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.estado").value("EN_TRAMITE"))
                .andExpect(jsonPath("$.data.claveComprobante").value("506CLAVE"))
                .andExpect(jsonPath("$.data.estadoComprobante").value("ENVIADO"));

        verify(comprobanteService).procesarFacturaAsync(30L);
    }

    @Test
    @DisplayName("pedido inválido → 400 con el mensaje de validación")
    void invalido_400() throws Exception {
        when(notaService.emitirNotaCreditoTotal(anyInt(), any(), anyString(), anyString(), anyString()))
                .thenThrow(new NotaFacturaPedidoInvalidoException("estará disponible en una fase posterior"));

        mockMvc.perform(post("/api/facturas/notas-credito").contentType(MediaType.APPLICATION_JSON).content(CUERPO)
                        .with(user("gerente").roles("USER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("fase posterior")));
    }

    @Test
    @DisplayName("tipo DEBITO o JSON mal formado → 400")
    void formato_400() throws Exception {
        mockMvc.perform(post("/api/facturas/notas-credito").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idFactura\":7,\"tipo\":\"OTRO\"}").with(user("gerente").roles("USER")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/facturas/notas-credito").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigoReferencia\":\"01\"}").with(user("gerente").roles("USER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("factura inexistente → 404")
    void facturaInexistente_404() throws Exception {
        when(notaService.emitirNotaCreditoTotal(anyInt(), any(), anyString(), anyString(), anyString()))
                .thenThrow(new NoSuchElementException("detalle interno"));

        mockMvc.perform(post("/api/facturas/notas-credito").contentType(MediaType.APPLICATION_JSON).content(CUERPO)
                        .with(user("gerente").roles("USER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("La factura no existe"));
    }

    @Test
    @DisplayName("la factura no admite la nota → 409 con el mensaje del caso")
    void conflicto_409() throws Exception {
        when(notaService.emitirNotaCreditoTotal(anyInt(), any(), anyString(), anyString(), anyString()))
                .thenThrow(new NotaFacturaConflictoException("La factura F-7 ya está anulada"));

        mockMvc.perform(post("/api/facturas/notas-credito").contentType(MediaType.APPLICATION_JSON).content(CUERPO)
                        .with(user("gerente").roles("USER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("La factura F-7 ya está anulada"));
    }

    @Test
    @DisplayName("error inesperado → 500 genérico, sin el mensaje de la excepción")
    void errorInesperado_sinDetalle() throws Exception {
        when(notaService.emitirNotaCreditoTotal(anyInt(), any(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("ruta /secreta/cert.p12 sin PIN"));

        mockMvc.perform(post("/api/facturas/notas-credito").contentType(MediaType.APPLICATION_JSON).content(CUERPO)
                        .with(user("gerente").roles("USER")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(not(containsString("secreta"))));
    }

    @Test
    @DisplayName("GET /{id}: la nota con el estado de su comprobante vigente; 404 si no existe")
    void obtener() throws Exception {
        ComprobanteElectronico aceptado = ComprobanteElectronico.builder().id(30L).claveNumerica("506CLAVE")
                .estado(EstadoComprobante.ACEPTADO).build();
        when(notaService.obtener(5L)).thenReturn(Optional.of(new NotaFacturaService.NotaConComprobante(nota(), aceptado)));
        when(notaService.obtener(6L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/facturas/notas-credito/5").with(user("gerente").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estadoComprobante").value("ACEPTADO"))
                .andExpect(jsonPath("$.data.idFactura").value(7));
        mockMvc.perform(get("/api/facturas/notas-credito/6").with(user("gerente").roles("USER")))
                .andExpect(status().isNotFound());
        verify(comprobanteService, never()).procesarFacturaAsync(anyLong());
    }

    private static NotaFactura nota() {
        Factura factura = new Factura();
        factura.setIdFactura(7);
        return NotaFactura.builder().id(5L).factura(factura).tipo(TipoNotaFactura.CREDITO).codigoReferencia("01")
                .razon("Anulación total").estado(EstadoNotaFactura.EN_TRAMITE)
                .fechaEmision(LocalDateTime.of(2026, 11, 12, 10, 0)).moneda("CRC").total(new BigDecimal("1630.00"))
                .claveReferencia("506ORIGINAL").build();
    }
}
