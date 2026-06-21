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
    public Optional<ConversacionPedido> findByTelefonoCliente(String telefonoCliente) {
        return repository.findByTelefonoCliente(telefonoCliente);
    }

    @Override
    @Transactional
    public ConversacionPedido upsert(String telefonoCliente, ConversacionPedido datos) {
        validarLineasAcumuladas(datos.getLineasAcumuladas());

        ConversacionPedido conversacion = repository.findByTelefonoCliente(telefonoCliente)
                .orElseGet(() -> {
                    ConversacionPedido nueva = new ConversacionPedido();
                    nueva.setTelefonoCliente(telefonoCliente);
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
    public boolean deleteByTelefonoCliente(String telefonoCliente) {
        if (!repository.existsByTelefonoCliente(telefonoCliente)) {
            return false;
        }
        repository.deleteByTelefonoCliente(telefonoCliente);
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
