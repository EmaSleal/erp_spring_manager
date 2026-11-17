package api.astro.whats_orders_manager.modules.producto.repository;

import api.astro.whats_orders_manager.modules.producto.model.ArticuloMaestro;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-034 criterio 9 — el filtro y el contador «Sin CAByS» cuentan solo artículos activos con código nulo o en blanco.
 * SQL de control: SELECT COUNT(*) FROM articulo_maestro WHERE active = 1 AND (codigo_cabys IS NULL OR TRIM(codigo_cabys) = '')
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@DisplayName("ArticuloMaestroRepository — Sin CAByS")
class ArticuloMaestroSinCabysJpaTest {

    @Autowired private TestEntityManager em;
    @Autowired private ArticuloMaestroRepository repository;

    private void maestro(String descripcion, boolean activo, String cabys) {
        ArticuloMaestro m = new ArticuloMaestro();
        m.setDescripcion(descripcion);
        m.setActive(activo);
        m.setCodigoCabys(cabys);
        em.persist(m);
    }

    @Test
    @DisplayName("solo activos con código null o en blanco; inactivos y con código quedan fuera")
    void cuentaYListaSoloActivosSinCabys() {
        maestro("A nulo activo", true, null);
        maestro("B vacio activo", true, "");
        maestro("C blancos activo", true, "   ");
        maestro("D con codigo activo", true, "2132100000100");
        maestro("E nulo inactivo", false, null);
        maestro("F vacio inactivo", false, "");
        em.flush();

        assertThat(repository.countActivosSinCabys()).isEqualTo(3);
        assertThat(repository.findActivosSinCabys())
                .extracting(ArticuloMaestro::getDescripcion)
                .containsExactly("A nulo activo", "B vacio activo", "C blancos activo");
    }
}
