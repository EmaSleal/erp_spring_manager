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

