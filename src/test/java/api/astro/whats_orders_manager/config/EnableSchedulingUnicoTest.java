package api.astro.whats_orders_manager.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-031 (auditoría 7d #14) y T-036 (criterio 12) — @EnableScheduling vive en una única configuración
 * (SchedulingConfig), condicionada por app.scheduling.enabled; ni la clase de la aplicación ni CacheConfig lo
 * declaran, y los métodos @Scheduled de CacheConfig siguen declarados.
 */
@DisplayName("T-036 — @EnableScheduling declarado una sola vez y condicionado")
class EnableSchedulingUnicoTest {

    @Test
    @DisplayName("ninguna clase de src/main lo declara salvo SchedulingConfig")
    void soloEnSchedulingConfig() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(org.springframework.beans.factory.annotation.AnnotatedBeanDefinition d) {
                return true;
            }
        };
        scanner.addIncludeFilter(new AnnotationTypeFilter(EnableScheduling.class));
        List<String> clases = scanner.findCandidateComponents("api.astro.whats_orders_manager").stream()
                .map(BeanDefinition::getBeanClassName).filter(n -> !n.contains("Test")).toList();

        assertThat(clases).containsExactly(SchedulingConfig.class.getName());
        assertThat(CacheConfig.class.isAnnotationPresent(EnableScheduling.class)).isFalse();
    }

    @Test
    @DisplayName("SchedulingConfig está condicionada por app.scheduling.enabled (true por defecto)")
    void condicionadaPorLaPropiedad() {
        ConditionalOnProperty c = SchedulingConfig.class.getAnnotation(ConditionalOnProperty.class);
        assertThat(c).isNotNull();
        assertThat(c.prefix()).isEqualTo("app.scheduling");
        assertThat(c.name()).containsExactly("enabled");
        assertThat(c.havingValue()).isEqualTo("true");
        assertThat(c.matchIfMissing()).isTrue();
    }

    @Test
    @DisplayName("los métodos @Scheduled de CacheConfig siguen declarados")
    void scheduledDeCacheConfigIntacto() {
        assertThat(Arrays.stream(CacheConfig.class.getDeclaredMethods())
                .anyMatch(m -> m.isAnnotationPresent(Scheduled.class))).isTrue();
    }
}
