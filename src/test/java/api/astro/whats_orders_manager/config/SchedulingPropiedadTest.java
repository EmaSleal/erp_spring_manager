package api.astro.whats_orders_manager.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.ScheduledTaskHolder;

import static org.assertj.core.api.Assertions.assertThat;

/** T-036 (criterio 11): app.scheduling.enabled=false no registra ninguna tarea programada; true (o ausente), sí. */
@DisplayName("T-036 — app.scheduling.enabled")
class SchedulingPropiedadTest {

    static class JobDePrueba {
        @Scheduled(fixedDelay = 3_600_000, initialDelay = 3_600_000)
        void tarea() {
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ConJob {
        @Bean
        JobDePrueba jobDePrueba() {
            return new JobDePrueba();
        }
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(SchedulingConfig.class, ConJob.class);

    @Test
    @DisplayName("false: no hay procesador de @Scheduled ni tareas registradas")
    void apagado() {
        runner.withPropertyValues("app.scheduling.enabled=false").run(ctx -> {
            assertThat(ctx).doesNotHaveBean(SchedulingConfig.class);
            assertThat(ctx).doesNotHaveBean(ScheduledAnnotationBeanPostProcessor.class);
            assertThat(ctx.getBeansOfType(ScheduledTaskHolder.class).values()
                    .stream().mapToInt(h -> h.getScheduledTasks().size()).sum()).isZero();
        });
    }

    @Test
    @DisplayName("true: la tarea @Scheduled queda registrada")
    void encendido() {
        runner.withPropertyValues("app.scheduling.enabled=true").run(ctx -> {
            assertThat(ctx).hasSingleBean(SchedulingConfig.class);
            assertThat(ctx.getBean(ScheduledAnnotationBeanPostProcessor.class).getScheduledTasks()).hasSize(1);
        });
    }

    @Test
    @DisplayName("sin la propiedad: por defecto encendido")
    void porDefecto() {
        runner.run(ctx -> assertThat(ctx.getBean(ScheduledAnnotationBeanPostProcessor.class)
                .getScheduledTasks()).hasSize(1));
    }
}
