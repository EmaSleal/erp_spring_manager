package api.astro.whats_orders_manager.modules.seguridad;

import api.astro.whats_orders_manager.modules.seguridad.config.MatrizPermisos;
import api.astro.whats_orders_manager.modules.seguridad.enums.Permiso;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** T-056 (criterio 7): REGIMEN_TOPE_COMPRAS_GESTIONAR existe, es crítico, de Configuración y solo de ADMIN. */
@DisplayName("T-056 — permiso REGIMEN_TOPE_COMPRAS_GESTIONAR")
class PermisoTopeComprasRtsTest {

    @Test
    @DisplayName("existe, es crítico y su categoría es Configuración")
    void existeYEsCritico() {
        Permiso p = Permiso.valueOf("REGIMEN_TOPE_COMPRAS_GESTIONAR");
        assertThat(p.esCritico()).isTrue();
        assertThat(p.getCategoria()).isEqualTo("Configuración");
        assertThat(p.getNombre()).isNotBlank();
    }

    @Test
    @DisplayName("ADMIN lo tiene; GERENTE y VENDEDOR no")
    void soloAdmin() {
        assertThat(MatrizPermisos.getPermisos("ADMIN")).contains(Permiso.valueOf("REGIMEN_TOPE_COMPRAS_GESTIONAR"));
        assertThat(MatrizPermisos.getPermisos("GERENTE")).doesNotContain(Permiso.valueOf("REGIMEN_TOPE_COMPRAS_GESTIONAR"));
        assertThat(MatrizPermisos.getPermisos("VENDEDOR")).doesNotContain(Permiso.valueOf("REGIMEN_TOPE_COMPRAS_GESTIONAR"));
    }
}
