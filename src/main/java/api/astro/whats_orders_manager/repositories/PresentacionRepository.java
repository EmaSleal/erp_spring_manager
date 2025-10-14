package api.astro.whats_orders_manager.repositories;

import api.astro.whats_orders_manager.models.Presentacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PresentacionRepository extends JpaRepository<Presentacion, Integer> {}
