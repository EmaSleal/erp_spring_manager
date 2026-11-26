package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.configuracion.service.ParametroSistemaService;
import api.astro.whats_orders_manager.modules.facturacion.dto.DesgloseImpuesto;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.politicas.PoliticaImpuestoVenta;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.repository.ProductoRepository;
import api.astro.whats_orders_manager.modules.regimen.enums.Regimen;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T-057 (ADR-073 §1, ADR-009, ADR-076 §3; criterios 4 y 10): el IVA de venta lo decide PoliticaImpuestoVenta según el
 * régimen sellado en la factura y CalculadoraImpuestoService (el único cálculo) lo aplica. SIMPLIFICADO → impuesto cero
 * en cada línea y total = suma de los importes al precio del cliente; GENERAL no cambia.
 */
@DisplayName("T-057 — IVA de venta según el régimen de la factura (política + calculadora)")
@SuppressWarnings("deprecation")
class VentaRegimenCalculoTest {

    private static final BigDecimal TRECE = new BigDecimal("13");

    private CalculadoraImpuestoService calculadora;
    private TotalesFacturaService totales;

    @BeforeEach
    void setUp() {
        ParametroSistemaService parametros = mock(ParametroSistemaService.class);
        when(parametros.obtenerValorDecimal(eq(CalculadoraImpuestoService.CLAVE_TARIFA_GENERAL), any())).thenReturn(TRECE);
        calculadora = new CalculadoraImpuestoService(parametros, new PoliticaImpuestoVenta());
        totales = new TotalesFacturaService(calculadora, mock(ProductoRepository.class));
    }

    private static Factura factura(Regimen regimen) {
        Factura f = new Factura();
        f.setRegimenEmisor(regimen);
        return f;
    }

    private static Producto producto(boolean gravado, boolean incluyeIva) {
        Producto p = new Producto();
        p.setGravado(gravado);
        p.setPorcentajeImpuesto(gravado ? TRECE : BigDecimal.ZERO);
        p.setPrecioIncluyeIva(incluyeIva);
        return p;
    }

    private static LineaFactura linea(Factura f, Producto p, int cantidad, String precio) {
        LineaFactura l = new LineaFactura();
        l.setFactura(f);
        l.setProducto(p);
        l.setCantidad(cantidad);
        l.setPrecioUnitario(new BigDecimal(precio));
        return l;
    }

    @Test
    @DisplayName("PoliticaImpuestoVenta: SIMPLIFICADO → tarifa 0 aunque el producto sea gravado; GENERAL conserva la del producto")
    void politica_decidePorRegimenSellado() {
        PoliticaImpuestoVenta politica = new PoliticaImpuestoVenta();

        assertThat(politica.tarifaAplicable(factura(Regimen.SIMPLIFICADO), TRECE)).isEqualByComparingTo("0");
        assertThat(politica.tarifaAplicable(factura(Regimen.GENERAL), TRECE)).isEqualByComparingTo("13");
        assertThat(politica.tarifaAplicable(factura(Regimen.GENERAL), BigDecimal.ZERO)).isEqualByComparingTo("0");
        assertThat(politica.cobraIva(factura(Regimen.SIMPLIFICADO))).isFalse();
        assertThat(politica.cobraIva(factura(Regimen.GENERAL))).isTrue();
    }

    @ParameterizedTest(name = "precioIncluyeIva={0}")
    @ValueSource(booleans = {true, false})
    @DisplayName("SIMPLIFICADO: línea gravada → impuesto 0 y total = importe al precio del cliente, con o sin IVA incluido")
    void simplificado_lineaGravada_sinImpuesto(boolean incluyeIva) {
        DesgloseImpuesto d = calculadora.desglosarLinea(factura(Regimen.SIMPLIFICADO), producto(true, incluyeIva),
                new BigDecimal("1130.00"));

        assertThat(d.impuesto()).isEqualByComparingTo("0");
        assertThat(d.base()).isEqualByComparingTo("1130.00");
        assertThat(d.total()).isEqualByComparingTo("1130.00");
        assertThat(d.tarifa()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("GENERAL: igual que antes (con IVA incluido 1130 → 1000 + 130; sin IVA incluido 500 → 500 + 65)")
    void general_noRegresiona() {
        DesgloseImpuesto incluido = calculadora.desglosarLinea(factura(Regimen.GENERAL), producto(true, true),
                new BigDecimal("1130.00"));
        DesgloseImpuesto adicional = calculadora.desglosarLinea(factura(Regimen.GENERAL), producto(true, false),
                new BigDecimal("500.00"));

        assertThat(incluido.base()).isEqualByComparingTo("1000.00");
        assertThat(incluido.impuesto()).isEqualByComparingTo("130.00");
        assertThat(adicional.base()).isEqualByComparingTo("500.00");
        assertThat(adicional.impuesto()).isEqualByComparingTo("65.00");
        assertThat(adicional.total()).isEqualByComparingTo("565.00");
    }

    @Test
    @DisplayName("TotalesFacturaService con SIMPLIFICADO: igv 0 y total = suma de líneas (gravada con y sin IVA incluido, exenta)")
    void totales_simplificado() {
        Factura f = factura(Regimen.SIMPLIFICADO);
        List<LineaFactura> lineas = List.of(
                linea(f, producto(true, true), 1, "1130.00"),
                linea(f, producto(true, false), 2, "250.00"),
                linea(f, producto(false, true), 1, "300.00"));

        totales.aplicarTotales(f, lineas);

        assertThat(f.getIgv()).isEqualByComparingTo("0");
        assertThat(f.getSubtotal()).isEqualByComparingTo("1930.00");
        assertThat(f.getTotal()).isEqualByComparingTo("1930.00");
    }

    @Test
    @DisplayName("TotalesFacturaService con GENERAL: mismos totales de siempre (1130 con IVA + 500 + 13 % = 1695)")
    void totales_general_noRegresiona() {
        Factura f = factura(Regimen.GENERAL);
        List<LineaFactura> lineas = List.of(
                linea(f, producto(true, true), 1, "1130.00"),
                linea(f, producto(true, false), 1, "500.00"));

        totales.aplicarTotales(f, lineas);

        assertThat(f.getSubtotal()).isEqualByComparingTo("1500.00");
        assertThat(f.getIgv()).isEqualByComparingTo("195.00");
        assertThat(f.getTotal()).isEqualByComparingTo("1695.00");
    }
}
