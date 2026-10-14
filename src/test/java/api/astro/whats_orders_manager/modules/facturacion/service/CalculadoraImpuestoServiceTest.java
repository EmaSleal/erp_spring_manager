package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.configuracion.service.ParametroSistemaService;
import api.astro.whats_orders_manager.modules.facturacion.dto.DesgloseImpuesto;
import api.astro.whats_orders_manager.modules.producto.model.ArticuloMaestro;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T-006 — ADR-009: componente único de desglose de IVA (criterio 2).
 */
@DisplayName("CalculadoraImpuestoService — desglose de IVA (ADR-009)")
@SuppressWarnings("deprecation")
class CalculadoraImpuestoServiceTest {

    private static final BigDecimal TRECE = new BigDecimal("13");

    private ParametroSistemaService parametros;
    private CalculadoraImpuestoService calculadora;

    @BeforeEach
    void setUp() {
        parametros = mock(ParametroSistemaService.class);
        when(parametros.obtenerValorDecimal(eq(CalculadoraImpuestoService.CLAVE_TARIFA_GENERAL), any()))
                .thenReturn(TRECE);
        calculadora = new CalculadoraImpuestoService(parametros);
    }

    @Nested
    @DisplayName("desglosar — precio con IVA incluido")
    class ConIvaIncluido {

        @ParameterizedTest(name = "{0} → base {1} + impuesto {2}")
        @CsvSource({
                "1130.00, 1000.00, 130.00",
                "1000.00, 884.96, 115.04",
                "565.00, 500.00, 65.00",
                "0.01, 0.01, 0.00",
                "0.05, 0.04, 0.01",
                "1.00, 0.88, 0.12",
                "0.00, 0.00, 0.00"
        })
        void desglosa(String importe, String base, String impuesto) {
            DesgloseImpuesto d = calculadora.desglosar(new BigDecimal(importe), TRECE, true);

            assertThat(d.base()).isEqualByComparingTo(base);
            assertThat(d.impuesto()).isEqualByComparingTo(impuesto);
            assertThat(d.total()).isEqualByComparingTo(importe);
            assertThat(d.base().scale()).isEqualTo(2);
            assertThat(d.impuesto().scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("cantidad fraccionaria: 2.5 × 452.00 = 1130.00 → 1000.00 + 130.00")
        void cantidadFraccionaria() {
            BigDecimal importe = new BigDecimal("2.5").multiply(new BigDecimal("452.00"));

            DesgloseImpuesto d = calculadora.desglosar(importe, TRECE, true);

            assertThat(d.base()).isEqualByComparingTo("1000.00");
            assertThat(d.impuesto()).isEqualByComparingTo("130.00");
        }

        @Test
        @DisplayName("cantidad fraccionaria con más de 2 decimales: 0.333 × 100.00 = 33.30 (HALF_UP) → 29.47 + 3.83")
        void cantidadFraccionariaRedondeo() {
            BigDecimal importe = new BigDecimal("0.333").multiply(new BigDecimal("100.00")); // 33.30000

            DesgloseImpuesto d = calculadora.desglosar(importe, TRECE, true);

            assertThat(d.base()).isEqualByComparingTo("29.47");
            assertThat(d.impuesto()).isEqualByComparingTo("3.83");
            assertThat(d.total()).isEqualByComparingTo("33.30");
        }
    }

    @Nested
    @DisplayName("desglosar — precio sin IVA incluido")
    class SinIvaIncluido {

        @Test
        @DisplayName("1000.00 → base 1000.00 + impuesto 130.00")
        void sumaImpuesto() {
            DesgloseImpuesto d = calculadora.desglosar(new BigDecimal("1000.00"), TRECE, false);

            assertThat(d.base()).isEqualByComparingTo("1000.00");
            assertThat(d.impuesto()).isEqualByComparingTo("130.00");
            assertThat(d.total()).isEqualByComparingTo("1130.00");
        }

        @Test
        @DisplayName("884.96 → impuesto 115.0448 redondeado HALF_UP a 115.04")
        void redondeaHalfUp() {
            DesgloseImpuesto d = calculadora.desglosar(new BigDecimal("884.96"), TRECE, false);

            assertThat(d.impuesto()).isEqualByComparingTo("115.04");
        }

        @Test
        @DisplayName("producto con precioIncluyeIva=false suma el impuesto sobre el importe")
        void productoSinIvaIncluido() {
            Producto p = producto(true, TRECE);
            p.setPrecioIncluyeIva(false);

            DesgloseImpuesto d = calculadora.desglosarLinea(p, new BigDecimal("500.00"));

            assertThat(d.base()).isEqualByComparingTo("500.00");
            assertThat(d.impuesto()).isEqualByComparingTo("65.00");
            assertThat(d.total()).isEqualByComparingTo("565.00");
        }
    }

    @Nested
    @DisplayName("exento / no gravado")
    class Exento {

        @Test
        @DisplayName("tarifa 0 → impuesto 0, base = importe")
        void tarifaCero() {
            DesgloseImpuesto d = calculadora.desglosar(new BigDecimal("1130.00"), BigDecimal.ZERO, true);

            assertThat(d.base()).isEqualByComparingTo("1130.00");
            assertThat(d.impuesto()).isEqualByComparingTo("0");
            assertThat(d.gravado()).isFalse();
        }

        @Test
        @DisplayName("producto no gravado (maestro) → impuesto 0 aunque el producto deprecado diga gravado")
        void productoExento() {
            ArticuloMaestro maestro = new ArticuloMaestro();
            maestro.setGravado(false);
            Producto p = producto(true, TRECE);
            p.setArticuloMaestro(maestro);

            DesgloseImpuesto d = calculadora.desglosarLinea(p, new BigDecimal("1130.00"));

            assertThat(d.base()).isEqualByComparingTo("1130.00");
            assertThat(d.impuesto()).isEqualByComparingTo("0");
            assertThat(d.tarifa()).isEqualByComparingTo("0");
        }
    }

    @Nested
    @DisplayName("resolverTarifa")
    class Tarifa {

        @Test
        @DisplayName("gravado con porcentaje propio → usa el del producto")
        void porcentajeDelProducto() {
            assertThat(calculadora.resolverTarifa(producto(true, new BigDecimal("4")))).isEqualByComparingTo("4");
        }

        @Test
        @DisplayName("gravado sin porcentaje → tarifa general del parámetro FACTURA_IVA_PORCENTAJE")
        void gravadoSinPorcentaje_usaTarifaGeneral() {
            when(parametros.obtenerValorDecimal(eq(CalculadoraImpuestoService.CLAVE_TARIFA_GENERAL), any()))
                    .thenReturn(new BigDecimal("13.00"));

            assertThat(calculadora.resolverTarifa(producto(true, null))).isEqualByComparingTo("13");
        }

        @Test
        @DisplayName("gravado indefinido (null) → tarifa general (ADR-009)")
        void gravadoNull_usaTarifaGeneral() {
            assertThat(calculadora.resolverTarifa(producto(null, null))).isEqualByComparingTo("13");
        }

        @Test
        @DisplayName("el maestro tiene precedencia sobre los campos deprecados del producto")
        void maestroTienePrecedencia() {
            ArticuloMaestro maestro = new ArticuloMaestro();
            maestro.setGravado(true);
            maestro.setPorcentajeImpuesto(new BigDecimal("2"));
            Producto p = producto(false, new BigDecimal("13"));
            p.setArticuloMaestro(maestro);

            assertThat(calculadora.resolverTarifa(p)).isEqualByComparingTo("2");
        }

        @Test
        @DisplayName("parámetro ausente → respaldo 13 %")
        void parametroAusente_respaldo13() {
            when(parametros.obtenerValorDecimal(eq(CalculadoraImpuestoService.CLAVE_TARIFA_GENERAL), any()))
                    .thenReturn(null);

            assertThat(calculadora.getTarifaGeneral()).isEqualByComparingTo("13");
        }

        @Test
        @DisplayName("precioIncluyeIva por defecto es true en un producto nuevo")
        void precioIncluyeIvaPorDefecto() {
            assertThat(new Producto().getPrecioIncluyeIva()).isTrue();
        }
    }

    private static Producto producto(Boolean gravado, BigDecimal porcentaje) {
        Producto p = new Producto();
        p.setGravado(gravado);
        p.setPorcentajeImpuesto(porcentaje);
        return p;
    }
}
