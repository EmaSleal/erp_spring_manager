package api.astro.whats_orders_manager.modules.facturacion.electronica.mapper;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ComprobanteElectronicoDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.AmbienteComprobanteGuard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** T-044 (criterio 8): el DTO lleva el ambiente y las banderas que calcula el servidor. */
@DisplayName("T-044 — ComprobanteElectronicoMapper: ambiente y banderas")
class ComprobanteElectronicoMapperAmbienteTest {

    private static ComprobanteElectronico comprobante(AmbienteHacienda ambiente) {
        return ComprobanteElectronico.builder().id(1L)
            .empresa(ConfiguracionEmpresa.builder().idConfiguracion(1).razonSocial("E").build())
            .tipoComprobante(TipoComprobanteElectronico.FACTURA_ELECTRONICA).estado(EstadoComprobante.NO_TRANSMITIDO)
            .ambiente(ambiente).build();
    }

    @Test
    @DisplayName("sin regla (pruebas unitarias o configuración ilegible): coincide y sin motivo")
    void sinRegla() {
        ComprobanteElectronicoDTO dto = new ComprobanteElectronicoMapper().toDTO(comprobante(AmbienteHacienda.PRODUCCION));
        assertThat(dto.getAmbiente()).isEqualTo("PRODUCCION");
        assertThat(dto.getAmbienteCoincide()).isTrue();
        assertThat(dto.getMotivoAmbiente()).isNull();
    }

    @Test
    @DisplayName("otro ambiente que el activo: no coincide, con el motivo fijo; puedeReenviar sigue siendo la regla por estado")
    void otroAmbiente() {
        AmbienteComprobanteGuard guard = mock(AmbienteComprobanteGuard.class);
        when(guard.ambienteActivo()).thenReturn(Optional.of(AmbienteHacienda.PRODUCCION));
        ComprobanteElectronicoMapper mapper = new ComprobanteElectronicoMapper();
        ReflectionTestUtils.setField(mapper, "ambienteGuard", guard);

        ComprobanteElectronicoDTO otro = mapper.toDTO(comprobante(AmbienteHacienda.SANDBOX));
        assertThat(otro.getAmbienteCoincide()).isFalse();
        assertThat(otro.getMotivoAmbiente())
            .isEqualTo("El comprobante pertenece al ambiente SANDBOX y la configuración activa es PRODUCCION; no se envía");
        assertThat(otro.getPuedeReenviar()).isTrue();

        ComprobanteElectronicoDTO igual = mapper.toDTO(comprobante(AmbienteHacienda.PRODUCCION));
        assertThat(igual.getAmbienteCoincide()).isTrue();
        assertThat(igual.getMotivoAmbiente()).isNull();
    }
}
