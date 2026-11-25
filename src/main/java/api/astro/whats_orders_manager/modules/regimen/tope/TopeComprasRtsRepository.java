package api.astro.whats_orders_manager.modules.regimen.tope;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopeComprasRtsRepository extends JpaRepository<TopeComprasRts, Long> {

    Optional<TopeComprasRts> findByAnio(Integer anio);

    List<TopeComprasRts> findAllByOrderByAnioDesc();
}
