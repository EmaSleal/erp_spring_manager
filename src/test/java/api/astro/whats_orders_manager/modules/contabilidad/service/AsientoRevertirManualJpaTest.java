package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.DetalleAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.repository.AsientoContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.DetalleAsientoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T-020 (ADR-023) — revertirManual contra una base real (H2, JPA): la reversión es un asiento
 * CONTABILIZADO con líneas espejo, fechado hoy y enlazado al original, que sigue CONTABILIZADO; los
 * saldos que leen los reportes (DetalleAsientoRepository) vuelven al valor previo. Revertir dos veces
 * o revertir la reversión se rechaza sin guardar nada.
 *
 * Sin transacción de test (NOT_SUPPORTED): revertirManual hace commit o rollback como en producción.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import(AsientoContableService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("T-020 — revertir un asiento manual (H2)")
class AsientoRevertirManualJpaTest {

    private static final LocalDate FECHA_ORIGINAL = LocalDate.of(2026, 9, 1);

    @Autowired private AsientoContableService service;
    @Autowired private AsientoContableRepository asientoRepository;
    @Autowired private DetalleAsientoRepository detalleRepository;
    @Autowired private CuentaContableRepository cuentaRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private TransactionTemplate tx;
    private CuentaContable gasto;
    private CuentaContable iva;
    private CuentaContable caja;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(s -> {
            gasto = cuenta("5.1.03.001", TipoCuenta.EGRESO);
            iva = cuenta("1.1.03", TipoCuenta.ACTIVO);
            caja = cuenta("1.1.01.001", TipoCuenta.ACTIVO);
        });
    }

    @AfterEach
    void limpiar() {
        tx.executeWithoutResult(s -> {
            detalleRepository.deleteAllInBatch();
            asientoRepository.findAll().forEach(a -> a.setAsientoRevertido(null));
            asientoRepository.flush();
            asientoRepository.deleteAllInBatch();
            cuentaRepository.deleteAllInBatch();
        });
    }

    @Test
    @DisplayName("revertir un MANUAL contabilizado: reversión espejo, hoy, enlazada; original CONTABILIZADO; saldos vuelven al valor previo")
    void revertirManual_creaReversionYSaldosVuelven() {
        LocalDate hoy = LocalDate.now();
        BigDecimal gastoPrevio = saldo(gasto, hoy);
        BigDecimal ivaPrevio = saldo(iva, hoy);
        BigDecimal cajaPrevio = saldo(caja, hoy);

        Long idOriginal = tx.execute(s -> asientoRepository.save(manualContabilizado()).getIdAsiento());
        assertThat(saldo(gasto, hoy)).isEqualByComparingTo(gastoPrevio.add(new BigDecimal("1000.00")));
        assertThat(saldo(caja, hoy)).isEqualByComparingTo(cajaPrevio.subtract(new BigDecimal("1130.00")));

        Long idReversion = service.revertirManual(idOriginal, "Registro duplicado").getIdAsiento();

        tx.executeWithoutResult(s -> {
            AsientoContable original = asientoRepository.findById(idOriginal).orElseThrow();
            AsientoContable reversion = asientoRepository.findById(idReversion).orElseThrow();
            assertThat(original.getEstado()).isEqualTo(EstadoAsiento.CONTABILIZADO);
            assertThat(original.getMotivoAnulacion()).isNull();
            assertThat(reversion.getEstado()).isEqualTo(EstadoAsiento.CONTABILIZADO);
            assertThat(reversion.getTipo()).isEqualTo(TipoAsiento.MANUAL);
            assertThat(reversion.getFecha()).isEqualTo(hoy);
            assertThat(reversion.getAsientoRevertido().getIdAsiento()).isEqualTo(idOriginal);
            assertThat(reversion.getConcepto()).contains(original.getNumero()).endsWith(" - Registro duplicado");

            List<String> espejo = reversion.getDetalles().stream()
                    .map(d -> d.getCuenta().getCodigo() + " " + d.getDebe().toPlainString() + "/" + d.getHaber().toPlainString())
                    .toList();
            assertThat(espejo).containsExactlyInAnyOrder(
                    "5.1.03.001 0.00/1000.00", "1.1.03 0.00/130.00", "1.1.01.001 1130.00/0.00");
            assertThat(service.obtenerReversionDe(idOriginal)).map(AsientoContable::getIdAsiento).contains(idReversion);
        });

        // Los saldos que leen los reportes vuelven al valor previo
        assertThat(saldo(gasto, hoy)).isEqualByComparingTo(gastoPrevio);
        assertThat(saldo(iva, hoy)).isEqualByComparingTo(ivaPrevio);
        assertThat(saldo(caja, hoy)).isEqualByComparingTo(cajaPrevio);
        List<Object[]> balance = detalleRepository.obtenerBalanceComprobacion(FECHA_ORIGINAL, hoy);
        for (Object[] fila : balance) {
            assertThat(((BigDecimal) fila[1]).subtract((BigDecimal) fila[2]))
                    .as("saldo neto de la cuenta %s en el balance de comprobación", fila[0])
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }
        assertThat(asientoRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("revertir dos veces y revertir la reversión: se rechazan sin crear asientos")
    void revertirDosVecesYReversion_rechazo() {
        Long idOriginal = tx.execute(s -> asientoRepository.save(manualContabilizado()).getIdAsiento());
        Long idReversion = service.revertirManual(idOriginal, "Primera reversión").getIdAsiento();

        assertThatThrownBy(() -> service.revertirManual(idOriginal, "Otra vez"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya fue revertido");
        assertThatThrownBy(() -> service.revertirManual(idReversion, "Revertir la reversión"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("es una reversión y no puede revertirse");

        assertThat(asientoRepository.count()).isEqualTo(2);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private BigDecimal saldo(CuentaContable cuenta, LocalDate fecha) {
        return detalleRepository.calcularSaldoCuentaHastaFecha(cuenta.getIdCuenta(), fecha);
    }

    private AsientoContable manualContabilizado() {
        AsientoContable a = new AsientoContable();
        a.setNumero("ASI-2026-0001");
        a.setFecha(FECHA_ORIGINAL);
        a.setConcepto("Compra de suministros");
        a.setTipo(TipoAsiento.MANUAL);
        a.setEstado(EstadoAsiento.CONTABILIZADO);
        a.agregarDetalle(linea(gasto, "1000.00", "0.00"));
        a.agregarDetalle(linea(iva, "130.00", "0.00"));
        a.agregarDetalle(linea(caja, "0.00", "1130.00"));
        return a;
    }

    private static DetalleAsiento linea(CuentaContable cuenta, String debe, String haber) {
        DetalleAsiento d = new DetalleAsiento();
        d.setCuenta(cuenta);
        d.setDebe(new BigDecimal(debe));
        d.setHaber(new BigDecimal(haber));
        d.setDescripcion("Línea " + cuenta.getCodigo());
        return d;
    }

    private CuentaContable cuenta(String codigo, TipoCuenta tipo) {
        CuentaContable c = new CuentaContable();
        c.setCodigo(codigo);
        c.setNombre("Cuenta " + codigo);
        c.setTipo(tipo);
        c.setNivel(4);
        c.setActiva(true);
        c.setAceptaMovimientos(true);
        return cuentaRepository.save(c);
    }
}
