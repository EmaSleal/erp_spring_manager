## 🔐 SISTEMA DE PERMISOS GRANULARES

### PermisoService

**Ubicación:** `src/main/java/api/whats_orders_manager/service/PermisoService.java`

```java
@Service
@Transactional
public class PermisoService {

    private final UsuarioPermisoRepository usuarioPermisoRepository;
    private final UsuarioRepository usuarioRepository;
    
    /**
     * Lista todos los permisos disponibles en el sistema
     */
    public List<PermisoDTO> listarTodosPermisos() {
        return Arrays.stream(Permiso.values())
            .map(p -> new PermisoDTO(
                p.name(),
                formatearNombre(p.name()),
                obtenerCategoria(p.name())
            ))
            .sorted(Comparator.comparing(PermisoDTO::getCategoria)
                .thenComparing(PermisoDTO::getNombre))
            .collect(Collectors.toList());
    }
    
    /**
     * Obtiene todos los permisos de un usuario (rol + personalizados)
     */
    public Set<Permiso> obtenerPermisosUsuario(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        
        Set<Permiso> permisos = new HashSet<>();
        
        // Permisos del rol
        permisos.addAll(usuario.getRol().getPermisos());
        
        // Permisos personalizados activos
        permisos.addAll(usuario.getPermisosPersonalizados().stream()
            .filter(UsuarioPermiso::getActivo)
            .map(UsuarioPermiso::getPermiso)
            .collect(Collectors.toSet()));
        
        return permisos;
    }
    
    /**
     * Asigna un permiso personalizado a un usuario
     */
    public void asignarPermiso(Long usuarioId, Permiso permiso, String motivo) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        
        // Verificar si ya tiene el permiso por rol
        if (usuario.getRol().getPermisos().contains(permiso)) {
            throw new IllegalStateException(
                "El usuario ya tiene este permiso a través de su rol"
            );
        }
        
        // Verificar si ya existe el permiso personalizado
        Optional<UsuarioPermiso> existente = usuarioPermisoRepository
            .findByUsuarioAndPermiso(usuario, permiso);
        
        if (existente.isPresent()) {
            // Reactivar si estaba inactivo
            UsuarioPermiso up = existente.get();
            if (!up.getActivo()) {
                up.setActivo(true);
                up.setMotivo(motivo);
                usuarioPermisoRepository.save(up);
                log.info("Permiso {} reactivado para usuario {}", permiso, usuarioId);
            } else {
                throw new IllegalStateException("El usuario ya tiene este permiso activo");
            }
        } else {
            // Crear nuevo permiso personalizado
            UsuarioPermiso nuevoPermiso = new UsuarioPermiso();
            nuevoPermiso.setUsuario(usuario);
            nuevoPermiso.setPermiso(permiso);
            nuevoPermiso.setMotivo(motivo);
            nuevoPermiso.setActivo(true);
            
            usuarioPermisoRepository.save(nuevoPermiso);
            log.info("Permiso {} asignado a usuario {}", permiso, usuarioId);
        }
    }
    
    /**
     * Revoca un permiso personalizado de un usuario
     */
    public void revocarPermiso(Long usuarioId, Permiso permiso, String motivo) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        
        UsuarioPermiso up = usuarioPermisoRepository
            .findByUsuarioAndPermiso(usuario, permiso)
            .orElseThrow(() -> new EntityNotFoundException(
                "El usuario no tiene este permiso personalizado"
            ));
        
        if (!up.getActivo()) {
            throw new IllegalStateException("El permiso ya está revocado");
        }
        
        up.setActivo(false);
        up.setMotivo(motivo);
        usuarioPermisoRepository.save(up);
        
        log.info("Permiso {} revocado de usuario {}", permiso, usuarioId);
    }
    
    /**
     * Verifica si un usuario tiene un permiso específico
     */
    public boolean tienePermiso(Long usuarioId, Permiso permiso) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        
        // Verificar en rol
        if (usuario.getRol().getPermisos().contains(permiso)) {
            return true;
        }
        
        // Verificar en permisos personalizados
        return usuario.getPermisosPersonalizados().stream()
            .anyMatch(up -> up.getPermiso() == permiso && up.getActivo());
    }
    
    /**
     * Obtiene el historial de cambios de permisos de un usuario
     */
    public List<UsuarioPermiso> obtenerHistorialPermisos(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        
        return usuarioPermisoRepository.findByUsuarioOrderByAsignadoEnDesc(usuario);
    }
    
    private String formatearNombre(String permiso) {
        return permiso.replace("_", " ").toLowerCase();
    }
    
    private String obtenerCategoria(String permiso) {
        String[] partes = permiso.split("_");
        return partes.length > 0 ? partes[0] : "OTROS";
    }
}
```

---

