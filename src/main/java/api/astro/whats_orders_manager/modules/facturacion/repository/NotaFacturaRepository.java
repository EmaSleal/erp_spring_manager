package api.astro.whats_orders_manager.modules.facturacion.repository;

import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.NotaFactura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotaFacturaRepository extends JpaRepository<NotaFactura, Long>, JpaSpecificationExecutor<NotaFactura> {

    boolean existsByFactura_IdFacturaAndEstadoIn(Integer idFactura, Collection<EstadoNotaFactura> estados);

    /** T-047: ¿la factura tiene notas en esos estados con alguno de esos códigos de referencia? (parciales: 02 y 14). */
    boolean existsByFactura_IdFacturaAndEstadoInAndCodigoReferenciaIn(Integer idFactura,
                                                                      Collection<EstadoNotaFactura> estados,
                                                                      Collection<String> codigos);

    /** T-047: ¿la factura tiene notas en esos estados con un código distinto de los dados? (no parciales: 01, 13...). */
    boolean existsByFactura_IdFacturaAndEstadoInAndCodigoReferenciaNotIn(Integer idFactura,
                                                                         Collection<EstadoNotaFactura> estados,
                                                                         Collection<String> codigos);

    /**
     * T-047 (ADR-051): total acreditado por línea de factura por las notas en esos estados (APLICADA y EN_TRAMITE para
     * los topes). Cada fila: {idLineaFactura (Integer), total acreditado (BigDecimal)}.
     */
    @org.springframework.data.jpa.repository.Query("SELECT l.lineaFactura.idLineaFactura, SUM(l.total) "
        + "FROM LineaNotaFactura l WHERE l.nota.factura.idFactura = :idFactura AND l.nota.estado IN :estados "
        + "AND l.lineaFactura IS NOT NULL GROUP BY l.lineaFactura.idLineaFactura")
    List<Object[]> acreditadoPorLinea(@org.springframework.data.repository.query.Param("idFactura") Integer idFactura,
                                      @org.springframework.data.repository.query.Param("estados")
                                      Collection<EstadoNotaFactura> estados);

    /** T-047: suma de los totales de las notas de la factura en esos estados (0 si no hay). */
    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(n.total), 0) FROM NotaFactura n "
        + "WHERE n.factura.idFactura = :idFactura AND n.estado IN :estados")
    BigDecimal sumaTotalPorFactura(@org.springframework.data.repository.query.Param("idFactura") Integer idFactura,
                                   @org.springframework.data.repository.query.Param("estados")
                                   Collection<EstadoNotaFactura> estados);

    List<NotaFactura> findByFactura_IdFacturaOrderByIdAsc(Integer idFactura);

    Optional<NotaFactura> findFirstByFactura_IdFacturaAndEstado(Integer idFactura, EstadoNotaFactura estado);

    /**
     * T-043 (ADR-048): notas en un estado, para el panel de Facturación. Con {@code ambiente}, solo las que tienen
     * comprobante vigente de ese ambiente (el ambiente de una nota es el de su comprobante, T-044); con null, todas.
     */
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(n) FROM NotaFactura n WHERE n.estado = :estado "
        + "AND (:ambiente IS NULL OR EXISTS (SELECT 1 FROM ComprobanteElectronico c WHERE c.notaFactura = n "
        + "AND c.vigente = true AND c.ambiente = :ambiente))")
    long contarPorEstado(@org.springframework.data.repository.query.Param("estado") EstadoNotaFactura estado,
                         @org.springframework.data.repository.query.Param("ambiente")
                         api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda ambiente);
}
