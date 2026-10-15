package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.model.CuentaBancaria;
import api.astro.whats_orders_manager.modules.configuracion.model.Empresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.CuentaBancariaRepository;
import api.astro.whats_orders_manager.modules.configuracion.service.ConfiguracionEmpresaService;
import api.astro.whats_orders_manager.modules.configuracion.service.EmpresaService;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.service.impl.FacturaPdfServiceImpl;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T-015 (ADR-018) — el PDF de la factura sigue mostrando las cuentas bancarias activas de la empresa
 * (consulta activa = true), ahora que cada una tiene su hoja contable.
 */
@DisplayName("T-015 — PDF de factura: cuentas bancarias activas")
class FacturaPdfCuentasBancariasTest {

    @Test
    @DisplayName("el pie del PDF lista las cuentas bancarias activas (entidad, IBAN y cuenta) y usa solo la consulta de activas")
    void pdfMuestraCuentasActivas() throws Exception {
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
        empresa.setNombreEmpresa("Empresa S.A.");
        when(empresaService.getEmpresaPrincipal()).thenReturn(empresa);
        ConfiguracionEmpresa configuracion = new ConfiguracionEmpresa();
        configuracion.setNombreComercial("Empresa S.A.");
        when(configuracionService.obtenerOCrearConfiguracion()).thenReturn(configuracion);
        when(cuentaBancariaRepository.findByEmpresaIdEmpresaAndActivaTrue(1)).thenReturn(List.of(
                banco("Banco Nacional - Colones", "CR74015104210010004988", "100-01-042-000498-5", "1.1.01.002.001"),
                banco("BAC San José - Dólares", "CR77010200009176559879", "91765598", "1.1.01.002.004")));

        byte[] pdf = service.generarPdfFactura(7);

        String texto;
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= doc.getNumberOfPages(); i++) {
                sb.append(PdfTextExtractor.getTextFromPage(doc.getPage(i)));
            }
            texto = sb.toString();
        }
        assertThat(texto).contains("Por favor depositar en las siguientes cuentas bancarias");
        // La columna Entidad es angosta: el nombre se parte en dos líneas; se verifican sus partes
        assertThat(texto).contains("Banco Nacional -", "CR74015104210010004988", "100-01-042-000498-5");
        assertThat(texto).contains("BAC San José -", "CR77010200009176559879", "91765598");
        // La hoja contable no se imprime en la factura
        assertThat(texto).doesNotContain("1.1.01.002");
        verify(cuentaBancariaRepository).findByEmpresaIdEmpresaAndActivaTrue(1);
        verify(cuentaBancariaRepository, never()).findByEmpresaIdEmpresaOrderByOrdenAsc(1);
    }

    private static CuentaBancaria banco(String entidad, String iban, String cuenta, String codigoHoja) {
        CuentaContable hoja = new CuentaContable();
        hoja.setCodigo(codigoHoja);
        CuentaBancaria b = new CuentaBancaria();
        b.setEntidad(entidad);
        b.setCuentaIban(iban);
        b.setCuentaBanco(cuenta);
        b.setActiva(true);
        b.setCuentaContable(hoja);
        return b;
    }
}
