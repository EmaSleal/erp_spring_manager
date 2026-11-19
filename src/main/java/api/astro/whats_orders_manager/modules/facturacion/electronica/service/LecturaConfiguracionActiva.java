package api.astro.whats_orders_manager.modules.facturacion.electronica.service;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.ConfiguracionHaciendaNoDisponibleException;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Lectura aislada de la configuración de Hacienda activa (T-041, reutilizada por T-044): una sola implementación para
 * el servicio de comprobantes y para {@link AmbienteComprobanteGuard}.
 */
public final class LecturaConfiguracionActiva {

    private LecturaConfiguracionActiva() {
    }

    /**
     * ADR-021: la única configuración de Hacienda activa, que debe pertenecer a la empresa indicada (sin empresa, no se
     * comprueba). Con gestor de transacciones se lee en su propia transacción (REQUIRES_NEW, solo lectura): si las
     * credenciales no se descifran, Hibernate marca esa transacción como rollback-only y la del llamador sigue sana.
     * Sin gestor (pruebas unitarias) se lee en la transacción del llamador.
     *
     * @throws ConfiguracionHaciendaNoDisponibleException si no se puede obtener una configuración válida
     */
    public static ConfiguracionHacienda leer(ConfiguracionHaciendaService configuracionService,
                                             PlatformTransactionManager transactionManager,
                                             ConfiguracionEmpresa empresa) {
        if (transactionManager == null) {
            return leerEnLaTransaccionActual(configuracionService, empresa);
        }
        TransactionTemplate lectura = new TransactionTemplate(transactionManager);
        lectura.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        lectura.setReadOnly(true);
        return lectura.execute(st -> leerEnLaTransaccionActual(configuracionService, empresa));
    }

    private static ConfiguracionHacienda leerEnLaTransaccionActual(ConfiguracionHaciendaService configuracionService,
                                                                   ConfiguracionEmpresa empresa) {
        ConfiguracionHacienda config;
        Integer empresaDeLaConfiguracion;
        try {
            config = configuracionService.obtenerConfiguracionActiva();
            // dentro de la sesión (relación LAZY); sin empresa que comprobar, no se toca
            empresaDeLaConfiguracion = empresa != null ? config.getEmpresa().getIdConfiguracion() : null;
        } catch (ConfiguracionHaciendaNoDisponibleException e) {
            throw e;
        } catch (RuntimeException e) {
            // Credenciales que no se descifran (CredencialCifradaConverter, T-021) u otra falla de lectura
            throw new ConfiguracionHaciendaNoDisponibleException(
                "No se pudo leer la configuración de Hacienda activa (" + causaRaiz(e) + ")",
                ConfiguracionHaciendaNoDisponibleException.MENSAJE_ILEGIBLE, e);
        }
        if (empresa != null && !empresaDeLaConfiguracion.equals(empresa.getIdConfiguracion())) {
            throw new ConfiguracionHaciendaNoDisponibleException("La configuración de Hacienda activa (id "
                + config.getId() + ") pertenece a la empresa " + empresaDeLaConfiguracion + ", no a la empresa "
                + empresa.getIdConfiguracion() + ".", ConfiguracionHaciendaNoDisponibleException.MENSAJE_NO_VALIDA);
        }
        return config;
    }

    private static String causaRaiz(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t.getClass().getSimpleName() + ": " + t.getMessage();
    }
}
