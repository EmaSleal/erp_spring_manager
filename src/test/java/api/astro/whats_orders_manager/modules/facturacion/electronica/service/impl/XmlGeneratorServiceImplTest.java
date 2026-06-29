package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.producto.model.ArticuloMaestro;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for XmlGeneratorServiceImpl — FE field null-safe navigation through ArticuloMaestro.
 *
 * Verifies:
 * - When producto.getArticuloMaestro() is null, FE fields fall back to deprecated Producto fields (no NPE).
 * - When producto.getArticuloMaestro() is non-null, FE fields are sourced from the master entity.
 *
 * Uses generarXmlTiquete (no XSD validation step) to keep the test isolated from schema files.
 */
@DisplayName("XmlGeneratorServiceImpl — FE field resolution via ArticuloMaestro")
@SuppressWarnings("deprecation")
class XmlGeneratorServiceImplTest {

    private XmlGeneratorServiceImpl service;

    /**
     * Minimal ConfiguracionEmpresa satisfying all non-null paths in agregarEmisor.
     * Uses alias methods getRuc() → numeroIdentificacion, getNombreEmpresa() → razonSocial.
     */
    private ConfiguracionEmpresa empresa;

    /** 50-digit clave that satisfies extraerConsecutivoDeClave without throwing. */
    private static final String CLAVE_50 = "50601042612345678901200110000011000010000100000001";

    @BeforeEach
    void setUp() {
        service = new XmlGeneratorServiceImpl();

        empresa = ConfiguracionEmpresa.builder()
                .razonSocial("Empresa Test SA")
                .numeroIdentificacion("3101234567")
                .codigoActividad("620100")
                .codigoProvincia("1")
                .canton("01")
                .distrito("01")
                .otrasSenas("Avenida Central San Jose Costa Rica")
                .build();
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private ComprobanteElectronico buildTiqueteWith(Producto producto) {
        LineaFactura linea = new LineaFactura();
        linea.setProducto(producto);

        Factura factura = new Factura();
        factura.getLineas().add(linea);

        return ComprobanteElectronico.builder()
                .claveNumerica(CLAVE_50)
                .fechaEmision(LocalDateTime.of(2026, 4, 1, 10, 0))
                .empresa(empresa)
                .factura(factura)
                .build();
    }

    // =========================================================================
    // Null-master fallback — XML-1 spec requirement
    // =========================================================================

    @Test
    @DisplayName("When articuloMaestro is null, must use Producto deprecated fields without NPE")
    void testResolve_masterNull_fallsBackToProductoFieldsWithoutNpe() {
        // Arrange
        Producto producto = new Producto();
        producto.setDescripcion("Producto Sin Maestro");
        producto.setCodigoCabys("1234567890001");
        producto.setGravado(false);
        // articuloMaestro is null (not set — transition guard scenario)

        ComprobanteElectronico comprobante = buildTiqueteWith(producto);

        // Act — must never throw NullPointerException
        String xml = assertDoesNotThrow(
                () -> service.generarXmlTiquete(comprobante),
                "generarXmlTiquete must not throw NPE when articuloMaestro is null"
        );

        // Assert — fallback fields used
        assertTrue(xml.contains("<Detalle>Producto Sin Maestro</Detalle>"),
                "XML must contain the fallback Producto descripcion");
        assertTrue(xml.contains("<CodigoCABYS>1234567890001</CodigoCABYS>"),
                "XML must contain the fallback Producto codigoCabys");
    }

    // =========================================================================
    // Master-present override — XML-1 spec requirement
    // =========================================================================

    @Test
    @DisplayName("When articuloMaestro is present, FE fields must come from the master entity")
    void testResolve_masterPresent_masterFieldsTakePrecedenceOverProducto() {
        // Arrange
        ArticuloMaestro master = new ArticuloMaestro();
        master.setDescripcion("Descripcion del Maestro");
        master.setCodigoCabys("9999999999999");
        master.setGravado(true);
        master.setPorcentajeImpuesto(new BigDecimal("13"));

        Producto producto = new Producto();
        // Deprecated fields set to DIFFERENT values so we can confirm which source was used
        producto.setDescripcion("Descripcion Antigua del Producto");
        producto.setCodigoCabys("0000000000000");
        producto.setGravado(false);
        producto.setArticuloMaestro(master);

        ComprobanteElectronico comprobante = buildTiqueteWith(producto);

        // Act
        String xml = assertDoesNotThrow(() -> service.generarXmlTiquete(comprobante));

        // Assert — master fields used
        assertTrue(xml.contains("<Detalle>Descripcion del Maestro</Detalle>"),
                "XML must use master.getDescripcion(), not the deprecated Producto field");
        assertTrue(xml.contains("<CodigoCABYS>9999999999999</CodigoCABYS>"),
                "XML must use master.getCodigoCabys(), not the deprecated Producto field");

        // Assert — old deprecated values must NOT appear in the Detalle element
        assertFalse(xml.contains("<Detalle>Descripcion Antigua del Producto</Detalle>"),
                "Deprecated Producto descripcion must NOT appear in XML when master is set");
    }

    // =========================================================================
    // Tax resolution from master
    // =========================================================================

    @Test
    @DisplayName("When articuloMaestro.gravado is false, the XML must emit the exento tax code")
    void testResolve_masterGravadoFalse_emitsExentoTaxCode() {
        // Arrange
        ArticuloMaestro master = new ArticuloMaestro();
        master.setDescripcion("Articulo Exento");
        master.setCodigoCabys("1111111111111");
        master.setGravado(false); // not subject to VAT

        Producto producto = new Producto();
        producto.setGravado(true); // conflicting deprecated field — master must take precedence
        producto.setArticuloMaestro(master);

        ComprobanteElectronico comprobante = buildTiqueteWith(producto);

        // Act
        String xml = assertDoesNotThrow(() -> service.generarXmlTiquete(comprobante));

        // Assert — exento code "10" used (0% VAT rate), not "07" (13%)
        assertTrue(xml.contains("<CodigoTarifaIVA>10</CodigoTarifaIVA>"),
                "Exento code '10' must be emitted when master.gravado=false, regardless of Producto.gravado");
    }
}
