package api.astro.whats_orders_manager.modules.rrhh.service;

import api.astro.whats_orders_manager.modules.rrhh.model.ParametroNominaEmpresa;
import api.astro.whats_orders_manager.modules.rrhh.repository.ParametroNominaEmpresaRepository;
import api.astro.whats_orders_manager.modules.rrhh.service.impl.ParametroNominaEmpresaServiceImpl;
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
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ParametroNominaEmpresaServiceImpl (T-010, ADR-013).
 * Same contract as ParametroCCSSServiceTest: findVigenteByFecha delegates to the repository and
 * throws NoSuchElementException when no record covers the date; findAll delegates.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ParametroNominaEmpresaServiceImpl — unit tests")
class ParametroNominaEmpresaServiceTest {

    @Mock
    private ParametroNominaEmpresaRepository repository;

    private ParametroNominaEmpresaService service;

    @BeforeEach
    void setUp() {
        service = new ParametroNominaEmpresaServiceImpl(repository);
    }

    @Test
    @DisplayName("findVigenteByFecha() returns the record whose vigenciaDesde <= fecha <= vigenciaHasta")
    void findVigenteByFecha_dateInRange_returnsRecord() {
        LocalDate fecha = LocalDate.of(2026, 6, 15);
        when(repository.findVigenteByFecha(fecha))
                .thenReturn(Optional.of(build("0.0533", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30))));

        ParametroNominaEmpresa result = service.findVigenteByFecha(fecha);

        assertEquals(new BigDecimal("0.0533"), result.getPorcentajeAportePatronalAse());
        verify(repository).findVigenteByFecha(fecha);
    }

    @Test
    @DisplayName("findVigenteByFecha() returns the record when vigenciaHasta is null (indefinite)")
    void findVigenteByFecha_vigenciaHastaNull_returnsRecord() {
        LocalDate fecha = LocalDate.of(2030, 1, 1);
        when(repository.findVigenteByFecha(fecha))
                .thenReturn(Optional.of(build("0.0400", LocalDate.of(2026, 7, 1), null)));

        ParametroNominaEmpresa result = service.findVigenteByFecha(fecha);

        assertNull(result.getVigenciaHasta());
        assertEquals(new BigDecimal("0.0400"), result.getPorcentajeAportePatronalAse());
    }

    @Test
    @DisplayName("findVigenteByFecha() throws NoSuchElementException when no record is vigente (same as ParametroCCSS)")
    void findVigenteByFecha_noRecord_throws() {
        LocalDate fecha = LocalDate.of(2020, 1, 1);
        when(repository.findVigenteByFecha(fecha)).thenReturn(Optional.empty());

        NoSuchElementException ex = assertThrows(NoSuchElementException.class,
                () -> service.findVigenteByFecha(fecha));
        assertTrue(ex.getMessage().contains("2020-01-01"));
        verify(repository).findVigenteByFecha(fecha);
    }

    @Test
    @DisplayName("findAll() delegates to the repository")
    void findAll_delegatesToRepository() {
        when(repository.findAll()).thenReturn(List.of(
                build("0.0533", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30)),
                build("0.0400", LocalDate.of(2026, 7, 1), null)));

        assertEquals(2, service.findAll().size());
        verify(repository).findAll();
    }

    private ParametroNominaEmpresa build(String tasa, LocalDate desde, LocalDate hasta) {
        ParametroNominaEmpresa p = new ParametroNominaEmpresa();
        p.setId(1L);
        p.setPorcentajeAportePatronalAse(new BigDecimal(tasa));
        p.setVigenciaDesde(desde);
        p.setVigenciaHasta(hasta);
        p.setCreatedAt(LocalDateTime.now());
        return p;
    }
}
