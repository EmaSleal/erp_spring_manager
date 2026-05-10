## 👥 GESTIÓN DE USUARIOS

### UsuarioService

**Ubicación:** `src/main/java/api/whats_orders_manager/service/UsuarioService.java`

```java
@Service
@Transactional
@Slf4j
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final PreferenciaNotificacionService preferenciaService;
    
    /**
     * Lista todos los usuarios con filtros
     */
    public Page<Usuario> listar(String busqueda, Rol rol, Boolean activo, Pageable pageable) {
        if (busqueda != null && !busqueda.isEmpty()) {
            return usuarioRepository.findByNombreContainingOrEmailContaining(
                busqueda, busqueda, pageable
            );
        }
        
        if (rol != null && activo != null) {
            return usuarioRepository.findByRolAndActivo(rol, activo, pageable);
        }
        
        if (rol != null) {
            return usuarioRepository.findByRol(rol, pageable);
        }
        
        if (activo != null) {
            return usuarioRepository.findByActivo(activo, pageable);
        }
        
        return usuarioRepository.findAll(pageable);
    }
    
    /**
     * Crea un nuevo usuario
     */
    public Usuario crear(UsuarioDTO dto) {
        // Validar email único
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }
        
        Usuario usuario = new Usuario();
        usuario.setNombre(dto.getNombre());
        usuario.setEmail(dto.getEmail());
        usuario.setTelefono(dto.getTelefono());
        usuario.setRol(dto.getRol());
        
        // Generar contraseña temporal
        String passwordTemporal = generarPasswordTemporal();
        usuario.setPassword(passwordEncoder.encode(passwordTemporal));
        
        usuario.setActivo(true);
        usuario.setBloqueado(false);
        
        Usuario guardado = usuarioRepository.save(usuario);
        
        // Crear preferencias por defecto
        preferenciaService.crearPreferenciasPorDefecto(guardado);
        
        // TODO: Enviar email con contraseña temporal
        log.info("Usuario creado: {} con password temporal: {}", guardado.getEmail(), passwordTemporal);
        
        return guardado;
    }
    
    /**
     * Actualiza un usuario existente
     */
    public Usuario actualizar(Long id, UsuarioDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        
        // Validar email único (si cambió)
        if (!usuario.getEmail().equals(dto.getEmail())) {
            if (usuarioRepository.existsByEmail(dto.getEmail())) {
                throw new IllegalArgumentException("El email ya está registrado");
            }
            usuario.setEmail(dto.getEmail());
        }
        
        usuario.setNombre(dto.getNombre());
        usuario.setTelefono(dto.getTelefono());
        
        return usuarioRepository.save(usuario);
    }
    
    /**
     * Cambia el rol de un usuario
     */
    @PreAuthorize("hasAuthority('USUARIOS_CAMBIAR_ROL')")
    public void cambiarRol(Long usuarioId, Rol nuevoRol, String motivo) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        
        Rol rolAnterior = usuario.getRol();
        
        if (rolAnterior == nuevoRol) {
            throw new IllegalArgumentException("El usuario ya tiene ese rol");
        }
        
        usuario.setRol(nuevoRol);
        usuarioRepository.save(usuario);
        
        log.info("Rol cambiado: Usuario {} de {} a {}. Motivo: {}", 
            usuarioId, rolAnterior, nuevoRol, motivo);
    }
    
    /**
     * Bloquea un usuario
     */
    @PreAuthorize("hasAuthority('USUARIOS_BLOQUEAR')")
    public void bloquear(Long usuarioId, String motivo) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        
        if (usuario.getBloqueado()) {
            throw new IllegalStateException("El usuario ya está bloqueado");
        }
        
        usuario.setBloqueado(true);
        usuario.setMotivoBloqueo(motivo);
        usuarioRepository.save(usuario);
        
        log.warn("Usuario {} bloqueado. Motivo: {}", usuarioId, motivo);
    }
    
    /**
     * Desbloquea un usuario
     */
    @PreAuthorize("hasAuthority('USUARIOS_DESBLOQUEAR')")
    public void desbloquear(Long usuarioId, String motivo) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        
        if (!usuario.getBloqueado()) {
            throw new IllegalStateException("El usuario no está bloqueado");
        }
        
        usuario.setBloqueado(false);
        usuario.setMotivoBloqueo(null);
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);
        
        log.info("Usuario {} desbloqueado. Motivo: {}", usuarioId, motivo);
    }
    
    /**
     * Cambia la contraseña de un usuario
     */
    @PreAuthorize("hasAuthority('USUARIOS_CAMBIAR_PASSWORD')")
    public void cambiarPassword(Long usuarioId, String nuevaPassword) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        
        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);
        
        log.info("Contraseña cambiada para usuario {}", usuarioId);
    }
    
    /**
     * Elimina un usuario (soft delete)
     */
    @PreAuthorize("hasAuthority('USUARIOS_ELIMINAR')")
    public void eliminar(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        
        // Soft delete: marcar como inactivo
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
        
        log.warn("Usuario {} marcado como inactivo", usuarioId);
    }
    
    /**
     * Registra intento de login fallido
     */
    public void registrarIntentoFallido(String email) {
        Optional<Usuario> optUsuario = usuarioRepository.findByEmail(email);
        
        if (optUsuario.isEmpty()) return;
        
        Usuario usuario = optUsuario.get();
        usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);
        
        // Bloquear temporalmente después de 5 intentos
        if (usuario.getIntentosFallidos() >= 5) {
            usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(15));
            usuario.setMotivoBloqueo("Bloqueado automáticamente por intentos fallidos");
            log.warn("Usuario {} bloqueado temporalmente por 15 minutos", email);
        }
        
        usuarioRepository.save(usuario);
    }
    
    /**
     * Registra login exitoso
     */
    public void registrarLoginExitoso(String email) {
        Optional<Usuario> optUsuario = usuarioRepository.findByEmail(email);
        
        if (optUsuario.isEmpty()) return;
        
        Usuario usuario = optUsuario.get();
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        
        usuarioRepository.save(usuario);
    }
    
    private String generarPasswordTemporal() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
```

---

