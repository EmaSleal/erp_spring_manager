package api.astro.whats_orders_manager.modules.whatsapp.service;

import api.astro.whats_orders_manager.modules.whatsapp.model.ConversacionPedido;
import api.astro.whats_orders_manager.modules.whatsapp.repository.ConversacionPedidoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversacionPedidoServiceImpl implements ConversacionPedidoService {

    private final ConversacionPedidoRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<ConversacionPedido> findByTelefonoVendedor(String telefonoVendedor) {
        return repository.findByTelefonoVendedor(telefonoVendedor);
    }

    @Override
    @Transactional
    public ConversacionPedido upsert(String telefonoVendedor, ConversacionPedido datos) {
        validarLineasAcumuladas(datos.getLineasAcumuladas());

        ConversacionPedido conversacion = repository.findByTelefonoVendedor(telefonoVendedor)
                .orElseGet(() -> {
                    ConversacionPedido nueva = new ConversacionPedido();
                    nueva.setTelefonoVendedor(telefonoVendedor);
                    return nueva;
                });

        if (datos.getIdVendedor() != null) conversacion.setIdVendedor(datos.getIdVendedor());
        if (datos.getEstadoConversacion() != null) conversacion.setEstadoConversacion(datos.getEstadoConversacion());
        if (datos.getLineasAcumuladas() != null) conversacion.setLineasAcumuladas(datos.getLineasAcumuladas());
        if (datos.getIdClientePropuesto() != null) conversacion.setIdClientePropuesto(datos.getIdClientePropuesto());
        if (datos.getEsperandoConfirmacion() != null) conversacion.setEsperandoConfirmacion(datos.getEsperandoConfirmacion());
        if (datos.getUltimoMensajeBot() != null) conversacion.setUltimoMensajeBot(datos.getUltimoMensajeBot());

        return repository.save(conversacion);
    }

    @Override
    @Transactional
    public boolean deleteByTelefonoVendedor(String telefonoVendedor) {
        if (!repository.existsByTelefonoVendedor(telefonoVendedor)) {
            return false;
        }
        repository.deleteByTelefonoVendedor(telefonoVendedor);
        return true;
    }

    private void validarLineasAcumuladas(String json) {
        if (json == null || json.isBlank()) return;
        try {
            objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("lineasAcumuladas no es un JSON válido: " + e.getMessage());
        }
    }
}
