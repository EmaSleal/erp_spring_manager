package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFacturaR;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.repository.LineaFacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.service.impl.LineaFacturaServiceImpl;
import api.astro.whats_orders_manager.modules.seguridad.service.impl.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ADR-008: al guardar las líneas de una factura se contabiliza su venta.
 * El flujo de creación guarda la factura sin líneas (total cero, sin asiento) y luego
 * sus líneas; esa primera carga de líneas debe generar el asiento de venta.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LineaFacturaServiceImpl.updateLineas — contabilización de la venta")
class LineaFacturaServiceAsientoTest {

    @Mock
    private LineaFacturaRepository lineaFacturaRepository;

    @Mock
    private FacturaRepository facturaRepository;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private AsientoContableService asientoContableService;

    @InjectMocks
    private LineaFacturaServiceImpl service;

    private Factura factura;

    @BeforeEach
    void setUp() {
        factura = new Factura();
        factura.setIdFactura(40);
        factura.setAnulada(false);
        when(userDetailsService.getCurrentUserID()).thenReturn(Optional.of(1));
        when(facturaRepository.findByIdFactura(40)).thenReturn(Optional.of(factura));
    }

    @Test
    @DisplayName("factura sin líneas previas (recién creada) → contabiliza como primera carga")
    void primeraCargaDeLineas() {
        when(lineaFacturaRepository.existsByFacturaId(40)).thenReturn(0L);

        service.updateLineas(List.of(linea(null, 1, "3000.00"), linea(null, 2, "600.00")));

        assertThat(factura.getTotal()).isEqualByComparingTo("3600.00");
        verify(asientoContableService).contabilizarVentaTrasGuardarLineas(factura, true);
    }

    @Test
    @DisplayName("factura que ya tenía líneas → solo sincroniza (no es primera carga)")
    void edicionDeLineasExistentes() {
        when(lineaFacturaRepository.existsByFacturaId(40)).thenReturn(1L);

        service.updateLineas(List.of(linea(10, 1, "3000.00")));

        verify(asientoContableService).contabilizarVentaTrasGuardarLineas(factura, false);
    }

    private static LineaFacturaR linea(Integer idLinea, int numero, String subtotal) {
        BigDecimal monto = new BigDecimal(subtotal);
        return new LineaFacturaR(idLinea, numero, 5, 40, null, 1, monto, monto, null, null, null, null);
    }
}
