package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.dto.EmitirNotaCreditoRequest.LineaAcreditar;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.AmbienteComprobanteGuard;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ComprobanteElectronicoService;
import api.astro.whats_orders_manager.modules.facturacion.enums.TipoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.exception.NotaFacturaConflictoException;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.NotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.repository.NotaFacturaRepository;
import api.astro.whats_orders_manager.modules.producto.model.ArticuloMaestro;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.regimen.enums.Regimen;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
 * T-057 (ADR-073 §1; criterio 6; P-132 casos 4 y 5): una nota de crédito sobre una factura SIMPLIFICADO se rechaza (409)
 * porque en el régimen simplificado se corrige anulando; sobre una GENERAL de diciembre, emitida en enero, el IVA se calcula
 * con el régimen general (no 0) y el período atribuido sigue las reglas de ADR-050.
 */
@DisplayName("T-057 — notas de crédito según el régimen sellado de la factura")
class NotaFacturaRegimenTest {

    private static final ZoneId CR = ZoneId.of("America/Costa_Rica");
    private static final LocalDateTime EMISION_NOTA = LocalDateTime.of(2027, 1, 10, 10, 0);
    private static final LocalDateTime EMISION_FACTURA = LocalDateTime.of(2026, 12, 20, 15, 0);

    private final FacturaRepository facturas = mock(FacturaRepository.class);
    private final NotaFacturaRepository notas = mock(NotaFacturaRepository.class);
    private final ComprobanteElectronicoRepository comprobantes = mock(ComprobanteElectronicoRepository.class);
    private final ComprobanteElectronicoService comprobanteService = mock(ComprobanteElectronicoService.class);
    private final CalculadoraImpuestoService calculadora = new CalculadoraImpuestoService(null);
    private final NotaFacturaService servicio = new NotaFacturaService(facturas, notas, comprobantes, comprobanteService,
            mock(api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService.class), calculadora,
            Clock.fixed(EMISION_NOTA.atZone(CR).toInstant(), CR), mock(AmbienteComprobanteGuard.class),
            new AtribucionFiscalService("ORIGINAL"), new NotaCreditoTopes(notas, calculadora));

    private Factura factura;

    @BeforeEach
    void setUp() {
        factura = new Factura();
        factura.setIdFactura(77);
        factura.setNumeroFactura("F-77");
        factura.setMonedaFE(MonedaFE.CRC);
        factura.setTipoCambio(BigDecimal.ONE);
        factura.setAnulada(false);
        factura.setRegimenEmisor(Regimen.GENERAL);
        factura.getLineas().add(linea(1, 2, "2260.00"));
        factura.setTotal(new BigDecimal("2260.00"));
        ComprobanteElectronico aceptado = ComprobanteElectronico.builder().id(10L).claveNumerica("CLAVE-F")
                .estado(EstadoComprobante.ACEPTADO).vigente(true).fechaEmision(EMISION_FACTURA)
                .xmlComprobante("<FacturaElectronica><ResumenFactura><TotalComprobante>2260.00</TotalComprobante>"
                        + "</ResumenFactura></FacturaElectronica>").build();
        when(facturas.findByIdParaActualizar(77)).thenReturn(Optional.of(factura));
        when(comprobantes.findFirstByFactura_IdFacturaAndVigenteTrue(77)).thenReturn(Optional.of(aceptado));
        when(notas.existsByFactura_IdFacturaAndEstadoIn(anyInt(), anyCollection())).thenReturn(false);
        when(notas.sumaTotalPorFactura(anyInt(), anyCollection())).thenReturn(BigDecimal.ZERO);
        when(notas.saveAndFlush(any())).thenAnswer(inv -> {
            NotaFactura n = inv.getArgument(0);
            n.setId(5L);
            return n;
        });
        when(comprobanteService.generarYFirmarDesdeNota(any())).thenReturn(
                ComprobanteElectronico.builder().claveNumerica("CLAVE-NC").estado(EstadoComprobante.FIRMADO).build());
    }

    @Test
    @DisplayName("criterio 6: nota parcial o total sobre una factura SIMPLIFICADO → 409 con el mensaje fijo, sin nota ni consecutivo")
    void simplificado_409() {
        factura.setRegimenEmisor(Regimen.SIMPLIFICADO);

        assertThatThrownBy(() -> servicio.emitirNotaCredito(77, TipoNotaFactura.CREDITO, "02", "Corrección de monto",
                List.of(new LineaAcreditar(1, new BigDecimal("100.00"))), "admin"))
                .isInstanceOf(NotaFacturaConflictoException.class)
                .hasMessage(NotaFacturaService.MENSAJE_REGIMEN_SIMPLIFICADO);
        assertThatThrownBy(() -> servicio.emitirNotaCredito(77, TipoNotaFactura.CREDITO, "13", "Anulación", null, "admin"))
                .isInstanceOf(NotaFacturaConflictoException.class)
                .hasMessage(NotaFacturaService.MENSAJE_REGIMEN_SIMPLIFICADO);
        assertThat(NotaFacturaService.MENSAJE_REGIMEN_SIMPLIFICADO).contains("régimen simplificado").contains("anul");
        verify(notas, never()).saveAndFlush(any());
        verify(comprobanteService, never()).generarYFirmarDesdeNota(any());
    }

    @Test
    @DisplayName("caso 4: nota parcial 02 el 10-01-2027 sobre una GENERAL de diciembre → IVA 13 % (no 0) y período 2027-01")
    void general_notaParcialEnEnero_ivaGeneral_periodoDeLaNota() {
        NotaFactura nota = servicio.emitirNotaCredito(77, TipoNotaFactura.CREDITO, "02", "Corrección de monto",
                List.of(new LineaAcreditar(1, new BigDecimal("1130.00"))), "admin");

        assertThat(nota.getLineas().get(0).getTarifaIva()).isEqualByComparingTo("13");
        assertThat(nota.getLineas().get(0).getMontoIva()).isEqualByComparingTo("130.00");
        assertThat(nota.getImpuesto()).isEqualByComparingTo("130.00");
        assertThat(nota.getPeriodoFiscalAtribucion()).isEqualTo("2027-01");
    }

    @Test
    @DisplayName("caso 5: nota 13 el 10-01-2027 sobre la misma factura → período atribuido 2026-12 (mes de la original)")
    void general_nota13EnEnero_periodoDeLaOriginal() {
        NotaFactura nota = servicio.emitirNotaCredito(77, TipoNotaFactura.CREDITO, "13", "Anulación", null, "admin");

        assertThat(nota.getPeriodoFiscalAtribucion()).isEqualTo("2026-12");
        assertThat(nota.getImpuesto()).isEqualByComparingTo("260.00");
    }

    @SuppressWarnings("deprecation")
    private LineaFactura linea(int numero, int cantidad, String subtotal) {
        Producto p = new Producto();
        p.setDescripcion("Gravado");
        ArticuloMaestro maestro = new ArticuloMaestro();
        maestro.setDescripcion("Gravado");
        maestro.setCodigoCabys("1234567890123");
        p.setArticuloMaestro(maestro);
        p.setGravado(true);
        p.setPorcentajeImpuesto(new BigDecimal("13"));
        p.setPrecioIncluyeIva(true);
        LineaFactura l = new LineaFactura();
        l.setIdLineaFactura(numero);
        l.setNumeroLinea(numero);
        l.setFactura(factura);
        l.setProducto(p);
        l.setCantidad(cantidad);
        l.setSubtotal(new BigDecimal(subtotal));
        l.setPrecioUnitario(new BigDecimal(subtotal).divide(BigDecimal.valueOf(cantidad), 5, java.math.RoundingMode.HALF_UP));
        return l;
    }
}
