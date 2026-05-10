## 🔐 SEGURIDAD IMPLEMENTADA

### 1. **Control de Acceso**
```java
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {
    // Solo usuarios con rol ADMIN pueden acceder
}
```

### 2. **Protección CSRF**
- Tokens CSRF en todos los formularios POST
- AJAX incluye headers CSRF:
```javascript
beforeSend: function(xhr) {
    xhr.setRequestHeader(csrfHeader, csrfToken);
}
```

### 3. **Validación de Unicidad**
```java
// En guardarUsuario()
Optional<Usuario> usuarioExistente = usuarioRepository.findByTelefono(usuario.getTelefono());
if (usuarioExistente.isPresent() && !usuarioExistente.get().getId().equals(usuario.getId())) {
    throw new IllegalArgumentException("Ya existe un usuario con ese teléfono");
}
```

### 4. **Protección de Cuenta Propia**
```java
// No puede eliminarse a sí mismo
if (id.equals(usuarioActual.getId())) {
    return ResponseEntity.badRequest()
        .body("No puedes eliminar tu propia cuenta");
}

// No puede desactivarse a sí mismo
if (id.equals(usuarioActual.getId())) {
    return ResponseEntity.badRequest()
        .body("No puedes desactivar tu propia cuenta");
}
```

### 5. **Encriptación de Contraseñas**
```java
// BCrypt para nuevas contraseñas
String passwordEncriptada = passwordEncoder.encode(usuario.getPassword());
usuario.setPassword(passwordEncriptada);

// Generador con SecureRandom
SecureRandom random = new SecureRandom();
```

### 6. **Restricción en Vistas**
```html
<div sec:authorize="hasRole('ADMIN')">
    <a th:href="@{/usuarios/form}" class="btn btn-primary">
        Nuevo Usuario
    </a>
</div>
```

---

