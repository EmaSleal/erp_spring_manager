package api.astro.whats_orders_manager.modules.regimen;

import api.astro.whats_orders_manager.modules.regimen.tope.TopeComprasRtsService;
import api.astro.whats_orders_manager.modules.regimen.verificacion.VerificacionRegimenScheduler;
import api.astro.whats_orders_manager.modules.regimen.verificacion.VerificacionRegimenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * T-056 (criterios 5 y 7): el job diario verifica con Hacienda y evalúa el tope; a las 06:30 de Costa Rica por defecto;
 * un fallo de una tarea no impide la otra ni escapa del job. Que respete APP_SCHEDULING_ENABLED lo garantiza
 * SchedulingConfig (sin @EnableScheduling no se procesa ningún @Scheduled).
 */
@DisplayName("T-056 — job diario de régimen")
class VerificacionRegimenSchedulerTest {

    @Test
    @DisplayName("ejecutar() verifica y evalúa el tope del año actual")
    void ejecutaAmbasTareas() {
        VerificacionRegimenService verificacion = mock(VerificacionRegimenService.class);
        TopeComprasRtsService tope = mock(TopeComprasRtsService.class);
        new VerificacionRegimenScheduler(verificacion, tope).ejecutar();
        verify(verificacion).verificar();
        verify(tope).evaluarAnioActual();
    }

    @Test
    @DisplayName("si la verificación falla, el tope se evalúa igual y nada escapa")
    void unFalloNoDetieneLaOtra() {
        VerificacionRegimenService verificacion = mock(VerificacionRegimenService.class);
        TopeComprasRtsService tope = mock(TopeComprasRtsService.class);
        doThrow(new IllegalStateException("x")).when(verificacion).verificar();
        assertThatCode(() -> new VerificacionRegimenScheduler(verificacion, tope).ejecutar()).doesNotThrowAnyException();
        verify(tope).evaluarAnioActual();

        doThrow(new IllegalStateException("y")).when(tope).evaluarAnioActual();
        assertThatCode(() -> new VerificacionRegimenScheduler(verificacion, tope).ejecutar()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("cron por defecto 06:30 y zona de Costa Rica, ambos por propiedad")
    void cronPorDefecto() throws Exception {
        Scheduled s = VerificacionRegimenScheduler.class.getMethod("ejecutar").getAnnotation(Scheduled.class);
        assertThat(s.cron()).isEqualTo("${regimen.verificacion.cron:0 30 6 * * *}");
        assertThat(s.zone()).isEqualTo("${regimen.verificacion.zone:America/Costa_Rica}");
    }
}
