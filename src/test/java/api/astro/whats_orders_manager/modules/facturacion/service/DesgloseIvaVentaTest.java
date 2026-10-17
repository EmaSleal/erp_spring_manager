package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.service.ParametroSistemaService;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.DetalleAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.model.ParametroContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.AsientoContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.DetalleAsientoRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl.XmlGeneratorServiceImpl;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFacturaR;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.repository.LineaFacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.service.impl.LineaFacturaServiceImpl;
import api.astro.whats_orders_manager.modules.producto.model.ArticuloMaestro;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.repository.ProductoRepository;
import api.astro.whats_orders_manager.modules.seguridad.service.impl.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T-006 — ADR-009: factura, asiento de venta (ADR-008) y XML FE usan el mismo desglose
 * de IVA por línea y coinciden al céntimo (criterios 3, 5 y 6).
 *
 * LineaFacturaServiceImpl, AsientoContableService, CalculadoraImpuestoService y
 * XmlGeneratorServiceImpl son reales; solo los repositorios están simulados.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Desglose de IVA — factura, asiento de venta y XML coinciden (ADR-009)")
class DesgloseIvaVentaTest {

    private static final String CLAVE_50 = "50601042612345678901200110000011000010000100000001";
    private static final int ID_FACTURA = 77;

    @Mock private LineaFacturaRepository lineaFacturaRepository;
    @Mock private FacturaRepository facturaRepository;
    @Mock private UserDetailsServiceImpl userDetailsService;
    @Mock private ProductoRepository productoRepository;
    @Mock private ParametroSistemaService parametroSistemaService;
    @Mock private AsientoContableRepository asientoRepository;
    @Mock private DetalleAsientoRepository detalleRepository;
    @Mock private CuentaContableRepository cuentaRepository;
    @Mock private ParametroContableRepository parametroContableRepository;

    private CalculadoraImpuestoService calculadora;
    private LineaFacturaServiceImpl lineaFacturaService;
    private XmlGeneratorServiceImpl xmlGenerator;
    private Factura factura;
    private final Map<String, CuentaContable> cuentas = new HashMap<>();
    private final Map<Integer, Producto> productos = new HashMap<>();

    @BeforeEach
    void setUp() {
        // Tarifa general: parámetro de sistema FACTURA_IVA_PORCENTAJE = 13
        when(parametroSistemaService.obtenerValorDecimal(eq(CalculadoraImpuestoService.CLAVE_TARIFA_GENERAL), any()))
                .thenReturn(new BigDecimal("13"));
        calculadora = new CalculadoraImpuestoService(parametroSistemaService);

        AsientoContableService asientoService = new AsientoContableService(
                asientoRepository, detalleRepository, cuentaRepository, parametroContableRepository);
        lineaFacturaService = new LineaFacturaServiceImpl();
        ReflectionTestUtils.setField(lineaFacturaService, "lineaFacturaRepository", lineaFacturaRepository);
        ReflectionTestUtils.setField(lineaFacturaService, "facturaRepository", facturaRepository);
        ReflectionTestUtils.setField(lineaFacturaService, "userDetailsService", userDetailsService);
        ReflectionTestUtils.setField(lineaFacturaService, "asientoContableService", asientoService);
        ReflectionTestUtils.setField(lineaFacturaService, "productoRepository", productoRepository);
        ReflectionTestUtils.setField(lineaFacturaService, "totalesFacturaService",
                new TotalesFacturaService(calculadora, productoRepository));
        xmlGenerator = new XmlGeneratorServiceImpl(calculadora);

        long id = 1;
        for (String clave : List.of("VENTA_CXC_CLIENTES", "VENTA_INGRESOS", "VENTA_IVA_POR_PAGAR")) {
            CuentaContable cuenta = new CuentaContable();
            cuenta.setIdCuenta(id++);
            cuenta.setCodigo(clave);
            cuenta.setNombre(clave);
            cuenta.setActiva(true);
            cuenta.setAceptaMovimientos(true);
            cuentas.put(clave, cuenta);
            ParametroContable param = new ParametroContable();
            param.setClave(clave);
            param.setCuentaContable(cuenta);
            when(parametroContableRepository.findByClave(clave)).thenReturn(Optional.of(param));
        }
        when(asientoRepository.findUltimoNumeroDelAnio(anyString())).thenReturn(Optional.empty());
        when(asientoRepository.save(any(AsientoContable.class))).thenAnswer(inv -> inv.getArgument(0));

        // Productos gravados: uno con porcentaje propio en el maestro, otro sin porcentaje (tarifa general)
        ArticuloMaestro maestro = new ArticuloMaestro();
        maestro.setDescripcion("Desinfectante");
        maestro.setCodigoCabys("3530100000100");
        maestro.setGravado(true);
        maestro.setPorcentajeImpuesto(new BigDecimal("13.00"));
        productos.put(5, producto(5, maestro, true, null));
        productos.put(6, producto(6, null, true, null));
        productos.forEach((idProducto, p) -> when(productoRepository.findById(idProducto)).thenReturn(Optional.of(p)));

        Cliente cliente = new Cliente();
        cliente.setIdCliente(1);
        cliente.setNombre("Cliente Prueba");
        factura = new Factura();
        factura.setIdFactura(ID_FACTURA);
        factura.setNumeroFactura("F-" + ID_FACTURA);
        factura.setCliente(cliente);
        factura.setAnulada(false);
        factura.setMonedaFE(MonedaFE.CRC);
        factura.setTipoCambio(BigDecimal.ONE);
        factura.setSubtotal(BigDecimal.ZERO);
        factura.setIgv(BigDecimal.ZERO);
        factura.setTotal(BigDecimal.ZERO);
        // Persisted invoice: its @CreatedDate (fechaEmision) was set on insert (ADR-014)
        factura.setCreateDate(LocalDateTime.of(2026, 9, 15, 10, 0));

        when(userDetailsService.getCurrentUserID()).thenReturn(Optional.of(1));
        when(facturaRepository.findByIdFactura(ID_FACTURA)).thenReturn(Optional.of(factura));
    }

    // =========================================================================
    // Criterio 3 — updateLineas desglosa el IVA
    // =========================================================================

    @Test
    @DisplayName("criterio 3: líneas 1130 + 565 → subtotal 1500.00, igv 195.00, total 1695.00")
    void updateLineas_desglosaIva() {

        lineaFacturaService.updateLineas(lineasCriterio3());

        assertThat(factura.getSubtotal()).isEqualByComparingTo("1500.00");
        assertThat(factura.getIgv()).isEqualByComparingTo("195.00");
        assertThat(factura.getTotal()).isEqualByComparingTo("1695.00");
    }

    // =========================================================================
    // Criterio 5 — asiento de venta con IVA por pagar
    // =========================================================================

    @Test
    @DisplayName("criterio 5: asiento DR CxC 1695.00 / CR Ingresos 1500.00 / CR IVA por pagar 195.00, cuadrado")
    void asientoVenta_registraIvaPorPagar() {

        lineaFacturaService.updateLineas(lineasCriterio3());

        AsientoContable asiento = ultimoAsientoGuardado();
        assertThat(asiento.getTipo()).isEqualTo(TipoAsiento.AUTOMATICO_VENTA);
        assertThat(asiento.getEstado()).isEqualTo(EstadoAsiento.CONTABILIZADO);
        assertThat(debe(asiento, "VENTA_CXC_CLIENTES")).isEqualByComparingTo("1695.00");
        assertThat(haber(asiento, "VENTA_INGRESOS")).isEqualByComparingTo("1500.00");
        assertThat(haber(asiento, "VENTA_IVA_POR_PAGAR")).isEqualByComparingTo("195.00");
        assertThat(asiento.getTotalDebe()).isEqualByComparingTo(asiento.getTotalHaber());
    }

    @Test
    @DisplayName("criterio 5: editar líneas de una factura con asiento → reversión + asiento nuevo desglosado")
    void editarLineas_reversionYAsientoNuevoDesglosado() {
        lineaFacturaService.updateLineas(lineasCriterio3());
        asignarIdsPersistidos(10, 11);
        AsientoContable vigente = ultimoAsientoGuardado();
        vigente.setIdAsiento(900L);

        when(asientoRepository.findAsientosVentaVigentes((long) ID_FACTURA)).thenReturn(List.of(vigente));
        clearInvocations(asientoRepository);

        // Se edita la primera línea: 2 × 1130 = 2260 → total 2825 = 2500 + 325
        lineaFacturaService.updateLineas(List.of(
                lineaR(10, 1, 5, 2, "1130.00", "2260.00"),
                lineaR(11, 2, 6, 5, "113.00", "565.00")));

        ArgumentCaptor<AsientoContable> captor = ArgumentCaptor.forClass(AsientoContable.class);
        verify(asientoRepository, atLeastOnce()).save(captor.capture());
        AsientoContable reversion = captor.getAllValues().stream()
                .filter(a -> a.getAsientoRevertido() == vigente).findFirst().orElseThrow();
        AsientoContable nuevo = captor.getAllValues().get(captor.getAllValues().size() - 1);

        assertThat(haber(reversion, "VENTA_CXC_CLIENTES")).isEqualByComparingTo("1695.00");
        assertThat(debe(reversion, "VENTA_INGRESOS")).isEqualByComparingTo("1500.00");
        assertThat(debe(reversion, "VENTA_IVA_POR_PAGAR")).isEqualByComparingTo("195.00");
        assertThat(nuevo.getAsientoRevertido()).isNull();
        assertThat(debe(nuevo, "VENTA_CXC_CLIENTES")).isEqualByComparingTo("2825.00");
        assertThat(haber(nuevo, "VENTA_INGRESOS")).isEqualByComparingTo("2500.00");
        assertThat(haber(nuevo, "VENTA_IVA_POR_PAGAR")).isEqualByComparingTo("325.00");
        assertThat(nuevo.getTotalDebe()).isEqualByComparingTo(nuevo.getTotalHaber());
    }

    // =========================================================================
    // Criterio 6 — XML FE con el mismo desglose
    // =========================================================================

    @Test
    @DisplayName("criterio 6: XML de la factura del criterio 3 → TotalComprobante 1695.00 = factura.total, TotalImpuesto 195.00")
    void xml_coincideConLaFactura() throws Exception {
        lineaFacturaService.updateLineas(lineasCriterio3());

        Document xml = parse(xmlGenerator.generarXmlFactura(comprobante()));

        BigDecimal totalComprobante = valor(xml, "TotalComprobante");
        assertThat(totalComprobante).isEqualByComparingTo("1695.00");
        assertThat(totalComprobante.subtract(factura.getTotal())).isEqualByComparingTo("0");
        assertThat(valor(xml, "TotalImpuesto")).isEqualByComparingTo("195.00")
                .isEqualByComparingTo(factura.getIgv());
        assertThat(valor(xml, "TotalVentaNeta")).isEqualByComparingTo("1500.00")
                .isEqualByComparingTo(factura.getSubtotal());

        NodeList lineas = xml.getElementsByTagName("LineaDetalle");
        assertThat(lineas.getLength()).isEqualTo(2);
        Element primera = (Element) lineas.item(0);
        assertThat(valor(primera, "PrecioUnitario")).isEqualByComparingTo("1000.00");
        assertThat(valor(primera, "SubTotal")).isEqualByComparingTo("1000.00");
        assertThat(valor(primera, "BaseImponible")).isEqualByComparingTo("1000.00");
        assertThat(texto(primera, "Tarifa")).isEqualTo("13.00");
        assertThat(texto(primera, "CodigoTarifaIVA")).isEqualTo("08");
        assertThat(valor(primera, "Monto")).isEqualByComparingTo("130.00");
        assertThat(valor(primera, "MontoTotalLinea")).isEqualByComparingTo("1130.00");
        Element segunda = (Element) lineas.item(1);
        assertThat(valor(segunda, "PrecioUnitario")).isEqualByComparingTo("100.00");
        assertThat(valor(segunda, "BaseImponible")).isEqualByComparingTo("500.00");
        assertThat(texto(segunda, "CodigoTarifaIVA")).isEqualTo("08");
        assertThat(valor(segunda, "MontoTotalLinea")).isEqualByComparingTo("565.00");
    }

    @Test
    @DisplayName("criterio 6: redondeo — 5 decimales del XML no generan diferencia con el total de 2 decimales")
    void xml_redondeoSinDiferencia() throws Exception {
        // 3 × 333.33 = 999.99 → 884.95 + 115.04; 1000 → 884.96 + 115.04; PrecioUnitario sin IVA no es exacto
        List<LineaFacturaR> lineas = List.of(
                lineaR(-1, 1, 5, 3, "333.33", "1000.00"),
                lineaR(-2, 2, 6, 1, "1000.00", "1000.00"),
                lineaR(-3, 3, 6, 7, "0.01", "0.07"));
        lineaFacturaService.updateLineas(lineas);

        Document xml = parse(xmlGenerator.generarXmlFactura(comprobante()));

        BigDecimal sumaLineas = BigDecimal.ZERO;
        NodeList nodos = xml.getElementsByTagName("LineaDetalle");
        for (int i = 0; i < nodos.getLength(); i++) {
            Element linea = (Element) nodos.item(i);
            BigDecimal base = valor(linea, "BaseImponible");
            BigDecimal impuesto = valor(linea, "Monto");
            assertThat(base.add(impuesto)).isEqualByComparingTo(valor(linea, "MontoTotalLinea"));
            sumaLineas = sumaLineas.add(valor(linea, "MontoTotalLinea"));
        }
        BigDecimal totalComprobante = valor(xml, "TotalComprobante");
        // T-024: el importe de línea es cantidad × precio (3 × 333.33 = 999.99), no el subtotal enviado
        assertThat(factura.getTotal()).isEqualByComparingTo("2000.06");
        assertThat(totalComprobante.subtract(factura.getTotal())).isEqualByComparingTo("0");
        assertThat(sumaLineas.subtract(factura.getTotal())).isEqualByComparingTo("0");
        assertThat(valor(xml, "TotalImpuesto").subtract(factura.getIgv())).isEqualByComparingTo("0");
        assertThat(valor(xml, "TotalVentaNeta").subtract(factura.getSubtotal())).isEqualByComparingTo("0");
        assertThat(valor(xml, "TotalVentaNeta").add(valor(xml, "TotalImpuesto"))).isEqualByComparingTo(totalComprobante);
        assertThat(valor(xml, "TotalMedioPago")).isEqualByComparingTo(totalComprobante);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static List<LineaFacturaR> lineasCriterio3() {
        return List.of(
                lineaR(-1, 1, 5, 1, "1130.00", "1130.00"),
                lineaR(-2, 2, 6, 5, "113.00", "565.00"));
    }

    private static LineaFacturaR lineaR(Integer idLinea, int numero, int idProducto, int cantidad,
                                        String precio, String subtotal) {
        return new LineaFacturaR(idLinea, numero, idProducto, ID_FACTURA, null, cantidad,
                new BigDecimal(precio), new BigDecimal(subtotal), null, null, null, null);
    }

    @SuppressWarnings("deprecation")
    private static Producto producto(int id, ArticuloMaestro maestro, Boolean gravado, BigDecimal porcentaje) {
        Producto p = new Producto();
        p.setIdProducto(id);
        p.setDescripcion("Producto " + id);
        p.setCodigoCabys("3530100000100");
        p.setGravado(gravado);
        p.setPorcentajeImpuesto(porcentaje);
        p.setArticuloMaestro(maestro);
        return p;
    }

    /**
     * Simula el INSERT: las líneas nuevas que updateLineas agregó a factura.getLineas() reciben su id. Esas mismas
     * entidades son las que lee el XML (XmlGeneratorServiceImpl recorre factura.getLineas()).
     */
    private void asignarIdsPersistidos(Integer... ids) {
        for (int i = 0; i < ids.length; i++) {
            factura.getLineas().get(i).setIdLineaFactura(ids[i]);
        }
    }

    private ComprobanteElectronico comprobante() {
        ConfiguracionEmpresa empresa = ConfiguracionEmpresa.builder()
                .razonSocial("Empresa Test SA")
                .numeroIdentificacion("3101234567")
                .codigoActividad("620100")
                .codigoProvincia("1")
                .canton("01")
                .distrito("01")
                .otrasSenas("Avenida Central San Jose Costa Rica")
                .build();
        return ComprobanteElectronico.builder()
                .claveNumerica(CLAVE_50)
                .fechaEmision(LocalDateTime.of(2026, 10, 1, 10, 0))
                .empresa(empresa)
                .factura(factura)
                .build();
    }

    private AsientoContable ultimoAsientoGuardado() {
        ArgumentCaptor<AsientoContable> captor = ArgumentCaptor.forClass(AsientoContable.class);
        verify(asientoRepository, atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }

    private BigDecimal debe(AsientoContable asiento, String clave) {
        return asiento.getDetalles().stream()
                .filter(d -> d.getCuenta() == cuentas.get(clave))
                .map(DetalleAsiento::getDebe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal haber(AsientoContable asiento, String clave) {
        return asiento.getDetalles().stream()
                .filter(d -> d.getCuenta() == cuentas.get(clave))
                .map(DetalleAsiento::getHaber)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static Document parse(String xml) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        return f.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    private static BigDecimal valor(Document doc, String tag) {
        return new BigDecimal(doc.getElementsByTagName(tag).item(0).getTextContent());
    }

    private static BigDecimal valor(Element el, String tag) {
        return new BigDecimal(texto(el, tag));
    }

    private static String texto(Element el, String tag) {
        return el.getElementsByTagName(tag).item(0).getTextContent();
    }
}
