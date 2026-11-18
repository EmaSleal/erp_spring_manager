package api.astro.whats_orders_manager.modules.facturacion.electronica.controller;

import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.EmisionNoPermitidaException;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ComprobanteElectronicoService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.EnvioHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** T-039 (criterio 13): enviar-email responde 409 con mensaje fijo si no está aceptado y no filtra e.getMessage() en 500. */
@DisplayName("T-039 — POST /{id}/enviar-email")
class EnviarEmailComprobanteApiTest {

    private final ComprobanteElectronicoService service = mock(ComprobanteElectronicoService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ComprobanteElectronicoController(
            service, mock(EnvioHaciendaService.class), mock(FacturaRepository.class))).build();

    @Test
    @DisplayName("comprobante no aceptado → 409 con el mensaje fijo")
    void noAceptado_409() throws Exception {
        when(service.enviarPorEmail(5L, null)).thenThrow(new EmisionNoPermitidaException("Mensaje fijo de prueba"));

        mockMvc.perform(post("/api/facturas/electronica/comprobantes/5/enviar-email"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Mensaje fijo de prueba"));
    }

    @Test
    @DisplayName("error inesperado → 500 con mensaje genérico, sin el texto de la excepción")
    void inesperado_500_sinFiltrar() throws Exception {
        when(service.enviarPorEmail(5L, null))
                .thenThrow(new RuntimeException("smtp://usuario:clave@host fallo interno"));

        mockMvc.perform(post("/api/facturas/electronica/comprobantes/5/enviar-email"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(not(containsString("smtp"))))
                .andExpect(jsonPath("$.message").value(ComprobanteElectronicoController.MENSAJE_ERROR_EMAIL));
    }

    @Test
    @DisplayName("envío correcto → 200")
    void ok() throws Exception {
        when(service.enviarPorEmail(5L, null)).thenReturn(true);
        mockMvc.perform(post("/api/facturas/electronica/comprobantes/5/enviar-email"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
    }
}
