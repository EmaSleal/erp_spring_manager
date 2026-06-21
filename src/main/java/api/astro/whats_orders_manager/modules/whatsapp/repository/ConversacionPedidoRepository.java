package api.astro.whats_orders_manager.modules.whatsapp.repository;

import api.astro.whats_orders_manager.modules.whatsapp.model.ConversacionPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversacionPedidoRepository extends JpaRepository<ConversacionPedido, Integer> {
    Optional<ConversacionPedido> findByTelefonoCliente(String telefonoCliente);
    boolean existsByTelefonoCliente(String telefonoCliente);
    void deleteByTelefonoCliente(String telefonoCliente);
}
