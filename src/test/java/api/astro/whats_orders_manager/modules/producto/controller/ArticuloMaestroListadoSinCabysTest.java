package api.astro.whats_orders_manager.modules.producto.controller;

import api.astro.whats_orders_manager.modules.configuracion.service.PresentacionService;
import api.astro.whats_orders_manager.modules.producto.model.ArticuloMaestro;
import api.astro.whats_orders_manager.modules.producto.service.ArticuloMaestroService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

/** T-034 criterio 9 — /articulos y /articulos?sinCabys=true. */
@DisplayName("ArticuloMaestroController.listar — filtro Sin CAByS")
class ArticuloMaestroListadoSinCabysTest {

    private final ArticuloMaestroService servicio = mock(ArticuloMaestroService.class);
    private final ArticuloMaestroController controller =
            new ArticuloMaestroController(servicio, mock(PresentacionService.class));
    private final TestingAuthenticationToken auth = new TestingAuthenticationToken("u", "p");

    @Test
    @DisplayName("sinCabys=true lista solo activos sin código y expone el contador")
    void conFiltro() {
        List<ArticuloMaestro> sin = List.of(new ArticuloMaestro());
        when(servicio.findActivosSinCabys()).thenReturn(sin);
        when(servicio.countActivosSinCabys()).thenReturn(146L);
        when(servicio.getVarianteCounts()).thenReturn(Map.of());
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("modules/producto/articulos", controller.listar(true, model, auth));

        assertSame(sin, model.get("maestros"));
        assertEquals(146L, model.get("totalSinCabys"));
        assertEquals(true, model.get("sinCabys"));
        verify(servicio, never()).findAll();
    }

    @Test
    @DisplayName("sin filtro lista todos y mantiene el contador")
    void sinFiltro() {
        List<ArticuloMaestro> todos = List.of(new ArticuloMaestro(), new ArticuloMaestro());
        when(servicio.findAll()).thenReturn(todos);
        when(servicio.countActivosSinCabys()).thenReturn(5L);
        when(servicio.getVarianteCounts()).thenReturn(Map.of());
        ExtendedModelMap model = new ExtendedModelMap();

        controller.listar(false, model, auth);

        assertSame(todos, model.get("maestros"));
        assertEquals(5L, model.get("totalSinCabys"));
        assertEquals(false, model.get("sinCabys"));
        verify(servicio, never()).findActivosSinCabys();
    }
}
