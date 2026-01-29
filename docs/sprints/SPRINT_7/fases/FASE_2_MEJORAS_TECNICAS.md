# 🔧 FASE 2: Mejoras Técnicas (CRÍTICO)

**Sprint:** 7  
**Fase:** 2 de 5  
**Duración estimada:** 6-8 días  
**Prioridad:** ⭐⭐⭐ CRÍTICA  
**Estado:** 📋 PENDIENTE (0/38 tareas)

---

## ⚠️ CRÍTICO: CORRECCIONES TÉCNICAS OBLIGATORIAS

Esta fase corrige **hallazgos críticos** encontrados en el código actual:

1. ⚠️ **Username usa teléfono** → Migrar a email/usuario real
2. ⚠️ **Timestamp deprecated** → Migrar a LocalDateTime
3. ⚠️ **Remember Me faltante** → Implementar en login
4. ⚠️ **Auditoría incompleta** → Completar sistema de auditoría

---

## 📋 OBJETIVO DE LA FASE

Mejorar calidad técnica y corregir deuda técnica:
- Migrar username de teléfono a email
- Migrar Timestamp a LocalDateTime
- Implementar "Remember Me"
- Completar auditoría
- Optimizar queries (problema N+1)
- Implementar caché
- Mejorar validaciones
- Centralizar excepciones

---

## 📊 PROGRESO GENERAL

```
Progreso: [0/38] ░░░░░░░░░░░░░░░░░░░░ 0%

├─ 1. Migración Username → Email      [0/8]  ░░░░░░░░░░ 0%
├─ 2. Migración Timestamp → LocalDT   [0/6]  ░░░░░░░░░░ 0%
├─ 3. Implementar Remember Me         [0/4]  ░░░░░░░░░░ 0%
├─ 4. Completar Auditoría             [0/6]  ░░░░░░░░░░ 0%
├─ 5. Optimización de Queries         [0/6]  ░░░░░░░░░░ 0%
├─ 6. Implementar Caché               [0/4]  ░░░░░░░░░░ 0%
└─ 7. Mejoras de Código               [0/4]  ░░░░░░░░░░ 0%
```

---

## 📦 1. MIGRACIÓN: USERNAME → EMAIL (8 tareas)

### 1.1. Problema Actual

**Hallazgo:** ⚠️ El campo `username` actualmente almacena el **teléfono** del usuario, lo cual:
- Viola principios de autenticación estándar
- Dificulta integración con sistemas externos
- Limita opciones de login

**Solución:** Migrar a sistema dual: email (principal) + username (opcional)

#### Tareas:

- [ ] **1.1.1** Agregar campo `email` y `username` real a `Usuario`

```java
// ANTES (Usuario.java):
@Column(unique = true, nullable = false, length = 15)
private String username; // Actualmente es teléfono

// DESPUÉS:
@Column(unique = true, nullable = false, length = 100)
private String email;

@Column(unique = true, length = 50)
private String username; // Usuario real (opcional)

@Column(length = 15)
private String telefono; // El teléfono se mueve aquí
```

- [ ] **1.1.2** Crear migration SQL para agregar campos

```sql
-- Migration: MIGRATION_USERNAME_EMAIL_SPRINT_7.sql

-- Paso 1: Agregar nuevos campos
ALTER TABLE usuarios
ADD COLUMN email VARCHAR(100) AFTER id,
ADD COLUMN username_nuevo VARCHAR(50) AFTER email,
ADD COLUMN telefono_nuevo VARCHAR(15) AFTER username_nuevo;

-- Paso 2: Copiar datos existentes
-- El username actual (teléfono) va a telefono_nuevo
UPDATE usuarios SET telefono_nuevo = username;

-- Paso 3: Generar email temporal si no existe
-- Formato: telefono@temp.erp.local
UPDATE usuarios 
SET email = CONCAT(username, '@temp.erp.local')
WHERE email IS NULL;

-- Paso 4: Generar username real desde nombre
-- Tomar primera letra nombre + apellido (o parte del nombre)
UPDATE usuarios u
SET username_nuevo = LOWER(
    CONCAT(
        SUBSTRING(u.nombre, 1, 1),
        REPLACE(SUBSTRING_INDEX(u.nombre, ' ', -1), ' ', '')
    )
)
WHERE username_nuevo IS NULL;

-- Paso 5: Resolver duplicados de username_nuevo
-- Agregar sufijo numérico a duplicados
UPDATE usuarios u1
JOIN (
    SELECT username_nuevo, COUNT(*) as cnt
    FROM usuarios
    GROUP BY username_nuevo
    HAVING cnt > 1
) u2 ON u1.username_nuevo = u2.username_nuevo
SET u1.username_nuevo = CONCAT(u1.username_nuevo, u1.id);

-- Paso 6: Hacer NOT NULL y agregar índices
ALTER TABLE usuarios
MODIFY COLUMN email VARCHAR(100) NOT NULL,
ADD UNIQUE KEY uk_email (email),
ADD UNIQUE KEY uk_username (username_nuevo),
ADD INDEX idx_telefono (telefono_nuevo);

-- Paso 7: Eliminar constraint antiguo de username
ALTER TABLE usuarios
DROP INDEX uk_username_old; -- Si existe

-- Paso 8: Renombrar columnas (CUIDADO: puede requerir downtime)
-- Opción A: Renombrar directamente (más simple pero requiere downtime)
ALTER TABLE usuarios
CHANGE COLUMN username username_old VARCHAR(15),
CHANGE COLUMN username_nuevo username VARCHAR(50),
CHANGE COLUMN telefono_nuevo telefono VARCHAR(15);

-- Opción B: Dual-write durante transición (más complejo pero sin downtime)
-- Ver documentación de estrategia de migración

-- Paso 9: Después de validar, eliminar columna antigua
-- ALTER TABLE usuarios DROP COLUMN username_old;
```

- [ ] **1.1.3** Actualizar modelo `Usuario.java`

```java
@Entity
@Table(name = "usuarios")
@Data
public class Usuario implements UserDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Email del usuario (usado para login)
     */
    @Column(unique = true, nullable = false, length = 100)
    @Email(message = "Email inválido")
    private String email;
    
    /**
     * Nombre de usuario (opcional, alternativa para login)
     */
    @Column(unique = true, length = 50)
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,50}$", message = "Username debe ser alfanumérico")
    private String username;
    
    /**
     * Teléfono del usuario
     */
    @Column(length = 15)
    @Pattern(regexp = "^[0-9]{8,15}$", message = "Teléfono inválido")
    private String telefono;
    
    // ... resto de campos
    
    /**
     * UserDetails.getUsername() ahora retorna email
     */
    @Override
    public String getUsername() {
        return this.email; // Cambio crítico
    }
    
    /**
     * Método auxiliar para obtener username real
     */
    public String getUsernameDisplay() {
        return this.username != null ? this.username : this.email;
    }
}
```

- [ ] **1.1.4** Actualizar `UserDetailsServiceImpl`

```java
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Override
    public UserDetails loadUserByUsername(String emailOrUsername) throws UsernameNotFoundException {
        // Buscar por email O username
        Usuario usuario = usuarioRepository.findByEmailOrUsername(emailOrUsername, emailOrUsername)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + emailOrUsername));
        
        return usuario;
    }
}
```

- [ ] **1.1.5** Actualizar `UsuarioRepository`

```java
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    /**
     * Busca por email o username
     */
    Optional<Usuario> findByEmailOrUsername(String email, String username);
    
    /**
     * Busca solo por email
     */
    Optional<Usuario> findByEmail(String email);
    
    /**
     * Busca solo por username
     */
    Optional<Usuario> findByUsername(String username);
    
    /**
     * Verifica si email existe
     */
    boolean existsByEmail(String email);
    
    /**
     * Verifica si username existe
     */
    boolean existsByUsername(String username);
}
```

- [ ] **1.1.6** Actualizar formularios de login y registro

```html
<!-- login.html -->
<form th:action="@{/login}" method="post">
    <div class="mb-3">
        <label for="username" class="form-label">Email o Usuario</label>
        <input type="text" 
               id="username" 
               name="username" 
               class="form-control" 
               placeholder="email@ejemplo.com o usuario"
               required>
        <small class="text-muted">Ingresa tu email o nombre de usuario</small>
    </div>
    <div class="mb-3">
        <label for="password" class="form-label">Contraseña</label>
        <input type="password" 
               id="password" 
               name="password" 
               class="form-control" 
               required>
    </div>
    <!-- Remember Me se agrega en siguiente sección -->
    <button type="submit" class="btn btn-primary">Iniciar Sesión</button>
</form>

<!-- registro.html -->
<form th:action="@{/registro}" method="post" th:object="${usuario}">
    <div class="mb-3">
        <label for="email" class="form-label">Email *</label>
        <input type="email" 
               th:field="*{email}" 
               class="form-control" 
               required>
        <span th:if="${#fields.hasErrors('email')}" 
              th:errors="*{email}" 
              class="text-danger"></span>
    </div>
    
    <div class="mb-3">
        <label for="username" class="form-label">Nombre de Usuario (opcional)</label>
        <input type="text" 
               th:field="*{username}" 
               class="form-control"
               pattern="^[a-zA-Z0-9_]{3,50}$">
        <small class="text-muted">Alfanumérico, 3-50 caracteres. Si no lo ingresas, usarás tu email para login.</small>
    </div>
    
    <div class="mb-3">
        <label for="telefono" class="form-label">Teléfono *</label>
        <input type="tel" 
               th:field="*{telefono}" 
               class="form-control" 
               pattern="[0-9]{8,15}"
               required>
    </div>
    
    <!-- ... resto de campos ... -->
</form>
```

- [ ] **1.1.7** Actualizar validaciones en `UsuarioService`

```java
@Service
public class UsuarioService {
    
    @Transactional
    public UsuarioDTO registrarUsuario(UsuarioDTO dto) {
        // Validar email único
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException("El email ya está registrado");
        }
        
        // Validar username único (si se proporciona)
        if (dto.getUsername() != null && !dto.getUsername().isEmpty()) {
            if (usuarioRepository.existsByUsername(dto.getUsername())) {
                throw new BusinessException("El nombre de usuario ya está en uso");
            }
        }
        
        Usuario usuario = new Usuario();
        usuario.setEmail(dto.getEmail());
        usuario.setUsername(dto.getUsername());
        usuario.setTelefono(dto.getTelefono());
        usuario.setNombre(dto.getNombre());
        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        
        // ... resto de la lógica
        
        usuario = usuarioRepository.save(usuario);
        
        log.info("Usuario registrado: {} (email: {})", usuario.getUsername(), usuario.getEmail());
        
        return toDTO(usuario);
    }
}
```

- [ ] **1.1.8** Actualizar tests unitarios

```java
@Test
void testLoginConEmail() {
    // Given
    Usuario usuario = crearUsuarioTest("test@example.com", "testuser");
    
    // When
    UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");
    
    // Then
    assertNotNull(userDetails);
    assertEquals("test@example.com", userDetails.getUsername());
}

@Test
void testLoginConUsername() {
    // Given
    Usuario usuario = crearUsuarioTest("test@example.com", "testuser");
    
    // When
    UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");
    
    // Then
    assertNotNull(userDetails);
    assertEquals("test@example.com", userDetails.getUsername()); // getUsername() retorna email
    assertEquals("testuser", ((Usuario) userDetails).getUsernameDisplay());
}
```

---

## 📦 2. MIGRACIÓN: TIMESTAMP → LOCALDATETIME (6 tareas)

### 2.1. Problema Actual

**Hallazgo:** ⚠️ Uso de `java.sql.Timestamp` (deprecated desde Java 8) en lugar de `java.time.LocalDateTime`

**Impacto:**
- `Timestamp` está marcado como deprecated
- `LocalDateTime` es más moderno y robusto
- Mejor soporte para zonas horarias con `ZonedDateTime`

#### Tareas:

- [ ] **2.1.1** Identificar todas las entidades con `Timestamp`

```bash
# Buscar todos los usos de Timestamp en el proyecto
grep -r "java.sql.Timestamp" src/main/java/
grep -r "Timestamp " src/main/java/ | grep -v "// " | grep -v "//"
```

- [ ] **2.1.2** Crear migration SQL para tipos de columna

```sql
-- Migration: MIGRATION_TIMESTAMP_TO_DATETIME_SPRINT_7.sql

-- Ejemplo para tabla usuarios
ALTER TABLE usuarios
MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
MODIFY COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

-- Ejemplo para tabla facturas
ALTER TABLE facturas
MODIFY COLUMN created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
MODIFY COLUMN updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

-- Repetir para TODAS las tablas que usan timestamps
-- Lista de tablas a actualizar:
-- - usuarios
-- - clientes
-- - productos
-- - facturas
-- - detalles_factura
-- - pagos
-- - empresas
-- - notificaciones
-- - movimientos_inventario
-- - etc.
```

- [ ] **2.1.3** Actualizar entidades JPA

```java
// ANTES:
import java.sql.Timestamp;

@Column(name = "created_at")
private Timestamp createdAt;

@Column(name = "updated_at")
private Timestamp updatedAt;

@PrePersist
protected void onCreate() {
    createdAt = new Timestamp(System.currentTimeMillis());
    updatedAt = new Timestamp(System.currentTimeMillis());
}

@PreUpdate
protected void onUpdate() {
    updatedAt = new Timestamp(System.currentTimeMillis());
}

// DESPUÉS:
import java.time.LocalDateTime;

@Column(name = "created_at", updatable = false)
private LocalDateTime createdAt;

@Column(name = "updated_at")
private LocalDateTime updatedAt;

@PrePersist
protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
}

@PreUpdate
protected void onUpdate() {
    updatedAt = LocalDateTime.now();
}
```

- [ ] **2.1.4** Actualizar DTOs y conversiones

```java
// Antes (DTO con Timestamp):
private Timestamp createdAt;

public void setCreatedAt(Timestamp createdAt) {
    this.createdAt = createdAt;
}

// Después (DTO con LocalDateTime):
private LocalDateTime createdAt;

public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
}

// Conversión en mapper:
public UsuarioDTO toDTO(Usuario usuario) {
    UsuarioDTO dto = new UsuarioDTO();
    dto.setCreatedAt(usuario.getCreatedAt()); // Ya es LocalDateTime
    return dto;
}
```

- [ ] **2.1.5** Actualizar formateo en templates Thymeleaf

```html
<!-- ANTES (con Timestamp): -->
<td th:text="${#dates.format(factura.createdAt, 'dd/MM/yyyy HH:mm')}"></td>

<!-- DESPUÉS (con LocalDateTime): -->
<td th:text="${#temporals.format(factura.createdAt, 'dd/MM/yyyy HH:mm')}"></td>

<!-- Otros formatos útiles: -->
<td th:text="${#temporals.format(factura.createdAt, 'dd-MM-yyyy')}"></td>
<td th:text="${#temporals.format(factura.createdAt, 'HH:mm:ss')}"></td>
```

- [ ] **2.1.6** Actualizar comparaciones y cálculos de fechas

```java
// ANTES (con Timestamp):
Timestamp ahora = new Timestamp(System.currentTimeMillis());
long diff = ahora.getTime() - factura.getCreatedAt().getTime();
int dias = (int) (diff / (1000 * 60 * 60 * 24));

// DESPUÉS (con LocalDateTime):
LocalDateTime ahora = LocalDateTime.now();
long dias = ChronoUnit.DAYS.between(factura.getCreatedAt(), ahora);

// Sumar/restar días:
LocalDateTime manana = ahora.plusDays(1);
LocalDateTime ayer = ahora.minusDays(1);

// Comparaciones:
if (factura.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7))) {
    // Factura creada en últimos 7 días
}
```

---

## 📦 3. IMPLEMENTAR "REMEMBER ME" (4 tareas)

### 3.1. Problema Actual

**Hallazgo:** ⚠️ El formulario de login NO tiene opción "Recordarme" / "Remember Me"

**Impacto:** Los usuarios deben login cada vez que cierran el navegador

#### Tareas:

- [ ] **3.1.1** Configurar Remember Me en Spring Security

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ... configuración existente ...
            
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )
            
            // NUEVO: Configurar Remember Me
            .rememberMe(remember -> remember
                .key("erp-remember-me-key-2026") // Cambiar en producción
                .tokenValiditySeconds(30 * 24 * 60 * 60) // 30 días
                .userDetailsService(userDetailsService)
                .rememberMeParameter("remember-me") // Nombre del checkbox
                .rememberMeCookieName("erp-remember-me") // Nombre de la cookie
            )
            
            .logout(logout -> logout
                .logoutUrl("/logout")
                .deleteCookies("JSESSIONID", "erp-remember-me") // Eliminar cookie al logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );
        
        return http.build();
    }
}
```

- [ ] **3.1.2** Agregar checkbox en formulario de login

```html
<!-- login.html -->
<form th:action="@{/login}" method="post">
    <div class="mb-3">
        <label for="username" class="form-label">Email o Usuario</label>
        <input type="text" 
               id="username" 
               name="username" 
               class="form-control" 
               placeholder="email@ejemplo.com"
               required>
    </div>
    
    <div class="mb-3">
        <label for="password" class="form-label">Contraseña</label>
        <input type="password" 
               id="password" 
               name="password" 
               class="form-control" 
               required>
    </div>
    
    <!-- NUEVO: Checkbox Remember Me -->
    <div class="mb-3 form-check">
        <input type="checkbox" 
               class="form-check-input" 
               id="remember-me" 
               name="remember-me"
               checked>
        <label class="form-check-label" for="remember-me">
            Recordarme en este dispositivo (30 días)
        </label>
    </div>
    
    <button type="submit" class="btn btn-primary w-100">Iniciar Sesión</button>
    
    <div class="mt-3">
        <a href="/password/reset">¿Olvidaste tu contraseña?</a>
    </div>
</form>
```

- [ ] **3.1.3** Crear tabla para tokens persistentes (opcional pero recomendado)

```sql
-- Migration: MIGRATION_REMEMBER_ME_TOKENS_SPRINT_7.sql

CREATE TABLE persistent_logins (
    username VARCHAR(64) NOT NULL,
    series VARCHAR(64) PRIMARY KEY,
    token VARCHAR(64) NOT NULL,
    last_used TIMESTAMP NOT NULL,
    
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

```java
// Actualizar SecurityConfig para usar tokens persistentes
@Bean
public PersistentTokenRepository persistentTokenRepository(DataSource dataSource) {
    JdbcTokenRepositoryImpl tokenRepository = new JdbcTokenRepositoryImpl();
    tokenRepository.setDataSource(dataSource);
    return tokenRepository;
}

// Actualizar configuración de Remember Me
.rememberMe(remember -> remember
    .key("erp-remember-me-key-2026")
    .tokenValiditySeconds(30 * 24 * 60 * 60)
    .tokenRepository(persistentTokenRepository(dataSource)) // Usar BD
    .userDetailsService(userDetailsService)
    .rememberMeParameter("remember-me")
)
```

- [ ] **3.1.4** Tests de Remember Me

```java
@Test
@WithMockUser
void testRememberMeFunciona() throws Exception {
    mockMvc.perform(post("/login")
            .param("username", "test@example.com")
            .param("password", "password")
            .param("remember-me", "on"))
        .andExpect(status().is3xxRedirection())
        .andExpect(cookie().exists("erp-remember-me"));
}

@Test
void testRememberMePermiteAccesoSinPassword() throws Exception {
    // Simular cookie de remember-me válida
    Cookie rememberMeCookie = new Cookie("erp-remember-me", "valid-token");
    
    mockMvc.perform(get("/dashboard")
            .cookie(rememberMeCookie))
        .andExpect(status().isOk());
}
```

---

## 📦 4. COMPLETAR SISTEMA DE AUDITORÍA (6 tareas)

### 4.1. Problema Actual

**Hallazgo:** ⚠️ Sistema de auditoría parcialmente implementado

**Faltan:**
- Registro completo de todas las operaciones críticas
- Auditoría de cambios en entidades
- Dashboard de auditoría
- Reportes de actividad

#### Tareas:

- [ ] **4.1.1** Crear entidad `AuditoriaEvento`

```java
@Entity
@Table(name = "auditoria_eventos")
@Data
public class AuditoriaEvento {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Tipo de evento
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoEventoAuditoria tipoEvento;
    
    /**
     * Usuario que realizó la acción
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
    
    /**
     * Email del usuario (por si se elimina)
     */
    @Column(length = 100)
    private String usuarioEmail;
    
    /**
     * IP desde la que se realizó
     */
    @Column(length = 45)
    private String ipAddress;
    
    /**
     * User Agent del navegador
     */
    @Column(length = 500)
    private String userAgent;
    
    /**
     * Tipo de entidad afectada
     */
    @Column(length = 100)
    private String entidadTipo;
    
    /**
     * ID de la entidad afectada
     */
    @Column(name = "entidad_id")
    private Long entidadId;
    
    /**
     * Acción realizada
     */
    @Column(length = 50)
    private String accion; // CREATE, UPDATE, DELETE, LOGIN, LOGOUT, etc.
    
    /**
     * Datos antes del cambio (JSON)
     */
    @Column(columnDefinition = "TEXT")
    private String datosAntes;
    
    /**
     * Datos después del cambio (JSON)
     */
    @Column(columnDefinition = "TEXT")
    private String datosDespues;
    
    /**
     * Descripción del evento
     */
    @Column(length = 500)
    private String descripcion;
    
    /**
     * Resultado del evento
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ResultadoEvento resultado; // EXITO, FALLO, ERROR
    
    /**
     * Mensaje de error (si aplica)
     */
    @Column(length = 1000)
    private String mensajeError;
    
    @Column(nullable = false)
    private LocalDateTime fecha;
    
    @PrePersist
    protected void onCreate() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }
}

enum TipoEventoAuditoria {
    AUTENTICACION,
    AUTORIZACION,
    CRUD_USUARIO,
    CRUD_PRODUCTO,
    CRUD_FACTURA,
    CRUD_PAGO,
    CRUD_CLIENTE,
    CONFIGURACION,
    SEGURIDAD,
    OTRO
}

enum ResultadoEvento {
    EXITO,
    FALLO,
    ERROR
}
```

- [ ] **4.1.2** Crear migration SQL

```sql
CREATE TABLE auditoria_eventos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tipo_evento VARCHAR(50) NOT NULL,
    usuario_id BIGINT,
    usuario_email VARCHAR(100),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    entidad_tipo VARCHAR(100),
    entidad_id BIGINT,
    accion VARCHAR(50),
    datos_antes TEXT,
    datos_despues TEXT,
    descripcion VARCHAR(500),
    resultado VARCHAR(20),
    mensaje_error VARCHAR(1000),
    fecha DATETIME NOT NULL,
    
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    
    INDEX idx_tipo_evento (tipo_evento),
    INDEX idx_usuario (usuario_id),
    INDEX idx_fecha (fecha),
    INDEX idx_entidad (entidad_tipo, entidad_id),
    INDEX idx_resultado (resultado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

- [ ] **4.1.3** Crear `AuditoriaService`

```java
@Service
@RequiredArgsConstructor
@Async
public class AuditoriaService {
    
    private final AuditoriaEventoRepository auditoriaRepository;
    
    @Autowired
    private HttpServletRequest request;
    
    /**
     * Registra un evento de auditoría.
     */
    public void registrarEvento(
        TipoEventoAuditoria tipo,
        Usuario usuario,
        String accion,
        String entidadTipo,
        Long entidadId,
        Object datosAntes,
        Object datosDespues,
        String descripcion
    ) {
        try {
            AuditoriaEvento evento = new AuditoriaEvento();
            evento.setTipoEvento(tipo);
            evento.setUsuario(usuario);
            evento.setUsuarioEmail(usuario != null ? usuario.getEmail() : "SISTEMA");
            evento.setIpAddress(obtenerIP());
            evento.setUserAgent(obtenerUserAgent());
            evento.setEntidadTipo(entidadTipo);
            evento.setEntidadId(entidadId);
            evento.setAccion(accion);
            evento.setDatosAntes(datosAntes != null ? convertirAJSON(datosAntes) : null);
            evento.setDatosDespues(datosDespues != null ? convertirAJSON(datosDespues) : null);
            evento.setDescripcion(descripcion);
            evento.setResultado(ResultadoEvento.EXITO);
            evento.setFecha(LocalDateTime.now());
            
            auditoriaRepository.save(evento);
            
        } catch (Exception e) {
            log.error("Error registrando evento de auditoría: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Registra login exitoso.
     */
    public void registrarLogin(Usuario usuario) {
        registrarEvento(
            TipoEventoAuditoria.AUTENTICACION,
            usuario,
            "LOGIN",
            "Usuario",
            usuario.getId(),
            null,
            null,
            "Login exitoso"
        );
    }
    
    /**
     * Registra login fallido.
     */
    public void registrarLoginFallido(String email, String motivo) {
        AuditoriaEvento evento = new AuditoriaEvento();
        evento.setTipoEvento(TipoEventoAuditoria.AUTENTICACION);
        evento.setUsuarioEmail(email);
        evento.setIpAddress(obtenerIP());
        evento.setUserAgent(obtenerUserAgent());
        evento.setAccion("LOGIN_FALLIDO");
        evento.setDescripcion(motivo);
        evento.setResultado(ResultadoEvento.FALLO);
        evento.setFecha(LocalDateTime.now());
        
        auditoriaRepository.save(evento);
    }
    
    private String obtenerIP() {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
    
    private String obtenerUserAgent() {
        return request.getHeader("User-Agent");
    }
    
    private String convertirAJSON(Object objeto) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(objeto);
        } catch (Exception e) {
            return objeto.toString();
        }
    }
}
```

- [ ] **4.1.4** Integrar auditoría en servicios críticos

```java
// Ejemplo en UsuarioService
@Service
public class UsuarioService {
    
    @Autowired
    private AuditoriaService auditoriaService;
    
    @Transactional
    public UsuarioDTO actualizar(Long id, UsuarioDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        
        // Guardar estado antes
        Usuario usuarioAntes = clonar(usuario);
        
        // Aplicar cambios
        usuario.setNombre(dto.getNombre());
        usuario.setEmail(dto.getEmail());
        // ... otros cambios
        
        usuario = usuarioRepository.save(usuario);
        
        // Auditar
        auditoriaService.registrarEvento(
            TipoEventoAuditoria.CRUD_USUARIO,
            obtenerUsuarioActual(),
            "UPDATE",
            "Usuario",
            usuario.getId(),
            usuarioAntes,
            usuario,
            "Usuario actualizado"
        );
        
        return toDTO(usuario);
    }
}
```

- [ ] **4.1.5** Crear dashboard de auditoría

- [ ] **4.1.6** Crear reportes de auditoría

---

## 📦 5. OPTIMIZACIÓN DE QUERIES (6 tareas)

### 5.1. Problema N+1

#### Tareas:

- [ ] **5.1.1** Identificar queries con problema N+1

```java
// PROBLEMA N+1:
List<Factura> facturas = facturaRepository.findAll();
for (Factura factura : facturas) {
    System.out.println(factura.getCliente().getNombre()); // Query por cada factura!
    factura.getDetalles().size(); // Otro query!
}

// SOLUCIÓN: Usar JOIN FETCH
@Query("SELECT f FROM Factura f " +
       "JOIN FETCH f.cliente " +
       "LEFT JOIN FETCH f.detalles " +
       "WHERE f.fecha BETWEEN :desde AND :hasta")
List<Factura> findAllWithDetails(LocalDate desde, LocalDate hasta);
```

- [ ] **5.1.2** Agregar @EntityGraph en queries comunes

```java
@EntityGraph(attributePaths = {"cliente", "detalles", "detalles.producto"})
@Query("SELECT f FROM Factura f WHERE f.id = :id")
Optional<Factura> findByIdWithDetails(@Param("id") Long id);
```

- [ ] **5.1.3** Implementar paginación en listados grandes

```java
@GetMapping("/facturas")
public String listar(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    Model model
) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("fecha").descending());
    Page<Factura> facturas = facturaRepository.findAll(pageable);
    
    model.addAttribute("facturas", facturas);
    return "facturas/lista";
}
```

- [ ] **5.1.4** Usar proyecciones DTO para queries ligeras

```java
// Interface projection
public interface FacturaResumenDTO {
    Long getId();
    String getNumero();
    LocalDate getFecha();
    BigDecimal getTotal();
    String getClienteNombre();
}

@Query("SELECT f.id as id, f.numero as numero, f.fecha as fecha, " +
       "f.total as total, c.nombre as clienteNombre " +
       "FROM Factura f JOIN f.cliente c")
List<FacturaResumenDTO> findAllResumen();
```

- [ ] **5.1.5** Configurar índices en BD

- [ ] **5.1.6** Monitorear queries lentas

---

## 📦 6. IMPLEMENTAR CACHÉ (4 tareas)

#### Tareas:

- [ ] **6.1** Configurar Redis para caché

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

- [ ] **6.2** Configurar CacheManager

- [ ] **6.3** Agregar @Cacheable en métodos frecuentes

```java
@Cacheable(value = "productos", key = "#id")
public ProductoDTO obtenerPorId(Long id) {
    // ...
}

@CacheEvict(value = "productos", key = "#id")
public void eliminar(Long id) {
    // ...
}
```

- [ ] **6.4** Configurar TTL y eviction policies

---

## 📦 7. MEJORAS DE CÓDIGO (4 tareas)

#### Tareas:

- [ ] **7.1** Centralizar manejo de excepciones

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        // ...
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ConstraintViolationException ex) {
        // ...
    }
}
```

- [ ] **7.2** Agregar validaciones exhaustivas con Bean Validation

- [ ] **7.3** Implementar DTOs para todas las respuestas

- [ ] **7.4** Documentar código crítico con JavaDoc

---

## 📊 CRITERIOS DE ACEPTACIÓN

✅ Username migrado a email + username real  
✅ Timestamp reemplazado por LocalDateTime  
✅ Remember Me funcionando  
✅ Auditoría completa implementada  
✅ Queries optimizadas (sin N+1)  
✅ Caché implementado en operaciones frecuentes  
✅ Validaciones exhaustivas  
✅ Excepciones centralizadas  

---

## 📚 DEPENDENCIAS

**Requiere:**
- ✅ Acceso a base de datos
- ⚠️ Plan de migración sin downtime

**Habilita:**
- 🚀 Sistema más robusto y mantenible
- 🚀 Mejor experiencia de usuario
- 🚀 Auditoría completa

---

## 🔄 PRÓXIMOS PASOS

1. ✅ Validar todas las migraciones en ambiente de desarrollo
2. ✅ Ejecutar tests exhaustivos
3. 🚀 Continuar con **FASE 3: Seguridad Avanzada**

---

**Hallazgos corregidos:**
- ⚠️ Username → Email (CRÍTICO)
- ⚠️ Timestamp → LocalDateTime (CRÍTICO)
- ⚠️ Remember Me implementado
- ⚠️ Auditoría completada

---

**Fase creada:** 16 de enero de 2026  
**Responsable:** Equipo de Desarrollo  
**Prioridad:** CRÍTICA - No omitir
