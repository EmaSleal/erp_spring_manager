package api.astro.whats_orders_manager.modules.facturacion.correo.scheduled;

import api.astro.whats_orders_manager.modules.facturacion.correo.client.MensajeCorreo;
import api.astro.whats_orders_manager.modules.facturacion.correo.client.ProveedorCorreo;
import api.astro.whats_orders_manager.modules.facturacion.correo.client.ResultadoEnvioCorreo;
import api.astro.whats_orders_manager.modules.facturacion.correo.config.CorreoProperties;
import api.astro.whats_orders_manager.modules.facturacion.correo.model.EntregaComprobante;
import api.astro.whats_orders_manager.modules.facturacion.correo.model.EstadoEntrega;
import api.astro.whats_orders_manager.modules.facturacion.correo.repository.EntregaComprobanteRepository;
import api.astro.whats_orders_manager.modules.facturacion.correo.service.ArmadorMensajeCorreo;
import api.astro.whats_orders_manager.modules.facturacion.correo.service.ArmadorMensajeCorreo.Preparado;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * T-055 (ADR-070): envía por correo las entregas PENDIENTE vencidas. Corre solo con app.scheduling.enabled (como el
 * resto de los jobs). Cada entrega se reclama con un UPDATE condicional (sin duplicar el envío aunque se superpongan dos
 * corridas o dos hilos), se arma en una transacción corta de lectura, se envía FUERA de toda transacción y el resultado
 * se registra en otra transacción corta (ADR-043).
 *
 * <ul>
 *   <li>Envío deshabilitado o proveedor sin configurar: no hace nada (no consume intentos).</li>
 *   <li>Éxito → ENVIADO con el id del proveedor. Reintentable → espera creciente (espera-inicial × 2^(n-1)); al llegar al
 *       máximo de intentos, FALLIDO. Definitivo → FALLIDO.</li>
 *   <li>429 del proveedor o tope diario alcanzado (ENVIADO del día en la zona del Clock) → las vencidas pasan al día
 *       siguiente sin consumir intentos.</li>
 * </ul>
 */
@Component
@Slf4j
public class EntregaComprobanteJob {

    private static final int MAX_ERROR = 500;

    private final EntregaComprobanteRepository repository;
    private final ArmadorMensajeCorreo armador;
    private final ProveedorCorreo proveedor;
    private final CorreoProperties props;
    private final Clock clock;
    private final TransactionTemplate escritura;
    private final TransactionTemplate lectura;

    public EntregaComprobanteJob(EntregaComprobanteRepository repository, ArmadorMensajeCorreo armador,
                                 ProveedorCorreo proveedor, CorreoProperties props, Clock clock,
                                 PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.armador = armador;
        this.proveedor = proveedor;
        this.props = props;
        this.clock = clock;
        this.escritura = new TransactionTemplate(transactionManager);
        this.lectura = new TransactionTemplate(transactionManager);
        this.lectura.setReadOnly(true);
    }

    @Scheduled(fixedDelayString = "${correo.job.intervalo:PT1M}")
    public void ejecutar() {
        try {
            procesarPendientes();
        } catch (RuntimeException e) {
            log.error("Job de entregas de comprobantes por correo: {}", e.toString());
        }
    }

    /** Una corrida; pública para las pruebas y para invocarla a mano. */
    public void procesarPendientes() {
        if (!props.habilitado()) {
            return;
        }
        if (!proveedor.configurado()) {
            log.debug("Envío de comprobantes habilitado pero sin clave o remitente: las entregas siguen PENDIENTE");
            return;
        }
        LocalDate hoy = LocalDate.now(clock);
        LocalDateTime inicioHoy = hoy.atStartOfDay();
        LocalDateTime manana = hoy.plusDays(1).atStartOfDay();
        long enviadasHoy = lectura.execute(s -> repository.contarEnviadasEntre(EstadoEntrega.ENVIADO, inicioHoy, manana));
        if (enviadasHoy >= props.getTopeDiario()) {
            diferirVencidas(manana, "Tope diario de correos alcanzado");
            return;
        }
        LocalDateTime ahora = LocalDateTime.now(clock);
        List<Long> ids = lectura.execute(s -> repository.idsVencidas(EstadoEntrega.PENDIENTE, ahora,
            PageRequest.of(0, Math.max(1, props.getJob().getLote()))));
        for (Long id : ids) {
            if (enviadasHoy >= props.getTopeDiario()) {
                diferirVencidas(manana, "Tope diario de correos alcanzado");
                return;
            }
            if (!reclamar(id)) {
                continue; // otra corrida u otro hilo la tomó
            }
            ResultadoEnvioCorreo.Tipo tipo = procesar(id, manana);
            if (tipo == ResultadoEnvioCorreo.Tipo.EXITO) {
                enviadasHoy++;
            } else if (tipo == ResultadoEnvioCorreo.Tipo.LIMITE) {
                diferirVencidas(manana, "Límite del proveedor");
                return;
            }
        }
    }

    private void diferirVencidas(LocalDateTime manana, String motivo) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        int n = escritura.execute(s -> repository.diferirVencidas(EstadoEntrega.PENDIENTE, ahora, manana));
        if (n > 0) {
            log.info("{}: {} entrega(s) de comprobantes pasan a {} sin consumir intentos", motivo, n, manana);
        }
    }

    private boolean reclamar(Long id) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDateTime hasta = ahora.plus(props.getJob().getReserva());
        Integer filas = escritura.execute(s -> repository.reclamar(id, EstadoEntrega.PENDIENTE, ahora, hasta));
        return filas != null && filas == 1;
    }

    /** Arma, envía y registra una entrega ya reclamada. Devuelve el tipo de resultado (null si se omitió). */
    private ResultadoEnvioCorreo.Tipo procesar(Long id, LocalDateTime manana) {
        Preparado preparado;
        try {
            preparado = lectura.execute(s -> armador.armar(id));
        } catch (RuntimeException e) {
            log.warn("Entrega {}: no se pudo armar el correo: {}", id, e.toString());
            ResultadoEnvioCorreo r = ResultadoEnvioCorreo.reintentable("No se pudo armar el correo: " + e.getMessage());
            registrar(id, r, manana);
            return r.tipo();
        }
        switch (preparado.accion()) {
            case OMITIR_SANDBOX -> {
                omitir(id, EstadoEntrega.OMITIDO_SANDBOX);
                return null;
            }
            case OMITIR_SIN_CORREO -> {
                omitir(id, EstadoEntrega.OMITIDO_SIN_CORREO);
                return null;
            }
            default -> {
            }
        }
        MensajeCorreo mensaje = preparado.mensaje();
        ResultadoEnvioCorreo resultado;
        try {
            resultado = proveedor.enviar(mensaje);
        } catch (RuntimeException e) {
            resultado = ResultadoEnvioCorreo.reintentable("Fallo del proveedor de correo: " + e.getMessage());
        }
        registrar(id, resultado, manana);
        return resultado.tipo();
    }

    private void omitir(Long id, EstadoEntrega estado) {
        escritura.executeWithoutResult(s -> repository.findById(id).ifPresent(e -> {
            e.setEstado(estado);
            e.setProximoIntento(null);
            repository.save(e);
        }));
    }

    private void registrar(Long id, ResultadoEnvioCorreo r, LocalDateTime manana) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        escritura.executeWithoutResult(s -> repository.findById(id).ifPresent(e -> aplicar(e, r, ahora, manana)));
    }

    private void aplicar(EntregaComprobante e, ResultadoEnvioCorreo r, LocalDateTime ahora, LocalDateTime manana) {
        switch (r.tipo()) {
            case EXITO -> {
                e.setEstado(EstadoEntrega.ENVIADO);
                e.setIdProveedor(r.idProveedor());
                e.setFechaEnvio(ahora);
                e.setIntentos(e.getIntentos() + 1);
                e.setProximoIntento(null);
                e.setUltimoError(null);
            }
            case REINTENTABLE -> {
                int intentos = e.getIntentos() + 1;
                e.setIntentos(intentos);
                e.setUltimoError(error(r.detalle()));
                if (intentos >= props.getReintentos().getMaximo()) {
                    e.setEstado(EstadoEntrega.FALLIDO);
                    e.setProximoIntento(null);
                } else {
                    long factor = 1L << Math.min(intentos - 1, 20);
                    e.setProximoIntento(ahora.plus(props.getReintentos().getEsperaInicial().multipliedBy(factor)));
                }
            }
            case LIMITE -> {
                e.setProximoIntento(manana);
                e.setUltimoError(error(r.detalle()));
            }
            case DEFINITIVO -> {
                e.setEstado(EstadoEntrega.FALLIDO);
                e.setIntentos(e.getIntentos() + 1);
                e.setProximoIntento(null);
                e.setUltimoError(error(r.detalle()));
            }
        }
        repository.save(e);
    }

    /** Error para guardar: sin la clave del proveedor y acotado a 500 caracteres. */
    private String error(String detalle) {
        String texto = detalle == null ? "" : detalle;
        String clave = props.getResend().getApiKey();
        if (clave != null && !clave.isBlank()) {
            texto = texto.replace(clave.trim(), "***");
        }
        return texto.length() > MAX_ERROR ? texto.substring(0, MAX_ERROR) : texto;
    }
}
