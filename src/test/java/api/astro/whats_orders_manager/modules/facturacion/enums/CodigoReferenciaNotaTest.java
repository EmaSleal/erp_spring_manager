package api.astro.whats_orders_manager.modules.facturacion.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** T-032 (criterios 4, 23 y 24): catálogo de la nota 9 del Anexo v4.4. */
@DisplayName("T-032 — catálogo de códigos de referencia (nota 9)")
class CodigoReferenciaNotaTest {

    @Test
    @DisplayName("contiene exactamente los códigos oficiales, con sus nombres; el 03 no existe")
    void catalogoOficial() {
        Map<String, String> catalogo = Arrays.stream(CodigoReferenciaNota.values())
                .collect(Collectors.toMap(CodigoReferenciaNota::getCodigo, CodigoReferenciaNota::getNombre));

        assertThat(catalogo).containsOnlyKeys("01", "02", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13",
                "14", "15", "16", "17", "99");
        assertThat(catalogo).containsEntry("01", "Anula documento de referencia")
                .containsEntry("02", "Corrige monto")
                .containsEntry("06", "Devolución de mercancía")
                .containsEntry("12", "Nota de crédito financiera por exoneración posterior a la facturación")
                .containsEntry("13", "Anula documento de referencia por error material")
                .containsEntry("14", "Corrige monto por error material")
                .containsEntry("15", "Sustituye comprobante electrónico por error material")
                .containsEntry("16", "Sustituye comprobante electrónico rechazado")
                .containsEntry("17", "Pago a comprobante electrónico")
                .containsEntry("99", "Otros");
        assertThat(CodigoReferenciaNota.deCodigo("03")).isEmpty();
    }

    @Test
    @DisplayName("periodo del efecto: 01, 02, 06 y 12 → NOTA; 13 y 14 → DOCUMENTO_ORIGINAL; el resto sin regla")
    void periodoDelEfecto() {
        Map<String, PeriodoEfectoNota> conRegla = Arrays.stream(CodigoReferenciaNota.values())
                .filter(c -> c.getPeriodoEfecto() != null)
                .collect(Collectors.toMap(CodigoReferenciaNota::getCodigo, CodigoReferenciaNota::getPeriodoEfecto));

        assertThat(conRegla).containsOnly(
                Map.entry("01", PeriodoEfectoNota.NOTA), Map.entry("02", PeriodoEfectoNota.NOTA),
                Map.entry("06", PeriodoEfectoNota.NOTA), Map.entry("12", PeriodoEfectoNota.NOTA),
                Map.entry("13", PeriodoEfectoNota.DOCUMENTO_ORIGINAL),
                Map.entry("14", PeriodoEfectoNota.DOCUMENTO_ORIGINAL));
    }

    @Test
    @DisplayName("07, 15 y 16 (comprobante sustituto) y 17 (recibo de pago) no se usan en nota de crédito")
    void noValidosEnNotaCredito() {
        assertThat(Arrays.stream(CodigoReferenciaNota.values()).filter(c -> !c.isValidoEnNotaCredito())
                .map(CodigoReferenciaNota::getCodigo)).containsExactlyInAnyOrder("07", "15", "16", "17");
    }

    @Test
    @DisplayName("en T-032 solo se emiten 01 y 13 (nota total)")
    void habilitadosEnT032() {
        assertThat(Arrays.stream(CodigoReferenciaNota.values()).filter(CodigoReferenciaNota::isHabilitadoEnT032)
                .map(CodigoReferenciaNota::getCodigo)).containsExactly("01", "13");
    }
}
