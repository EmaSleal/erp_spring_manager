package api.astro.whats_orders_manager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import api.astro.whats_orders_manager.modules.facturacion.electronica.util.HoraFiscal;

import java.time.Clock;

/**
 * Reloj de la aplicación como bean, para que las reglas que dependen de la hora (p. ej. los 15 minutos antes de
 * verificar un envío incierto con Hacienda, ADR-030) se prueben con un reloj fijo.
 *
 * Está en America/Costa_Rica (T-050, ADR-060): «hoy», «mes» y «vencido» son los del día de Costa Rica y salen de este
 * reloj, no del de la base de datos ni de la zona predeterminada de la JVM.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.system(HoraFiscal.ZONA);
    }
}
