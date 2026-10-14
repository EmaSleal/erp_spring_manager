package api.astro.whats_orders_manager.modules.rrhh.repository;

import api.astro.whats_orders_manager.modules.rrhh.model.ParametroNominaEmpresa;
import api.astro.whats_orders_manager.modules.rrhh.service.ParametroNominaEmpresaService;
import api.astro.whats_orders_manager.modules.rrhh.service.impl.ParametroNominaEmpresaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T-010 (ADR-013) — vigencia of parametros_nomina_empresa against a real JPA provider (H2):
 * 0.0533 valid until 2026-06-30 and 0.0400 from 2026-07-01 (open-ended).
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import(ParametroNominaEmpresaServiceImpl.class)
@DisplayName("ParametroNominaEmpresa — findVigenteByFecha by vigencia")
class ParametroNominaEmpresaVigenciaJpaTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ParametroNominaEmpresaService service;

    @BeforeEach
    void setUp() {
        em.persist(build("0.0533", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30)));
        em.persist(build("0.0400", LocalDate.of(2026, 7, 1), null));
        em.flush();
    }

    @Test
    @DisplayName("June payroll date uses 0.0533 (including the last day of vigencia)")
    void junio_usaTasa0533() {
        assertThat(service.findVigenteByFecha(LocalDate.of(2026, 6, 1)).getPorcentajeAportePatronalAse())
                .isEqualByComparingTo("0.0533");
        assertThat(service.findVigenteByFecha(LocalDate.of(2026, 6, 30)).getPorcentajeAportePatronalAse())
                .isEqualByComparingTo("0.0533");
    }

    @Test
    @DisplayName("July payroll date uses 0.0400 (open-ended vigencia)")
    void julio_usaTasa0400() {
        assertThat(service.findVigenteByFecha(LocalDate.of(2026, 7, 1)).getPorcentajeAportePatronalAse())
                .isEqualByComparingTo("0.0400");
        assertThat(service.findVigenteByFecha(LocalDate.of(2026, 7, 31)).getPorcentajeAportePatronalAse())
                .isEqualByComparingTo("0.0400");
        assertThat(service.findVigenteByFecha(LocalDate.of(2030, 1, 1)).getPorcentajeAportePatronalAse())
                .isEqualByComparingTo("0.0400");
    }

    @Test
    @DisplayName("Date before any vigencia throws NoSuchElementException")
    void antesDeVigencia_lanzaNoSuchElement() {
        assertThatThrownBy(() -> service.findVigenteByFecha(LocalDate.of(2025, 12, 31)))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("findAll returns the full history")
    void findAll_historial() {
        assertThat(service.findAll()).hasSize(2);
    }

    private ParametroNominaEmpresa build(String tasa, LocalDate desde, LocalDate hasta) {
        ParametroNominaEmpresa p = new ParametroNominaEmpresa();
        p.setPorcentajeAportePatronalAse(new BigDecimal(tasa));
        p.setVigenciaDesde(desde);
        p.setVigenciaHasta(hasta);
        return p;
    }
}
