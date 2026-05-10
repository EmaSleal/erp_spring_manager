package api.astro.whats_orders_manager.modules.seguridad.service.impl;

import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioRepository;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * USUARIO SERVICE IMPLEMENTATION
 * WhatsApp Orders Manager
 * ============================================================================
 * Implementación del servicio de gestión de usuarios.
 * 
 * @version 2.0 - Sprint 4
 * @since 22/12/2025
 * ============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {
    
    private final UsuarioRepository usuarioRepository;

    // ==================== CRUD BÁSICO ====================

    @Override
    public List<Usuario> findAll() {
        log.debug("Buscando todos los usuarios");
        return usuarioRepository.findAll();
    }

    @Override
    public Page<Usuario> findAll(Pageable pageable) {
        log.debug("Buscando usuarios con paginación: {}", pageable);
        return usuarioRepository.findAll(pageable);
    }

    @Override
    public Optional<Usuario> findById(Integer id) {
        log.debug("Buscando usuario por ID: {}", id);
        return usuarioRepository.findById(id);
    }

    @Override
    @Transactional
    public Usuario save(Usuario usuario) {
        log.info("Guardando usuario: {}", usuario.getNombre());
        return usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void deleteById(Integer id) {
        log.warn("Eliminando usuario con ID: {}", id);
        usuarioRepository.deleteById(id);
    }

    // ==================== BÚSQUEDAS ====================

    @Override
    public Optional<Usuario> findByTelefono(String telefono) {
        log.debug("Buscando usuario por teléfono: {}", telefono);
        return usuarioRepository.findByTelefono(telefono);
    }

    @Override
    public Optional<Usuario> findByEmail(String email) {
        log.debug("Buscando usuario por email: {}", email);
        return usuarioRepository.findByEmail(email);
    }

    @Override
    public List<Usuario> findByRol(String rol) {
        log.debug("Buscando usuarios por rol: {}", rol);
        return usuarioRepository.findByRol(rol);
    }

    @Override
    public List<Usuario> findByActivo(Boolean activo) {
        log.debug("Buscando usuarios activos: {}", activo);
        return usuarioRepository.findByActivo(activo);
    }

    @Override
    public List<Usuario> findByBloqueado(Boolean bloqueado) {
        log.debug("Buscando usuarios bloqueados: {}", bloqueado);
        return usuarioRepository.findByBloqueado(bloqueado);
    }

    // ==================== GESTIÓN ADMIN ====================

    @Override
    @Transactional
    public Usuario bloquearUsuario(Integer idUsuario, String razon, Integer adminId) {
        log.warn("Bloqueando usuario ID: {} por: {}", idUsuario, razon);
        
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + idUsuario));
        
        usuario.bloquear(razon, adminId);
        
        Usuario usuarioBloqueado = usuarioRepository.save(usuario);
        log.info("Usuario {} bloqueado exitosamente", usuario.getNombre());
        
        return usuarioBloqueado;
    }

    @Override
    @Transactional
    public Usuario desbloquearUsuario(Integer idUsuario) {
        log.info("Desbloqueando usuario ID: {}", idUsuario);
        
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + idUsuario));
        
        usuario.desbloquear();
        
        Usuario usuarioDesbloqueado = usuarioRepository.save(usuario);
        log.info("Usuario {} desbloqueado exitosamente", usuario.getNombre());
        
        return usuarioDesbloqueado;
    }

    @Override
    @Transactional
    public Usuario cambiarRol(Integer idUsuario, String nuevoRol) {
        log.info("Cambiando rol del usuario ID: {} a: {}", idUsuario, nuevoRol);
        
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + idUsuario));
        
        String rolAnterior = usuario.getRol();
        usuario.setRol(nuevoRol);
        
        Usuario usuarioActualizado = usuarioRepository.save(usuario);
        log.info("Rol del usuario {} cambiado de {} a {}", 
                usuario.getNombre(), rolAnterior, nuevoRol);
        
        return usuarioActualizado;
    }

    @Override
    @Transactional
    public Usuario cambiarEstadoActivo(Integer idUsuario, Boolean activo) {
        log.info("Cambiando estado activo del usuario ID: {} a: {}", idUsuario, activo);
        
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + idUsuario));
        
        usuario.setActivo(activo);
        
        Usuario usuarioActualizado = usuarioRepository.save(usuario);
        log.info("Usuario {} {} exitosamente", 
                usuario.getNombre(), activo ? "activado" : "desactivado");
        
        return usuarioActualizado;
    }

    // ==================== SEGURIDAD ====================

    @Override
    @Transactional
    public void incrementarIntentosFallidos(Integer idUsuario) {
        log.debug("Incrementando intentos fallidos del usuario ID: {}", idUsuario);
        
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + idUsuario));
        
        usuario.incrementarIntentosFallidos();
        usuarioRepository.save(usuario);
        
        log.warn("Usuario {} tiene {} intentos fallidos", 
                usuario.getNombre(), usuario.getIntentosFallidos());
    }

    @Override
    @Transactional
    public void resetearIntentosFallidos(Integer idUsuario) {
        log.debug("Reseteando intentos fallidos del usuario ID: {}", idUsuario);
        
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + idUsuario));
        
        usuario.resetearIntentosFallidos();
        usuarioRepository.save(usuario);
        
        log.info("Intentos fallidos reseteados para usuario {}", usuario.getNombre());
    }

    @Override
    @Transactional
    public void actualizarUltimoAcceso(Integer idUsuario) {
        log.debug("Actualizando último acceso del usuario ID: {}", idUsuario);
        
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + idUsuario));
        
        usuario.actualizarUltimoAcceso();
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void forzarCambioPassword(Integer idUsuario) {
        log.info("Forzando cambio de contraseña para usuario ID: {}", idUsuario);
        
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + idUsuario));
        
        usuario.setRequireCambioPassword(true);
        usuarioRepository.save(usuario);
        
        log.info("Usuario {} debe cambiar su contraseña en el próximo login", 
                usuario.getNombre());
    }

    // ==================== ESTADÍSTICAS ====================

    @Override
    public long count() {
        return usuarioRepository.count();
    }

    @Override
    public long countByActivo(Boolean activo) {
        return usuarioRepository.countByActivo(activo);
    }

    @Override
    public long countByBloqueado(Boolean bloqueado) {
        return usuarioRepository.countByBloqueado(bloqueado);
    }

    @Override
    public long countByRol(String rol) {
        return usuarioRepository.countByRol(rol);
    }
}
