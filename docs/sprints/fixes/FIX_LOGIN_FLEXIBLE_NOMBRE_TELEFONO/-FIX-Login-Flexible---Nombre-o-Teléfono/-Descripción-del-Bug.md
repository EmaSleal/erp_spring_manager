## 🐛 Descripción del Bug

### Contexto

Durante la implementación del Punto 7.3 (Último Acceso), se modificó el método `loadUserByUsername()` en `UserDetailsServiceImpl` con la intención de mejorar el código. Sin embargo, se hizo una **asunción incorrecta** sobre qué campo se usa para el login.

### Código Problemático (ANTES - Funcionaba)

```java
@Override
public UserDetails loadUserByUsername(String nombre) throws UsernameNotFoundException {
    Usuario usuario = usuarioRepository.findByNombre(nombre)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + nombre));
    
    actualizarUltimoAcceso(usuario);
    
    return User.withUsername(usuario.getTelefono())
            .password(usuario.getPassword())
            .roles(usuario.getRol())
            .build();
}
```

### Código con Bug (DESPUÉS - No funcionaba)

```java
@Override
public UserDetails loadUserByUsername(String telefono) throws UsernameNotFoundException {
    // ❌ BUG: Asume que el parámetro es teléfono, pero puede ser nombre
    Usuario usuario = usuarioRepository.findByTelefono(telefono)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con teléfono: " + telefono));
    
    if (usuario.getActivo() == null || !usuario.getActivo()) {
        throw new UsernameNotFoundException("Usuario inactivo: " + telefono);
    }
    
    actualizarUltimoAcceso(usuario);
    
    return User.withUsername(usuario.getTelefono())
            .password(usuario.getPassword())
            .roles(usuario.getRol())
            .build();
}
```

### ¿Por Qué Falló?

**Flujo de Login:**

1. **login.html** → Campo genérico: `<input name="username">`
2. **AuthController** → Recibe: `@RequestParam String username`
3. **AuthenticationManager** → Llama a: `loadUserByUsername(username)`
4. **UserDetailsServiceImpl** → Busca usuario

**El problema:**

- ❌ El código asumía que `username` siempre es `telefono`
- ✅ En realidad, `username` puede ser `nombre` O `telefono` (dependiendo de lo que ingrese el usuario)
- 🔴 Al cambiar de `findByNombre()` a `findByTelefono()`, los usuarios que ingresaban su nombre ya no podían entrar

### Evidencia del Formulario

```html
<!-- src/main/resources/templates/auth/login.html -->
<div class="mb-3">
    <label for="username" class="form-label">
        <i class="fas fa-user me-2"></i>Usuario
    </label>
    <input type="text" 
           class="form-control" 
           id="username" 
           name="username"  <!-- ⚠️ Campo genérico, no específico de teléfono -->
           placeholder="Ingresa tu usuario"  <!-- ⚠️ Texto genérico -->
           required 
           autofocus>
</div>
```

**Observaciones:**

- El label dice "Usuario" (genérico)
- El placeholder dice "Ingresa tu usuario" (genérico)
- El atributo `name` es "username" (genérico)
- **NO** dice "Ingresa tu teléfono" ni "Ingresa tu nombre"

---

