package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.event.ComprobanteNotaResueltoEvent;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.enums.TipoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.NotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.repository.NotaFacturaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** T-032 (criterios 14 a 16): efectos de la nota cuando Hacienda resuelve su comprobante. */
@DisplayName("T-032 — efectos de la nota de crédito al resolverse su comprobante")
class NotaFacturaEfectosTest {

    private static final LocalDateTime EMISION_NOTA = LocalDateTime.of(2026, 11, 12, 10, 0);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 12, 11, 0);
    private static final LocalDate FECHA_VENTA = LocalDate.of(2026, 10, 3);

    private final NotaFacturaRepository notaRepository = mock(NotaFacturaRepository.class);
    private final AsientoContableService asientoService = mock(AsientoContableService.class);
    private final FacturaService facturaService = mock(FacturaService.class);
    private final NotaFacturaEfectos efectos = new NotaFacturaEfectos(notaRepository, asientoService, facturaService,
            Clock.fixed(AHORA.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()));

    private Factura factura;
    private AsientoContable venta;

    @BeforeEach
    void setUp() {
        factura = new Factura();
        factura.setIdFactura(9);
        factura.setNumeroFactura("F-9");
        factura.setAnulada(false);
        venta = new AsientoContable();
        venta.setIdAsiento(100L);
        venta.setFecha(FECHA_VENTA);
        when(asientoService.obtenerAsientosVentaVigentes(factura)).thenReturn(List.of(venta));
    }

    @Test
    @DisplayName("ACEPTADO (01): reversión fechada con la emisión de la nota, factura anulada y nota APLICADA")
    void aceptado_codigo01() {
        NotaFactura nota = nota("01");

        efectos.alResolverseComprobante(evento(nota, EstadoComprobante.ACEPTADO));

        verify(asientoService).generarAsientoReversion(eq(venta), anyString(), eq(EMISION_NOTA.toLocalDate()), eq(nota));
        verify(facturaService).anularPorNotaCredito(eq(factura), any());
        assertThat(nota.getEstado()).isEqualTo(EstadoNotaFactura.APLICADA);
        assertThat(nota.getFechaAplicacion()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("ACEPTADO (13): reversión con la fecha del asiento de venta original")
    void aceptado_codigo13() {
        NotaFactura nota = nota("13");

        efectos.alResolverseComprobante(evento(nota, EstadoComprobante.ACEPTADO));

        verify(asientoService).generarAsientoReversion(eq(venta), anyString(), eq(FECHA_VENTA), eq(nota));
        assertThat(NotaFacturaEfectos.fechaDelAsiento(nota, venta)).isEqualTo(FECHA_VENTA);
        assertThat(NotaFacturaEfectos.fechaDelAsiento(nota("01"), venta)).isEqualTo(EMISION_NOTA.toLocalDate());
    }

    @Test
    @DisplayName("el motivo de la anulación cita la nota y la clave de su comprobante")
    void motivoCitaLaNota() {
        NotaFactura nota = nota("01");

        efectos.alResolverseComprobante(evento(nota, EstadoComprobante.ACEPTADO));

        verify(facturaService).anularPorNotaCredito(eq(factura),
                org.mockito.ArgumentMatchers.argThat(m -> m.contains("Nota de crédito 5") && m.contains("CLAVE-NC")));
    }

    @Test
    @DisplayName("invocado dos veces con ACEPTADO, los efectos se aplican una sola vez")
    void idempotente() {
        NotaFactura nota = nota("01");
        ComprobanteNotaResueltoEvent evento = evento(nota, EstadoComprobante.ACEPTADO);

        efectos.alResolverseComprobante(evento);
        efectos.alResolverseComprobante(evento);

        verify(asientoService, times(1)).generarAsientoReversion(any(), anyString(), any(), any());
        verify(facturaService, times(1)).anularPorNotaCredito(any(), any());
    }

    @Test
    @DisplayName("sin asiento de venta vigente: se omite la reversión y el resto se aplica")
    void sinAsientoDeVenta() {
        when(asientoService.obtenerAsientosVentaVigentes(factura)).thenReturn(List.of());
        NotaFactura nota = nota("01");

        efectos.alResolverseComprobante(evento(nota, EstadoComprobante.ACEPTADO));

        verify(asientoService, never()).generarAsientoReversion(any(), anyString(), any(), any());
        verify(facturaService).anularPorNotaCredito(eq(factura), any());
        assertThat(nota.getEstado()).isEqualTo(EstadoNotaFactura.APLICADA);
    }

    @Test
    @DisplayName("RECHAZADO: nota RECHAZADA, sin asiento y la factura sigue igual")
    void rechazado() {
        NotaFactura nota = nota("01");

        efectos.alResolverseComprobante(evento(nota, EstadoComprobante.RECHAZADO));

        assertThat(nota.getEstado()).isEqualTo(EstadoNotaFactura.RECHAZADA);
        verify(asientoService, never()).generarAsientoReversion(any(), anyString(), any(), any());
        verify(facturaService, never()).anularPorNotaCredito(any(), any());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = EstadoComprobante.class, names = {"ERROR", "NO_TRANSMITIDO", "ENVIO_INCIERTO", "PROCESANDO",
            "ERROR_HACIENDA"})
    @DisplayName("otros estados: la nota sigue EN_TRAMITE")
    void otrosEstados(EstadoComprobante estado) {
        NotaFactura nota = nota("01");

        efectos.alResolverseComprobante(evento(nota, estado));

        assertThat(nota.getEstado()).isEqualTo(EstadoNotaFactura.EN_TRAMITE);
        verify(facturaService, never()).anularPorNotaCredito(any(), any());
    }

    @Test
    @DisplayName("un comprobante que no es el vigente de la nota no aplica efectos")
    void comprobanteNoVigente() {
        NotaFactura nota = nota("01");
        ComprobanteNotaResueltoEvent evento = evento(nota, EstadoComprobante.ACEPTADO);
        evento.comprobante().setVigente(false);

        efectos.alResolverseComprobante(evento);

        assertThat(nota.getEstado()).isEqualTo(EstadoNotaFactura.EN_TRAMITE);
    }

    private NotaFactura nota(String codigo) {
        return NotaFactura.builder().id(5L).factura(factura).tipo(TipoNotaFactura.CREDITO).codigoReferencia(codigo)
                .razon("Anulación total").estado(EstadoNotaFactura.EN_TRAMITE).fechaEmision(EMISION_NOTA).build();
    }

    private static ComprobanteNotaResueltoEvent evento(NotaFactura nota, EstadoComprobante estado) {
        return new ComprobanteNotaResueltoEvent(ComprobanteElectronico.builder().id(50L).notaFactura(nota)
                .claveNumerica("CLAVE-NC").estado(estado).vigente(true).build());
    }
}
