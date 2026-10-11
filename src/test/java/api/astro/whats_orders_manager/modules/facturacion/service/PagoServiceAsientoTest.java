package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.cliente.repository.ClienteRepository;
import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.ParametroContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.AsientoContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.DetalleAsientoRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.facturacion.dto.mapper.FacturaMapper;
import api.astro.whats_orders_manager.modules.facturacion.dto.mapper.PagoMapper;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoPago;
import api.astro.whats_orders_manager.modules.facturacion.enums.MetodoPago;
import api.astro.whats_orders_manager.modules.facturacion.enums.TipoPago;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.Pago;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.repository.PagoRepository;
import api.astro.whats_orders_manager.modules.facturacion.service.impl.PagoServiceImpl;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T-005 — PagoServiceImpl contabiliza cobros al pasar a CONFIRMADO y los revierte al anular (ADR-008).
 * Criterios 5 y 6.
 *
 * Flujo real verificado:
 *   registrarPago: estado inicial = el recibido, o CONFIRMADO si viene null → asiento si queda CONFIRMADO/CONCILIADO.
 *   confirmarPago: PENDIENTE/RECHAZADO/ANULADO → CONFIRMADO genera asiento; CONFIRMADO es no-op.
 *   conciliarPago: CONFIRMADO → CONCILIADO no genera asiento; PENDIENTE → CONCILIADO lo genera si no existe.
 *   anularPago / rechazarPago: revierten el asiento de cobro vigente (si existe).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PagoServiceImpl — contabilización de cobros (ADR-008)")
class PagoServiceAsientoTest {

    @Mock private PagoRepository pagoRepository;
    @Mock private FacturaRepository facturaRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private NumeroPagoGeneratorService numeroPagoGenerator;
    @Mock private PagoMapper pagoMapper;
    @Mock private FacturaMapper facturaMapper;

    // Repositorios del AsientoContableService real
    @Mock private AsientoContableRepository asientoRepository;
    @Mock private DetalleAsientoRepository detalleRepository;
    @Mock private CuentaContableRepository cuentaRepository;
    @Mock private ParametroContableRepository parametroContableRepository;

    private PagoServiceImpl service;
    private final Map<String, CuentaContable> cuentas = new HashMap<>();
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        AsientoContableService asientoService = new AsientoContableService(
                asientoRepository, detalleRepository, cuentaRepository, parametroContableRepository);
        service = new PagoServiceImpl();
        ReflectionTestUtils.setField(service, "pagoRepository", pagoRepository);
        ReflectionTestUtils.setField(service, "facturaRepository", facturaRepository);
        ReflectionTestUtils.setField(service, "clienteRepository", clienteRepository);
        ReflectionTestUtils.setField(service, "numeroPagoGenerator", numeroPagoGenerator);
        ReflectionTestUtils.setField(service, "pagoMapper", pagoMapper);
        ReflectionTestUtils.setField(service, "facturaMapper", facturaMapper);
        ReflectionTestUtils.setField(service, "asientoContableService", asientoService);

        long id = 1;
        for (String clave : List.of("VENTA_CXC_CLIENTES", "COBRO_ANTICIPOS_CLIENTES",
                "COBRO_CAJA_EFECTIVO", "COBRO_CAJA_TRANSFERENCIA")) {
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
        when(asientoRepository.findUltimoNumeroDelAnio(anyString())).thenReturn(Optional.empty());
        when(asientoRepository.save(any(AsientoContable.class))).thenAnswer(inv -> inv.getArgument(0));

        cliente = new Cliente();
        cliente.setIdCliente(1);
        cliente.setNombre("Cliente Prueba");
        when(clienteRepository.findById(1)).thenReturn(Optional.of(cliente));
        when(numeroPagoGenerator.generarNumeroPago()).thenReturn("PAG-20261010-0001");
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            if (p.getIdPago() == null) {
                p.setIdPago(77L);
            }
            return p;
        });
    }

    // =========================================================================
    // Criterio 5 — cobro al confirmar
    // =========================================================================

    @Test
    @DisplayName("registrarPago con estado null (→ CONFIRMADO) genera DR COBRO_CAJA_<método> / CR VENTA_CXC_CLIENTES")
    void registrarPagoConfirmado_generaAsiento() {
        Factura factura = factura(5);
        when(facturaRepository.findById(5)).thenReturn(Optional.of(factura));
        Pago pago = nuevoPago("40000.00", MetodoPago.EFECTIVO, null, factura);

        Pago registrado = service.registrarPago(pago);

        assertThat(registrado.getEstado()).isEqualTo(EstadoPago.CONFIRMADO);
        AsientoContable asiento = ultimoAsiento();
        assertThat(asiento.getPago()).isSameAs(registrado);
        assertThat(monto(asiento, "COBRO_CAJA_EFECTIVO", true)).isEqualByComparingTo("40000.00");
        assertThat(monto(asiento, "VENTA_CXC_CLIENTES", false)).isEqualByComparingTo("40000.00");
        assertThat(asiento.getTotalDebe()).isEqualByComparingTo(asiento.getTotalHaber());
    }

    @Test
    @DisplayName("registrarPago de adelanto sin factura acredita COBRO_ANTICIPOS_CLIENTES (sin NPE)")
    void registrarAdelanto_acreditaAnticipos() {
        Pago pago = nuevoPago("15000.00", MetodoPago.EFECTIVO, EstadoPago.CONFIRMADO, null);
        pago.setTipoPago(TipoPago.ADELANTO);

        service.registrarPago(pago);

        AsientoContable asiento = ultimoAsiento();
        assertThat(monto(asiento, "COBRO_CAJA_EFECTIVO", true)).isEqualByComparingTo("15000.00");
        assertThat(monto(asiento, "COBRO_ANTICIPOS_CLIENTES", false)).isEqualByComparingTo("15000.00");
    }

    @Test
    @DisplayName("registrarPago PENDIENTE no genera asiento; confirmarPago sí lo genera")
    void pendienteNoGenera_confirmarGenera() {
        Factura factura = factura(5);
        when(facturaRepository.findById(5)).thenReturn(Optional.of(factura));
        Pago pago = nuevoPago("10000.00", MetodoPago.TRANSFERENCIA, EstadoPago.PENDIENTE, factura);
        pago.setReferenciaBancaria("TRX-1");

        Pago registrado = service.registrarPago(pago);
        verify(asientoRepository, never()).save(any());

        when(pagoRepository.findById(77L)).thenReturn(Optional.of(registrado));
        service.confirmarPago(77L);

        AsientoContable asiento = ultimoAsiento();
        assertThat(monto(asiento, "COBRO_CAJA_TRANSFERENCIA", true)).isEqualByComparingTo("10000.00");
        assertThat(monto(asiento, "VENTA_CXC_CLIENTES", false)).isEqualByComparingTo("10000.00");
    }

    @Test
    @DisplayName("rechazarPago de un pago PENDIENTE no genera asiento")
    void rechazarPendiente_noGenera() {
        Pago pago = existente(EstadoPago.PENDIENTE, null);
        when(pagoRepository.findById(77L)).thenReturn(Optional.of(pago));

        service.rechazarPago(77L, "Fondos insuficientes");

        verify(asientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("conciliar un pago CONFIRMADO ya contabilizado no genera un segundo asiento")
    void conciliarConfirmado_noDuplica() {
        Pago pago = existente(EstadoPago.CONFIRMADO, factura(5));
        when(pagoRepository.findById(77L)).thenReturn(Optional.of(pago));
        when(asientoRepository.existeAsientoPagoParaPago(77L)).thenReturn(true);

        Pago conciliado = service.conciliarPago(77L);

        assertThat(conciliado.getEstado()).isEqualTo(EstadoPago.CONCILIADO);
        verify(asientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("conciliar un pago PENDIENTE sin asiento lo contabiliza una sola vez")
    void conciliarPendiente_generaUnaVez() {
        Pago pago = existente(EstadoPago.PENDIENTE, factura(5));
        when(pagoRepository.findById(77L)).thenReturn(Optional.of(pago));

        service.conciliarPago(77L);

        AsientoContable asiento = ultimoAsiento();
        assertThat(monto(asiento, "COBRO_CAJA_EFECTIVO", true)).isEqualByComparingTo("20000.00");
    }

    @Test
    @DisplayName("confirmarPago sobre un pago ya CONFIRMADO no genera asiento")
    void confirmarConfirmado_noop() {
        Pago pago = existente(EstadoPago.CONFIRMADO, factura(5));
        when(pagoRepository.findById(77L)).thenReturn(Optional.of(pago));

        service.confirmarPago(77L);

        verify(asientoRepository, never()).save(any());
    }

    // =========================================================================
    // Criterio 6 — reversión del cobro al anular
    // =========================================================================

    @Test
    @DisplayName("anularPago genera reversión espejo vinculada al pago; la suma por cuenta de ambos asientos es cero")
    void anularPago_revierteAsiento() {
        Pago pago = existente(EstadoPago.CONFIRMADO, factura(5));
        AsientoContableService asientoService =
                (AsientoContableService) ReflectionTestUtils.getField(service, "asientoContableService");
        AsientoContable original = asientoService.generarAsientoPago(pago);
        original.setIdAsiento(300L);
        original.setNumero("ASI-2026-0300");
        clearInvocations(asientoRepository);
        when(pagoRepository.findById(77L)).thenReturn(Optional.of(pago));
        when(asientoRepository.findAsientosPagoVigentes(77L)).thenReturn(List.of(original));

        service.anularPago(77L, "Cheque devuelto", 1);

        AsientoContable reversion = ultimoAsiento();
        assertThat(reversion.getAsientoRevertido()).isSameAs(original);
        assertThat(reversion.getPago()).isSameAs(pago);
        assertThat(reversion.getConcepto()).contains("ASI-2026-0300");
        assertThat(reversion.getTotalDebe()).isEqualByComparingTo(reversion.getTotalHaber());
        for (String clave : List.of("COBRO_CAJA_EFECTIVO", "VENTA_CXC_CLIENTES")) {
            BigDecimal saldo = monto(original, clave, true).subtract(monto(original, clave, false))
                    .add(monto(reversion, clave, true)).subtract(monto(reversion, clave, false));
            assertThat(saldo).isEqualByComparingTo("0");
        }
        // El original no se edita ni se borra
        assertThat(original.getEstado().name()).isEqualTo("CONTABILIZADO");
        verify(asientoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("anular un pago sin asiento (p. ej. PENDIENTE) no genera reversión")
    void anularPagoSinAsiento_noRevierte() {
        Pago pago = existente(EstadoPago.PENDIENTE, null);
        when(pagoRepository.findById(77L)).thenReturn(Optional.of(pago));

        service.anularPago(77L, "Duplicado", 1);

        verify(asientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteById rechaza un pago con asiento contabilizado")
    void deletePagoContabilizado_rechaza() {
        Pago pago = existente(EstadoPago.CONFIRMADO, null);
        when(pagoRepository.findById(77L)).thenReturn(Optional.of(pago));
        when(asientoRepository.existeAsientoContabilizadoParaPago(77L)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteById(77L))
                .isInstanceOf(IllegalStateException.class);
        verify(pagoRepository, never()).deleteById(anyLong());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Factura factura(int id) {
        Factura f = new Factura();
        f.setIdFactura(id);
        f.setNumeroFactura("F-" + id);
        f.setCliente(cliente);
        f.setSubtotal(new BigDecimal("100000.00"));
        f.setIgv(new BigDecimal("13000.00"));
        f.setTotal(new BigDecimal("113000.00"));
        f.setMonedaFE(MonedaFE.CRC);
        f.setTipoCambio(BigDecimal.ONE);
        return f;
    }

    private Pago nuevoPago(String monto, MetodoPago metodo, EstadoPago estado, Factura factura) {
        Pago p = new Pago();
        p.setCliente(cliente);
        p.setFactura(factura);
        p.setMonto(new BigDecimal(monto));
        p.setMetodoPago(metodo);
        p.setEstado(estado);
        p.setTipoPago(factura != null ? TipoPago.PARCIAL : TipoPago.ADELANTO);
        return p;
    }

    private Pago existente(EstadoPago estado, Factura factura) {
        Pago p = nuevoPago("20000.00", MetodoPago.EFECTIVO, estado, factura);
        p.setIdPago(77L);
        p.setNumeroPago("PAG-77");
        p.setFechaPago(LocalDateTime.now());
        return p;
    }

    private AsientoContable ultimoAsiento() {
        ArgumentCaptor<AsientoContable> captor = ArgumentCaptor.forClass(AsientoContable.class);
        verify(asientoRepository, atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }

    private BigDecimal monto(AsientoContable asiento, String clave, boolean debe) {
        return asiento.getDetalles().stream()
                .filter(d -> d.getCuenta() == cuentas.get(clave))
                .map(d -> debe ? d.getDebe() : d.getHaber())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
