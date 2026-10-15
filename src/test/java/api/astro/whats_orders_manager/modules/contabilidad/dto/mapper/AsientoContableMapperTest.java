package api.astro.whats_orders_manager.modules.contabilidad.dto.mapper;

import api.astro.whats_orders_manager.modules.contabilidad.dto.AsientoContableDTO;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.DetalleAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.repository.AsientoContableRepository;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.repository.PagoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * AsientoContableMapper:
 * - T-005 criterio 2: expone el id real de la factura.
 * - T-020 (ADR-023): puedeRevertirse, revertidoPor y reversionDe en el DTO del detalle y en el del
 *   listado (toDTOSimple), que además lleva puedeModificarse y puedeContabilizarse.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AsientoContableMapper")
class AsientoContableMapperTest {

    @Mock private DetalleAsientoMapper detalleMapper;
    @Mock private FacturaRepository facturaRepository;
    @Mock private PagoRepository pagoRepository;
    @Mock private AsientoContableRepository asientoRepository;

    private AsientoContableMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new AsientoContableMapper(detalleMapper, facturaRepository, pagoRepository, asientoRepository);
        lenient().when(asientoRepository.findFirstByAsientoRevertidoIdAsiento(anyLong())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("toDTO expone el id real de la factura (no null)")
    void toDTO_facturaIdReal() {
        Factura factura = new Factura();
        factura.setIdFactura(42);
        factura.setNumeroFactura("F-42");

        AsientoContable asiento = asiento(1L, "ASI-2026-0001", TipoAsiento.AUTOMATICO_VENTA, EstadoAsiento.CONTABILIZADO);
        asiento.setFactura(factura);

        AsientoContableDTO dto = mapper.toDTO(asiento);

        assertThat(dto.getFacturaId()).isEqualTo(42L);
        assertThat(dto.getFacturaNumero()).isEqualTo("F-42");
        assertThat(dto.getPuedeRevertirse()).isFalse();
    }

    @Test
    @DisplayName("T-020 MANUAL contabilizado sin revertir: puedeRevertirse en detalle y listado; sin enlaces")
    void manualContabilizado_puedeRevertirse() {
        AsientoContable asiento = asiento(1L, "ASI-2026-0001", TipoAsiento.MANUAL, EstadoAsiento.CONTABILIZADO);

        for (Function<AsientoContable, AsientoContableDTO> conversion : conversiones()) {
            AsientoContableDTO dto = conversion.apply(asiento);
            assertThat(dto.getPuedeRevertirse()).isTrue();
            assertThat(dto.getRevertidoPorId()).isNull();
            assertThat(dto.getReversionDeId()).isNull();
            assertThat(dto.getPuedeModificarse()).isFalse();
            assertThat(dto.getPuedeContabilizarse()).isFalse();
        }
    }

    @Test
    @DisplayName("T-020 asiento revertido: revertidoPor (id y número) y ya no puedeRevertirse")
    void revertido_exponeRevertidoPor() {
        AsientoContable original = asiento(1L, "ASI-2026-0001", TipoAsiento.MANUAL, EstadoAsiento.CONTABILIZADO);
        AsientoContable reversion = asiento(2L, "ASI-2026-0002", TipoAsiento.MANUAL, EstadoAsiento.CONTABILIZADO);
        reversion.setAsientoRevertido(original);
        when(asientoRepository.findFirstByAsientoRevertidoIdAsiento(1L)).thenReturn(Optional.of(reversion));

        for (Function<AsientoContable, AsientoContableDTO> conversion : conversiones()) {
            AsientoContableDTO dto = conversion.apply(original);
            assertThat(dto.getRevertidoPorId()).isEqualTo(2L);
            assertThat(dto.getRevertidoPorNumero()).isEqualTo("ASI-2026-0002");
            assertThat(dto.getReversionDeId()).isNull();
            assertThat(dto.getPuedeRevertirse()).isFalse();
        }
    }

    @Test
    @DisplayName("T-020 reversión: reversionDe (id y número) y no puedeRevertirse")
    void reversion_exponeReversionDe() {
        AsientoContable original = asiento(1L, "ASI-2026-0001", TipoAsiento.MANUAL, EstadoAsiento.CONTABILIZADO);
        AsientoContable reversion = asiento(2L, "ASI-2026-0002", TipoAsiento.MANUAL, EstadoAsiento.CONTABILIZADO);
        reversion.setAsientoRevertido(original);

        for (Function<AsientoContable, AsientoContableDTO> conversion : conversiones()) {
            AsientoContableDTO dto = conversion.apply(reversion);
            assertThat(dto.getReversionDeId()).isEqualTo(1L);
            assertThat(dto.getReversionDeNumero()).isEqualTo("ASI-2026-0001");
            assertThat(dto.getRevertidoPorId()).isNull();
            assertThat(dto.getPuedeRevertirse()).isFalse();
        }
    }

    @Test
    @DisplayName("T-020 borrador: el listado ofrece Editar y Contabilizar (cuadrado, 2 líneas), no Revertir")
    void borrador_listadoOfreceEditarYContabilizar() {
        AsientoContable borrador = asiento(3L, "ASI-2026-0003", TipoAsiento.MANUAL, EstadoAsiento.BORRADOR);
        borrador.agregarDetalle(linea("100.00", "0"));
        borrador.agregarDetalle(linea("0", "100.00"));

        AsientoContableDTO dto = mapper.toDTOSimple(borrador);

        assertThat(dto.getPuedeModificarse()).isTrue();
        assertThat(dto.getPuedeContabilizarse()).isTrue();
        assertThat(dto.getPuedeRevertirse()).isFalse();
    }

    @Test
    @DisplayName("T-020 AJUSTE contabilizado sin documento: puedeRevertirse; AUTOMATICO_VENTA sin documento: no")
    void ajusteSinDocumento_puedeRevertirse_automaticoNo() {
        assertThat(mapper.toDTOSimple(asiento(4L, "A4", TipoAsiento.AJUSTE, EstadoAsiento.CONTABILIZADO))
                .getPuedeRevertirse()).isTrue();
        assertThat(mapper.toDTOSimple(asiento(5L, "A5", TipoAsiento.AUTOMATICO_VENTA, EstadoAsiento.CONTABILIZADO))
                .getPuedeRevertirse()).isFalse();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private List<Function<AsientoContable, AsientoContableDTO>> conversiones() {
        return List.of(mapper::toDTO, mapper::toDTOSimple);
    }

    private static AsientoContable asiento(Long id, String numero, TipoAsiento tipo, EstadoAsiento estado) {
        AsientoContable asiento = new AsientoContable();
        asiento.setIdAsiento(id);
        asiento.setNumero(numero);
        asiento.setFecha(LocalDate.now());
        asiento.setConcepto("Asiento " + numero);
        asiento.setTipo(tipo);
        asiento.setEstado(estado);
        asiento.setFechaCreacion(LocalDateTime.now());
        asiento.setFechaModificacion(LocalDateTime.now());
        return asiento;
    }

    private static DetalleAsiento linea(String debe, String haber) {
        DetalleAsiento d = new DetalleAsiento();
        d.setDebe(new BigDecimal(debe));
        d.setHaber(new BigDecimal(haber));
        return d;
    }
}
