package api.astro.whats_orders_manager.modules.regimen;

import api.astro.whats_orders_manager.modules.configuracion.dto.HaciendaConsultaDTO;
import api.astro.whats_orders_manager.modules.configuracion.dto.HaciendaConsultaDTO.ResultadoConsulta;
import api.astro.whats_orders_manager.modules.configuracion.dto.HaciendaRegimenDTO;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmpresaRepository;
import api.astro.whats_orders_manager.modules.configuracion.service.HaciendaApiService;
import api.astro.whats_orders_manager.modules.notificacion.enums.CanalNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.enums.TipoNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.model.Notificacion;
import api.astro.whats_orders_manager.modules.notificacion.model.PreferenciaNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.repository.NotificacionRepository;
import api.astro.whats_orders_manager.modules.notificacion.repository.PreferenciaNotificacionRepository;
import api.astro.whats_orders_manager.modules.notificacion.service.NotificacionService;
import api.astro.whats_orders_manager.modules.notificacion.service.PlantillaNotificacionService;
import api.astro.whats_orders_manager.modules.notificacion.service.PreferenciaNotificacionService;
import api.astro.whats_orders_manager.modules.notificacion.service.impl.NotificacionServiceImpl;
import api.astro.whats_orders_manager.modules.notificacion.service.impl.PreferenciaNotificacionServiceImpl;
import api.astro.whats_orders_manager.modules.regimen.aviso.NotificadorAdministradores;
import api.astro.whats_orders_manager.modules.regimen.enums.Regimen;
import api.astro.whats_orders_manager.modules.regimen.model.RegimenTributario;
import api.astro.whats_orders_manager.modules.regimen.repository.RegimenTributarioRepository;
import api.astro.whats_orders_manager.modules.regimen.service.ConsultaDocumentosPorFecha;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenTributarioService;
import api.astro.whats_orders_manager.modules.regimen.tope.TopeComprasRtsDTO;
import api.astro.whats_orders_manager.modules.regimen.tope.TopeComprasRtsRepository;
import api.astro.whats_orders_manager.modules.regimen.tope.TopeComprasRtsService;
import api.astro.whats_orders_manager.modules.regimen.verificacion.VerificacionRegimenRepository;
import api.astro.whats_orders_manager.modules.regimen.verificacion.VerificacionRegimenService;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioActividadService;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T-056 (revisión 1, criterio 15): con un administrador cuya preferencia SISTEMA/WEB está inactiva (filtro de T-030 con el
 * servicio real de preferencias), los avisos críticos (Hacienda difiere, tope al 100 %) igual se guardan; el del 80 % no.
 * Pasa por las decisiones reales de VerificacionRegimenService y TopeComprasRtsService y el NotificadorAdministradores real.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "regimen.verificacion.dias-sin-respuesta-aviso=3"
})
@Import({VerificacionRegimenService.class, TopeComprasRtsService.class, RegimenTributarioService.class,
        NotificadorAdministradores.class, AvisosCriticosSinPreferenciaJpaTest.Config.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("T-056 r1 — avisos críticos ignoran la preferencia de notificación (H2, filtro T-030 real)")
class AvisosCriticosSinPreferenciaJpaTest {

    static final LocalDate HOY = LocalDate.of(2026, 11, 25);

    static final NotificacionRepository notificaciones = mock(NotificacionRepository.class);
    static final UsuarioService usuarios = mock(UsuarioService.class);

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        RelojMutable reloj() {
            return new RelojMutable(HOY);
        }

        @Bean
        UsuarioService usuarioService() {
            return usuarios;
        }

        @Bean
        @Primary
        NotificacionRepository notificacionRepositorySimulado() {
            return notificaciones;
        }

        @Bean
        PreferenciaNotificacionService preferenciaService() {
            PreferenciaNotificacionRepository preferencias = mock(PreferenciaNotificacionRepository.class);
            PreferenciaNotificacion inactiva = PreferenciaNotificacion.builder().idPreferencia(1)
                    .tipoNotificacion(TipoNotificacion.SISTEMA).canal(CanalNotificacion.WEB).activa(false).build();
            when(preferencias.findByUsuarioId(7)).thenReturn(List.of(inactiva));
            PreferenciaNotificacionServiceImpl preferenciaService = new PreferenciaNotificacionServiceImpl();
            ReflectionTestUtils.setField(preferenciaService, "preferenciaRepository", preferencias);
            return preferenciaService;
        }

        @Bean
        PlantillaNotificacionService plantillaService() {
            return mock(PlantillaNotificacionService.class);
        }

        /** El servicio real de notificaciones con el filtro real de preferencias; solo el repositorio es simulado. */
        @Bean
        NotificacionService notificacionService() {
            return new NotificacionServiceImpl();
        }
    }

    @Autowired private VerificacionRegimenService verificacion;
    @Autowired private TopeComprasRtsService tope;
    @Autowired private RegimenTributarioRepository tramos;
    @Autowired private RegimenTributarioService regimenService;
    @Autowired private VerificacionRegimenRepository verificaciones;
    @Autowired private TopeComprasRtsRepository topes;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private HaciendaApiService hacienda;
    @MockitoBean private ConfiguracionEmpresaRepository empresaRepository;
    @MockitoBean private UsuarioActividadService actividadService;
    @MockitoBean private ConsultaDocumentosPorFecha documentos;

    private int secuencia;

    @BeforeEach
    void setUp() {
        reset(notificaciones, usuarios, hacienda, empresaRepository);
        when(notificaciones.save(any(Notificacion.class))).thenAnswer(i -> i.getArgument(0));
        Usuario admin = new Usuario();
        admin.setIdUsuario(7);
        admin.setActivo(true);
        admin.setBloqueado(false);
        when(usuarios.findByRol("ADMIN")).thenReturn(List.of(admin));
        jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
        limpiar();
        tramo(Regimen.SIMPLIFICADO, LocalDate.of(2000, 1, 1));
        tramo(Regimen.GENERAL, LocalDate.of(2026, 9, 1));
        regimenService.invalidarCache();
        ConfiguracionEmpresa empresa = new ConfiguracionEmpresa();
        empresa.setNumeroIdentificacion("3-101-123456");
        when(empresaRepository.findFirstByOrderByIdConfiguracionAsc()).thenReturn(Optional.of(empresa));
    }

    @AfterEach
    void limpiar() {
        jdbc.execute("DELETE FROM cuentas_por_pagar");
        verificaciones.deleteAllInBatch();
        topes.deleteAllInBatch();
        tramos.deleteAllInBatch();
        regimenService.invalidarCache();
    }

    private void tramo(Regimen regimen, LocalDate desde) {
        tramos.saveAndFlush(RegimenTributario.builder().regimen(regimen).vigenteDesde(desde).nota("n")
                .fechaRegistro(LocalDateTime.of(2026, 11, 1, 8, 0)).build());
    }

    private void cxp(String monto, String comprobante) {
        jdbc.update("INSERT INTO cuentas_por_pagar (numero, proveedor_id, descripcion, monto, saldo_pendiente, moneda, tipo_cambio, "
                + "fecha_emision, fecha_comprobante, estado_aceptacion, estado) VALUES (?, 1, 'c', ?, ?, 'CRC', NULL, DATE '"
                + comprobante + "', DATE '" + comprobante + "', 'PENDIENTE', 'PENDIENTE')",
                "CP" + (++secuencia), new BigDecimal(monto), new BigDecimal(monto));
    }

    private void parametros2026() {
        Usuario u = mock(Usuario.class);
        when(u.getIdUsuario()).thenReturn(7);
        tope.crear(new TopeComprasRtsDTO(2026, new BigDecimal("462200.00"), null, null, null, null), u);
    }

    @Test
    @DisplayName("Hacienda difiere del ERP: el aviso se guarda aunque SISTEMA/WEB esté inactiva")
    void difiereLlegaSinPreferencia() {
        doReturn(HaciendaConsultaDTO.builder().nombre("E").exitosa(true).resultado(ResultadoConsulta.EXITOSA)
                .regimen(new HaciendaRegimenDTO(2, "Régimen Simplificado")).build()).when(hacienda).consultarContribuyente(anyString());

        verificacion.verificar();

        verify(notificaciones, atLeastOnce()).save(any(Notificacion.class));
    }

    @Test
    @DisplayName("cruzar el 100 % del tope: el aviso se guarda aunque SISTEMA/WEB esté inactiva")
    void topeLlegaSinPreferencia() {
        parametros2026();
        cxp("86000000.00", "2026-03-01");

        tope.evaluar(2026);

        verify(notificaciones, atLeastOnce()).save(any(Notificacion.class));
    }

    @Test
    @DisplayName("cruzar el 80 % del tope: respeta la preferencia y no se crea la notificación")
    void avisoOchentaRespetaPreferencia() {
        parametros2026();
        cxp("70000000.00", "2026-03-01");

        tope.evaluar(2026);

        verify(notificaciones, never()).save(any(Notificacion.class));
    }
}
