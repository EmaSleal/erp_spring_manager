package api.astro.whats_orders_manager.modules.facturacion.electronica.controller;

import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.ProductosSinCabysException;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ComprobanteElectronicoService;
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

/** T-033 (criterio 14): emitir con productos sin CAByS responde 409 y nombra los productos. */
@DisplayName("T-033 — emisión con productos sin CAByS → 409")
class EmisionSinCabysApiTest {

    private final ComprobanteElectronicoService service = mock(ComprobanteElectronicoService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ComprobanteElectronicoController(service, mock(FacturaRepository.class))).build();

    @Test
    @DisplayName("POST /procesar/{id} → 409 con los productos sin código")
    void procesar_409() throws Exception {
        when(service.procesarFactura(7L)).thenThrow(new ProductosSinCabysException(List.of("Servicio de instalación")));

        mockMvc.perform(post("/api/facturas/electronica/comprobantes/procesar/7"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("Servicio de instalación")));
    }

    @Test
    @DisplayName("POST /generar y /{id}/reemitir → 409")
    void generarYReemitir_409() throws Exception {
        ProductosSinCabysException sinCabys = new ProductosSinCabysException(List.of("Producto A", "Producto B"));
        when(service.generarDesdeFactura(7L)).thenThrow(sinCabys);
        when(service.reemitir(5L)).thenThrow(sinCabys);

        mockMvc.perform(post("/api/facturas/electronica/comprobantes/generar").param("facturaId", "7"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("Producto A, Producto B")));
        mockMvc.perform(post("/api/facturas/electronica/comprobantes/5/reemitir"))
                .andExpect(status().isConflict());
    }
}
