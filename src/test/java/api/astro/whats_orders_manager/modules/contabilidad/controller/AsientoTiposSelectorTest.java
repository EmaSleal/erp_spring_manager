package api.astro.whats_orders_manager.modules.contabilidad.controller;

import api.astro.whats_orders_manager.modules.contabilidad.dto.mapper.AsientoContableMapper;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.shared.service.MonedaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T-031 (P-037 sub-ítem 3) — el selector de tipo del formulario de asiento manual solo ofrece tipos no automáticos.
 */
@DisplayName("T-031 — selector de tipo de asiento manual")
class AsientoTiposSelectorTest {

    private final AsientoContableService service = mock(AsientoContableService.class);
    private final AsientoContableMapper mapper = mock(AsientoContableMapper.class);
    private final AsientoContableController controller =
            new AsientoContableController(service, mapper, mock(MonedaService.class));

    @Test
    @DisplayName("el formulario de creación no ofrece ningún tipo AUTOMATICO_*")
    @SuppressWarnings("unchecked")
    void crear_sinTiposAutomaticos() {
        ConcurrentModel model = new ConcurrentModel();

        controller.mostrarFormularioCrear(null, model);

        List<TipoAsiento> tipos = (List<TipoAsiento>) model.getAttribute("tiposAsiento");
        assertThat(tipos).isNotEmpty().noneMatch(TipoAsiento::esAutomatico);
        assertThat(tipos).contains(TipoAsiento.MANUAL, TipoAsiento.AJUSTE);
    }

    @Test
    @DisplayName("el formulario de edición tampoco ofrece tipos automáticos")
    @SuppressWarnings("unchecked")
    void editar_sinTiposAutomaticos() {
        AsientoContable asiento = mock(AsientoContable.class);
        when(asiento.puedeModificarse()).thenReturn(true);
        when(service.obtenerPorId(any())).thenReturn(Optional.of(asiento));
        ConcurrentModel model = new ConcurrentModel();

        controller.mostrarFormularioEditar(null, 1L, model);

        List<TipoAsiento> tipos = (List<TipoAsiento>) model.getAttribute("tiposAsiento");
        assertThat(tipos).isNotEmpty().noneMatch(TipoAsiento::esAutomatico);
    }
}
