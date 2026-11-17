package api.astro.whats_orders_manager.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Único lugar donde se habilita @Scheduled (T-036). Con {@code app.scheduling.enabled=false} (variable de entorno
 * APP_SCHEDULING_ENABLED=false) ningún método @Scheduled de la aplicación se ejecuta: útil en verificaciones locales
 * y smoke tests. Por defecto (propiedad ausente o true) los jobs corren como siempre.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(prefix = "app.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
