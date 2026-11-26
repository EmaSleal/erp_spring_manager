package api.astro.whats_orders_manager.modules.politicas;

import api.astro.whats_orders_manager.modules.facturacion.exception.FacturaRegimenConflictoException;
import api.astro.whats_orders_manager.modules.regimen.enums.Regimen;
import api.astro.whats_orders_manager.modules.regimen.model.RegimenTributario;
import api.astro.whats_orders_manager.modules.regimen.repository.RegimenTributarioRepository;
import api.astro.whats_orders_manager.modules.regimen.service.ConsultaDocumentosPorFecha;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenNoDefinidoException;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenTributarioService;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioActividadService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T-057 (ADR-073 Opción A, criterios 3 y 9; P-132 casos 1, 2, 12 y 13): el régimen de una factura se sella al crearla con el
 * régimen vigente en su fecha; una edición no lo recalcula y mover la fecha a un tramo de otro régimen es 409.
 */
@DisplayName("T-057 — PoliticaRegimenDocumento: sello del régimen al crear y frontera al editar")
class PoliticaRegimenDocumentoTest {

    private static final ZoneId CR = ZoneId.of("America/Costa_Rica");

    private static RegimenTributario tramo(Regimen regimen, String desde) {
        return RegimenTributario.builder().id((long) desde.hashCode()).regimen(regimen).vigenteDesde(LocalDate.parse(desde))
                .fechaRegistro(LocalDateTime.of(2026, 1, 1, 0, 0)).build();
    }

    private static PoliticaRegimenDocumento politica(RegimenTributario... tramos) {
        RegimenTributarioRepository repo = mock(RegimenTributarioRepository.class);
        when(repo.findAllByOrderByVigenteDesdeAsc()).thenReturn(new ArrayList<>(List.of(tramos)));
        Clock clock = Clock.fixed(LocalDate.of(2026, 11, 25).atTime(9, 0).atZone(CR).toInstant(), CR);
        return new PoliticaRegimenDocumento(new RegimenTributarioService(repo, mock(ConsultaDocumentosPorFecha.class),
                mock(UsuarioActividadService.class), clock));
    }

    @Test
    @DisplayName("casos 1 y 2: 2026-12-31 → GENERAL; 2027-01-01 → SIMPLIFICADO")
    void sellaSegunLaFechaDeLaFactura() {
        PoliticaRegimenDocumento p = politica(tramo(Regimen.GENERAL, "2000-01-01"), tramo(Regimen.SIMPLIFICADO, "2027-01-01"));

        assertThat(p.sellar(LocalDate.of(2026, 12, 31))).isEqualTo(Regimen.GENERAL);
        assertThat(p.sellar(LocalDate.of(2027, 1, 1))).isEqualTo(Regimen.SIMPLIFICADO);
    }

    @Test
    @DisplayName("caso 12: fecha anterior al primer tramo → RegimenNoDefinidoException, nunca un régimen supuesto")
    void antesDelPrimerTramo_error() {
        PoliticaRegimenDocumento p = politica(tramo(Regimen.GENERAL, "2026-09-01"));

        assertThatThrownBy(() -> p.sellar(LocalDate.of(2026, 8, 31))).isInstanceOf(RegimenNoDefinidoException.class);
    }

    @Test
    @DisplayName("sin ningún tramo → RegimenNoDefinidoException")
    void sinTramos_error() {
        assertThatThrownBy(() -> politica().sellar(LocalDate.of(2026, 12, 31))).isInstanceOf(RegimenNoDefinidoException.class);
    }

    @Test
    @DisplayName("caso 13: tramo GENERAL desde 2027-07-01 → la del 30-06 SIMPLIFICADO, la del 01-07 GENERAL")
    void salidaDelRts() {
        PoliticaRegimenDocumento p = politica(tramo(Regimen.GENERAL, "2000-01-01"), tramo(Regimen.SIMPLIFICADO, "2027-01-01"),
                tramo(Regimen.GENERAL, "2027-07-01"));

        assertThat(p.sellar(LocalDate.of(2027, 6, 30))).isEqualTo(Regimen.SIMPLIFICADO);
        assertThat(p.sellar(LocalDate.of(2027, 7, 1))).isEqualTo(Regimen.GENERAL);
    }

    @Test
    @DisplayName("mover la fecha a un tramo de otro régimen → FacturaRegimenConflictoException (409); dentro del mismo tramo, no")
    void moverFechaEntreTramos() {
        PoliticaRegimenDocumento p = politica(tramo(Regimen.GENERAL, "2000-01-01"), tramo(Regimen.SIMPLIFICADO, "2027-01-01"));

        assertThatThrownBy(() -> p.exigirMismoRegimen(Regimen.GENERAL, LocalDate.of(2027, 1, 5)))
                .isInstanceOf(FacturaRegimenConflictoException.class)
                .hasMessage(PoliticaRegimenDocumento.MENSAJE_CAMBIO_DE_REGIMEN);
        assertThatThrownBy(() -> p.exigirMismoRegimen(Regimen.SIMPLIFICADO, LocalDate.of(2026, 12, 30)))
                .isInstanceOf(FacturaRegimenConflictoException.class);
        assertThatCode(() -> p.exigirMismoRegimen(Regimen.GENERAL, LocalDate.of(2026, 12, 1))).doesNotThrowAnyException();
    }
}
