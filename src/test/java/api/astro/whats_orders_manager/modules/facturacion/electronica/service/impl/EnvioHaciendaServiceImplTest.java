package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ComprobanteElectronicoDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.RespuestaHaciendaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.ResultadoEnvioHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ComprobanteElectronicoService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.EnvioHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.HaciendaApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * T-037 (ADR-043): etapas del envío con su transacción, la marca previa al POST, el registro y el sondeo en segundo
 * plano. Sin BD: el gestor de transacciones de {@link EnvioHaciendaDePrueba} abre transacciones reales para
 * TransactionSynchronizationManager. La concurrencia, el consecutivo y la caída del registro se prueban con MySQL en
 * EnvioHaciendaEtapasIT.
 */
@DisplayName("T-037 — envío a Hacienda en etapas cortas")
class EnvioHaciendaServiceImplTest {

    private static final Instant AHORA = Instant.parse("2026-11-17T18:00:00Z");

    private final ComprobanteElectronicoRepository repository = mock(ComprobanteElectronicoRepository.class);
    private final ComprobanteElectronicoService comprobantes = mock(ComprobanteElectronicoService.class);
    private final HaciendaApiService hacienda = mock(HaciendaApiService.class);
    private final Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);
    private ComprobanteElectronico comprobante;

    @BeforeEach
    void preparar() {
        comprobante = ComprobanteElectronico.builder().id(5L).empresa(ConfiguracionEmpresa.builder().idConfiguracion(1).build())
            .tipoComprobante(TipoComprobanteElectronico.FACTURA_ELECTRONICA).claveNumerica("506CLAVE")
            .consecutivo("00000000000000000007").estado(EstadoComprobante.FIRMADO).vigente(true).intentosEnvio(0)
            .xmlComprobante("<FacturaElectronica/>").build();
        EnvioHaciendaDePrueba.simularBloqueoYMarca(repository, () -> comprobante);
        when(repository.findById(5L)).thenAnswer(inv -> Optional.of(comprobante));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(comprobantes.prepararParaEnvio(any())).thenReturn(9L);
    }

    private static RespuestaHaciendaDTO recibido() {
        return RespuestaHaciendaDTO.builder().resultadoEnvio(ResultadoEnvioHacienda.RECIBIDO).codigoHttp(202)
            .mensaje("Comprobante recibido por Hacienda, en procesamiento").build();
    }

    @Test
    @DisplayName("criterio 2: preparación, marca y registro en transacción; token y POST sin transacción activa")
    void etapasConYSinTransaccion() {
        AtomicBoolean preparacionEnTransaccion = new AtomicBoolean();
        AtomicBoolean postSinTransaccion = new AtomicBoolean();
        when(comprobantes.prepararParaEnvio(any())).thenAnswer(inv -> {
            preparacionEnTransaccion.set(TransactionSynchronizationManager.isActualTransactionActive());
            return 9L;
        });
        when(hacienda.enviarComprobante(anyString(), anyString(), anyLong())).thenAnswer(inv -> {
            postSinTransaccion.set(!TransactionSynchronizationManager.isActualTransactionActive());
            assertThat(comprobante.getEstado()).as("marca previa al POST").isEqualTo(EstadoComprobante.ENVIO_INCIERTO);
            assertThat(comprobante.getFechaEnvio()).isEqualTo(LocalDateTime.ofInstant(AHORA, ZoneOffset.UTC));
            return recibido();
        });

        ComprobanteElectronicoDTO resultado = EnvioHaciendaDePrueba.crear(repository, comprobantes, hacienda, clock)
            .enviar(5L);

        assertThat(preparacionEnTransaccion).isTrue();
        assertThat(postSinTransaccion).as("el POST corre sin transacción activa").isTrue();
        assertThat(resultado.getEstado()).isEqualTo(EstadoComprobante.ENVIADO);
        assertThat(comprobante.getIntentosEnvio()).isEqualTo(1);
        verify(hacienda, times(1)).enviarComprobante("<FacturaElectronica/>", "506CLAVE", 9L);
    }

    @Test
    @DisplayName("criterio 2: con una transacción del llamador abierta, el POST la suspende")
    void postSuspendeLaTransaccionDelLlamador() {
        AtomicBoolean postSinTransaccion = new AtomicBoolean();
        when(hacienda.enviarComprobante(anyString(), anyString(), anyLong())).thenAnswer(inv -> {
            postSinTransaccion.set(!TransactionSynchronizationManager.isActualTransactionActive());
            return recibido();
        });
        EnvioHaciendaServiceImpl envio = EnvioHaciendaDePrueba.crear(repository, comprobantes, hacienda, clock);
        org.springframework.transaction.support.TransactionTemplate llamador =
            new org.springframework.transaction.support.TransactionTemplate(new EnvioHaciendaDePrueba.SinBd());

        llamador.executeWithoutResult(st -> envio.enviar(5L));

        assertThat(postSinTransaccion).isTrue();
    }

    @Test
    @DisplayName("criterio 4: si la preparación falla (XSD, firma, configuración) no hay marca ni POST")
    void fallaLaPreparacion_sinMarcaNiPost() {
        when(comprobantes.prepararParaEnvio(any())).thenAnswer(inv -> {
            comprobante.setEstado(EstadoComprobante.ERROR);
            comprobante.setUltimoError(ComprobanteElectronicoServiceImpl.ENCABEZADO_XML_INVALIDO_XSD + "\nlínea 1, columna 2: x");
            return null;
        });

        ComprobanteElectronicoDTO resultado = EnvioHaciendaDePrueba.crear(repository, comprobantes, hacienda, clock)
            .enviar(5L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoComprobante.ERROR);
        verify(repository, never()).marcarEnvioEnCurso(anyLong(), any(), any(), any());
        verifyNoInteractions(hacienda);
    }

    @Test
    @DisplayName("criterio 5: la marca actualiza 0 filas → no se transmite y se devuelve el estado actual")
    void marcaSinFilas_noTransmite() {
        org.mockito.Mockito.doAnswer(inv -> {
            comprobante.setEstado(EstadoComprobante.ENVIO_INCIERTO); // otro proceso lo tomó entre la preparación y la marca
            return 0;
        }).when(repository).marcarEnvioEnCurso(anyLong(), any(), any(), any());

        ComprobanteElectronicoDTO resultado = EnvioHaciendaDePrueba.crear(repository, comprobantes, hacienda, clock)
            .enviar(5L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoComprobante.ENVIO_INCIERTO);
        verifyNoInteractions(hacienda);
    }

    @Test
    @DisplayName("criterio 5: la marca es condicional sobre GENERADO, FIRMADO, ERROR y NO_TRANSMITIDO, solo el vigente")
    void marcaCondicionalSobreLosTransmisibles() {
        when(hacienda.enviarComprobante(anyString(), anyString(), anyLong())).thenReturn(recibido());

        EnvioHaciendaDePrueba.crear(repository, comprobantes, hacienda, clock).enviar(5L);

        verify(repository).marcarEnvioEnCurso(org.mockito.ArgumentMatchers.eq(5L),
            org.mockito.ArgumentMatchers.eq(LocalDateTime.ofInstant(AHORA, ZoneOffset.UTC)),
            org.mockito.ArgumentMatchers.eq(EstadoComprobante.ENVIO_INCIERTO),
            org.mockito.ArgumentMatchers.argThat(estados -> estados.size() == 4
                && estados.containsAll(List.of(EstadoComprobante.GENERADO, EstadoComprobante.FIRMADO,
                    EstadoComprobante.ERROR, EstadoComprobante.NO_TRANSMITIDO))));
    }

    @Test
    @DisplayName("criterio 6: reenviar un comprobante marcado (ENVIO_INCIERTO) → «no puede ser reenviado», sin POST")
    void reenviarMarcado_mensajeActual() {
        comprobante.setEstado(EstadoComprobante.ENVIO_INCIERTO);

        assertThatThrownBy(() -> EnvioHaciendaDePrueba.crear(repository, comprobantes, hacienda, clock).reenviar(5L))
            .isInstanceOf(EnvioHaciendaService.ComprobanteNoEnviableException.class)
            .hasMessage("Comprobante no puede ser reenviado en estado ENVIO_INCIERTO");
        verifyNoInteractions(hacienda);
    }

    @Test
    @DisplayName("criterio 7: el POST lanza una excepción no clasificada → ENVIO_INCIERTO con fecha_envio")
    void excepcionNoClasificada_quedaIncierto() {
        when(hacienda.enviarComprobante(anyString(), anyString(), anyLong()))
            .thenThrow(new IllegalStateException("fallo inesperado del cliente"));

        ComprobanteElectronicoDTO resultado = EnvioHaciendaDePrueba.crear(repository, comprobantes, hacienda, clock)
            .enviar(5L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoComprobante.ENVIO_INCIERTO);
        assertThat(comprobante.getFechaEnvio()).isNotNull();
        assertThat(comprobante.getUltimoError()).isEqualTo(EnvioHaciendaServiceImpl.ERROR_NO_CLASIFICADO);
    }

    @Test
    @DisplayName("criterio 8: el registro no pisa un estado que otro proceso adoptó tras la marca")
    void registroNoPisaOtroEstado() {
        when(hacienda.enviarComprobante(anyString(), anyString(), anyLong())).thenAnswer(inv -> {
            comprobante.setEstado(EstadoComprobante.ACEPTADO); // p. ej. la consulta manual mientras viajaba el POST
            return recibido();
        });

        ComprobanteElectronicoDTO resultado = EnvioHaciendaDePrueba.crear(repository, comprobantes, hacienda, clock)
            .enviar(5L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoComprobante.ACEPTADO);
    }

    @Test
    @DisplayName("job: registrar el fallo de un reintento relee la fila y no pisa la marca de otro envío")
    void falloDeReintentoNoPisaLaMarca() {
        comprobante.setEstado(EstadoComprobante.ENVIO_INCIERTO);
        EnvioHaciendaDePrueba.crear(repository, comprobantes, hacienda, clock).registrarFalloDeReintento(5L, "x");
        assertThat(comprobante.getUltimoError()).isNull();
        assertThat(comprobante.getIntentosEnvio()).isZero();

        comprobante.setEstado(EstadoComprobante.NO_TRANSMITIDO);
        EnvioHaciendaDePrueba.crear(repository, comprobantes, hacienda, clock).registrarFalloDeReintento(5L, "Reintento 1: x");
        assertThat(comprobante.getUltimoError()).isEqualTo("Reintento 1: x");
        assertThat(comprobante.getIntentosEnvio()).isEqualTo(1);
    }

    @Test
    @DisplayName("review 1, c.26: si dos emisiones de la misma factura chocan con uk_comprobante_un_vigente_por_factura → 409 fijo")
    void choqueDelVigenteUnico_mismo409() {
        when(comprobantes.generarParaEmision(7L)).thenThrow(new org.springframework.dao.DataIntegrityViolationException(
            "could not execute statement", new java.sql.SQLIntegrityConstraintViolationException(
                "Duplicate entry '7' for key 'comprobante_electronico.uk_comprobante_un_vigente_por_factura'")));

        assertThatThrownBy(() -> EnvioHaciendaDePrueba.crear(repository, comprobantes, hacienda, clock).emitirFactura(7L))
            .isInstanceOf(api.astro.whats_orders_manager.modules.facturacion.electronica.exception.EmisionNoPermitidaException.class)
            .hasMessage(ComprobanteElectronicoServiceImpl.MENSAJE_COMPROBANTE_EN_CURSO);
        verifyNoInteractions(hacienda);
    }

    /** TaskScheduler controlable: guarda las tareas y su instante, sin hilos ni esperas reales. */
    static final class ProgramadorDePrueba implements TaskScheduler {
        final List<Runnable> tareas = new ArrayList<>();
        final List<Instant> instantes = new ArrayList<>();

        void ejecutarSiguiente() {
            tareas.remove(0).run();
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
            tareas.add(task);
            instantes.add(startTime);
            return null;
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
            throw new UnsupportedOperationException();
        }
    }

    @Nested
    @DisplayName("criterio 15: sondeo en segundo plano tras un ENVIADO")
    class Sondeo {

        private final ProgramadorDePrueba programador = new ProgramadorDePrueba();

        private ComprobanteElectronicoDTO estado(EstadoComprobante e) {
            return ComprobanteElectronicoDTO.builder().id(5L).estado(e).build();
        }

        @BeforeEach
        void enviado() {
            when(hacienda.enviarComprobante(anyString(), anyString(), anyLong())).thenReturn(recibido());
        }

        @Test
        @DisplayName("ENVIADO → consulta a +30 s, fuera de la petición; hasta 5 consultas si sigue procesando")
        void hastaCincoConsultas() {
            when(comprobantes.consultarYActualizarEstado(5L)).thenReturn(estado(EstadoComprobante.PROCESANDO));
            EnvioHaciendaServiceImpl envio = EnvioHaciendaDePrueba.crear(repository, comprobantes, hacienda, clock,
                programador);

            envio.enviar(5L);

            verify(comprobantes, never()).consultarYActualizarEstado(anyLong()); // nada se consulta dentro de la petición
            assertThat(programador.tareas).hasSize(1);
            assertThat(programador.instantes.get(0)).isEqualTo(AHORA.plusSeconds(30));
            for (int i = 1; i <= 5; i++) {
                programador.ejecutarSiguiente();
            }
            verify(comprobantes, times(5)).consultarYActualizarEstado(5L);
            assertThat(programador.tareas).as("tras la 5.ª consulta sigue el job horario").isEmpty();
        }

        @Test
        @DisplayName("se detiene en un estado terminal (ACEPTADO)")
        void seDetieneEnTerminal() {
            when(comprobantes.consultarYActualizarEstado(5L)).thenReturn(estado(EstadoComprobante.PROCESANDO),
                estado(EstadoComprobante.ACEPTADO));
            EnvioHaciendaDePrueba.crear(repository, comprobantes, hacienda, clock, programador).enviar(5L);

            programador.ejecutarSiguiente();
            programador.ejecutarSiguiente();

            verify(comprobantes, times(2)).consultarYActualizarEstado(5L);
            assertThat(programador.tareas).isEmpty();
        }

        @Test
        @DisplayName("se detiene en ENVIO_INCIERTO")
        void seDetieneEnIncierto() {
            when(comprobantes.consultarYActualizarEstado(5L)).thenReturn(estado(EstadoComprobante.ENVIO_INCIERTO));
            EnvioHaciendaDePrueba.crear(repository, comprobantes, hacienda, clock, programador).enviar(5L);

            programador.ejecutarSiguiente();

            assertThat(programador.tareas).isEmpty();
        }

        @Test
        @DisplayName("una consulta que falla no corta el sondeo")
        void consultaQueFallaSigue() {
            when(comprobantes.consultarYActualizarEstado(5L)).thenThrow(new RuntimeException("sin respuesta"))
                .thenReturn(estado(EstadoComprobante.ACEPTADO));
            EnvioHaciendaDePrueba.crear(repository, comprobantes, hacienda, clock, programador).enviar(5L);

            programador.ejecutarSiguiente();
            programador.ejecutarSiguiente();

            verify(comprobantes, times(2)).consultarYActualizarEstado(5L);
        }

        @Test
        @DisplayName("NO_TRANSMITIDO o ENVIO_INCIERTO al enviar: no se programa sondeo")
        void sinRecepcionNoHaySondeo() {
            when(hacienda.enviarComprobante(anyString(), anyString(), anyLong())).thenReturn(RespuestaHaciendaDTO.builder()
                .resultadoEnvio(ResultadoEnvioHacienda.NO_TRANSMITIDO).mensaje("sin conexión").build());
            EnvioHaciendaDePrueba.crear(repository, comprobantes, hacienda, clock, programador).enviar(5L);

            assertThat(programador.tareas).isEmpty();
        }

        @Test
        @DisplayName("app.scheduling.enabled=false (sin TaskScheduler): no se programa nada y el envío responde igual")
        void sinTaskScheduler() {
            ComprobanteElectronicoDTO resultado = EnvioHaciendaDePrueba.crear(repository, comprobantes, hacienda, clock)
                .enviar(5L);

            assertThat(resultado.getEstado()).isEqualTo(EstadoComprobante.ENVIADO);
            verify(comprobantes, never()).consultarYActualizarEstado(anyLong());
        }
    }
}
