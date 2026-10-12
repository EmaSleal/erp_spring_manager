package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.enums.FuenteTipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.exception.TipoCambioNoDisponibleException;
import api.astro.whats_orders_manager.modules.facturacion.model.TipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.repository.TipoCambioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T-007 criterio 2 — TipoCambioService.obtenerTasaVenta contra un proveedor JPA real (H2):
 * toma el registro ACTIVO más reciente con fecha &lt;= la consultada e ignora inactivos y posteriores.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@DisplayName("TipoCambioService.obtenerTasaVenta — consulta real (H2)")
class TipoCambioServiceTasaVentaJpaTest {

    @Autowired
    private TipoCambioRepository repo;

    @Autowired
    private TestEntityManager em;

    private TipoCambioService service;

    @BeforeEach
    void setUp() {
        service = new TipoCambioService(repo);
        // 2026-10-01 activo 500 (anterior, vigente para el 10-11 si no hubiera otro)
        persistir(LocalDate.of(2026, 10, 1), "500.00000", true);
        // 2026-10-08 activo 505 → el más reciente <= 2026-10-11
        persistir(LocalDate.of(2026, 10, 8), "505.00000", true);
        // 2026-10-10 inactivo 999 → se ignora
        persistir(LocalDate.of(2026, 10, 10), "999.00000", false);
        // 2026-10-12 activo 520 → posterior, se ignora
        persistir(LocalDate.of(2026, 10, 12), "520.00000", true);
        em.flush();
    }

    @Test
    @DisplayName("devuelve la tasa del activo más reciente con fecha <= consultada, ignorando inactivos y posteriores")
    void devuelveActivoMasRecienteAnteriorOIgual() {
        assertThat(service.obtenerTasaVenta(MonedaFE.USD, LocalDate.of(2026, 10, 11))).isEqualByComparingTo("505");
    }

    @Test
    @DisplayName("una fecha exacta toma la tasa de ese día")
    void fechaExacta() {
        assertThat(service.obtenerTasaVenta(MonedaFE.USD, LocalDate.of(2026, 10, 12))).isEqualByComparingTo("520");
        assertThat(service.obtenerTasaVenta(MonedaFE.USD, LocalDate.of(2026, 10, 1))).isEqualByComparingTo("500");
    }

    @Test
    @DisplayName("sin tasa anterior o igual a la fecha (o en otra moneda) → excepción con moneda y fecha")
    void sinTasa_lanzaExcepcion() {
        assertThatThrownBy(() -> service.obtenerTasaVenta(MonedaFE.USD, LocalDate.of(2026, 9, 30)))
                .isInstanceOf(TipoCambioNoDisponibleException.class)
                .hasMessageContaining("USD").hasMessageContaining("2026-09-30");
        assertThatThrownBy(() -> service.obtenerTasaVenta(MonedaFE.EUR, LocalDate.of(2026, 10, 11)))
                .isInstanceOf(TipoCambioNoDisponibleException.class)
                .hasMessageContaining("EUR").hasMessageContaining("2026-10-11");
    }

    @Test
    @DisplayName("CRC devuelve 1")
    void crc() {
        assertThat(service.obtenerTasaVenta(MonedaFE.CRC, LocalDate.of(2026, 10, 11))).isEqualByComparingTo("1");
    }

    private void persistir(LocalDate fecha, String tasaVenta, boolean activo) {
        em.persist(TipoCambio.builder()
                .monedaOrigen(MonedaFE.CRC)
                .monedaDestino(MonedaFE.USD)
                .fecha(fecha)
                .tasaCompra(new BigDecimal(tasaVenta).subtract(BigDecimal.TEN))
                .tasaVenta(new BigDecimal(tasaVenta))
                .fuente(FuenteTipoCambio.MANUAL)
                .activo(activo)
                .build());
    }
}
