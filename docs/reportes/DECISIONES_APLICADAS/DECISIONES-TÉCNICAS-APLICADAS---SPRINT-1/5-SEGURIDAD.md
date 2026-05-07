## 5️⃣ SEGURIDAD

### **Decisión 5.1: Spring Security 6.x**

#### ✅ Decisión Final:
**Spring Security 6.x** con configuración Java

#### 🎯 Justificación:
- ✅ Estándar de la industria
- ✅ Integración nativa con Spring Boot
- ✅ CSRF protection automático
- ✅ Session management robusto

#### 📝 Configuración:
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Permite @PreAuthorize
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/auth/**").permitAll()
                .requestMatchers("/dashboard", "/perfil/**").authenticated()
                .requestMatchers("/clientes/**", "/productos/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/admin/**").hasRole("ADMIN")
            )
            .formLogin(/* ... */)
            .logout(/* ... */);
    }
}
```

---

### **Decisión 5.2: BCrypt para Contraseñas**

#### ✅ Decisión Final:
**BCrypt** con factor 10

#### 🎯 Justificación:
- ✅ Algoritmo de hashing seguro
- ✅ Salt automático
- ✅ Resistente a rainbow tables
- ✅ Estándar de Spring Security

#### 📝 Implementación:
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

---

### **Decisión 5.3: Roles con ROLE_ Prefix**

#### ✅ Decisión Final:
**Prefijo `ROLE_` automático** de Spring Security

#### 📝 Implementación:
```java
// Base de datos
rol VARCHAR(20) → "ADMIN", "USER", "CLIENTE"

// Spring Security agrega prefijo
.roles("ADMIN") // Internamente: ROLE_ADMIN
.hasRole("ADMIN") // Busca: ROLE_ADMIN
```

---

### **Decisión 5.4: Sesiones Limitadas**

#### ✅ Decisión Final:
**Máximo 1 sesión activa por usuario**

#### 🎯 Justificación:
- ✅ Previene uso compartido de cuentas
- ✅ Mejor seguridad
- ✅ Cierra sesión anterior automáticamente

#### 📝 Configuración:
```java
.sessionManagement(session -> session
    .maximumSessions(1)
    .maxSessionsPreventsLogin(false)  // Permite nuevo login
)
```

---

### **Decisión 5.5: CSRF Protection Habilitado**

#### ✅ Decisión Final:
**CSRF protection activo** en todos los formularios

#### 📝 Implementación:
```html
<!-- Meta tags en layout.html -->
<meta name="_csrf" th:content="${_csrf.token}"/>
<meta name="_csrf_header" th:content="${_csrf.headerName}"/>

<!-- Token en formularios -->
<input type="hidden" th:name="${_csrf.parameterName}" 
       th:value="${_csrf.token}"/>

<!-- JavaScript -->
const token = document.querySelector('meta[name="_csrf"]').content;
const header = document.querySelector('meta[name="_csrf_header"]').content;

fetch('/api/endpoint', {
    headers: {
        [header]: token
    }
});
```

---

