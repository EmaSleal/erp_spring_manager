package api.astro.whats_orders_manager.modules.facturacion.repository;

import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.NotaFactura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotaFacturaRepository extends JpaRepository<NotaFactura, Long> {

    boolean existsByFactura_IdFacturaAndEstadoIn(Integer idFactura, Collection<EstadoNotaFactura> estados);

    List<NotaFactura> findByFactura_IdFacturaOrderByIdAsc(Integer idFactura);

    Optional<NotaFactura> findFirstByFactura_IdFacturaAndEstado(Integer idFactura, EstadoNotaFactura estado);
}
