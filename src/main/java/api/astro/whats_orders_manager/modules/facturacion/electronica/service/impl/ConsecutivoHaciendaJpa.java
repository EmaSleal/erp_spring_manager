package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConsecutivoHacienda;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consecutivo con bloqueo pesimista de la fila de configuracion_hacienda (P-089, ADR-043 §1a).
 *
 * <p>Se toma la instancia gestionada de la fila (la que cargó {@code obtenerConfiguracionActiva()} en la misma
 * transacción, o se busca) y se refresca con {@code PESSIMISTIC_WRITE}: se emite {@code SELECT ... FOR UPDATE} y se
 * relee el valor de la BD. Un {@code find} con bloqueo sobre una entidad ya cargada bloquearía la fila pero conservaría
 * el valor leído antes del bloqueo. El incremento lo escribe el flush de la transacción del llamador; el bloqueo dura hasta su commit, de modo
 * que otra emisión espera y luego lee el valor ya incrementado.
 */
@Component
@Slf4j
public class ConsecutivoHaciendaJpa implements ConsecutivoHacienda {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Long siguiente(ConfiguracionHacienda config, String codigoTipo) {
        // La instancia gestionada de esa fila (la del llamador, si ya la cargó), releída con SELECT ... FOR UPDATE
        ConfiguracionHacienda bloqueada = entityManager.contains(config)
            ? config
            : entityManager.find(ConfiguracionHacienda.class, config.getId());
        if (bloqueada == null) {
            throw new IllegalStateException("La configuración de Hacienda " + config.getId() + " no existe");
        }
        entityManager.refresh(bloqueada, LockModeType.PESSIMISTIC_WRITE);
        Long consecutivo = bloqueada.siguienteConsecutivo(codigoTipo);
        log.info("Consecutivo {} del tipo {} tomado con bloqueo de la configuración {}", consecutivo, codigoTipo,
            bloqueada.getId());
        return consecutivo;
    }
}
