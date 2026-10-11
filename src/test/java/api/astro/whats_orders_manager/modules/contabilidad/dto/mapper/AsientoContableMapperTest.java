package api.astro.whats_orders_manager.modules.contabilidad.dto.mapper;

import api.astro.whats_orders_manager.modules.contabilidad.dto.AsientoContableDTO;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.repository.PagoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-005 criterio 2 — el mapper ya no usa Long.getLong y expone el id real de la factura.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AsientoContableMapper — facturaId real")
class AsientoContableMapperTest {

    @Mock private DetalleAsientoMapper detalleMapper;
    @Mock private FacturaRepository facturaRepository;
    @Mock private PagoRepository pagoRepository;

    @Test
    @DisplayName("toDTO expone el id real de la factura (no null)")
    void toDTO_facturaIdReal() {
        AsientoContableMapper mapper = new AsientoContableMapper(detalleMapper, facturaRepository, pagoRepository);

        Factura factura = new Factura();
        factura.setIdFactura(42);
        factura.setNumeroFactura("F-42");

        AsientoContable asiento = new AsientoContable();
        asiento.setNumero("ASI-2026-0001");
        asiento.setFecha(LocalDate.now());
        asiento.setConcepto("Venta");
        asiento.setTipo(TipoAsiento.AUTOMATICO_VENTA);
        asiento.setEstado(EstadoAsiento.CONTABILIZADO);
        asiento.setFechaCreacion(LocalDateTime.now());
        asiento.setFechaModificacion(LocalDateTime.now());
        asiento.setFactura(factura);

        AsientoContableDTO dto = mapper.toDTO(asiento);

        assertThat(dto.getFacturaId()).isEqualTo(42L);
        assertThat(dto.getFacturaNumero()).isEqualTo("F-42");
    }
}
