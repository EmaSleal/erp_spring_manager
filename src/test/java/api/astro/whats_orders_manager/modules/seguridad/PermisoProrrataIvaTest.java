package api.astro.whats_orders_manager.modules.seguridad;

import api.astro.whats_orders_manager.modules.seguridad.config.MatrizPermisos;
import api.astro.whats_orders_manager.modules.seguridad.enums.Permiso;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** T-054 (criterio 7): el permiso PRORRATA_IVA_GESTIONAR existe, es crítico, de la categoría Contabilidad y solo de ADMIN. */
@DisplayName("T-054 — permiso PRORRATA_IVA_GESTIONAR")
class PermisoProrrataIvaTest {

    @Test
    @DisplayName("existe, es crítico y su categoría es Contabilidad")
    void existeYEsCritico() {
        Permiso p = Permiso.valueOf("PRORRATA_IVA_GESTIONAR");
        assertThat(p.esCritico()).isTrue();
        assertThat(p.getCategoria()).isEqualTo("Contabilidad");
        assertThat(p.getNombre()).isNotBlank();
    }

    @Test
    @DisplayName("ADMIN lo tiene; GERENTE y VENDEDOR no")
    void soloAdmin() {
        assertThat(MatrizPermisos.getPermisos("ADMIN")).contains(Permiso.valueOf("PRORRATA_IVA_GESTIONAR"));
        assertThat(MatrizPermisos.getPermisos("GERENTE")).doesNotContain(Permiso.valueOf("PRORRATA_IVA_GESTIONAR"));
        assertThat(MatrizPermisos.getPermisos("VENDEDOR")).doesNotContain(Permiso.valueOf("PRORRATA_IVA_GESTIONAR"));
    }
}
