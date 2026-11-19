package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.electronica.service.AmbienteComprobanteGuard;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.enums.TipoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.NotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.repository.NotaFacturaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** T-039 (criterios 2 y 3): regla única de elegibilidad de la factura sustituta (ADR-045 §1 y §3). */
@DisplayName("T-039 — elegibilidad de la factura sustituta")
class SustitucionElegibilidadTest {

    private NotaFacturaRepository notas;
    private ComprobanteElectronicoRepository comprobantes;
    private FacturaRepository facturas;
    private SustitucionElegibilidad regla;

    private Factura original;
    private ComprobanteElectronico origen;
    private NotaFactura nota;
    private ComprobanteElectronico comprobanteNota;

    @BeforeEach
    void setUp() {
        notas = mock(NotaFacturaRepository.class);
        comprobantes = mock(ComprobanteElectronicoRepository.class);
        facturas = mock(FacturaRepository.class);
        regla = new SustitucionElegibilidad(notas, comprobantes, facturas, mock(AmbienteComprobanteGuard.class));

        original = new Factura();
        original.setIdFactura(5);
        original.setNumeroFactura("FA01-00005");
        original.setAnulada(true);
        origen = ComprobanteElectronico.builder().id(1L).factura(original).estado(EstadoComprobante.ACEPTADO)
                .claveNumerica("6".repeat(50)).consecutivo("00100001010000000005")
                .fechaEmision(LocalDateTime.of(2026, 10, 1, 8, 0)).vigente(true).build();
        nota = NotaFactura.builder().id(9L).factura(original).tipo(TipoNotaFactura.CREDITO).codigoReferencia("13")
                .estado(EstadoNotaFactura.APLICADA).comprobanteReferencia(origen).build();
        comprobanteNota = ComprobanteElectronico.builder().id(2L).estado(EstadoComprobante.ACEPTADO)
                .consecutivo("00100001030000000003").vigente(true).build();
        when(notas.findByFactura_IdFacturaOrderByIdAsc(5)).thenReturn(List.of(nota));
        when(comprobantes.findFirstByNotaFactura_IdAndVigenteTrue(9L)).thenReturn(Optional.of(comprobanteNota));
        when(facturas.findFirstByFacturaSustituida_IdFacturaAndAnuladaFalse(anyInt())).thenReturn(Optional.empty());
    }

    @ParameterizedTest(name = "nota {0} → sustituta {1}")
    @CsvSource({"01,07", "13,15"})
    @DisplayName("procede y deduce el código de la sustituta de la nota: 01→07, 13→15")
    void procede_yDeduceElCodigo(String codigoNota, String codigoSustituta) {
        nota.setCodigoReferencia(codigoNota);

        var e = regla.evaluar(original);

        assertThat(e.elegible()).isTrue();
        assertThat(e.codigoSustitucion()).isEqualTo(codigoSustituta);
        assertThat(e.numeroNota()).isEqualTo("00100001030000000003");
        assertThat(e.comprobanteOriginal()).isSameAs(origen);
        assertThat(regla.puedeEmitirSustituta(original)).isTrue();
    }

    @Test
    @DisplayName("sin anular → 409 fijo")
    void sinAnular() {
        original.setAnulada(false);

        var e = regla.evaluar(original);

        assertThat(e.elegible()).isFalse();
        assertThat(e.mensaje()).isEqualTo(SustitucionElegibilidad.MENSAJE_NO_ANULADA);
    }

    @Test
    @DisplayName("nota en trámite o rechazada (no APLICADA) → no procede")
    void notaNoAplicada() {
        for (EstadoNotaFactura estado : List.of(EstadoNotaFactura.EN_TRAMITE, EstadoNotaFactura.RECHAZADA)) {
            nota.setEstado(estado);
            assertThat(regla.evaluar(original).mensaje()).isEqualTo(SustitucionElegibilidad.MENSAJE_NO_ANULADA);
        }
    }

    @Test
    @DisplayName("anulada sin nota de crédito (anulación manual de una factura sin comprobante) → no procede")
    void sinNota() {
        when(notas.findByFactura_IdFacturaOrderByIdAsc(5)).thenReturn(List.of());

        assertThat(regla.evaluar(original).mensaje()).isEqualTo(SustitucionElegibilidad.MENSAJE_NO_ANULADA);
    }

    @Test
    @DisplayName("nota con otro código (07, 15, 16) no cuenta como anulación total")
    void notaConOtroCodigo() {
        nota.setCodigoReferencia("16");

        assertThat(regla.evaluar(original).elegible()).isFalse();
    }

    @Test
    @DisplayName("el comprobante de la nota no está ACEPTADO → no procede")
    void comprobanteDeLaNotaNoAceptado() {
        comprobanteNota.setEstado(EstadoComprobante.ENVIADO);

        assertThat(regla.evaluar(original).mensaje()).isEqualTo(SustitucionElegibilidad.MENSAJE_NOTA_NO_ACEPTADA);

        when(comprobantes.findFirstByNotaFactura_IdAndVigenteTrue(anyLong())).thenReturn(Optional.empty());
        assertThat(regla.evaluar(original).mensaje()).isEqualTo(SustitucionElegibilidad.MENSAJE_NOTA_NO_ACEPTADA);
    }

    @Test
    @DisplayName("el comprobante de origen no está ACEPTADO o no tiene clave → no procede")
    void origenNoAceptadoOSinClave() {
        origen.setEstado(EstadoComprobante.RECHAZADO);
        assertThat(regla.evaluar(original).mensaje()).isEqualTo(SustitucionElegibilidad.MENSAJE_ORIGEN_NO_ACEPTADO);

        origen.setEstado(EstadoComprobante.ACEPTADO);
        origen.setClaveNumerica(" ");
        assertThat(regla.evaluar(original).mensaje()).isEqualTo(SustitucionElegibilidad.MENSAJE_ORIGEN_NO_ACEPTADO);
    }

    @Test
    @DisplayName("sin comprobante de referencia en la nota, usa el ACEPTADO de la factura")
    void origenPorLaFactura() {
        nota.setComprobanteReferencia(null);
        when(comprobantes.findByFactura_IdFacturaOrderByIdAsc(5)).thenReturn(List.of(origen));

        assertThat(regla.evaluar(original).elegible()).isTrue();
    }

    @Test
    @DisplayName("ya existe una sustituta no anulada → «La factura ya tiene una sustituta: FA…»; la etapa a la ignora")
    void yaTieneSustituta() {
        Factura existente = new Factura();
        existente.setIdFactura(6);
        existente.setNumeroFactura("FA01-00006");
        when(facturas.findFirstByFacturaSustituida_IdFacturaAndAnuladaFalse(5)).thenReturn(Optional.of(existente));

        var e = regla.evaluar(original);

        assertThat(e.elegible()).isFalse();
        assertThat(e.mensaje()).isEqualTo("La factura ya tiene una sustituta: FA01-00006");
        assertThat(regla.evaluarSinSustituta(original).elegible()).isTrue();
    }

    @Test
    @DisplayName("número de la nota: consecutivo de su comprobante; sin él, «NC-» y el id de la nota")
    void numeroDeNota() {
        assertThat(SustitucionElegibilidad.numeroDeNota(nota, comprobanteNota)).isEqualTo("00100001030000000003");
        assertThat(SustitucionElegibilidad.numeroDeNota(nota, null)).isEqualTo("NC-9");
        comprobanteNota.setConsecutivo(" ");
        assertThat(SustitucionElegibilidad.numeroDeNota(nota, comprobanteNota)).isEqualTo("NC-9");
    }
}
