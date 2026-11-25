package api.astro.whats_orders_manager.modules.regimen;

import api.astro.whats_orders_manager.modules.regimen.dto.RegimenTributarioDTO;
import api.astro.whats_orders_manager.modules.regimen.enums.Regimen;
import api.astro.whats_orders_manager.modules.regimen.model.RegimenTributario;
import api.astro.whats_orders_manager.modules.regimen.repository.RegimenTributarioRepository;
import api.astro.whats_orders_manager.modules.regimen.service.ConsultaDocumentosPorFecha;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenTributarioService;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T-056 (revisión 1, criterio 13): crear un tramo con inicio hoy o anterior se permite solo si no hay facturas, notas ni
 * CxP (anuladas incluidas) con fecha igual o posterior al inicio; si los hay, 409 con mensaje fijo que dice cuántos. Usa la
 * consulta real de documentos (H2). Hoy es 2026-11-25 (Clock fijo).
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import({RegimenTributarioService.class, ConsultaDocumentosPorFecha.class, RegimenCrearEnElPasadoJpaTest.RelojFijo.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("T-056 r1 — crear un tramo con inicio hoy o anterior (H2, consulta real de documentos)")
class RegimenCrearEnElPasadoJpaTest {

    static final LocalDate HOY = LocalDate.of(2026, 11, 25);
    static final LocalDate AYER = HOY.minusDays(1);

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
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private UsuarioActividadService actividadService;

    @BeforeEach
    void setUp() {
        jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
        limpiar();
        guardar(Regimen.SIMPLIFICADO, LocalDate.of(2000, 1, 1));
        guardar(Regimen.GENERAL, LocalDate.of(2026, 9, 1));
        service.invalidarCache();
    }

    @AfterEach
    void limpiar() {
        jdbc.execute("DELETE FROM nota_factura");
        jdbc.execute("DELETE FROM factura");
        jdbc.execute("DELETE FROM cuentas_por_pagar");
        repository.deleteAllInBatch();
        service.invalidarCache();
    }

    private void guardar(Regimen regimen, LocalDate desde) {
        repository.saveAndFlush(RegimenTributario.builder().regimen(regimen).vigenteDesde(desde).nota("semilla")
                .fechaRegistro(LocalDateTime.of(2026, 11, 1, 8, 0)).build());
    }

    private static RegimenTributarioDTO dto(Regimen regimen, LocalDate desde) {
        return new RegimenTributarioDTO(regimen, desde, "nota");
    }

    private void facturaDeHoy() {
        jdbc.update("INSERT INTO factura (id_cliente, anulada, monto_acreditado, acreditada_totalmente, create_date, fecha_contable) "
                + "VALUES (1, TRUE, 0, FALSE, TIMESTAMP '2026-11-25 09:00:00', NULL)");
    }

    private void cxp(String comprobante) {
        jdbc.update("INSERT INTO cuentas_por_pagar (numero, proveedor_id, descripcion, monto, saldo_pendiente, fecha_emision, "
                + "fecha_comprobante, estado_aceptacion, estado) VALUES ('CP1', 1, 'c', 1, 1, DATE '2020-01-01', DATE '"
                + comprobante + "', 'PENDIENTE', 'PENDIENTE')");
    }

    @Test
    @DisplayName("inicio ayer sin documentos: se crea y vigenteEn(ayer) devuelve el tramo nuevo")
    void crearAyerSinDocumentos() {
        RegimenTributario nuevo = service.crear(dto(Regimen.SIMPLIFICADO, AYER), null);

        assertThat(service.vigenteEn(AYER).getId()).isEqualTo(nuevo.getId());
        assertThat(service.vigenteEn(AYER.minusDays(1)).getRegimen()).isEqualTo(Regimen.GENERAL);
        assertThat(service.vigenteHoy().getId()).isEqualTo(nuevo.getId());
    }

    @Test
    @DisplayName("inicio hoy sin documentos: se crea")
    void crearHoySinDocumentos() {
        RegimenTributario nuevo = service.crear(dto(Regimen.SIMPLIFICADO, HOY), null);
        assertThat(service.vigenteEn(HOY).getId()).isEqualTo(nuevo.getId());
    }

    @Test
    @DisplayName("inicio ayer con una factura (anulada) de hoy: 409 con mensaje fijo que dice cuántos")
    void crearAyerConFacturaDeHoy() {
        facturaDeHoy();
        assertThatThrownBy(() -> service.crear(dto(Regimen.SIMPLIFICADO, AYER), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Ya existen documentos (facturas, notas de crédito o cuentas por pagar) con fecha igual o posterior al "
                        + "2026-11-24: 1 en total");
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("borde: una CxP con fecha igual al inicio da 409; con el día anterior se crea")
    void bordeCxp() {
        cxp("2026-11-24");
        assertThatThrownBy(() -> service.crear(dto(Regimen.SIMPLIFICADO, AYER), null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("2026-11-24: 1 en total");

        jdbc.execute("DELETE FROM cuentas_por_pagar");
        cxp("2026-11-23");
        assertThat(service.crear(dto(Regimen.SIMPLIFICADO, AYER), null).getId()).isNotNull();
    }

    @Test
    @DisplayName("se mantienen la fecha única y los tramos consecutivos")
    void siguenLasReglas() {
        assertThatThrownBy(() -> service.crear(dto(Regimen.SIMPLIFICADO, LocalDate.of(2026, 9, 1)), null))
                .isInstanceOf(IllegalStateException.class).hasMessage(RegimenTributarioService.MSG_FECHA_REPETIDA);
        assertThatThrownBy(() -> service.crear(dto(Regimen.GENERAL, AYER), null))
                .isInstanceOf(IllegalStateException.class).hasMessage(RegimenTributarioService.MSG_CONSECUTIVOS);
    }

    @Test
    @DisplayName("un tramo que ya empezó sigue sin editarse ni borrarse")
    void siguePasadoInmutable() {
        RegimenTributario nuevo = service.crear(dto(Regimen.SIMPLIFICADO, AYER), null);
        assertThatThrownBy(() -> service.corregir(nuevo.getId(), dto(Regimen.GENERAL, AYER), null))
                .isInstanceOf(IllegalStateException.class).hasMessage(RegimenTributarioService.MSG_PASADO_EDITAR);
        assertThatThrownBy(() -> service.eliminar(nuevo.getId(), null))
                .isInstanceOf(IllegalStateException.class).hasMessage(RegimenTributarioService.MSG_PASADO_ELIMINAR);
    }
}
