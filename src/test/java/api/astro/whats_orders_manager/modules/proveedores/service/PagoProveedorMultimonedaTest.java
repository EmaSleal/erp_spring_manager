package api.astro.whats_orders_manager.modules.proveedores.service;

import api.astro.whats_orders_manager.modules.configuracion.model.CuentaBancaria;
import api.astro.whats_orders_manager.modules.configuracion.service.CuentaBancariaService;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoCuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.model.DetalleAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.model.ParametroContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.AsientoContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaPorPagarRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.DetalleAsientoRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.contabilidad.service.impl.CuentaPorPagarServiceImpl;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.enums.FuenteTipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.model.TipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.repository.TipoCambioRepository;
import api.astro.whats_orders_manager.modules.facturacion.service.TipoCambioService;
import api.astro.whats_orders_manager.modules.proveedores.dto.PagoProveedorDTO;
import api.astro.whats_orders_manager.modules.proveedores.enums.EstadoPagoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.enums.MetodoPagoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.PagoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import api.astro.whats_orders_manager.modules.proveedores.repository.OrdenCompraRepository;
import api.astro.whats_orders_manager.modules.proveedores.repository.PagoProveedorRepository;
import api.astro.whats_orders_manager.modules.proveedores.repository.ProveedorRepository;
import api.astro.whats_orders_manager.modules.proveedores.service.impl.PagoProveedorServiceImpl;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T-007 (ADR-011) — pagos a proveedor con moneda, tipo de cambio y monto aplicado propios;
 * diferencial cambiario realizado (criterio 9) y reversión al anular (criterio 10).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PagoProveedorServiceImpl — multimoneda y reversión (ADR-011)")
class PagoProveedorMultimonedaTest {

    @Mock private PagoProveedorRepository pagoRepository;
    @Mock private CuentaPorPagarRepository cuentaPorPagarRepository;
    @Mock private ProveedorRepository proveedorRepository;
    @Mock private OrdenCompraRepository ordenCompraRepository;
    @Mock private TipoCambioRepository tipoCambioRepository;
    @Mock private AsientoContableRepository asientoRepository;
    @Mock private DetalleAsientoRepository detalleRepository;
    @Mock private CuentaContableRepository cuentaRepository;
    @Mock private ParametroContableRepository parametroContableRepository;
    @Mock private CuentaBancariaService cuentaBancariaService;

    private PagoProveedorServiceImpl service;
    private final Map<String, CuentaContable> cuentas = new HashMap<>();
    private Proveedor proveedor;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        AsientoContableService asientoService = new AsientoContableService(
                asientoRepository, detalleRepository, cuentaRepository, parametroContableRepository);
        TipoCambioService tipoCambioService = new TipoCambioService(tipoCambioRepository);
        CuentaPorPagarServiceImpl cuentaService = new CuentaPorPagarServiceImpl(cuentaPorPagarRepository,
                proveedorRepository, ordenCompraRepository, asientoService, tipoCambioService);
        service = new PagoProveedorServiceImpl(pagoRepository, cuentaPorPagarRepository, cuentaService,
                asientoService, tipoCambioService, cuentaBancariaService);

        long id = 1;
        for (String clave : List.of("CPP_CUENTA_POR_PAGAR",
                "DIFERENCIAL_CAMBIARIO_INGRESO", "DIFERENCIAL_CAMBIARIO_GASTO")) {
            CuentaContable c = new CuentaContable();
            c.setIdCuenta(id++);
            c.setCodigo(clave);
            c.setNombre(clave);
            c.setActiva(true);
            c.setAceptaMovimientos(true);
            cuentas.put(clave, c);
            ParametroContable p = new ParametroContable();
            p.setClave(clave);
            p.setCuentaContable(c);
            when(parametroContableRepository.findByClave(clave)).thenReturn(Optional.of(p));
        }
        // ADR-018: TRANSFERENCIA acredita la hoja de la cuenta bancaria de origen (sin clave CPP_BANCO_*)
        CuentaContable hojaBanco = new CuentaContable();
        hojaBanco.setIdCuenta(id);
        hojaBanco.setCodigo("1.1.01.002.001");
        hojaBanco.setNombre("Banco de prueba");
        hojaBanco.setActiva(true);
        hojaBanco.setAceptaMovimientos(true);
        cuentas.put("BANCO", hojaBanco);
        when(cuentaBancariaService.resolverParaPago(any(), any(), anyString())).thenAnswer(inv -> {
            CuentaBancaria banco = new CuentaBancaria();
            banco.setIdCuentaBancaria(1);
            banco.setEntidad("Banco de prueba");
            banco.setMoneda(((MonedaFE) inv.getArgument(1)).getCodigo());
            banco.setActiva(true);
            banco.setCuentaContable(hojaBanco);
            return banco;
        });
        when(asientoRepository.findUltimoNumeroDelAnio(anyString())).thenReturn(Optional.empty());
        when(asientoRepository.save(any(AsientoContable.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pagoRepository.findUltimoConsecutivo(anyInt())).thenReturn(null);
        when(pagoRepository.save(any(PagoProveedor.class))).thenAnswer(inv -> {
            PagoProveedor p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(55L);
            }
            return p;
        });
        when(cuentaPorPagarRepository.save(any(CuentaPorPagar.class))).thenAnswer(inv -> inv.getArgument(0));

        proveedor = new Proveedor();
        proveedor.setId(1L);
        proveedor.setNombre("Proveedor Prueba");
        proveedor.setSaldoPendiente(new BigDecimal("113.00"));
        usuario = new Usuario();
        usuario.setNombre("Contador");
    }

    @Test
    @DisplayName("criterio 9: CxP USD (TC 500) pagada 113 USD a TC 510 → DR CxP 56500 / DR Gasto 1130 / CR Banco 57630; saldo 0")
    void pagoUsdTcMayor_gastoPorDiferencial() {
        CuentaPorPagar cpp = cppUsd("500");
        tasaDelDia("510");

        PagoProveedor pago = service.registrarPago(dto(cpp, "113.00", null, null), usuario);

        assertThat(pago.getMoneda()).isEqualTo(MonedaFE.USD);
        assertThat(pago.getTipoCambio()).isEqualByComparingTo("510");
        assertThat(pago.getMontoAplicado()).isEqualByComparingTo("113.00");
        AsientoContable asiento = ultimoAsiento();
        assertThat(asiento.getTipo()).isEqualTo(TipoAsiento.AUTOMATICO_GASTO);
        assertThat(debe(asiento, "CPP_CUENTA_POR_PAGAR")).isEqualByComparingTo("56500.00");
        assertThat(debe(asiento, "DIFERENCIAL_CAMBIARIO_GASTO")).isEqualByComparingTo("1130.00");
        assertThat(haber(asiento, "BANCO")).isEqualByComparingTo("57630.00");
        assertThat(haber(asiento, "DIFERENCIAL_CAMBIARIO_INGRESO")).isEqualByComparingTo("0");
        assertThat(asiento.getTotalDebe()).isEqualByComparingTo(asiento.getTotalHaber());
        assertThat(cpp.getSaldoPendiente()).isEqualByComparingTo("0");
        assertThat(cpp.getEstado()).isEqualTo(EstadoCuentaPorPagar.PAGADA);
    }

    @Test
    @DisplayName("criterio 9: mismo pago a TC 490 → DR CxP 56500 / CR Banco 55370 / CR Ingreso 1130; saldo 0")
    void pagoUsdTcMenor_ingresoPorDiferencial() {
        CuentaPorPagar cpp = cppUsd("500");
        tasaDelDia("490");

        service.registrarPago(dto(cpp, "113.00", null, null), usuario);

        AsientoContable asiento = ultimoAsiento();
        assertThat(debe(asiento, "CPP_CUENTA_POR_PAGAR")).isEqualByComparingTo("56500.00");
        assertThat(haber(asiento, "BANCO")).isEqualByComparingTo("55370.00");
        assertThat(haber(asiento, "DIFERENCIAL_CAMBIARIO_INGRESO")).isEqualByComparingTo("1130.00");
        assertThat(debe(asiento, "DIFERENCIAL_CAMBIARIO_GASTO")).isEqualByComparingTo("0");
        assertThat(asiento.getTotalDebe()).isEqualByComparingTo(asiento.getTotalHaber());
        assertThat(cpp.getSaldoPendiente()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("pago en CRC de una CxP USD: montoAplicado = monto / TC y el saldo baja en USD")
    void pagoCrcDeCxpUsd_montoAplicadoEnUsd() {
        CuentaPorPagar cpp = cppUsd("500");
        tasaDelDia("510");

        PagoProveedor pago = service.registrarPago(dto(cpp, "28815.00", MonedaFE.CRC, null), usuario);

        assertThat(pago.getMoneda()).isEqualTo(MonedaFE.CRC);
        assertThat(pago.getTipoCambio()).isEqualByComparingTo("510");
        assertThat(pago.getMontoAplicado()).isEqualByComparingTo("56.50");
        assertThat(cpp.getSaldoPendiente()).isEqualByComparingTo("56.50");
        AsientoContable asiento = ultimoAsiento();
        assertThat(debe(asiento, "CPP_CUENTA_POR_PAGAR")).isEqualByComparingTo("28250.00");
        assertThat(haber(asiento, "BANCO")).isEqualByComparingTo("28815.00");
        assertThat(debe(asiento, "DIFERENCIAL_CAMBIARIO_GASTO")).isEqualByComparingTo("565.00");
    }

    @Test
    @DisplayName("pago CRC de CxP CRC no genera diferencial")
    void pagoCrc_sinDiferencial() {
        CuentaPorPagar cpp = cppUsd("1");
        cpp.setMoneda(MonedaFE.CRC);

        service.registrarPago(dto(cpp, "50.00", null, null), usuario);

        AsientoContable asiento = ultimoAsiento();
        assertThat(asiento.getDetalles()).hasSize(2);
        assertThat(debe(asiento, "CPP_CUENTA_POR_PAGAR")).isEqualByComparingTo("50.00");
        assertThat(haber(asiento, "BANCO")).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("criterio 12: pago en EUR de una CxP en USD se rechaza con mensaje claro")
    void pagoEurDeCxpUsd_rechazado() {
        CuentaPorPagar cpp = cppUsd("500");

        assertThatThrownBy(() -> service.registrarPago(dto(cpp, "10.00", MonedaFE.EUR, null), usuario))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EUR").hasMessageContaining("USD");
        verify(pagoRepository, never()).save(any());
    }

    @Test
    @DisplayName("el monto aplicado no puede superar el saldo de la CxP")
    void montoAplicadoSuperaSaldo_rechazado() {
        CuentaPorPagar cpp = cppUsd("500");
        tasaDelDia("510");

        assertThatThrownBy(() -> service.registrarPago(dto(cpp, "60000.00", MonedaFE.CRC, null), usuario))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("supera el saldo pendiente");
    }

    @Test
    @DisplayName("criterio 13: si falta DIFERENCIAL_CAMBIARIO_GASTO y hay diferencia → IllegalStateException, sin asiento")
    void claveDiferencialFaltante_lanzaIllegalState() {
        when(parametroContableRepository.findByClave("DIFERENCIAL_CAMBIARIO_GASTO")).thenReturn(Optional.empty());
        CuentaPorPagar cpp = cppUsd("500");
        tasaDelDia("510");

        assertThatThrownBy(() -> service.registrarPago(dto(cpp, "113.00", null, null), usuario))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DIFERENCIAL_CAMBIARIO_GASTO");
        verify(asientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("criterio 10: anular un pago contabilizado genera reversión espejo vinculada (incluye diferencial), restituye el saldo y no modifica el original")
    void anularPago_revierteAsientoYRestituyeSaldo() {
        CuentaPorPagar cpp = cppUsd("500");
        tasaDelDia("510");
        PagoProveedor pago = service.registrarPago(dto(cpp, "113.00", null, null), usuario);
        AsientoContable original = ultimoAsiento();
        original.setIdAsiento(800L);
        original.setNumero("ASI-2026-0800");
        int lineasOriginal = original.getDetalles().size();
        BigDecimal debeOriginal = original.getTotalDebe();
        clearInvocations(asientoRepository);
        when(pagoRepository.findById(55L)).thenReturn(Optional.of(pago));
        when(asientoRepository.findAsientosPagoProveedorVigentes(55L)).thenReturn(List.of(original));

        service.anularPago(55L, usuario);

        assertThat(pago.getEstado()).isEqualTo(EstadoPagoProveedor.ANULADO);
        assertThat(cpp.getSaldoPendiente()).isEqualByComparingTo("113.00");
        assertThat(cpp.getEstado()).isEqualTo(EstadoCuentaPorPagar.PENDIENTE);

        AsientoContable reversion = ultimoAsiento();
        assertThat(reversion.getAsientoRevertido()).isSameAs(original);
        assertThat(reversion.getPagoProveedor()).isSameAs(pago);
        assertThat(reversion.getEstado()).isEqualTo(EstadoAsiento.CONTABILIZADO);
        assertThat(reversion.getConcepto()).contains("ASI-2026-0800");
        assertThat(haber(reversion, "CPP_CUENTA_POR_PAGAR")).isEqualByComparingTo("56500.00");
        assertThat(haber(reversion, "DIFERENCIAL_CAMBIARIO_GASTO")).isEqualByComparingTo("1130.00");
        assertThat(debe(reversion, "BANCO")).isEqualByComparingTo("57630.00");
        assertThat(reversion.getTotalDebe()).isEqualByComparingTo(reversion.getTotalHaber());
        for (String clave : cuentas.keySet()) {
            BigDecimal saldo = debe(original, clave).subtract(haber(original, clave))
                    .add(debe(reversion, clave)).subtract(haber(reversion, clave));
            assertThat(saldo).as(clave).isEqualByComparingTo("0");
        }
        // El original no se edita ni se borra
        assertThat(original.getEstado()).isEqualTo(EstadoAsiento.CONTABILIZADO);
        assertThat(original.getDetalles()).hasSize(lineasOriginal);
        assertThat(original.getTotalDebe()).isEqualByComparingTo(debeOriginal);
        verify(asientoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("anular un pago sin asiento vigente no genera reversión pero restituye el saldo")
    void anularPagoSinAsiento_noRevierte() {
        CuentaPorPagar cpp = cppUsd("500");
        cpp.setSaldoPendiente(BigDecimal.ZERO);
        cpp.setEstado(EstadoCuentaPorPagar.PAGADA);
        PagoProveedor pago = PagoProveedor.builder().id(56L).numero("PP-2026-00009").cuentaPorPagar(cpp)
                .monto(new BigDecimal("113.00")).metodoPago(MetodoPagoProveedor.TRANSFERENCIA)
                .estado(EstadoPagoProveedor.CONFIRMADO).fecha(LocalDate.now()).build();
        when(pagoRepository.findById(56L)).thenReturn(Optional.of(pago));
        when(asientoRepository.findAsientosPagoProveedorVigentes(56L)).thenReturn(List.of());

        service.anularPago(56L, usuario);

        assertThat(cpp.getSaldoPendiente()).isEqualByComparingTo("113.00");
        verify(asientoRepository, never()).save(any());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private CuentaPorPagar cppUsd(String tipoCambio) {
        CuentaPorPagar cpp = CuentaPorPagar.builder()
                .id(10L)
                .numero("CPP-2026-00001")
                .proveedor(proveedor)
                .descripcion("Compra USD")
                .monto(new BigDecimal("113.00"))
                .saldoPendiente(new BigDecimal("113.00"))
                .moneda(MonedaFE.USD)
                .tipoCambio(new BigDecimal(tipoCambio))
                .estado(EstadoCuentaPorPagar.PENDIENTE)
                .build();
        when(cuentaPorPagarRepository.findById(10L)).thenReturn(Optional.of(cpp));
        return cpp;
    }

    private PagoProveedorDTO dto(CuentaPorPagar cpp, String monto, MonedaFE moneda, String tipoCambio) {
        return PagoProveedorDTO.builder()
                .cuentaPorPagarId(cpp.getId())
                .monto(new BigDecimal(monto))
                .moneda(moneda)
                .tipoCambio(tipoCambio != null ? new BigDecimal(tipoCambio) : null)
                .metodoPago(MetodoPagoProveedor.TRANSFERENCIA)
                .idCuentaBancariaOrigen(1)
                .fecha(LocalDate.of(2026, 10, 11))
                .build();
    }

    private void tasaDelDia(String tasaVenta) {
        when(tipoCambioRepository.findFirstByMonedaDestinoAndActivoTrueAndFechaLessThanEqualOrderByFechaDescIdDesc(
                eq(MonedaFE.USD), eq(LocalDate.of(2026, 10, 11)))).thenReturn(Optional.of(TipoCambio.builder()
                .monedaOrigen(MonedaFE.CRC).monedaDestino(MonedaFE.USD).fecha(LocalDate.of(2026, 10, 11))
                .tasaCompra(new BigDecimal(tasaVenta)).tasaVenta(new BigDecimal(tasaVenta))
                .fuente(FuenteTipoCambio.MANUAL).activo(true).build()));
    }

    private AsientoContable ultimoAsiento() {
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
}
