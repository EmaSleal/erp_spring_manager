package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.model.Empresa;
import api.astro.whats_orders_manager.modules.configuracion.model.Presentacion;
import api.astro.whats_orders_manager.modules.configuracion.repository.CuentaBancariaRepository;
import api.astro.whats_orders_manager.modules.configuracion.service.ConfiguracionEmpresaService;
import api.astro.whats_orders_manager.modules.configuracion.service.EmpresaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.facturacion.service.impl.FacturaPdfServiceImpl;
import api.astro.whats_orders_manager.modules.producto.model.ArticuloMaestro;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** T-034 (criterio 13): la columna UM del PDF usa la misma unidad que el XML (DatosFeProducto.unidadMedida). */
@DisplayName("T-034 — PDF de la factura: UM igual a la del XML")
class FacturaPdfUnidadMedidaTest {

    @Test
    @DisplayName("empacada → Unid (no su nombre); no empacada → su código de unidad FE")
    void umComoEnElXml() throws Exception {
        FacturaService facturaService = mock(FacturaService.class);
        EmpresaService empresaService = mock(EmpresaService.class);
        ConfiguracionEmpresaService configuracionService = mock(ConfiguracionEmpresaService.class);
        CuentaBancariaRepository cuentas = mock(CuentaBancariaRepository.class);
        FacturaPdfServiceImpl service = new FacturaPdfServiceImpl(facturaService, empresaService, configuracionService,
                cuentas, mock(UsuarioService.class),
                mock(api.astro.whats_orders_manager.modules.facturacion.repository.NotaFacturaRepository.class),
                mock(api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository.class),
                mock(api.astro.whats_orders_manager.modules.facturacion.service.ConfiguracionFacturacionService.class), mock(api.astro.whats_orders_manager.modules.facturacion.service.impl.LugarVentaResolver.class));

        Cliente cliente = new Cliente();
        cliente.setIdCliente(1);
        cliente.setNombre("Cliente de prueba");
        Factura factura = new Factura();
        factura.setIdFactura(7);
        factura.setNumeroFactura("FAC-0007");
        factura.setCliente(cliente);
        factura.setSubtotal(new BigDecimal("300.00"));
        factura.setTotal(new BigDecimal("339.00"));
        factura.setMonedaFE(MonedaFE.CRC);
        factura.setLineas(new ArrayList<>(List.of(
                linea(producto("Caja12", true, "Kg")), linea(producto("Botellon", false, "Gal")))));
        when(facturaService.findById(7)).thenReturn(Optional.of(factura));
        Empresa empresa = new Empresa();
        empresa.setIdEmpresa(1);
        empresa.setNombreEmpresa("Monrachem");
        when(empresaService.getEmpresaPrincipal()).thenReturn(empresa);
        ConfiguracionEmpresa configuracion = new ConfiguracionEmpresa();
        configuracion.setRazonSocial("EMANUEL SOTO LEAL");
        configuracion.setNumeroIdentificacion("3101234567");
        when(configuracionService.obtenerOCrearConfiguracion()).thenReturn(configuracion);
        when(cuentas.findByEmpresaIdEmpresaAndActivaTrue(1)).thenReturn(List.of());

        byte[] pdf = service.generarPdfFactura(7);

        StringBuilder sb = new StringBuilder();
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            for (int i = 1; i <= doc.getNumberOfPages(); i++) {
                sb.append(PdfTextExtractor.getTextFromPage(doc.getPage(i)));
            }
        }
        String texto = sb.toString();
        assertThat(texto).contains("Unid").contains("Gal");
        // «Caja12» solo aparece en el Detalle (empacada), y «Botellon» no aparece: ya no sale como UM ni en el Detalle
        assertThat(texto).contains("Alivion Caja12").doesNotContain("Botellon").doesNotContain("Kg");
    }

    private static Producto producto(String presentacion, boolean empacado, String codigo) {
        ArticuloMaestro maestro = new ArticuloMaestro();
        maestro.setDescripcion("Alivion");
        maestro.setCodigoCabys("1234567890123");
        Presentacion pr = new Presentacion();
        pr.setNombre(presentacion);
        pr.setEmpacado(empacado);
        pr.setCodigoUnidadFE(codigo);
        Producto p = new Producto();
        p.setArticuloMaestro(maestro);
        p.setPresentacion(pr);
        return p;
    }

    private static LineaFactura linea(Producto p) {
        LineaFactura l = new LineaFactura();
        l.setProducto(p);
        l.setCantidad(1);
        l.setPrecioUnitario(new BigDecimal("100.00"));
        l.setSubtotal(new BigDecimal("100.00"));
        return l;
    }
}
