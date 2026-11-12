package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.service.PoliticaMutabilidadFactura.Decision;
import api.astro.whats_orders_manager.modules.facturacion.service.PoliticaMutabilidadFactura.Mutabilidad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-025 — reglas de ADR-030 §2 de PoliticaMutabilidadFactura para cada estado del comprobante vigente.
 */
@DisplayName("T-025 — PoliticaMutabilidadFactura: decisión por estado del comprobante vigente")
class PoliticaMutabilidadFacturaTest {

    private static final String CLAVE = "506" + "1".repeat(47);

    @ParameterizedTest
    @EnumSource(value = EstadoComprobante.class,
            names = {"GENERADO", "FIRMADO", "ERROR", "NO_TRANSMITIDO", "RECHAZADO", "ANULADO"})
    @DisplayName("estados que Hacienda no tiene, el rechazado y el anulado → EDITABLE")
    void editables(EstadoComprobante estado) {
        Decision decision = PoliticaMutabilidadFactura.decidir(estado, CLAVE, null);
        assertThat(decision.mutabilidad()).isEqualTo(Mutabilidad.EDITABLE);
        assertThat(decision.permite()).isTrue();
    }

    @Test
    @DisplayName("ACEPTADO → BLOQUEADA: emitir nota de crédito o débito")
    void aceptado() {
        Decision decision = PoliticaMutabilidadFactura.decidir(EstadoComprobante.ACEPTADO, CLAVE, null);
        assertThat(decision.mutabilidad()).isEqualTo(Mutabilidad.BLOQUEADA);
        assertThat(decision.motivo()).contains(CLAVE).contains("nota de crédito o débito");
    }

    @ParameterizedTest
    @EnumSource(value = EstadoComprobante.class, names = {"ENVIADO", "PROCESANDO"})
    @DisplayName("ENVIADO / PROCESANDO → BLOQUEADA: esperar la respuesta de Hacienda")
    void enHacienda(EstadoComprobante estado) {
        Decision decision = PoliticaMutabilidadFactura.decidir(estado, CLAVE, null);
        assertThat(decision.mutabilidad()).isEqualTo(Mutabilidad.BLOQUEADA);
        assertThat(decision.motivo()).contains("espere la respuesta de Hacienda");
    }

    @Test
    @DisplayName("ERROR_HACIENDA (ind-estado 'error') → BLOQUEADA: revisar el comprobante en ATV")
    void errorHacienda() {
        Decision decision = PoliticaMutabilidadFactura.decidir(EstadoComprobante.ERROR_HACIENDA, CLAVE, null);
        assertThat(decision.mutabilidad()).isEqualTo(Mutabilidad.BLOQUEADA);
        assertThat(decision.motivo()).contains(CLAVE).contains("ATV");
    }

    @Test
    @DisplayName("ENVIO_INCIERTO → REQUIERE_VERIFICACION: consultar el estado en Hacienda; un 404 cuenta desde fechaEnvio + 15 min")
    void envioIncierto() {
        Decision decision = PoliticaMutabilidadFactura.decidir(EstadoComprobante.ENVIO_INCIERTO, CLAVE,
                LocalDateTime.of(2026, 10, 17, 9, 50));
        assertThat(decision.mutabilidad()).isEqualTo(Mutabilidad.REQUIERE_VERIFICACION);
        assertThat(decision.permite()).isFalse();
        assertThat(decision.motivo()).contains("Consulte el estado en Hacienda").contains("10:05");
    }
}
