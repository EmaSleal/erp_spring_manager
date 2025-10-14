package api.astro.whats_orders_manager.services.impl;

import api.astro.whats_orders_manager.models.Usuario;
import api.astro.whats_orders_manager.repositories.UsuarioRepository;
import api.astro.whats_orders_manager.services.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioServiceImpl implements UsuarioService {
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public List<Usuario> findAll() { return usuarioRepository.findAll(); }

    @Override
    public Optional<Usuario> findById(Integer id) { return usuarioRepository.findById(id); }

    @Override
    public Usuario save(Usuario usuario) { return usuarioRepository.save(usuario); }

    @Override
    public void deleteById(Integer id) { usuarioRepository.deleteById(id); }

    @Override
    public Optional<Usuario> findByTelefono(String telefono) { return usuarioRepository.findByTelefono(telefono); }

    @Override
    public Optional<Usuario> findByEmail(String email) { return usuarioRepository.findByEmail(email); }
}