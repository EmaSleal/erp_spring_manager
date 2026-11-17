package api.astro.whats_orders_manager.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Único lugar donde se habilita @Scheduled (T-036). Con {@code app.scheduling.enabled=false} (variable de entorno
 * APP_SCHEDULING_ENABLED=false) ningún método @Scheduled de la aplicación se ejecuta: útil en verificaciones locales
 * y smoke tests. Por defecto (propiedad ausente o true) los jobs corren como siempre.
 *
 * <p>T-037 (ADR-043 §3): el TaskScheduler {@code taskScheduler} lo usan los @Scheduled y el sondeo en segundo plano
 * tras un envío (EnvioHaciendaServiceImpl). Con la propiedad en false no existe, así que tampoco se programa el sondeo.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(prefix = "app.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {

    @Bean(name = "taskScheduler", destroyMethod = "shutdown")
    public ThreadPoolTaskScheduler taskScheduler(@Value("${app.scheduling.pool-size:2}") int poolSize) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(poolSize);
        scheduler.setThreadNamePrefix("tareas-");
        scheduler.setWaitForTasksToCompleteOnShutdown(false);
        return scheduler;
    }
}
