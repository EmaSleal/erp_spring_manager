package api.astro.whats_orders_manager.modules.regimen.verificacion;

import api.astro.whats_orders_manager.modules.regimen.tope.TopeComprasRtsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job diario de régimen (T-056): verifica el régimen con Hacienda y evalúa el tope de compras del RTS del año. Corre a las
 * 06:30 de Costa Rica por defecto ({@code regimen.verificacion.cron} y {@code regimen.verificacion.zone}). Respeta
 * APP_SCHEDULING_ENABLED: sin @EnableScheduling (SchedulingConfig) ningún @Scheduled se ejecuta. Una tarea que falla no
 * impide la otra y nada escapa del job.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VerificacionRegimenScheduler {

    private final VerificacionRegimenService verificacionService;
    private final TopeComprasRtsService topeService;

    @Scheduled(cron = "${regimen.verificacion.cron:0 30 6 * * *}", zone = "${regimen.verificacion.zone:America/Costa_Rica}")
    public void ejecutar() {
        try {
            verificacionService.verificar();
        } catch (RuntimeException e) {
            log.warn("Fallo inesperado en la verificación diaria del régimen: {}", e.getMessage(), e);
        }
        try {
            topeService.evaluarAnioActual();
        } catch (RuntimeException e) {
            log.warn("Fallo inesperado en la evaluación diaria del tope de compras del RTS: {}", e.getMessage(), e);
        }
    }
}
