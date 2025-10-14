# 🚀 MEJORAS FUTURAS Y CAMBIOS PLANIFICADOS

**Proyecto:** WhatsApp Orders Manager  
**Fecha de creación:** 12 de octubre de 2025  
**Última actualización:** 12 de octubre de 2025

---

## 📋 ÍNDICE

1. [Mejoras de Alta Prioridad](#mejoras-de-alta-prioridad)
2. [Mejoras de Media Prioridad](#mejoras-de-media-prioridad)
3. [Mejoras de Baja Prioridad](#mejoras-de-baja-prioridad)
4. [Refactorizaciones Técnicas](#refactorizaciones-técnicas)
5. [Nuevas Funcionalidades](#nuevas-funcionalidades)

---

## 🔴 MEJORAS DE ALTA PRIORIDAD

### 1. CAMBIAR USERNAME DE TELÉFONO A NOMBRE DE USUARIO

**Estado:** 📝 Planificado  
**Prioridad:** Alta  
**Sprint sugerido:** Sprint 2 o 3  
**Fecha de solicitud:** 12/10/2025

#### Descripción
Actualmente el sistema usa el **número de teléfono** como username para autenticación. Se requiere implementar un **campo username único** independiente del teléfono.

#### Problema Actual
```java
// UserDetailsServiceImpl.java
@Override
public UserDetails loadUserByUsername(String nombre) {
    Usuario usuario = usuarioRepository.findByNombre(nombre)
        .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + nombre));
    
    return User.withUsername(usuario.getTelefono())  // ← USA TELÉFONO
            .password(usuario.getPassword())
            .roles(usuario.getRol())
            .build();
}
```

```java
// SecurityConfig.java
.formLogin(form -> form
    .loginPage("/auth/login")
    .usernameParameter("telefono")  // ← PARÁMETRO ES TELÉFONO
    .passwordParameter("password")
    // ...
)
```

#### Solución Propuesta

##### 1. Modificar Modelo Usuario

```java
// Usuario.java
@Entity
@Table(name = "usuario")
public class Usuario implements Serializable {
    
    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;  // ← NUEVO CAMPO
    
    @Column(name = "telefono", unique = true, length = 20)
    private String telefono;  // Deja de ser username, solo dato de contacto
    
    // ... resto de campos
}
```

##### 2. Migración SQL

```sql
-- Fase: Agregar campo username
ALTER TABLE usuario ADD COLUMN username VARCHAR(50) UNIQUE;

-- Generar username temporal basado en nombre o teléfono
UPDATE usuario 
SET username = CONCAT('user', id_usuario) 
WHERE username IS NULL;

-- Hacer NOT NULL después de poblar
ALTER TABLE usuario MODIFY COLUMN username VARCHAR(50) NOT NULL;

-- Crear índice
CREATE UNIQUE INDEX idx_usuario_username ON usuario(username);
```

##### 3. Actualizar Repository

```java
// UsuarioRepository.java
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    Optional<Usuario> findByUsername(String username);  // ← NUEVO
    Optional<Usuario> findByTelefono(String telefono);  // Mantener
    Optional<Usuario> findByEmail(String email);
    
    boolean existsByUsername(String username);  // ← NUEVO
    boolean existsByTelefono(String telefono);
    boolean existsByEmail(String email);
}
```

##### 4. Actualizar UserDetailsService

```java
// UserDetailsServiceImpl.java
@Override
public UserDetails loadUserByUsername(String username) {  // ← CAMBIA DE nombre a username
    Usuario usuario = usuarioRepository.findByUsername(username)  // ← USA USERNAME
        .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

    actualizarUltimoAcceso(usuario);

    return User.withUsername(usuario.getUsername())  // ← USA USERNAME
            .password(usuario.getPassword())
            .roles(usuario.getRol())
            .build();
}
```

##### 5. Actualizar SecurityConfig

```java
// SecurityConfig.java
.formLogin(form -> form
    .loginPage("/auth/login")
    .usernameParameter("username")  // ← CAMBIAR DE "telefono" A "username"
    .passwordParameter("password")
    .defaultSuccessUrl("/dashboard", true)
    .failureUrl("/auth/login?error=true")
    .permitAll()
)
```

##### 6. Actualizar Vista de Login

```html
<!-- templates/auth/login.html -->
<form method="POST" th:action="@{/auth/login}">
    <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
    
    <!-- ❌ ANTES -->
    <input type="tel" name="telefono" placeholder="Número de teléfono" required>
    
    <!-- ✅ DESPUÉS -->
    <input type="text" name="username" placeholder="Nombre de usuario" required 
           pattern="[a-zA-Z0-9_]{3,50}" 
           title="3-50 caracteres: letras, números y guión bajo">
    
    <input type="password" name="password" placeholder="Contraseña" required>
    <button type="submit">Iniciar Sesión</button>
</form>
```

##### 7. Actualizar Vista de Registro

```html
<!-- templates/auth/register.html -->
<form method="POST" th:action="@{/auth/register}" th:object="${usuario}">
    
    <!-- NUEVO CAMPO -->
    <div class="form-group">
        <label for="username">Nombre de Usuario *</label>
        <input type="text" id="username" th:field="*{username}" 
               class="form-control" required
               pattern="[a-zA-Z0-9_]{3,50}"
               minlength="3" maxlength="50"
               placeholder="ej. juan_perez">
        <small>3-50 caracteres: solo letras, números y guión bajo</small>
        <span th:if="${#fields.hasErrors('username')}" 
              th:errors="*{username}" 
              class="error"></span>
    </div>
    
    <!-- Teléfono ahora es solo un dato de contacto -->
    <div class="form-group">
        <label for="telefono">Teléfono *</label>
        <input type="tel" id="telefono" th:field="*{telefono}" 
               class="form-control" required>
    </div>
    
    <!-- ... resto del formulario -->
</form>
```

##### 8. Actualizar AuthController

```java
// AuthController.java
@PostMapping("/register")
public String register(@Valid @ModelAttribute Usuario usuario, 
                       BindingResult result, 
                       Model model) {
    
    // Validar username único
    if (usuarioService.existsByUsername(usuario.getUsername())) {
        result.rejectValue("username", "error.usuario", 
                          "El nombre de usuario ya está en uso");
    }
    
    // Validar teléfono único (ya no es username)
    if (usuarioService.existsByTelefono(usuario.getTelefono())) {
        result.rejectValue("telefono", "error.usuario", 
                          "El teléfono ya está registrado");
    }
    
    // Validar email único
    if (usuarioService.existsByEmail(usuario.getEmail())) {
        result.rejectValue("email", "error.usuario", 
                          "El email ya está registrado");
    }
    
    if (result.hasErrors()) {
        return "auth/register";
    }
    
    // ... resto del código
}
```

##### 9. Actualizar Perfil

```html
<!-- templates/perfil/ver.html -->
<div class="info-row">
    <div class="info-label">
        <i class="fas fa-user"></i>
        Nombre de Usuario
    </div>
    <div class="info-value">
        <span th:text="${usuario.username}">juan_perez</span>
    </div>
</div>

<div class="info-row">
    <div class="info-label">
        <i class="fas fa-phone"></i>
        Teléfono
    </div>
    <div class="info-value">
        <span th:text="${usuario.telefono}">+52 999 123 4567</span>
    </div>
</div>
```

#### Archivos a Modificar

| Archivo | Tipo | Cambio |
|---------|------|--------|
| `models/Usuario.java` | Backend | Agregar campo `username` |
| `repositories/UsuarioRepository.java` | Backend | Agregar métodos `findByUsername`, `existsByUsername` |
| `services/UsuarioService.java` | Backend | Agregar métodos username |
| `services/UsuarioServiceImpl.java` | Backend | Implementar métodos username |
| `security/UserDetailsServiceImpl.java` | Backend | Cambiar `findByNombre` a `findByUsername` |
| `config/SecurityConfig.java` | Backend | Cambiar `usernameParameter("telefono")` a `usernameParameter("username")` |
| `controllers/AuthController.java` | Backend | Validar `username` único en registro |
| `controllers/PerfilController.java` | Backend | Permitir editar `username` |
| `templates/auth/login.html` | Frontend | Cambiar campo teléfono a username |
| `templates/auth/register.html` | Frontend | Agregar campo username |
| `templates/perfil/ver.html` | Frontend | Mostrar username |
| `templates/perfil/editar.html` | Frontend | Editar username |
| `MIGRATION_USERNAME.sql` | Database | Script de migración |

**Total:** 13 archivos

#### Validaciones Necesarias

```java
// Usuario.java
@Column(name = "username", unique = true, nullable = false, length = 50)
@Pattern(regexp = "^[a-zA-Z0-9_]{3,50}$", 
         message = "Username debe tener 3-50 caracteres (solo letras, números y _)")
private String username;
```

#### Testing Requerido

- [ ] Login con username funciona
- [ ] Registro con username único
- [ ] Username duplicado rechazado
- [ ] Editar username en perfil
- [ ] Username inválido rechazado (caracteres especiales)
- [ ] Migración de datos existentes correcta
- [ ] Último acceso se actualiza con nuevo sistema
- [ ] Spring Security reconoce username

#### Estimación
- **Desarrollo:** 4-6 horas
- **Testing:** 2-3 horas
- **Migración datos:** 1 hora
- **Documentación:** 1 hora
- **TOTAL:** ~8-11 horas (1-1.5 días)

#### Riesgos
- ⚠️ Migración de usuarios existentes (necesitan username temporal)
- ⚠️ Sesiones activas pueden invalidarse
- ⚠️ Código que referencia teléfono como username

#### Dependencias
- Base de datos debe soportar ALTER TABLE
- Backup antes de migración
- Notificar usuarios del cambio

---

## 🟡 MEJORAS DE MEDIA PRIORIDAD

### 2. Migrar de Timestamp a LocalDateTime

**Estado:** 📝 Planificado  
**Sprint sugerido:** Sprint 3

#### Descripción
Cambiar de `java.sql.Timestamp` a `java.time.LocalDateTime` en el modelo `Usuario` para mejor compatibilidad con Thymeleaf y estándares modernos.

#### Cambios

```java
// Usuario.java - ANTES
@Column(name = "ultimo_acceso")
private Timestamp ultimoAcceso;

@CreatedDate
@Column(name = "createDate", updatable = false)
private Timestamp createDate;

@CreatedDate
@Column(name = "updateDate")
private Timestamp updateDate;
```

```java
// Usuario.java - DESPUÉS
@Column(name = "ultimo_acceso")
private LocalDateTime ultimoAcceso;

@CreatedDate
@Column(name = "createDate", updatable = false)
private LocalDateTime createDate;

@LastModifiedDate
@Column(name = "updateDate")
private LocalDateTime updateDate;
```

```java
// UserDetailsServiceImpl.java - ANTES
usuario.setUltimoAcceso(new Timestamp(System.currentTimeMillis()));

// DESPUÉS
usuario.setUltimoAcceso(LocalDateTime.now());
```

```html
<!-- Templates - Thymeleaf - CAMBIO -->
<!-- Poder usar #temporals en lugar de #dates -->
th:text="${#temporals.format(usuario.createDate, 'dd/MM/yyyy HH:mm')}"
```

**Archivos a modificar:** 3-4  
**Estimación:** 2-3 horas

---

### 3. Implementar Remember Me

**Estado:** 📝 Planificado  
**Sprint sugerido:** Sprint 2-3

#### Descripción
Agregar funcionalidad "Recordarme" en login para mantener sesión por más tiempo.

```java
// SecurityConfig.java
.rememberMe(remember -> remember
    .key("uniqueAndSecret")
    .tokenValiditySeconds(86400 * 7)  // 7 días
    .userDetailsService(userDetailsService)
)
```

```html
<!-- login.html -->
<div class="form-check">
    <input type="checkbox" name="remember-me" id="remember-me" class="form-check-input">
    <label for="remember-me" class="form-check-label">Recordarme</label>
</div>
```

**Archivos a modificar:** 2  
**Estimación:** 1-2 horas

---

### 4. Internacionalización (i18n)

**Estado:** 📝 Planificado  
**Sprint sugerido:** Sprint 4+

#### Descripción
Soporte para múltiples idiomas (Español, Inglés).

```java
// messages_es.properties
login.title=Iniciar Sesión
login.username=Nombre de Usuario
login.password=Contraseña
login.submit=Entrar

// messages_en.properties
login.title=Login
login.username=Username
login.password=Password
login.submit=Sign In
```

**Archivos a crear:** ~20 (messages properties)  
**Estimación:** 8-12 horas

---

## 🟢 MEJORAS DE BAJA PRIORIDAD

### 5. Historial de Accesos

**Estado:** 💭 Idea  
**Sprint sugerido:** Sprint 5+

Tabla `accesos` para registrar todos los logins:
```sql
CREATE TABLE accesos (
    id_acceso BIGINT PRIMARY KEY AUTO_INCREMENT,
    id_usuario BIGINT,
    fecha_acceso TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    exitoso BOOLEAN,
    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
);
```

---

### 6. Two-Factor Authentication (2FA)

**Estado:** 💭 Idea  
**Sprint sugerido:** Sprint 6+

Implementar autenticación de dos factores con TOTP (Google Authenticator).

---

### 7. Social Login (OAuth2)

**Estado:** 💭 Idea  
**Sprint sugerido:** Sprint 7+

Login con Google, Facebook, Microsoft.

---

## 🔧 REFACTORIZACIONES TÉCNICAS

### 8. Separar Configuración de Seguridad

**Estado:** 📝 Planificado  
**Sprint sugerido:** Sprint 3

Dividir `SecurityConfig.java` en múltiples archivos:
- `SecurityConfig.java` - Configuración principal
- `SecurityAuthenticationConfig.java` - Authentication manager
- `SecurityAuthorizationConfig.java` - Permisos y roles
- `SecuritySessionConfig.java` - Session management

**Estimación:** 3-4 horas

---

### 9. Crear DTOs para Responses

**Estado:** 📝 Planificado  
**Sprint sugerido:** Sprint 3

Separar modelos de base de datos de DTOs de respuesta:
```java
// UsuarioDTO.java
public class UsuarioDTO {
    private Long id;
    private String username;
    private String email;
    private String telefono;
    private String rol;
    // NO incluir password
}
```

**Estimación:** 4-6 horas

---

## 🆕 NUEVAS FUNCIONALIDADES

### 10. Dashboard con Gráficas Reales

**Estado:** 📝 Planificado  
**Sprint sugerido:** Sprint 2

Implementar gráficas con Chart.js:
- Ventas por mes
- Productos más vendidos
- Clientes frecuentes
- Estado de facturas (pie chart)

**Estimación:** 8-12 horas

---

### 11. Notificaciones en Tiempo Real

**Estado:** 💭 Idea  
**Sprint sugerido:** Sprint 5+

Implementar WebSocket para notificaciones:
- Nueva factura creada
- Pago recibido
- Producto sin stock
- Mensaje de cliente

**Estimación:** 16-20 horas

---

### 12. Exportación de Reportes

**Estado:** 📝 Planificado  
**Sprint sugerido:** Sprint 3-4

Exportar datos a:
- PDF (iText o JasperReports)
- Excel (Apache POI)
- CSV

**Estimación:** 12-16 horas

---

## 📊 RESUMEN DE PRIORIDADES

```
ALTA PRIORIDAD (Sprint 2-3):
├── ✅ #1: Username en lugar de teléfono (8-11 horas)
├── ⏳ #3: Remember Me (1-2 horas)
└── ⏳ #10: Gráficas reales en Dashboard (8-12 horas)

MEDIA PRIORIDAD (Sprint 3-4):
├── ⏳ #2: Migrar a LocalDateTime (2-3 horas)
├── ⏳ #8: Refactorizar SecurityConfig (3-4 horas)
├── ⏳ #9: Crear DTOs (4-6 horas)
└── ⏳ #12: Exportación de reportes (12-16 horas)

BAJA PRIORIDAD (Sprint 4+):
├── 💭 #4: Internacionalización (8-12 horas)
├── 💭 #5: Historial de accesos (6-8 horas)
├── 💭 #6: Two-Factor Authentication (16-20 horas)
├── 💭 #7: Social Login (20-24 horas)
└── 💭 #11: Notificaciones en tiempo real (16-20 horas)
```

---

## 📝 NOTAS

- Todas las estimaciones son aproximadas
- Prioridades pueden cambiar según necesidades del negocio
- Algunas mejoras pueden combinarse en un mismo sprint
- Testing y documentación incluidos en estimaciones

---

**Última actualización:** 12 de octubre de 2025  
**Responsable:** GitHub Copilot Agent  
**Estado del documento:** 🟢 Activo
