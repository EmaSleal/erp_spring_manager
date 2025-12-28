package api.astro.whats_orders_manager.services;

import api.astro.whats_orders_manager.models.Cliente;
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
    
    /**
     * Crea o actualiza un cliente junto con su usuario asociado
     * Si el usuario ya existe (por teléfono), lo actualiza
     * Si no existe, crea un nuevo usuario
     * 
     * @param cliente Cliente a guardar (debe incluir usuario con teléfono)
     * @return Cliente guardado con su usuario asociado
     * @throws IllegalArgumentException si el cliente o el usuario están incompletos
     */
    Cliente guardarClienteConUsuario(Cliente cliente);
}

