package api.astro.whats_orders_manager.modules.facturacion.scheduler;

import api.astro.whats_orders_manager.modules.facturacion.service.TipoCambioSyncService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T-008 criterio 6 — el job diario se registra según tipo-cambio.sync.enabled
 * y con el cron/zona configurables (por defecto 06:15 America/Costa_Rica).
 */
@DisplayName("TipoCambioSyncScheduler — habilitación y programación")
class TipoCambioSyncSchedulerTest {

    @Configuration
    @EnableScheduling
    static class Config {
        @Bean
        TipoCambioSyncService tipoCambioSyncService() {
            return mock(TipoCambioSyncService.class);
        }
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Config.class, TipoCambioSyncScheduler.class);

    @Test
    @DisplayName("con tipo-cambio.sync.enabled=false el job no se registra")
    void deshabilitado_noRegistraJob() {
        runner.withPropertyValues("tipo-cambio.sync.enabled=false").run(ctx -> {
            assertThat(ctx).doesNotHaveBean(TipoCambioSyncScheduler.class);
            assertThat(cronTasks(ctx.getBean(ScheduledTaskHolder.class).getScheduledTasks())).isEmpty();
        });
    }

    @Test
    @DisplayName("por defecto el job se registra a las 06:15 todos los días")
    void porDefecto_registraJobCronDiario() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(TipoCambioSyncScheduler.class);
            assertThat(cronTasks(ctx.getBean(ScheduledTaskHolder.class).getScheduledTasks()))
                    .extracting(CronTask::getExpression).containsExactly("0 15 6 * * *");
        });
    }

    @Test
    @DisplayName("el cron es configurable por propiedad")
    void cronConfigurable() {
        runner.withPropertyValues("tipo-cambio.sync.enabled=true", "tipo-cambio.sync.cron=0 0 7 * * *").run(ctx ->
                assertThat(cronTasks(ctx.getBean(ScheduledTaskHolder.class).getScheduledTasks()))
                        .extracting(CronTask::getExpression).containsExactly("0 0 7 * * *"));
    }

    @Test
    @DisplayName("el job nunca propaga excepciones")
    void ejecutar_noPropagaExcepciones() {
        TipoCambioSyncService service = mock(TipoCambioSyncService.class);
        when(service.sincronizar()).thenThrow(new IllegalStateException("fallo inesperado"));

        assertThatCode(() -> new TipoCambioSyncScheduler(service).ejecutar()).doesNotThrowAnyException();
        verify(service).sincronizar();
    }

    private static java.util.List<CronTask> cronTasks(Set<ScheduledTask> tasks) {
        return tasks.stream()
                .map(ScheduledTask::getTask)
                .filter(CronTask.class::isInstance)
                .map(CronTask.class::cast)
                .toList();
    }
}
