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
import api.astro.whats_orders_manager.modules.facturacion.model.Pago;
import api.astro.whats_orders_manager.modules.facturacion.repository.NotaFacturaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
    private final SaldoFavorService saldoFavorService = mock(SaldoFavorService.class);
    private final jakarta.persistence.EntityManager entityManager = mock(jakarta.persistence.EntityManager.class);
    private final api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository facturaRepository =
            mock(api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository.class);
    private final NotaFacturaEfectos efectos = new NotaFacturaEfectos(notaRepository, asientoService, facturaService,
            Clock.fixed(AHORA.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()),
            facturaRepository, saldoFavorService, entityManager);

    private Factura factura;
    private AsientoContable venta;

    @BeforeEach
    void setUp() {
        factura = new Factura();
        factura.setIdFactura(9);
        factura.setNumeroFactura("F-9");
        factura.setTotal(new BigDecimal("300.00"));
        factura.setAnulada(false);
        venta = new AsientoContable();
        venta.setIdAsiento(100L);
        venta.setFecha(FECHA_VENTA);
        when(asientoService.obtenerAsientosVentaVigentes(factura)).thenReturn(List.of(venta));
    }

    @Test
    @DisplayName("ACEPTADO (01): asiento de la nota fechado con su emisión, factura anulada y nota APLICADA")
    void aceptado_codigo01() {
        NotaFactura nota = nota("01");

        efectos.alResolverseComprobante(evento(nota, EstadoComprobante.ACEPTADO));

        verify(asientoService).generarAsientoNotaCredito(eq(nota), eq(venta), eq(EMISION_NOTA.toLocalDate()), any(), any());
        verify(asientoService, never()).generarAsientoReversion(any(), anyString(), any(), any());
        verify(facturaService).anularPorNotaCredito(eq(factura), any());
        assertThat(nota.getEstado()).isEqualTo(EstadoNotaFactura.APLICADA);
        assertThat(nota.getFechaAplicacion()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("ACEPTADO (13): asiento con la fecha de la nota (ADR-050), no la del asiento de venta original")
    void aceptado_codigo13() {
        NotaFactura nota = nota("13");

        efectos.alResolverseComprobante(evento(nota, EstadoComprobante.ACEPTADO));

        verify(asientoService).generarAsientoNotaCredito(eq(nota), eq(venta), eq(EMISION_NOTA.toLocalDate()), any(), any());
        assertThat(NotaFacturaEfectos.fechaDelAsiento(nota)).isEqualTo(EMISION_NOTA.toLocalDate())
                .isNotEqualTo(FECHA_VENTA);
        assertThat(NotaFacturaEfectos.fechaDelAsiento(nota("01"))).isEqualTo(EMISION_NOTA.toLocalDate());
    }

    @Test
    @DisplayName("una nota sin fecha contable no se aplica")
    void sinFechaContable() {
        NotaFactura nota = nota("01");
        nota.setFechaContable(null);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> NotaFacturaEfectos.fechaDelAsiento(nota))
                .isInstanceOf(IllegalStateException.class);
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

        verify(asientoService, times(1)).generarAsientoNotaCredito(any(), any(), any(), any(), any());
        verify(facturaService, times(1)).anularPorNotaCredito(any(), any());
    }

    @Test
    @DisplayName("sin asiento de venta vigente: se omite el asiento y el resto se aplica")
    void sinAsientoDeVenta() {
        when(asientoService.obtenerAsientosVentaVigentes(factura)).thenReturn(List.of());
        NotaFactura nota = nota("01");

        efectos.alResolverseComprobante(evento(nota, EstadoComprobante.ACEPTADO));

        verify(asientoService, never()).generarAsientoNotaCredito(any(), any(), any(), any(), any());
        verify(facturaService).anularPorNotaCredito(eq(factura), any());
        assertThat(nota.getEstado()).isEqualTo(EstadoNotaFactura.APLICADA);
    }

    @Test
    @DisplayName("T-048: la factura se bloquea FOR UPDATE (refresh) antes de calcular el reparto")
    void bloqueaLaFactura() {
        efectos.alResolverseComprobante(evento(nota("01"), EstadoComprobante.ACEPTADO));

        verify(entityManager).refresh(factura, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    @DisplayName("T-048: sin pagos todo el total va a CxC y no se crea saldo a favor")
    void reparto_sinPagos() {
        NotaFactura nota = nota("01");

        efectos.alResolverseComprobante(evento(nota, EstadoComprobante.ACEPTADO));

        verify(asientoService).generarAsientoNotaCredito(eq(nota), eq(venta), eq(EMISION_NOTA.toLocalDate()),
                argThat(m -> m.compareTo(new BigDecimal("300.00")) == 0), argThat(m -> m.signum() == 0));
        verify(saldoFavorService, never()).crearDesdeNota(any(), any(), any());
        assertThat(nota.getMontoCreditoCxc()).isEqualByComparingTo("300.00");
        assertThat(nota.getMontoSaldoFavor()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("T-048: nota total sobre factura con pago: CxC = saldo pendiente, excedente = lo pagado a saldo a favor; la factura se anula y los pagos no se tocan")
    void reparto_notaTotalConPagos() {
        Pago pago = pagoValido("120.00");
        factura.getPagos().add(pago);
        NotaFactura nota = nota("01");

        efectos.alResolverseComprobante(evento(nota, EstadoComprobante.ACEPTADO));

        verify(asientoService).generarAsientoNotaCredito(eq(nota), eq(venta), eq(EMISION_NOTA.toLocalDate()),
                argThat(m -> m.compareTo(new BigDecimal("180.00")) == 0),
                argThat(m -> m.compareTo(new BigDecimal("120.00")) == 0));
        verify(saldoFavorService).crearDesdeNota(eq(nota), argThat(m -> m.compareTo(new BigDecimal("120.00")) == 0),
                eq("CLAVE-NC"));
        verify(facturaService).anularPorNotaCredito(eq(factura), any());
        assertThat(pago.getEstado()).isEqualTo(api.astro.whats_orders_manager.modules.facturacion.enums.EstadoPago.CONFIRMADO);
        assertThat(nota.getMontoCreditoCxc()).isEqualByComparingTo("180.00");
        assertThat(nota.getMontoSaldoFavor()).isEqualByComparingTo("120.00");
        assertThat(nota.getEstado()).isEqualTo(EstadoNotaFactura.APLICADA);
    }

    @Test
    @DisplayName("T-048: parcial sobre factura pagada por completo: saldo pendiente 0, todo el total de la nota va a saldo a favor")
    void reparto_parcialSobrePagadaPorCompleto() {
        factura.getPagos().add(pagoValido("300.00"));
        NotaFactura nota = notaParcial("02", "100.00", "300.00");

        efectos.alResolverseComprobante(evento(nota, EstadoComprobante.ACEPTADO));

        verify(asientoService).generarAsientoNotaCredito(eq(nota), eq(venta), eq(EMISION_NOTA.toLocalDate()),
                argThat(m -> m.signum() == 0), argThat(m -> m.compareTo(new BigDecimal("100.00")) == 0));
        verify(saldoFavorService).crearDesdeNota(eq(nota), argThat(m -> m.compareTo(new BigDecimal("100.00")) == 0),
                eq("CLAVE-NC"));
        assertThat(factura.calcularSaldoPendiente()).as("nunca negativo").isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("T-048: si falta el parámetro del saldo a favor el asiento falla, nada se aplica y no se crea saldo")
    void reparto_parametroDeSaldoFavorAusente() {
        factura.getPagos().add(pagoValido("120.00"));
        when(asientoService.generarAsientoNotaCredito(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("Parámetro contable no configurado: SALDO_FAVOR_CLIENTES_CRC"));
        NotaFactura nota = nota("01");

        assertThatThrownBy(() -> efectos.alResolverseComprobante(evento(nota, EstadoComprobante.ACEPTADO)))
                .hasMessageContaining("SALDO_FAVOR_CLIENTES_CRC");

        verify(saldoFavorService, never()).crearDesdeNota(any(), any(), any());
        verify(facturaService, never()).anularPorNotaCredito(any(), any());
        assertThat(nota.getEstado()).isEqualTo(EstadoNotaFactura.EN_TRAMITE);
    }

    private static Pago pagoValido(String montoAplicado) {
        Pago pago = new Pago();
        pago.setEstado(api.astro.whats_orders_manager.modules.facturacion.enums.EstadoPago.CONFIRMADO);
        pago.setMonto(new BigDecimal(montoAplicado));
        pago.setMontoAplicado(new BigDecimal(montoAplicado));
        return pago;
    }

    @Test
    @DisplayName("RECHAZADO: nota RECHAZADA, sin asiento y la factura sigue igual")
    void rechazado() {
        NotaFactura nota = nota("01");

        efectos.alResolverseComprobante(evento(nota, EstadoComprobante.RECHAZADO));

        assertThat(nota.getEstado()).isEqualTo(EstadoNotaFactura.RECHAZADA);
        verify(asientoService, never()).generarAsientoNotaCredito(any(), any(), any(), any(), any());
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


    @Test
    @DisplayName("T-047 ACEPTADO (02, parcial): asiento de la nota, lo acreditado sube, la factura no se anula")
    void aceptado_parcial() {
        factura.setTotal(new BigDecimal("300.00"));
        NotaFactura nota = notaParcial("02", "100.00", "300.00");

        efectos.alResolverseComprobante(evento(nota, EstadoComprobante.ACEPTADO));

        verify(asientoService).generarAsientoNotaCredito(eq(nota), eq(venta), eq(EMISION_NOTA.toLocalDate()), any(), any());
        verify(facturaService, never()).anularPorNotaCredito(any(), any());
        assertThat(factura.getMontoAcreditado()).isEqualByComparingTo("100.00");
        assertThat(factura.estaAcreditadaTotalmente()).isFalse();
        assertThat(factura.calcularSaldoPendiente()).isEqualByComparingTo("200.00");
        verify(facturaRepository).save(factura);
        assertThat(nota.getEstado()).isEqualTo(EstadoNotaFactura.APLICADA);
    }

    @Test
    @DisplayName("T-047: la parcial que completa el TotalComprobante deja la factura acreditada totalmente, sin anular")
    void aceptado_parcialCompletaElTotal() {
        factura.setTotal(new BigDecimal("300.00"));
        factura.setMontoAcreditado(new BigDecimal("180.00"));

        efectos.alResolverseComprobante(evento(notaParcial("14", "120.00", "300.00"), EstadoComprobante.ACEPTADO));

        assertThat(factura.getMontoAcreditado()).isEqualByComparingTo("300.00");
        assertThat(factura.estaAcreditadaTotalmente()).isTrue();
        assertThat(factura.estaAnulada()).isFalse();
        assertThat(factura.calcularSaldoPendiente()).isEqualByComparingTo("0");
        verify(facturaService, never()).anularPorNotaCredito(any(), any());
    }

    @Test
    @DisplayName("T-047 criterio 22/26: si el asiento falla (p. ej. falta el parámetro contable) nada se aplica y la nota sigue EN_TRAMITE")
    void parametroAusente_notaSigueEnTramite() {
        when(asientoService.generarAsientoNotaCredito(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("Parámetro contable no configurado: NOTA_CREDITO_ANULACIONES_DESCUENTOS"));
        factura.setTotal(new BigDecimal("300.00"));
        NotaFactura nota = notaParcial("02", "100.00", "300.00");

        assertThatThrownBy(() -> efectos.alResolverseComprobante(evento(nota, EstadoComprobante.ACEPTADO)))
                .hasMessageContaining("NOTA_CREDITO_ANULACIONES_DESCUENTOS");

        assertThat(nota.getEstado()).isEqualTo(EstadoNotaFactura.EN_TRAMITE);
        assertThat(factura.getMontoAcreditado()).isEqualByComparingTo("0");
        verify(facturaRepository, never()).save(any());
    }

    @Test
    @DisplayName("T-047: una parcial RECHAZADA no deja efectos")
    void parcialRechazada() {
        NotaFactura nota = notaParcial("02", "100.00", "300.00");

        efectos.alResolverseComprobante(evento(nota, EstadoComprobante.RECHAZADO));

        assertThat(nota.getEstado()).isEqualTo(EstadoNotaFactura.RECHAZADA);
        assertThat(factura.getMontoAcreditado()).isEqualByComparingTo("0");
        verify(asientoService, never()).generarAsientoNotaCredito(any(), any(), any(), any(), any());
    }

    private NotaFactura notaParcial(String codigo, String total, String totalComprobante) {
        NotaFactura nota = nota(codigo);
        nota.setTotal(new BigDecimal(total));
        nota.setComprobanteReferencia(ComprobanteElectronico.builder().id(1L).claveNumerica("CLAVE-F")
                .xmlComprobante("<FacturaElectronica><ResumenFactura><TotalComprobante>" + totalComprobante
                        + "</TotalComprobante></ResumenFactura></FacturaElectronica>").build());
        return nota;
    }

    private NotaFactura nota(String codigo) {
        return NotaFactura.builder().id(5L).factura(factura).tipo(TipoNotaFactura.CREDITO).codigoReferencia(codigo)
                .razon("Anulación total").total(new BigDecimal("300.00")).estado(EstadoNotaFactura.EN_TRAMITE).fechaEmision(EMISION_NOTA)
                .fechaContable(EMISION_NOTA.toLocalDate())
                .periodoFiscalAtribucion("13".equals(codigo) ? "2026-10" : "2026-11").build();
    }

    private static ComprobanteNotaResueltoEvent evento(NotaFactura nota, EstadoComprobante estado) {
        return new ComprobanteNotaResueltoEvent(ComprobanteElectronico.builder().id(50L).notaFactura(nota)
                .claveNumerica("CLAVE-NC").estado(estado).vigente(true).build());
    }
}
