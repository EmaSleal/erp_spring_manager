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
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.repository.NotaFacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.service.FacturaService;
import api.astro.whats_orders_manager.modules.regimen.enums.Regimen;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T-057 (ADR-073 §1, criterio 8): el PDF de una factura SIMPLIFICADO es el comprobante de venta del RTS (título
 * configurable, sin clave ni consecutivo de Hacienda, sin columna %IVA ni fila IVA, con el total). El de una GENERAL no cambia.
 */
@DisplayName("T-057 — PDF de la factura según el régimen sellado")
class FacturaPdfRegimenSimplificadoTest {

    private static final String CLAVE = "50614112600310123456700100001010000000007100000001";
    private static final String CONSECUTIVO = "00100001010000000007";

    @Test
    @DisplayName("decisión pura: SIMPLIFICADO → título del RTS, sin clave ni consecutivo, sin leyendas y sin IVA")
    void decisionPura() {
        PdfFiscalDecision d = PdfFiscalDecision.deSimplificado("Comprobante de venta — Régimen de Tributación Simplificada");

        assertThat(d.titulo()).isEqualTo("Comprobante de venta — Régimen de Tributación Simplificada");
        assertThat(d.mostrarClaveYConsecutivo()).isFalse();
        assertThat(d.enTramite()).isFalse();
        assertThat(d.leyendaResolucion()).isFalse();
        assertThat(d.mostrarIva()).isFalse();
        assertThat(PdfFiscalDecision.de(EstadoComprobante.ACEPTADO, TipoComprobanteElectronico.FACTURA_ELECTRONICA).mostrarIva())
                .isTrue();
    }

    @Test
    @DisplayName("PDF SIMPLIFICADO: título del RTS, sin CLAVE, sin NÚMERO CONSECUTIVO, sin %IVA ni IVA:, con Total")
    void pdfSimplificado() throws Exception {
        String t = pdf(factura(Regimen.SIMPLIFICADO, null), null);

        assertThat(t).contains("Comprobante de venta").contains("Tributación Simplificada")
                .contains("FA01-00001").contains("Total")
                .doesNotContain("CLAVE").doesNotContain("NÚMERO CONSECUTIVO").doesNotContain("%IVA")
                .doesNotContain("IVA:").doesNotContain("proforma").doesNotContain("MH-DGT-RES-0027-2024");
    }

    @Test
    @DisplayName("el título del RTS es configurable (pendiente de P-127)")
    void tituloConfigurable() throws Exception {
        String t = pdf(factura(Regimen.SIMPLIFICADO, null), "Recibo de venta RTS");

        assertThat(t).contains("Recibo de venta RTS").doesNotContain("Comprobante de venta");
    }

    @Test
    @DisplayName("PDF GENERAL sin comprobante: sigue siendo la proforma de siempre, con %IVA y fila IVA:")
    void pdfGeneral_noCambia() throws Exception {
        String t = pdf(factura(Regimen.GENERAL, null), null);

        assertThat(t).contains("Documento no fiscal (proforma)").contains("%IVA").contains("IVA:").contains("Total")
                .doesNotContain("Tributación Simplificada");
    }

    @Test
    @DisplayName("PDF GENERAL ACEPTADO: clave, consecutivo, %IVA e IVA: como antes")
    void pdfGeneralAceptado_noCambia() throws Exception {
        ComprobanteElectronico aceptado = ComprobanteElectronico.builder().id(10L).estado(EstadoComprobante.ACEPTADO)
                .tipoComprobante(TipoComprobanteElectronico.FACTURA_ELECTRONICA).vigente(true).claveNumerica(CLAVE)
                .consecutivo(CONSECUTIVO).fechaEmision(LocalDateTime.of(2026, 12, 20, 10, 0)).build();
        String t = pdf(factura(Regimen.GENERAL, aceptado), null);

        assertThat(t).contains("Factura electrónica").contains(CLAVE).contains(CONSECUTIVO).contains("%IVA").contains("IVA:");
    }

    private static Factura factura(Regimen regimen, ComprobanteElectronico vigente) {
        Cliente cliente = new Cliente();
        cliente.setIdCliente(1);
        cliente.setNombre("Cliente de prueba");
        Factura f = new Factura();
        f.setIdFactura(1);
        f.setNumeroFactura("FA01-00001");
        f.setCliente(cliente);
        f.setRegimenEmisor(regimen);
        boolean rts = regimen == Regimen.SIMPLIFICADO;
        f.setSubtotal(new BigDecimal(rts ? "113.00" : "100.00"));
        f.setIgv(new BigDecimal(rts ? "0.00" : "13.00"));
        f.setTotal(new BigDecimal("113.00"));
        f.setMonedaFE(MonedaFE.CRC);
        f.setAnulada(false);
        f.setLineas(new ArrayList<>());
        if (vigente != null) {
            f.getComprobantes().add(vigente);
        }
        return f;
    }

    private String pdf(Factura factura, String tituloRts) throws Exception {
        FacturaService facturaService = mock(FacturaService.class);
        EmpresaService empresaService = mock(EmpresaService.class);
        ConfiguracionEmpresaService configuracionService = mock(ConfiguracionEmpresaService.class);
        CuentaBancariaRepository cuentas = mock(CuentaBancariaRepository.class);
        FacturaPdfServiceImpl service = new FacturaPdfServiceImpl(facturaService, empresaService, configuracionService,
                cuentas, mock(UsuarioService.class), mock(NotaFacturaRepository.class),
                mock(ComprobanteElectronicoRepository.class));
        if (tituloRts != null) {
            ReflectionTestUtils.setField(service, "tituloRts", tituloRts);
        }
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
