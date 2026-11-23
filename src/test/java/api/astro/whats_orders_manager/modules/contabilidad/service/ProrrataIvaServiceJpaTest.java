package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.config.ClockConfig;
import api.astro.whats_orders_manager.modules.contabilidad.dto.ProrrataIvaDTO;
import api.astro.whats_orders_manager.modules.contabilidad.model.ProrrataIva;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ProrrataIvaRepository;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioActividadService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * T-054 (ADR-067 §4, criterio 7): alta y corrección del porcentaje provisional de prorrata por año, con validación
 * (0 a 100, 2 decimales, un registro por año) y auditoría del valor anterior y el nuevo.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import({ProrrataIvaService.class, ClockConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("T-054 — prorrata provisional del IVA por año (H2)")
class ProrrataIvaServiceJpaTest {

    @Autowired private ProrrataIvaService service;
    @Autowired private ProrrataIvaRepository repository;
    @MockitoBean private UsuarioActividadService actividadService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = mock(Usuario.class);
        when(usuario.getIdUsuario()).thenReturn(7);
    }

    @AfterEach
    void limpiar() {
        repository.deleteAllInBatch();
    }

    @Test
    @DisplayName("crear guarda año, porcentaje, nota, usuario y fechas, y deja auditoría")
    void crear_ok() {
        ProrrataIva p = service.crear(new ProrrataIvaDTO(2026, new BigDecimal("85.5"), "  Provisional del año anterior  "), usuario);

        ProrrataIva g = repository.findByAnio(2026).orElseThrow();
        assertThat(g.getId()).isEqualTo(p.getId());
        assertThat(g.getPorcentaje()).isEqualByComparingTo("85.50");
        assertThat(g.getNota()).isEqualTo("Provisional del año anterior");
        assertThat(g.getUsuarioId()).isEqualTo(7);
        assertThat(g.getFechaRegistro()).isNotNull();
        assertThat(g.getFechaActualizacion()).isNotNull();
        verify(actividadService).registrarActividad(eq(7), eq("PRORRATA_IVA_CREADA"), contains("2026"),
                eq("ProrrataIva"), eq(p.getId().intValue()));
    }

    @Test
    @DisplayName("los límites 0 y 100 son válidos")
    void crear_limites() {
        service.crear(new ProrrataIvaDTO(2026, BigDecimal.ZERO, null), usuario);
        service.crear(new ProrrataIvaDTO(2027, new BigDecimal("100.00"), null), usuario);
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("porcentaje fuera de 0 a 100, nulo o con más de 2 decimales → 400 con mensaje fijo y nada guardado")
    void crear_porcentajeInvalido() {
        for (String v : new String[] {"-0.01", "100.01", "150"}) {
            assertThatThrownBy(() -> service.crear(new ProrrataIvaDTO(2026, new BigDecimal(v), null), usuario))
                    .isInstanceOf(IllegalArgumentException.class).hasMessage(ProrrataIvaService.MSG_PORCENTAJE);
        }
        assertThatThrownBy(() -> service.crear(new ProrrataIvaDTO(2026, null, null), usuario))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(ProrrataIvaService.MSG_PORCENTAJE);
        assertThatThrownBy(() -> service.crear(new ProrrataIvaDTO(2026, new BigDecimal("50.123"), null), usuario))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(ProrrataIvaService.MSG_PORCENTAJE_DECIMALES);
        assertThat(repository.count()).isZero();
        verifyNoInteractions(actividadService);
    }

    @Test
    @DisplayName("año nulo o fuera de 2000 a 2100 y nota de más de 300 caracteres → 400")
    void crear_anioYNota() {
        for (Integer anio : new Integer[] {null, 1999, 2101}) {
            assertThatThrownBy(() -> service.crear(new ProrrataIvaDTO(anio, new BigDecimal("50"), null), usuario))
                    .isInstanceOf(IllegalArgumentException.class).hasMessage(ProrrataIvaService.MSG_ANIO);
        }
        assertThatThrownBy(() -> service.crear(new ProrrataIvaDTO(2026, new BigDecimal("50"), "n".repeat(301)), usuario))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(ProrrataIvaService.MSG_NOTA);
    }

    @Test
    @DisplayName("un segundo alta del mismo año → 409; hay que corregir el existente")
    void crear_duplicadoDelAnio() {
        service.crear(new ProrrataIvaDTO(2026, new BigDecimal("80"), null), usuario);
        assertThatThrownBy(() -> service.crear(new ProrrataIvaDTO(2026, new BigDecimal("70"), null), usuario))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Ya existe un porcentaje de prorrata para 2026; corríjalo en lugar de crear otro");
        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findByAnio(2026).orElseThrow().getPorcentaje()).isEqualByComparingTo("80");
    }

    @Test
    @DisplayName("corregir cambia el porcentaje y la nota, conserva la fecha de registro y audita el valor anterior y el nuevo")
    void corregir_ok() {
        ProrrataIva creada = service.crear(new ProrrataIvaDTO(2026, new BigDecimal("80"), "inicial"), usuario);
        Usuario otro = mock(Usuario.class);
        when(otro.getIdUsuario()).thenReturn(9);

        service.corregir(2026, new ProrrataIvaDTO(null, new BigDecimal("72.25"), "corregida"), otro);

        ProrrataIva g = repository.findByAnio(2026).orElseThrow();
        assertThat(g.getId()).isEqualTo(creada.getId());
        assertThat(g.getPorcentaje()).isEqualByComparingTo("72.25");
        assertThat(g.getNota()).isEqualTo("corregida");
        assertThat(g.getUsuarioId()).isEqualTo(9);
        assertThat(g.getFechaRegistro()).isEqualTo(repository.findById(creada.getId()).orElseThrow().getFechaRegistro());
        verify(actividadService).registrarActividad(eq(9), eq("PRORRATA_IVA_CORREGIDA"),
                contains("2026"), eq("ProrrataIva"), eq(creada.getId().intValue()));
    }

    @Test
    @DisplayName("la descripción de la auditoría trae el valor anterior y el nuevo")
    void corregir_auditaAnteriorYNuevo() {
        service.crear(new ProrrataIvaDTO(2026, new BigDecimal("80"), null), usuario);
        service.corregir(2026, new ProrrataIvaDTO(2026, new BigDecimal("72.25"), null), usuario);

        verify(actividadService).registrarActividad(eq(7), eq("PRORRATA_IVA_CORREGIDA"),
                eq("Prorrata del IVA de 2026 corregida de 80.00 % a 72.25 %"), eq("ProrrataIva"),
                eq(repository.findByAnio(2026).orElseThrow().getId().intValue()));
    }

    @Test
    @DisplayName("corregir un año sin porcentaje → 404; con un porcentaje inválido → 400 y sin cambios")
    void corregir_errores() {
        assertThatThrownBy(() -> service.corregir(2030, new ProrrataIvaDTO(2030, new BigDecimal("50"), null), usuario))
                .isInstanceOf(NoSuchElementException.class).hasMessage("No hay porcentaje de prorrata configurado para 2030");
        service.crear(new ProrrataIvaDTO(2026, new BigDecimal("80"), null), usuario);
        assertThatThrownBy(() -> service.corregir(2026, new ProrrataIvaDTO(2026, new BigDecimal("101"), null), usuario))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(ProrrataIvaService.MSG_PORCENTAJE);
        assertThat(repository.findByAnio(2026).orElseThrow().getPorcentaje()).isEqualByComparingTo("80");
    }

    @Test
    @DisplayName("listar devuelve los años del más reciente al más antiguo; buscar por año vacío si no existe")
    void listarYBuscar() {
        service.crear(new ProrrataIvaDTO(2025, new BigDecimal("70"), null), usuario);
        service.crear(new ProrrataIvaDTO(2027, new BigDecimal("90"), null), usuario);
        service.crear(new ProrrataIvaDTO(2026, new BigDecimal("80"), null), usuario);

        assertThat(service.listar()).extracting(ProrrataIva::getAnio).containsExactly(2027, 2026, 2025);
        assertThat(service.buscar(2026)).isPresent();
        assertThat(service.buscar(2031)).isEmpty();
    }
}
