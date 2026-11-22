package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.dto.EmitirNotaCreditoRequest.LineaAcreditar;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.AmbienteComprobanteGuard;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ComprobanteElectronicoService;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.enums.TipoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.exception.NotaFacturaConflictoException;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.NotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.repository.NotaFacturaRepository;
import api.astro.whats_orders_manager.modules.producto.model.ArticuloMaestro;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T-047 (criterios 3, 4, 5 y 14): desglose por línea y redondeo al céntimo de la nota parcial, topes por línea y por
 * factura (justo en el límite y un céntimo por encima) y la nota total tras una parcial. Todo se rechaza antes de
 * guardar la nota y de tomar el consecutivo.
 */
@DisplayName("T-047 — nota de crédito parcial: desglose, redondeo y topes")
class NotaFacturaParcialTest {

    private static final LocalDateTime EMISION = LocalDateTime.of(2026, 11, 12, 16, 5);

    private final FacturaRepository facturas = mock(FacturaRepository.class);
    private final NotaFacturaRepository notas = mock(NotaFacturaRepository.class);
    private final ComprobanteElectronicoRepository comprobantes = mock(ComprobanteElectronicoRepository.class);
    private final ComprobanteElectronicoService comprobanteService = mock(ComprobanteElectronicoService.class);
    private final PagoService pagos = mock(PagoService.class);
    private final CalculadoraImpuestoService calculadora = new CalculadoraImpuestoService(null);
    private final NotaFacturaService servicio = new NotaFacturaService(facturas, notas, comprobantes,
            comprobanteService, mock(api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService.class), calculadora,
            Clock.fixed(EMISION.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()),
            mock(AmbienteComprobanteGuard.class), new AtribucionFiscalService("ORIGINAL"),
            new NotaCreditoTopes(notas, calculadora));

    private Factura factura;

    @BeforeEach
    void setUp() {
        factura = new Factura();
        factura.setIdFactura(77);
        factura.setNumeroFactura("F-77");
        factura.setMonedaFE(MonedaFE.CRC);
        factura.setTipoCambio(BigDecimal.ONE);
        factura.setAnulada(false);
        // línea 1: 2 unidades gravadas 13 %, 2260.00 con IVA; línea 2: exenta, 500.00; línea 3: 1 unidad gravada, 100.00
        factura.getLineas().add(linea(1, producto("Gravado", "1234567890123", true), 2, "2260.00"));
        factura.getLineas().add(linea(2, producto("Exento", "9876543210987", false), 1, "500.00"));
        factura.getLineas().add(linea(3, producto("Gravado barato", "1234567890124", true), 3, "100.00"));
        factura.setTotal(new BigDecimal("2860.00"));
        ComprobanteElectronico aceptado = ComprobanteElectronico.builder().id(10L).claveNumerica("CLAVE-F")
                .estado(EstadoComprobante.ACEPTADO).vigente(true).fechaEmision(EMISION.minusDays(3))
                .xmlComprobante("<FacturaElectronica><ResumenFactura><TotalComprobante>2860.00</TotalComprobante>"
                        + "</ResumenFactura></FacturaElectronica>").build();
        when(facturas.findByIdParaActualizar(77)).thenReturn(Optional.of(factura));
        when(comprobantes.findFirstByFactura_IdFacturaAndVigenteTrue(77)).thenReturn(Optional.of(aceptado));
        when(notas.existsByFactura_IdFacturaAndEstadoIn(anyInt(), anyCollection())).thenReturn(false);
        when(notas.sumaTotalPorFactura(anyInt(), anyCollection())).thenReturn(BigDecimal.ZERO);
        when(pagos.findPagosValidosByFacturaId(77)).thenReturn(List.of());
        when(notas.saveAndFlush(any())).thenAnswer(inv -> {
            NotaFactura n = inv.getArgument(0);
            n.setId(5L);
            return n;
        });
        when(comprobanteService.generarYFirmarDesdeNota(any())).thenReturn(
                ComprobanteElectronico.builder().claveNumerica("CLAVE-NC").estado(EstadoComprobante.FIRMADO).build());
    }

    private NotaFactura emitir(String codigo, LineaAcreditar... lineas) {
        return servicio.emitirNotaCredito(77, TipoNotaFactura.CREDITO, codigo, "Corrección de monto",
                List.of(lineas), "admin");
    }

    private static LineaAcreditar acreditar(int linea, String monto) {
        return new LineaAcreditar(linea, new BigDecimal(monto));
    }

    private void yaAcreditado(int linea, String monto) {
        when(notas.acreditadoPorLinea(anyInt(), anyCollection()))
                .thenReturn(java.util.Collections.singletonList(new Object[] {linea, new BigDecimal(monto)}));
    }

    @Test
    @DisplayName("desglose: base e IVA del monto con la tarifa de la línea; precio = base/cantidad a 5 decimales; totales = suma")
    void desgloseYRedondeo() {
        // 1130.00 con 13 %: base 1000.00, IVA 130.00; 250.00 exento: base 250.00; 0.01 gravado: base 0.01, IVA 0.00
        NotaFactura nota = emitir("02", acreditar(1, "1130.00"), acreditar(2, "250.00"));

        assertThat(nota.getLineas()).hasSize(2);
        LineaNotaFactura l1 = nota.getLineas().get(0);
        assertThat(l1.getLineaFactura().getIdLineaFactura()).isEqualTo(1);
        assertThat(l1.getDescripcion()).isEqualTo("Gravado");
        assertThat(l1.getCodigoCabys()).isEqualTo("1234567890123");
        assertThat(l1.getCantidad()).isEqualByComparingTo("2");
        assertThat(l1.getTarifaIva()).isEqualByComparingTo("13");
        assertThat(l1.getSubtotal()).isEqualByComparingTo("1000.00");
        assertThat(l1.getMontoIva()).isEqualByComparingTo("130.00");
        assertThat(l1.getTotal()).isEqualByComparingTo("1130.00");
        assertThat(l1.getPrecioUnitario()).isEqualByComparingTo("500.00000");
        LineaNotaFactura l2 = nota.getLineas().get(1);
        assertThat(l2.getTarifaIva()).isEqualByComparingTo("0");
        assertThat(l2.getSubtotal()).isEqualByComparingTo("250.00");
        assertThat(l2.getMontoIva()).isEqualByComparingTo("0");
        assertThat(nota.getSubtotal()).isEqualByComparingTo("1250.00");
        assertThat(nota.getImpuesto()).isEqualByComparingTo("130.00");
        assertThat(nota.getTotal()).isEqualByComparingTo("1380.00");
        assertThat(nota.getEstado()).isEqualTo(EstadoNotaFactura.EN_TRAMITE);
        verify(comprobanteService).generarYFirmarDesdeNota(nota);
    }

    @Test
    @DisplayName("redondeo al céntimo: base + IVA da exactamente el monto pedido para cualquier monto, y el precio unitario es base/cantidad")
    void redondeoCuadraConElMonto() {
        for (String monto : new String[] {"0.01", "0.02", "1.13", "33.33", "99.99", "100.00"}) {
            NotaFactura nota = emitir("14", acreditar(3, monto));
            LineaNotaFactura l = nota.getLineas().get(0);
            assertThat(l.getSubtotal().add(l.getMontoIva())).as(monto).isEqualByComparingTo(monto);
            assertThat(l.getTotal()).as(monto).isEqualByComparingTo(monto);
            assertThat(nota.getTotal()).as(monto).isEqualByComparingTo(monto);
            assertThat(l.getPrecioUnitario()).isEqualByComparingTo(
                    l.getSubtotal().divide(new BigDecimal("3"), 5, java.math.RoundingMode.HALF_UP));
        }
    }

    @Test
    @DisplayName("tope por línea: justo el total de la línea pasa; un céntimo más → 409 sin guardar ni consecutivo")
    void topePorLinea() {
        assertThat(emitir("02", acreditar(1, "2260.00")).getTotal()).isEqualByComparingTo("2260.00");

        assertThatThrownBy(() -> emitir("02", acreditar(1, "2260.01")))
                .isInstanceOf(NotaFacturaConflictoException.class).hasMessage(NotaFacturaService.MENSAJE_TOPE_LINEA);
    }

    @Test
    @DisplayName("tope por línea con lo ya acreditado (APLICADAS y EN_TRAMITE): el disponible baja")
    void topePorLineaConAcreditado() {
        yaAcreditado(1, "2000.00");

        assertThat(emitir("14", acreditar(1, "260.00")).getTotal()).isEqualByComparingTo("260.00");
        assertThatThrownBy(() -> emitir("14", acreditar(1, "260.01")))
                .isInstanceOf(NotaFacturaConflictoException.class).hasMessageContaining("disponible");
    }

    @Test
    @DisplayName("tope por factura: la suma con lo acreditado alcanza el TotalComprobante pasa; un céntimo más → 409")
    void topePorFactura() {
        when(notas.sumaTotalPorFactura(anyInt(), anyCollection())).thenReturn(new BigDecimal("2859.00"));
        assertThat(emitir("02", acreditar(2, "1.00")).getTotal()).isEqualByComparingTo("1.00");

        assertThatThrownBy(() -> emitir("02", acreditar(2, "1.01")))
                .isInstanceOf(NotaFacturaConflictoException.class)
                .hasMessage(NotaFacturaService.MENSAJE_TOPE_FACTURA);
    }

    @Test
    @DisplayName("la línea debe pertenecer a la factura → 409 sin nota ni comprobante")
    void lineaAjena() {
        assertThatThrownBy(() -> emitir("02", acreditar(999, "10.00")))
                .isInstanceOf(NotaFacturaConflictoException.class).hasMessage(NotaFacturaService.MENSAJE_LINEA_AJENA);
        verify(notas, never()).saveAndFlush(any());
        verify(comprobanteService, never()).generarYFirmarDesdeNota(any());
    }

    @Test
    @DisplayName("un rechazo de tope no guarda la nota ni toma el consecutivo")
    void rechazoSinConsecutivo() {
        assertThatThrownBy(() -> emitir("02", acreditar(2, "500.01"))).isInstanceOf(NotaFacturaConflictoException.class);
        verify(notas, never()).saveAndFlush(any());
        verify(comprobanteService, never()).generarYFirmarDesdeNota(any());
    }

    @Test
    @DisplayName("nota total (01, 06 sin líneas o 13) con notas parciales aplicadas o en trámite → 409 con el mensaje fijo")
    void totalTrasParcial() {
        when(notas.existsByFactura_IdFacturaAndEstadoInAndModalidad(anyInt(), anyCollection(),
                org.mockito.ArgumentMatchers.eq(api.astro.whats_orders_manager.modules.facturacion.enums.ModalidadNota.PARCIAL)))
                .thenReturn(true);
        for (String codigo : new String[] {"01", "06", "13"}) {
            assertThatThrownBy(() -> servicio.emitirNotaCredito(77, TipoNotaFactura.CREDITO, codigo, "Anulación",
                    null, "admin"))
                    .isInstanceOf(NotaFacturaConflictoException.class)
                    .hasMessage("La factura tiene notas parciales; acredite el resto con una nota parcial");
        }
        verify(notas, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("una nota parcial en trámite bloquea otra nota; factura acreditada totalmente o anulada → 409")
    void estadosQueBloquean() {
        when(notas.existsByFactura_IdFacturaAndEstadoIn(anyInt(), anyCollection())).thenReturn(true);
        assertThatThrownBy(() -> emitir("02", acreditar(1, "10.00")))
                .isInstanceOf(NotaFacturaConflictoException.class).hasMessageContaining("en trámite");

        when(notas.existsByFactura_IdFacturaAndEstadoIn(anyInt(), anyCollection())).thenReturn(false);
        factura.setAcreditadaTotalmente(true);
        assertThatThrownBy(() -> emitir("02", acreditar(1, "10.00")))
                .isInstanceOf(NotaFacturaConflictoException.class).hasMessage(NotaFacturaService.MENSAJE_ACREDITADA_TOTALMENTE);

        factura.setAcreditadaTotalmente(false);
        factura.setAnulada(true);
        assertThatThrownBy(() -> emitir("02", acreditar(1, "10.00")))
                .isInstanceOf(NotaFacturaConflictoException.class).hasMessageContaining("anulada");
    }

    @Test
    @DisplayName("T-048: una factura con pagos admite la nota, también la parcial (se levanta el 409 de T-032)")
    void conPagos() {
        when(pagos.findPagosValidosByFacturaId(77)).thenReturn(List.of(new api.astro.whats_orders_manager.modules.facturacion.model.Pago()));

        NotaFactura nota = emitir("02", acreditar(1, "10.00"));

        assertThat(nota.getEstado()).isEqualTo(api.astro.whats_orders_manager.modules.facturacion.enums.EstadoNotaFactura.EN_TRAMITE);
        verify(notas).saveAndFlush(any());
    }

    @Test
    @DisplayName("la nota parcial guarda la referencia a la línea y conserva el código y la fecha contable (ADR-050)")
    void atribucion() {
        ArgumentCaptor<NotaFactura> captor = ArgumentCaptor.forClass(NotaFactura.class);
        emitir("14", acreditar(1, "113.00"));
        verify(notas).saveAndFlush(captor.capture());
        NotaFactura nota = captor.getValue();
        assertThat(nota.getCodigoReferencia()).isEqualTo("14");
        assertThat(nota.esParcial()).isTrue();
        assertThat(nota.getFechaContable()).isEqualTo(EMISION.toLocalDate());
        // 14: mes de la factura original (ORIGINAL), 02 más abajo: mes de la nota
        assertThat(nota.getPeriodoFiscalAtribucion()).isEqualTo("2026-11");
    }

    private static Producto producto(String descripcion, String cabys, boolean gravado) {
        Producto p = new Producto();
        p.setDescripcion(descripcion);
        ArticuloMaestro maestro = new ArticuloMaestro();
        maestro.setDescripcion(descripcion);
        maestro.setCodigoCabys(cabys);
        p.setArticuloMaestro(maestro);
        p.setGravado(gravado);
        p.setPorcentajeImpuesto(gravado ? new BigDecimal("13") : BigDecimal.ZERO);
        p.setPrecioIncluyeIva(true);
        return p;
    }

    private LineaFactura linea(int numero, Producto producto, int cantidad, String subtotal) {
        LineaFactura linea = new LineaFactura();
        linea.setIdLineaFactura(numero);
        linea.setNumeroLinea(numero);
        linea.setFactura(factura);
        linea.setProducto(producto);
        linea.setCantidad(cantidad);
        linea.setSubtotal(new BigDecimal(subtotal));
        linea.setPrecioUnitario(new BigDecimal(subtotal).divide(BigDecimal.valueOf(cantidad), 5,
                java.math.RoundingMode.HALF_UP));
        return linea;
    }
}
