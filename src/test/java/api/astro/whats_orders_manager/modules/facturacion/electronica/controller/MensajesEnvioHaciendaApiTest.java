package api.astro.whats_orders_manager.modules.facturacion.electronica.controller;

import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ComprobanteElectronicoDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ConfiguracionHaciendaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.EmisionNoPermitidaException;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ComprobanteElectronicoService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConfiguracionHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.EnvioHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl.ComprobanteElectronicoServiceImpl;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-037 (criterios 17 y 19): mensajes fijos por resultado al emitir, enviar y reenviar, sin el texto de ninguna
 * excepción (P-070), y renovar-token con el resultado real (P-088).
 */
@DisplayName("T-037 — mensajes fijos del envío y renovar-token")
class MensajesEnvioHaciendaApiTest {

    private final ComprobanteElectronicoService comprobantes = mock(ComprobanteElectronicoService.class);
    private final EnvioHaciendaService envio = mock(EnvioHaciendaService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
        new ComprobanteElectronicoController(comprobantes, envio, mock(FacturaRepository.class))).build();

    private static ComprobanteElectronicoDTO comprobante(EstadoComprobante estado, String ultimoError) {
        return ComprobanteElectronicoDTO.builder().id(5L).estado(estado).ultimoError(ultimoError).build();
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
        "ENVIADO, 200, true, Recibido por Hacienda",
        "NO_TRANSMITIDO, 200, false, No se pudo transmitir a Hacienda; se reintentará.",
        "ENVIO_INCIERTO, 200, false, Envío incierto; se verificará con Hacienda.",
        "ERROR, 422, false, No se pudo preparar el comprobante"
    })
    @DisplayName("emitir, enviar y reenviar: mensaje y código según el estado resultante")
    void mensajePorResultado(EstadoComprobante estado, int http, boolean exito, String mensaje) throws Exception {
        ComprobanteElectronicoDTO dto = comprobante(estado, estado == EstadoComprobante.ERROR ? "Error en firma digital: PIN" : null);
        when(envio.emitirFactura(7L)).thenReturn(dto);
        when(envio.enviar(5L)).thenReturn(dto);
        when(envio.reenviar(5L)).thenReturn(dto);
        for (String url : new String[] {"/api/facturas/electronica/comprobantes/procesar/7",
                "/api/facturas/electronica/comprobantes/5/enviar", "/api/facturas/electronica/comprobantes/5/reenviar"}) {
            mvc.perform(post(url))
                .andExpect(status().is(http))
                .andExpect(jsonPath("$.success").value(exito))
                .andExpect(jsonPath("$.message").value(containsString(mensaje)))
                .andExpect(jsonPath("$.message").value(not(containsString("PIN"))));
        }
    }

    @Test
    @DisplayName("XML inválido según el XSD: 422 con el mensaje de T-036")
    void xmlInvalido() throws Exception {
        when(envio.enviar(5L)).thenReturn(comprobante(EstadoComprobante.ERROR,
            ComprobanteElectronicoServiceImpl.ENCABEZADO_XML_INVALIDO_XSD + "\nlínea 1, columna 2: x"));
        mvc.perform(post("/api/facturas/electronica/comprobantes/5/enviar"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.message").value(ComprobanteElectronicoServiceImpl.MENSAJE_XML_INVALIDO_XSD));
    }

    @Test
    @DisplayName("excepción inesperada al emitir, generar, firmar, enviar o reenviar: 500 con mensaje fijo, sin su texto")
    void excepcionInesperada_mensajeFijo() throws Exception {
        RuntimeException secreta = new RuntimeException("detalle interno jdbc:mysql://10.0.0.5 secreto");
        when(envio.emitirFactura(7L)).thenThrow(secreta);
        when(comprobantes.generarDesdeFactura(7L)).thenThrow(secreta);
        when(comprobantes.firmar(5L)).thenThrow(secreta);
        when(envio.enviar(5L)).thenThrow(secreta);
        when(envio.reenviar(5L)).thenThrow(secreta);
        for (var pedido : java.util.List.of(post("/api/facturas/electronica/comprobantes/procesar/7"),
                post("/api/facturas/electronica/comprobantes/generar").param("facturaId", "7"),
                post("/api/facturas/electronica/comprobantes/5/firmar"),
                post("/api/facturas/electronica/comprobantes/5/enviar"),
                post("/api/facturas/electronica/comprobantes/5/reenviar"))) {
            mvc.perform(pedido)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(MensajesEnvioHacienda.ERROR_INESPERADO));
        }
    }

    @Test
    @DisplayName("regla del negocio al emitir (cliente sin FE): 409 con el mensaje de la aplicación")
    void reglaDelNegocio_409() throws Exception {
        when(envio.emitirFactura(7L)).thenThrow(new EmisionNoPermitidaException("El cliente 'X' no requiere facturación electrónica."));
        mvc.perform(post("/api/facturas/electronica/comprobantes/procesar/7"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("El cliente 'X' no requiere facturación electrónica."));
    }

    @Test
    @DisplayName("reenviar un comprobante en ENVIO_INCIERTO (marca de envío): 409 «no puede ser reenviado»")
    void reenviarMarcado_409() throws Exception {
        when(envio.reenviar(5L)).thenThrow(
            new EnvioHaciendaService.ComprobanteNoEnviableException(EstadoComprobante.ENVIO_INCIERTO, true));
        mvc.perform(post("/api/facturas/electronica/comprobantes/5/reenviar"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Comprobante no puede ser reenviado en estado ENVIO_INCIERTO"));
    }

    @Nested
    @DisplayName("criterio 19 (P-088): renovar-token")
    class RenovarToken {

        private final ConfiguracionHaciendaService configuracion = mock(ConfiguracionHaciendaService.class);
        private final MockMvc mvcConfig = MockMvcBuilders.standaloneSetup(new ConfiguracionHaciendaController(
            configuracion, new api.astro.whats_orders_manager.modules.facturacion.electronica.mapper.ConfiguracionHaciendaMapper())).build();

        @Test
        @DisplayName("la autenticación falla → 502 con mensaje fijo")
        void fallo_502() throws Exception {
            when(configuracion.renovarToken(3L)).thenReturn(false);
            mvcConfig.perform(post("/api/facturas/configuracion-hacienda/3/renovar-token"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(ConfiguracionHaciendaController.MENSAJE_TOKEN_NO_RENOVADO));
        }

        @Test
        @DisplayName("token nuevo y vigente → 200 con la vigencia")
        void exito_200() throws Exception {
            when(configuracion.renovarToken(3L)).thenReturn(true);
            when(configuracion.obtenerPorId(3L)).thenReturn(Optional.of(ConfiguracionHaciendaDTO.builder().id(3L)
                .tokenValido(true).tokenExpira(LocalDateTime.of(2026, 11, 17, 13, 5)).build()));
            mvcConfig.perform(post("/api/facturas/configuracion-hacienda/3/renovar-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token renovado; vigente hasta 17/11/2026 13:05"));
        }

        @Test
        @DisplayName("renovado pero sin token vigente → 502, nunca un falso éxito")
        void sinTokenVigente_502() throws Exception {
            when(configuracion.renovarToken(3L)).thenReturn(true);
            when(configuracion.obtenerPorId(3L)).thenReturn(Optional.of(ConfiguracionHaciendaDTO.builder().id(3L)
                .tokenValido(false).build()));
            mvcConfig.perform(post("/api/facturas/configuracion-hacienda/3/renovar-token"))
                .andExpect(status().isBadGateway());
        }

        @Test
        @DisplayName("excepción del servicio → 502 con mensaje fijo, sin su texto")
        void excepcion_502() throws Exception {
            when(configuracion.renovarToken(3L)).thenThrow(new RuntimeException("Connection refused 10.0.0.5"));
            mvcConfig.perform(post("/api/facturas/configuracion-hacienda/3/renovar-token"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value(ConfiguracionHaciendaController.MENSAJE_TOKEN_NO_RENOVADO));
        }
    }
}
