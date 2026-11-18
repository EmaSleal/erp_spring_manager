package api.astro.whats_orders_manager.modules.facturacion.service.impl;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.model.Empresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.CuentaBancariaRepository;
import api.astro.whats_orders_manager.modules.configuracion.service.ConfiguracionEmpresaService;
import api.astro.whats_orders_manager.modules.configuracion.service.EmpresaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.enums.TipoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.NotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.repository.NotaFacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.service.FacturaService;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** T-039 (ADR-045 §6, P-030): el PDF presenta título, campos fiscales y leyendas según el comprobante vigente. */
@DisplayName("T-039 — PDF de la factura según el comprobante vigente")
class FacturaPdfSegunComprobanteTest {

    private static final String CLAVE = "50614112600310123456700100001010000000007100000001";
    private static final String CONSECUTIVO = "00100001010000000007";
    private static final String RESOLUCION = "MH-DGT-RES-0027-2024";

    private final NotaFacturaRepository notas = mock(NotaFacturaRepository.class);
    private final ComprobanteElectronicoRepository comprobantes = mock(ComprobanteElectronicoRepository.class);

    // ── Decisión pura ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("decisión pura: una fila por estado")
    void decisionPorEstado() {
        TipoComprobanteElectronico fe = TipoComprobanteElectronico.FACTURA_ELECTRONICA;
        for (EstadoComprobante e : List.of(EstadoComprobante.GENERADO, EstadoComprobante.FIRMADO,
                EstadoComprobante.ERROR, EstadoComprobante.NO_TRANSMITIDO, EstadoComprobante.ANULADO)) {
            assertThat(PdfFiscalDecision.de(e, fe))
                    .isEqualTo(new PdfFiscalDecision("Documento no fiscal (proforma)", false, false, false));
        }
        assertThat(PdfFiscalDecision.de(null, null).titulo()).isEqualTo("Documento no fiscal (proforma)");
        for (EstadoComprobante e : List.of(EstadoComprobante.ENVIADO, EstadoComprobante.PROCESANDO,
                EstadoComprobante.ENVIO_INCIERTO, EstadoComprobante.ERROR_HACIENDA)) {
            assertThat(PdfFiscalDecision.de(e, fe))
                    .isEqualTo(new PdfFiscalDecision("Factura electrónica", true, true, true));
        }
        assertThat(PdfFiscalDecision.de(EstadoComprobante.ACEPTADO, fe))
                .isEqualTo(new PdfFiscalDecision("Factura electrónica", true, false, true));
        assertThat(PdfFiscalDecision.de(EstadoComprobante.ACEPTADO, TipoComprobanteElectronico.TIQUETE_ELECTRONICO)
                .titulo()).isEqualTo("Tiquete electrónico");
        assertThat(PdfFiscalDecision.de(EstadoComprobante.RECHAZADO, fe))
                .isEqualTo(new PdfFiscalDecision("Documento no fiscal (rechazado por Hacienda)", false, false, false));
    }

    // ── Texto del PDF, una prueba por fila de la tabla ───────────────────────────────

    @Test
    @DisplayName("sin comprobante → proforma, sin clave ni consecutivo ni resolución")
    void sinComprobante() throws Exception {
        String t = pdf(factura(1, null, false));
        assertThat(t).contains("Documento no fiscal (proforma)").contains("FA01-00001")
                .doesNotContain("CLAVE").doesNotContain("NÚMERO CONSECUTIVO").doesNotContain(RESOLUCION)
                .doesNotContain("En trámite");
    }

    @ParameterizedTest(name = "{0} → proforma")
    @EnumSource(value = EstadoComprobante.class, names = {"GENERADO", "FIRMADO", "ERROR", "NO_TRANSMITIDO"})
    @DisplayName("GENERADO, FIRMADO, ERROR, NO_TRANSMITIDO → proforma")
    void estadosNoFiscales(EstadoComprobante estado) throws Exception {
        String t = pdf(factura(1, comprobante(estado, TipoComprobanteElectronico.FACTURA_ELECTRONICA), false));
        assertThat(t).contains("Documento no fiscal (proforma)")
                .doesNotContain(CLAVE).doesNotContain("NÚMERO CONSECUTIVO").doesNotContain(RESOLUCION);
    }

    @ParameterizedTest(name = "{0} → en trámite")
    @EnumSource(value = EstadoComprobante.class, names = {"ENVIADO", "PROCESANDO", "ENVIO_INCIERTO", "ERROR_HACIENDA"})
    @DisplayName("ENVIADO, PROCESANDO, ENVIO_INCIERTO (y ERROR_HACIENDA) → Factura electrónica en trámite")
    void enTramite(EstadoComprobante estado) throws Exception {
        String t = pdf(factura(1, comprobante(estado, TipoComprobanteElectronico.FACTURA_ELECTRONICA), false));
        assertThat(t).contains("Factura electrónica").contains("En trámite ante Hacienda")
                .contains(CLAVE).contains(CONSECUTIVO).contains(RESOLUCION)
                .doesNotContain("Documento no fiscal");
    }

    @Test
    @DisplayName("ACEPTADO → Factura electrónica con clave y consecutivo, sin leyenda de trámite")
    void aceptado() throws Exception {
        String t = pdf(factura(1, comprobante(EstadoComprobante.ACEPTADO,
                TipoComprobanteElectronico.FACTURA_ELECTRONICA), false));
        assertThat(t).contains("Factura electrónica").contains(CLAVE).contains(CONSECUTIVO).contains(RESOLUCION)
                .doesNotContain("En trámite").doesNotContain("Documento no fiscal").doesNotContain("ANULADA");
    }

    @Test
    @DisplayName("ACEPTADO de tipo 04 → Tiquete electrónico")
    void tiquete() throws Exception {
        String t = pdf(factura(1, comprobante(EstadoComprobante.ACEPTADO,
                TipoComprobanteElectronico.TIQUETE_ELECTRONICO), false));
        assertThat(t).contains("Tiquete electrónico").contains(CLAVE).doesNotContain("Factura electrónica");
    }

    @Test
    @DisplayName("RECHAZADO → no fiscal (rechazado por Hacienda), sin clave ni consecutivo")
    void rechazado() throws Exception {
        String t = pdf(factura(1, comprobante(EstadoComprobante.RECHAZADO,
                TipoComprobanteElectronico.FACTURA_ELECTRONICA), false));
        // el título largo se parte en dos renglones dentro de la columna: se verifican las dos mitades
        assertThat(t).contains("Documento no fiscal (rechazado por").contains("Hacienda)")
                .doesNotContain("Documento no fiscal (proforma)")
                .doesNotContain(CLAVE).doesNotContain("NÚMERO CONSECUTIVO").doesNotContain(RESOLUCION);
    }

    @Test
    @DisplayName("comprobante ANULADO → proforma")
    void comprobanteAnulado() throws Exception {
        String t = pdf(factura(1, comprobante(EstadoComprobante.ANULADO,
                TipoComprobanteElectronico.FACTURA_ELECTRONICA), false));
        assertThat(t).contains("Documento no fiscal (proforma)").doesNotContain(CLAVE);
    }

    @Test
    @DisplayName("factura ANULADA con nota aplicada → marca «ANULADA por la nota <consecutivo de la nota>»")
    void anuladaPorNota() throws Exception {
        Factura f = factura(1, comprobante(EstadoComprobante.ACEPTADO, TipoComprobanteElectronico.FACTURA_ELECTRONICA),
                true);
        NotaFactura nota = NotaFactura.builder().id(5L).factura(f).tipo(TipoNotaFactura.CREDITO)
                .estado(EstadoNotaFactura.APLICADA).build();
        ComprobanteElectronico cNota = ComprobanteElectronico.builder().id(9L).consecutivo("00100001030000000002")
                .estado(EstadoComprobante.ACEPTADO).vigente(true).build();
        when(notas.findByFactura_IdFacturaOrderByIdAsc(1)).thenReturn(List.of(nota));
        when(comprobantes.findFirstByNotaFactura_IdAndVigenteTrue(5L)).thenReturn(Optional.of(cNota));

        String t = pdf(f);
        assertThat(t).contains("ANULADA por la nota 00100001030000000002").contains(CLAVE);
    }

    @Test
    @DisplayName("factura ANULADA sin comprobante de la nota → número «NC-<id>»")
    void anuladaNotaSinComprobante() throws Exception {
        Factura f = factura(1, comprobante(EstadoComprobante.ACEPTADO, TipoComprobanteElectronico.FACTURA_ELECTRONICA),
                true);
        NotaFactura nota = NotaFactura.builder().id(5L).factura(f).tipo(TipoNotaFactura.CREDITO)
                .estado(EstadoNotaFactura.APLICADA).build();
        when(notas.findByFactura_IdFacturaOrderByIdAsc(1)).thenReturn(List.of(nota));

        assertThat(pdf(f)).contains("ANULADA por la nota NC-5");
    }

    @Test
    @DisplayName("sustituta → «Sustituye a <número de la original>»")
    void sustituta() throws Exception {
        Factura original = factura(1, null, true);
        original.setNumeroFactura("FA01-00001");
        Factura sustituta = factura(2, comprobante(EstadoComprobante.ACEPTADO,
                TipoComprobanteElectronico.FACTURA_ELECTRONICA), false);
        sustituta.setNumeroFactura("FA01-00002");
        sustituta.setFacturaSustituida(original);

        assertThat(pdf(sustituta)).contains("Sustituye a FA01-00001").doesNotContain("ANULADA");
    }

    // ── Apoyo ───────────────────────────────────────────────────────────────────────

    private static ComprobanteElectronico comprobante(EstadoComprobante estado, TipoComprobanteElectronico tipo) {
        return ComprobanteElectronico.builder().id(10L).estado(estado).tipoComprobante(tipo).vigente(true)
                .claveNumerica(CLAVE).consecutivo(CONSECUTIVO).fechaEmision(LocalDateTime.of(2026, 11, 13, 10, 0))
                .build();
    }

    private static Factura factura(int id, ComprobanteElectronico vigente, boolean anulada) {
        Cliente cliente = new Cliente();
        cliente.setIdCliente(1);
        cliente.setNombre("Cliente de prueba");
        Factura f = new Factura();
        f.setIdFactura(id);
        f.setNumeroFactura(String.format("FA01-%05d", id));
        f.setCliente(cliente);
        f.setSubtotal(new BigDecimal("100.00"));
        f.setTotal(new BigDecimal("113.00"));
        f.setMonedaFE(MonedaFE.CRC);
        f.setAnulada(anulada);
        f.setLineas(new ArrayList<>());
        if (vigente != null) {
            f.getComprobantes().add(vigente);
        }
        return f;
    }

    private String pdf(Factura factura) throws Exception {
        FacturaService facturaService = mock(FacturaService.class);
        EmpresaService empresaService = mock(EmpresaService.class);
        ConfiguracionEmpresaService configuracionService = mock(ConfiguracionEmpresaService.class);
        CuentaBancariaRepository cuentas = mock(CuentaBancariaRepository.class);
        FacturaPdfServiceImpl service = new FacturaPdfServiceImpl(facturaService, empresaService, configuracionService,
                cuentas, mock(UsuarioService.class), notas, comprobantes);
        when(facturaService.findById(factura.getIdFactura())).thenReturn(Optional.of(factura));
        Empresa empresa = new Empresa();
        empresa.setIdEmpresa(1);
        empresa.setNombreEmpresa("Monrachem");
        when(empresaService.getEmpresaPrincipal()).thenReturn(empresa);
        ConfiguracionEmpresa configuracion = new ConfiguracionEmpresa();
        configuracion.setRazonSocial("EMANUEL SOTO LEAL");
        configuracion.setNumeroIdentificacion("3101234567");
        when(configuracionService.obtenerOCrearConfiguracion()).thenReturn(configuracion);
        when(cuentas.findByEmpresaIdEmpresaAndActivaTrue(1)).thenReturn(List.of());

        byte[] bytes = service.generarPdfFactura(factura.getIdFactura());
        StringBuilder sb = new StringBuilder();
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(bytes)))) {
            for (int i = 1; i <= doc.getNumberOfPages(); i++) {
                sb.append(PdfTextExtractor.getTextFromPage(doc.getPage(i))).append('\n');
            }
        }
        return sb.toString();
    }
}
