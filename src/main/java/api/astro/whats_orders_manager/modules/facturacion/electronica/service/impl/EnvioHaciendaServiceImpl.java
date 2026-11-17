package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ComprobanteElectronicoDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.RespuestaHaciendaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.ResultadoEnvioHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.mapper.ComprobanteElectronicoMapper;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ComprobanteElectronicoService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.EnvioHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.HaciendaApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * Envío de comprobantes a Hacienda en etapas cortas (T-037, ADR-043). Ver {@link EnvioHaciendaService}.
 *
 * <p>Las transacciones se abren aquí con TransactionTemplate: REQUIRES_NEW para la preparación (a), la marca (b) y el
 * registro (d), y NOT_SUPPORTED para el token y el POST (c), que así corren sin transacción activa aunque el llamador
 * tuviera una. Cada consulta del sondeo (e) corre en la transacción de
 * {@link ComprobanteElectronicoService#consultarYActualizarEstado}, invocada a través del proxy.
 */
@Service
@Slf4j
public class EnvioHaciendaServiceImpl implements EnvioHaciendaService {

    /** Estados desde los que se transmite (ADR-030): Hacienda no tiene el comprobante. */
    public static final Set<EstadoComprobante> TRANSMISIBLES = EnumSet.of(EstadoComprobante.GENERADO,
        EstadoComprobante.FIRMADO, EstadoComprobante.ERROR, EstadoComprobante.NO_TRANSMITIDO);

    /** ultimoError cuando el POST terminó con una excepción que el cliente no clasificó: Hacienda pudo recibirlo. */
    public static final String ERROR_NO_CLASIFICADO =
        "El envío terminó con un error no clasificado; Hacienda pudo recibirlo. Se verificará con Hacienda.";

    /** Espera antes de cada consulta del sondeo, por defecto (ADR-043 §3). */
    public static final Duration ESPERA_SONDEO_DEFECTO = Duration.ofSeconds(30);

    /** Consultas del sondeo en segundo plano, por defecto (ADR-043 §3). */
    public static final int MAX_CONSULTAS_SONDEO_DEFECTO = 5;

    private final ComprobanteElectronicoRepository repository;
    private final ComprobanteElectronicoService comprobanteService;
    private final ComprobanteElectronicoMapper mapper;
    private final HaciendaApiService haciendaApi;
    private final Clock clock;
    private final ObjectProvider<TaskScheduler> scheduler;
    private final TransactionTemplate nueva;
    private final TransactionTemplate sinTransaccion;
    private final Duration esperaSondeo;
    private final int maxConsultasSondeo;

    @Autowired
    public EnvioHaciendaServiceImpl(ComprobanteElectronicoRepository repository,
                                    ComprobanteElectronicoService comprobanteService,
                                    ComprobanteElectronicoMapper mapper,
                                    HaciendaApiService haciendaApi,
                                    Clock clock,
                                    @Qualifier("taskScheduler") ObjectProvider<TaskScheduler> scheduler,
                                    PlatformTransactionManager transactionManager,
                                    @Value("${facturacion.hacienda.sondeo.espera:30s}") Duration esperaSondeo,
                                    @Value("${facturacion.hacienda.sondeo.max-consultas:5}") int maxConsultasSondeo) {
        this.repository = repository;
        this.comprobanteService = comprobanteService;
        this.mapper = mapper;
        this.haciendaApi = haciendaApi;
        this.clock = clock;
        this.scheduler = scheduler;
        this.nueva = new TransactionTemplate(transactionManager);
        this.nueva.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.sinTransaccion = new TransactionTemplate(transactionManager);
        this.sinTransaccion.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
        this.esperaSondeo = esperaSondeo;
        this.maxConsultasSondeo = maxConsultasSondeo;
    }

    @Override
    public ComprobanteElectronicoDTO emitirFactura(Long facturaId) {
        // Etapa a (1/2): cliente, CAByS (409 sin consumir consecutivo), vigente, consecutivo con bloqueo, clave y XML
        ComprobanteElectronicoDTO generado = nueva.execute(st -> comprobanteService.generarParaEmision(facturaId));
        return enviarComprobante(generado.getId(), false);
    }

    @Override
    public ComprobanteElectronicoDTO enviar(Long comprobanteId) {
        return enviarComprobante(comprobanteId, false);
    }

    @Override
    public ComprobanteElectronicoDTO reenviar(Long comprobanteId) {
        return enviarComprobante(comprobanteId, true);
    }

    private ComprobanteElectronicoDTO enviarComprobante(Long id, boolean reenvio) {
        // a. Preparación, con la fila bloqueada: firma, XSD y configuración. ERROR local → sin marca ni POST
        Long configuracionId = nueva.execute(st -> preparar(id, reenvio));
        if (configuracionId == null) {
            return leer(id);
        }

        // b. Marca: UPDATE condicional a ENVIO_INCIERTO. 0 filas → otro proceso ya envía: no se transmite
        Marcado marcado = nueva.execute(st -> marcar(id));
        if (marcado == null) {
            log.warn("Comprobante {}: no se tomó la marca de envío (otro proceso lo envía o ya no es transmisible)", id);
            return leer(id);
        }

        // c. Token y POST de recepción, sin transacción activa y sin reintento automático (ADR-043 §2)
        Transmision transmision = sinTransaccion.execute(st -> transmitir(marcado, configuracionId));

        // d. Registro. Si falla, la marca queda: ENVIO_INCIERTO con fecha_envio, nunca un estado transmisible
        try {
            nueva.executeWithoutResult(st -> registrar(id, transmision));
        } catch (RuntimeException e) {
            log.error("No se pudo registrar el resultado del envío de {} ({}); queda en ENVIO_INCIERTO y se "
                + "verificará con Hacienda", marcado.clave(), e.getMessage(), e);
        }

        ComprobanteElectronicoDTO resultado = leer(id);
        // e. Sondeo en segundo plano, fuera de la petición
        if (resultado.getEstado() == EstadoComprobante.ENVIADO) {
            programarSondeo(id, 1);
        }
        return resultado;
    }

    private Long preparar(Long id, boolean reenvio) {
        ComprobanteElectronico comprobante = repository.findByIdParaEnvio(id)
            .orElseThrow(() -> new IllegalArgumentException("Comprobante no encontrado"));
        if (!comprobante.puedeReenviar()) {
            throw new ComprobanteNoEnviableException(comprobante.getEstado(), reenvio);
        }
        Long configuracionId = comprobanteService.prepararParaEnvio(comprobante);
        repository.save(comprobante);
        return configuracionId;
    }

    private Marcado marcar(Long id) {
        int filas = repository.marcarEnvioEnCurso(id, LocalDateTime.now(clock), EstadoComprobante.ENVIO_INCIERTO,
            TRANSMISIBLES);
        if (filas == 0) {
            return null;
        }
        // Lo que se transmite es lo que quedó marcado
        ComprobanteElectronico marcado = repository.findById(id)
            .orElseThrow(() -> new IllegalStateException("Comprobante no encontrado tras la marca"));
        log.info("Comprobante {} marcado ENVIO_INCIERTO antes del POST (intento {})", marcado.getClaveNumerica(),
            marcado.getIntentosEnvio());
        return new Marcado(marcado.getXmlComprobante(), marcado.getClaveNumerica());
    }

    private Transmision transmitir(Marcado marcado, Long configuracionId) {
        try {
            RespuestaHaciendaDTO respuesta = haciendaApi.enviarComprobante(marcado.xml(), marcado.clave(),
                configuracionId);
            return new Transmision(respuesta, null);
        } catch (RuntimeException e) {
            // El cliente clasifica en origen y no lanza; si algo lanza aquí, Hacienda pudo recibirlo
            log.error("Excepción no clasificada enviando {}: {}", marcado.clave(), e.getMessage(), e);
            return new Transmision(null, e);
        }
    }

    private void registrar(Long id, Transmision transmision) {
        ComprobanteElectronico comprobante = repository.findById(id)
            .orElseThrow(() -> new IllegalStateException("Comprobante no encontrado al registrar el envío"));
        if (comprobante.getEstado() != EstadoComprobante.ENVIO_INCIERTO) {
            // Entre la marca y el registro otro proceso adoptó un estado de Hacienda (p. ej. la consulta manual)
            log.warn("Comprobante {} pasó a {} antes de registrar el envío; se conserva", comprobante.getClaveNumerica(),
                comprobante.getEstado());
            return;
        }
        RespuestaHaciendaDTO respuesta = transmision.respuesta();
        if (respuesta == null) {
            comprobante.setUltimoError(ERROR_NO_CLASIFICADO);
            repository.save(comprobante);
            return;
        }
        ResultadoEnvioHacienda resultado = respuesta.getResultadoEnvio() != null
            ? respuesta.getResultadoEnvio()
            : ResultadoEnvioHacienda.ENVIO_INCIERTO;
        comprobante.setEstado(resultado.estado());
        comprobante.setCodigoRespuesta(respuesta.getCodigoHttp() != null ? String.valueOf(respuesta.getCodigoHttp()) : null);
        comprobante.setMensajeRespuesta(respuesta.getMensaje());
        comprobante.setFechaRespuesta(respuesta.getFechaRespuesta());
        comprobante.setUltimoError(resultado == ResultadoEnvioHacienda.RECIBIDO ? null : respuesta.getMensaje());
        repository.save(comprobante);
        log.info("Envío de {} clasificado como {} → {}", comprobante.getClaveNumerica(), resultado,
            comprobante.getEstado());
    }

    private ComprobanteElectronicoDTO leer(Long id) {
        TransactionTemplate lectura = new TransactionTemplate(nueva.getTransactionManager());
        lectura.setReadOnly(true);
        return lectura.execute(st -> repository.findById(id).map(mapper::toDTO)
            .orElseThrow(() -> new IllegalArgumentException("Comprobante no encontrado")));
    }

    @Override
    public void registrarFalloDeReintento(Long comprobanteId, String detalle) {
        nueva.executeWithoutResult(st -> repository.findById(comprobanteId).ifPresent(c -> {
            if (!TRANSMISIBLES.contains(c.getEstado())) {
                return; // otro proceso lo marcó o Hacienda lo tiene: no se pisa su estado
            }
            c.setUltimoError(detalle);
            c.incrementarIntentos();
            repository.save(c);
        }));
    }

    /**
     * Etapa e (ADR-043 §3): programa la consulta n del sondeo en el TaskScheduler, {@code esperaSondeo} después de
     * ahora. Sin TaskScheduler (app.scheduling.enabled=false) no se programa nada: el estado lo resuelven la consulta
     * manual o el job horario cuando vuelvan a estar activos.
     */
    void programarSondeo(Long id, int consulta) {
        TaskScheduler programador = scheduler.getIfAvailable();
        if (programador == null) {
            log.info("Sondeo de {} no programado: no hay TaskScheduler (app.scheduling.enabled=false)", id);
            return;
        }
        programador.schedule(() -> consultar(id, consulta), clock.instant().plus(esperaSondeo));
    }

    /** Una consulta del sondeo, en su propia transacción; se detiene en un estado terminal o en ENVIO_INCIERTO. */
    void consultar(Long id, int consulta) {
        EstadoComprobante estado = null;
        try {
            estado = comprobanteService.consultarYActualizarEstado(id).getEstado();
            log.info("Sondeo de {} ({}/{}): {}", id, consulta, maxConsultasSondeo, estado);
        } catch (RuntimeException e) {
            log.warn("Sondeo de {} ({}/{}) sin respuesta: {}", id, consulta, maxConsultasSondeo, e.getMessage());
        }
        if (estado != null && (estado.esTerminal() || estado == EstadoComprobante.ENVIO_INCIERTO)) {
            return;
        }
        if (consulta < maxConsultasSondeo) {
            programarSondeo(id, consulta + 1);
        } else {
            log.info("Sondeo de {} terminado sin estado final; sigue el job horario", id);
        }
    }

    private record Marcado(String xml, String clave) {
    }

    private record Transmision(RespuestaHaciendaDTO respuesta, RuntimeException excepcion) {
    }
}
