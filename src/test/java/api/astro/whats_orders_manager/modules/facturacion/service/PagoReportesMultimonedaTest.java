package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.dto.ReporteCajaDTO;
import api.astro.whats_orders_manager.modules.facturacion.dto.mapper.PagoMapper;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoPago;
import api.astro.whats_orders_manager.modules.facturacion.enums.MetodoPago;
import api.astro.whats_orders_manager.modules.facturacion.model.Pago;
import api.astro.whats_orders_manager.modules.facturacion.repository.PagoRepository;
import api.astro.whats_orders_manager.modules.facturacion.service.impl.PagoServiceImpl;
import api.astro.whats_orders_manager.modules.facturacion.util.ConversionMoneda;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * T-007 (ADR-011), criterio 17 — los totales de caja e ingresos suman el monto en CRC de cada pago:
 * monto si la moneda del pago es CRC; monto × tipoCambio si es extranjera; monto nominal si es una
 * fila anterior a ADR-011 sin tipo de cambio.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PagoServiceImpl — totales de caja e ingresos en CRC (ADR-011)")
class PagoReportesMultimonedaTest {

    private static final LocalDate DIA = LocalDate.of(2026, 10, 11);

    @Mock private PagoRepository pagoRepository;
    @Mock private PagoMapper pagoMapper;

    private PagoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PagoServiceImpl();
        ReflectionTestUtils.setField(service, "pagoRepository", pagoRepository);
        ReflectionTestUtils.setField(service, "pagoMapper", pagoMapper);
        when(pagoMapper.toDTOList(any())).thenReturn(List.of());
    }

    @Test
    @DisplayName("criterio 17: un pago de 10 000 CRC y uno de 100 USD a TC 500 suman 60 000 CRC en los ingresos del día")
    void ingresosDelDia_sumaEnCrc() {
        LocalDate hoy = LocalDate.now();
        when(pagoRepository.findValidosByFechaPagoRango(hoy.atStartOfDay(), hoy.plusDays(1).atStartOfDay()))
            .thenReturn(List.of(
                pago("10000.00", MonedaFE.CRC, null, MetodoPago.EFECTIVO),
                pago("100.00", MonedaFE.USD, "500", MetodoPago.TRANSFERENCIA)));

        assertThat(service.sumIngresosDiaActual()).isEqualByComparingTo("60000.00");
    }

    @Test
    @DisplayName("criterio 17: el reporte de caja totaliza 60 000 CRC y convierte cada método de pago")
    void reporteCaja_totalesEnCrc() {
        when(pagoRepository.findValidosByFechaPagoRango(DIA.atStartOfDay(), DIA.plusDays(1).atStartOfDay()))
            .thenReturn(List.of(
                pago("10000.00", MonedaFE.CRC, null, MetodoPago.EFECTIVO),
                pago("100.00", MonedaFE.USD, "500", MetodoPago.TRANSFERENCIA)));

        ReporteCajaDTO reporte = service.buildReporteCaja(DIA);

        assertThat(reporte.getTotalIngresos()).isEqualByComparingTo("60000.00");
        assertThat(reporte.getTotalEfectivo()).isEqualByComparingTo("10000.00");
        assertThat(reporte.getTotalTransferencia()).isEqualByComparingTo("50000.00");
        assertThat(reporte.getCantidadEfectivo()).isEqualTo(1L);
        assertThat(reporte.getCantidadTransferencia()).isEqualTo(1L);
    }

    @Test
    @DisplayName("criterio 17: un pago en CRC sobre factura USD (guarda TC del USD) aporta su monto, sin multiplicar")
    void pagoCrcSobreFacturaUsd_noSeMultiplica() {
        when(pagoRepository.findValidosByFechaPagoRango(DIA.atStartOfDay(), DIA.plusDays(1).atStartOfDay()))
            .thenReturn(List.of(pago("50000.00", MonedaFE.CRC, "500", MetodoPago.EFECTIVO)));

        ReporteCajaDTO reporte = service.buildReporteCaja(DIA);

        assertThat(reporte.getTotalIngresos()).isEqualByComparingTo("50000.00");
    }

    @Test
    @DisplayName("criterio 17: suma por método de pago en un rango usa montos en CRC")
    void sumaPorMetodo_enCrc() {
        LocalDateTime inicio = DIA.atStartOfDay();
        LocalDateTime fin = DIA.atTime(23, 59, 59);
        when(pagoRepository.findValidosByFechaPagoRango(eq(inicio), any()))
            .thenReturn(List.of(
                pago("10000.00", MonedaFE.CRC, null, MetodoPago.TRANSFERENCIA),
                pago("100.00", MonedaFE.USD, "500", MetodoPago.TRANSFERENCIA),
                pago("7.00", MonedaFE.USD, "500", MetodoPago.EFECTIVO)));

        assertThat(service.sumByMetodoPagoAndFecha(MetodoPago.TRANSFERENCIA, inicio, fin))
            .isEqualByComparingTo("60000.00");
    }

    @Test
    @DisplayName("montoEnCrcReporte: CRC → monto; extranjera con TC → monto × TC; sin TC (histórico) → monto nominal")
    void montoEnCrcReporte_reglas() {
        assertThat(ConversionMoneda.montoEnCrcReporte(new BigDecimal("10000"), MonedaFE.CRC, new BigDecimal("500")))
            .isEqualByComparingTo("10000");
        assertThat(ConversionMoneda.montoEnCrcReporte(new BigDecimal("10000"), null, null))
            .isEqualByComparingTo("10000");
        assertThat(ConversionMoneda.montoEnCrcReporte(new BigDecimal("100"), MonedaFE.USD, new BigDecimal("503.27")))
            .isEqualByComparingTo("50327.00");
        assertThat(ConversionMoneda.montoEnCrcReporte(new BigDecimal("100"), MonedaFE.USD, null))
            .isEqualByComparingTo("100");
        assertThat(ConversionMoneda.montoEnCrcReporte(null, MonedaFE.USD, new BigDecimal("500")))
            .isEqualByComparingTo("0");
    }

    private Pago pago(String monto, MonedaFE moneda, String tipoCambio, MetodoPago metodo) {
        Pago p = new Pago();
        p.setMonto(new BigDecimal(monto));
        p.setMoneda(moneda);
        p.setTipoCambio(tipoCambio != null ? new BigDecimal(tipoCambio) : null);
        p.setMetodoPago(metodo);
        p.setEstado(EstadoPago.CONFIRMADO);
        p.setFechaPago(DIA.atTime(10, 0));
        return p;
    }
}
