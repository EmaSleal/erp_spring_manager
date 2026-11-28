package api.astro.whats_orders_manager.modules.facturacion.service.impl;

import api.astro.whats_orders_manager.modules.facturacion.service.ConfiguracionFacturacionService;
import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.model.Empresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.CuentaBancariaRepository;
import api.astro.whats_orders_manager.modules.configuracion.service.ConfiguracionEmpresaService;
import api.astro.whats_orders_manager.modules.configuracion.service.EmpresaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.enums.TipoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaNotaFactura;
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
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** T-042 (criterio 11, ADR-045 §6): el PDF de la nota sigue las mismas reglas de estado que el de la factura. */
@DisplayName("T-042 — PDF de la nota de crédito según el comprobante vigente")
class NotaCreditoPdfSegunComprobanteTest {

    private static final String CLAVE = "50614112600310123456700100001030000000002100000001";
    private static final String CLAVE_FACTURA = "50613112600310123456700100001010000000007100000009";
    private static final String CONSECUTIVO = "00100001030000000002";
    private static final String RESOLUCION = "MH-DGT-RES-0027-2024";

    private final NotaFacturaRepository notas = mock(NotaFacturaRepository.class);
    private final ComprobanteElectronicoRepository comprobantes = mock(ComprobanteElectronicoRepository.class);

    @Test
    @DisplayName("decisión pura: una fila por estado, ERROR_HACIENDA como en trámite")
    void decisionPorEstado() {
        for (EstadoComprobante e : List.of(EstadoComprobante.GENERADO, EstadoComprobante.FIRMADO,
                EstadoComprobante.ERROR, EstadoComprobante.NO_TRANSMITIDO, EstadoComprobante.ANULADO)) {
            assertThat(PdfFiscalDecision.deNota(e))
                    .isEqualTo(new PdfFiscalDecision("Documento no fiscal", false, false, false));
        }
        assertThat(PdfFiscalDecision.deNota(null).titulo()).isEqualTo("Documento no fiscal");
        for (EstadoComprobante e : List.of(EstadoComprobante.ENVIADO, EstadoComprobante.PROCESANDO,
                EstadoComprobante.ENVIO_INCIERTO, EstadoComprobante.ERROR_HACIENDA)) {
            assertThat(PdfFiscalDecision.deNota(e))
                    .isEqualTo(new PdfFiscalDecision("Nota de crédito electrónica", true, true, true));
        }
        assertThat(PdfFiscalDecision.deNota(EstadoComprobante.ACEPTADO))
                .isEqualTo(new PdfFiscalDecision("Nota de crédito electrónica", true, false, true));
        assertThat(PdfFiscalDecision.deNota(EstadoComprobante.RECHAZADO))
                .isEqualTo(new PdfFiscalDecision("Documento no fiscal (rechazado por Hacienda)", false, false, false));
    }

    @Test
    @DisplayName("sin comprobante → no fiscal, sin clave ni consecutivo ni resolución; lleva la referencia")
    void sinComprobante() throws Exception {
        String t = pdf(nota(), null);
        assertThat(t).contains("Documento no fiscal").contains("NC-5")
                .doesNotContain("CLAVE").doesNotContain("NÚMERO CONSECUTIVO").doesNotContain(RESOLUCION)
                .doesNotContain("En trámite").doesNotContain("Nota de crédito electrónica");
        assertReferencia(t);
    }

    @ParameterizedTest(name = "{0} → no fiscal")
    @EnumSource(value = EstadoComprobante.class, names = {"GENERADO", "FIRMADO", "ERROR", "NO_TRANSMITIDO"})
    @DisplayName("GENERADO, FIRMADO, ERROR, NO_TRANSMITIDO → Documento no fiscal")
    void estadosNoFiscales(EstadoComprobante estado) throws Exception {
        String t = pdf(nota(), comprobante(estado));
        assertThat(t).contains("Documento no fiscal")
                .doesNotContain(CLAVE).doesNotContain(CONSECUTIVO).doesNotContain("NÚMERO CONSECUTIVO")
                .doesNotContain(RESOLUCION).doesNotContain("En trámite");
        assertReferencia(t);
    }

    @ParameterizedTest(name = "{0} → en trámite")
    @EnumSource(value = EstadoComprobante.class, names = {"ENVIADO", "PROCESANDO", "ENVIO_INCIERTO", "ERROR_HACIENDA"})
    @DisplayName("ENVIADO, PROCESANDO, ENVIO_INCIERTO (y ERROR_HACIENDA) → en trámite ante Hacienda")
    void enTramite(EstadoComprobante estado) throws Exception {
        String t = pdf(nota(), comprobante(estado));
        assertThat(t).contains("Nota de crédito electrónica").contains("En trámite ante Hacienda")
                .contains(CLAVE).contains(CONSECUTIVO).contains(RESOLUCION)
                .doesNotContain("Documento no fiscal");
        assertReferencia(t);
    }

    @Test
    @DisplayName("ACEPTADO → Nota de crédito electrónica con clave, consecutivo y leyenda de la resolución")
    void aceptado() throws Exception {
        String t = pdf(nota(), comprobante(EstadoComprobante.ACEPTADO));
        assertThat(t).contains("Nota de crédito electrónica").contains(CLAVE).contains(CONSECUTIVO)
                .contains(RESOLUCION).doesNotContain("En trámite").doesNotContain("Documento no fiscal");
        assertReferencia(t);
        // las líneas y el cliente vienen de la nota y de su factura
        assertThat(t).contains("Cliente de prueba").contains("Producto A").contains("1234567890123");
    }

    @Test
    @DisplayName("RECHAZADO → no fiscal (rechazado por Hacienda), sin clave ni consecutivo")
    void rechazado() throws Exception {
        String t = pdf(nota(), comprobante(EstadoComprobante.RECHAZADO));
        // el título largo se parte en dos renglones dentro de la columna: se verifican las dos mitades
        assertThat(t).contains("Documento no fiscal (rechazado por").contains("Hacienda)")
                .doesNotContain("Nota de crédito electrónica")
                .doesNotContain(CLAVE).doesNotContain("NÚMERO CONSECUTIVO").doesNotContain(RESOLUCION);
        assertReferencia(t);
    }

    @Test
    @DisplayName("nota inexistente → NoSuchElementException")
    void inexistente() {
        when(notas.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio().generarPdfNotaCredito(99L)).isInstanceOf(NoSuchElementException.class);
    }

    /** El bloque «Referencia»: factura, clave de la factura, código y nombre, razón y fecha de la factura. */
    private static void assertReferencia(String t) {
        assertThat(t).contains("Referencia").contains("FA01-00007").contains(CLAVE_FACTURA)
                .contains("01").contains("Anula documento de referencia").contains("Producto devuelto por el cliente")
                .contains("10-11-2026");
    }

    // ── Apoyo ───────────────────────────────────────────────────────────────────────

    private static ComprobanteElectronico comprobante(EstadoComprobante estado) {
        return ComprobanteElectronico.builder().id(10L).estado(estado)
                .tipoComprobante(TipoComprobanteElectronico.NOTA_CREDITO).vigente(true)
                .claveNumerica(CLAVE).consecutivo(CONSECUTIVO).fechaEmision(LocalDateTime.of(2026, 11, 13, 10, 0))
                .build();
    }

    private static NotaFactura nota() {
        Cliente cliente = new Cliente();
        cliente.setIdCliente(1);
        cliente.setNombre("Cliente de prueba");
        Factura factura = new Factura();
        factura.setIdFactura(7);
        factura.setNumeroFactura("FA01-00007");
        factura.setCliente(cliente);
        NotaFactura nota = NotaFactura.builder().id(5L).factura(factura).tipo(TipoNotaFactura.CREDITO)
                .codigoReferencia("01").razon("Producto devuelto por el cliente").estado(EstadoNotaFactura.EN_TRAMITE)
                .fechaEmision(LocalDateTime.of(2026, 11, 13, 10, 0)).moneda("CRC")
                .tipoCambio(BigDecimal.ONE).subtotal(new BigDecimal("100.00")).descuento(BigDecimal.ZERO)
                .impuesto(new BigDecimal("13.00")).total(new BigDecimal("113.00")).usuarioCreador("gerente")
                .claveReferencia(CLAVE_FACTURA).fechaEmisionReferencia(LocalDateTime.of(2026, 11, 10, 9, 0))
                .lineas(new ArrayList<>()).build();
        nota.agregarLinea(LineaNotaFactura.builder().numeroLinea(1).descripcion("Producto A")
                .codigoCabys("1234567890123").unidadMedida("Unid").cantidad(BigDecimal.ONE)
                .precioUnitario(new BigDecimal("100.00")).descuento(BigDecimal.ZERO).tarifaIva(new BigDecimal("13.00"))
                .montoIva(new BigDecimal("13.00")).subtotal(new BigDecimal("100.00")).total(new BigDecimal("113.00"))
                .build());
        return nota;
    }

    private FacturaPdfServiceImpl servicio() {
        return new FacturaPdfServiceImpl(mock(FacturaService.class), mock(EmpresaService.class),
                mock(ConfiguracionEmpresaService.class), mock(CuentaBancariaRepository.class),
                mock(UsuarioService.class), notas, comprobantes,
                mock(ConfiguracionFacturacionService.class), mock(LugarVentaResolver.class));
    }

    private String pdf(NotaFactura nota, ComprobanteElectronico vigente) throws Exception {
        EmpresaService empresaService = mock(EmpresaService.class);
        ConfiguracionEmpresaService configuracionService = mock(ConfiguracionEmpresaService.class);
        CuentaBancariaRepository cuentas = mock(CuentaBancariaRepository.class);
        FacturaPdfServiceImpl service = new FacturaPdfServiceImpl(mock(FacturaService.class), empresaService,
                configuracionService, cuentas, mock(UsuarioService.class), notas, comprobantes,
                mock(ConfiguracionFacturacionService.class), mock(LugarVentaResolver.class));
        when(notas.findById(nota.getId())).thenReturn(Optional.of(nota));
        when(comprobantes.findFirstByNotaFactura_IdAndVigenteTrue(nota.getId())).thenReturn(Optional.ofNullable(vigente));
        Empresa empresa = new Empresa();
        empresa.setIdEmpresa(1);
        empresa.setNombreEmpresa("Monrachem");
        when(empresaService.getEmpresaPrincipal()).thenReturn(empresa);
        ConfiguracionEmpresa configuracion = new ConfiguracionEmpresa();
        configuracion.setRazonSocial("EMANUEL SOTO LEAL");
        configuracion.setNumeroIdentificacion("3101234567");
        when(configuracionService.obtenerOCrearConfiguracion()).thenReturn(configuracion);
        when(cuentas.findByEmpresaIdEmpresaAndActivaTrue(1)).thenReturn(List.of());

        byte[] bytes = service.generarPdfNotaCredito(nota.getId());
        StringBuilder sb = new StringBuilder();
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(bytes)))) {
            for (int i = 1; i <= doc.getNumberOfPages(); i++) {
                sb.append(PdfTextExtractor.getTextFromPage(doc.getPage(i))).append('\n');
            }
        }
        return sb.toString();
    }
}
