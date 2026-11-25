package api.astro.whats_orders_manager.modules.regimen.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Responde si el sistema tiene documentos fiscales con fecha igual o posterior a un día (T-056, criterio 3): un tramo
 * futuro solo se puede crear, editar o borrar mientras ningún documento caiga en él. Cuenta también los anulados,
 * porque siguen siendo documentos del período.
 *
 * <ul>
 *   <li>Factura: la fecha de creación (la de emisión; la factura no tiene otra columna) o la fecha contable.</li>
 *   <li>Nota de crédito o débito: la fecha de emisión o la fecha contable.</li>
 *   <li>Cuenta por pagar: la fecha del comprobante del proveedor, o la de registro si no tiene comprobante.</li>
 * </ul>
 */
@Component
public class ConsultaDocumentosPorFecha {

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public boolean existenDesde(LocalDate desde) {
        LocalDateTime inicio = desde.atStartOfDay();
        return cuenta("select count(f) from Factura f where f.createDate >= :inicio or f.fechaContable >= :desde", desde, inicio)
            || cuenta("select count(n) from NotaFactura n where n.fechaEmision >= :inicio or n.fechaContable >= :desde", desde, inicio)
            || cuenta("select count(c) from CuentaPorPagar c where coalesce(c.fechaComprobante, c.fechaEmision) >= :desde", desde, null);
    }

    private boolean cuenta(String jpql, LocalDate desde, LocalDateTime inicio) {
        TypedQuery<Long> query = em.createQuery(jpql, Long.class).setParameter("desde", desde);
        if (inicio != null) {
            query.setParameter("inicio", inicio);
        }
        return query.getSingleResult() > 0;
    }
}
