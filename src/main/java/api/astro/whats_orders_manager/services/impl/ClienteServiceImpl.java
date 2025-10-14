package api.astro.whats_orders_manager.services.impl;

import api.astro.whats_orders_manager.models.Cliente;
import api.astro.whats_orders_manager.repositories.ClienteRepository;
import api.astro.whats_orders_manager.services.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Optional;

@Service
public class ClienteServiceImpl implements ClienteService {
    @Autowired
    private ClienteRepository clienteRepository;

    @Override
    public List<Cliente> findAll() { return clienteRepository.findAll(); }

    @Override
    public Optional<Cliente> findById(Integer id) { return clienteRepository.findById(id); }

    @Override
    public Cliente save(Cliente cliente) { return clienteRepository.save(cliente); }

    @Override
    public void deleteById(Integer id) { clienteRepository.deleteById(id); }

    @Override
    public long count() { return clienteRepository.count(); }
}
