package api.astro.whats_orders_manager.modules.facturacion.electronica.service;

import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.ConsultaManualDemasiadoProntoException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Límite de "Consultar estado en Hacienda" (ADR-031 §4): 1 consulta manual cada 30 s por comprobante, con el reloj
 * inyectado. Se guarda en memoria: la aplicación corre en una sola instancia y un reinicio solo habilita antes una
 * consulta de solo lectura.
 */
@Component
public class LimiteConsultaManualHacienda {

    /** Intervalo mínimo entre consultas manuales del mismo comprobante. */
    public static final Duration INTERVALO = Duration.ofSeconds(30);

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Clock clock;
    private final Map<Long, Instant> ultimaConsulta = new ConcurrentHashMap<>();

    public LimiteConsultaManualHacienda(Clock clock) {
        this.clock = clock;
    }

    /**
     * Registra una consulta manual del comprobante si ya pasaron 30 s desde la anterior.
     *
     * @throws ConsultaManualDemasiadoProntoException (429) si no pasaron, con la hora a partir de la cual se puede
     */
    public void exigirYRegistrar(Long idComprobante) {
        Instant ahora = clock.instant();
        Instant[] rechazadaHasta = new Instant[1];
        ultimaConsulta.compute(idComprobante, (id, anterior) -> {
            if (anterior != null && ahora.isBefore(anterior.plus(INTERVALO))) {
                rechazadaHasta[0] = anterior.plus(INTERVALO);
                return anterior;
            }
            return ahora;
        });
        if (rechazadaHasta[0] != null) {
            LocalDateTime desde = LocalDateTime.ofInstant(rechazadaHasta[0], clock.getZone());
            throw new ConsultaManualDemasiadoProntoException("Ya se consultó el estado de este comprobante hace menos de "
                    + INTERVALO.toSeconds() + " segundos. Podrá consultar de nuevo a partir de las "
                    + desde.format(HORA) + ".", desde);
        }
    }
}
