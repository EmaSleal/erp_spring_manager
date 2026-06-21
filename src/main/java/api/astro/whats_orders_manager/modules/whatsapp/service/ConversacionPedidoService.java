package api.astro.whats_orders_manager.modules.whatsapp.service;

import api.astro.whats_orders_manager.modules.whatsapp.model.ConversacionPedido;

import java.util.Optional;

public interface ConversacionPedidoService {
    Optional<ConversacionPedido> findByTelefonoCliente(String telefonoCliente);
    ConversacionPedido upsert(String telefonoCliente, ConversacionPedido datos);
    boolean deleteByTelefonoCliente(String telefonoCliente);
}
