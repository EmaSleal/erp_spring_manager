package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.CondicionVentaFE;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MedioPagoFE;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ComprobanteElectronicoService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.XmlValidator;
import api.astro.whats_orders_manager.modules.facturacion.enums.TipoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.NotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.repository.NotaFacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.service.CalculadoraImpuestoService;
import api.astro.whats_orders_manager.modules.facturacion.service.NotaFacturaService;
import api.astro.whats_orders_manager.modules.facturacion.service.PagoService;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T-032 (criterios 8, 10, 11, 26 y 32): XML de la nota de crédito total contra el XSD oficial
 * NotaCreditoElectronica_V4.4.xsd (src/main/resources/xsd, sin simular el validador), y regresión del XML firmado de
 * factura y tiquete contra FacturaElectronica_V4.4.xsd y TiqueteElectronico_V4.4.xsd.
 *
 * La nota se arma con NotaFacturaService (repositorios simulados) a partir de una factura cuyo comprobante ACEPTADO
 * tiene el XML real de XmlGeneratorServiceImpl. El XSD exige ds:Signature, así que el XML de la nota se firma con
 * FirmaDigitalServiceImpl y un certificado de prueba generado con keytool en un directorio temporal.
 */
@DisplayName("T-032 — XML de la nota de crédito v4.4 contra el XSD oficial")
@SuppressWarnings("deprecation")
class NotaCreditoXmlTest {

    private static final String CLAVE_FACTURA = "50612112600310123456700100001010000000001100000001";
    private static final String CLAVE_NOTA = "50612112600310123456700100001030000000001100000002";
    private static final LocalDateTime FECHA_FACTURA = LocalDateTime.of(2026, 11, 2, 9, 30, 15);
    private static final LocalDateTime FECHA_NOTA = LocalDateTime.of(2026, 11, 12, 16, 5, 0);
    private static final String PIN = "t032pin";

    @TempDir
    static Path dir;
    private static Path p12;

    private final XmlGeneratorServiceImpl generador = new XmlGeneratorServiceImpl(new CalculadoraImpuestoService(null));
    private final FirmaDigitalServiceImpl firma = new FirmaDigitalServiceImpl();

    private ConfiguracionEmpresa empresa;
    private Factura factura;
    private ComprobanteElectronico aceptado;
    private Producto gravado;

    @BeforeAll
    static void certificado() throws Exception {
        p12 = dir.resolve("t032-prueba.p12");
        String keytool = Path.of(System.getProperty("java.home"), "bin", "keytool").toString();
        Process proceso = new ProcessBuilder(keytool, "-genkeypair", "-alias", "t032", "-keyalg", "RSA",
                "-keysize", "2048", "-validity", "30", "-storetype", "PKCS12", "-keystore", p12.toString(),
                "-storepass", PIN, "-keypass", PIN,
                "-dname", "CN=EMPRESA T032 PRUEBA, OU=CPJ, O=PERSONA JURIDICA, C=CR, SERIALNUMBER=CPJ-3-101-234567")
                .redirectErrorStream(true).start();
        assertThat(proceso.waitFor(60, TimeUnit.SECONDS)).isTrue();
        assertThat(proceso.exitValue()).as(new String(proceso.getInputStream().readAllBytes())).isZero();
    }

    @BeforeEach
    void setUp() {
        empresa = ConfiguracionEmpresa.builder()
                .razonSocial("Empresa T032 S.A.")
                .numeroIdentificacion("3101234567")
                .codigoActividad("4773.0")
                .codigoProvincia("1")
                .canton("01")
                .distrito("01")
                .otrasSenas("Avenida Central, San José")
                .email("facturacion@empresa-t032.example")
                .telefono("22223333")
                .build();

        Cliente cliente = new Cliente();
        cliente.setNombre("Cliente T032 & Hijos <Ltda>");
        cliente.setNumeroIdentificacion("109990999");
        cliente.setEmail("cliente@t032.example");

        gravado = producto("Producto gravado 13 %", "1234567890123", true);
        Producto exento = producto("Producto exento", "9876543210987", false);

        factura = new Factura();
        factura.setIdFactura(77);
        factura.setNumeroFactura("F-T032-77");
        factura.setCliente(cliente);
        factura.setMonedaFE(MonedaFE.CRC);
        factura.setTipoCambio(BigDecimal.ONE);
        factura.setAnulada(false);
        factura.getLineas().add(linea(1, gravado, 2, "2260.00"));
        factura.getLineas().add(linea(2, exento, 1, "500.00"));
        factura.setTotal(new BigDecimal("2760.00"));

        aceptado = ComprobanteElectronico.builder()
                .id(10L)
                .factura(factura)
                .empresa(empresa)
                .tipoComprobante(TipoComprobanteElectronico.FACTURA_ELECTRONICA)
                .claveNumerica(CLAVE_FACTURA)
                .consecutivo("00100001010000000001")
                .fechaEmision(FECHA_FACTURA)
                .estado(EstadoComprobante.ACEPTADO)
                .vigente(true)
                .build();
        aceptado.setXmlComprobante(firma.firmarXml(generador.generarXmlFactura(aceptado), p12.toString(), PIN));
    }

    @ParameterizedTest(name = "código {0}")
    @ValueSource(strings = {"01", "13"})
    @DisplayName("la nota total firmada valida contra NotaCreditoElectronica_V4.4.xsd")
    void notaTotal_validaContraXsd(String codigo) throws Exception {
        String xml = xmlFirmadoDeLaNota(codigo, "Anulación total por devolución del pedido");

        assertThat(XmlValidator.erroresContraXsd(xml, TipoComprobanteElectronico.NOTA_CREDITO)).isEmpty();
    }

    @Test
    @DisplayName("regresión (criterio 32): la factura firmada del flujo real valida contra FacturaElectronica_V4.4.xsd")
    void factura_validaContraXsd() {
        assertThat(XmlValidator.erroresContraXsd(aceptado.getXmlComprobante(),
                TipoComprobanteElectronico.FACTURA_ELECTRONICA)).isEmpty();
    }

    @Test
    @DisplayName("regresión (criterio 32): factura a crédito en USD con transferencia valida contra su XSD")
    void facturaCreditoUsd_validaContraXsd() {
        factura.setMonedaFE(MonedaFE.USD);
        factura.setTipoCambio(new BigDecimal("512.34000"));
        factura.setCondicionVentaFE(CondicionVentaFE.CREDITO);
        factura.setPlazoCredito(30);
        factura.setMedioPagoFE(MedioPagoFE.TRANSFERENCIA_DEPOSITO);
        String xml = firma.firmarXml(generador.generarXmlFactura(aceptado), p12.toString(), PIN);

        assertThat(XmlValidator.erroresContraXsd(xml, TipoComprobanteElectronico.FACTURA_ELECTRONICA)).isEmpty();
    }

    @Test
    @DisplayName("regresión (criterio 32): el tiquete firmado del flujo real valida contra TiqueteElectronico_V4.4.xsd")
    void tiquete_validaContraXsd() {
        ComprobanteElectronico tiquete = ComprobanteElectronico.builder()
                .factura(factura)
                .empresa(empresa)
                .tipoComprobante(TipoComprobanteElectronico.TIQUETE_ELECTRONICO)
                .claveNumerica(CLAVE_FACTURA)
                .fechaEmision(FECHA_FACTURA)
                .build();
        String xml = firma.firmarXml(generador.generarXmlTiquete(tiquete), p12.toString(), PIN);

        assertThat(XmlValidator.erroresContraXsd(xml, TipoComprobanteElectronico.TIQUETE_ELECTRONICO)).isEmpty();
    }

    @Test
    @DisplayName("sin firma, el único error del XSD es la ds:Signature que falta (el XSD real se aplica)")
    void sinFirma_elXsdExigeLaFirma() {
        String xml = generador.generarXmlNotaCredito(comprobanteDeLaNota(nota("01", "Anulación total")));

        List<String> errores = XmlValidator.erroresContraXsd(xml, TipoComprobanteElectronico.NOTA_CREDITO);

        assertThat(errores).hasSize(1);
        assertThat(errores.get(0)).contains("Signature");
    }

    @Test
    @DisplayName("InformacionReferencia: etiquetas del XSD, después de ResumenFactura, con clave y fecha del original")
    void informacionReferencia_segunXsd() throws Exception {
        Element raiz = raiz(xmlFirmadoDeLaNota("13", "Error material en el precio"));

        Element info = hijo(raiz, "InformacionReferencia");
        assertThat(anterior(info).getLocalName()).isEqualTo("ResumenFactura");
        assertThat(nombresDeHijos(info)).containsExactly("TipoDocIR", "Numero", "FechaEmisionIR", "Codigo", "Razon");
        assertThat(texto(info, "TipoDocIR")).isEqualTo("01");
        assertThat(texto(info, "Numero")).isEqualTo(CLAVE_FACTURA);
        assertThat(texto(info, "FechaEmisionIR"))
                .isEqualTo(FECHA_FACTURA.atOffset(ZoneOffset.of("-06:00")).toString());
        assertThat(texto(info, "Codigo")).isEqualTo("13");
        assertThat(texto(info, "Razon")).isEqualTo("Error material en el precio");
        assertThat(texto(raiz, "ProveedorSistemas")).isEqualTo("3101234567");
    }

    @Test
    @DisplayName("emisor, receptor, moneda y medio de pago iguales al comprobante original; total = TotalComprobante")
    void datosDelOriginal() throws Exception {
        Element nota = raiz(xmlFirmadoDeLaNota("01", "Anulación total"));
        Element original = raiz(aceptado.getXmlComprobante());

        assertThat(hijo(nota, "Emisor").getTextContent()).isEqualTo(hijo(original, "Emisor").getTextContent());
        assertThat(hijo(nota, "Receptor").getTextContent()).isEqualTo(hijo(original, "Receptor").getTextContent());
        assertThat(texto(nota, "CondicionVenta")).isEqualTo(texto(original, "CondicionVenta"));
        Element resumenNota = hijo(nota, "ResumenFactura");
        Element resumenOriginal = hijo(original, "ResumenFactura");
        assertThat(hijo(resumenNota, "CodigoTipoMoneda").getTextContent())
                .isEqualTo(hijo(resumenOriginal, "CodigoTipoMoneda").getTextContent());
        assertThat(hijo(resumenNota, "MedioPago").getTextContent())
                .isEqualTo(hijo(resumenOriginal, "MedioPago").getTextContent());
        assertThat(texto(resumenNota, "TotalComprobante")).isEqualTo(texto(resumenOriginal, "TotalComprobante"))
                .isEqualTo("2760.00000");
        assertThat(hijo(nota, "DetalleServicio").getTextContent())
                .isEqualTo(hijo(original, "DetalleServicio").getTextContent());
    }

    @Test
    @DisplayName("la nota copia cada línea con su CAByS y sus totales coinciden con la factura")
    void lineasYTotales() {
        NotaFactura nota = nota("01", "Anulación total");

        assertThat(nota.getLineas()).hasSize(2);
        assertThat(nota.getLineas().get(0).getCodigoCabys()).isEqualTo("1234567890123");
        assertThat(nota.getLineas().get(0).getCantidad()).isEqualByComparingTo("2");
        assertThat(nota.getLineas().get(0).getSubtotal()).isEqualByComparingTo("2000.00");
        assertThat(nota.getLineas().get(0).getMontoIva()).isEqualByComparingTo("260.00");
        assertThat(nota.getLineas().get(1).getCodigoCabys()).isEqualTo("9876543210987");
        assertThat(nota.getLineas().get(1).getMontoIva()).isEqualByComparingTo("0.00");
        assertThat(nota.getSubtotal()).isEqualByComparingTo("2500.00");
        assertThat(nota.getImpuesto()).isEqualByComparingTo("260.00");
        assertThat(nota.getTotal()).isEqualByComparingTo(factura.getTotal());
        assertThat(nota.getClaveReferencia()).isEqualTo(CLAVE_FACTURA);
        assertThat(nota.getFechaEmisionReferencia()).isEqualTo(FECHA_FACTURA);
    }

    @Test
    @DisplayName("la razón escapa & y < y sigue validando contra el XSD")
    void razonConCaracteresEspeciales() throws Exception {
        String razon = "Error & <corrección> del \"precio\"";
        String sinFirma = generador.generarXmlNotaCredito(comprobanteDeLaNota(nota("01", razon)));
        String xml = firma.firmarXml(sinFirma, p12.toString(), PIN);

        assertThat(sinFirma).contains("<Razon>Error &amp; &lt;corrección&gt; del &quot;precio&quot;</Razon>");
        assertThat(texto(hijo(raiz(xml), "InformacionReferencia"), "Razon")).isEqualTo(razon);
        assertThat(XmlValidator.erroresContraXsd(xml, TipoComprobanteElectronico.NOTA_CREDITO)).isEmpty();
    }

    // ── Apoyo ────────────────────────────────────────────────────────────────

    private String xmlFirmadoDeLaNota(String codigo, String razon) {
        String xml = generador.generarXmlNotaCredito(comprobanteDeLaNota(nota(codigo, razon)));
        return firma.firmarXml(xml, p12.toString(), PIN);
    }

    private ComprobanteElectronico comprobanteDeLaNota(NotaFactura nota) {
        return ComprobanteElectronico.builder()
                .notaFactura(nota)
                .empresa(empresa)
                .tipoComprobante(TipoComprobanteElectronico.NOTA_CREDITO)
                .claveNumerica(CLAVE_NOTA)
                .consecutivo("00100001030000000001")
                .fechaEmision(nota.getFechaEmision())
                .estado(EstadoComprobante.GENERADO)
                .build();
    }

    /** Nota total armada por NotaFacturaService, como al emitirla (repositorios simulados). */
    private NotaFactura nota(String codigo, String razon) {
        FacturaRepository facturas = mock(FacturaRepository.class);
        NotaFacturaRepository notas = mock(NotaFacturaRepository.class);
        ComprobanteElectronicoRepository comprobantes = mock(ComprobanteElectronicoRepository.class);
        ComprobanteElectronicoService comprobanteService = mock(ComprobanteElectronicoService.class);
        PagoService pagos = mock(PagoService.class);
        when(facturas.findByIdParaActualizar(77)).thenReturn(Optional.of(factura));
        when(comprobantes.findFirstByFactura_IdFacturaAndVigenteTrue(77)).thenReturn(Optional.of(aceptado));
        when(notas.existsByFactura_IdFacturaAndEstadoIn(anyInt(), anyCollection())).thenReturn(false);
        when(pagos.findPagosValidosByFacturaId(77)).thenReturn(new ArrayList<>());
        when(notas.saveAndFlush(any())).thenAnswer(inv -> {
            NotaFactura n = inv.getArgument(0);
            n.setId(5L);
            return n;
        });
        when(comprobanteService.generarYFirmarDesdeNota(any())).thenReturn(comprobanteDeLaNotaVacio());
        Clock reloj = Clock.fixed(FECHA_NOTA.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

        new NotaFacturaService(facturas, notas, comprobantes, comprobanteService, pagos,
                new CalculadoraImpuestoService(null), reloj)
                .emitirNotaCreditoTotal(77, TipoNotaFactura.CREDITO, codigo, razon, "admin");

        ArgumentCaptor<NotaFactura> captor = ArgumentCaptor.forClass(NotaFactura.class);
        verify(comprobanteService).generarYFirmarDesdeNota(captor.capture());
        return captor.getValue();
    }

    private ComprobanteElectronico comprobanteDeLaNotaVacio() {
        return ComprobanteElectronico.builder().claveNumerica(CLAVE_NOTA).estado(EstadoComprobante.FIRMADO).build();
    }

    private static Producto producto(String descripcion, String cabys, boolean esGravado) {
        Producto p = new Producto();
        p.setDescripcion(descripcion);
        p.setCodigoCabys(cabys);
        p.setGravado(esGravado);
        p.setPorcentajeImpuesto(esGravado ? new BigDecimal("13") : BigDecimal.ZERO);
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
        linea.setPrecioUnitario(new BigDecimal(subtotal).divide(BigDecimal.valueOf(cantidad)));
        return linea;
    }

    private static Element raiz(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        return doc.getDocumentElement();
    }

    private static Element hijo(Element padre, String nombre) {
        for (Node n = padre.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && nombre.equals(e.getLocalName())) {
                return e;
            }
        }
        throw new AssertionError("Sin elemento " + nombre + " en " + padre.getLocalName());
    }

    private static Element anterior(Element e) {
        Node n = e.getPreviousSibling();
        while (n != null && !(n instanceof Element)) {
            n = n.getPreviousSibling();
        }
        return (Element) n;
    }

    private static List<String> nombresDeHijos(Element e) {
        List<String> nombres = new ArrayList<>();
        for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element hijo) {
                nombres.add(hijo.getLocalName());
            }
        }
        return nombres;
    }

    private static String texto(Element padre, String nombre) {
        return hijo(padre, nombre).getTextContent();
    }
}
