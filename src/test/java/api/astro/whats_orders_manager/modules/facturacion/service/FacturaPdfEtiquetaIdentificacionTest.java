package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.configuracion.enums.TipoIdentificacion;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.model.Empresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.CuentaBancariaRepository;
import api.astro.whats_orders_manager.modules.configuracion.service.ConfiguracionEmpresaService;
import api.astro.whats_orders_manager.modules.configuracion.service.EmpresaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.service.impl.FacturaPdfServiceImpl;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T-018 (ADR-021) — en el encabezado del PDF, la etiqueta de la identificación del emisor sale de su tipo
 * (antes era siempre "Cédula Jurídica").
 */
@DisplayName("T-018 — etiqueta de la identificación del emisor en el PDF")
class FacturaPdfEtiquetaIdentificacionTest {

    @ParameterizedTest(name = "{0} → \"{1}: {2}\"")
    @CsvSource({
            "CEDULA_FISICA, Cédula Física, 118200878",
            "CEDULA_JURIDICA, Cédula Jurídica, 3101234567"
    })
    void etiquetaSegunTipo(TipoIdentificacion tipo, String etiqueta, String numero) throws Exception {
        FacturaService facturaService = mock(FacturaService.class);
        EmpresaService empresaService = mock(EmpresaService.class);
        ConfiguracionEmpresaService configuracionService = mock(ConfiguracionEmpresaService.class);
        CuentaBancariaRepository cuentaBancariaRepository = mock(CuentaBancariaRepository.class);
        FacturaPdfServiceImpl service = new FacturaPdfServiceImpl(facturaService, empresaService, configuracionService,
                cuentaBancariaRepository, mock(UsuarioService.class));

        Cliente cliente = new Cliente();
        cliente.setIdCliente(1);
        cliente.setNombre("Cliente de prueba");
        Factura factura = new Factura();
        factura.setIdFactura(7);
        factura.setNumeroFactura("FAC-0007");
        factura.setCliente(cliente);
        factura.setSubtotal(new BigDecimal("100.00"));
        factura.setTotal(new BigDecimal("113.00"));
        factura.setMonedaFE(MonedaFE.CRC);
        factura.setLineas(new ArrayList<>());
        when(facturaService.findById(7)).thenReturn(Optional.of(factura));
        Empresa empresa = new Empresa();
        empresa.setIdEmpresa(1);
        empresa.setNombreEmpresa("Monrachem");
        when(empresaService.getEmpresaPrincipal()).thenReturn(empresa);
        ConfiguracionEmpresa configuracion = new ConfiguracionEmpresa();
        configuracion.setRazonSocial("EMANUEL SOTO LEAL");
        configuracion.setNombreComercial("MONRACHEM");
        configuracion.setTipoIdentificacion(tipo);
        configuracion.setNumeroIdentificacion(numero);
        when(configuracionService.obtenerOCrearConfiguracion()).thenReturn(configuracion);
        when(cuentaBancariaRepository.findByEmpresaIdEmpresaAndActivaTrue(1)).thenReturn(List.of());

        byte[] pdf = service.generarPdfFactura(7);

        String texto;
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= doc.getNumberOfPages(); i++) {
                sb.append(PdfTextExtractor.getTextFromPage(doc.getPage(i)));
            }
            texto = sb.toString();
        }
        assertThat(texto).contains(etiqueta + ": " + numero);
        if (tipo == TipoIdentificacion.CEDULA_FISICA) {
            assertThat(texto).doesNotContain("Cédula Jurídica");
        }
    }
}
