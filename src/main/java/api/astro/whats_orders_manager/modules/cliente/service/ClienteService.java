package api.astro.whats_orders_manager.modules.cliente.service;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface ClienteService {
    List<Cliente> findAll();
    Page<Cliente> findAll(Pageable pageable);
    Optional<Cliente> findById(Integer id);
    Cliente save(Cliente cliente);
    void deleteById(Integer id);
    long count();
    
}

