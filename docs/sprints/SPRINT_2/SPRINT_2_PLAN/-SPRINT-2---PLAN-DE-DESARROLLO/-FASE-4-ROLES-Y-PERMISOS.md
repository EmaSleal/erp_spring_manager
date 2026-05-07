## 📦 FASE 4: ROLES Y PERMISOS

### Objetivo
Implementar sistema de permisos basado en roles.

### Tareas

#### 4.1 Definición de Roles

**Roles disponibles:**
1. **ADMIN** - Acceso total
2. **USER** - Acceso a módulos operativos
3. **VENDEDOR** - Solo crear facturas y ver clientes
4. **VISUALIZADOR** - Solo lectura

#### 4.2 Tabla de Permisos

| Módulo | ADMIN | USER | VENDEDOR | VISUALIZADOR |
|--------|-------|------|----------|--------------|
| Dashboard | ✅ | ✅ | ✅ | ✅ |
| Clientes (Ver) | ✅ | ✅ | ✅ | ✅ |
| Clientes (Crear/Editar) | ✅ | ✅ | ❌ | ❌ |
| Productos (Ver) | ✅ | ✅ | ✅ | ✅ |
| Productos (Crear/Editar) | ✅ | ✅ | ❌ | ❌ |
| Facturas (Ver) | ✅ | ✅ | ✅ | ✅ |
| Facturas (Crear) | ✅ | ✅ | ✅ | ❌ |
| Facturas (Editar/Eliminar) | ✅ | ✅ | ❌ | ❌ |
| Reportes | ✅ | ✅ | ❌ | ❌ |
| Configuración | ✅ | ❌ | ❌ | ❌ |
| Usuarios | ✅ | ❌ | ❌ | ❌ |

#### 4.3 Implementación
**Archivo:** `SecurityConfig.java` (actualizar)

```java
@Override
protected void configure(HttpSecurity http) throws Exception {
    http
        .authorizeRequests()
            // Públicas
            .antMatchers("/auth/**", "/css/**", "/js/**", "/images/**").permitAll()
            
            // Dashboard y perfil (todos autenticados)
            .antMatchers("/dashboard", "/perfil/**").authenticated()
            
            // Módulos operativos (ADMIN, USER, VENDEDOR)
            .antMatchers("/clientes", "/productos", "/facturas").hasAnyRole("ADMIN", "USER", "VENDEDOR")
            
            // Crear/Editar (ADMIN, USER)
            .antMatchers("/clientes/save", "/productos/save").hasAnyRole("ADMIN", "USER")
            .antMatchers("/facturas/save").hasAnyRole("ADMIN", "USER", "VENDEDOR")
            
            // Eliminar (solo ADMIN, USER)
            .antMatchers("/*/delete/**").hasAnyRole("ADMIN", "USER")
            
            // Reportes (ADMIN, USER)
            .antMatchers("/reportes/**").hasAnyRole("ADMIN", "USER")
            
            // Configuración y Usuarios (solo ADMIN)
            .antMatchers("/configuracion/**", "/usuarios/**").hasRole("ADMIN")
            
            .anyRequest().authenticated()
        .and()
        .formLogin()
            .loginPage("/auth/login")
            .defaultSuccessUrl("/dashboard")
        .and()
        .logout()
            .logoutUrl("/auth/logout")
            .logoutSuccessUrl("/auth/login");
}
```

#### 4.4 Directivas Thymeleaf
```html
<!-- Mostrar solo para ADMIN -->
<div sec:authorize="hasRole('ADMIN')">
    <a href="/configuracion">Configuración</a>
</div>

<!-- Mostrar para ADMIN o USER -->
<button sec:authorize="hasAnyRole('ADMIN', 'USER')">Editar</button>

<!-- Ocultar para VISUALIZADOR -->
<form sec:authorize="!hasRole('VISUALIZADOR')">
    <!-- Formulario de edición -->
</form>
```

---

