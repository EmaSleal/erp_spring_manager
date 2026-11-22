package api.astro.whats_orders_manager.modules.facturacion.scheduler;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.notificacion.enums.TipoNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.service.NotificacionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** T-047 (criterio 32): los recordatorios no incluyen anuladas ni acreditadas totalmente; el monto es el saldo. */
@DisplayName("T-047 — recordatorios de pago: anuladas, acreditadas totalmente y saldo")
class RecordatorioPagoSchedulerTest {

    private static Factura vencida(String numero, String total, String acreditado) {
        Cliente cliente = new Cliente();
        cliente.setTelefono("+50688887777");
        cliente.setEmail("cliente@example.invalid");
        Factura f = new Factura();
        f.setNumeroFactura(numero);
        f.setCliente(cliente);
        f.setTotal(new BigDecimal(total));
        f.setMontoAcreditado(new BigDecimal(acreditado));
        f.setAnulada(false);
        f.setAcreditadaTotalmente(false);
        f.setEntregado(true);
        f.setFechaPago(java.sql.Date.valueOf(LocalDate.now().minusDays(5)));
        return f;
    }

    @Test
    @DisplayName("vencida acreditada totalmente, anulada y con nota parcial: solo la tercera recibe recordatorio, por su saldo")
    void soloLaConSaldo() {
        Factura acreditada = vencida("F-ACRED", "1630.00", "1630.00");
        acreditada.setAcreditadaTotalmente(true);
        Factura anulada = vencida("F-ANUL", "500.00", "0.00");
        anulada.setAnulada(true);
        Factura parcial = vencida("F-PARC", "11500.00", "1334.56");

        FacturaRepository repo = mock(FacturaRepository.class);
        when(repo.findFacturasConPagoVencido(any())).thenReturn(List.of(acreditada, anulada, parcial));
        when(repo.findAll()).thenReturn(List.of());
        NotificacionService notificaciones = mock(NotificacionService.class);
        RecordatorioPagoScheduler scheduler = new RecordatorioPagoScheduler();
        ReflectionTestUtils.setField(scheduler, "facturaRepository", repo);
        ReflectionTestUtils.setField(scheduler, "notificacionService", notificaciones);
        ReflectionTestUtils.setField(scheduler, "clock", java.time.Clock.systemDefaultZone());

        scheduler.enviarRecordatoriosPago();

        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(notificaciones, times(1)).enviarNotificacionExterna(anyString(), eq(TipoNotificacion.FACTURA_VENCIDA),
                any(), anyString(), mensaje.capture());
        assertThat(mensaje.getValue()).contains("F-PARC").contains("10165.44").doesNotContain("11500");
    }
}
