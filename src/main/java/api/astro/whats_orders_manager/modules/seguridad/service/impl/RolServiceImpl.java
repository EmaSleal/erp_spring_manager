package api.astro.whats_orders_manager.modules.seguridad.service.impl;

import api.astro.whats_orders_manager.modules.seguridad.model.Permiso;
import api.astro.whats_orders_manager.modules.seguridad.model.Rol;
import api.astro.whats_orders_manager.modules.seguridad.repository.PermisoRepository;
import api.astro.whats_orders_manager.modules.seguridad.repository.RolRepository;
import api.astro.whats_orders_manager.modules.seguridad.service.RolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * ============================================================================
 * IMPLEMENTACIÓN DEL SERVICIO DE ROLES
 * WhatsApp Orders Manager
 * ============================================================================
 * Gestiona roles y sus permisos asociados en base de datos.
 * 
 * @version 1.0 - Sprint 4 Fase 4.6
 * @since 23/12/2025
 * ============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RolServiceImpl implements RolService {
    
    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    
    @Override
    public List<Rol> obtenerTodosActivos() {
        log.debug("Obteniendo todos los roles activos");
        return rolRepository.findByActivoTrue();
    }
    
    @Override
    public List<Rol> obtenerTodos() {
        log.debug("Obteniendo todos los roles");
        return rolRepository.findAll();
    }
    
    @Override
    public Optional<Rol> buscarPorCodigo(String codigo) {
        log.debug("Buscando rol por código: {}", codigo);
        return rolRepository.findByCodigo(codigo);
    }
    
    @Override
    public Optional<Rol> buscarPorId(Long id) {
        log.debug("Buscando rol por ID: {}", id);
        return rolRepository.findById(id);
    }
    
    @Override
    public Optional<Rol> buscarPorCodigoConPermisos(String codigo) {
        log.debug("Buscando rol con permisos por código: {}", codigo);
        return rolRepository.findByCodigoWithPermisos(codigo);
    }
    
    @Override
    @Transactional
    public Rol crearRol(String codigo, String nombre, String descripcion) {
        log.info("Creando nuevo rol: {}", codigo);
        
        if (rolRepository.existsByCodigo(codigo)) {
            throw new IllegalArgumentException("Ya existe un rol con el código: " + codigo);
        }
        
        Rol rol = new Rol();
        rol.setCodigo(codigo);
        rol.setNombre(nombre);
        rol.setDescripcion(descripcion);
        rol.setActivo(true);
        
        Rol guardado = rolRepository.save(rol);
        log.info("Rol creado exitosamente: {}", codigo);
        return guardado;
    }
    
    @Override
    @Transactional
    public Rol actualizarRol(Long id, String nombre, String descripcion) {
        log.info("Actualizando rol ID: {}", id);
        
        Rol rol = rolRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + id));
        
        rol.setNombre(nombre);
        rol.setDescripcion(descripcion);
        
        Rol actualizado = rolRepository.save(rol);
        log.info("Rol actualizado exitosamente: {}", rol.getCodigo());
        return actualizado;
    }
    
    @Override
    @Transactional
    public Rol cambiarEstado(Long id, boolean activo) {
        log.info("Cambiando estado de rol ID: {} a {}", id, activo);
        
        Rol rol = rolRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + id));
        
        rol.setActivo(activo);
        
        Rol actualizado = rolRepository.save(rol);
        log.info("Estado del rol {} cambiado a: {}", rol.getCodigo(), activo);
        return actualizado;
    }
    
    @Override
    @Transactional
    public Rol asignarPermiso(Long idRol, Long idPermiso) {
        log.info("Asignando permiso {} al rol {}", idPermiso, idRol);
        
        Rol rol = rolRepository.findById(idRol)
            .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + idRol));
        
        Permiso permiso = permisoRepository.findById(idPermiso)
            .orElseThrow(() -> new IllegalArgumentException("Permiso no encontrado: " + idPermiso));
        
        rol.agregarPermiso(permiso);
        
        Rol actualizado = rolRepository.save(rol);
        log.info("Permiso {} asignado al rol {}", permiso.getCodigo(), rol.getCodigo());
        return actualizado;
    }
    
    @Override
    @Transactional
    public Rol removerPermiso(Long idRol, Long idPermiso) {
        log.info("Removiendo permiso {} del rol {}", idPermiso, idRol);
        
        Rol rol = rolRepository.findById(idRol)
            .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + idRol));
        
        Permiso permiso = permisoRepository.findById(idPermiso)
            .orElseThrow(() -> new IllegalArgumentException("Permiso no encontrado: " + idPermiso));
        
        rol.eliminarPermiso(permiso);
        
        Rol actualizado = rolRepository.save(rol);
        log.info("Permiso {} removido del rol {}", permiso.getCodigo(), rol.getCodigo());
        return actualizado;
    }
    
    @Override
    @Transactional
    public Rol asignarPermisos(Long idRol, Set<Long> idsPermisos) {
        log.info("Asignando {} permisos al rol {}", idsPermisos.size(), idRol);
        
        Rol rol = rolRepository.findById(idRol)
            .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + idRol));
        
        // Limpiar permisos actuales
        rol.getPermisos().clear();
        
        // Asignar nuevos permisos
        for (Long idPermiso : idsPermisos) {
            Permiso permiso = permisoRepository.findById(idPermiso)
                .orElseThrow(() -> new IllegalArgumentException("Permiso no encontrado: " + idPermiso));
            rol.agregarPermiso(permiso);
        }
        
        Rol actualizado = rolRepository.save(rol);
        log.info("{} permisos asignados al rol {}", idsPermisos.size(), rol.getCodigo());
        return actualizado;
    }
    
    @Override
    public Set<Permiso> obtenerPermisos(Long idRol) {
        log.debug("Obteniendo permisos del rol ID: {}", idRol);
        
        Rol rol = rolRepository.findById(idRol)
            .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + idRol));
        
        return new HashSet<>(rol.getPermisos());
    }
    
    @Override
    public Set<Permiso> obtenerPermisosPorCodigo(String codigoRol) {
        log.debug("Obteniendo permisos del rol: {}", codigoRol);
        
        Optional<Rol> rolOpt = rolRepository.findByCodigoWithPermisos(codigoRol);
        return rolOpt.map(rol -> new HashSet<>(rol.getPermisos()))
                     .orElse(new HashSet<>());
    }
    
    @Override
    public boolean tienePermiso(String codigoRol, String codigoPermiso) {
        log.debug("Verificando si rol {} tiene permiso {}", codigoRol, codigoPermiso);
        
        Optional<Rol> rolOpt = rolRepository.findByCodigoWithPermisos(codigoRol);
        if (rolOpt.isEmpty()) {
            log.warn("Rol no encontrado: {}", codigoRol);
            return false;
        }
        
        Rol rol = rolOpt.get();
        boolean tiene = rol.getPermisos().stream()
            .anyMatch(p -> p.getCodigo().equals(codigoPermiso));
        
        log.debug("Rol {} {} permiso {}", codigoRol, tiene ? "tiene" : "NO tiene", codigoPermiso);
        return tiene;
    }
    
    @Override
    public List<Object[]> contarUsuariosPorRol() {
        log.debug("Contando usuarios por rol");
        return rolRepository.contarUsuariosPorRol();
    }

    @Override
    public List<Rol> findByActivoTrue() {
        log.debug("Buscando roles activos");
        return rolRepository.findByActivoTrue();
    }
}
