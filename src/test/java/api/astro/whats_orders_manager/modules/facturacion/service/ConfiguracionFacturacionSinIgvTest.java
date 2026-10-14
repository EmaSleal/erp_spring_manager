package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.model.ConfiguracionFacturacion;
import api.astro.whats_orders_manager.modules.facturacion.service.impl.ConfiguracionFacturacionServiceImpl;
import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * ADR-009 (T-006, revisión 1): la configuración de facturación ya no lleva la tarifa
 * heredada del IGV peruano (18 %). La tarifa de IVA vive solo en FACTURA_IVA_PORCENTAJE.
 */
class ConfiguracionFacturacionSinIgvTest {

    @Test
    void laEntidadNoMapeaLaColumnaIgv() {
        boolean mapeaIgv = Arrays.stream(ConfiguracionFacturacion.class.getDeclaredFields())
                .anyMatch(ConfiguracionFacturacionSinIgvTest::esColumnaIgv);

        assertThat(mapeaIgv).isFalse();
    }

    @Test
    void validarConfiguracionNoExigeTarifaDeImpuesto() {
        ConfiguracionFacturacion configuracion = new ConfiguracionFacturacion();
        configuracion.setSerieFactura("FA01");
        configuracion.setNumeroInicial(1);
        configuracion.setNumeroActual(1);
        configuracion.setMoneda("CRC");
        configuracion.setSimboloMoneda("₡");
        configuracion.setDecimales(2);
        configuracion.setFormatoNumero("{serie}-{numero}");

        assertThatCode(() -> new ConfiguracionFacturacionServiceImpl().validarConfiguracion(configuracion))
                .doesNotThrowAnyException();
    }

    private static boolean esColumnaIgv(Field campo) {
        Column columna = campo.getAnnotation(Column.class);
        return "igv".equalsIgnoreCase(campo.getName())
                || (columna != null && "igv".equalsIgnoreCase(columna.name()));
    }
}
