package api.astro.whats_orders_manager.modules.facturacion.electronica.util;

import api.astro.whats_orders_manager.modules.configuracion.model.Presentacion;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.producto.model.ArticuloMaestro;
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
        Producto sin = conMaestro("Servicio sin código", null);
        Producto blanco = conMaestro("Otro sin código", "  ");
        Producto con = conMaestro("Con código", "1234567890123");

        assertThat(DatosFeProducto.codigoCabys(sin)).isNull();
        assertThat(DatosFeProducto.codigoCabys(con)).isEqualTo("1234567890123");
        assertThat(DatosFeProducto.productosSinCabys(List.of(linea(sin), linea(con), linea(blanco), linea(sin))))
                .containsExactly("Servicio sin código", "Otro sin código");
    }

    // ---- criterio 8 (ADR-040): el CAByS sale solo del artículo maestro ----

    @Test
    @DisplayName("maestro con código → ese código")
    void cabys_maestroConCodigo() {
        assertThat(DatosFeProducto.codigoCabys(conMaestro("A", " 1234567890123 "))).isEqualTo("1234567890123");
    }

    @Test
    @DisplayName("maestro sin código → sin CAByS")
    void cabys_maestroSinCodigo() {
        Producto p = conMaestro("A", null);
        assertThat(DatosFeProducto.codigoCabys(p)).isNull();
        assertThat(DatosFeProducto.productosSinCabys(List.of(linea(p)))).containsExactly("A");
    }

    @Test
    @DisplayName("sin maestro → sin CAByS, aunque Producto.codigoCabys tenga valor")
    void cabys_sinMaestro() {
        Producto p = producto("Suelto", "1234567890123");
        assertThat(DatosFeProducto.codigoCabys(p)).isNull();
        assertThat(DatosFeProducto.productosSinCabys(List.of(linea(p)))).containsExactly("Suelto");
    }

    @Test
    @DisplayName("producto con código propio y maestro sin código → sin CAByS (sin respaldo en Producto)")
    void cabys_codigoPropioMaestroSinCodigo() {
        Producto p = conMaestro("A", null);
        p.setCodigoCabys("1234567890123");
        assertThat(DatosFeProducto.codigoCabys(p)).isNull();
    }

    @Test
    @DisplayName("la descripción sale del maestro aunque Producto tenga otra")
    void descripcion_delMaestro() {
        Producto p = conMaestro("Del maestro", "1234567890123");
        p.setDescripcion("De producto");
        assertThat(DatosFeProducto.descripcion(p)).isEqualTo("Del maestro");
    }

    // ---- criterio 11: unidad de medida ----

    @Test
    @DisplayName("presentación empacada → Unid (aunque tenga código)")
    void unidad_empacada() {
        assertThat(DatosFeProducto.unidadMedida(conPresentacion("200 g", true, "Kg"))).isEqualTo("Unid");
    }

    @Test
    @DisplayName("presentación no empacada → su codigo_unidad_fe")
    void unidad_noEmpacada() {
        assertThat(DatosFeProducto.unidadMedida(conPresentacion("Litro", false, "L"))).isEqualTo("L");
    }

    @Test
    @DisplayName("sin presentación → Unid")
    void unidad_sinPresentacion() {
        assertThat(DatosFeProducto.unidadMedida(conMaestro("A", "1234567890123"))).isEqualTo("Unid");
    }

    @Test
    @DisplayName("no empacada con codigo_unidad_fe nulo o vacío → Unid")
    void unidad_noEmpacadaSinCodigo() {
        assertThat(DatosFeProducto.unidadMedida(conPresentacion("Litro", false, null))).isEqualTo("Unid");
        assertThat(DatosFeProducto.unidadMedida(conPresentacion("Litro", false, " "))).isEqualTo("Unid");
    }

    @Test
    @DisplayName("empacado nulo cuenta como empacada")
    void unidad_empacadoNulo() {
        Producto p = conPresentacion("200 g", true, "Kg");
        p.getPresentacion().setEmpacado(null);
        assertThat(DatosFeProducto.unidadMedida(p)).isEqualTo("Unid");
        assertThat(DatosFeProducto.detalle(p)).isEqualTo("Alivion 200 g");
    }

    // ---- criterio 12: Detalle ----

    @Test
    @DisplayName("empacada → «descripción presentación»")
    void detalle_empacada() {
        assertThat(DatosFeProducto.detalle(conPresentacion("200 g", true, null))).isEqualTo("Alivion 200 g");
    }

    @Test
    @DisplayName("no empacada o sin presentación → solo la descripción")
    void detalle_noEmpacadaOSin() {
        assertThat(DatosFeProducto.detalle(conPresentacion("Litro", false, "L"))).isEqualTo("Alivion");
        assertThat(DatosFeProducto.detalle(conMaestro("Alivion", "1234567890123"))).isEqualTo("Alivion");
    }

    @Test
    @DisplayName("DETALLE_MAX coincide con el maxLength de Detalle en los XSD v4.4")
    void detalleMax_coincideConXsd() throws Exception {
        for (String xsd : List.of("FacturaElectronica_V4.4.xsd", "TiqueteElectronico_V4.4.xsd",
                "NotaCreditoElectronica_V4.4.xsd")) {
            String texto = new String(getClass().getResourceAsStream("/xsd/" + xsd).readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8);
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("name=\"Detalle\">.*?<xs:maxLength value=\"(\\d+)\"", java.util.regex.Pattern.DOTALL)
                    .matcher(texto);
            assertThat(m.find()).as(xsd).isTrue();
            assertThat(Integer.parseInt(m.group(1))).as(xsd).isEqualTo(DatosFeProducto.DETALLE_MAX);
        }
    }

    @Test
    @DisplayName("descripción larga: se recorta a DETALLE_MAX y conserva la presentación")
    void detalle_largo() {
        Producto p = conPresentacion("200 g", true, null);
        p.getArticuloMaestro().setDescripcion("X".repeat(300));
        String detalle = DatosFeProducto.detalle(p);
        assertThat(detalle).hasSize(DatosFeProducto.DETALLE_MAX).endsWith(" 200 g");

        Producto noEmpacado = conPresentacion("Litro", false, "L");
        noEmpacado.getArticuloMaestro().setDescripcion("X".repeat(300));
        assertThat(DatosFeProducto.detalle(noEmpacado)).hasSize(DatosFeProducto.DETALLE_MAX);
    }

    private static Producto producto(String descripcion, String cabys) {
        Producto p = new Producto();
        p.setDescripcion(descripcion);
        p.setCodigoCabys(cabys);
        return p;
    }

    private static Producto conMaestro(String descripcion, String cabys) {
        ArticuloMaestro m = new ArticuloMaestro();
        m.setDescripcion(descripcion);
        m.setCodigoCabys(cabys);
        Producto p = new Producto();
        p.setArticuloMaestro(m);
        return p;
    }

    private static Producto conPresentacion(String nombre, boolean empacado, String codigo) {
        Producto p = conMaestro("Alivion", "1234567890123");
        Presentacion pr = new Presentacion();
        pr.setNombre(nombre);
        pr.setEmpacado(empacado);
        pr.setCodigoUnidadFE(codigo);
        p.setPresentacion(pr);
        return p;
    }

    private static LineaFactura linea(Producto p) {
        LineaFactura l = new LineaFactura();
        l.setProducto(p);
        return l;
    }
}
