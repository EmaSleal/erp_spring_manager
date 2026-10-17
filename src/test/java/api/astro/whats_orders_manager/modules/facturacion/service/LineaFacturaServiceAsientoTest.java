package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFacturaR;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.service.impl.LineaFacturaServiceImpl;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.repository.ProductoRepository;
import api.astro.whats_orders_manager.modules.seguridad.service.impl.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * LineaFacturaServiceImpl.updateLineas: el cuerpo es el conjunto completo de líneas de la factura (T-024) y al
 * guardarlas se contabiliza la venta (ADR-008). El flujo de creación guarda la factura sin líneas (total cero,
 * sin asiento) y luego sus líneas; esa primera carga de líneas genera el asiento de venta.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LineaFacturaServiceImpl.updateLineas — sincronización de líneas y contabilización de la venta")
class LineaFacturaServiceAsientoTest {

    private static final int ID_FACTURA = 40;
    private static final int ID_PRODUCTO = 5;

    @Mock private FacturaRepository facturaRepository;
    @Mock private UserDetailsServiceImpl userDetailsService;
    @Mock private AsientoContableService asientoContableService;
    @Mock private ProductoRepository productoRepository;

    private LineaFacturaServiceImpl service;
    private Factura factura;

    @BeforeEach
    void setUp() {
        service = new LineaFacturaServiceImpl();
        ReflectionTestUtils.setField(service, "facturaRepository", facturaRepository);
        ReflectionTestUtils.setField(service, "userDetailsService", userDetailsService);
        ReflectionTestUtils.setField(service, "asientoContableService", asientoContableService);
        ReflectionTestUtils.setField(service, "productoRepository", productoRepository);
        ReflectionTestUtils.setField(service, "totalesFacturaService",
                new TotalesFacturaService(new CalculadoraImpuestoService(null), productoRepository));

        factura = new Factura();
        factura.setIdFactura(ID_FACTURA);
        factura.setAnulada(false);
        Producto producto = new Producto();
        producto.setIdProducto(ID_PRODUCTO);
        when(productoRepository.findById(ID_PRODUCTO)).thenReturn(Optional.of(producto));
        when(userDetailsService.getCurrentUserID()).thenReturn(Optional.of(1));
        when(facturaRepository.findByIdFactura(ID_FACTURA)).thenReturn(Optional.of(factura));
        when(facturaRepository.save(any(Factura.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("factura sin líneas previas (recién creada) → contabiliza como primera carga")
    void primeraCargaDeLineas() {
        service.updateLineas(List.of(linea(-1, 1, "3000.00"), linea(-2, 1, "600.00")));

        assertThat(factura.getLineas()).hasSize(2);
        assertThat(factura.getTotal()).isEqualByComparingTo("3600.00");
        verify(asientoContableService).contabilizarVentaTrasGuardarLineas(factura, true);
    }

    @Test
    @DisplayName("factura que ya tenía líneas → solo sincroniza (no es primera carga)")
    void edicionDeLineasExistentes() {
        existente(10, "3000.00");

        service.updateLineas(List.of(linea(10, 1, "3000.00")));

        verify(asientoContableService).contabilizarVentaTrasGuardarLineas(factura, false);
    }

    @Test
    @DisplayName("la línea que no viene en el cuerpo se elimina y el total sale de las líneas resultantes")
    void lineaNoRecibida_seElimina() {
        LineaFactura conservada = existente(10, "3000.00");
        existente(11, "600.00");

        var totales = service.updateLineas(List.of(linea(10, 2, "3000.00")));

        assertThat(factura.getLineas()).containsExactly(conservada);
        assertThat(conservada.getSubtotal()).isEqualByComparingTo("6000.00");
        assertThat(factura.getTotal()).isEqualByComparingTo("6000.00");
        assertThat(totales.total()).isEqualByComparingTo("6000.00");
        assertThat(totales.lineas()).isEqualTo(1);
    }

    @Test
    @DisplayName("id de una línea que no pertenece a la factura → IllegalArgumentException sin cambios")
    void lineaAjena_rechazada() {
        LineaFactura propia = existente(10, "3000.00");

        assertThatThrownBy(() -> service.updateLineas(List.of(linea(99, 1, "100.00"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no pertenece");
        assertThat(factura.getLineas()).containsExactly(propia);
        verify(facturaRepository, never()).save(any());
        verifyNoInteractions(asientoContableService);
    }

    @Test
    @DisplayName("cantidad 0 o precio negativo → IllegalArgumentException sin cambios")
    void montosInvalidos_rechazados() {
        assertThatThrownBy(() -> service.updateLineas(List.of(linea(-1, 0, "100.00"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.updateLineas(List.of(linea(-1, 1, "-0.01"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(factura.getLineas()).isEmpty();
        verify(facturaRepository, never()).save(any());
    }

    @Test
    @DisplayName("cuerpo vacío o con líneas de varias facturas → IllegalArgumentException")
    void cuerpoInvalido_rechazado() {
        assertThatThrownBy(() -> service.updateLineas(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        LineaFacturaR otraFactura = new LineaFacturaR(-2, 2, ID_PRODUCTO, 41, null, 1,
                BigDecimal.ONE, BigDecimal.ONE, null, null, null, null);
        assertThatThrownBy(() -> service.updateLineas(List.of(linea(-1, 1, "1.00"), otraFactura)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(facturaRepository, never()).save(any());
    }

    @Test
    @DisplayName("factura anulada → se mantiene el rechazo (IllegalStateException)")
    void facturaAnulada_rechazada() {
        factura.setAnulada(true);

        assertThatThrownBy(() -> service.updateLineas(List.of(linea(-1, 1, "1.00"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("anulada");
        verify(facturaRepository, never()).save(any());
    }

    private LineaFactura existente(int idLinea, String precio) {
        LineaFactura linea = new LineaFactura();
        linea.setIdLineaFactura(idLinea);
        linea.setFactura(factura);
        linea.setCantidad(1);
        linea.setPrecioUnitario(new BigDecimal(precio));
        linea.setSubtotal(new BigDecimal(precio));
        factura.getLineas().add(linea);
        return linea;
    }

    private static LineaFacturaR linea(Integer idLinea, int cantidad, String precio) {
        BigDecimal monto = new BigDecimal(precio);
        return new LineaFacturaR(idLinea, 1, ID_PRODUCTO, ID_FACTURA, null, cantidad, monto, monto,
                null, null, null, null);
    }
}
