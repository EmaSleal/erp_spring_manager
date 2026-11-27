package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.model.ConfiguracionFacturacion;
import api.astro.whats_orders_manager.modules.facturacion.service.impl.ConfiguracionFacturacionServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T-059 criterio 4 (ADR-078): reglas de guardado del título y la leyenda del PDF del régimen simplificado, con mensaje fijo.
 * Viven en validarConfiguracion, que aplican save, update y saveOrUpdate (todas las rutas de guardado).
 */
@DisplayName("T-059 — validación del título y la leyenda del PDF del RTS")
class ConfiguracionFacturacionPdfRtsValidacionTest {

    static final String MSG_TITULO_OBLIGATORIO = "El título del comprobante del régimen simplificado es obligatorio";
    static final String MSG_TITULO_LARGO = "El título del comprobante del régimen simplificado no puede exceder 120 caracteres";
    static final String MSG_TITULO_SIN_FACTURA =
            "El título del comprobante del régimen simplificado debe contener la palabra «Factura»";
    static final String MSG_LEYENDA_LARGA = "La leyenda del comprobante del régimen simplificado no puede exceder 300 caracteres";

    private final ConfiguracionFacturacionServiceImpl servicio = new ConfiguracionFacturacionServiceImpl();

    private static ConfiguracionFacturacion valida(String titulo, String leyenda) {
        ConfiguracionFacturacion c = new ConfiguracionFacturacion();
        c.setSerieFactura("FA01");
        c.setNumeroInicial(1);
        c.setNumeroActual(1);
        c.setMoneda("CRC");
        c.setSimboloMoneda("₡");
        c.setDecimales(2);
        c.setFormatoNumero("{serie}-{numero}");
        c.setPdfRtsTitulo(titulo);
        c.setPdfRtsLeyenda(leyenda);
        return c;
    }

    @Test
    @DisplayName("título válido: se acepta y se guarda recortado")
    void tituloValido() {
        ConfiguracionFacturacion c = valida("  Factura — Régimen de Tributación Simplificada  ", null);

        assertThatCode(() -> servicio.validarConfiguracion(c)).doesNotThrowAnyException();
        assertThat(c.getPdfRtsTitulo()).isEqualTo("Factura — Régimen de Tributación Simplificada");
    }

    @ParameterizedTest(name = "[{index}] «{0}» se acepta")
    @ValueSource(strings = {"factura", "FACTURA", "Fáctura", "FÁCTURA de venta", "Mi Factura RTS", "Factura", "Una factura.",
            "Factura-RTS", "(Factura)", "Fa\u0301ctura"})
    @DisplayName("la palabra «Factura» se reconoce sin distinguir mayúsculas ni tildes")
    void facturaVariantes(String titulo) {
        assertThatCode(() -> servicio.validarConfiguracion(valida(titulo, null))).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "[{index}] «{0}» → obligatorio")
    @ValueSource(strings = {"", "   ", "\t\n"})
    @DisplayName("título vacío o en blanco: rechazado con mensaje fijo")
    void tituloVacio(String titulo) {
        assertThatThrownBy(() -> servicio.validarConfiguracion(valida(titulo, null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(MSG_TITULO_OBLIGATORIO);
    }

    @Test
    @DisplayName("título nulo: rechazado como obligatorio")
    void tituloNulo() {
        assertThatThrownBy(() -> servicio.validarConfiguracion(valida(null, null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(MSG_TITULO_OBLIGATORIO);
    }

    @ParameterizedTest(name = "[{index}] «{0}» → sin la palabra Factura")
    @ValueSource(strings = {"Comprobante de venta — Régimen de Tributación Simplificada", "Facturación", "Facturas", "Prefactura",
            "Contrafactura", "Fac tura", "Recibo"})
    @DisplayName("título sin la palabra «Factura»: rechazado (palabra completa, no fragmento)")
    void sinFactura(String titulo) {
        assertThatThrownBy(() -> servicio.validarConfiguracion(valida(titulo, null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(MSG_TITULO_SIN_FACTURA);
    }

    @Test
    @DisplayName("título de 120 caracteres (después de recortar): aceptado; de 121: rechazado")
    void tituloLargo() {
        String t120 = "Factura " + "x".repeat(112);
        assertThat(t120).hasSize(120);
        assertThatCode(() -> servicio.validarConfiguracion(valida("  " + t120 + "  ", null))).doesNotThrowAnyException();

        assertThatThrownBy(() -> servicio.validarConfiguracion(valida(t120 + "x", null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(MSG_TITULO_LARGO);
    }

    @Test
    @DisplayName("leyenda en blanco o vacía: se guarda como NULL")
    void leyendaVaciaEsNull() {
        for (String blanco : new String[]{"", "   ", null}) {
            ConfiguracionFacturacion c = valida("Factura RTS", blanco);
            servicio.validarConfiguracion(c);
            assertThat(c.getPdfRtsLeyenda()).isNull();
        }
    }

    @Test
    @DisplayName("leyenda: se guarda recortada; 300 caracteres se aceptan, 301 se rechazan con mensaje fijo")
    void leyendaLarga() {
        ConfiguracionFacturacion c = valida("Factura RTS", "  No cobra IVA.  ");
        servicio.validarConfiguracion(c);
        assertThat(c.getPdfRtsLeyenda()).isEqualTo("No cobra IVA.");

        String l300 = "a".repeat(300);
        assertThatCode(() -> servicio.validarConfiguracion(valida("Factura RTS", " " + l300 + " "))).doesNotThrowAnyException();
        assertThatThrownBy(() -> servicio.validarConfiguracion(valida("Factura RTS", l300 + "a")))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(MSG_LEYENDA_LARGA);
    }

    @Test
    @DisplayName("texto con caracteres especiales se acepta tal cual (se imprime como texto)")
    void caracteresEspeciales() {
        ConfiguracionFacturacion c = valida("Factura <b>RTS</b> & \"x\" 'y'", "<i>leyenda</i> & \"z\"");
        assertThatCode(() -> servicio.validarConfiguracion(c)).doesNotThrowAnyException();
        assertThat(c.getPdfRtsTitulo()).isEqualTo("Factura <b>RTS</b> & \"x\" 'y'");
    }
}
