## ✅ FASE 5: SEGURIDAD AVANZADA

**Estado:** Completada al 100%  
**Fecha:** 12/10/2025

### 5.1 SecurityConfig.java Modernizado

**Archivo:** `config/SecurityConfig.java`  
**Líneas:** 200+  
**Spring Security:** 6.5.0

#### Características Implementadas

##### @EnableMethodSecurity
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // ← Permite @PreAuthorize, @PostAuthorize, @Secured
public class SecurityConfig {
    // ...
}
```

Permite anotaciones de seguridad a nivel de método:
```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(Long id) { ... }
```

##### Autorización Granular

```java
.authorizeHttpRequests(auth -> auth
    // Recursos públicos
    .requestMatchers("/", "/auth/**", "/css/**", "/js/**", "/images/**")
        .permitAll()
    
    // Dashboard y perfil - requiere autenticación
    .requestMatchers("/dashboard", "/perfil/**")
        .authenticated()
    
    // Módulos operativos - requiere USER o ADMIN
    .requestMatchers("/clientes/**", "/productos/**", "/facturas/**")
        .hasAnyRole("USER", "ADMIN")
    
    // Módulos administrativos - solo ADMIN
    .requestMatchers("/configuracion/**", "/usuarios/**", "/reportes/**")
        .hasRole("ADMIN")
    
    // Resto requiere autenticación
    .anyRequest().authenticated()
)
```

##### Configuración de Login

```java
.formLogin(form -> form
    .loginPage("/auth/login")
    .usernameParameter("telefono")
    .passwordParameter("password")
    .defaultSuccessUrl("/dashboard", true)
    .failureUrl("/auth/login?error=true")
    .permitAll()
)
```

##### Configuración de Logout

```java
.logout(logout -> logout
    .logoutUrl("/logout")  // ← Cambiado de /auth/logout
    .logoutSuccessUrl("/auth/login?logout")
    .invalidateHttpSession(true)
    .deleteCookies("JSESSIONID")
    .permitAll()
)
```

##### Session Management

```java
.sessionManagement(session -> session
    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
    .maximumSessions(1)  // ← Máximo 1 sesión por usuario
    .maxSessionsPreventsLogin(false)  // Nueva sesión cierra la anterior
)
```

##### Headers de Seguridad

```java
.headers(headers -> headers
    .frameOptions(frame -> frame.sameOrigin())  // Permite iframes del mismo origen
    .xssProtection(xss -> xss.disable())  // Deshabilitado (Spring lo maneja)
)
```

##### AuthenticationManager (Spring Security 6.x)

```java
@Bean
public AuthenticationManager authenticationManager(
        HttpSecurity http,
        UserDetailsService userDetailsService,
        PasswordEncoder passwordEncoder) throws Exception {
    
    AuthenticationManagerBuilder authManagerBuilder = 
        http.getSharedObject(AuthenticationManagerBuilder.class);
    
    authManagerBuilder
        .userDetailsService(userDetailsService)
        .passwordEncoder(passwordEncoder);
    
    return authManagerBuilder.build();
}
```

### 5.2 CSRF Protection

#### Meta Tags en layout.html
```html
<meta name="_csrf" th:content="${_csrf.token}"/>
<meta name="_csrf_header" th:content="${_csrf.headerName}"/>
```

#### Uso en Formularios
```html
<form method="POST" action="/perfil/actualizar" th:action="@{/perfil/actualizar}">
    <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
    <!-- ... -->
</form>
```

#### Uso en JavaScript (navbar.js)
```javascript
async function handleLogout(event) {
    event.preventDefault();
    
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = '/logout';  // ← Cambiado de /auth/logout
    
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    if (csrfToken) {
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = '_csrf';  // ← Nombre estático (no dinámico)
        input.value = csrfToken;
        form.appendChild(input);
    }
    
    document.body.appendChild(form);
    form.submit();
}
```

### 5.3 Tracking de Último Acceso

#### UserDetailsServiceImpl.java
**Archivo:** `security/UserDetailsServiceImpl.java`  
**Cambios:**

```java
import java.sql.Timestamp;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Override
    public UserDetails loadUserByUsername(String nombre) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByNombre(nombre)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + nombre));

        // ← NUEVO: Actualizar último acceso
        actualizarUltimoAcceso(usuario);

        return User.withUsername(usuario.getTelefono())
                .password(usuario.getPassword())
                .roles(usuario.getRol())
                .build();
    }
    
    // ← NUEVO MÉTODO
    private void actualizarUltimoAcceso(Usuario usuario) {
        try {
            usuario.setUltimoAcceso(new Timestamp(System.currentTimeMillis()));
            usuarioRepository.save(usuario);
            System.out.println("✅ Último acceso actualizado para: " + usuario.getTelefono());
        } catch (Exception e) {
            // No interrumpir el login si falla la actualización
            System.err.println("❌ Error al actualizar último acceso para usuario " + 
                             usuario.getTelefono() + ": " + e.getMessage());
        }
    }
}
```

**Funcionamiento:**
- Se llama automáticamente en cada login
- Actualiza `ultimo_acceso` con timestamp actual
- No interrumpe el login si falla (try-catch)
- Log de confirmación/error

#### Visualización en Perfil

**perfil/ver.html:**
```html
<div class="info-row">
    <div class="info-label">
        <i class="fas fa-clock"></i>
        Último Acceso
    </div>
    <div class="info-value">
        <span th:if="${usuario.ultimoAcceso != null}"
              th:text="${#dates.format(usuario.ultimoAcceso, 'dd/MM/yyyy HH:mm')}">
            12/10/2025 15:30
        </span>
        <span th:unless="${usuario.ultimoAcceso != null}" class="empty">
            No registrado
        </span>
    </div>
</div>
```

---

