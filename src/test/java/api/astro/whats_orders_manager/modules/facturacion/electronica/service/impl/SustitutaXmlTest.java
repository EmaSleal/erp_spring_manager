package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.XmlValidator;
import api.astro.whats_orders_manager.modules.facturacion.electronica.validacion.CatalogoUbicacionDePrueba;
import api.astro.whats_orders_manager.modules.facturacion.electronica.validacion.DatosFeDePrueba;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.facturacion.service.CalculadoraImpuestoService;
import api.astro.whats_orders_manager.modules.producto.model.ArticuloMaestro;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-039 (criterios 4, 5, 8, 9 y 10): InformacionReferencia de la factura (sustituta 07/15 y reemisión 10/16) en el XML
 * firmado, validado contra el FacturaElectronica_V4.4.xsd real, con los cuatro campos verificados.
 */
@DisplayName("T-039 — InformacionReferencia de la factura sustituta y de la reemisión")
class SustitutaXmlTest {

    private static final String CLAVE_ORIGINAL = "50612112600310123456700100001010000000001100000001";
    private static final String CLAVE_RECHAZADO = "50612112600310123456700100001010000000002100000002";
    private static final String CLAVE_NUEVA = "50612112600310123456700100001010000000003100000003";
    private static final LocalDateTime FECHA_ORIGINAL = LocalDateTime.of(2026, 10, 28, 9, 30, 15);
    private static final LocalDateTime FECHA_RECHAZADO = LocalDateTime.of(2026, 11, 5, 14, 0, 1);
    private static final String PIN = "t039pin";

    @TempDir
    static Path dir;
    private static Path p12;

    private final XmlGeneratorServiceImpl generador = new XmlGeneratorServiceImpl(new CalculadoraImpuestoService(null),
            CatalogoUbicacionDePrueba.validador());
    private final FirmaDigitalServiceImpl firma = new FirmaDigitalServiceImpl();

    private ConfiguracionEmpresa empresa;
    private Cliente cliente;
    private Producto gravado;
    private Factura original;
    private ComprobanteElectronico aceptadoOriginal;

    @BeforeAll
    static void certificado() throws Exception {
        p12 = dir.resolve("t039-prueba.p12");
        String keytool = Path.of(System.getProperty("java.home"), "bin", "keytool").toString();
        Process proceso = new ProcessBuilder(keytool, "-genkeypair", "-alias", "t039", "-keyalg", "RSA",
                "-keysize", "2048", "-validity", "30", "-storetype", "PKCS12", "-keystore", p12.toString(),
                "-storepass", PIN, "-keypass", PIN,
                "-dname", "CN=EMPRESA T039 PRUEBA, OU=CPJ, O=PERSONA JURIDICA, C=CR, SERIALNUMBER=CPJ-3-101-234567")
                .redirectErrorStream(true).start();
        assertThat(proceso.waitFor(60, TimeUnit.SECONDS)).isTrue();
        assertThat(proceso.exitValue()).as(new String(proceso.getInputStream().readAllBytes())).isZero();
    }

    @BeforeEach
    void setUp() {
        empresa = ConfiguracionEmpresa.builder()
                .razonSocial("Empresa T039 S.A.").numeroIdentificacion("3101234567").codigoActividad("4773.0")
                .codigoProvincia("1").canton("01").distrito("01").otrasSenas("Avenida Central, San José")
                .email("facturacion@empresa-t039.example").telefono("22223333").build();
        DatosFeDePrueba.emisor(empresa);
        cliente = new Cliente();
        cliente.setNombre("Cliente T039");
        cliente.setNumeroIdentificacion("109990999");
        cliente.setEmail("cliente@t039.example");
        DatosFeDePrueba.cliente(cliente);
        gravado = producto();

        original = factura(77, "FA-T039-77");
        original.setAnulada(true);
        aceptadoOriginal = comprobante(10L, original, CLAVE_ORIGINAL, FECHA_ORIGINAL, EstadoComprobante.ACEPTADO, true);
    }

    @ParameterizedTest(name = "código {0}")
    @CsvSource({"07", "15"})
    @DisplayName("la sustituta lleva 01, la clave y la fecha de la original, el código de la nota y la razón; valida")
    void sustituta_validaContraXsdConLosCuatroCampos(String codigo) throws Exception {
        Factura sustituta = sustituta(78, original, codigo, "Corrige el precio & las <cantidades>");
        ComprobanteElectronico nuevo = comprobante(null, sustituta, CLAVE_NUEVA, LocalDateTime.of(2026, 11, 18, 10, 0, 0),
                EstadoComprobante.GENERADO, true);

        String xml = firmado(nuevo);

        assertThat(XmlValidator.erroresContraXsd(xml, TipoComprobanteElectronico.FACTURA_ELECTRONICA)).isEmpty();
        Element raiz = raiz(xml);
        List<Element> refs = referencias(raiz);
        assertThat(refs).hasSize(1);
        Element ir = refs.get(0);
        assertThat(nombresDeHijos(ir)).containsExactly("TipoDocIR", "Numero", "FechaEmisionIR", "Codigo", "Razon");
        assertThat(texto(ir, "TipoDocIR")).isEqualTo("01");
        assertThat(texto(ir, "Numero")).isEqualTo(CLAVE_ORIGINAL);
        assertThat(texto(ir, "FechaEmisionIR")).isEqualTo("2026-10-28T09:30:15-06:00");
        assertThat(texto(ir, "Codigo")).isEqualTo(codigo);
        assertThat(texto(ir, "Razon")).isEqualTo("Corrige el precio & las <cantidades>");
        // Posición del XSD: justo después de ResumenFactura
        assertThat(anterior(ir).getLocalName()).isEqualTo("ResumenFactura");
        assertThat(ir.getNextSibling().getLocalName()).isEqualTo("Signature");
    }

    @Test
    @DisplayName("reemisión tras un rechazo: 10, clave y fecha del rechazado, código 16 y razón fija; valida")
    void reemision_conReferencia16() throws Exception {
        Factura normal = factura(80, "FA-T039-80");
        comprobante(20L, normal, CLAVE_RECHAZADO, FECHA_RECHAZADO, EstadoComprobante.RECHAZADO, false);
        ComprobanteElectronico nuevo = comprobante(null, normal, CLAVE_NUEVA, LocalDateTime.of(2026, 11, 18, 10, 0, 0),
                EstadoComprobante.GENERADO, true);

        String xml = firmado(nuevo);

        assertThat(XmlValidator.erroresContraXsd(xml, TipoComprobanteElectronico.FACTURA_ELECTRONICA)).isEmpty();
        List<Element> refs = referencias(raiz(xml));
        assertThat(refs).hasSize(1);
        Element ir = refs.get(0);
        assertThat(nombresDeHijos(ir)).containsExactly("TipoDocIR", "Numero", "FechaEmisionIR", "Codigo", "Razon");
        assertThat(texto(ir, "TipoDocIR")).isEqualTo("10");
        assertThat(texto(ir, "Numero")).isEqualTo(CLAVE_RECHAZADO);
        assertThat(texto(ir, "FechaEmisionIR")).isEqualTo("2026-11-05T14:00:01-06:00");
        assertThat(texto(ir, "Codigo")).isEqualTo("16");
        assertThat(texto(ir, "Razon")).isEqualTo("Sustituye comprobante rechazado por Hacienda");
    }

    @ParameterizedTest(name = "rechazado {0}, nuevo {1}")
    @CsvSource({"PRODUCCION, PRODUCCION", "SANDBOX, SANDBOX"})
    @DisplayName("T-044 criterio 6: rechazado del mismo ambiente que el nuevo → lleva la referencia 10/16; valida")
    void reemision_mismoAmbiente_llevaReferencia(AmbienteHacienda delRechazado, AmbienteHacienda delNuevo)
            throws Exception {
        Factura normal = factura(81, "FA-T044-81");
        comprobante(21L, normal, CLAVE_RECHAZADO, FECHA_RECHAZADO, EstadoComprobante.RECHAZADO, false)
                .setAmbiente(delRechazado);
        ComprobanteElectronico nuevo = comprobante(null, normal, CLAVE_NUEVA, LocalDateTime.of(2026, 11, 18, 10, 0, 0),
                EstadoComprobante.GENERADO, true);
        nuevo.setAmbiente(delNuevo);

        String xml = firmado(nuevo);

        assertThat(XmlValidator.erroresContraXsd(xml, TipoComprobanteElectronico.FACTURA_ELECTRONICA)).isEmpty();
        List<Element> refs = referencias(raiz(xml));
        assertThat(refs).hasSize(1);
        assertThat(texto(refs.get(0), "TipoDocIR")).isEqualTo("10");
        assertThat(texto(refs.get(0), "Codigo")).isEqualTo("16");
    }

    @ParameterizedTest(name = "rechazado {0}, nuevo {1}")
    @CsvSource({"SANDBOX, PRODUCCION", "PRODUCCION, SANDBOX"})
    @DisplayName("T-044 criterio 6: rechazado de otro ambiente → se emite sin la referencia 10/16, lo registra el log; valida")
    void reemision_otroAmbiente_sinReferencia(AmbienteHacienda delRechazado, AmbienteHacienda delNuevo)
            throws Exception {
        Factura normal = factura(82, "FA-T044-82");
        comprobante(22L, normal, CLAVE_RECHAZADO, FECHA_RECHAZADO, EstadoComprobante.RECHAZADO, false)
                .setAmbiente(delRechazado);
        ComprobanteElectronico nuevo = comprobante(null, normal, CLAVE_NUEVA, LocalDateTime.of(2026, 11, 18, 10, 0, 0),
                EstadoComprobante.GENERADO, true);
        nuevo.setAmbiente(delNuevo);
        ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(XmlGeneratorServiceImpl.class);
        ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> logs =
                new ch.qos.logback.core.read.ListAppender<>();
        logs.start();
        logger.addAppender(logs);
        String xml;
        try {
            xml = firmado(nuevo);
        } finally {
            logger.detachAppender(logs);
        }

        assertThat(XmlValidator.erroresContraXsd(xml, TipoComprobanteElectronico.FACTURA_ELECTRONICA)).isEmpty();
        assertThat(referencias(raiz(xml))).isEmpty();
        assertThat(logs.list).anyMatch(e -> e.getFormattedMessage().contains("Referencia 10/16 omitida")
                && e.getFormattedMessage().contains(delRechazado.name()) && e.getFormattedMessage().contains(delNuevo.name()));
    }

    @Test
    @DisplayName("sustituta reemitida tras un rechazo: dos referencias, 07/15 primero y 16 después; valida")
    void sustitutaReemitida_dosReferencias() throws Exception {
        Factura sustituta = sustituta(78, original, "15", "Error material en el precio");
        comprobante(20L, sustituta, CLAVE_RECHAZADO, FECHA_RECHAZADO, EstadoComprobante.RECHAZADO, false);
        ComprobanteElectronico nuevo = comprobante(21L, sustituta, CLAVE_NUEVA, LocalDateTime.of(2026, 11, 18, 10, 0, 0),
                EstadoComprobante.GENERADO, true);

        String xml = firmado(nuevo);

        assertThat(XmlValidator.erroresContraXsd(xml, TipoComprobanteElectronico.FACTURA_ELECTRONICA)).isEmpty();
        List<Element> refs = referencias(raiz(xml));
        assertThat(refs).hasSize(2);
        assertThat(texto(refs.get(0), "Codigo")).isEqualTo("15");
        assertThat(texto(refs.get(0), "Numero")).isEqualTo(CLAVE_ORIGINAL);
        assertThat(texto(refs.get(1), "Codigo")).isEqualTo("16");
        assertThat(texto(refs.get(1), "Numero")).isEqualTo(CLAVE_RECHAZADO);
        assertThat(texto(refs.get(1), "TipoDocIR")).isEqualTo("10");
    }

    @Test
    @DisplayName("una primera emisión normal no lleva InformacionReferencia y valida")
    void primeraEmision_sinReferencias() throws Exception {
        Factura normal = factura(81, "FA-T039-81");
        ComprobanteElectronico nuevo = comprobante(null, normal, CLAVE_NUEVA, LocalDateTime.of(2026, 11, 18, 10, 0, 0),
                EstadoComprobante.GENERADO, true);

        String xml = firmado(nuevo);

        assertThat(XmlValidator.erroresContraXsd(xml, TipoComprobanteElectronico.FACTURA_ELECTRONICA)).isEmpty();
        assertThat(referencias(raiz(xml))).isEmpty();
    }

    @Test
    @DisplayName("regenerar el XML de un comprobante ya guardado da las mismas referencias y no toma de referencia a sí mismo")
    void regeneracion_esEstableYSoloMiraRechazadosAnteriores() throws Exception {
        Factura normal = factura(82, "FA-T039-82");
        comprobante(20L, normal, CLAVE_RECHAZADO, FECHA_RECHAZADO, EstadoComprobante.RECHAZADO, false);
        ComprobanteElectronico nuevo = comprobante(21L, normal, CLAVE_NUEVA, LocalDateTime.of(2026, 11, 18, 10, 0, 0),
                EstadoComprobante.ERROR, true);
        // Un RECHAZADO posterior (p. ej. el propio comprobante tras la respuesta de Hacienda) no entra
        nuevo.setEstado(EstadoComprobante.RECHAZADO);

        List<Element> refs = referencias(raiz(generador.generarXmlFactura(nuevo)));

        assertThat(refs).hasSize(1);
        assertThat(texto(refs.get(0), "Numero")).isEqualTo(CLAVE_RECHAZADO);
    }

    @Test
    @DisplayName("criterio 8: la sustituta de una sustituta anulada referencia a la segunda, no a la primera")
    void sustitutaDeSustituta_referenciaALaSegunda() throws Exception {
        Factura segunda = sustituta(78, original, "15", "Primera sustituta");
        segunda.setAnulada(true);
        String claveSegunda = "50612112600310123456700100001010000000004100000004";
        LocalDateTime fechaSegunda = LocalDateTime.of(2026, 11, 10, 8, 15, 30);
        comprobante(30L, segunda, claveSegunda, fechaSegunda, EstadoComprobante.ACEPTADO, true);
        Factura tercera = sustituta(79, segunda, "07", "Segunda sustituta");
        ComprobanteElectronico nuevo = comprobante(null, tercera, CLAVE_NUEVA, LocalDateTime.of(2026, 11, 18, 10, 0, 0),
                EstadoComprobante.GENERADO, true);

        String xml = firmado(nuevo);

        assertThat(XmlValidator.erroresContraXsd(xml, TipoComprobanteElectronico.FACTURA_ELECTRONICA)).isEmpty();
        List<Element> refs = referencias(raiz(xml));
        assertThat(refs).hasSize(1);
        assertThat(texto(refs.get(0), "Numero")).isEqualTo(claveSegunda).isNotEqualTo(CLAVE_ORIGINAL);
        assertThat(texto(refs.get(0), "FechaEmisionIR")).isEqualTo("2026-11-10T08:15:30-06:00");
        assertThat(texto(refs.get(0), "Codigo")).isEqualTo("07");
    }

    // ── Apoyo ────────────────────────────────────────────────────────────────

    private String firmado(ComprobanteElectronico c) {
        return firma.firmarXml(generador.generarXmlFactura(c), p12.toString(), PIN);
    }

    private Factura factura(int id, String numero) {
        Factura f = new Factura();
        f.setIdFactura(id);
        f.setNumeroFactura(numero);
        f.setCliente(cliente);
        f.setMonedaFE(MonedaFE.CRC);
        f.setTipoCambio(BigDecimal.ONE);
        f.setAnulada(false);
        LineaFactura l = new LineaFactura();
        l.setIdLineaFactura(id);
        l.setNumeroLinea(1);
        l.setFactura(f);
        l.setProducto(gravado);
        l.setCantidad(2);
        l.setSubtotal(new BigDecimal("2260.00"));
        l.setPrecioUnitario(new BigDecimal("1130.00"));
        f.getLineas().add(l);
        f.setTotal(new BigDecimal("2260.00"));
        return f;
    }

    private Factura sustituta(int id, Factura sustituida, String codigo, String razon) {
        Factura f = factura(id, "FA-T039-" + id);
        f.setFacturaSustituida(sustituida);
        f.setCodigoSustitucion(codigo);
        f.setRazonSustitucion(razon);
        return f;
    }

    private ComprobanteElectronico comprobante(Long id, Factura f, String clave, LocalDateTime fecha,
                                               EstadoComprobante estado, boolean vigente) {
        ComprobanteElectronico c = ComprobanteElectronico.builder()
                .id(id).factura(f).empresa(empresa)
                .tipoComprobante(TipoComprobanteElectronico.FACTURA_ELECTRONICA)
                .claveNumerica(clave).consecutivo(clave.substring(21, 41)).fechaEmision(fecha)
                .estado(estado).vigente(vigente).build();
        if (id != null) {
            f.getComprobantes().add(c);
        }
        return c;
    }

    private static Producto producto() {
        Producto p = new Producto();
        p.setDescripcion("Producto gravado 13 %");
        ArticuloMaestro maestro = new ArticuloMaestro();
        maestro.setDescripcion("Producto gravado 13 %");
        maestro.setCodigoCabys("1234567890123");
        p.setArticuloMaestro(maestro);
        p.setGravado(true);
        p.setPorcentajeImpuesto(new BigDecimal("13"));
        p.setPrecioIncluyeIva(true);
        return p;
    }

    private static Element raiz(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        return doc.getDocumentElement();
    }

    private static List<Element> referencias(Element raiz) {
        List<Element> refs = new ArrayList<>();
        for (Node n = raiz.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && "InformacionReferencia".equals(e.getLocalName())) {
                refs.add(e);
            }
        }
        return refs;
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
        for (Node n = padre.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && nombre.equals(e.getLocalName())) {
                return e.getTextContent();
            }
        }
        throw new AssertionError("Sin elemento " + nombre);
    }
}
