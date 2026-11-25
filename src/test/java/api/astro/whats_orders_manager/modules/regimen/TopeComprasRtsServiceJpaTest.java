package api.astro.whats_orders_manager.modules.regimen;

import api.astro.whats_orders_manager.modules.regimen.aviso.NotificadorAdministradores;
import api.astro.whats_orders_manager.modules.regimen.enums.Regimen;
import api.astro.whats_orders_manager.modules.regimen.model.RegimenTributario;
import api.astro.whats_orders_manager.modules.regimen.repository.RegimenTributarioRepository;
import api.astro.whats_orders_manager.modules.regimen.service.ConsultaDocumentosPorFecha;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenTributarioService;
import api.astro.whats_orders_manager.modules.regimen.tope.EstadoTope;
import api.astro.whats_orders_manager.modules.regimen.tope.NivelTope;
import api.astro.whats_orders_manager.modules.regimen.tope.TopeComprasRts;
import api.astro.whats_orders_manager.modules.regimen.tope.TopeComprasRtsDTO;
import api.astro.whats_orders_manager.modules.regimen.tope.TopeComprasRtsRepository;
import api.astro.whats_orders_manager.modules.regimen.tope.TopeComprasRtsService;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * T-056 (criterio 7): tope anual de compras del RTS contra una base real (H2). Parámetros por año (el multiplicador 186 y
 * los umbrales 80 y 100 vienen de los valores por defecto de la columna, no del código), acumulado en CRC de las CxP no
 * anuladas con fecha de comprobante en el año y en tramos SIMPLIFICADO, avisos sin bloquear y sin repetirse. Hoy es
 * 2026-11-25; SIMPLIFICADO desde 2000-01-01 y GENERAL desde 2026-09-01. Tope 2026 = 462 200 x 186 = 85 969 200,00.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import({TopeComprasRtsService.class, RegimenTributarioService.class, TopeComprasRtsServiceJpaTest.Config.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("T-056 — tope de compras del RTS: parámetros, acumulado y avisos (H2)")
class TopeComprasRtsServiceJpaTest {

    static final LocalDate HOY = LocalDate.of(2026, 11, 25);

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        RelojMutable reloj() {
            return new RelojMutable(HOY);
        }
    }

    @Autowired private TopeComprasRtsService servicio;
    @Autowired private TopeComprasRtsRepository repositorio;
    @Autowired private RegimenTributarioRepository tramos;
    @Autowired private RegimenTributarioService regimenService;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private NotificadorAdministradores notificador;
    @MockitoBean private UsuarioActividadService actividadService;
    @MockitoBean private ConsultaDocumentosPorFecha documentos;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        reset(notificador, actividadService);
        usuario = mock(Usuario.class);
        when(usuario.getIdUsuario()).thenReturn(7);
        jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
        limpiar();
        tramo(Regimen.SIMPLIFICADO, LocalDate.of(2000, 1, 1));
        tramo(Regimen.GENERAL, LocalDate.of(2026, 9, 1));
        regimenService.invalidarCache();
    }

    @AfterEach
    void limpiar() {
        jdbc.execute("DELETE FROM cuentas_por_pagar");
        repositorio.deleteAllInBatch();
        tramos.deleteAllInBatch();
        regimenService.invalidarCache();
    }

    private void tramo(Regimen regimen, LocalDate desde) {
        tramos.saveAndFlush(RegimenTributario.builder().regimen(regimen).vigenteDesde(desde).nota("n")
                .fechaRegistro(LocalDateTime.of(2026, 11, 1, 8, 0)).build());
    }

    private void parametros2026() {
        servicio.crear(new TopeComprasRtsDTO(2026, new BigDecimal("462200.00"), null, null, null, null), usuario);
    }

    private int secuencia;

    private void cxp(String monto, String moneda, String tc, String emision, String comprobante, String estado) {
        jdbc.update("INSERT INTO cuentas_por_pagar (numero, proveedor_id, descripcion, monto, saldo_pendiente, moneda, tipo_cambio, "
                + "fecha_emision, fecha_comprobante, estado_aceptacion, estado) VALUES (?, 1, 'c', ?, ?, ?, ?, DATE '" + emision
                + "', " + (comprobante == null ? "NULL" : "DATE '" + comprobante + "'") + ", 'PENDIENTE', ?)",
                "CP" + (++secuencia), new BigDecimal(monto), new BigDecimal(monto), moneda, tc == null ? null : new BigDecimal(tc), estado);
    }

    private void cxp(String monto, String comprobante) {
        cxp(monto, "CRC", null, comprobante, comprobante, "PENDIENTE");
    }

    // ── Parámetros por año ───────────────────────────────────────────────────

    @Test
    @DisplayName("crear: multiplicador 186, umbral de aviso 80 y de tope 100 salen de la columna; el tope es salario x multiplicador")
    void crearConValoresPorDefectoDeLaColumna() {
        TopeComprasRts t = servicio.crear(new TopeComprasRtsDTO(2026, new BigDecimal("462200.00"), null, null, null, " n "), usuario);
        assertThat(t.getMultiplicador()).isEqualByComparingTo("186");
        assertThat(t.getUmbralAviso()).isEqualByComparingTo("80");
        assertThat(t.getUmbralTope()).isEqualByComparingTo("100");
        assertThat(t.getNota()).isEqualTo("n");
        assertThat(servicio.estado(2026).tope()).isEqualByComparingTo("85969200.00");
        verify(actividadService).registrarActividad(eq(7), eq("TOPE_COMPRAS_RTS_CREADO"), contains("2026"), eq("TopeComprasRts"), eq(t.getId().intValue()));
    }

    @Test
    @DisplayName("crear con valores propios y validaciones: año, salario, multiplicador, umbrales, nota")
    void validaciones() {
        TopeComprasRts t = servicio.crear(new TopeComprasRtsDTO(2027, new BigDecimal("470000"), new BigDecimal("200"),
                new BigDecimal("70"), new BigDecimal("90"), null), usuario);
        assertThat(servicio.estado(2027).tope()).isEqualByComparingTo("94000000.00");
        assertThat(t.getUmbralAviso()).isEqualByComparingTo("70");

        assertThatThrownBy(() -> servicio.crear(new TopeComprasRtsDTO(1999, BigDecimal.TEN, null, null, null, null), usuario))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(TopeComprasRtsService.MSG_ANIO);
        assertThatThrownBy(() -> servicio.crear(new TopeComprasRtsDTO(2028, null, null, null, null, null), usuario))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(TopeComprasRtsService.MSG_SALARIO);
        assertThatThrownBy(() -> servicio.crear(new TopeComprasRtsDTO(2028, BigDecimal.ZERO, null, null, null, null), usuario))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(TopeComprasRtsService.MSG_SALARIO);
        assertThatThrownBy(() -> servicio.crear(new TopeComprasRtsDTO(2028, new BigDecimal("1.234"), null, null, null, null), usuario))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(TopeComprasRtsService.MSG_SALARIO);
        assertThatThrownBy(() -> servicio.crear(new TopeComprasRtsDTO(2028, BigDecimal.TEN, new BigDecimal("-1"), null, null, null), usuario))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(TopeComprasRtsService.MSG_MULTIPLICADOR);
        assertThatThrownBy(() -> servicio.crear(new TopeComprasRtsDTO(2028, BigDecimal.TEN, null, new BigDecimal("0"), null, null), usuario))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(TopeComprasRtsService.MSG_UMBRALES);
        assertThatThrownBy(() -> servicio.crear(new TopeComprasRtsDTO(2028, BigDecimal.TEN, null, new BigDecimal("90"), new BigDecimal("80"), null), usuario))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(TopeComprasRtsService.MSG_UMBRALES);
        assertThatThrownBy(() -> servicio.crear(new TopeComprasRtsDTO(2028, BigDecimal.TEN, null, null, new BigDecimal("101"), null), usuario))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(TopeComprasRtsService.MSG_UMBRALES);
        assertThatThrownBy(() -> servicio.crear(new TopeComprasRtsDTO(2028, BigDecimal.TEN, null, null, null, "x".repeat(301)), usuario))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(TopeComprasRtsService.MSG_NOTA);
        assertThat(repositorio.count()).as("ninguna inválida deja fila").isEqualTo(1);
    }

    @Test
    @DisplayName("un año repetido es 409; corregir uno que no existe es 404; corregir conserva lo no enviado y audita")
    void duplicadoYCorregir() {
        parametros2026();
        assertThatThrownBy(() -> servicio.crear(new TopeComprasRtsDTO(2026, BigDecimal.TEN, null, null, null, null), usuario))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("2026");
        assertThatThrownBy(() -> servicio.corregir(2030, new TopeComprasRtsDTO(null, BigDecimal.TEN, null, null, null, null), usuario))
                .isInstanceOf(NoSuchElementException.class);

        TopeComprasRts c = servicio.corregir(2026, new TopeComprasRtsDTO(null, new BigDecimal("500000.00"), null, new BigDecimal("75"), null, "ajuste"), usuario);
        assertThat(c.getSalarioBase()).isEqualByComparingTo("500000.00");
        assertThat(c.getMultiplicador()).isEqualByComparingTo("186");
        assertThat(c.getUmbralAviso()).isEqualByComparingTo("75");
        assertThat(c.getUmbralTope()).isEqualByComparingTo("100");
        assertThat(c.getNota()).isEqualTo("ajuste");
        verify(actividadService).registrarActividad(eq(7), eq("TOPE_COMPRAS_RTS_CORREGIDO"), contains("2026"), eq("TopeComprasRts"), eq(c.getId().intValue()));
    }

    // ── Acumulado y niveles ──────────────────────────────────────────────────

    @Test
    @DisplayName("bajo el 80 %: nivel BAJO, porcentaje y acumulado, y ningún aviso")
    void bajoElOchentaPorCiento() {
        parametros2026();
        cxp("60000000.00", "2026-03-01");
        EstadoTope e = servicio.evaluar(2026);
        assertThat(e.configurado()).isTrue();
        assertThat(e.acumulado()).isEqualByComparingTo("60000000.00");
        assertThat(e.porcentaje()).isEqualByComparingTo("69.79");
        assertThat(e.nivel()).isEqualTo(NivelTope.BAJO);
        verifyNoInteractions(notificador);
    }

    @Test
    @DisplayName("cruzar el 80 %: AVISO y una notificación; evaluar otra vez no la repite")
    void cruceDelOchentaPorCiento() {
        parametros2026();
        cxp("60000000.00", "2026-03-01");
        servicio.evaluar(2026);
        cxp("10000000.00", "2026-05-10");
        EstadoTope e = servicio.evaluar(2026);
        assertThat(e.nivel()).isEqualTo(NivelTope.AVISO);
        assertThat(e.acumulado()).isEqualByComparingTo("70000000.00");
        verify(notificador, times(1)).notificar(contains("80"), anyString());
        servicio.evaluar(2026);
        servicio.evaluar(2026);
        verify(notificador, times(1)).notificar(anyString(), anyString());
    }

    @Test
    @DisplayName("cruzar el 100 %: TOPE y una segunda notificación; saltar de BAJO a TOPE avisa una sola vez, del tope")
    void cruceDelCienPorCiento() {
        parametros2026();
        cxp("70000000.00", "2026-03-01");
        servicio.evaluar(2026);
        cxp("16000000.00", "2026-06-01");
        assertThat(servicio.evaluar(2026).nivel()).isEqualTo(NivelTope.TOPE);
        verify(notificador, times(2)).notificar(anyString(), anyString());
        verify(notificador, times(1)).notificar(contains("100"), anyString());

        reset(notificador);
        servicio.crear(new TopeComprasRtsDTO(2025, new BigDecimal("462200.00"), null, null, null, null), usuario);
        cxp("90000000.00", "2025-02-02");
        assertThat(servicio.evaluar(2025).nivel()).isEqualTo(NivelTope.TOPE);
        verify(notificador, times(1)).notificar(anyString(), anyString());
    }

    @Test
    @DisplayName("si el acumulado baja (anulación) y vuelve a cruzar, vuelve a avisar")
    void bajaYVuelveACruzar() {
        parametros2026();
        cxp("70000000.00", "2026-03-01");
        servicio.evaluar(2026);
        jdbc.update("UPDATE cuentas_por_pagar SET estado = 'ANULADA'");
        assertThat(servicio.evaluar(2026).nivel()).isEqualTo(NivelTope.BAJO);
        cxp("70000000.00", "2026-04-01");
        assertThat(servicio.evaluar(2026).nivel()).isEqualTo(NivelTope.AVISO);
        verify(notificador, times(2)).notificar(anyString(), anyString());
    }

    @Test
    @DisplayName("sin parámetros del año: «Tope de 2027 sin configurar», sin usar los de 2026 y sin avisos")
    void sinParametros() {
        parametros2026();
        cxp("90000000.00", "2027-01-15");
        EstadoTope e = servicio.evaluar(2027);
        assertThat(e.configurado()).isFalse();
        assertThat(e.tope()).isNull();
        assertThat(e.nivel()).isNull();
        assertThat(e.mensaje()).isEqualTo("Tope de 2027 sin configurar");
        verifyNoInteractions(notificador);
    }

    @Test
    @DisplayName("compras en tramo GENERAL no cuentan; la víspera del cambio (2026-08-31) sí")
    void tramoGeneralNoCuenta() {
        parametros2026();
        cxp("30000000.00", "2026-08-31");
        cxp("50000000.00", "2026-09-01");
        cxp("50000000.00", "2026-11-20");
        assertThat(servicio.estado(2026).acumulado()).isEqualByComparingTo("30000000.00");
    }

    @Test
    @DisplayName("una CxP anulada no cuenta")
    void anuladaNoCuenta() {
        parametros2026();
        cxp("40000000.00", "CRC", null, "2026-02-01", "2026-02-01", "ANULADA");
        cxp("1000.00", "2026-02-02");
        assertThat(servicio.estado(2026).acumulado()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("cuenta por la fecha del comprobante (o la de registro si no hay), solo del año; moneda extranjera en CRC")
    void fechaDelAnioYMoneda() {
        parametros2026();
        cxp("5000.00", "CRC", null, "2026-03-10", "2025-12-31", "PENDIENTE");
        cxp("6000.00", "CRC", null, "2025-12-30", "2026-01-01", "PENDIENTE");
        cxp("7000.00", "CRC", null, "2026-02-01", null, "PARCIAL");
        cxp("8000.00", "CRC", null, "2027-01-01", null, "PENDIENTE");
        cxp("1000.00", "USD", "520.00000", "2026-04-04", "2026-04-04", "PENDIENTE");
        assertThat(servicio.estado(2026).acumulado()).isEqualByComparingTo("533000.00");
    }

    @Test
    @DisplayName("evaluarTrasRegistro nunca propaga un fallo (la CxP no se bloquea)")
    void evaluarTrasRegistroNoLanza() {
        parametros2026();
        cxp("70000000.00", "2026-03-01");
        doThrow(new RuntimeException("sin correo")).when(notificador).notificar(anyString(), anyString());
        assertThatCode(() -> servicio.evaluarTrasRegistro(2026)).doesNotThrowAnyException();
        assertThatCode(() -> servicio.evaluarTrasRegistro(2099)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("evaluarAnioActual evalúa el año del reloj (2026)")
    void evaluarAnioActual() {
        parametros2026();
        cxp("70000000.00", "2026-03-01");
        servicio.evaluarAnioActual();
        verify(notificador, times(1)).notificar(contains("80"), anyString());
    }
}
