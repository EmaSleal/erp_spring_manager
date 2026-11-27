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
import api.astro.whats_orders_manager.modules.facturacion.model.ConfiguracionFacturacion;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.service.ConfiguracionFacturacionService;
import api.astro.whats_orders_manager.modules.facturacion.repository.NotaFacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.service.FacturaService;
import api.astro.whats_orders_manager.modules.regimen.enums.Regimen;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

    private static final String TITULO = "Factura — Régimen de Tributación Simplificada";
    private static final String LEYENDA = "Contribuyente del Régimen de Tributación Simplificada. No cobra IVA.";

    @Test
    @DisplayName("decisión pura: SIMPLIFICADO → título y leyenda de la configuración, sin clave ni consecutivo, sin leyendas de Hacienda y sin IVA")
    void decisionPura() {
        PdfFiscalDecision d = PdfFiscalDecision.deSimplificado(TITULO, LEYENDA);

        assertThat(d.titulo()).isEqualTo(TITULO);
        assertThat(d.leyendaRts()).isEqualTo(LEYENDA);
        assertThat(d.esComprobanteRts()).isTrue();
        assertThat(d.mostrarClaveYConsecutivo()).isFalse();
        assertThat(d.enTramite()).isFalse();
        assertThat(d.leyendaResolucion()).isFalse();
        assertThat(d.mostrarIva()).isFalse();
        assertThat(PdfFiscalDecision.de(EstadoComprobante.ACEPTADO, TipoComprobanteElectronico.FACTURA_ELECTRONICA).mostrarIva())
                .isTrue();
        assertThat(PdfFiscalDecision.de(EstadoComprobante.ACEPTADO, TipoComprobanteElectronico.FACTURA_ELECTRONICA)
                .esComprobanteRts()).isFalse();
    }

    @Test
    @DisplayName("decisión pura: la lectura no inventa texto (título nulo → vacío; leyenda en blanco → sin leyenda)")
    void decisionNoInventaTexto() {
        PdfFiscalDecision d = PdfFiscalDecision.deSimplificado(null, "   ");

        assertThat(d.titulo()).isEmpty();
        assertThat(d.leyendaRts()).isNull();
    }

    @Test
    @DisplayName("PDF SIMPLIFICADO: título y leyenda configurados, sin CLAVE, sin NÚMERO CONSECUTIVO, sin %IVA ni IVA:, con Total")
    void pdfSimplificado() throws Exception {
        String t = pdf(factura(Regimen.SIMPLIFICADO, null), config(TITULO, LEYENDA), null);

        // el título largo se parte en dos líneas dentro de la celda del encabezado
        assertThat(t).contains("Factura — Régimen de Tributación").contains("Simplificada").contains(LEYENDA)
                .contains("FA01-00001").contains("Total")
                .doesNotContain("CLAVE").doesNotContain("NÚMERO CONSECUTIVO").doesNotContain("%IVA")
                .doesNotContain("IVA:").doesNotContain("proforma").doesNotContain("MH-DGT-RES-0027-2024")
                .doesNotContain("Comprobante de venta");
        // la leyenda se imprime junto al total, después de la fila Total
        assertThat(t.indexOf(LEYENDA)).isGreaterThan(t.indexOf("Total"));
    }

    @Test
    @DisplayName("el título y la leyenda salen de la configuración (cambian al editarlos)")
    void textosConfigurables() throws Exception {
        String t = pdf(factura(Regimen.SIMPLIFICADO, null), config("Factura de venta RTS", "Sin IVA por el régimen"), null);

        assertThat(t).contains("Factura de venta RTS").contains("Sin IVA por el régimen")
                .doesNotContain("Tributación Simplificada").doesNotContain(TITULO);
    }

    @Test
    @DisplayName("leyenda vacía: el PDF no lleva línea de leyenda")
    void leyendaVacia() throws Exception {
        String conLeyenda = pdf(factura(Regimen.SIMPLIFICADO, null), config(TITULO, LEYENDA), null);
        String sinLeyenda = pdf(factura(Regimen.SIMPLIFICADO, null), config(TITULO, null), null);
        String enBlanco = pdf(factura(Regimen.SIMPLIFICADO, null), config(TITULO, "   "), null);

        assertThat(sinLeyenda).contains("Factura — Régimen de Tributación").doesNotContain("Contribuyente del Régimen").doesNotContain("No cobra IVA");
        assertThat(enBlanco).isEqualTo(sinLeyenda);
        assertThat(conLeyenda.lines().count()).isGreaterThan(sinLeyenda.lines().count());
    }

    @Test
    @DisplayName("título en blanco en la base: el PDF no inventa un texto (sin título de respaldo)")
    void tituloEnBlancoNoSeInventa() throws Exception {
        String t = pdf(factura(Regimen.SIMPLIFICADO, null), config("  ", LEYENDA), null);

        assertThat(t).doesNotContain("Comprobante de venta").doesNotContain("Régimen de Tributación Simplificada —")
                .doesNotContain("Documento no fiscal").contains(LEYENDA).contains("FA01-00001");
    }

    @Test
    @DisplayName("sin configuración activa: tampoco se inventa el título")
    void sinConfiguracion() throws Exception {
        String t = pdf(factura(Regimen.SIMPLIFICADO, null), null, null);

        assertThat(t).doesNotContain("Comprobante de venta").doesNotContain("Factura —").contains("FA01-00001");
    }

    @Test
    @DisplayName("caracteres especiales (<, &, comillas) salen literales en el PDF, no interpretados")
    void caracteresEspeciales() throws Exception {
        String titulo = "Factura <b>RTS</b> & \"Co\" 'x'";
        String leyenda = "<script>alert(1)</script> & &amp; \"comillas\" 'simples'";
        String t = pdf(factura(Regimen.SIMPLIFICADO, null), config(titulo, leyenda), null);

        assertThat(t).contains("Factura <b>RTS</b> & \"Co\" 'x'")
                .contains("<script>alert(1)</script> & &amp; \"comillas\" 'simples'");
    }

    @Test
    @DisplayName("T-059 criterio 6: el PDF del RTS lleva «Lugar» con el cantón y la provincia; el GENERAL no")
    void lugarSoloEnRts() throws Exception {
        String rts = pdf(factura(Regimen.SIMPLIFICADO, null), config(TITULO, LEYENDA), "Escazú, San José");
        String general = pdf(factura(Regimen.GENERAL, null), config(TITULO, LEYENDA), "Escazú, San José");

        assertThat(rts).contains("Lugar").contains("Escazú, San José");
        assertThat(general).doesNotContain("Lugar").doesNotContain("Escazú");
    }

    @Test
    @DisplayName("sin lugar resoluble el PDF del RTS no imprime una línea «Lugar» vacía")
    void sinLugar() throws Exception {
        assertThat(pdf(factura(Regimen.SIMPLIFICADO, null), config(TITULO, LEYENDA), null)).doesNotContain("Lugar");
    }

    @Test
    @DisplayName("T-059 criterio 7: el PDF GENERAL no cambia (texto exacto de antes de T-059), con o sin textos del RTS configurados")
    void pdfGeneralTextoExacto() throws Exception {
        String esperado = GENERAL_PROFORMA_ESPERADO;

        assertThat(pdf(factura(Regimen.GENERAL, null), null, null)).isEqualTo(esperado);
        assertThat(pdf(factura(Regimen.GENERAL, null), config(TITULO, LEYENDA), "Escazú, San José")).isEqualTo(esperado);
    }

    @Test
    @DisplayName("PDF GENERAL sin comprobante: sigue siendo la proforma de siempre, con %IVA y fila IVA:")
    void pdfGeneral_noCambia() throws Exception {
        String t = pdf(factura(Regimen.GENERAL, null), null, null);

        assertThat(t).contains("Documento no fiscal (proforma)").contains("%IVA").contains("IVA:").contains("Total")
                .doesNotContain("Tributación Simplificada");
    }

    @Test
    @DisplayName("PDF GENERAL ACEPTADO: clave, consecutivo, %IVA e IVA: como antes")
    void pdfGeneralAceptado_noCambia() throws Exception {
        ComprobanteElectronico aceptado = ComprobanteElectronico.builder().id(10L).estado(EstadoComprobante.ACEPTADO)
                .tipoComprobante(TipoComprobanteElectronico.FACTURA_ELECTRONICA).vigente(true).claveNumerica(CLAVE)
                .consecutivo(CONSECUTIVO).fechaEmision(LocalDateTime.of(2026, 12, 20, 10, 0)).build();
        String t = pdf(factura(Regimen.GENERAL, aceptado), null, null);

        assertThat(t).contains("Factura electrónica").contains(CLAVE).contains(CONSECUTIVO).contains("%IVA").contains("IVA:");
    }

    @Test
    @DisplayName("T-057 criterio 15: PDF GENERAL con clave externa → línea «Comprobante emitido fuera del ERP — Clave:» y la clave; sin clave no la lleva")
    void pdfGeneralConClaveExterna() throws Exception {
        Factura f = factura(Regimen.GENERAL, null);
        f.setComprobanteExternoClave(CLAVE);
        f.setComprobanteExternoFecha(java.time.LocalDate.of(2026, 12, 28));

        String t = pdf(f, null, null);

        assertThat(t).contains("Comprobante emitido fuera del ERP").contains(CLAVE).contains("28-12-2026")
                .contains("%IVA").contains("IVA:");
        assertThat(pdf(factura(Regimen.GENERAL, null), null, null)).doesNotContain("fuera del ERP");
    }

    private static final String GENERAL_PROFORMA_ESPERADO =
            "Documento no fiscal (proforma)\n" +
            "EMANUEL SOTO LEAL\n" +
            "DOCUMENTO No.\n" +
            "Identificación: 3101234567\n" +
            "FA01-00001\n" +
            "México\n" +
            "ACTIVIDAD ECONOMICA\n" +
            "Cliente:  C1  Cliente de prueba Fecha: \n" +
            "Identificación: Forma Pago: \n" +
            "Correo: Vencimiento:  N/A\n" +
            "Dirección: Plazo:  0 días naturales\n" +
            "Contacto: Vendedor: \n" +
            "Teléfono: Tipo Cambio: \n" +
            "Orden Compra:  Observ.: Activ Econ.: \n" +
            "Lin Código Cod Cabys Detalle Cantidad UM Precio %IVA Total\n" +
            "CIENTO TRECE CON 00/100 Colones\n" +
            "Por favor depositar en las siguientes cuentas bancarias\n" +
            "Sub Total ¢100.00\n" +
            "Descuento ¢0.00\n" +
            "Neto ¢100.00\n" +
            "IVA: ¢13.00\n" +
            "Total ¢113.00\n" +
            "Moneda: Colones\n";
    // texto extraído del PDF GENERAL antes de T-059 (mismo constructor de la fixture, sin textos del RTS)

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

    private static ConfiguracionFacturacion config(String titulo, String leyenda) {
        ConfiguracionFacturacion c = new ConfiguracionFacturacion();
        c.setPdfRtsTitulo(titulo);
        c.setPdfRtsLeyenda(leyenda);
        return c;
    }

    private String pdf(Factura factura, ConfiguracionFacturacion cfg, String lugar) throws Exception {
        FacturaService facturaService = mock(FacturaService.class);
        EmpresaService empresaService = mock(EmpresaService.class);
        ConfiguracionEmpresaService configuracionService = mock(ConfiguracionEmpresaService.class);
        CuentaBancariaRepository cuentas = mock(CuentaBancariaRepository.class);
        ConfiguracionFacturacionService configuracionFacturacion = mock(ConfiguracionFacturacionService.class);
        LugarVentaResolver lugarVenta = mock(LugarVentaResolver.class);
        FacturaPdfServiceImpl service = new FacturaPdfServiceImpl(facturaService, empresaService, configuracionService,
                cuentas, mock(UsuarioService.class), mock(NotaFacturaRepository.class),
                mock(ComprobanteElectronicoRepository.class), configuracionFacturacion, lugarVenta);
        when(configuracionFacturacion.getConfiguracionActiva()).thenReturn(Optional.ofNullable(cfg));
        when(lugarVenta.lugarDe(any())).thenReturn(lugar);
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
