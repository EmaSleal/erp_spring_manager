package api.astro.whats_orders_manager.modules.facturacion.service.impl;

import api.astro.whats_orders_manager.modules.configuracion.model.CantonCostaRica;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.model.ProvinciaCostaRica;
import api.astro.whats_orders_manager.modules.configuracion.repository.CantonCostaRicaRepository;
import api.astro.whats_orders_manager.modules.configuracion.repository.ProvinciaCostaRicaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** T-059 criterio 6: lugar de la venta = cantón y provincia del emisor, por nombre (no por código). */
@DisplayName("T-059 — lugar de la venta del PDF del RTS")
class LugarVentaResolverTest {

    private final ProvinciaCostaRicaRepository provincias = mock(ProvinciaCostaRicaRepository.class);
    private final CantonCostaRicaRepository cantones = mock(CantonCostaRicaRepository.class);
    private final LugarVentaResolver resolver = new LugarVentaResolver(provincias, cantones);

    private static ConfiguracionEmpresa emisor(String provincia, String canton) {
        ConfiguracionEmpresa c = new ConfiguracionEmpresa();
        c.setCodigoProvincia(provincia);
        c.setCanton(canton);
        return c;
    }

    @Test
    @DisplayName("cantón y provincia resueltos: «Escazú, San José»")
    void cantonYProvincia() {
        ProvinciaCostaRica p = new ProvinciaCostaRica();
        p.setCodigo("1");
        p.setNombre("San José");
        CantonCostaRica c = new CantonCostaRica();
        c.setNombre("Escazú");
        when(provincias.findByCodigo("1")).thenReturn(Optional.of(p));
        when(cantones.findByProvinciaCodigoAndCodigo("1", "02")).thenReturn(c);

        assertThat(resolver.lugarDe(emisor("1", "02"))).isEqualTo("Escazú, San José");
    }

    @Test
    @DisplayName("solo la provincia resuelve: se imprime la provincia")
    void soloProvincia() {
        ProvinciaCostaRica p = new ProvinciaCostaRica();
        p.setCodigo("1");
        p.setNombre("San José");
        when(provincias.findByCodigo("1")).thenReturn(Optional.of(p));

        assertThat(resolver.lugarDe(emisor("1", null))).isEqualTo("San José");
    }

    @Test
    @DisplayName("sin códigos o sin coincidencia: null (el PDF no imprime la línea)")
    void sinDatos() {
        assertThat(resolver.lugarDe(emisor(null, null))).isNull();
        assertThat(resolver.lugarDe(emisor("9", "99"))).isNull();
        assertThat(resolver.lugarDe(null)).isNull();
    }
}
