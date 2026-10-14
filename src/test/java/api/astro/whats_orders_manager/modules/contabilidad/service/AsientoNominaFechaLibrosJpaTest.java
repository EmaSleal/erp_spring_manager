package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.ParametroContable;
import api.astro.whats_orders_manager.modules.nomina.enums.EstadoNomina;
import api.astro.whats_orders_manager.modules.nomina.enums.TipoNomina;
import api.astro.whats_orders_manager.modules.nomina.model.Nomina;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * T-011 (ADR-014) — a September 2026 payroll entry, posted against a real JPA provider (H2), shows
 * up in the general journal (posted entries by period) and in the general ledger for September,
 * and not for the current month.
 *
 * "Libro diario" = {@link AsientoContableService#obtenerContabilizadosPorPeriodo} (posted entries
 * between two dates); "libro mayor" = {@link CalculoContableService#obtenerLibroMayor}.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import({AsientoContableService.class, CalculoContableService.class})
@DisplayName("Asiento de nómina en libro diario y mayor por periodo (H2)")
class AsientoNominaFechaLibrosJpaTest {

    private static final LocalDate SEPT_INICIO = LocalDate.of(2026, 9, 1);
    private static final LocalDate SEPT_FIN = LocalDate.of(2026, 9, 30);

    @Autowired private AsientoContableService asientoService;
    @Autowired private CalculoContableService calculoService;
    @Autowired private TestEntityManager em;

    private CuentaContable gastoSueldos;
    private CuentaContable cargasSociales;

    @BeforeEach
    void setUp() {
        gastoSueldos = cuenta("5.1.01.001", TipoCuenta.EGRESO);
        param("NOMINA_GASTO_SUELDOS", gastoSueldos);
        cargasSociales = cuenta("5.1.02.003", TipoCuenta.EGRESO);
        param("NOMINA_GASTO_CARGAS_SOCIALES", cargasSociales);
        param("NOMINA_CCSS_PATRONAL_POR_PAGAR", cuenta("2.1.01.002", TipoCuenta.PASIVO));
        param("NOMINA_CCSS_POR_PAGAR", cuenta("2.1.01.003", TipoCuenta.PASIVO));
        param("NOMINA_INS_POR_PAGAR", cuenta("2.1.01.004", TipoCuenta.PASIVO));
        param("NOMINA_RENTA_POR_PAGAR", cuenta("2.1.01.005", TipoCuenta.PASIVO));
        param("NOMINA_SALARIOS_POR_PAGAR", cuenta("2.1.01.001", TipoCuenta.PASIVO));
        em.flush();
    }

    @Test
    @DisplayName("el asiento de nómina de septiembre aparece en septiembre y no en el mes actual")
    void asientoNomina_enLibrosDeSeptiembre_noDelMesActual() {
        // The "current month" check is only meaningful when today is not in September 2026
        assumeFalse(YearMonth.now().equals(YearMonth.from(SEPT_FIN)));

        AsientoContable asiento = asientoService.generarAsientoNomina(nominaSeptiembre());
        em.flush();
        em.clear();

        YearMonth mesActual = YearMonth.now();
        LocalDate inicioMesActual = mesActual.atDay(1);
        LocalDate finMesActual = mesActual.atEndOfMonth();

        // Libro diario
        List<AsientoContable> diarioSeptiembre = asientoService.obtenerContabilizadosPorPeriodo(SEPT_INICIO, SEPT_FIN);
        List<AsientoContable> diarioMesActual = asientoService.obtenerContabilizadosPorPeriodo(inicioMesActual, finMesActual);
        assertThat(diarioSeptiembre).extracting(AsientoContable::getNumero).containsExactly(asiento.getNumero());
        assertThat(diarioSeptiembre.get(0).getFecha()).isEqualTo(SEPT_FIN);
        assertThat(diarioMesActual).extracting(AsientoContable::getNumero).doesNotContain(asiento.getNumero());

        // Libro mayor (sueldos: bruto 1 000 000; cargas sociales: CCSS patronal 268 300, ADR-017)
        List<CalculoContableService.MovimientoCuenta> mayorSeptiembre =
                calculoService.obtenerLibroMayor(gastoSueldos.getIdCuenta(), SEPT_INICIO, SEPT_FIN);
        List<CalculoContableService.MovimientoCuenta> mayorMesActual =
                calculoService.obtenerLibroMayor(gastoSueldos.getIdCuenta(), inicioMesActual, finMesActual);
        assertThat(mayorSeptiembre).hasSize(1)
                .allSatisfy(m -> {
                    assertThat(m.getAsientoNumero()).isEqualTo(asiento.getNumero());
                    assertThat(m.getFecha()).isEqualTo(SEPT_FIN);
                });
        assertThat(mayorSeptiembre.get(0).getSaldo()).isEqualByComparingTo("1000000.00");
        assertThat(mayorMesActual)
                .noneMatch(m -> asiento.getNumero().equals(m.getAsientoNumero()));
        List<CalculoContableService.MovimientoCuenta> mayorCargas =
                calculoService.obtenerLibroMayor(cargasSociales.getIdCuenta(), SEPT_INICIO, SEPT_FIN);
        assertThat(mayorCargas).hasSize(1);
        assertThat(mayorCargas.get(0).getSaldo()).isEqualByComparingTo("268300.00");
        assertThat(mayorCargas.get(0).getFecha()).isEqualTo(SEPT_FIN);
    }

    private Nomina nominaSeptiembre() {
        Nomina nomina = Nomina.builder()
                .numero("NOM-2026-0009")
                .periodoInicio(SEPT_INICIO)
                .periodoFin(SEPT_FIN)
                .fechaPago(SEPT_FIN.plusDays(5))
                .tipo(TipoNomina.MENSUAL)
                .estado(EstadoNomina.APROBADA)
                .totalBruto(new BigDecimal("1000000.00"))
                .totalCcssPatronal(new BigDecimal("268300.00"))
                .totalNeto(new BigDecimal("1000000.00"))
                .build();
        return em.persist(nomina);
    }

    private CuentaContable cuenta(String codigo, TipoCuenta tipo) {
        CuentaContable c = new CuentaContable();
        c.setCodigo(codigo);
        c.setNombre("Cuenta " + codigo);
        c.setTipo(tipo);
        c.setNivel(4);
        c.setActiva(true);
        c.setAceptaMovimientos(true);
        return em.persist(c);
    }

    private void param(String clave, CuentaContable cuenta) {
        em.persist(ParametroContable.builder().clave(clave).cuentaContable(cuenta).build());
    }
}
