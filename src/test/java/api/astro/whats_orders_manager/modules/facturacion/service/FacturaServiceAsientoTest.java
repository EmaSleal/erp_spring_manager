package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.ParametroContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.AsientoContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.DetalleAsientoRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.facturacion.dto.mapper.FacturaMapper;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoPago;
import api.astro.whats_orders_manager.modules.facturacion.model.ConfiguracionFacturacion;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.Pago;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.repository.TipoCambioRepository;
import api.astro.whats_orders_manager.modules.facturacion.enums.FuenteTipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.model.TipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.service.impl.FacturaServiceImpl;
import api.astro.whats_orders_manager.modules.inventario.service.MovimientoInventarioService;
import api.astro.whats_orders_manager.modules.notificacion.service.NotificacionService;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.repository.ProductoRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T-005 — FacturaServiceImpl genera, protege y revierte el asiento de venta (ADR-008).
 * Criterios 3, 4 y 7.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FacturaServiceImpl — contabilización de ventas (ADR-008)")
class FacturaServiceAsientoTest {

    @Mock private FacturaRepository facturaRepository;
    @Mock private ConfiguracionFacturacionService configuracionFacturacionService;
    @Mock private NotificacionService notificacionService;
    @Mock private PagoService pagoService;
    @Mock private LineaFacturaService lineaFacturaService;
    @Mock private FacturaMapper facturaMapper;
    @Mock private MovimientoInventarioService movimientoInventarioService;
    @Mock private AsientoContableService asientoServiceMock;

    // Repositorios para el AsientoContableService real (criterio 3)
    @Mock private AsientoContableRepository asientoRepository;
    @Mock private DetalleAsientoRepository detalleRepository;
    @Mock private CuentaContableRepository cuentaRepository;
    @Mock private ParametroContableRepository parametroContableRepository;
    @Mock private TipoCambioRepository tipoCambioRepository;

    private CuentaContable cxc;
    private CuentaContable ingresos;
    private CuentaContable iva;

    @BeforeEach
    void setUp() {
        ConfiguracionFacturacion config = mock(ConfiguracionFacturacion.class);
        when(configuracionFacturacionService.getOrCreateConfiguracion()).thenReturn(config);
        when(facturaRepository.save(any(Factura.class))).thenAnswer(inv -> {
            Factura f = inv.getArgument(0);
            if (f.getIdFactura() == null) {
                f.setIdFactura(42);
            }
            // Simulates AuditingEntityListener: @CreatedDate (fechaEmision) is set on insert
            if (f.getCreateDate() == null) {
                f.setCreateDate(LocalDateTime.now());
            }
            return f;
        });

        cxc = cuenta(1L, "VENTA_CXC_CLIENTES");
        ingresos = cuenta(2L, "VENTA_INGRESOS");
        iva = cuenta(3L, "VENTA_IVA_POR_PAGAR");
        param("VENTA_CXC_CLIENTES", cxc);
        param("VENTA_INGRESOS", ingresos);
        param("VENTA_IVA_POR_PAGAR", iva);
        when(asientoRepository.findUltimoNumeroDelAnio(anyString())).thenReturn(Optional.empty());
        when(asientoRepository.save(any(AsientoContable.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // =========================================================================
    // Criterio 3 — asiento de venta al crear la factura
    // =========================================================================

    @Test
    @DisplayName("crear factura 100000 + 13000 produce asiento AUTOMATICO_VENTA contabilizado y vinculado")
    void crearFactura_generaAsientoVenta() {
        FacturaServiceImpl service = servicio(new AsientoContableService(
                asientoRepository, detalleRepository, cuentaRepository, parametroContableRepository));
        Factura factura = nuevaFactura("100000.00", "13000.00", "113000.00", MonedaFE.CRC, null);

        Factura guardada = service.save(factura);

        ArgumentCaptor<AsientoContable> captor = ArgumentCaptor.forClass(AsientoContable.class);
        verify(asientoRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        AsientoContable asiento = captor.getValue();
        assertThat(asiento.getTipo()).isEqualTo(TipoAsiento.AUTOMATICO_VENTA);
        assertThat(asiento.getEstado()).isEqualTo(EstadoAsiento.CONTABILIZADO);
        assertThat(asiento.getFactura()).isSameAs(guardada);
        assertThat(monto(asiento, cxc, true)).isEqualByComparingTo("113000.00");
        assertThat(monto(asiento, ingresos, false)).isEqualByComparingTo("100000.00");
        assertThat(monto(asiento, iva, false)).isEqualByComparingTo("13000.00");
        assertThat(asiento.getTotalDebe()).isEqualByComparingTo(asiento.getTotalHaber());
    }

    @Test
    @DisplayName("si generarAsientoVenta falla, la excepción se propaga dentro de la transacción (rollback)")
    void crearFactura_fallaAsiento_rollback() throws Exception {
        FacturaServiceImpl service = servicio(asientoServiceMock);
        when(asientoServiceMock.generarAsientoVenta(any()))
                .thenThrow(new IllegalStateException("Parámetro contable no configurado: VENTA_INGRESOS"));
        Factura factura = nuevaFactura("100000.00", "13000.00", "113000.00", MonedaFE.CRC, null);

        assertThatThrownBy(() -> service.save(factura))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("VENTA_INGRESOS");

        // Nada posterior al asiento se ejecuta (contador, inventario)
        verify(configuracionFacturacionService, never()).incrementarNumeroFactura();
        verify(movimientoInventarioService, never()).registrarSalida(any(), any(), any(), any(), any(), any(), any());
        // save() es transaccional de escritura: una RuntimeException provoca rollback de la factura
        Transactional tx = FacturaServiceImpl.class.getMethod("save", Factura.class).getAnnotation(Transactional.class);
        assertThat(tx).isNotNull();
        assertThat(tx.readOnly()).isFalse();
        assertThat(RuntimeException.class).isAssignableFrom(IllegalStateException.class);
    }

    @Test
    @DisplayName("T-007 criterio 11: crear factura USD sin tipo de cambio y sin tasa registrada falla con mensaje claro y no genera asiento")
    void crearFacturaUsdSinTipoCambio_falla() {
        FacturaServiceImpl service = servicio(asientoServiceMock);
        when(tipoCambioRepository.findFirstByMonedaDestinoAndActivoTrueAndFechaLessThanEqualOrderByFechaDescIdDesc(
                any(), any())).thenReturn(Optional.empty());
        Factura factura = nuevaFactura("100.00", "13.00", "113.00", MonedaFE.USD, null);

        assertThatThrownBy(() -> service.save(factura))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No hay tipo de cambio de USD para " + LocalDate.now() + "; regístrelo");
        verify(asientoServiceMock, never()).generarAsientoVenta(any());
        verify(facturaRepository, never()).save(any());
    }

    @Test
    @DisplayName("T-007 criterio 11: crear factura USD sin tipo de cambio usa la tasa de venta registrada para su fecha")
    void crearFacturaUsdSinTipoCambio_usaTasaDeLaTabla() {
        FacturaServiceImpl service = servicio(new AsientoContableService(
                asientoRepository, detalleRepository, cuentaRepository, parametroContableRepository));
        when(tipoCambioRepository.findFirstByMonedaDestinoAndActivoTrueAndFechaLessThanEqualOrderByFechaDescIdDesc(
                MonedaFE.USD, LocalDate.now())).thenReturn(Optional.of(TipoCambio.builder()
                        .monedaOrigen(MonedaFE.CRC).monedaDestino(MonedaFE.USD).fecha(LocalDate.now())
                        .tasaCompra(new BigDecimal("495")).tasaVenta(new BigDecimal("505.12345"))
                        .fuente(FuenteTipoCambio.MANUAL).activo(true).build()));
        Factura factura = nuevaFactura("100.00", "13.00", "113.00", MonedaFE.USD, null);

        Factura guardada = service.save(factura);

        assertThat(guardada.getTipoCambio()).isEqualByComparingTo("505.12345");
        ArgumentCaptor<AsientoContable> captor = ArgumentCaptor.forClass(AsientoContable.class);
        verify(asientoRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        // 113 × 505.12345 = 57078.95 (HALF_UP)
        assertThat(monto(captor.getValue(), cxc, true)).isEqualByComparingTo("57078.95");
    }

    @Test
    @DisplayName("criterio 4: crear factura USD convierte el asiento a CRC con el tipo de cambio")
    void crearFacturaUsd_asientoEnCrc() {
        FacturaServiceImpl service = servicio(new AsientoContableService(
                asientoRepository, detalleRepository, cuentaRepository, parametroContableRepository));
        Factura factura = nuevaFactura("100.00", "13.00", "113.00", MonedaFE.USD, new BigDecimal("500.00"));

        service.save(factura);

        ArgumentCaptor<AsientoContable> captor = ArgumentCaptor.forClass(AsientoContable.class);
        verify(asientoRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        AsientoContable asiento = captor.getValue();
        assertThat(monto(asiento, cxc, true)).isEqualByComparingTo("56500.00");
        assertThat(monto(asiento, ingresos, false)).isEqualByComparingTo("50000.00");
        assertThat(monto(asiento, iva, false)).isEqualByComparingTo("6500.00");
    }

    // =========================================================================
    // T-006 (ADR-009) — save() toma los totales del desglose por línea
    // =========================================================================

    @Test
    @DisplayName("T-006 criterio 4: factura con líneas 1130 + 565 → 1500.00 + 195.00 = 1695.00 (desglose ADR-009)")
    void crearFacturaConLineas_totalesDelDesglose() {
        FacturaServiceImpl service = servicio(new AsientoContableService(
                asientoRepository, detalleRepository, cuentaRepository, parametroContableRepository));
        ReflectionTestUtils.setField(service, "totalesFacturaService",
                new TotalesFacturaService(new CalculadoraImpuestoService(null), mock(ProductoRepository.class)));
        Factura factura = nuevaFactura("1695.00", "0.00", "0.00", MonedaFE.CRC, null);
        factura.getLineas().add(lineaGravada(factura, 1, "1130.00", "1130.00"));
        factura.getLineas().add(lineaGravada(factura, 5, "113.00", "565.00"));

        Factura guardada = service.save(factura);

        assertThat(guardada.getSubtotal()).isEqualByComparingTo("1500.00");
        assertThat(guardada.getIgv()).isEqualByComparingTo("195.00");
        assertThat(guardada.getTotal()).isEqualByComparingTo("1695.00");

        ArgumentCaptor<AsientoContable> captor = ArgumentCaptor.forClass(AsientoContable.class);
        verify(asientoRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        AsientoContable asiento = captor.getValue();
        assertThat(monto(asiento, cxc, true)).isEqualByComparingTo("1695.00");
        assertThat(monto(asiento, ingresos, false)).isEqualByComparingTo("1500.00");
        assertThat(monto(asiento, iva, false)).isEqualByComparingTo("195.00");
    }

    @Test
    @DisplayName("T-006 criterio 4: factura sin líneas no suma IVA sobre el subtotal (montos recibidos)")
    void crearFacturaSinLineas_noSumaIva() {
        FacturaServiceImpl service = servicio(asientoServiceMock);
        Factura factura = nuevaFactura("1000.00", "0.00", "0.00", MonedaFE.CRC, null);

        Factura guardada = service.save(factura);

        assertThat(guardada.getSubtotal()).isEqualByComparingTo("1000.00");
        assertThat(guardada.getIgv()).isEqualByComparingTo("0.00");
        assertThat(guardada.getTotal()).isEqualByComparingTo("1000.00");
    }

    @SuppressWarnings("deprecation")
    private static LineaFactura lineaGravada(Factura factura, int cantidad, String precio, String subtotal) {
        Producto producto = new Producto();
        producto.setGravado(true);
        producto.setPorcentajeImpuesto(new BigDecimal("13.00"));
        LineaFactura linea = new LineaFactura();
        linea.setFactura(factura);
        linea.setProducto(producto);
        linea.setCantidad(cantidad);
        linea.setPrecioUnitario(new BigDecimal(precio));
        linea.setSubtotal(new BigDecimal(subtotal));
        return linea;
    }

    // =========================================================================
    // Criterio 7a — no se borra una factura con asiento contabilizado
    // =========================================================================

    @Test
    @DisplayName("eliminarConValidacion rechaza una factura con asiento contabilizado")
    void eliminarConValidacion_conAsiento_rechaza() {
        FacturaServiceImpl service = servicio(asientoServiceMock);
        Factura factura = existente(42);
        when(facturaRepository.findById(42)).thenReturn(Optional.of(factura));
        when(asientoServiceMock.tieneAsientoContabilizadoFactura(42)).thenReturn(true);

        assertThatThrownBy(() -> service.eliminarConValidacion(42))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("asientos contables");
        verify(facturaRepository, never()).deleteById(anyInt());
    }

    @Test
    @DisplayName("deleteById rechaza una factura con asiento contabilizado")
    void deleteById_conAsiento_rechaza() {
        FacturaServiceImpl service = servicio(asientoServiceMock);
        when(asientoServiceMock.tieneAsientoContabilizadoFactura(42)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteById(42))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Anúlela");
        verify(facturaRepository, never()).deleteById(anyInt());
    }

    @Test
    @DisplayName("deleteById borra una factura sin asientos (comportamiento previo)")
    void deleteById_sinAsiento_borra() {
        FacturaServiceImpl service = servicio(asientoServiceMock);

        service.deleteById(43);

        verify(facturaRepository).deleteById(43);
    }

    // =========================================================================
    // Criterio 7b — anulación de factura
    // =========================================================================

    @Test
    @DisplayName("anular factura sin pagos válidos revierte el asiento de venta y la marca como anulada")
    void anular_sinPagos_revierteYMarca() {
        FacturaServiceImpl service = servicio(asientoServiceMock);
        Factura factura = existente(42);
        when(facturaRepository.findById(42)).thenReturn(Optional.of(factura));
        when(pagoService.findPagosValidosByFacturaId(42)).thenReturn(List.of());

        Factura anulada = service.anular(42, "Error de digitación");

        verify(asientoServiceMock).revertirAsientoVenta(any(Factura.class), anyString());
        assertThat(anulada.estaAnulada()).isTrue();
        assertThat(anulada.getFechaAnulacion()).isNotNull();
        assertThat(anulada.getMotivoAnulacion()).isEqualTo("Error de digitación");
    }

    @Test
    @DisplayName("anular con el AsientoContableService real deja saldo cero por cuenta")
    void anular_reversionRealSaldoCero() {
        AsientoContableService real = new AsientoContableService(
                asientoRepository, detalleRepository, cuentaRepository, parametroContableRepository);
        FacturaServiceImpl service = servicio(real);
        Factura factura = existente(42);
        AsientoContable original = real.generarAsientoVenta(factura);
        original.setIdAsiento(900L);
        original.setNumero("ASI-2026-0900");
        org.mockito.Mockito.clearInvocations(asientoRepository);
        when(facturaRepository.findById(42)).thenReturn(Optional.of(factura));
        when(asientoRepository.findAsientosVentaVigentes(42L)).thenReturn(List.of(original));

        service.anular(42, "Cliente canceló");

        ArgumentCaptor<AsientoContable> captor = ArgumentCaptor.forClass(AsientoContable.class);
        verify(asientoRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        AsientoContable reversion = captor.getValue();
        assertThat(reversion.getAsientoRevertido()).isSameAs(original);
        assertThat(reversion.getFactura()).isSameAs(factura);
        for (CuentaContable c : List.of(cxc, ingresos, iva)) {
            BigDecimal saldo = monto(original, c, true).subtract(monto(original, c, false))
                    .add(monto(reversion, c, true)).subtract(monto(reversion, c, false));
            assertThat(saldo).isEqualByComparingTo("0");
        }
    }

    @Test
    @DisplayName("no se puede anular una factura con pagos CONFIRMADOS o CONCILIADOS")
    void anular_conPagosValidos_rechaza() {
        FacturaServiceImpl service = servicio(asientoServiceMock);
        Factura factura = existente(42);
        Pago pago = new Pago();
        pago.setEstado(EstadoPago.CONFIRMADO);
        when(facturaRepository.findById(42)).thenReturn(Optional.of(factura));
        when(pagoService.findPagosValidosByFacturaId(42)).thenReturn(List.of(pago));

        assertThatThrownBy(() -> service.anular(42, "x"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pagos");
        verify(asientoServiceMock, never()).revertirAsientoVenta(any(), anyString());
        assertThat(factura.estaAnulada()).isFalse();
    }

    @Test
    @DisplayName("anular exige motivo y no se repite sobre una factura ya anulada")
    void anular_validaciones() {
        FacturaServiceImpl service = servicio(asientoServiceMock);
        assertThatThrownBy(() -> service.anular(42, " "))
                .isInstanceOf(IllegalArgumentException.class);

        Factura factura = existente(42);
        factura.setAnulada(true);
        when(facturaRepository.findById(42)).thenReturn(Optional.of(factura));
        assertThatThrownBy(() -> service.anular(42, "otra vez"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya está anulada");
    }

    // =========================================================================
    // Criterio 7c — edición de montos
    // =========================================================================

    @Test
    @DisplayName("editar una factura existente sincroniza su asiento (no genera uno nuevo directamente)")
    void editarFactura_sincronizaAsiento() {
        FacturaServiceImpl service = servicio(asientoServiceMock);
        Factura existente = existente(42);
        when(facturaRepository.findById(42)).thenReturn(Optional.of(existente));
        Factura editada = existente(42);
        editada.setSubtotal(new BigDecimal("200000.00"));
        editada.setIgv(new BigDecimal("26000.00"));
        editada.setTotal(new BigDecimal("226000.00"));

        service.save(editada);

        verify(asientoServiceMock).sincronizarAsientoVenta(editada);
        verify(asientoServiceMock, never()).generarAsientoVenta(any());
    }

    @Test
    @DisplayName("editar montos con el servicio real genera reversión + asiento nuevo")
    void editarFactura_reversionYAsientoNuevo() {
        AsientoContableService real = new AsientoContableService(
                asientoRepository, detalleRepository, cuentaRepository, parametroContableRepository);
        FacturaServiceImpl service = servicio(real);
        Factura factura = existente(42);
        AsientoContable original = real.generarAsientoVenta(factura);
        original.setIdAsiento(901L);
        org.mockito.Mockito.clearInvocations(asientoRepository);
        when(facturaRepository.findById(42)).thenReturn(Optional.of(existente(42)));
        when(asientoRepository.findAsientosVentaVigentes(42L)).thenReturn(List.of(original));

        Factura editada = existente(42);
        editada.setSubtotal(new BigDecimal("200000.00"));
        editada.setIgv(new BigDecimal("26000.00"));
        editada.setTotal(new BigDecimal("226000.00"));
        service.save(editada);

        ArgumentCaptor<AsientoContable> captor = ArgumentCaptor.forClass(AsientoContable.class);
        verify(asientoRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        List<AsientoContable> guardados = captor.getAllValues();
        assertThat(guardados).anyMatch(a -> a.getAsientoRevertido() == original
                && monto(a, cxc, false).compareTo(new BigDecimal("113000.00")) == 0);
        assertThat(guardados).anyMatch(a -> a.getAsientoRevertido() == null
                && monto(a, cxc, true).compareTo(new BigDecimal("226000.00")) == 0);
        // El original no se edita
        assertThat(monto(original, cxc, true)).isEqualByComparingTo("113000.00");
    }

    @Test
    @DisplayName("no se puede editar una factura anulada")
    void editarFacturaAnulada_rechaza() {
        FacturaServiceImpl service = servicio(asientoServiceMock);
        Factura existente = existente(42);
        existente.setAnulada(true);
        when(facturaRepository.findById(42)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.save(existente(42)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("anulada");
        verify(facturaRepository, never()).save(any());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private FacturaServiceImpl servicio(AsientoContableService asientoService) {
        FacturaServiceImpl service = new FacturaServiceImpl();
        ReflectionTestUtils.setField(service, "facturaRepository", facturaRepository);
        ReflectionTestUtils.setField(service, "configuracionFacturacionService", configuracionFacturacionService);
        ReflectionTestUtils.setField(service, "notificacionService", notificacionService);
        ReflectionTestUtils.setField(service, "pagoService", pagoService);
        ReflectionTestUtils.setField(service, "lineaFacturaService", lineaFacturaService);
        ReflectionTestUtils.setField(service, "facturaMapper", facturaMapper);
        ReflectionTestUtils.setField(service, "movimientoInventarioService", movimientoInventarioService);
        ReflectionTestUtils.setField(service, "asientoContableService", asientoService);
        ReflectionTestUtils.setField(service, "tipoCambioService", new TipoCambioService(tipoCambioRepository));
        return service;
    }

    private static Factura nuevaFactura(String subtotal, String igv, String total, MonedaFE moneda, BigDecimal tc) {
        Cliente cliente = new Cliente();
        cliente.setIdCliente(1);
        cliente.setNombre("Cliente Prueba");
        Factura f = new Factura();
        f.setCliente(cliente);
        f.setNumeroFactura("F-0001");
        f.setSerie("F");
        f.setSubtotal(new BigDecimal(subtotal));
        f.setIgv(new BigDecimal(igv));
        f.setTotal(new BigDecimal(total));
        f.setMonedaFE(moneda);
        f.setTipoCambio(tc);
        return f;
    }

    private static Factura existente(int id) {
        Factura f = nuevaFactura("100000.00", "13000.00", "113000.00", MonedaFE.CRC, BigDecimal.ONE);
        f.setIdFactura(id);
        // Already persisted: its @CreatedDate (fechaEmision) was set on insert
        f.setCreateDate(LocalDateTime.of(2026, 9, 15, 10, 0));
        return f;
    }

    private static CuentaContable cuenta(long id, String codigo) {
        CuentaContable c = new CuentaContable();
        c.setIdCuenta(id);
        c.setCodigo(codigo);
        c.setNombre(codigo);
        c.setActiva(true);
        c.setAceptaMovimientos(true);
        return c;
    }

    private void param(String clave, CuentaContable cuenta) {
        ParametroContable p = new ParametroContable();
        p.setClave(clave);
        p.setCuentaContable(cuenta);
        when(parametroContableRepository.findByClave(clave)).thenReturn(Optional.of(p));
    }

    private static BigDecimal monto(AsientoContable asiento, CuentaContable cuenta, boolean debe) {
        return asiento.getDetalles().stream()
                .filter(d -> d.getCuenta() == cuenta)
                .map(d -> debe ? d.getDebe() : d.getHaber())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
