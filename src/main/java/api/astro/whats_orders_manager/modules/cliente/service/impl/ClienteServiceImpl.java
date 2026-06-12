package api.astro.whats_orders_manager.modules.cliente.service.impl;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.cliente.repository.ClienteRepository;
import api.astro.whats_orders_manager.modules.cliente.service.ClienteService;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClienteServiceImpl implements ClienteService {

    private static final String PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int PASSWORD_LENGTH = 10;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ClienteRepository clienteRepository;
    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<Cliente> findAll() { 
        return clienteRepository.findAll(); 
    }

    @Override
    public Page<Cliente> findAll(Pageable pageable) { 
        return clienteRepository.findAll(pageable); 
    }

    @Override
    public Optional<Cliente> findById(Integer id) { 
        return clienteRepository.findById(id); 
    }

    @Override
    public Cliente save(Cliente cliente) { 
        return clienteRepository.save(cliente); 
    }

    @Override
    public void deleteById(Integer id) { 
        clienteRepository.deleteById(id); 
    }

    @Override
    public long count() { 
        return clienteRepository.count(); 
    }

    /**
     * Guarda o actualiza un cliente junto con su usuario asociado
     * Implementa la lógica de negocio completa para la creación/actualización
     */
    @Override
    @Transactional
    public Cliente guardarClienteConUsuario(Cliente cliente) {
        log.info("Iniciando proceso de guardar cliente: {}", cliente.getNombre());
        
        // Validaciones
        validarCliente(cliente);
        
        String telefono = cliente.getUsuario().getTelefono();
        Optional<Usuario> usuarioExistente = usuarioService.findByTelefono(telefono);
        
        Usuario usuario;
        if (usuarioExistente.isEmpty()) {
            // Crear nuevo usuario
            usuario = crearNuevoUsuario(cliente);
            log.info("Usuario nuevo creado con teléfono: {}", telefono);
        } else {
            // Actualizar usuario existente
            usuario = actualizarUsuarioExistente(usuarioExistente.get(), cliente);
            log.info("Usuario existente actualizado: {}", telefono);
        }
        
        // Guardar o actualizar cliente
        cliente.setUsuario(usuario);
        Cliente clienteGuardado = clienteRepository.save(cliente);
        
        log.info("Cliente guardado exitosamente con ID: {}", clienteGuardado.getIdCliente());
        return clienteGuardado;
    }

    /**
     * Valida que el cliente tenga los datos mínimos necesarios
     */
    private void validarCliente(Cliente cliente) {
        if (cliente == null) {
            throw new IllegalArgumentException("El cliente no puede ser nulo");
        }
        
        if (cliente.getNombre() == null || cliente.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del cliente es obligatorio");
        }
        
        if (cliente.getUsuario() == null ||
            cliente.getUsuario().getTelefono() == null ||
            cliente.getUsuario().getTelefono().trim().isEmpty()) {
            throw new IllegalArgumentException("El teléfono del usuario es obligatorio");
        }

        String otrasSenas = cliente.getOtrasSenas();
        if (otrasSenas != null && !otrasSenas.isBlank() && otrasSenas.trim().length() < 5) {
            throw new IllegalArgumentException("Otras señas debe tener al menos 5 caracteres (requerido por Hacienda CR)");
        }
    }

    /**
     * Crea un nuevo usuario para el cliente
     */
    private Usuario crearNuevoUsuario(Cliente cliente) {
        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombre(cliente.getNombre());
        nuevoUsuario.setPassword(passwordEncoder.encode(generateRandomPassword()));
        nuevoUsuario.setRol("CLIENTE");
        nuevoUsuario.setTelefono(cliente.getUsuario().getTelefono());
        nuevoUsuario.setRequireCambioPassword(true);

        return usuarioService.save(nuevoUsuario);
    }

    private String generateRandomPassword() {
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            password.append(PASSWORD_CHARS.charAt(SECURE_RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return password.toString();
    }

    /**
     * Actualiza un usuario existente con los nuevos datos del cliente
     */
    private Usuario actualizarUsuarioExistente(Usuario usuario, Cliente cliente) {
        usuario.setNombre(cliente.getNombre());
        return usuarioService.save(usuario);
    }
}
