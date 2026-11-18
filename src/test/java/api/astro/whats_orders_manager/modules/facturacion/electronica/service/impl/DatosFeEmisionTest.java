package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.configuracion.enums.TipoIdentificacion;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.model.ProvinciaCostaRica;
import api.astro.whats_orders_manager.modules.configuracion.service.ConfiguracionEmpresaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.DatosFeIncompletosException;
import api.astro.whats_orders_manager.modules.facturacion.electronica.mapper.ComprobanteElectronicoMapper;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConfiguracionHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.ClaveNumericaGenerator;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.XmlValidator;
import api.astro.whats_orders_manager.modules.facturacion.electronica.validacion.CatalogoUbicacionDePrueba;
import api.astro.whats_orders_manager.modules.facturacion.electronica.validacion.DatosFeDePrueba;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.service.CalculadoraImpuestoService;
import api.astro.whats_orders_manager.modules.producto.model.ArticuloMaestro;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T-038 (criterios 8 a 11): la emisión bloquea con 409 (DatosFeIncompletosException) los obligatorios inválidos del
 * receptor y del emisor antes de tomar el consecutivo; los opcionales inválidos del receptor se omiten del XML, que
 * valida contra el XSD real; el emisor no se inventa.
 */
@DisplayName("T-038 — emisión: datos FE obligatorios bloquean, opcionales inválidos se omiten")
class DatosFeEmisionTest {

    private static final Instant AHORA = Instant.parse("2026-11-17T15:00:00Z");
    private static final String CLAVE_50 = "50617112600310123456700100001010000000001100000001";

    private final ComprobanteElectronicoRepository repository = mock(ComprobanteElectronicoRepository.class);
    private final FacturaRepository facturaRepository = mock(FacturaRepository.class);
    private final ConfiguracionHaciendaService configuracionService = mock(ConfiguracionHaciendaService.class);
    private final ConfiguracionEmpresaService empresaService = mock(ConfiguracionEmpresaService.class);
    private final XmlGeneratorServiceImpl generador = new XmlGeneratorServiceImpl(new CalculadoraImpuestoService(null),
            CatalogoUbicacionDePrueba.validador());

    private ConfiguracionEmpresa empresa;
    private ConfiguracionHacienda config;
    private Factura factura;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        empresa = ConfiguracionEmpresa.builder().idConfiguracion(1).razonSocial("EMPRESA T038 S.A.")
                .numeroIdentificacion("3101234567").codigoActividad("4773.0").build();
        DatosFeDePrueba.emisor(empresa);
        config = ConfiguracionHacienda.builder().id(3L).empresa(empresa).ambiente(AmbienteHacienda.SANDBOX)
                .activa(true).consecutivoFactura(28L).build();
        when(empresaService.obtenerConfiguracion()).thenReturn(Optional.of(empresa));
        when(configuracionService.obtenerConfiguracionActiva()).thenReturn(config);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        cliente = new Cliente();
        cliente.setIdCliente(1);
        cliente.setNombre("Cliente T038");
        cliente.setTipoIdentificacion(TipoIdentificacion.CEDULA_FISICA);
        cliente.setNumeroIdentificacion("109990999");

        factura = new Factura();
        factura.setIdFactura(3);
        factura.setCliente(cliente);
        factura.getLineas().add(linea());
        when(facturaRepository.findById(3)).thenReturn(Optional.of(factura));
    }

    // ── Etapa a: 409 sin consumir consecutivo ───────────────────────────────

    @Test
    @DisplayName("cliente sin identificación → 409 que nombra los campos, sin consecutivo ni comprobante")
    void clienteSinIdentificacion() {
        cliente.setNumeroIdentificacion(null);
        cliente.setTipoIdentificacion(null);

        assertThatThrownBy(() -> emitir())
                .isInstanceOf(DatosFeIncompletosException.class)
                .hasMessage("El cliente Cliente T038 no tiene los datos de facturación electrónica completos: identificación");
        sinConsumo();
    }

    @Test
    @DisplayName("cliente con cédula física de 8 dígitos → 409 sin consecutivo")
    void fisicaDeOchoDigitos() {
        cliente.setNumeroIdentificacion("10999099");

        assertThatThrownBy(() -> emitir()).isInstanceOf(DatosFeIncompletosException.class)
                .hasMessageContaining("identificación");
        sinConsumo();
    }

    @Test
    @DisplayName("cliente sin nombre → 409 que lo nombra por su id")
    void clienteSinNombre() {
        cliente.setNombre(" ");

        assertThatThrownBy(() -> emitir()).isInstanceOf(DatosFeIncompletosException.class)
                .hasMessage("El cliente #1 no tiene los datos de facturación electrónica completos: nombre");
        sinConsumo();
    }

    @Test
    @DisplayName("emisor sin distrito → 409 que remite a Configuración → Empresa, sin consecutivo")
    void emisorSinDistrito() {
        empresa.setDistrito(null);

        assertThatThrownBy(() -> emitir()).isInstanceOf(DatosFeIncompletosException.class)
                .hasMessageContaining("distrito").hasMessageContaining("Configuración → Empresa");
        sinConsumo();
    }

    @Test
    @DisplayName("emisor sin tipo de identificación, teléfono ni correo → 409 con todos los campos")
    void emisorSinDatos() {
        empresa.setTipoIdentificacion(null);
        empresa.setTelefono(null);
        empresa.setEmail(null);

        assertThatThrownBy(() -> emitir()).isInstanceOf(DatosFeIncompletosException.class)
                .hasMessageContaining("identificación").hasMessageContaining("teléfono")
                .hasMessageContaining("correo");
        sinConsumo();
    }

    // ── Opcionales inválidos: se omiten, la factura sale ───────────────────

    @Test
    @DisplayName("teléfono 0000000000, correo vacío y otras señas de 200 → XML válido sin Telefono, CorreoElectronico ni Ubicacion; se emite")
    void opcionalesInvalidos_seOmiten() throws Exception {
        cliente.setTelefono("0000000000");
        cliente.setEmail("");
        cliente.setProvincia(provincia("1"));
        cliente.setCanton("01");
        cliente.setDistrito("01");
        cliente.setOtrasSenas("x".repeat(200));
        cliente.setCodigoActividad("4773.0");

        ComprobanteElectronico emitido = emitir();

        assertThat(config.getConsecutivoFactura()).isEqualTo(29L); // se tomó el consecutivo: la factura sale
        assertThat(emitido.getXmlComprobante()).isNotEmpty();
        assertThat(erroresSinFirma(emitido.getXmlComprobante())).isEmpty();
        Element receptor = hijo(raiz(emitido.getXmlComprobante()), "Receptor");
        assertThat(nombresDeHijos(receptor)).containsExactly("Nombre", "Identificacion");
        assertThat(emitido.getXmlComprobante()).doesNotContain("CodigoActividadReceptor");
    }

    @Test
    @DisplayName("receptor con todo válido → Ubicacion, Telefono, CorreoElectronico y CodigoActividadReceptor tras el del emisor")
    void receptorCompleto() throws Exception {
        cliente.setTelefono("+506 8888-7777");
        cliente.setEmail("  cli&ente@t038.example ");
        cliente.setProvincia(provincia("2"));
        cliente.setCanton("03");
        cliente.setDistrito("04");
        cliente.setOtrasSenas("Frente al parque <central>");
        cliente.setCodigoActividad("620100");

        String xml = emitir().getXmlComprobante();

        assertThat(erroresSinFirma(xml)).isEmpty();
        Element raiz = raiz(xml);
        assertThat(nombresDeHijos(raiz)).containsSubsequence("CodigoActividadEmisor", "CodigoActividadReceptor",
                "NumeroConsecutivo");
        Element receptor = hijo(raiz, "Receptor");
        assertThat(nombresDeHijos(receptor)).containsExactly("Nombre", "Identificacion", "Ubicacion", "Telefono",
                "CorreoElectronico");
        assertThat(hijo(receptor, "CorreoElectronico").getTextContent()).isEqualTo("cli&ente@t038.example");
        assertThat(xml).contains("cli&amp;ente@t038.example").contains("Frente al parque &lt;central&gt;");
    }

    @Test
    @DisplayName("la identificación del receptor sale sin guiones ni espacios")
    void identificacionSinGuiones() throws Exception {
        cliente.setNumeroIdentificacion("1-0999-0999");

        Element id = hijo(hijo(raiz(emitir().getXmlComprobante()), "Receptor"), "Identificacion");

        assertThat(hijo(id, "Numero").getTextContent()).isEqualTo("109990999");
        assertThat(hijo(id, "Tipo").getTextContent()).isEqualTo("01");
    }

    // ── Emisor: nada inventado (defensa del generador) ──────────────────────

    @Test
    @DisplayName("el generador, sin pasar por la emisión, falla localmente si el emisor no tiene distrito: no inventa «01»")
    void generadorNoInventaElEmisor() {
        empresa.setDistrito(null);

        assertThatThrownBy(() -> generador.generarXmlFactura(comprobante()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("distrito");
    }

    @Test
    @DisplayName("barrio de 3 caracteres (código) no se emite; de 5 a 50 sí; el correo del emisor se escapa")
    void barrioYCorreoDelEmisor() throws Exception {
        empresa.setBarrio("001");
        empresa.setEmail("a&b@empresa.example");
        Element ubicacion = hijo(hijo(raiz(generador.generarXmlFactura(comprobante())), "Emisor"), "Ubicacion");
        assertThat(nombresDeHijos(ubicacion)).doesNotContain("Barrio");

        empresa.setBarrio("Barrio Los Yoses");
        String xml = generador.generarXmlFactura(comprobante());
        assertThat(nombresDeHijos(hijo(hijo(raiz(xml), "Emisor"), "Ubicacion"))).contains("Barrio");
        assertThat(xml).contains("a&amp;b@empresa.example");
    }

    // ── Apoyo ───────────────────────────────────────────────────────────────

    private ComprobanteElectronico emitir() {
        ComprobanteElectronicoServiceImpl service = new ComprobanteElectronicoServiceImpl(repository,
                facturaRepository, null, configuracionService, empresaService, generador, null, null,
                new ClaveNumericaGenerator(), mock(ComprobanteElectronicoMapper.class), null, null,
                Clock.fixed(AHORA, ZoneOffset.UTC), null, null, null, (c, t) -> c.siguienteConsecutivo(t),
                CatalogoUbicacionDePrueba.validador());
        service.generarDesdeFactura(3L);
        ArgumentCaptor<ComprobanteElectronico> guardado = ArgumentCaptor.forClass(ComprobanteElectronico.class);
        verify(repository).save(guardado.capture());
        return guardado.getValue();
    }

    /** Errores del XSD salvo la firma, que se agrega después (el XML del generador aún no está firmado). */
    private static List<String> erroresSinFirma(String xml) {
        return XmlValidator.erroresContraXsd(xml, TipoComprobanteElectronico.FACTURA_ELECTRONICA).stream()
                .filter(e -> !e.contains("xmldsig#\":Signature")).toList();
    }

    private void sinConsumo() {
        assertThat(config.getConsecutivoFactura()).isEqualTo(28L);
        verify(repository, never()).save(any());
        verify(repository, never()).saveAndFlush(any());
    }

    private ComprobanteElectronico comprobante() {
        return ComprobanteElectronico.builder().claveNumerica(CLAVE_50).fechaEmision(LocalDateTime.of(2026, 11, 17, 9, 0))
                .empresa(empresa).factura(factura).build();
    }

    private static ProvinciaCostaRica provincia(String codigo) {
        ProvinciaCostaRica p = new ProvinciaCostaRica();
        p.setCodigo(codigo);
        return p;
    }

    private LineaFactura linea() {
        Producto p = new Producto();
        p.setDescripcion("Producto T-038");
        ArticuloMaestro maestro = new ArticuloMaestro();
        maestro.setDescripcion("Producto T-038");
        maestro.setCodigoCabys("1234567890123");
        p.setArticuloMaestro(maestro);
        p.setGravado(true);
        p.setPorcentajeImpuesto(new BigDecimal("13"));
        p.setPrecioIncluyeIva(true);
        LineaFactura linea = new LineaFactura();
        linea.setFactura(factura);
        linea.setProducto(p);
        linea.setCantidad(1);
        linea.setSubtotal(new BigDecimal("1130.00"));
        return linea;
    }

    private static Element raiz(String xml) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(false);
        Document d = f.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        return d.getDocumentElement();
    }

    private static Element hijo(Element padre, String nombre) {
        NodeList hijos = padre.getChildNodes();
        for (int i = 0; i < hijos.getLength(); i++) {
            if (hijos.item(i) instanceof Element e && e.getTagName().equals(nombre)) {
                return e;
            }
        }
        return null;
    }

    private static List<String> nombresDeHijos(Element padre) {
        List<String> nombres = new ArrayList<>();
        NodeList hijos = padre.getChildNodes();
        for (int i = 0; i < hijos.getLength(); i++) {
            Node n = hijos.item(i);
            if (n instanceof Element e) {
                nombres.add(e.getTagName());
            }
        }
        return nombres;
    }
}
