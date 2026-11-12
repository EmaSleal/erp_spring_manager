package api.astro.whats_orders_manager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Reloj del sistema como bean, para que las reglas que dependen de la hora (p. ej. los 15 minutos antes de
 * verificar un envío incierto con Hacienda, ADR-030) se prueben con un reloj fijo.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
