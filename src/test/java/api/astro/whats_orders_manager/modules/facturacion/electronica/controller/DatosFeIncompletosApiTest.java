package api.astro.whats_orders_manager.modules.facturacion.electronica.controller;

import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.DatosFeIncompletosException;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ComprobanteElectronicoService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.EnvioHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.validacion.ProblemaDatoFe;
import api.astro.whats_orders_manager.modules.facturacion.electronica.validacion.ReglaDatoFe;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** T-038 (criterio 8): datos FE obligatorios inválidos del receptor o del emisor → 409 con mensaje fijo. */
@DisplayName("T-038 — emisión con datos FE incompletos → 409")
class DatosFeIncompletosApiTest {

    private final ComprobanteElectronicoService service = mock(ComprobanteElectronicoService.class);
    private final EnvioHaciendaService envio = mock(EnvioHaciendaService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ComprobanteElectronicoController(service, envio, mock(FacturaRepository.class))).build();

    private static final List<ProblemaDatoFe> RECEPTOR = List.of(
        new ProblemaDatoFe("numeroIdentificacion", "x", ReglaDatoFe.IDENTIFICACION_NUMERO, true),
        new ProblemaDatoFe("nombre", "x", ReglaDatoFe.NOMBRE_LONGITUD, true));
    private static final List<ProblemaDatoFe> EMISOR = List.of(
        new ProblemaDatoFe("distrito", "x", ReglaDatoFe.UBICACION, true));

    @Test
    @DisplayName("POST /procesar/{id} → 409 con el mensaje del receptor")
    void procesar_receptor() throws Exception {
        when(envio.emitirFactura(7L)).thenThrow(DatosFeIncompletosException.delReceptor("Cliente X", RECEPTOR));
        mockMvc.perform(post("/api/facturas/electronica/comprobantes/procesar/7"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                    "El cliente Cliente X no tiene los datos de facturación electrónica completos: identificación, nombre"));
    }

    @Test
    @DisplayName("POST /generar y /{id}/reemitir → 409 con el mensaje del emisor que remite a Configuración → Empresa")
    void generarYReemitir_emisor() throws Exception {
        DatosFeIncompletosException e = DatosFeIncompletosException.delEmisor(EMISOR);
        when(service.generarDesdeFactura(7L)).thenThrow(e);
        when(service.reemitir(5L)).thenThrow(e);
        mockMvc.perform(post("/api/facturas/electronica/comprobantes/generar").param("facturaId", "7"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("Configuración → Empresa")))
                .andExpect(jsonPath("$.message").value(containsString("distrito")));
        mockMvc.perform(post("/api/facturas/electronica/comprobantes/5/reemitir"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("Configuración → Empresa")));
    }
}
