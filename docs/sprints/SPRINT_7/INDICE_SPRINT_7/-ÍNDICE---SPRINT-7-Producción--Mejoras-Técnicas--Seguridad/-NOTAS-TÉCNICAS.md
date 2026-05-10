## 💡 NOTAS TÉCNICAS

### Migración Username

**Antes (actual):**
```java
@Entity
public class Usuario {
    @Column(unique = true)
    private String username; // Contiene TELÉFONO ⚠️
    private String telefono; // ¿Duplicado?
}
```

**Después (Sprint 7):**
```java
@Entity
public class Usuario {
    @Column(unique = true)
    private String username; // Email o usuario real
    
    @Column(unique = true)
    private String email;
    
    private String telefono; // Solo para contacto
}

// Login permitido con username O email
```

**Script de migración:**
```sql
-- Paso 1: Añadir columna username_nuevo
ALTER TABLE usuario ADD COLUMN username_nuevo VARCHAR(100);

-- Paso 2: Migrar datos (username_nuevo = email si existe, sino = telefono)
UPDATE usuario SET username_nuevo = COALESCE(email, CONCAT('user_', telefono));

-- Paso 3: Validar migración
-- SELECT * FROM usuario WHERE username_nuevo IS NULL;

-- Paso 4: Renombrar columnas
-- ALTER TABLE usuario CHANGE username telefono_legacy VARCHAR(20);
-- ALTER TABLE usuario CHANGE username_nuevo username VARCHAR(100);
```

---

### Migración Timestamp → LocalDateTime

**Antes (actual):**
```java
@Entity
public class Factura {
    private Timestamp fechaCreacion; // ⚠️ Deprecated
}
```

**Después (Sprint 7):**
```java
@Entity
public class Factura {
    private LocalDateTime fechaCreacion; // ✅ Moderno
}
```

**Configuración Jackson:**
```java
@Configuration
public class JacksonConfig {
    @Bean
    public Jackson2ObjectMapperBuilder jacksonBuilder() {
        return new Jackson2ObjectMapperBuilder()
            .serializers(new LocalDateTimeSerializer(DateTimeFormatter.ISO_DATE_TIME))
            .deserializers(new LocalDateTimeDeserializer(DateTimeFormatter.ISO_DATE_TIME));
    }
}
```

---

### Remember Me Implementation

**SecurityConfig:**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.rememberMe()
            .key("uniqueAndSecret")
            .tokenValiditySeconds(86400 * 30) // 30 días
            .rememberMeParameter("remember-me")
            .rememberMeCookieName("remember-me-cookie");
        return http.build();
    }
}
```

**Login form:**
```html
<form th:action="@{/login}" method="post">
    <input type="text" name="username" />
    <input type="password" name="password" />
    <input type="checkbox" name="remember-me" /> Recordarme
    <button type="submit">Iniciar sesión</button>
</form>
```

---

### Sistema de Auditoría Completo

**Modelo:**
```java
@Entity
public class Auditoria {
    @Id
    @GeneratedValue
    private Long id;
    
    private String usuario;
    private String accion; // CREAR, MODIFICAR, ELIMINAR, LOGIN, LOGOUT
    private String entidad; // Usuario, Factura, Producto, etc.
    private Long entidadId;
    private String ip;
    private String userAgent;
    private LocalDateTime timestamp;
    private String detalles; // JSON con cambios
}
```

**Interceptor:**
```java
@Aspect
@Component
public class AuditoriaAspect {
    
    @AfterReturning("@annotation(Auditable)")
    public void auditar(JoinPoint joinPoint) {
        // Registrar en tabla auditoria
    }
}
```

---

**Documento creado:** 16 de enero de 2026  
**Creado por:** GitHub Copilot  
**Versión:** 1.0  
**Estado:** 📋 PLANIFICADO  
**Decisión pendiente:** ¿Implementar módulo de Producción?
