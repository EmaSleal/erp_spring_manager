package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import static org.mockito.Mockito.mock;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.AmbienteComprobanteGuard;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.mapper.ComprobanteElectronicoMapper;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ComprobanteElectronicoService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.HaciendaApiService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;

/**
 * Apoyo de pruebas unitarias del envío en etapas (T-037): arma un {@link EnvioHaciendaServiceImpl} con un gestor de
 * transacciones sin BD (las etapas abren y cierran "transacciones" de verdad para TransactionSynchronizationManager,
 * pero sin conexión) y simula sobre un repositorio de Mockito la carga con bloqueo y la marca condicional.
 */
public final class EnvioHaciendaDePrueba {

    private EnvioHaciendaDePrueba() {
    }

    /** Gestor de transacciones sin recursos: comienza, confirma y revierte sin BD. */
    public static final class SinBd extends AbstractPlatformTransactionManager {
        /** Marca de "hay una transacción en curso" para que REQUIRES_NEW y NOT_SUPPORTED suspendan la del llamador. */
        private static final ThreadLocal<Integer> ABIERTAS = ThreadLocal.withInitial(() -> 0);

        @Override
        protected Object doGetTransaction() {
            return new boolean[] {ABIERTAS.get() > 0};
        }

        @Override
        protected boolean isExistingTransaction(Object transaction) {
            return ((boolean[]) transaction)[0];
        }

        @Override
        protected void doCleanupAfterCompletion(Object transaction) {
            ABIERTAS.set(Math.max(0, ABIERTAS.get() - 1));
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            ABIERTAS.set(ABIERTAS.get() + 1);
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }

        @Override
        protected Object doSuspend(Object transaction) {
            Integer abiertas = ABIERTAS.get();
            ABIERTAS.set(0);
            return abiertas;
        }

        @Override
        protected void doResume(Object transaction, Object suspendedResources) {
            ABIERTAS.set((Integer) suspendedResources);
        }
    }

    /** Servicio de envío sin TaskScheduler (como con app.scheduling.enabled=false). */
    public static EnvioHaciendaServiceImpl crear(ComprobanteElectronicoRepository repository,
                                                 ComprobanteElectronicoService comprobanteService,
                                                 HaciendaApiService hacienda, Clock clock) {
        return crear(repository, comprobanteService, hacienda, clock, null);
    }

    public static EnvioHaciendaServiceImpl crear(ComprobanteElectronicoRepository repository,
                                                 ComprobanteElectronicoService comprobanteService,
                                                 HaciendaApiService hacienda, Clock clock, TaskScheduler scheduler) {
        return crear(repository, comprobanteService, hacienda, clock, scheduler, mock(AmbienteComprobanteGuard.class));
    }

    /** Con la regla de ambiente indicada (T-044). */
    public static EnvioHaciendaServiceImpl crear(ComprobanteElectronicoRepository repository,
                                                 ComprobanteElectronicoService comprobanteService,
                                                 HaciendaApiService hacienda, Clock clock, TaskScheduler scheduler,
                                                 AmbienteComprobanteGuard guard) {
        DefaultListableBeanFactory fabrica = new DefaultListableBeanFactory();
        if (scheduler != null) {
            fabrica.registerSingleton("taskScheduler", scheduler);
        }
        ObjectProvider<TaskScheduler> proveedor = fabrica.getBeanProvider(TaskScheduler.class);
        return new EnvioHaciendaServiceImpl(repository, comprobanteService, new ComprobanteElectronicoMapper(), hacienda,
            clock, guard, proveedor, new SinBd(), Duration.ofSeconds(30), 5);
    }

    /**
     * El repositorio simulado devuelve el comprobante en memoria para la carga con bloqueo y aplica la marca
     * condicional sobre él (1 fila si su estado es transmisible y es vigente o sin dato; 0 si no).
     */
    @SuppressWarnings("unchecked")
    public static void simularBloqueoYMarca(ComprobanteElectronicoRepository repository,
                                            Supplier<ComprobanteElectronico> comprobante) {
        lenient().when(repository.findByIdParaEnvio(anyLong()))
            .thenAnswer(inv -> Optional.ofNullable(comprobante.get()));
        lenient().when(repository.marcarEnvioEnCurso(anyLong(), any(), any(), any())).thenAnswer(inv -> {
            ComprobanteElectronico c = comprobante.get();
            Collection<EstadoComprobante> transmisibles = inv.getArgument(3);
            if (c == null || Boolean.FALSE.equals(c.getVigente()) || !transmisibles.contains(c.getEstado())) {
                return 0;
            }
            c.setEstado(inv.getArgument(2));
            c.setFechaEnvio(inv.<LocalDateTime>getArgument(1));
            c.setIntentosEnvio((c.getIntentosEnvio() == null ? 0 : c.getIntentosEnvio()) + 1);
            return 1;
        });
    }
}
