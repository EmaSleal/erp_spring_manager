package api.astro.whats_orders_manager.services;

import api.astro.whats_orders_manager.models.Usuario;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface UsuarioService {
    List<Usuario> findAll();
    Optional<Usuario> findById(Integer id);
    Usuario save(Usuario usuario);
    void deleteById(Integer id);
    Optional<Usuario> findByTelefono(String telefono);
    Optional<Usuario> findByEmail(String email);
}