package api.astro.whats_orders_manager.modules.whatsapp.service;

import api.astro.whats_orders_manager.modules.whatsapp.model.ConversacionPedido;

import java.util.Optional;

public interface ConversacionPedidoService {
    Optional<ConversacionPedido> findByTelefonoVendedor(String telefonoVendedor);
    ConversacionPedido upsert(String telefonoVendedor, ConversacionPedido datos);
    boolean deleteByTelefonoVendedor(String telefonoVendedor);
}
