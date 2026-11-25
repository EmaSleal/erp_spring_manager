package api.astro.whats_orders_manager.modules.regimen.repository;

import api.astro.whats_orders_manager.modules.regimen.model.RegimenTributario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegimenTributarioRepository extends JpaRepository<RegimenTributario, Long> {

    /** Todos los tramos, del más antiguo al más reciente. */
    List<RegimenTributario> findAllByOrderByVigenteDesdeAsc();
}
