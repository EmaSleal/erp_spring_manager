package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.ParametroContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.AsientoContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.DetalleAsientoRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * T-039 (criterio 7, ADR-045 §4): fecha del asiento de venta de una factura sustituta. Con código 15 se fecha con el
 * comprobante ACEPTADO de la original (su periodo); con 07 y en una venta normal, con la fecha de emisión de la factura.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("T-039 — fecha contable del asiento de venta de la sustituta")
class AsientoVentaSustitutaFechaTest {

    private static final LocalDate FECHA_SUSTITUTA = LocalDate.of(2026, 11, 18);
    private static final LocalDateTime FECHA_COMPROBANTE_ORIGINAL = LocalDateTime.of(2026, 9, 29, 23, 40, 0);

    @Mock private AsientoContableRepository asientoRepository;
    @Mock private DetalleAsientoRepository detalleRepository;
    @Mock private CuentaContableRepository cuentaRepository;
    @Mock private ParametroContableRepository parametroContableRepository;

    private AsientoContableService service;

    @BeforeEach
    void setUp() {
        service = new AsientoContableService(asientoRepository, detalleRepository, cuentaRepository,
                parametroContableRepository);
        long id = 1;
        for (String clave : List.of("VENTA_CXC_CLIENTES", "VENTA_INGRESOS", "VENTA_IVA_POR_PAGAR")) {
            CuentaContable c = new CuentaContable();
            c.setIdCuenta(id++);
            c.setCodigo(clave);
            c.setNombre(clave);
            c.setActiva(true);
            c.setAceptaMovimientos(true);
            ParametroContable p = new ParametroContable();
            p.setClave(clave);
            p.setCuentaContable(c);
            when(parametroContableRepository.findByClave(clave)).thenReturn(Optional.of(p));
        }
        when(asientoRepository.findUltimoNumeroDelAnio(anyString())).thenReturn(Optional.empty());
        when(asientoRepository.save(any(AsientoContable.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("sustituta con código 15: el asiento lleva la fecha del comprobante de la original")
    void codigo15_fechaDelComprobanteOriginal() {
        Factura sustituta = sustituta("15");

        AsientoContable asiento = service.generarAsientoVenta(sustituta);

        assertThat(asiento.getFecha()).isEqualTo(FECHA_COMPROBANTE_ORIGINAL.toLocalDate())
                .isNotEqualTo(FECHA_SUSTITUTA);
    }

    @Test
    @DisplayName("sustituta con código 07: el asiento lleva su propia fecha de emisión, como cualquier venta")
    void codigo07_fechaDeEmision() {
        AsientoContable asiento = service.generarAsientoVenta(sustituta("07"));

        assertThat(asiento.getFecha()).isEqualTo(FECHA_SUSTITUTA);
    }

    @Test
    @DisplayName("factura normal: fecha de emisión")
    void facturaNormal_fechaDeEmision() {
        Factura normal = AsientoVentaCobroServiceTest.factura(90, "100.00", "13.00", "113.00", MonedaFE.CRC,
                BigDecimal.ONE);
        normal.setCreateDate(FECHA_SUSTITUTA.atTime(9, 0));

        assertThat(service.generarAsientoVenta(normal).getFecha()).isEqualTo(FECHA_SUSTITUTA);
    }

    private static Factura sustituta(String codigo) {
        Factura original = AsientoVentaCobroServiceTest.factura(70, "100.00", "13.00", "113.00", MonedaFE.CRC,
                BigDecimal.ONE);
        original.setAnulada(true);
        original.getComprobantes().add(ComprobanteElectronico.builder()
                .id(1L).factura(original).claveNumerica("5".repeat(50)).consecutivo("1".repeat(20))
                .fechaEmision(FECHA_COMPROBANTE_ORIGINAL).estado(EstadoComprobante.ACEPTADO).vigente(true).build());
        Factura sustituta = AsientoVentaCobroServiceTest.factura(71, "100.00", "13.00", "113.00", MonedaFE.CRC,
                BigDecimal.ONE);
        sustituta.setCreateDate(FECHA_SUSTITUTA.atTime(9, 0));
        sustituta.setFacturaSustituida(original);
        sustituta.setCodigoSustitucion(codigo);
        sustituta.setRazonSustitucion("Sustituye la factura F-70");
        return sustituta;
    }
}
