package api.astro.whats_orders_manager.modules.regimen;

import api.astro.whats_orders_manager.modules.configuracion.dto.HaciendaConsultaDTO;
import api.astro.whats_orders_manager.modules.configuracion.dto.HaciendaConsultaDTO.ResultadoConsulta;
import api.astro.whats_orders_manager.modules.configuracion.dto.HaciendaRegimenDTO;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmpresaRepository;
import api.astro.whats_orders_manager.modules.configuracion.service.HaciendaApiService;
import api.astro.whats_orders_manager.modules.regimen.aviso.NotificadorAdministradores;
import api.astro.whats_orders_manager.modules.regimen.enums.Regimen;
import api.astro.whats_orders_manager.modules.regimen.model.RegimenTributario;
import api.astro.whats_orders_manager.modules.regimen.repository.RegimenTributarioRepository;
import api.astro.whats_orders_manager.modules.regimen.service.ConsultaDocumentosPorFecha;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenTributarioService;
import api.astro.whats_orders_manager.modules.regimen.verificacion.ResultadoVerificacion;
import api.astro.whats_orders_manager.modules.regimen.verificacion.VerificacionRegimen;
import api.astro.whats_orders_manager.modules.regimen.verificacion.VerificacionRegimenRepository;
import api.astro.whats_orders_manager.modules.regimen.verificacion.VerificacionRegimenService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * T-056 (criterios 5 y 6): VerificacionRegimenService contra una base real (H2) con HaciendaApiService simulado:
 * códigos 1, 2, 3, uno desconocido, NO_ENCONTRADA y ERROR; guarda cada verificación y conserva la última; DIFIERE avisa
 * una vez por cambio; SIN_RESPUESTA avisa solo al repetirse N días; nunca modifica regimen_tributario. Hoy es 2026-11-25
 * y el régimen vigente es GENERAL (desde 2026-09-01).
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "regimen.verificacion.dias-sin-respuesta-aviso=3",
        "regimen.verificacion.retencion-dias=90"
})
@Import({VerificacionRegimenService.class, RegimenTributarioService.class, VerificacionRegimenServiceJpaTest.Config.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("T-056 — verificación del régimen con Hacienda (H2, Hacienda simulado)")
class VerificacionRegimenServiceJpaTest {

    static final LocalDate HOY = LocalDate.of(2026, 11, 25);

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        RelojMutable reloj() {
            return new RelojMutable(HOY);
        }
    }

    @Autowired private VerificacionRegimenService servicio;
    @Autowired private VerificacionRegimenRepository repositorio;
    @Autowired private RegimenTributarioRepository tramos;
    @Autowired private RegimenTributarioService regimenService;
    @Autowired private RelojMutable reloj;
    @MockitoBean private HaciendaApiService hacienda;
    @MockitoBean private ConfiguracionEmpresaRepository empresaRepository;
    @MockitoBean private NotificadorAdministradores notificador;
    @MockitoBean private UsuarioActividadService actividadService;
    @MockitoBean private ConsultaDocumentosPorFecha documentos;

    @BeforeEach
    void setUp() {
        reset(hacienda, empresaRepository, notificador);
        reloj.fijar(HOY);
        repositorio.deleteAllInBatch();
        tramos.deleteAllInBatch();
        tramo(Regimen.SIMPLIFICADO, LocalDate.of(2000, 1, 1));
        tramo(Regimen.GENERAL, LocalDate.of(2026, 9, 1));
        regimenService.invalidarCache();
        ConfiguracionEmpresa empresa = new ConfiguracionEmpresa();
        empresa.setNumeroIdentificacion("3-101-123456");
        when(empresaRepository.findFirstByOrderByIdConfiguracionAsc()).thenReturn(Optional.of(empresa));
    }

    @AfterEach
    void limpiar() {
        repositorio.deleteAllInBatch();
        tramos.deleteAllInBatch();
        regimenService.invalidarCache();
    }

    private void tramo(Regimen regimen, LocalDate desde) {
        tramos.saveAndFlush(RegimenTributario.builder().regimen(regimen).vigenteDesde(desde).nota("n")
                .fechaRegistro(LocalDateTime.of(2026, 11, 1, 8, 0)).build());
    }

    private void respuestaHacienda(Integer codigo, String descripcion) {
        HaciendaConsultaDTO dto = HaciendaConsultaDTO.builder().nombre("EMISOR").exitosa(true)
                .resultado(ResultadoConsulta.EXITOSA)
                .regimen(codigo == null ? null : new HaciendaRegimenDTO(codigo, descripcion)).build();
        doReturn(dto).when(hacienda).consultarContribuyente(anyString());
    }

    private void respuestaTipificada(ResultadoConsulta resultado) {
        doReturn(HaciendaConsultaDTO.builder().exitosa(false).resultado(resultado).mensajeError("m").build())
                .when(hacienda).consultarContribuyente(anyString());
    }

    // ── Resultados ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("código 1 (general) y el ERP en GENERAL: COINCIDE, guarda código y descripción, sin aviso")
    void coincide() {
        respuestaHacienda(1, "Régimen General");
        VerificacionRegimen v = servicio.verificar();
        assertThat(v.getResultado()).isEqualTo(ResultadoVerificacion.COINCIDE);
        assertThat(v.getCodigoHacienda()).isEqualTo(1);
        assertThat(v.getDescripcionHacienda()).isEqualTo("Régimen General");
        assertThat(v.getRegimenHacienda()).isEqualTo("GENERAL");
        assertThat(v.getRegimenLocal()).isEqualTo("GENERAL");
        assertThat(repositorio.count()).isEqualTo(1);
        verifyNoInteractions(notificador);
        assertThat(servicio.avisoPanel()).isEmpty();
    }

    @Test
    @DisplayName("código 2 (simplificado) con el ERP en GENERAL: DIFIERE, avisa a los administradores y el panel muestra los dos valores")
    void difiere() {
        respuestaHacienda(2, "Régimen Simplificado");
        VerificacionRegimen v = servicio.verificar();
        assertThat(v.getResultado()).isEqualTo(ResultadoVerificacion.DIFIERE);
        assertThat(v.getRegimenHacienda()).isEqualTo("SIMPLIFICADO");
        verify(notificador, times(1)).notificarCritico(anyString(), anyString());
        verify(notificador, never()).notificar(anyString(), anyString());
        assertThat(servicio.avisoPanel()).hasValueSatisfying(texto -> assertThat(texto)
                .contains("Hacienda").contains("Simplificado").contains("General").contains("Régimen Simplificado"));
    }

    @Test
    @DisplayName("el aviso de DIFIERE menciona el próximo tramo si existe")
    void difiereConProximoTramo() {
        tramo(Regimen.SIMPLIFICADO, LocalDate.of(2027, 1, 1));
        regimenService.invalidarCache();
        respuestaHacienda(2, "Régimen Simplificado");
        servicio.verificar();
        assertThat(servicio.avisoPanel()).hasValueSatisfying(texto -> assertThat(texto)
                .contains("Próximo tramo").contains("2027-01-01").contains("Simplificado"));
    }

    @Test
    @DisplayName("código 3 (no contribuyente) y un código desconocido: se traducen a OTRO y DIFIEREN")
    void codigoTresYDesconocido() {
        respuestaHacienda(3, "No contribuyente");
        VerificacionRegimen tres = servicio.verificar();
        assertThat(tres.getRegimenHacienda()).isEqualTo("OTRO");
        assertThat(tres.getResultado()).isEqualTo(ResultadoVerificacion.DIFIERE);
        respuestaHacienda(9, "Régimen nuevo");
        VerificacionRegimen nueve = servicio.verificar();
        assertThat(nueve.getRegimenHacienda()).isEqualTo("OTRO");
        assertThat(nueve.getCodigoHacienda()).isEqualTo(9);
        assertThat(nueve.getResultado()).isEqualTo(ResultadoVerificacion.DIFIERE);
    }

    @Test
    @DisplayName("NO_ENCONTRADA de Hacienda: NO_ENCONTRADO, sin aviso")
    void noEncontrado() {
        respuestaTipificada(ResultadoConsulta.NO_ENCONTRADA);
        assertThat(servicio.verificar().getResultado()).isEqualTo(ResultadoVerificacion.NO_ENCONTRADO);
        verifyNoInteractions(notificador);
    }

    @Test
    @DisplayName("ERROR de Hacienda, una excepción, una respuesta sin régimen y la empresa sin identificación: SIN_RESPUESTA")
    void sinRespuesta() {
        respuestaTipificada(ResultadoConsulta.ERROR);
        assertThat(servicio.verificar().getResultado()).isEqualTo(ResultadoVerificacion.SIN_RESPUESTA);
        when(hacienda.consultarContribuyente(anyString())).thenThrow(new IllegalStateException("caída"));
        assertThat(servicio.verificar().getResultado()).isEqualTo(ResultadoVerificacion.SIN_RESPUESTA);
        respuestaHacienda(null, null);
        assertThat(servicio.verificar().getResultado()).isEqualTo(ResultadoVerificacion.SIN_RESPUESTA);

        reset(hacienda);
        ConfiguracionEmpresa sinId = new ConfiguracionEmpresa();
        when(empresaRepository.findFirstByOrderByIdConfiguracionAsc()).thenReturn(Optional.of(sinId));
        assertThat(servicio.verificar().getResultado()).isEqualTo(ResultadoVerificacion.SIN_RESPUESTA);
        verifyNoInteractions(hacienda);
    }

    // ── Aviso sin repetir y nunca escribe el régimen ─────────────────────────

    @Test
    @DisplayName("DIFIERE repetido con los mismos valores notifica una sola vez; si cambian, vuelve a notificar")
    void notificaSoloAlCambiar() {
        respuestaHacienda(2, "Régimen Simplificado");
        servicio.verificar();
        servicio.verificar();
        verify(notificador, times(1)).notificarCritico(anyString(), anyString());
        respuestaHacienda(3, "No contribuyente");
        servicio.verificar();
        verify(notificador, times(2)).notificarCritico(anyString(), anyString());
        respuestaHacienda(1, "Régimen General");
        servicio.verificar();
        respuestaHacienda(2, "Régimen Simplificado");
        servicio.verificar();
        verify(notificador, times(3)).notificarCritico(anyString(), anyString());
    }

    @Test
    @DisplayName("nunca modifica regimen_tributario, ni con DIFIERE")
    void noModificaTramos() {
        List<RegimenTributario> antes = tramos.findAllByOrderByVigenteDesdeAsc();
        respuestaHacienda(2, "Régimen Simplificado");
        servicio.verificar();
        respuestaHacienda(1, "Régimen General");
        servicio.verificar();
        List<RegimenTributario> despues = tramos.findAllByOrderByVigenteDesdeAsc();
        assertThat(despues).hasSameSizeAs(antes);
        for (int i = 0; i < antes.size(); i++) {
            assertThat(despues.get(i).getId()).isEqualTo(antes.get(i).getId());
            assertThat(despues.get(i).getRegimen()).isEqualTo(antes.get(i).getRegimen());
            assertThat(despues.get(i).getVigenteDesde()).isEqualTo(antes.get(i).getVigenteDesde());
            assertThat(despues.get(i).getNota()).isEqualTo(antes.get(i).getNota());
        }
    }

    // ── SIN_RESPUESTA repetido ───────────────────────────────────────────────

    @Test
    @DisplayName("SIN_RESPUESTA avisa solo al repetirse 3 días seguidos (un solo aviso); varias veces el mismo día no suman")
    void sinRespuestaRepetido() {
        respuestaTipificada(ResultadoConsulta.ERROR);
        servicio.verificar();
        servicio.verificar();
        assertThat(servicio.avisoPanel()).as("mismo día, dos veces").isEmpty();
        reloj.fijar(HOY.plusDays(1));
        servicio.verificar();
        assertThat(servicio.avisoPanel()).as("2 días").isEmpty();
        verifyNoInteractions(notificador);
        reloj.fijar(HOY.plusDays(2));
        servicio.verificar();
        assertThat(servicio.avisoPanel()).hasValueSatisfying(t -> assertThat(t).contains("3 días"));
        verify(notificador, times(1)).notificar(anyString(), anyString());
        reloj.fijar(HOY.plusDays(3));
        servicio.verificar();
        verify(notificador, times(1)).notificar(anyString(), anyString());
    }

    @Test
    @DisplayName("una verificación con respuesta en medio reinicia la cuenta de SIN_RESPUESTA")
    void respuestaReiniciaLaCuenta() {
        respuestaTipificada(ResultadoConsulta.ERROR);
        servicio.verificar();
        reloj.fijar(HOY.plusDays(1));
        servicio.verificar();
        respuestaHacienda(1, "Régimen General");
        reloj.fijar(HOY.plusDays(2));
        servicio.verificar();
        respuestaTipificada(ResultadoConsulta.ERROR);
        reloj.fijar(HOY.plusDays(3));
        servicio.verificar();
        assertThat(servicio.avisoPanel()).isEmpty();
        verify(notificador, never()).notificar(anyString(), anyString());
    }

    // ── Retención ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("retención corta: se borran las verificaciones de más de 90 días y siempre queda la última")
    void retencion() {
        respuestaHacienda(1, "Régimen General");
        reloj.fijar(HOY.minusDays(200));
        servicio.verificar();
        reloj.fijar(HOY.minusDays(100));
        servicio.verificar();
        assertThat(repositorio.count()).as("la de hace 200 días ya pasó la retención").isEqualTo(1);
        reloj.fijar(HOY.minusDays(95));
        servicio.verificar();
        assertThat(repositorio.count()).as("la de -100 aún está dentro de los 90 días de la de -95").isEqualTo(2);
        reloj.fijar(HOY);
        servicio.verificar();
        assertThat(repositorio.count()).isEqualTo(1);
        assertThat(servicio.ultima()).hasValueSatisfying(v -> assertThat(v.getFechaHora().toLocalDate()).isEqualTo(HOY));
    }

    @Test
    @DisplayName("nunca borra la última aunque sea más vieja que la retención")
    void conservaLaUltima() {
        respuestaHacienda(1, "Régimen General");
        reloj.fijar(HOY.minusDays(200));
        servicio.verificar();
        reloj.fijar(HOY);
        servicio.purgarAntiguas();
        assertThat(repositorio.count()).isEqualTo(1);
    }
}
