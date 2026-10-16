package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.facturacion.dto.mapper.FacturaMapper;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.model.ConfiguracionFacturacion;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFacturaR;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.repository.LineaFacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.service.impl.FacturaServiceImpl;
import api.astro.whats_orders_manager.modules.facturacion.service.impl.LineaFacturaServiceImpl;
import api.astro.whats_orders_manager.modules.inventario.service.MovimientoInventarioService;
import api.astro.whats_orders_manager.modules.notificacion.service.NotificacionService;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.repository.ProductoRepository;
import api.astro.whats_orders_manager.modules.seguridad.service.impl.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * T-024 (criterio 4) — FacturaService.save y LineaFacturaService.updateLineas usan el mismo cálculo
 * (TotalesFacturaService): para el mismo conjunto de líneas producen los mismos subtotales de línea y los mismos
 * factura.subtotal/igv/total. Incluye dos tarifas distintas y una factura en USD (montos en la moneda de la
 * factura, sin convertir, ADR-011).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("T-024 — las dos rutas que guardan líneas producen los mismos totales")
class TotalesFacturaRutasEquivalentesTest {

    private static final int ID_FACTURA = 77;
    private static final int GRAVADO_13 = 1;
    private static final int GRAVADO_4 = 2;
    private static final int EXENTO = 3;

    @Mock private FacturaRepository facturaRepository;
    @Mock private LineaFacturaRepository lineaFacturaRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private ConfiguracionFacturacionService configuracionFacturacionService;
    @Mock private NotificacionService notificacionService;
    @Mock private PagoService pagoService;
    @Mock private FacturaMapper facturaMapper;
    @Mock private MovimientoInventarioService movimientoInventarioService;
    @Mock private AsientoContableService asientoContableService;
    @Mock private TipoCambioService tipoCambioService;
    @Mock private UserDetailsServiceImpl userDetailsService;

    private FacturaServiceImpl facturaService;
    private LineaFacturaServiceImpl lineaFacturaService;

    @BeforeEach
    void setUp() {
        TotalesFacturaService totales = new TotalesFacturaService(new CalculadoraImpuestoService(null),
                productoRepository);

        lineaFacturaService = new LineaFacturaServiceImpl();
        ReflectionTestUtils.setField(lineaFacturaService, "lineaFacturaRepository", lineaFacturaRepository);
        ReflectionTestUtils.setField(lineaFacturaService, "facturaRepository", facturaRepository);
        ReflectionTestUtils.setField(lineaFacturaService, "userDetailsService", userDetailsService);
        ReflectionTestUtils.setField(lineaFacturaService, "asientoContableService", asientoContableService);
        ReflectionTestUtils.setField(lineaFacturaService, "productoRepository", productoRepository);
        ReflectionTestUtils.setField(lineaFacturaService, "totalesFacturaService", totales);

        facturaService = new FacturaServiceImpl();
        ReflectionTestUtils.setField(facturaService, "facturaRepository", facturaRepository);
        ReflectionTestUtils.setField(facturaService, "configuracionFacturacionService", configuracionFacturacionService);
        ReflectionTestUtils.setField(facturaService, "notificacionService", notificacionService);
        ReflectionTestUtils.setField(facturaService, "pagoService", pagoService);
        ReflectionTestUtils.setField(facturaService, "lineaFacturaService", lineaFacturaService);
        ReflectionTestUtils.setField(facturaService, "facturaMapper", facturaMapper);
        ReflectionTestUtils.setField(facturaService, "movimientoInventarioService", movimientoInventarioService);
        ReflectionTestUtils.setField(facturaService, "asientoContableService", asientoContableService);
        ReflectionTestUtils.setField(facturaService, "tipoCambioService", tipoCambioService);
        ReflectionTestUtils.setField(facturaService, "totalesFacturaService", totales);

        when(configuracionFacturacionService.getOrCreateConfiguracion()).thenReturn(new ConfiguracionFacturacion());
        when(facturaRepository.save(any(Factura.class))).thenAnswer(inv -> {
            Factura f = inv.getArgument(0);
            if (f.getIdFactura() == null) {
                f.setIdFactura(500); // INSERT simulado
            }
            return f;
        });
        when(userDetailsService.getCurrentUserID()).thenReturn(Optional.of(1));
        when(productoRepository.findById(GRAVADO_13)).thenReturn(Optional.of(producto(GRAVADO_13, true, "13")));
        when(productoRepository.findById(GRAVADO_4)).thenReturn(Optional.of(producto(GRAVADO_4, true, "4")));
        when(productoRepository.findById(EXENTO)).thenReturn(Optional.of(producto(EXENTO, false, null)));
    }

    /** Línea de prueba: producto, cantidad, precio unitario con IVA incluido. */
    record Linea(int idProducto, int cantidad, String precio) {
    }

    static Stream<Arguments> casos() {
        return Stream.of(
                // 1130 → 1000 + 130 (13 %); 3 × 104 = 312 → 300 + 12 (4 %); 2 × 500 exento
                Arguments.of("CRC, dos tarifas y exento", MonedaFE.CRC, BigDecimal.ONE, List.of(
                        new Linea(GRAVADO_13, 1, "1130.00"),
                        new Linea(GRAVADO_4, 3, "104.00"),
                        new Linea(EXENTO, 2, "500.00")),
                        "2300.00", "142.00", "2442.00"),
                // USD al TC 520: los montos quedan en USD. 2 × 11.30 = 22.60 → 20.00 + 2.60;
                // 10.40 → 10.00 + 0.40; 5.00 exento
                Arguments.of("USD, dos tarifas y exento", MonedaFE.USD, new BigDecimal("520.00"), List.of(
                        new Linea(GRAVADO_13, 2, "11.30"),
                        new Linea(GRAVADO_4, 1, "10.40"),
                        new Linea(EXENTO, 1, "5.00")),
                        "35.00", "3.00", "38.00"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("casos")
    @DisplayName("FacturaService.save y updateLineas → mismos subtotales de línea y mismos subtotal/igv/total")
    void mismasLineas_mismosTotales(String caso, MonedaFE moneda, BigDecimal tipoCambio, List<Linea> lineas,
                                    String subtotal, String igv, String total) {
        // Ruta 1: la factura llega con sus líneas (POST /facturas/guardar). El subtotal recibido se ignora.
        Factura porFacturaService = factura(null, moneda, tipoCambio);
        for (Linea l : lineas) {
            LineaFactura entidad = new LineaFactura();
            entidad.setFactura(porFacturaService);
            Producto soloId = new Producto();
            soloId.setIdProducto(l.idProducto());
            entidad.setProducto(soloId);
            entidad.setCantidad(l.cantidad());
            entidad.setPrecioUnitario(new BigDecimal(l.precio()));
            entidad.setSubtotal(BigDecimal.ONE);
            porFacturaService.getLineas().add(entidad);
        }
        facturaService.save(porFacturaService);

        // Ruta 2: la factura existe sin líneas y se guardan con PUT /lineas-factura/actualizar
        Factura porLineas = factura(ID_FACTURA, moneda, tipoCambio);
        when(facturaRepository.findByIdFactura(ID_FACTURA)).thenReturn(Optional.of(porLineas));
        int idTemporal = -1;
        List<LineaFacturaR> recibidas = new ArrayList<>();
        for (Linea l : lineas) {
            recibidas.add(new LineaFacturaR(idTemporal--, recibidas.size() + 1, l.idProducto(), ID_FACTURA, null,
                    l.cantidad(), new BigDecimal(l.precio()), BigDecimal.ONE, null, null, null, null));
        }
        lineaFacturaService.updateLineas(recibidas);

        assertThat(porFacturaService.getSubtotal()).isEqualByComparingTo(subtotal)
                .isEqualByComparingTo(porLineas.getSubtotal());
        assertThat(porFacturaService.getIgv()).isEqualByComparingTo(igv)
                .isEqualByComparingTo(porLineas.getIgv());
        assertThat(porFacturaService.getTotal()).isEqualByComparingTo(total)
                .isEqualByComparingTo(porLineas.getTotal());
        assertThat(subtotalesDeLinea(porFacturaService)).isEqualTo(subtotalesDeLinea(porLineas));
        assertThat(porLineas.getMonedaFE()).isEqualTo(moneda);
    }

    private static List<BigDecimal> subtotalesDeLinea(Factura factura) {
        return factura.getLineas().stream().map(LineaFactura::getSubtotal).toList();
    }

    private static Factura factura(Integer id, MonedaFE moneda, BigDecimal tipoCambio) {
        Cliente cliente = new Cliente();
        cliente.setIdCliente(1);
        cliente.setNombre("Cliente Prueba");
        Factura f = new Factura();
        f.setIdFactura(id);
        f.setCliente(cliente);
        f.setNumeroFactura("F-" + (id != null ? id : "NUEVA"));
        f.setSerie("F");
        f.setMonedaFE(moneda);
        f.setTipoCambio(tipoCambio);
        f.setSubtotal(BigDecimal.ZERO);
        f.setIgv(BigDecimal.ZERO);
        f.setTotal(BigDecimal.ZERO);
        f.setAnulada(false);
        return f;
    }

    @SuppressWarnings("deprecation")
    private static Producto producto(int id, boolean gravado, String porcentaje) {
        Producto p = new Producto();
        p.setIdProducto(id);
        p.setDescripcion("Producto " + id);
        p.setGravado(gravado);
        p.setPorcentajeImpuesto(porcentaje != null ? new BigDecimal(porcentaje) : null);
        p.setPrecioIncluyeIva(true);
        p.setStock(null);
        return p;
    }
}
