package api.astro.whats_orders_manager.repositories;

import api.astro.whats_orders_manager.models.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {}
