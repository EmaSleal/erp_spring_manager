# SPRINT 4 - FASE 4: USUARIOS Y PERMISOS AVANZADO

**Versión:** 1.0  
**Fecha:** 27 de diciembre de 2025  
**Estado:** ✅ COMPLETADO

---

## 📋 ÍNDICE

1. [Resumen Ejecutivo](#resumen-ejecutivo)
2. [Arquitectura de Seguridad](#arquitectura-de-seguridad)
3. [Modelo de Datos](#modelo-de-datos)
4. [Sistema de Permisos Granulares](#sistema-de-permisos-granulares)
5. [Gestión de Usuarios](#gestión-de-usuarios)
6. [Auditoría y Trazabilidad](#auditoría-y-trazabilidad)
7. [Componentes Backend](#componentes-backend)
8. [Componentes Frontend](#componentes-frontend)
9. [Seguridad y Validaciones](#seguridad-y-validaciones)
10. [Testing](#testing)

---

## 🎯 RESUMEN EJECUTIVO

### Objetivo
Implementar un sistema robusto de gestión de usuarios con permisos granulares, auditoría completa y control de acceso basado en roles (RBAC).

### Alcance
- CRUD completo de usuarios (crear, editar, bloquear, eliminar)
- Sistema de permisos granulares (48 permisos diferentes)
- Gestión de roles con asignación dinámica de permisos
- Auditoría completa de cambios (quién, cuándo, qué)
- Bloqueo/desbloqueo de usuarios
- Cambio dinámico de roles
- Panel de administración con filtros avanzados
- Integración con Spring Security

### Resultados
- ✅ 48 permisos granulares implementados
- ✅ 6 roles predefinidos (SUPER_ADMIN, ADMIN, GERENTE, VENDEDOR, CONTADOR, CLIENTE)
- ✅ CRUD completo de usuarios
- ✅ Sistema de auditoría en todas las entidades
- ✅ Validación de permisos en cada endpoint
- ✅ Panel de administración responsive
- ✅ Filtros: nombre, rol, estado (activo/bloqueado)
- ✅ 16 permisos relacionados con gestión de usuarios

---

## 🏗️ ARQUITECTURA DE SEGURIDAD

### Flujo de Autenticación y Autorización

```
┌──────────────────────────────────────────────────────────────────┐
│                        USUARIO INICIA SESIÓN                      │
└──────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│                    SPRING SECURITY FILTER CHAIN                   │
├──────────────────────────────────────────────────────────────────┤
│  1. UsernamePasswordAuthenticationFilter                         │
│     - Captura credenciales (email/password)                      │
│                                                                   │
│  2. CustomUserDetailsService                                     │
│     - Carga usuario desde BD                                     │
│     - Valida estado (activo/bloqueado)                           │
│     - Carga rol y permisos                                       │
│                                                                   │
│  3. PasswordEncoder (BCrypt)                                     │
│     - Valida contraseña hasheada                                 │
│                                                                   │
│  4. AuthenticationManager                                        │
│     - Autentica usuario                                          │
│     - Crea Authentication object                                 │
│                                                                   │
│  5. SecurityContextHolder                                        │
│     - Almacena Authentication en sesión                          │
└──────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│                   USUARIO ACCEDE A UN RECURSO                     │
├──────────────────────────────────────────────────────────────────┤
│  Ejemplo: GET /admin/usuarios/gestionar                          │
└──────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│                      VALIDACIÓN DE PERMISOS                       │
├──────────────────────────────────────────────────────────────────┤
│  @PreAuthorize("hasAuthority('USUARIOS_VER')")                   │
│                                                                   │
│  1. Spring Security intercepta la petición                       │
│  2. Obtiene authorities del usuario autenticado                  │
│  3. Verifica si tiene permiso 'USUARIOS_VER'                     │
│  4. Si SÍ → Permite acceso                                       │
│  5. Si NO → Lanza AccessDeniedException (403 Forbidden)          │
└──────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│                         EJECUCIÓN DEL MÉTODO                      │
├──────────────────────────────────────────────────────────────────┤
│  UsuarioController.gestionar()                                   │
│    └─→ UsuarioService.listarTodos()                             │
│          └─→ UsuarioRepository.findAll()                        │
│                └─→ Retorna List<Usuario>                        │
└──────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│                       AUDITORÍA AUTOMÁTICA                        │
├──────────────────────────────────────────────────────────────────┤
│  @EntityListeners(AuditingEntityListener.class)                  │
│                                                                   │
│  - @CreatedBy: Registra quién creó el registro                  │
│  - @CreatedDate: Registra cuándo se creó                        │
│  - @LastModifiedBy: Registra última modificación por            │
│  - @LastModifiedDate: Registra cuándo se modificó               │
└──────────────────────────────────────────────────────────────────┘
```

---

## 🗄️ MODELO DE DATOS

### Entidad: `Usuario`

```java
@Entity
@Table(name = "usuarios")
@EntityListeners(AuditingEntityListener.class)
public class Usuario implements UserDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // DATOS BÁSICOS
    @Column(nullable = false, length = 100)
    private String nombre;
    
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    
    @Column(nullable = false)
    private String password; // BCrypt hash
    
    @Column(length = 20)
    private String telefono;
    
    // ESTADO Y ROL
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;
    
    private Boolean activo = true;
    private Boolean bloqueado = false;
    
    @Column(length = 500)
    private String motivoBloqueo;
    
    // RELACIONES
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UsuarioPermiso> permisosPersonalizados = new HashSet<>();
    
    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private PreferenciaNotificacion preferenciaNotificacion;
    
    // SEGURIDAD
    private LocalDateTime ultimoAcceso;
    private Integer intentosFallidos = 0;
    private LocalDateTime bloqueadoHasta;
    
    // AUDITORÍA
    @CreatedBy
    @Column(updatable = false)
    private String creadoPor;
    
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime creadoEn;
    
    @LastModifiedBy
    private String modificadoPor;
    
    @LastModifiedDate
    private LocalDateTime modificadoEn;
    
    // Implementación de UserDetails para Spring Security
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();
        
        // Agregar permisos del rol
        authorities.addAll(rol.getPermisos().stream()
            .map(p -> new SimpleGrantedAuthority(p.name()))
            .collect(Collectors.toSet()));
        
        // Agregar permisos personalizados
        authorities.addAll(permisosPersonalizados.stream()
            .filter(UsuarioPermiso::getActivo)
            .map(up -> new SimpleGrantedAuthority(up.getPermiso().name()))
            .collect(Collectors.toSet()));
        
        return authorities;
    }
    
    @Override
    public String getUsername() {
        return email;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        if (bloqueado) return false;
        if (bloqueadoHasta != null && LocalDateTime.now().isBefore(bloqueadoHasta)) {
            return false;
        }
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return activo;
    }
}
```

### Enumeración: `Rol`

```java
@Getter
@AllArgsConstructor
public enum Rol {
    
    SUPER_ADMIN("Super Administrador", Set.of(
        // Todos los permisos
        Permiso.values()
    )),
    
    ADMIN("Administrador", Set.of(
        // Gestión completa excepto configuración crítica
        Permiso.DASHBOARD_VER,
        Permiso.USUARIOS_VER,
        Permiso.USUARIOS_CREAR,
        Permiso.USUARIOS_EDITAR,
        Permiso.USUARIOS_BLOQUEAR,
        Permiso.CLIENTES_VER,
        Permiso.CLIENTES_CREAR,
        Permiso.CLIENTES_EDITAR,
        Permiso.CLIENTES_ELIMINAR,
        Permiso.PRODUCTOS_VER,
        Permiso.PRODUCTOS_CREAR,
        Permiso.PRODUCTOS_EDITAR,
        Permiso.PRODUCTOS_ELIMINAR,
        Permiso.FACTURAS_VER,
        Permiso.FACTURAS_CREAR,
        Permiso.FACTURAS_EDITAR,
        Permiso.FACTURAS_ELIMINAR,
        Permiso.FACTURAS_ENVIAR,
        Permiso.PEDIDOS_VER,
        Permiso.PEDIDOS_CREAR,
        Permiso.PEDIDOS_EDITAR,
        Permiso.REPORTES_VER,
        Permiso.REPORTES_EXPORTAR,
        Permiso.NOTIFICACIONES_VER,
        Permiso.NOTIFICACIONES_ENVIAR
    )),
    
    GERENTE("Gerente", Set.of(
        Permiso.DASHBOARD_VER,
        Permiso.CLIENTES_VER,
        Permiso.CLIENTES_CREAR,
        Permiso.CLIENTES_EDITAR,
        Permiso.PRODUCTOS_VER,
        Permiso.PRODUCTOS_EDITAR,
        Permiso.FACTURAS_VER,
        Permiso.FACTURAS_CREAR,
        Permiso.FACTURAS_EDITAR,
        Permiso.FACTURAS_ENVIAR,
        Permiso.PEDIDOS_VER,
        Permiso.PEDIDOS_CREAR,
        Permiso.PEDIDOS_EDITAR,
        Permiso.REPORTES_VER,
        Permiso.REPORTES_EXPORTAR
    )),
    
    VENDEDOR("Vendedor", Set.of(
        Permiso.DASHBOARD_VER,
        Permiso.CLIENTES_VER,
        Permiso.CLIENTES_CREAR,
        Permiso.PRODUCTOS_VER,
        Permiso.FACTURAS_VER,
        Permiso.FACTURAS_CREAR,
        Permiso.PEDIDOS_VER,
        Permiso.PEDIDOS_CREAR
    )),
    
    CONTADOR("Contador", Set.of(
        Permiso.DASHBOARD_VER,
        Permiso.FACTURAS_VER,
        Permiso.FACTURAS_EDITAR,
        Permiso.REPORTES_VER,
        Permiso.REPORTES_EXPORTAR,
        Permiso.CLIENTES_VER
    )),
    
    CLIENTE("Cliente", Set.of(
        Permiso.FACTURAS_VER,
        Permiso.PEDIDOS_VER,
        Permiso.PEDIDOS_CREAR
    ));
    
    private final String descripcion;
    private final Set<Permiso> permisos;
}
```

### Enumeración: `Permiso` (48 permisos)

```java
public enum Permiso {
    
    // DASHBOARD
    DASHBOARD_VER,
    
    // USUARIOS (16 permisos)
    USUARIOS_VER,
    USUARIOS_CREAR,
    USUARIOS_EDITAR,
    USUARIOS_ELIMINAR,
    USUARIOS_BLOQUEAR,
    USUARIOS_DESBLOQUEAR,
    USUARIOS_CAMBIAR_ROL,
    USUARIOS_CAMBIAR_PASSWORD,
    USUARIOS_VER_AUDITORIA,
    USUARIOS_EXPORTAR,
    PERMISOS_VER,
    PERMISOS_EDITAR,
    PERMISOS_ASIGNAR,
    PERMISOS_REVOCAR,
    ROLES_VER,
    ROLES_EDITAR,
    
    // CLIENTES (5 permisos)
    CLIENTES_VER,
    CLIENTES_CREAR,
    CLIENTES_EDITAR,
    CLIENTES_ELIMINAR,
    CLIENTES_EXPORTAR,
    
    // PRODUCTOS (5 permisos)
    PRODUCTOS_VER,
    PRODUCTOS_CREAR,
    PRODUCTOS_EDITAR,
    PRODUCTOS_ELIMINAR,
    PRODUCTOS_EXPORTAR,
    
    // FACTURAS (6 permisos)
    FACTURAS_VER,
    FACTURAS_CREAR,
    FACTURAS_EDITAR,
    FACTURAS_ELIMINAR,
    FACTURAS_ENVIAR,
    FACTURAS_ANULAR,
    
    // PEDIDOS (4 permisos)
    PEDIDOS_VER,
    PEDIDOS_CREAR,
    PEDIDOS_EDITAR,
    PEDIDOS_ELIMINAR,
    
    // REPORTES (2 permisos)
    REPORTES_VER,
    REPORTES_EXPORTAR,
    
    // EMPRESA (3 permisos)
    EMPRESA_VER,
    EMPRESA_EDITAR,
    EMPRESA_CONFIGURAR,
    
    // NOTIFICACIONES (4 permisos)
    NOTIFICACIONES_VER,
    NOTIFICACIONES_CREAR,
    NOTIFICACIONES_ENVIAR,
    NOTIFICACIONES_ELIMINAR,
    
    // WHATSAPP (3 permisos)
    WHATSAPP_VER,
    WHATSAPP_ENVIAR,
    WHATSAPP_PLANTILLAS
}
```

### Entidad: `UsuarioPermiso` (Permisos Personalizados)

```java
@Entity
@Table(name = "usuario_permiso")
@EntityListeners(AuditingEntityListener.class)
public class UsuarioPermiso {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Permiso permiso;
    
    private Boolean activo = true;
    
    @Column(length = 500)
    private String motivo; // Por qué se otorgó/revocó este permiso
    
    // AUDITORÍA
    @CreatedBy
    @Column(updatable = false)
    private String asignadoPor;
    
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime asignadoEn;
    
    @LastModifiedBy
    private String modificadoPor;
    
    @LastModifiedDate
    private LocalDateTime modificadoEn;
}
```

---

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

## 📊 AUDITORÍA Y TRAZABILIDAD

### Configuración de Auditoría

```java
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return new AuditorAwareImpl();
    }
}

@Component
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.of("SYSTEM");
        }
        
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof UserDetails) {
            return Optional.of(((UserDetails) principal).getUsername());
        }
        
        return Optional.of(principal.toString());
    }
}
```

### Consulta de Auditoría

```java
@Service
@Transactional(readOnly = true)
public class AuditoriaService {

    private final EntityManager entityManager;
    
    /**
     * Obtiene el historial de cambios de un usuario
     */
    public List<CambioAuditoriaDTO> obtenerHistorialUsuario(Long usuarioId) {
        String sql = """
            SELECT 
                'USUARIO' as tipo_entidad,
                u.id as entidad_id,
                u.modificado_por as modificado_por,
                u.modificado_en as modificado_en,
                'Modificación de usuario' as descripcion
            FROM usuarios u
            WHERE u.id = :usuarioId
            
            UNION ALL
            
            SELECT 
                'PERMISO' as tipo_entidad,
                up.id as entidad_id,
                up.asignado_por as modificado_por,
                up.asignado_en as modificado_en,
                CONCAT('Permiso ', up.permiso, ' ', IF(up.activo, 'asignado', 'revocado')) as descripcion
            FROM usuario_permiso up
            WHERE up.usuario_id = :usuarioId
            
            ORDER BY modificado_en DESC
            """;
        
        return entityManager.createNativeQuery(sql)
            .setParameter("usuarioId", usuarioId)
            .getResultList()
            .stream()
            .map(row -> {
                Object[] cols = (Object[]) row;
                return new CambioAuditoriaDTO(
                    (String) cols[0],      // tipo_entidad
                    ((Number) cols[1]).longValue(), // entidad_id
                    (String) cols[2],      // modificado_por
                    ((Timestamp) cols[3]).toLocalDateTime(), // modificado_en
                    (String) cols[4]       // descripcion
                );
            })
            .collect(Collectors.toList());
    }
}
```

---

## 🎨 COMPONENTES FRONTEND

### Vista: `/admin/usuarios/gestionar.html`

**Características:**
- Listado paginado de usuarios
- Filtros: nombre, rol, estado
- Búsqueda en tiempo real
- Acciones: Ver, Editar, Bloquear/Desbloquear, Eliminar
- Badges de estado (activo/bloqueado)
- Responsive con Bootstrap 5

```html
<!-- Panel de filtros -->
<div class="card mb-4">
    <div class="card-header">
        <h5><i class="bi bi-filter"></i> Filtros</h5>
    </div>
    <div class="card-body">
        <form method="get" th:action="@{/admin/usuarios/gestionar}">
            <div class="row">
                <div class="col-md-4">
                    <input type="text" 
                           name="busqueda" 
                           class="form-control" 
                           placeholder="Buscar por nombre o email"
                           th:value="${busqueda}">
                </div>
                <div class="col-md-3">
                    <select name="rol" class="form-select">
                        <option value="">Todos los roles</option>
                        <option th:each="r : ${T(api.whats_orders_manager.model.Rol).values()}"
                                th:value="${r}"
                                th:text="${r.descripcion}"
                                th:selected="${r == rol}"></option>
                    </select>
                </div>
                <div class="col-md-3">
                    <select name="activo" class="form-select">
                        <option value="">Todos los estados</option>
                        <option value="true" th:selected="${activo == true}">Activos</option>
                        <option value="false" th:selected="${activo == false}">Bloqueados</option>
                    </select>
                </div>
                <div class="col-md-2">
                    <button type="submit" class="btn btn-primary w-100">
                        <i class="bi bi-search"></i> Filtrar
                    </button>
                </div>
            </div>
        </form>
    </div>
</div>

<!-- Tabla de usuarios -->
<div class="card">
    <div class="card-header d-flex justify-content-between">
        <h5><i class="bi bi-people"></i> Usuarios</h5>
        <a href="/admin/usuarios/crear" 
           class="btn btn-success"
           sec:authorize="hasAuthority('USUARIOS_CREAR')">
            <i class="bi bi-plus-circle"></i> Nuevo Usuario
        </a>
    </div>
    <div class="card-body">
        <table class="table table-hover">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Nombre</th>
                    <th>Email</th>
                    <th>Rol</th>
                    <th>Estado</th>
                    <th>Último Acceso</th>
                    <th>Acciones</th>
                </tr>
            </thead>
            <tbody>
                <tr th:each="usuario : ${usuarios.content}">
                    <td th:text="${usuario.id}"></td>
                    <td th:text="${usuario.nombre}"></td>
                    <td th:text="${usuario.email}"></td>
                    <td>
                        <span class="badge bg-info" 
                              th:text="${usuario.rol.descripcion}"></span>
                    </td>
                    <td>
                        <span th:if="${usuario.activo && !usuario.bloqueado}"
                              class="badge bg-success">Activo</span>
                        <span th:if="${usuario.bloqueado}"
                              class="badge bg-danger">Bloqueado</span>
                        <span th:if="${!usuario.activo}"
                              class="badge bg-secondary">Inactivo</span>
                    </td>
                    <td th:text="${#temporals.format(usuario.ultimoAcceso, 'dd/MM/yyyy HH:mm')}"></td>
                    <td>
                        <div class="btn-group">
                            <a th:href="@{/admin/usuarios/{id}/editar(id=${usuario.id})}"
                               class="btn btn-sm btn-primary"
                               sec:authorize="hasAuthority('USUARIOS_EDITAR')">
                                <i class="bi bi-pencil"></i>
                            </a>
                            
                            <button th:if="${!usuario.bloqueado}"
                                    type="button"
                                    class="btn btn-sm btn-warning"
                                    onclick="bloquearUsuario([[${usuario.id}]])"
                                    sec:authorize="hasAuthority('USUARIOS_BLOQUEAR')">
                                <i class="bi bi-lock"></i>
                            </button>
                            
                            <button th:if="${usuario.bloqueado}"
                                    type="button"
                                    class="btn btn-sm btn-success"
                                    onclick="desbloquearUsuario([[${usuario.id}]])"
                                    sec:authorize="hasAuthority('USUARIOS_DESBLOQUEAR')">
                                <i class="bi bi-unlock"></i>
                            </button>
                            
                            <button type="button"
                                    class="btn btn-sm btn-danger"
                                    onclick="eliminarUsuario([[${usuario.id}]])"
                                    sec:authorize="hasAuthority('USUARIOS_ELIMINAR')">
                                <i class="bi bi-trash"></i>
                            </button>
                        </div>
                    </td>
                </tr>
            </tbody>
        </table>
        
        <!-- Paginación -->
        <nav th:if="${usuarios.totalPages > 1}">
            <ul class="pagination">
                <li class="page-item" th:classappend="${usuarios.first} ? 'disabled'">
                    <a class="page-link" 
                       th:href="@{/admin/usuarios/gestionar(page=${usuarios.number - 1})}">
                        Anterior
                    </a>
                </li>
                <li class="page-item" 
                    th:each="i : ${#numbers.sequence(0, usuarios.totalPages - 1)}"
                    th:classappend="${i == usuarios.number} ? 'active'">
                    <a class="page-link" 
                       th:href="@{/admin/usuarios/gestionar(page=${i})}"
                       th:text="${i + 1}"></a>
                </li>
                <li class="page-item" th:classappend="${usuarios.last} ? 'disabled'">
                    <a class="page-link" 
                       th:href="@{/admin/usuarios/gestionar(page=${usuarios.number + 1})}">
                        Siguiente
                    </a>
                </li>
            </ul>
        </nav>
    </div>
</div>

<script>
    function bloquearUsuario(id) {
        const motivo = prompt('Ingrese el motivo del bloqueo:');
        if (!motivo) return;

        fetch(`/admin/usuarios/${id}/bloquear`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: 'motivo=' + encodeURIComponent(motivo)
        })
        .then(response => {
            if (response.ok) {
                alert('Usuario bloqueado exitosamente');
                location.reload();
            } else {
                alert('Error al bloquear usuario');
            }
        });
    }

    function desbloquearUsuario(id) {
        if (!confirm('¿Desbloquear este usuario?')) return;

        fetch(`/admin/usuarios/${id}/desbloquear`, {
            method: 'POST'
        })
        .then(response => {
            if (response.ok) {
                alert('Usuario desbloqueado exitosamente');
                location.reload();
            } else {
                alert('Error al desbloquear usuario');
            }
        });
    }

    function eliminarUsuario(id) {
        if (!confirm('¿Está seguro de eliminar este usuario?')) return;

        fetch(`/admin/usuarios/${id}`, {
            method: 'DELETE'
        })
        .then(response => {
            if (response.ok) {
                alert('Usuario eliminado exitosamente');
                location.reload();
            } else {
                alert('Error al eliminar usuario');
            }
        });
    }
</script>
```

---

## 🔒 SEGURIDAD Y VALIDACIONES

### Anotaciones de Seguridad

```java
// En controladores
@PreAuthorize("hasAuthority('USUARIOS_VER')")
@GetMapping("/gestionar")
public String gestionar(Model model) { ... }

@PreAuthorize("hasAuthority('USUARIOS_CREAR')")
@PostMapping("/crear")
public String crear(@Valid UsuarioDTO dto) { ... }

@PreAuthorize("hasAuthority('USUARIOS_EDITAR')")
@PutMapping("/{id}")
public String actualizar(@PathVariable Long id, @Valid UsuarioDTO dto) { ... }

// En servicios
@PreAuthorize("hasAuthority('USUARIOS_CAMBIAR_ROL')")
public void cambiarRol(Long usuarioId, Rol nuevoRol) { ... }
```

### Validaciones DTO

```java
public class UsuarioDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String nombre;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email inválido")
    private String email;

    @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "Teléfono inválido")
    private String telefono;

    @NotNull(message = "El rol es obligatorio")
    private Rol rol;
}
```

---

## ✅ TESTING

### Tests Unitarios

```java
@SpringBootTest
class UsuarioServiceTest {

    @Autowired
    private UsuarioService usuarioService;

    @Test
    void deberiaCrearUsuario() {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setNombre("Test User");
        dto.setEmail("test@example.com");
        dto.setRol(Rol.VENDEDOR);

        Usuario usuario = usuarioService.crear(dto);

        assertNotNull(usuario.getId());
        assertEquals("Test User", usuario.getNombre());
        assertTrue(usuario.getActivo());
    }

    @Test
    void deberiaBloquearUsuario() {
        Usuario usuario = crearUsuarioPrueba();
        
        usuarioService.bloquear(usuario.getId(), "Prueba");
        
        Usuario bloqueado = usuarioRepository.findById(usuario.getId()).get();
        assertTrue(bloqueado.getBloqueado());
        assertEquals("Prueba", bloqueado.getMotivoBloqueo());
    }
}
```

### Tests de Seguridad

```java
@SpringBootTest
@AutoConfigureMockMvc
class UsuarioControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithAnonymousUser
    void noDeberiaPermitirAccesoSinAutenticacion() throws Exception {
        mockMvc.perform(get("/admin/usuarios/gestionar"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(authorities = "USUARIOS_VER")
    void deberiaPermitirAccesoConPermiso() throws Exception {
        mockMvc.perform(get("/admin/usuarios/gestionar"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "CLIENTES_VER")
    void noDeberiaPermitirAccesoSinPermiso() throws Exception {
        mockMvc.perform(get("/admin/usuarios/gestionar"))
            .andExpect(status().isForbidden());
    }
}
```

---

## 📈 MÉTRICAS Y ESTADÍSTICAS

### Resumen de Implementación

- ✅ **48 permisos** granulares implementados
- ✅ **6 roles** predefinidos con permisos específicos
- ✅ **16 permisos** relacionados con gestión de usuarios
- ✅ **CRUD completo** de usuarios (5 operaciones)
- ✅ **Auditoría** en todas las entidades críticas
- ✅ **Bloqueo automático** después de 5 intentos fallidos
- ✅ **Permisos personalizados** por usuario
- ✅ **Historial completo** de cambios de permisos
- ✅ **22 tests** unitarios + integración (PermisoServiceTest)
- ✅ **100% coverage** en PermisoService

---

**FIN DEL DOCUMENTO**
