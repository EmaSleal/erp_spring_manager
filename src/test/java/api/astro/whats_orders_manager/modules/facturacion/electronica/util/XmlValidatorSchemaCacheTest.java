package api.astro.whats_orders_manager.modules.facturacion.electronica.util;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/** T-036 (criterio 4): el Schema se compila una vez por tipo y se comparte; un Validator por validación. */
@DisplayName("T-036 — Schema XSD compilado una vez por tipo, validaciones concurrentes")
class XmlValidatorSchemaCacheTest {

    @Test
    @DisplayName("la misma instancia de Schema se reutiliza por tipo y cada tipo tiene la suya")
    void schemaReutilizadoPorTipo() {
        for (TipoComprobanteElectronico tipo : List.of(TipoComprobanteElectronico.FACTURA_ELECTRONICA,
                TipoComprobanteElectronico.TIQUETE_ELECTRONICO, TipoComprobanteElectronico.NOTA_CREDITO,
                TipoComprobanteElectronico.NOTA_DEBITO)) {
            assertThat(XmlValidator.schemaDe(tipo)).isSameAs(XmlValidator.schemaDe(tipo));
        }
        assertThat(XmlValidator.schemaDe(TipoComprobanteElectronico.FACTURA_ELECTRONICA))
                .isNotSameAs(XmlValidator.schemaDe(TipoComprobanteElectronico.NOTA_CREDITO));
    }

    @Test
    @DisplayName("validar no recompila el Schema y 20 validaciones en paralelo dan el mismo resultado, con línea y columna")
    void validacionesConcurrentes() {
        var schema = XmlValidator.schemaDe(TipoComprobanteElectronico.FACTURA_ELECTRONICA);
        String xml = "<FacturaElectronica xmlns=\"https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/facturaElectronica\">"
                + "<Clave>1</Clave></FacturaElectronica>";

        List<List<String>> resultados = IntStream.range(0, 20).parallel()
                .mapToObj(i -> XmlValidator.erroresContraXsd(xml, TipoComprobanteElectronico.FACTURA_ELECTRONICA))
                .toList();

        assertThat(resultados).hasSize(20).allSatisfy(errores -> assertThat(errores).isNotEmpty()
                .allSatisfy(e -> assertThat(e).matches("(?s)línea \\d+, columna \\d+: .+")));
        assertThat(resultados).containsOnly(resultados.get(0));
        assertThat(XmlValidator.schemaDe(TipoComprobanteElectronico.FACTURA_ELECTRONICA)).isSameAs(schema);
    }
}
