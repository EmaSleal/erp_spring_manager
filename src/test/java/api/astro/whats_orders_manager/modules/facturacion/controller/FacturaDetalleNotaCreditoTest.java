package api.astro.whats_orders_manager.modules.facturacion.controller;

import api.astro.whats_orders_manager.modules.cliente.service.ClienteService;
import api.astro.whats_orders_manager.modules.facturacion.dto.FacturaDetalleDTO;
import api.astro.whats_orders_manager.modules.facturacion.dto.mapper.FacturaMapper;
import api.astro.whats_orders_manager.modules.facturacion.dto.mapper.PagoMapper;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.service.FacturaPdfService;
import api.astro.whats_orders_manager.modules.facturacion.service.FacturaService;
import api.astro.whats_orders_manager.modules.facturacion.service.FacturaSustitutaService;
import api.astro.whats_orders_manager.modules.facturacion.service.NotaCreditoElegibilidad;
import api.astro.whats_orders_manager.modules.facturacion.service.PagoService;
import api.astro.whats_orders_manager.modules.facturacion.service.PoliticaMutabilidadFactura;
import api.astro.whats_orders_manager.shared.service.EmailService;
import api.astro.whats_orders_manager.shared.service.MonedaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** T-042: GET /facturas/detalle/{id} lleva la bandera de la nota de crédito que decide el servidor. */
@DisplayName("T-042 — Detalle de la factura: bandera de la nota de crédito")
class FacturaDetalleNotaCreditoTest {

    private final FacturaService facturaService = mock(FacturaService.class);
    private final FacturaMapper facturaMapper = mock(FacturaMapper.class);
    private final NotaCreditoElegibilidad elegibilidad = mock(NotaCreditoElegibilidad.class);
    private MockMvc mvc;
    private final Factura factura = new Factura();

    @BeforeEach
    void setUp() {
        factura.setIdFactura(7);
        FacturaController controller = new FacturaController(facturaService, mock(ClienteService.class),
            mock(PagoService.class), mock(EmailService.class), mock(MonedaService.class), facturaMapper,
            mock(FacturaPdfService.class), mock(PagoMapper.class), mock(PoliticaMutabilidadFactura.class),
            mock(FacturaSustitutaService.class), elegibilidad,
            mock(api.astro.whats_orders_manager.modules.facturacion.electronica.service.AmbienteComprobanteGuard.class));
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
        when(facturaService.findById(7)).thenReturn(Optional.of(factura));
        when(facturaMapper.toDetalleDTO(factura)).thenReturn(FacturaDetalleDTO.builder().build());
    }

    private static UsernamePasswordAuthenticationToken usuario() {
        return new UsernamePasswordAuthenticationToken("gerente", "x");
    }

    @Test
    @DisplayName("elegible → puedeEmitirNotaCredito true y sin motivo")
    void elegible() throws Exception {
        when(elegibilidad.evaluar(factura, "gerente")).thenReturn(new NotaCreditoElegibilidad.Resultado(true, null));
        mvc.perform(get("/facturas/detalle/7").principal(usuario()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.puedeEmitirNotaCredito").value(true))
            .andExpect(jsonPath("$.motivoNotaCreditoNoDisponible").doesNotExist());
    }

    @Test
    @DisplayName("T-046: elegible → el detalle trae el mes de la factura original para el texto del código 13")
    void mesDeLaFacturaOriginal() throws Exception {
        when(elegibilidad.evaluar(factura, "gerente")).thenReturn(new NotaCreditoElegibilidad.Resultado(true, null));
        when(elegibilidad.periodoFacturaOriginal(factura)).thenReturn(java.util.Optional.of("2026-09"));
        mvc.perform(get("/facturas/detalle/7").principal(usuario()))
            .andExpect(jsonPath("$.periodoFacturaOriginal").value("2026-09"));
    }

    @Test
    @DisplayName("con pagos → deshabilitada con el motivo fijo")
    void conPagos() throws Exception {
        when(elegibilidad.evaluar(factura, "gerente")).thenReturn(
            new NotaCreditoElegibilidad.Resultado(false, NotaCreditoElegibilidad.MOTIVO_CON_PAGOS));
        mvc.perform(get("/facturas/detalle/7").principal(usuario()))
            .andExpect(jsonPath("$.puedeEmitirNotaCredito").value(false))
            .andExpect(jsonPath("$.motivoNotaCreditoNoDisponible")
                .value("La nota de crédito sobre facturas con pagos llega en una próxima versión"));
    }

    @Test
    @DisplayName("no elegible → false y sin motivo (no hay botón)")
    void noElegible() throws Exception {
        when(elegibilidad.evaluar(factura, "gerente")).thenReturn(new NotaCreditoElegibilidad.Resultado(false, null));
        mvc.perform(get("/facturas/detalle/7").principal(usuario()))
            .andExpect(jsonPath("$.puedeEmitirNotaCredito").value(false))
            .andExpect(jsonPath("$.motivoNotaCreditoNoDisponible").doesNotExist());
    }
}
