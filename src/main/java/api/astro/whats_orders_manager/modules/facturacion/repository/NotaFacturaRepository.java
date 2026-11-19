package api.astro.whats_orders_manager.modules.facturacion.repository;

import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.NotaFactura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotaFacturaRepository extends JpaRepository<NotaFactura, Long>, JpaSpecificationExecutor<NotaFactura> {

    boolean existsByFactura_IdFacturaAndEstadoIn(Integer idFactura, Collection<EstadoNotaFactura> estados);

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
