package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.facturacion.service.CalculadoraImpuestoService;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T-018 (ADR-021) — el código de actividad del emisor se envía tal como está registrado; sin código, la
 * emisión falla (ya no se usa el valor por defecto 620100).
 */
@DisplayName("T-018 — código de actividad económica en el XML")
class XmlActividadEconomicaTest {

    private static final String CLAVE_50 = "50601042612345678901200110000011000010000100000001";

    private final XmlGeneratorServiceImpl service = new XmlGeneratorServiceImpl(new CalculadoraImpuestoService(null));

    private ComprobanteElectronico comprobanteCon(String codigoActividad) {
        ConfiguracionEmpresa empresa = ConfiguracionEmpresa.builder()
                .razonSocial("EMANUEL SOTO LEAL")
                .numeroIdentificacion("118200878")
                .codigoActividad(codigoActividad)
                .codigoProvincia("2")
                .canton("01")
                .distrito("04")
                .otrasSenas("Alajuela, el roble")
                .build();
        Producto producto = new Producto();
        producto.setDescripcion("Producto de prueba");
        producto.setCodigoCabys("1234567890001");
        producto.setGravado(false);
        LineaFactura linea = new LineaFactura();
        linea.setProducto(producto);
        Factura factura = new Factura();
        factura.getLineas().add(linea);
        return ComprobanteElectronico.builder()
                .claveNumerica(CLAVE_50)
                .fechaEmision(LocalDateTime.of(2026, 10, 15, 10, 0))
                .empresa(empresa)
                .factura(factura)
                .build();
    }

    @ParameterizedTest(name = "código de actividad [{0}] → IllegalStateException")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void sinCodigo_lanza(String codigo) {
        ComprobanteElectronico comprobante = comprobanteCon(codigo);

        assertThatThrownBy(() -> service.obtenerActividadEconomica(comprobante))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("La empresa no tiene código de actividad económica configurado");
        assertThatThrownBy(() -> service.generarXmlTiquete(comprobante))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("La empresa no tiene código de actividad económica configurado");
    }

    @Test
    @DisplayName("\"4773.0\" se envía tal cual en CodigoActividadEmisor")
    void codigoRegistrado_seEnviaTalCual() {
        ComprobanteElectronico comprobante = comprobanteCon("4773.0");

        assertThat(service.obtenerActividadEconomica(comprobante)).isEqualTo("4773.0");
        assertThat(service.generarXmlTiquete(comprobante))
                .contains("<CodigoActividadEmisor>4773.0</CodigoActividadEmisor>")
                .doesNotContain("620100");
    }
}
