## 🔧 IMPLEMENTACIÓN TÉCNICA

### 1. Modelo de Base de Datos

#### Tabla: `usuario_rol`
```sql
CREATE TABLE usuario_rol (
    usuario_id INT NOT NULL,
    rol_nombre VARCHAR(50) NOT NULL,
    PRIMARY KEY (usuario_id, rol_nombre),
    FOREIGN KEY (usuario_id) REFERENCES usuario(id_usuario) ON DELETE CASCADE
);
```

**Características:**
- Relación N:N entre Usuario y Rol
- Un usuario puede tener múltiples roles
- Clave primaria compuesta
- Cascada en eliminación de usuario

#### Modelo Java: `Usuario.java`
```java
@ElementCollection(fetch = FetchType.EAGER)
@CollectionTable(
    name = "usuario_rol",
    joinColumns = @JoinColumn(name = "usuario_id")
)
@Column(name = "rol_nombre")
private Set<String> roles = new HashSet<>();
```

---

### 2. SecurityConfig

#### Archivo: `SecurityConfig.java`

**Configuración de URLs por Rol:**

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            // Público
            .requestMatchers("/login", "/css/**", "/js/**", "/img/**").permitAll()
            
            // Solo ADMIN
            .requestMatchers("/usuarios/**").hasRole("ADMIN")
            .requestMatchers("/configuracion/**").hasRole("ADMIN")
            
            // ADMIN y AGENTE
            .requestMatchers("/clientes/**").hasAnyRole("ADMIN", "AGENTE")
            .requestMatchers("/productos/**").hasAnyRole("ADMIN", "AGENTE")
            .requestMatchers("/facturas/crear", "/facturas/editar/**", "/facturas/eliminar/**")
                .hasAnyRole("ADMIN", "AGENTE")
            
            // ADMIN, AGENTE y CONTADOR (solo lectura facturas)
            .requestMatchers("/facturas/**").hasAnyRole("ADMIN", "AGENTE", "CONTADOR", "VIEWER")
            
            // Reportes (todos excepto básicos para VIEWER)
            .requestMatchers("/reportes/**").hasAnyRole("ADMIN", "AGENTE", "CONTADOR", "VIEWER")
            
            // Dashboard (todos)
            .requestMatchers("/dashboard").authenticated()
            
            // Cualquier otra URL requiere autenticación
            .anyRequest().authenticated()
        )
        .formLogin(form -> form
            .loginPage("/login")
            .defaultSuccessUrl("/dashboard", true)
            .permitAll()
        )
        .logout(logout -> logout
            .logoutSuccessUrl("/login?logout")
            .permitAll()
        );
    
    return http.build();
}
```

---

### 3. Anotaciones en Controladores

#### `@PreAuthorize` para Métodos Específicos

**Ejemplo: UsuarioController.java**
```java
@Controller
@RequestMapping("/usuarios")
@PreAuthorize("hasRole('ADMIN')") // Toda la clase solo para ADMIN
public class UsuarioController {
    
    @GetMapping
    public String listarUsuarios(Model model) {
        // Solo accesible por ADMIN
    }
    
    @PostMapping("/crear")
    @PreAuthorize("hasRole('ADMIN')") // Redundante pero explícito
    public String crearUsuario(@ModelAttribute Usuario usuario) {
        // Solo ADMIN puede crear usuarios
    }
}
```

**Ejemplo: ClienteController.java**
```java
@Controller
@RequestMapping("/clientes")
public class ClienteController {
    
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENTE')")
    public String listarClientes(Model model) {
        // ADMIN y AGENTE pueden ver clientes
    }
    
    @PostMapping("/crear")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENTE')")
    public String crearCliente(@ModelAttribute Cliente cliente) {
        // Solo ADMIN y AGENTE pueden crear
    }
}
```

**Ejemplo: FacturaController.java**
```java
@Controller
@RequestMapping("/facturas")
public class FacturaController {
    
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENTE', 'CONTADOR', 'VIEWER')")
    public String listarFacturas(Model model) {
        // Todos pueden ver listado
    }
    
    @PostMapping("/crear")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENTE')")
    public String crearFactura(@ModelAttribute Factura factura) {
        // Solo ADMIN y AGENTE pueden crear
    }
    
    @GetMapping("/detalle/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENTE', 'CONTADOR', 'VIEWER')")
    public String verDetalle(@PathVariable Integer id, Model model) {
        // Todos pueden ver detalles (solo lectura)
    }
}
```

---

### 4. Control en Vistas (Thymeleaf)

#### Namespace de Seguridad
```html
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
```

#### Ejemplos de Uso

**Mostrar botones solo para roles específicos:**

```html
<!-- Solo ADMIN ve botón de configuración -->
<a th:href="@{/configuracion}" 
   sec:authorize="hasRole('ADMIN')"
   class="btn btn-primary">
    <i class="fas fa-cog"></i> Configuración
</a>

<!-- ADMIN y AGENTE ven botón de crear cliente -->
<a th:href="@{/clientes/crear}" 
   sec:authorize="hasAnyRole('ADMIN', 'AGENTE')"
   class="btn btn-success">
    <i class="fas fa-plus"></i> Nuevo Cliente
</a>

<!-- Solo ADMIN ve botón de eliminar -->
<button sec:authorize="hasRole('ADMIN')"
        th:onclick="'eliminarCliente(' + ${cliente.idCliente} + ')'"
        class="btn btn-danger btn-sm">
    <i class="fas fa-trash"></i>
</button>

<!-- Todos excepto VIEWER ven botón de exportar -->
<a th:href="@{/reportes/exportar}" 
   sec:authorize="hasAnyRole('ADMIN', 'AGENTE', 'CONTADOR')"
   class="btn btn-info">
    <i class="fas fa-download"></i> Exportar
</a>
```

**Mostrar/ocultar secciones completas:**

```html
<!-- Solo ADMIN ve panel de gestión de usuarios -->
<div sec:authorize="hasRole('ADMIN')" class="card mb-3">
    <div class="card-header">
        <i class="fas fa-users"></i> Gestión de Usuarios
    </div>
    <div class="card-body">
        <!-- Contenido de gestión -->
    </div>
</div>

<!-- CONTADOR ve facturas pero sin botones de acción -->
<div sec:authorize="hasRole('CONTADOR')">
    <table class="table">
        <!-- Tabla sin botones de editar/eliminar -->
    </table>
</div>
```

**Mostrar nombre de usuario y rol:**

```html
<div class="user-info">
    <span sec:authentication="principal.username"></span>
    <span class="badge bg-primary" sec:authentication="principal.authorities"></span>
</div>
```

---

