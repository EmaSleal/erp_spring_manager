package api.astro.whats_orders_manager.modules.reportes.service.impl;

import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** T-047 (criterio 30): el pendiente de los reportes resta lo acreditado y excluye anuladas y acreditadas totalmente. */
@DisplayName("T-047 — reporte de ventas: totalPendiente con notas de crédito parciales")
class ReporteServiceImplSaldoTest {

    private final ReporteServiceImpl servicio = new ReporteServiceImpl();

    private static Factura factura(String total, String acreditado) {
        Factura f = new Factura();
        f.setTotal(new BigDecimal(total));
        f.setMontoAcreditado(new BigDecimal(acreditado));
        f.setAnulada(false);
        f.setAcreditadaTotalmente(false);
        return f;
    }

    @Test
    @DisplayName("factura de 11500 con una nota parcial APLICADA de 1334,56 → pendiente 10165,44")
    void restaLoAcreditado() {
        Map<String, Object> e = servicio.calcularEstadisticasVentas(List.of(factura("11500.00", "1334.56")));

        assertThat((BigDecimal) e.get("totalPendiente")).isEqualByComparingTo("10165.44");
    }

    @Test
    @DisplayName("una anulada y una acreditada totalmente no suman al pendiente; la otra sí, por su saldo")
    void excluyeAnuladasYAcreditadasTotalmente() {
        Factura anulada = factura("500.00", "0.00");
        anulada.setAnulada(true);
        Factura acreditada = factura("1630.00", "1630.00");
        acreditada.setAcreditadaTotalmente(true);

        Map<String, Object> e = servicio.calcularEstadisticasVentas(
                List.of(anulada, acreditada, factura("11500.00", "1334.56")));

        assertThat((BigDecimal) e.get("totalPendiente")).isEqualByComparingTo("10165.44");
    }
}
