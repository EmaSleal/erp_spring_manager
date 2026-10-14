package api.astro.whats_orders_manager.modules.configuracion.service;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.cliente.repository.ClienteRepository;
import api.astro.whats_orders_manager.modules.configuracion.model.CuentaBancaria;
import api.astro.whats_orders_manager.modules.configuracion.model.Empresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.CuentaBancariaRepository;
import api.astro.whats_orders_manager.modules.configuracion.repository.EmpresaRepository;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoCuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.enums.NaturalezaCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.model.DetalleAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.repository.AsientoContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaPorPagarRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.DetalleAsientoRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.service.CuentaContableService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoPago;
import api.astro.whats_orders_manager.modules.facturacion.enums.MetodoPago;
import api.astro.whats_orders_manager.modules.facturacion.enums.TipoPago;
import api.astro.whats_orders_manager.modules.facturacion.model.Pago;
import api.astro.whats_orders_manager.modules.facturacion.repository.PagoRepository;
import api.astro.whats_orders_manager.modules.proveedores.enums.EstadoPagoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.enums.MetodoPagoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.enums.TipoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.PagoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import api.astro.whats_orders_manager.modules.proveedores.repository.PagoProveedorRepository;
import api.astro.whats_orders_manager.modules.proveedores.repository.ProveedorRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * T-015 (ADR-018) — Configuración → Empresa → Cuentas Bancarias: cada operación sobre la cuenta
 * bancaria mantiene su hoja contable bajo 1.1.01.002, en la misma transacción (H2, JPA).
 *
 * Fixture: 1.1.01.002 Bancos agrupadora con BN CRC (.001) y BN USD (.002), como tras V30.
 * Los métodos corren sin transacción de prueba (NOT_SUPPORTED) para que un rechazo revierta como en
 * producción; la base se relee en una transacción nueva.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import({CuentaBancariaService.class, CuentaContableService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("T-015 — cuentas bancarias de la empresa con hoja contable propia (H2)")
class CuentaBancariaServiceJpaTest {

    @MockitoBean private EmpresaService empresaService;

    @Autowired private CuentaBancariaService service;
    @Autowired private CuentaContableService cuentaContableService;
    @Autowired private CuentaBancariaRepository cuentaBancariaRepository;
    @Autowired private CuentaContableRepository cuentaRepository;
    @Autowired private ParametroContableRepository parametroRepository;
    @Autowired private AsientoContableRepository asientoRepository;
    @Autowired private DetalleAsientoRepository detalleRepository;
    @Autowired private EmpresaRepository empresaRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private PagoRepository pagoRepository;
    @Autowired private ProveedorRepository proveedorRepository;
    @Autowired private CuentaPorPagarRepository cuentaPorPagarRepository;
    @Autowired private PagoProveedorRepository pagoProveedorRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private TransactionTemplate tx;
    private Empresa empresa;
    private CuentaContable bancos;
    private CuentaBancaria bnCrc;
    private CuentaBancaria bnUsd;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(s -> {
            empresa = new Empresa();
            empresa.setNombreEmpresa("Empresa de prueba S.A.");
            empresa.setActivo(true);
            empresa = empresaRepository.save(empresa);
            bancos = cuenta("1.1.01.002", "Bancos", null, false);
            bnCrc = banco("Banco Nacional - Colones", "CRC", cuenta("1.1.01.002.001", "Banco Nacional - Colones CRC", bancos, true));
            bnUsd = banco("Banco Nacional - Dólares", "USD", cuenta("1.1.01.002.002", "Banco Nacional - Dólares USD", bancos, true));
        });
        when(empresaService.getEmpresaPrincipal()).thenReturn(empresa);
    }

    @AfterEach
    void limpiar() {
        tx.executeWithoutResult(s -> {
            detalleRepository.deleteAllInBatch();
            asientoRepository.deleteAllInBatch();
            pagoRepository.deleteAllInBatch();
            pagoProveedorRepository.deleteAllInBatch();
            cuentaPorPagarRepository.deleteAllInBatch();
            proveedorRepository.deleteAllInBatch();
            clienteRepository.deleteAllInBatch();
            cuentaBancariaRepository.deleteAllInBatch();
            parametroRepository.deleteAllInBatch();
            cuentaRepository.findAll().forEach(c -> c.setCuentaPadre(null));
            cuentaRepository.flush();
            cuentaRepository.deleteAllInBatch();
            empresaRepository.deleteAllInBatch();
        });
    }

    // ── Alta ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("crear: crea la hoja con el siguiente código bajo 1.1.01.002, nombre '<entidad> <moneda>', ACTIVO/DEUDORA, nivel 5")
    void crear_creaHojaConSiguienteCodigo() {
        CuentaBancaria creada = service.crear("BAC San José - Colones", "CR28010200009050908416", "90509084", "CRC");

        tx.executeWithoutResult(s -> {
            CuentaBancaria releida = cuentaBancariaRepository.findById(creada.getIdCuentaBancaria()).orElseThrow();
            CuentaContable hoja = releida.getCuentaContable();
            assertThat(hoja.getCodigo()).isEqualTo("1.1.01.002.003");
            assertThat(hoja.getNombre()).isEqualTo("BAC San José - Colones CRC");
            assertThat(hoja.getCuentaPadre().getIdCuenta()).isEqualTo(bancos.getIdCuenta());
            assertThat(hoja.getTipo()).isEqualTo(TipoCuenta.ACTIVO);
            assertThat(hoja.getNaturaleza()).isEqualTo(NaturalezaCuenta.DEUDORA);
            assertThat(hoja.getNivel()).isEqualTo(5);
            assertThat(hoja.getActiva()).isTrue();
            assertThat(hoja.getAceptaMovimientos()).isTrue();
            assertThat(releida.getActiva()).isTrue();
            assertThat(releida.getMoneda()).isEqualTo("CRC");
            assertThat(releida.getEmpresa().getIdEmpresa()).isEqualTo(empresa.getIdEmpresa());
            assertThat(cuentaRepository.findByCodigo("1.1.01.002").orElseThrow().getAceptaMovimientos()).isFalse();
        });
    }

    @Test
    @DisplayName("crear: el código salta huecos (máximo + 1) y 1.1.01.002 hoja sin movimientos pasa a agrupadora")
    void crear_codigoMaximoMasUno_yBancosPasaAAgrupadora() {
        tx.executeWithoutResult(s -> {
            // Hueco: solo existe .002 tras borrar .001; 1.1.01.002 marcado como hoja (estado previo a V30)
            cuentaBancariaRepository.delete(cuentaBancariaRepository.findById(bnCrc.getIdCuentaBancaria()).orElseThrow());
            cuentaRepository.delete(cuentaRepository.findByCodigo("1.1.01.002.001").orElseThrow());
            CuentaContable b = cuentaRepository.findByCodigo("1.1.01.002").orElseThrow();
            b.setAceptaMovimientos(true);
        });

        CuentaBancaria creada = service.crear("Banco Popular", null, null, "usd");

        tx.executeWithoutResult(s -> {
            CuentaContable hoja = cuentaBancariaRepository.findById(creada.getIdCuentaBancaria()).orElseThrow().getCuentaContable();
            assertThat(hoja.getCodigo()).isEqualTo("1.1.01.002.003");
            assertThat(hoja.getNombre()).isEqualTo("Banco Popular USD");
            assertThat(cuentaRepository.findByCodigo("1.1.01.002").orElseThrow().getAceptaMovimientos()).isFalse();
        });
    }

    @Test
    @DisplayName("crear: sin entidad o con moneda inválida se rechaza y no se crea ninguna cuenta contable")
    void crear_datosInvalidos_rechazado() {
        long cuentasAntes = cuentaRepository.count();

        assertThatThrownBy(() -> service.crear("  ", null, null, "CRC"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("entidad");
        assertThatThrownBy(() -> service.crear("Banco X", null, null, "XYZ"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Moneda");

        assertThat(cuentaRepository.count()).isEqualTo(cuentasAntes);
        assertThat(cuentaBancariaRepository.count()).isEqualTo(2);
    }

    // ── Edición ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("editar la entidad renombra la hoja a '<entidad> <moneda>'")
    void editarEntidad_renombraHoja() {
        service.actualizar(bnCrc.getIdCuentaBancaria(), "BN Colones Operativa", "CR74015104210010004988",
                "100-01-042-000498-5", "CRC");

        tx.executeWithoutResult(s -> {
            CuentaBancaria releida = cuentaBancariaRepository.findById(bnCrc.getIdCuentaBancaria()).orElseThrow();
            assertThat(releida.getEntidad()).isEqualTo("BN Colones Operativa");
            assertThat(releida.getCuentaContable().getNombre()).isEqualTo("BN Colones Operativa CRC");
            assertThat(releida.getCuentaContable().getCodigo()).isEqualTo("1.1.01.002.001");
        });
    }

    @Test
    @DisplayName("cambiar la moneda con movimientos en la hoja se rechaza y no cambia nada")
    void cambiarMonedaConMovimientos_rechazado() {
        movimiento(bnCrc.getCuentaContable(), "1000.00");

        assertThatThrownBy(() -> service.actualizar(bnCrc.getIdCuentaBancaria(), "Banco Nacional - Colones",
                null, null, "USD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("moneda").hasMessageContaining("movimientos");

        tx.executeWithoutResult(s -> {
            CuentaBancaria releida = cuentaBancariaRepository.findById(bnCrc.getIdCuentaBancaria()).orElseThrow();
            assertThat(releida.getMoneda()).isEqualTo("CRC");
            assertThat(releida.getCuentaContable().getNombre()).isEqualTo("Banco Nacional - Colones CRC");
        });
    }

    @Test
    @DisplayName("cambiar la moneda sin movimientos ni pagos se permite y renombra la hoja")
    void cambiarMonedaSinMovimientos_permitido() {
        service.actualizar(bnCrc.getIdCuentaBancaria(), "Banco Nacional - Euros", null, null, "EUR");

        tx.executeWithoutResult(s -> {
            CuentaBancaria releida = cuentaBancariaRepository.findById(bnCrc.getIdCuentaBancaria()).orElseThrow();
            assertThat(releida.getMoneda()).isEqualTo("EUR");
            assertThat(releida.getCuentaContable().getNombre()).isEqualTo("Banco Nacional - Euros EUR");
        });
    }

    // ── Baja ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE con un cobro que la usa se rechaza (pide desactivarla) y no borra nada")
    void eliminarConCobro_rechazado() {
        tx.executeWithoutResult(s -> {
            Cliente cliente = new Cliente();
            cliente.setNombre("Cliente");
            cliente = clienteRepository.save(cliente);
            Pago pago = new Pago();
            pago.setNumeroPago("PAG-T015-0001");
            pago.setCliente(cliente);
            pago.setTipoPago(TipoPago.ADELANTO);
            pago.setMonto(new BigDecimal("1000.00"));
            pago.setMoneda(MonedaFE.CRC);
            pago.setMetodoPago(MetodoPago.TRANSFERENCIA);
            pago.setReferenciaBancaria("TRX-1");
            pago.setFechaPago(LocalDateTime.of(2026, 10, 1, 9, 0));
            pago.setEstado(EstadoPago.PENDIENTE);
            pago.setCuentaBancariaDestino(cuentaBancariaRepository.findById(bnCrc.getIdCuentaBancaria()).orElseThrow());
            pagoRepository.save(pago);
        });

        assertThatThrownBy(() -> service.eliminar(bnCrc.getIdCuentaBancaria()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Desactívela");
        assertSigueExistiendo(bnCrc);
    }

    @Test
    @DisplayName("DELETE con un pago a proveedor que la usa se rechaza (pide desactivarla) y no borra nada")
    void eliminarConPagoProveedor_rechazado() {
        tx.executeWithoutResult(s -> {
            Proveedor proveedor = proveedorRepository.save(Proveedor.builder().cedula("3101000001").nombre("Proveedor")
                    .tipoProveedor(TipoProveedor.values()[0]).saldoPendiente(BigDecimal.ZERO).build());
            CuentaPorPagar cpp = cuentaPorPagarRepository.save(CuentaPorPagar.builder().numero("CPP-T015-1")
                    .proveedor(proveedor).descripcion("Compra").monto(new BigDecimal("10.00"))
                    .saldoPendiente(BigDecimal.ZERO).moneda(MonedaFE.USD).tipoCambio(new BigDecimal("500"))
                    .fechaEmision(LocalDate.of(2026, 9, 1)).estado(EstadoCuentaPorPagar.PAGADA).build());
            pagoProveedorRepository.save(PagoProveedor.builder().numero("PP-T015-1").cuentaPorPagar(cpp)
                    .monto(new BigDecimal("10.00")).moneda(MonedaFE.USD).metodoPago(MetodoPagoProveedor.SINPE)
                    .fecha(LocalDate.of(2026, 9, 2)).estado(EstadoPagoProveedor.CONFIRMADO)
                    .cuentaBancariaOrigen(cuentaBancariaRepository.findById(bnUsd.getIdCuentaBancaria()).orElseThrow())
                    .build());
        });

        assertThatThrownBy(() -> service.eliminar(bnUsd.getIdCuentaBancaria()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pagos a proveedor").hasMessageContaining("Desactívela");
        assertSigueExistiendo(bnUsd);
    }

    @Test
    @DisplayName("DELETE con movimientos en la hoja se rechaza (pide desactivarla) y no borra nada")
    void eliminarConMovimientos_rechazado() {
        movimiento(bnCrc.getCuentaContable(), "500.00");

        assertThatThrownBy(() -> service.eliminar(bnCrc.getIdCuentaBancaria()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("movimientos").hasMessageContaining("Desactívela");
        assertSigueExistiendo(bnCrc);
    }

    @Test
    @DisplayName("DELETE sin pagos ni movimientos borra la cuenta bancaria y su hoja; 1.1.01.002 sigue agrupadora aunque quede sin hojas")
    void eliminarSinPagos_borraAmbas() {
        Long hojaCrc = bnCrc.getCuentaContable().getIdCuenta();
        Long hojaUsd = bnUsd.getCuentaContable().getIdCuenta();

        service.eliminar(bnCrc.getIdCuentaBancaria());
        service.eliminar(bnUsd.getIdCuentaBancaria());

        tx.executeWithoutResult(s -> {
            assertThat(cuentaBancariaRepository.count()).isZero();
            assertThat(cuentaRepository.findById(hojaCrc)).isEmpty();
            assertThat(cuentaRepository.findById(hojaUsd)).isEmpty();
            CuentaContable b = cuentaRepository.findByCodigo("1.1.01.002").orElseThrow();
            assertThat(b.getAceptaMovimientos()).isFalse();
        });
    }

    // ── Estado ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("desactivar la cuenta bancaria desactiva su hoja; reactivarla la reactiva")
    void desactivar_desactivaHoja() {
        service.cambiarEstado(bnUsd.getIdCuentaBancaria(), false);

        tx.executeWithoutResult(s -> {
            CuentaBancaria releida = cuentaBancariaRepository.findById(bnUsd.getIdCuentaBancaria()).orElseThrow();
            assertThat(releida.getActiva()).isFalse();
            assertThat(releida.getCuentaContable().getActiva()).isFalse();
        });
        assertThat(service.listarActivas("USD")).isEmpty();
        assertThat(service.listar(true)).hasSize(2);

        service.cambiarEstado(bnUsd.getIdCuentaBancaria(), true);

        tx.executeWithoutResult(s -> {
            CuentaBancaria releida = cuentaBancariaRepository.findById(bnUsd.getIdCuentaBancaria()).orElseThrow();
            assertThat(releida.getActiva()).isTrue();
            assertThat(releida.getCuentaContable().getActiva()).isTrue();
        });
    }

    @Test
    @DisplayName("listarActivas(moneda): solo cuentas activas de esa moneda")
    void listarActivas_filtraPorMonedaYEstado() {
        service.crear("BAC San José - Colones", null, null, "CRC");
        CuentaBancaria inactiva = service.crear("Banco Popular - Colones", null, null, "CRC");
        service.cambiarEstado(inactiva.getIdCuentaBancaria(), false);

        assertThat(service.listarActivas("CRC")).extracting(CuentaBancaria::getEntidad)
                .containsExactlyInAnyOrder("Banco Nacional - Colones", "BAC San José - Colones");
        assertThat(service.listarActivas("USD")).extracting(CuentaBancaria::getEntidad)
                .containsExactly("Banco Nacional - Dólares");
    }

    // ── Plan de cuentas ──────────────────────────────────────────────────────

    @Test
    @DisplayName("el plan de cuentas no elimina ni desactiva la hoja de una cuenta bancaria (se gestiona desde Configuración → Empresa)")
    void planDeCuentas_noTocaHojaBancaria() {
        Long hoja = bnCrc.getCuentaContable().getIdCuenta();

        assertThatThrownBy(() -> cuentaContableService.eliminar(hoja))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Banco Nacional - Colones").hasMessageContaining("Configuración");
        assertThatThrownBy(() -> cuentaContableService.cambiarEstado(hoja, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Banco Nacional - Colones");

        tx.executeWithoutResult(s -> assertThat(cuentaRepository.findById(hoja).orElseThrow().getActiva()).isTrue());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void assertSigueExistiendo(CuentaBancaria banco) {
        tx.executeWithoutResult(s -> {
            assertThat(cuentaBancariaRepository.findById(banco.getIdCuentaBancaria())).isPresent();
            assertThat(cuentaRepository.findById(banco.getCuentaContable().getIdCuenta())).isPresent();
        });
    }

    private void movimiento(CuentaContable cuenta, String monto) {
        tx.executeWithoutResult(s -> {
            AsientoContable asiento = new AsientoContable();
            asiento.setNumero("ASI-T015-" + System.nanoTime() % 100000);
            asiento.setFecha(LocalDate.of(2026, 10, 1));
            asiento.setConcepto("Movimiento de prueba");
            asiento.setTipo(TipoAsiento.MANUAL);
            asiento.setEstado(EstadoAsiento.CONTABILIZADO);
            asiento = asientoRepository.save(asiento);
            DetalleAsiento d = new DetalleAsiento();
            d.setAsiento(asiento);
            d.setCuenta(cuenta);
            d.setDebe(new BigDecimal(monto));
            d.setHaber(BigDecimal.ZERO);
            detalleRepository.save(d);
        });
    }

    private CuentaContable cuenta(String codigo, String nombre, CuentaContable padre, boolean acepta) {
        CuentaContable c = new CuentaContable();
        c.setCodigo(codigo);
        c.setNombre(nombre);
        c.setTipo(TipoCuenta.ACTIVO);
        c.setNaturaleza(NaturalezaCuenta.DEUDORA);
        c.setNivel(codigo.split("\\.").length);
        c.setActiva(true);
        c.setAceptaMovimientos(acepta);
        c.setCuentaPadre(padre);
        return cuentaRepository.save(c);
    }

    private CuentaBancaria banco(String entidad, String moneda, CuentaContable hoja) {
        CuentaBancaria b = new CuentaBancaria();
        b.setEmpresa(empresa);
        b.setEntidad(entidad);
        b.setMoneda(moneda);
        b.setActiva(true);
        b.setOrden(0);
        b.setCuentaContable(hoja);
        return cuentaBancariaRepository.save(b);
    }
}
