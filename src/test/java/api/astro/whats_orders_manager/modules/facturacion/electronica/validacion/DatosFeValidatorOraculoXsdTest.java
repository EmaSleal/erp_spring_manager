package api.astro.whats_orders_manager.modules.facturacion.electronica.validacion;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.configuracion.enums.TipoIdentificacion;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.model.ProvinciaCostaRica;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl.XmlGeneratorServiceImpl;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.TelefonoFe;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.XmlValidator;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.facturacion.service.CalculadoraImpuestoService;
import api.astro.whats_orders_manager.modules.producto.model.ArticuloMaestro;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-038 (criterio 2): oráculo del validador contra el XSD real de la factura v4.4.
 *
 * <p>Método. El generador real (XmlGeneratorServiceImpl) produce una factura completa y válida. Como el generador omite
 * los opcionales inválidos (D10) y rechaza los obligatorios inválidos, no puede producir el XML con el dato crudo. Por
 * eso cada caso reemplaza en esa factura el bloque Receptor (o Emisor) por uno armado en esta prueba con el valor tal
 * cual lo emitiría un generador sin validar (recortado, sin guiones en la identificación, teléfono con los dígitos que
 * quedan tras quitar el 506, sin las comprobaciones de longitud), y valida el documento completo con
 * XmlValidator.erroresContraXsd (se ignora solo el error de la firma ausente). El veredicto del validador
 * (validarReceptor / validarEmisor vacío) se compara con el del XSD.
 *
 * <p>Dos tipos de caso: COINCIDEN (validador y XSD dan el mismo veredicto) y SOLO_ANEXO (el XSD acepta el valor pero el
 * Anexo v4.4 lo rechaza, así que el validador lo marca: el XSD no codifica la regla). Los SOLO_ANEXO son: longitud de
 * la identificación por tipo y cero inicial (el XSD solo fija maxLength 20), otras señas del receptor de 161 a 250
 * (el XSD admite 250; el Anexo, 160 para el receptor), patrón de correo (el XSD solo fija maxLength 160), existencia
 * de provincia, cantón y distrito en el catálogo (el XSD solo pide 1 y 2 dígitos), teléfono de 1 a 7 dígitos con valor
 * de al menos 100 (el XSD no exige 8), y código de actividad de 6 caracteres que no sea 6 dígitos ni «NNNN.N», p. ej. «ABCDEF» (el XSD solo pide 6
 * caracteres: minLength y maxLength 6, sin patrón). «4773.0» es el formato de /fe/ae y es válido.
 * Teléfono 0000000000: lo rechaza el XSD porque NumTelefono es xs:integer con minInclusive 100 y ese valor es 0.
 */
@DisplayName("T-038 — oráculo: el validador de datos FE contra el XSD real")
class DatosFeValidatorOraculoXsdTest {

    private enum Veredicto { COINCIDEN, SOLO_ANEXO }

    private record Caso(String nombre, Cliente cliente, Veredicto veredicto, boolean xsdAcepta) {
        @Override
        public String toString() {
            return nombre;
        }
    }

    private record CasoEmisor(String nombre, ConfiguracionEmpresa empresa, Veredicto veredicto, boolean xsdAcepta) {
        @Override
        public String toString() {
            return nombre;
        }
    }

    private static final String CLAVE_50 = "50617112600310123456700100001010000000001100000001";
    private static final DatosFeValidator VALIDADOR = CatalogoUbicacionDePrueba.validador();

    private String facturaBase;

    @BeforeEach
    void generarFacturaBase() {
        ConfiguracionEmpresa empresa = DatosFeDePrueba.emisor(ConfiguracionEmpresa.builder().idConfiguracion(1)
            .razonSocial("EMPRESA ORACULO S.A.").numeroIdentificacion("3101234567").codigoActividad("4773.0").build());
        Factura factura = new Factura();
        factura.setIdFactura(3);
        factura.setCliente(DatosFeDePrueba.cliente(clienteValido()));
        Producto p = new Producto();
        p.setDescripcion("Producto");
        ArticuloMaestro maestro = new ArticuloMaestro();
        maestro.setDescripcion("Producto");
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
        factura.getLineas().add(linea);
        facturaBase = new XmlGeneratorServiceImpl(new CalculadoraImpuestoService(null), VALIDADOR)
            .generarXmlFactura(ComprobanteElectronico.builder().claveNumerica(CLAVE_50)
                .fechaEmision(LocalDateTime.of(2026, 11, 17, 9, 0)).empresa(empresa).factura(factura).build());
        assertThat(erroresSinFirma(facturaBase)).as("la factura base debe ser válida").isEmpty();
    }

    @Test
    @DisplayName("el oráculo se sostiene solo: la factura base con su emisor y receptor válidos pasa el XSD y el validador")
    void baseValida() {
        assertThat(VALIDADOR.validarReceptor(clienteValido())).isEmpty();
    }

    // ── Receptor ────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0}")
    @MethodSource("casosReceptor")
    void receptor(Caso caso) {
        String xml = facturaBase.replaceFirst("(?s)<Receptor>.*?</Receptor>", Matcher.quoteReplacement(receptorCrudo(caso.cliente())))
            .replaceFirst("</CodigoActividadEmisor>", Matcher.quoteReplacement("</CodigoActividadEmisor>"
                + actividadReceptorCruda(caso.cliente())));
        boolean xsd = erroresSinFirma(xml).isEmpty();
        boolean validador = VALIDADOR.validarReceptor(caso.cliente()).isEmpty();

        assertThat(xsd).as("veredicto del XSD").isEqualTo(caso.xsdAcepta());
        if (caso.veredicto() == Veredicto.COINCIDEN) {
            assertThat(validador).as("validador == XSD").isEqualTo(xsd);
        } else {
            assertThat(xsd).as("el XSD acepta (regla solo del Anexo)").isTrue();
            assertThat(validador).as("el validador la rechaza por el Anexo").isFalse();
        }
    }

    static Stream<Caso> casosReceptor() {
        List<Caso> c = new java.util.ArrayList<>();
        // nombre (3 a 100)
        c.add(coinciden("nombre válido de 3", cli(x -> x.setNombre("abc")), true));
        c.add(coinciden("nombre válido de 100", cli(x -> x.setNombre("a".repeat(100))), true));
        c.add(coinciden("nombre de 2", cli(x -> x.setNombre("ab")), false));
        c.add(coinciden("nombre de 101", cli(x -> x.setNombre("a".repeat(101))), false));
        c.add(coinciden("nombre en blanco", cli(x -> x.setNombre("   ")), false));
        // identificación
        c.add(coinciden("física de 9 dígitos", cli(x -> { }), true));
        c.add(soloAnexo("física de 8 dígitos", cli(x -> x.setNumeroIdentificacion("10999099"))));
        c.add(soloAnexo("física con cero inicial", cli(x -> x.setNumeroIdentificacion("009990999"))));
        c.add(soloAnexo("física de 10 dígitos", cli(x -> x.setNumeroIdentificacion("1099909990"))));
        c.add(coinciden("física con guiones se limpia", cli(x -> x.setNumeroIdentificacion("1-0999-0999")), true));
        c.add(coinciden("jurídica de 10", cli(x -> { x.setTipoIdentificacion(TipoIdentificacion.CEDULA_JURIDICA);
            x.setNumeroIdentificacion("3101234567"); }), true));
        c.add(soloAnexo("jurídica de 9", cli(x -> { x.setTipoIdentificacion(TipoIdentificacion.CEDULA_JURIDICA);
            x.setNumeroIdentificacion("310123456"); })));
        c.add(coinciden("DIMEX de 12", cli(x -> { x.setTipoIdentificacion(TipoIdentificacion.DIMEX);
            x.setNumeroIdentificacion("123456789012"); }), true));
        c.add(soloAnexo("DIMEX de 10", cli(x -> { x.setTipoIdentificacion(TipoIdentificacion.DIMEX);
            x.setNumeroIdentificacion("1234567890"); })));
        c.add(soloAnexo("DIMEX con cero inicial", cli(x -> { x.setTipoIdentificacion(TipoIdentificacion.DIMEX);
            x.setNumeroIdentificacion("012345678901"); })));
        c.add(coinciden("NITE de 10", cli(x -> { x.setTipoIdentificacion(TipoIdentificacion.NITE);
            x.setNumeroIdentificacion("1234567890"); }), true));
        c.add(coinciden("número de 21 dígitos", cli(x -> x.setNumeroIdentificacion("1".repeat(21))), false));
        // teléfono (opcional)
        c.add(coinciden("teléfono válido", cli(x -> x.setTelefono("+506 8888-7777")), true));
        c.add(coinciden("teléfono 0000000000 (valor 0 < 100)", cli(x -> x.setTelefono("0000000000")), false));
        c.add(coinciden("teléfono 00000100 (valor 100)", cli(x -> x.setTelefono("00000100")), true));
        c.add(soloAnexo("teléfono de 7 dígitos", cli(x -> x.setTelefono("8888777"))));
        c.add(coinciden("teléfono de 21 dígitos", cli(x -> x.setTelefono("1".repeat(21))), false));
        c.add(coinciden("teléfono en blanco = ausente", cli(x -> x.setTelefono("  ")), true));
        // correo (opcional)
        c.add(coinciden("correo válido", cli(x -> x.setEmail("a@b.com")), true));
        c.add(coinciden("correo en blanco = ausente", cli(x -> x.setEmail("")), true));
        c.add(soloAnexo("correo sin arroba", cli(x -> x.setEmail("sin-arroba"))));
        c.add(soloAnexo("correo con espacio interno", cli(x -> x.setEmail("a b@c.com"))));
        c.add(coinciden("correo de 160", cli(x -> x.setEmail("a".repeat(151) + "@mail.com")), true));
        c.add(coinciden("correo de 161", cli(x -> x.setEmail("a".repeat(152) + "@mail.com")), false));
        // ubicación (opcional)
        c.add(coinciden("ubicación válida", cli(x -> ubicacion(x, "1", "01", "01", "Frente a la iglesia")), true));
        c.add(coinciden("cantón 00", cli(x -> ubicacion(x, "1", "00", "01", "Frente a la iglesia")), false));
        c.add(coinciden("distrito de un dígito", cli(x -> ubicacion(x, "1", "01", "1", "Frente a la iglesia")), false));
        c.add(coinciden("provincia 0", cli(x -> ubicacion(x, "0", "01", "01", "Frente a la iglesia")), false));
        c.add(soloAnexo("provincia 9 fuera del catálogo", cli(x -> ubicacion(x, "9", "01", "01", "Frente a la iglesia"))));
        c.add(coinciden("otras señas de 4", cli(x -> ubicacion(x, "1", "01", "01", "1234")), false));
        c.add(coinciden("otras señas de 160", cli(x -> ubicacion(x, "1", "01", "01", "x".repeat(160))), true));
        c.add(soloAnexo("otras señas de 161", cli(x -> ubicacion(x, "1", "01", "01", "x".repeat(161)))));
        c.add(soloAnexo("otras señas de 200", cli(x -> ubicacion(x, "1", "01", "01", "x".repeat(200)))));
        c.add(coinciden("otras señas de 251", cli(x -> ubicacion(x, "1", "01", "01", "x".repeat(251))), false));
        // código de actividad del receptor (opcional)
        c.add(coinciden("actividad de 6 dígitos", cli(x -> x.setCodigoActividad("620100")), true));
        c.add(coinciden("actividad de 5", cli(x -> x.setCodigoActividad("62010")), false));
        c.add(coinciden("actividad «4773.0» (formato /fe/ae)", cli(x -> x.setCodigoActividad("4773.0")), true));
        c.add(coinciden("actividad «477300»", cli(x -> x.setCodigoActividad("477300")), true));
        c.add(coinciden("actividad «47» (el XSD exige 6)", cli(x -> x.setCodigoActividad("47")), false));
        c.add(coinciden("actividad de 7 «1234567»", cli(x -> x.setCodigoActividad("1234567")), false));
        c.add(soloAnexo("actividad «ABCDEF» (6 caracteres, regla documentada)", cli(x -> x.setCodigoActividad("ABCDEF"))));
        return c.stream();
    }

    // ── Emisor ──────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0}")
    @MethodSource("casosEmisor")
    void emisor(CasoEmisor caso) {
        String xml = facturaBase.replaceFirst("(?s)<Emisor>.*?</Emisor>", Matcher.quoteReplacement(emisorCrudo(caso.empresa())))
            .replaceFirst("<CodigoActividadEmisor>.*?</CodigoActividadEmisor>", Matcher.quoteReplacement(
                "<CodigoActividadEmisor>" + nvl(caso.empresa().getCodigoActividad()).trim() + "</CodigoActividadEmisor>"));
        boolean xsd = erroresSinFirma(xml).isEmpty();
        boolean validador = VALIDADOR.validarEmisor(caso.empresa()).isEmpty();

        assertThat(xsd).as("veredicto del XSD").isEqualTo(caso.xsdAcepta());
        if (caso.veredicto() == Veredicto.COINCIDEN) {
            assertThat(validador).as("validador == XSD").isEqualTo(xsd);
        } else {
            assertThat(xsd).as("el XSD acepta (regla solo del Anexo o del negocio)").isTrue();
            assertThat(validador).as("el validador la rechaza").isFalse();
        }
    }

    static Stream<CasoEmisor> casosEmisor() {
        List<CasoEmisor> c = new java.util.ArrayList<>();
        c.add(emi("emisor válido", e -> { }, Veredicto.COINCIDEN, true));
        c.add(emi("razón social de 5", e -> e.setRazonSocial("abcde"), Veredicto.COINCIDEN, true));
        c.add(emi("razón social de 4", e -> e.setRazonSocial("abcd"), Veredicto.COINCIDEN, false));
        c.add(emi("razón social de 101", e -> e.setRazonSocial("a".repeat(101)), Veredicto.COINCIDEN, false));
        c.add(emi("jurídica de 9 dígitos", e -> e.setNumeroIdentificacion("310123456"), Veredicto.SOLO_ANEXO, true));
        c.add(emi("otras señas de 4", e -> e.setOtrasSenas("1234"), Veredicto.COINCIDEN, false));
        c.add(emi("otras señas de 250", e -> e.setOtrasSenas("x".repeat(250)), Veredicto.COINCIDEN, true));
        c.add(emi("otras señas de 251", e -> e.setOtrasSenas("x".repeat(251)), Veredicto.COINCIDEN, false));
        c.add(emi("distrito 00", e -> e.setDistrito("00"), Veredicto.COINCIDEN, false));
        c.add(emi("provincia 9 fuera del catálogo", e -> e.setCodigoProvincia("9"), Veredicto.SOLO_ANEXO, true));
        c.add(emi("teléfono 0000000000", e -> e.setTelefono("0000000000"), Veredicto.COINCIDEN, false));
        c.add(emi("correo sin arroba", e -> e.setEmail("sin-arroba"), Veredicto.SOLO_ANEXO, true));
        c.add(emi("actividad «4773.0»", e -> e.setCodigoActividad("4773.0"), Veredicto.COINCIDEN, true));
        c.add(emi("actividad «477300»", e -> e.setCodigoActividad("477300"), Veredicto.COINCIDEN, true));
        c.add(emi("actividad «47» (el XSD exige 6)", e -> e.setCodigoActividad("47"), Veredicto.COINCIDEN, false));
        c.add(emi("actividad de 7 «1234567»", e -> e.setCodigoActividad("1234567"), Veredicto.COINCIDEN, false));
        c.add(emi("actividad «ABCDEF» (regla documentada)", e -> e.setCodigoActividad("ABCDEF"), Veredicto.SOLO_ANEXO, true));
        c.add(emi("correo de 161", e -> e.setEmail("a".repeat(152) + "@mail.com"), Veredicto.COINCIDEN, false));
        return c.stream();
    }

    // ── Construcción del XML crudo (lo que emitiría un generador sin validar) ───────────────

    private static String receptorCrudo(Cliente c) {
        String numero = DatosFeValidator.numeroIdentificacion(c);
        StringBuilder x = new StringBuilder("<Receptor>");
        x.append("<Nombre>").append(esc(c.getNombre().trim())).append("</Nombre>");
        x.append("<Identificacion><Tipo>").append(c.getTipoIdentificacion().getCodigo()).append("</Tipo><Numero>")
            .append(numero).append("</Numero></Identificacion>");
        String prov = c.getProvincia() != null ? c.getProvincia().getCodigo().trim() : "";
        String otras = c.getOtrasSenas() != null ? c.getOtrasSenas().trim() : "";
        if (!prov.isEmpty() || !otras.isEmpty()) {
            x.append("<Ubicacion><Provincia>").append(prov).append("</Provincia><Canton>").append(c.getCanton())
                .append("</Canton><Distrito>").append(c.getDistrito()).append("</Distrito><OtrasSenas>")
                .append(esc(otras)).append("</OtrasSenas></Ubicacion>");
        }
        telefonoCrudo(x, c.getTelefono());
        if (c.getEmail() != null && !c.getEmail().isBlank()) {
            x.append("<CorreoElectronico>").append(esc(c.getEmail().trim())).append("</CorreoElectronico>");
        }
        return x.append("</Receptor>").toString();
    }

    private static String actividadReceptorCruda(Cliente c) {
        return c.getCodigoActividad() == null || c.getCodigoActividad().isBlank() ? ""
            : "<CodigoActividadReceptor>" + c.getCodigoActividad().trim() + "</CodigoActividadReceptor>";
    }

    private static String emisorCrudo(ConfiguracionEmpresa e) {
        StringBuilder x = new StringBuilder("<Emisor>");
        x.append("<Nombre>").append(esc(e.getRazonSocial().trim())).append("</Nombre>");
        x.append("<Identificacion><Tipo>").append(e.getTipoIdentificacion().getCodigo()).append("</Tipo><Numero>")
            .append(DatosFeValidator.numeroLimpio(e.getNumeroIdentificacion())).append("</Numero></Identificacion>");
        x.append("<NombreComercial>Comercial</NombreComercial>");
        x.append("<Ubicacion><Provincia>").append(nvl(e.getCodigoProvincia())).append("</Provincia><Canton>")
            .append(nvl(e.getCanton())).append("</Canton><Distrito>").append(nvl(e.getDistrito()))
            .append("</Distrito><OtrasSenas>").append(esc(nvl(e.getOtrasSenas()).trim())).append("</OtrasSenas></Ubicacion>");
        telefonoCrudo(x, e.getTelefono());
        x.append("<CorreoElectronico>").append(esc(nvl(e.getEmail()).trim())).append("</CorreoElectronico>");
        return x.append("</Emisor>").toString();
    }

    /** Telefono tal cual sin la regla de longitud ni de valor: dígitos, sin el 506 inicial si quedan al menos 8. */
    private static void telefonoCrudo(StringBuilder x, String telefono) {
        String d = telefono == null ? "" : telefono.replaceAll("[^0-9]", "");
        if (d.startsWith(TelefonoFe.CODIGO_PAIS) && d.length() - 3 >= 8) {
            d = d.substring(3);
        }
        if (!d.isEmpty()) {
            x.append("<Telefono><CodigoPais>506</CodigoPais><NumTelefono>").append(d).append("</NumTelefono></Telefono>");
        }
    }

    // ── Apoyo ───────────────────────────────────────────────────────────────

    private static List<String> erroresSinFirma(String xml) {
        return XmlValidator.erroresContraXsd(xml, TipoComprobanteElectronico.FACTURA_ELECTRONICA).stream()
            .filter(e -> !e.contains("xmldsig#\":Signature")).toList();
    }

    private static Cliente clienteValido() {
        Cliente c = new Cliente();
        c.setNombre("Cliente Oráculo");
        c.setTipoIdentificacion(TipoIdentificacion.CEDULA_FISICA);
        c.setNumeroIdentificacion("109990999");
        return c;
    }

    private static Cliente cli(java.util.function.Consumer<Cliente> cambio) {
        Cliente c = clienteValido();
        cambio.accept(c);
        return c;
    }

    private static void ubicacion(Cliente c, String provincia, String canton, String distrito, String otras) {
        ProvinciaCostaRica p = new ProvinciaCostaRica();
        p.setCodigo(provincia);
        c.setProvincia(p);
        c.setCanton(canton);
        c.setDistrito(distrito);
        c.setOtrasSenas(otras);
    }

    private static Caso coinciden(String nombre, Cliente c, boolean xsdAcepta) {
        return new Caso(nombre, c, Veredicto.COINCIDEN, xsdAcepta);
    }

    private static Caso soloAnexo(String nombre, Cliente c) {
        return new Caso(nombre, c, Veredicto.SOLO_ANEXO, true);
    }

    private static CasoEmisor emi(String nombre, java.util.function.Consumer<ConfiguracionEmpresa> cambio, Veredicto v,
                                  boolean xsdAcepta) {
        ConfiguracionEmpresa e = DatosFeDePrueba.emisor(ConfiguracionEmpresa.builder().razonSocial("EMPRESA ORACULO S.A.")
            .numeroIdentificacion("3101234567").build());
        cambio.accept(e);
        return new CasoEmisor(nombre, e, v, xsdAcepta);
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
