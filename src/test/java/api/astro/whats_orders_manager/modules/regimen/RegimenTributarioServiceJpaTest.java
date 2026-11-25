package api.astro.whats_orders_manager.modules.regimen;

import api.astro.whats_orders_manager.modules.regimen.dto.RegimenTributarioDTO;
import api.astro.whats_orders_manager.modules.regimen.enums.Regimen;
import api.astro.whats_orders_manager.modules.regimen.model.RegimenTributario;
import api.astro.whats_orders_manager.modules.regimen.repository.RegimenTributarioRepository;
import api.astro.whats_orders_manager.modules.regimen.service.ConsultaDocumentosPorFecha;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenNoDefinidoException;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenTributarioService;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenTributarioService.TramoEstado;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioActividadService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * T-056 (ADR-072, criterios 2 y 3): régimen vigente por fecha (mayor vigente_desde menor o igual), error sin régimen por
 * defecto, caché invalidada al escribir, y reglas de escritura (pasado inmutable, futuro solo sin documentos, sin dos
 * tramos seguidos con el mismo régimen, fechas únicas, auditoría). Hoy es 2026-11-25 (Clock fijo).
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import({RegimenTributarioService.class, RegimenTributarioServiceJpaTest.RelojFijo.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("T-056 — régimen tributario con vigencia: servicio por fecha y reglas de escritura (H2)")
class RegimenTributarioServiceJpaTest {

    static final LocalDate HOY = LocalDate.of(2026, 11, 25);
    static final LocalDate INICIO_SIMPLIFICADO = LocalDate.of(2000, 1, 1);
    static final LocalDate INICIO_GENERAL = LocalDate.of(2026, 9, 1);
    static final LocalDate FUTURO = LocalDate.of(2027, 1, 1);

    @TestConfiguration
    static class RelojFijo {
        @Bean
        @Primary
        Clock clock() {
            return Clock.fixed(HOY.atTime(10, 0).atZone(ZoneId.of("America/Costa_Rica")).toInstant(), ZoneId.of("America/Costa_Rica"));
        }
    }

    @Autowired private RegimenTributarioService service;
    @Autowired private RegimenTributarioRepository repository;
    @MockitoBean private UsuarioActividadService actividadService;
    @MockitoBean private ConsultaDocumentosPorFecha documentos;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = mock(Usuario.class);
        when(usuario.getIdUsuario()).thenReturn(7);
        reset(actividadService, documentos);
        when(documentos.existenDesde(any())).thenReturn(false);
        repository.deleteAllInBatch();
        sembrarComoProduccion();
    }

    @AfterEach
    void limpiar() {
        repository.deleteAllInBatch();
        service.invalidarCache();
    }

    /** La semilla de V58: SIMPLIFICADO desde 2000-01-01 y GENERAL desde 2026-09-01. */
    private void sembrarComoProduccion() {
        guardar(Regimen.SIMPLIFICADO, INICIO_SIMPLIFICADO);
        guardar(Regimen.GENERAL, INICIO_GENERAL);
        service.invalidarCache();
    }

    private RegimenTributario guardar(Regimen regimen, LocalDate desde) {
        return repository.saveAndFlush(RegimenTributario.builder().regimen(regimen).vigenteDesde(desde)
                .nota("semilla").fechaRegistro(LocalDateTime.of(2026, 11, 1, 8, 0)).build());
    }

    private static RegimenTributarioDTO dto(Regimen regimen, LocalDate desde) {
        return new RegimenTributarioDTO(regimen, desde, "nota");
    }

    // ── Criterio 2: vigenteEn ────────────────────────────────────────────────

    @Test
    @DisplayName("un día antes y el mismo día de cada inicio: 2000-01-01 y 2026-09-01")
    void vigenteEn_bordes() {
        assertThat(service.vigenteEn(INICIO_SIMPLIFICADO).getRegimen()).isEqualTo(Regimen.SIMPLIFICADO);
        assertThat(service.vigenteEn(INICIO_SIMPLIFICADO.plusDays(1)).getRegimen()).isEqualTo(Regimen.SIMPLIFICADO);
        assertThat(service.vigenteEn(INICIO_GENERAL.minusDays(1)).getRegimen()).isEqualTo(Regimen.SIMPLIFICADO);
        assertThat(service.vigenteEn(INICIO_GENERAL).getRegimen()).isEqualTo(Regimen.GENERAL);
        assertThat(service.vigenteEn(INICIO_GENERAL.plusDays(1)).getRegimen()).isEqualTo(Regimen.GENERAL);
        assertThat(service.vigenteEn(HOY).getRegimen()).isEqualTo(Regimen.GENERAL);
    }

    @Test
    @DisplayName("una fecha anterior a todo tramo lanza un error explícito, sin régimen por defecto")
    void vigenteEn_anteriorATodoTramo() {
        assertThatThrownBy(() -> service.vigenteEn(INICIO_SIMPLIFICADO.minusDays(1)))
                .isInstanceOf(RegimenNoDefinidoException.class)
                .hasMessageContaining("1999-12-31");
        assertThatThrownBy(() -> service.vigenteEn(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("sin ningún tramo lanza el error explícito")
    void vigenteEn_sinTramos() {
        repository.deleteAllInBatch();
        service.invalidarCache();
        assertThatThrownBy(() -> service.vigenteEn(HOY)).isInstanceOf(RegimenNoDefinidoException.class);
    }

    @Test
    @DisplayName("un tramo futuro no afecta hoy y manda desde su inicio: 2026-12-31 es GENERAL, 2027-01-01 es SIMPLIFICADO")
    void vigenteEn_tramoFuturo() {
        service.crear(dto(Regimen.SIMPLIFICADO, FUTURO), usuario);

        assertThat(service.vigenteEn(HOY).getRegimen()).isEqualTo(Regimen.GENERAL);
        assertThat(service.vigenteEn(FUTURO.minusDays(1)).getRegimen()).isEqualTo(Regimen.GENERAL);
        assertThat(service.vigenteEn(FUTURO).getRegimen()).isEqualTo(Regimen.SIMPLIFICADO);
        assertThat(service.vigenteEn(FUTURO.plusYears(3)).getRegimen()).isEqualTo(Regimen.SIMPLIFICADO);
    }

    @Test
    @DisplayName("caché: la segunda consulta no relee la base, y crear, corregir y eliminar la invalidan")
    void cache() {
        assertThat(service.vigenteEn(FUTURO).getRegimen()).isEqualTo(Regimen.GENERAL);
        // una escritura que se salta el servicio no se ve: la consulta sale de la caché
        guardar(Regimen.SIMPLIFICADO, LocalDate.of(2026, 12, 1));
        assertThat(service.vigenteEn(FUTURO).getRegimen()).isEqualTo(Regimen.GENERAL);
        service.invalidarCache();
        assertThat(service.vigenteEn(FUTURO).getRegimen()).isEqualTo(Regimen.SIMPLIFICADO);

        RegimenTributario creado = service.crear(dto(Regimen.GENERAL, LocalDate.of(2027, 6, 1)), usuario);
        assertThat(service.vigenteEn(LocalDate.of(2027, 6, 1)).getRegimen()).isEqualTo(Regimen.GENERAL);

        service.corregir(creado.getId(), dto(Regimen.GENERAL, LocalDate.of(2027, 7, 1)), usuario);
        assertThat(service.vigenteEn(LocalDate.of(2027, 6, 1)).getRegimen()).isEqualTo(Regimen.SIMPLIFICADO);
        assertThat(service.vigenteEn(LocalDate.of(2027, 7, 1)).getRegimen()).isEqualTo(Regimen.GENERAL);

        service.eliminar(creado.getId(), usuario);
        assertThat(service.vigenteEn(LocalDate.of(2027, 7, 1)).getRegimen()).isEqualTo(Regimen.SIMPLIFICADO);
    }

    // ── Criterio 3: crear ────────────────────────────────────────────────────

    @Test
    @DisplayName("crear un tramo futuro guarda usuario y fecha de registro del Clock, y deja auditoría")
    void crear_ok() {
        RegimenTributario t = service.crear(dto(Regimen.SIMPLIFICADO, FUTURO), usuario);

        RegimenTributario g = repository.findById(t.getId()).orElseThrow();
        assertThat(g.getRegimen()).isEqualTo(Regimen.SIMPLIFICADO);
        assertThat(g.getVigenteDesde()).isEqualTo(FUTURO);
        assertThat(g.getNota()).isEqualTo("nota");
        assertThat(g.getUsuarioId()).isEqualTo(7);
        assertThat(g.getFechaRegistro()).isEqualTo(LocalDateTime.of(2026, 11, 25, 10, 0));
        verify(actividadService).registrarActividad(eq(7), eq("REGIMEN_TRIBUTARIO_CREADO"), contains("2027-01-01"),
                eq("RegimenTributario"), eq(t.getId().intValue()));
    }

    @Test
    @DisplayName("crear con datos inválidos: 400 (IllegalArgumentException) con mensaje fijo")
    void crear_invalido() {
        assertThatThrownBy(() -> service.crear(dto(null, FUTURO), usuario))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(RegimenTributarioService.MSG_REGIMEN);
        assertThatThrownBy(() -> service.crear(dto(Regimen.SIMPLIFICADO, null), usuario))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(RegimenTributarioService.MSG_FECHA);
        assertThatThrownBy(() -> service.crear(new RegimenTributarioDTO(Regimen.SIMPLIFICADO, FUTURO, "x".repeat(301)), usuario))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(RegimenTributarioService.MSG_NOTA);
        assertThat(repository.count()).isEqualTo(2);
        verifyNoInteractions(actividadService);
    }

    @Test
    @DisplayName("crear con inicio hoy o anterior: 409, la historia no se toca")
    void crear_enElPasado() {
        for (LocalDate fecha : List.of(HOY, HOY.minusDays(1), LocalDate.of(2020, 1, 1))) {
            assertThatThrownBy(() -> service.crear(dto(Regimen.SIMPLIFICADO, fecha), usuario))
                    .isInstanceOf(IllegalStateException.class).hasMessage(RegimenTributarioService.MSG_PASADO_CREAR);
        }
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("crear con una fecha que ya tiene tramo: 409")
    void crear_fechaRepetida() {
        service.crear(dto(Regimen.SIMPLIFICADO, FUTURO), usuario);
        assertThatThrownBy(() -> service.crear(dto(Regimen.GENERAL, FUTURO), usuario))
                .isInstanceOf(IllegalStateException.class).hasMessage(RegimenTributarioService.MSG_FECHA_REPETIDA);
    }

    @Test
    @DisplayName("crear con el mismo régimen que el tramo anterior: 409 (al final)")
    void crear_mismoRegimenQueElAnterior() {
        assertThatThrownBy(() -> service.crear(dto(Regimen.GENERAL, FUTURO), usuario))
                .isInstanceOf(IllegalStateException.class).hasMessage(RegimenTributarioService.MSG_CONSECUTIVOS);
    }

    @Test
    @DisplayName("crear en medio con el mismo régimen que el siguiente o que el anterior: 409")
    void crear_enMedioConMismoRegimen() {
        service.crear(dto(Regimen.SIMPLIFICADO, FUTURO), usuario); // GENERAL(sep) -> SIMPLIFICADO(2027)

        assertThatThrownBy(() -> service.crear(dto(Regimen.SIMPLIFICADO, LocalDate.of(2026, 12, 1)), usuario))
                .as("igual que el siguiente").isInstanceOf(IllegalStateException.class).hasMessage(RegimenTributarioService.MSG_CONSECUTIVOS);
        assertThatThrownBy(() -> service.crear(dto(Regimen.GENERAL, LocalDate.of(2026, 12, 1)), usuario))
                .as("igual que el anterior").isInstanceOf(IllegalStateException.class).hasMessage(RegimenTributarioService.MSG_CONSECUTIVOS);
        assertThat(repository.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("crear con documentos de fecha igual o posterior al inicio: 409")
    void crear_conDocumentos() {
        when(documentos.existenDesde(FUTURO)).thenReturn(true);
        assertThatThrownBy(() -> service.crear(dto(Regimen.SIMPLIFICADO, FUTURO), usuario))
                .isInstanceOf(IllegalStateException.class).hasMessage(RegimenTributarioService.MSG_DOCUMENTOS);
        assertThat(repository.count()).isEqualTo(2);
    }

    // ── Criterio 3: editar ───────────────────────────────────────────────────

    @Test
    @DisplayName("editar un tramo cuyo inicio es hoy o anterior: 409; no cambia nada")
    void corregir_pasadoOHoy() {
        RegimenTributario hoy = guardar(Regimen.SIMPLIFICADO, HOY);
        service.invalidarCache();
        for (RegimenTributario t : repository.findAll()) {
            assertThatThrownBy(() -> service.corregir(t.getId(), dto(t.getRegimen(), FUTURO), usuario))
                    .as("tramo " + t.getVigenteDesde()).isInstanceOf(IllegalStateException.class)
                    .hasMessage(RegimenTributarioService.MSG_PASADO_EDITAR);
        }
        assertThat(repository.findById(hoy.getId()).orElseThrow().getVigenteDesde()).isEqualTo(HOY);
        verifyNoInteractions(actividadService);
    }

    @Test
    @DisplayName("editar un tramo futuro sin documentos: cambia régimen, fecha y nota, conserva fecha de registro y audita")
    void corregir_futuro_ok() {
        RegimenTributario futuro = service.crear(dto(Regimen.SIMPLIFICADO, FUTURO), usuario);
        reset(actividadService);

        RegimenTributario c = service.corregir(futuro.getId(),
                new RegimenTributarioDTO(Regimen.SIMPLIFICADO, LocalDate.of(2027, 2, 1), "otra nota"), usuario);

        RegimenTributario g = repository.findById(c.getId()).orElseThrow();
        assertThat(g.getVigenteDesde()).isEqualTo(LocalDate.of(2027, 2, 1));
        assertThat(g.getNota()).isEqualTo("otra nota");
        assertThat(g.getFechaRegistro()).isEqualTo(LocalDateTime.of(2026, 11, 25, 10, 0));
        verify(actividadService).registrarActividad(eq(7), eq("REGIMEN_TRIBUTARIO_CORREGIDO"), contains("2027-02-01"),
                eq("RegimenTributario"), eq(c.getId().intValue()));
    }

    @Test
    @DisplayName("editar un tramo futuro con documentos desde su inicio (o desde la nueva fecha): 409")
    void corregir_futuro_conDocumentos() {
        RegimenTributario futuro = service.crear(dto(Regimen.SIMPLIFICADO, FUTURO), usuario);
        when(documentos.existenDesde(FUTURO)).thenReturn(true);
        assertThatThrownBy(() -> service.corregir(futuro.getId(), dto(Regimen.SIMPLIFICADO, FUTURO), usuario))
                .isInstanceOf(IllegalStateException.class).hasMessage(RegimenTributarioService.MSG_DOCUMENTOS);

        // mover el inicio hacia atrás hacia fechas con documentos también se rechaza
        reset(documentos);
        when(documentos.existenDesde(LocalDate.of(2026, 12, 1))).thenReturn(true);
        assertThatThrownBy(() -> service.corregir(futuro.getId(), dto(Regimen.SIMPLIFICADO, LocalDate.of(2026, 12, 1)), usuario))
                .isInstanceOf(IllegalStateException.class).hasMessage(RegimenTributarioService.MSG_DOCUMENTOS);
        assertThat(repository.findById(futuro.getId()).orElseThrow().getVigenteDesde()).isEqualTo(FUTURO);
    }

    @Test
    @DisplayName("editar a una fecha ya usada, al pasado, con régimen igual al vecino, o un id inexistente")
    void corregir_reglas() {
        RegimenTributario a = service.crear(dto(Regimen.SIMPLIFICADO, FUTURO), usuario);
        RegimenTributario b = service.crear(dto(Regimen.GENERAL, LocalDate.of(2028, 1, 1)), usuario);

        assertThatThrownBy(() -> service.corregir(b.getId(), dto(Regimen.GENERAL, FUTURO), usuario))
                .as("fecha repetida").isInstanceOf(IllegalStateException.class).hasMessage(RegimenTributarioService.MSG_FECHA_REPETIDA);
        assertThatThrownBy(() -> service.corregir(b.getId(), dto(Regimen.GENERAL, HOY), usuario))
                .as("al pasado").isInstanceOf(IllegalStateException.class).hasMessage(RegimenTributarioService.MSG_PASADO_CREAR);
        assertThatThrownBy(() -> service.corregir(b.getId(), dto(Regimen.SIMPLIFICADO, LocalDate.of(2028, 1, 1)), usuario))
                .as("igual al anterior").isInstanceOf(IllegalStateException.class).hasMessage(RegimenTributarioService.MSG_CONSECUTIVOS);
        assertThatThrownBy(() -> service.corregir(a.getId(), dto(Regimen.GENERAL, FUTURO), usuario))
                .as("el anterior (GENERAL) y el siguiente (GENERAL) quedan junto a él")
                .isInstanceOf(IllegalStateException.class).hasMessage(RegimenTributarioService.MSG_CONSECUTIVOS);
        assertThatThrownBy(() -> service.corregir(9999L, dto(Regimen.GENERAL, FUTURO), usuario))
                .isInstanceOf(NoSuchElementException.class).hasMessage(RegimenTributarioService.MSG_NO_EXISTE);
    }

    // ── Criterio 3: eliminar ─────────────────────────────────────────────────

    @Test
    @DisplayName("eliminar un tramo futuro sin documentos: lo borra y audita")
    void eliminar_futuro_ok() {
        RegimenTributario futuro = service.crear(dto(Regimen.SIMPLIFICADO, FUTURO), usuario);
        reset(actividadService);

        service.eliminar(futuro.getId(), usuario);

        assertThat(repository.findById(futuro.getId())).isEmpty();
        verify(actividadService).registrarActividad(eq(7), eq("REGIMEN_TRIBUTARIO_ELIMINADO"), contains("2027-01-01"),
                eq("RegimenTributario"), eq(futuro.getId().intValue()));
    }

    @Test
    @DisplayName("eliminar un tramo con inicio hoy o anterior: 409; el seed y uno que empieza hoy quedan")
    void eliminar_pasadoOHoy() {
        RegimenTributario hoy = guardar(Regimen.SIMPLIFICADO, HOY);
        for (RegimenTributario t : repository.findAll()) {
            assertThatThrownBy(() -> service.eliminar(t.getId(), usuario))
                    .as("tramo " + t.getVigenteDesde()).isInstanceOf(IllegalStateException.class)
                    .hasMessage(RegimenTributarioService.MSG_PASADO_ELIMINAR);
        }
        assertThat(repository.count()).isEqualTo(3);
        assertThat(repository.findById(hoy.getId())).isPresent();
    }

    @Test
    @DisplayName("eliminar un tramo futuro con documentos desde su inicio: 409")
    void eliminar_conDocumentos() {
        RegimenTributario futuro = service.crear(dto(Regimen.SIMPLIFICADO, FUTURO), usuario);
        when(documentos.existenDesde(FUTURO)).thenReturn(true);
        assertThatThrownBy(() -> service.eliminar(futuro.getId(), usuario))
                .isInstanceOf(IllegalStateException.class).hasMessage(RegimenTributarioService.MSG_DOCUMENTOS);
        assertThat(repository.findById(futuro.getId())).isPresent();
    }

    @Test
    @DisplayName("eliminar en medio que dejaría dos tramos seguidos con el mismo régimen: 409; un id inexistente: 404")
    void eliminar_dejaConsecutivosIguales() {
        RegimenTributario medio = service.crear(dto(Regimen.SIMPLIFICADO, FUTURO), usuario);
        service.crear(dto(Regimen.GENERAL, LocalDate.of(2028, 1, 1)), usuario); // GENERAL, SIMPLIFICADO, GENERAL

        assertThatThrownBy(() -> service.eliminar(medio.getId(), usuario))
                .isInstanceOf(IllegalStateException.class).hasMessage(RegimenTributarioService.MSG_CONSECUTIVOS);
        assertThat(repository.count()).isEqualTo(4);
        assertThatThrownBy(() -> service.eliminar(9999L, usuario))
                .isInstanceOf(NoSuchElementException.class).hasMessage(RegimenTributarioService.MSG_NO_EXISTE);
    }

    // ── Historial (criterio 4: vigente hoy, próximo y editable) ──────────────

    @Test
    @DisplayName("historial: marca el vigente hoy y el próximo, y solo los futuros sin documentos son editables")
    void historial() {
        service.crear(dto(Regimen.SIMPLIFICADO, FUTURO), usuario);
        service.crear(dto(Regimen.GENERAL, LocalDate.of(2028, 1, 1)), usuario);
        when(documentos.existenDesde(LocalDate.of(2028, 1, 1))).thenReturn(true);

        List<TramoEstado> h = service.historial();

        assertThat(h).extracting(e -> e.tramo().getVigenteDesde())
                .containsExactly(INICIO_SIMPLIFICADO, INICIO_GENERAL, FUTURO, LocalDate.of(2028, 1, 1));
        assertThat(h).extracting(TramoEstado::vigente).containsExactly(false, true, false, false);
        assertThat(h).extracting(TramoEstado::proximo).containsExactly(false, false, true, false);
        assertThat(h).extracting(TramoEstado::editable).containsExactly(false, false, true, false);
        assertThat(h.get(0).bloqueo()).isEqualTo(RegimenTributarioService.MSG_PASADO_EDITAR);
        assertThat(h.get(2).bloqueo()).isNull();
        assertThat(h.get(3).bloqueo()).isEqualTo(RegimenTributarioService.MSG_DOCUMENTOS);
    }
}
