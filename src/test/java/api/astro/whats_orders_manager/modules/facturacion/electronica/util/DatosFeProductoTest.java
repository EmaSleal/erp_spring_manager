package api.astro.whats_orders_manager.modules.facturacion.electronica.util;

import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** T-033 (criterios 5 y 13): clasificación por CAByS y sin CAByS por defecto. */
@DisplayName("T-033 — CAByS: servicio o mercancía, sin valor por defecto")
@SuppressWarnings("deprecation")
class DatosFeProductoTest {

    @ParameterizedTest(name = "primer dígito {0} → servicio = {1}")
    @CsvSource({"0,false", "1,false", "2,false", "3,false", "4,false", "5,true", "6,true", "7,true", "8,true",
            "9,true"})
    @DisplayName("0 a 4 = mercancía; 5 a 9 = servicio")
    void esServicio_porPrimerDigito(char digito, boolean servicio) {
        assertThat(DatosFeProducto.esServicio(digito + "234567890123")).isEqualTo(servicio);
    }

    @Test
    @DisplayName("CAByS vacío o no numérico → error")
    void esServicio_cabysInvalido() {
        assertThatThrownBy(() -> DatosFeProducto.esServicio(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DatosFeProducto.esServicio("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DatosFeProducto.esServicio("X234")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("un producto sin CAByS no recibe uno inventado: codigoCabys es null y aparece en productosSinCabys")
    void sinCabysPorDefecto() {
        Producto sin = producto("Servicio sin código", null);
        Producto blanco = producto("Otro sin código", "  ");
        Producto con = producto("Con código", "1234567890123");

        assertThat(DatosFeProducto.codigoCabys(sin)).isNull();
        assertThat(DatosFeProducto.codigoCabys(con)).isEqualTo("1234567890123");
        assertThat(DatosFeProducto.productosSinCabys(List.of(linea(sin), linea(con), linea(blanco), linea(sin))))
                .containsExactly("Servicio sin código", "Otro sin código");
    }

    private static Producto producto(String descripcion, String cabys) {
        Producto p = new Producto();
        p.setDescripcion(descripcion);
        p.setCodigoCabys(cabys);
        return p;
    }

    private static LineaFactura linea(Producto p) {
        LineaFactura l = new LineaFactura();
        l.setProducto(p);
        return l;
    }
}
