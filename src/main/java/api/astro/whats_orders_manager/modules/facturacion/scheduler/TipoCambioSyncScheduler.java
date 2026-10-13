package api.astro.whats_orders_manager.modules.facturacion.scheduler;

import api.astro.whats_orders_manager.modules.facturacion.service.TipoCambioSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job diario de sincronización del tipo de cambio (T-008).
 *
 * Propiedades:
 * <ul>
 *   <li>{@code tipo-cambio.sync.enabled} (por defecto true): con false el job no se registra.</li>
 *   <li>{@code tipo-cambio.sync.cron} (por defecto "0 15 6 * * *": todos los días 06:15).</li>
 *   <li>{@code tipo-cambio.sync.zone} (por defecto America/Costa_Rica).</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tipo-cambio.sync", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TipoCambioSyncScheduler {

    private final TipoCambioSyncService tipoCambioSyncService;

    @Scheduled(cron = "${tipo-cambio.sync.cron:0 15 6 * * *}", zone = "${tipo-cambio.sync.zone:America/Costa_Rica}")
    public void ejecutar() {
        try {
            tipoCambioSyncService.sincronizar();
        } catch (RuntimeException e) {
            // Nunca propagar fuera del job
            log.warn("Fallo inesperado en la sincronización diaria del tipo de cambio: {}", e.getMessage(), e);
        }
    }
}
