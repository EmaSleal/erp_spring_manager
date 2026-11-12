package api.astro.whats_orders_manager.config;

import api.astro.whats_orders_manager.WhatsOrdersManagerApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-031 (auditoría 7d #14) — @EnableScheduling vive solo en la clase de la aplicación; CacheConfig ya no lo
 * duplica y sus métodos @Scheduled siguen registrándose con la anotación de la aplicación.
 */
@DisplayName("T-031 — @EnableScheduling declarado una sola vez")
class EnableSchedulingUnicoTest {

    @Test
    @DisplayName("está en la clase de la aplicación y no en CacheConfig")
    void soloEnLaAplicacion() {
        assertThat(WhatsOrdersManagerApplication.class.isAnnotationPresent(EnableScheduling.class)).isTrue();
        assertThat(CacheConfig.class.isAnnotationPresent(EnableScheduling.class)).isFalse();
    }

    @Test
    @DisplayName("los métodos @Scheduled de CacheConfig siguen declarados")
    void scheduledDeCacheConfigIntacto() {
        assertThat(Arrays.stream(CacheConfig.class.getDeclaredMethods())
                .anyMatch(m -> m.isAnnotationPresent(Scheduled.class))).isTrue();
    }
}
