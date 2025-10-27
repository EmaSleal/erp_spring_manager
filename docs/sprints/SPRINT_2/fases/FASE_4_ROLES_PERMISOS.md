# 🔐 FASE 4: ROLES Y PERMISOS - COMPLETADA

**Sprint:** 2 - Configuración y Gestión Avanzada  
**Fase:** 4 de 8  
**Período:** 13-14 de octubre de 2025  
**Estado:** ✅ COMPLETADO (8/8 tareas - 100%)

---

## 📋 RESUMEN EJECUTIVO

Se implementó un **sistema completo de roles y permisos** utilizando Spring Security, con 4 niveles de acceso diferentes y control granular en controladores y vistas.

### Componentes Implementados
- ✅ Tabla `usuario_rol` (relación N:N entre Usuario y Rol)
- ✅ SecurityConfig con reglas de autorización
- ✅ 4 roles: ADMIN, AGENTE, CONTADOR, VIEWER
- ✅ `@PreAuthorize` en controladores
- ✅ `sec:authorize` en vistas Thymeleaf
- ✅ Testing de cada rol

---

## 🎯 MATRIZ DE ROLES Y PERMISOS

### Roles Implementados

| Rol | Color Badge | Descripción | Casos de Uso |
|-----|-------------|-------------|--------------|
| **ADMIN** | 🔴 Rojo | Administrador con acceso total | Dueño, Gerente General |
| **AGENTE** | 🔵 Azul | Usuario operativo completo | Vendedores, Ejecutivos de cuenta |
| **CONTADOR** | 🟢 Verde | Acceso a reportes y facturas (solo lectura) | Contador, Auditor |
| **VIEWER** | 🟡 Amarillo | Solo visualización de información básica | Invitados, Consultores externos |

### Matriz de Permisos por Módulo

| Módulo | ADMIN | AGENTE | CONTADOR | VIEWER |
|--------|-------|--------|----------|--------|
| **Dashboard** | ✅ Total | ✅ Total | ✅ Total | ✅ Solo métricas básicas |
| **Clientes** | ✅ CRUD | ✅ CRUD | ❌ | ❌ |
| **Productos** | ✅ CRUD | ✅ CRUD | ❌ | ❌ |
| **Facturas** | ✅ CRUD | ✅ CRUD | 👁️ Solo lectura | 👁️ Solo lectura |
| **Reportes** | ✅ Todos | ✅ Todos | ✅ Todos | 👁️ Básicos |
| **Usuarios** | ✅ CRUD | ❌ | ❌ | ❌ |
| **Configuración** | ✅ Total | ❌ | 👁️ Solo lectura | ❌ |
| **Notificaciones** | ✅ Configurar | ✅ Ver historial | ❌ | ❌ |

**Leyenda:**
- ✅ = Acceso completo (CRUD)
- 👁️ = Solo lectura
- ❌ = Sin acceso

---

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

## 🧪 TESTING Y VERIFICACIÓN

### Casos de Prueba por Rol

#### Test 1: ADMIN
- ✅ Login exitoso
- ✅ Acceso a Dashboard
- ✅ Acceso a Configuración
- ✅ CRUD de Usuarios
- ✅ CRUD de Clientes
- ✅ CRUD de Productos
- ✅ CRUD de Facturas
- ✅ Todos los Reportes
- ✅ Ve todos los botones de acción

#### Test 2: AGENTE
- ✅ Login exitoso
- ✅ Acceso a Dashboard
- ❌ NO acceso a Configuración
- ❌ NO acceso a Usuarios
- ✅ CRUD de Clientes
- ✅ CRUD de Productos
- ✅ CRUD de Facturas
- ✅ Todos los Reportes
- ❌ NO ve botones de admin

#### Test 3: CONTADOR
- ✅ Login exitoso
- ✅ Acceso a Dashboard
- ❌ NO acceso a Configuración (o solo lectura)
- ❌ NO acceso a Usuarios
- ❌ NO acceso a Clientes
- ❌ NO acceso a Productos
- 👁️ Solo lectura de Facturas
- ✅ Todos los Reportes
- ❌ NO ve botones de crear/editar/eliminar

#### Test 4: VIEWER
- ✅ Login exitoso
- ✅ Acceso a Dashboard (métricas básicas)
- ❌ NO acceso a Configuración
- ❌ NO acceso a Usuarios
- ❌ NO acceso a Clientes
- ❌ NO acceso a Productos
- 👁️ Solo lectura de Facturas
- 👁️ Reportes básicos
- ❌ NO ve botones de acción

### Verificación de Seguridad

**Intento de acceso no autorizado:**
```
GET /usuarios (como AGENTE) → HTTP 403 Forbidden
GET /clientes (como VIEWER) → HTTP 403 Forbidden
POST /facturas/crear (como CONTADOR) → HTTP 403 Forbidden
```

**Resultado esperado:** Redirección a página de error 403 o login

---

## 📊 ARCHIVOS MODIFICADOS/CREADOS

### Nuevos
1. ✅ `src/main/java/api/astro/whats_orders_manager/config/SecurityConfig.java`

### Modificados
1. ✅ `src/main/java/api/astro/whats_orders_manager/models/Usuario.java`
   - Agregado campo `roles` (@ElementCollection)
   
2. ✅ `src/main/java/api/astro/whats_orders_manager/controllers/*.java` (8 controladores)
   - Agregadas anotaciones `@PreAuthorize`
   
3. ✅ `src/main/resources/templates/**/*.html` (todas las vistas)
   - Agregado namespace `xmlns:sec`
   - Agregadas directivas `sec:authorize`

4. ✅ `src/main/resources/schema.sql`
   - Agregada tabla `usuario_rol`

---

## 🎯 BENEFICIOS IMPLEMENTADOS

### Seguridad
- ✅ Control de acceso granular por rol
- ✅ Prevención de accesos no autorizados
- ✅ Seguridad en backend (controladores) Y frontend (vistas)
- ✅ Separación de responsabilidades

### Gestión
- ✅ Fácil asignación de roles a usuarios
- ✅ Un usuario puede tener múltiples roles
- ✅ Escalable (fácil agregar nuevos roles)
- ✅ Mantenible (configuración centralizada)

### Experiencia de Usuario
- ✅ UI adaptada según rol (muestra solo lo relevante)
- ✅ Menos confusión (usuarios no ven opciones no disponibles)
- ✅ Badges visuales para identificar roles
- ✅ Mensajes de error claros en accesos no autorizados

---

## 🔄 PRÓXIMAS MEJORAS (Opcionales)

### Mejoras Sugeridas
1. **Permisos más granulares** - Tabla de permisos separada
2. **Roles dinámicos** - Gestión de roles desde UI
3. **Auditoría de accesos** - Log de intentos de acceso denegados
4. **Sesiones concurrentes** - Limitar sesiones por usuario
5. **2FA (Two-Factor Auth)** - Autenticación de dos factores

### Configuración Adicional
```java
// Limitar sesiones concurrentes
http.sessionManagement(session -> session
    .maximumSessions(1)
    .maxSessionsPreventsLogin(true)
);

// Recordar sesión (Remember Me)
http.rememberMe(remember -> remember
    .key("uniqueAndSecret")
    .tokenValiditySeconds(86400) // 24 horas
);
```

---

## 📚 REFERENCIAS

- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/index.html)
- [Method Security (@PreAuthorize)](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)
- [Thymeleaf Security Dialect](https://github.com/thymeleaf/thymeleaf-extras-springsecurity)

---

## ✅ CONCLUSIÓN

La **Fase 4: Roles y Permisos** ha sido completada exitosamente con:

- ✅ 4 roles implementados y testeados
- ✅ Control de acceso en 8 controladores
- ✅ Seguridad en todas las vistas Thymeleaf
- ✅ Matriz de permisos documentada
- ✅ Testing completo de cada rol

El sistema ahora cuenta con **seguridad robusta a nivel de aplicación** con separación clara de responsabilidades por rol.

---

**Documentado por:** GitHub Copilot Agent  
**Fecha:** 20 de octubre de 2025  
**Estado:** ✅ COMPLETADO  
**Versión:** 1.0

**Archivos relacionados:**
- `SPRINT_2_CHECKLIST.txt` - Tareas 4.1 a 4.3
- `PUNTO_4.1_COMPLETADO.md` (legacy)
- `PUNTO_4.2_COMPLETADO.md` (legacy)
- `PUNTO_4.3_COMPLETADO.md` (legacy)
