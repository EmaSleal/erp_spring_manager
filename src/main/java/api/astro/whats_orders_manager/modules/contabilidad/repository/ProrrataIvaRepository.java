package api.astro.whats_orders_manager.modules.contabilidad.repository;

import api.astro.whats_orders_manager.modules.contabilidad.model.ProrrataIva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProrrataIvaRepository extends JpaRepository<ProrrataIva, Long> {

    Optional<ProrrataIva> findByAnio(Integer anio);

    List<ProrrataIva> findAllByOrderByAnioDesc();
}
