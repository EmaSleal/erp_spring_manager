## 🎯 Puntos Completados

### ✅ 4.1 - Extensión del Modelo Usuario
**Archivo:** `Usuario.java`

Se agregaron **4 nuevos campos** al modelo Usuario:

```java
// Campo 1: Email único
@Column(name = "email", unique = true, length = 100)
private String email;

// Campo 2: Avatar (ruta del archivo)
@Column(name = "avatar", length = 255)
private String avatar;

// Campo 3: Estado activo/inactivo
@Column(name = "activo")
private Boolean activo = true;

// Campo 4: Último acceso
@Column(name = "ultimo_acceso")
private Timestamp ultimoAcceso;
```

**Script de migración:** `MIGRATION_USUARIO_FASE_4.sql`
- 4 comandos ALTER TABLE
- Script de migración de datos existentes
- Índices opcionales para performance

---

### ✅ 4.2 - PerfilController
**Archivo:** `PerfilController.java` (400+ líneas)

**6 Endpoints implementados:**

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/perfil` | Ver perfil del usuario autenticado |
| GET | `/perfil/editar` | Formulario de edición |
| POST | `/perfil/actualizar` | Actualizar datos personales |
| POST | `/perfil/cambiar-password` | Cambiar contraseña |
| POST | `/perfil/subir-avatar` | Subir foto de perfil |
| POST | `/perfil/eliminar-avatar` | Eliminar foto de perfil |

**Características clave:**

#### 🔐 Seguridad
- Autenticación mediante `Authentication` de Spring Security
- Obtención del usuario actual desde el contexto de seguridad
- Validación de contraseña actual con `BCryptPasswordEncoder`
- Codificación de nueva contraseña con BCrypt

#### ✅ Validaciones
1. **Email:**
   - Formato válido (regex)
   - Único en el sistema
   - Opcional (puede estar vacío)

2. **Teléfono:**
   - Único en el sistema
   - Obligatorio

3. **Contraseña:**
   - Contraseña actual correcta
   - Nueva contraseña mínimo 6 caracteres
   - Confirmación de contraseña coincide

4. **Avatar:**
   - Tipo de archivo: solo imágenes (JPG, JPEG, PNG, GIF)
   - Tamaño máximo: 2MB
   - Validación de extensión

#### 📁 Gestión de Archivos
- Directorio de upload: `src/main/resources/static/images/avatars/`
- Nomenclatura: `avatar_{userId}_{UUID}.{extension}`
- Eliminación de avatar anterior al subir uno nuevo
- Verificación de existencia de archivo antes de eliminar

#### 📊 Logging
```java
@Slf4j
log.info("Usuario {} actualizó su perfil", usuario.getNombre());
log.info("Usuario {} cambió su contraseña", usuario.getNombre());
log.warn("Intento de cambio de contraseña con contraseña incorrecta");
```

#### 🔄 Mensajes Flash
```java
redirectAttributes.addFlashAttribute("success", "Perfil actualizado correctamente");
redirectAttributes.addFlashAttribute("error", "El email ya está en uso");
redirectAttributes.addFlashAttribute("errorPassword", "Contraseña actual incorrecta");
redirectAttributes.addFlashAttribute("errorAvatar", "Archivo demasiado grande");
```

**Extensiones realizadas:**

1. **UsuarioService.java:**
   ```java
   Optional<Usuario> findByEmail(String email);
   ```

2. **UsuarioServiceImpl.java:**
   ```java
   @Override
   public Optional<Usuario> findByEmail(String email) {
       return usuarioRepository.findByEmail(email);
   }
   ```

3. **UsuarioRepository.java:**
   ```java
   Optional<Usuario> findByEmail(String email);
   // Spring Data JPA auto-implementa el método
   ```

---

### ✅ 4.3 - Vista de Perfil (Solo Lectura)
**Archivo:** `templates/perfil/ver.html`

**Diseño visual:**
- ✨ Card moderno con sombras y bordes redondeados
- 🎨 Header con gradiente morado (667eea → 764ba2)
- 👤 Avatar circular de 150px con borde blanco
- 📱 Responsive design (mobile-first)

**Avatar dinámico:**
```html
<!-- Si existe avatar -->
<img th:if="${usuario.avatar != null}" th:src="@{${usuario.avatar}}">

<!-- Si no existe, mostrar iniciales -->
<div th:unless="${usuario.avatar != null}" class="avatar-initials">
    <span th:text="${iniciales}">AB</span>
</div>
```

**Secciones de información:**

1. **Información Personal:**
   - 👤 Nombre completo
   - 📧 Email (o "No configurado")
   - 📱 Teléfono
   - 🏷️ Rol
   - 🔄 Estado (badge verde "Activo" / rojo "Inactivo")

2. **Información de Cuenta:**
   - 📅 Fecha de registro
   - 🕐 Último acceso
   - ✏️ Última modificación
   - Formato: `dd/MM/yyyy HH:mm`

**Mensajes flash:**
- ✅ Success (verde, con icono check-circle)
- ❌ Error (rojo, con icono exclamation-circle)
- ⏱️ Auto-hide después de 5 segundos
- ❌ Botón de cerrar manual

**Botones de acción:**
```html
<a th:href="@{/perfil/editar}" class="btn btn-primary">
    <i class="fas fa-edit"></i> Editar Perfil
</a>
<a th:href="@{/dashboard}" class="btn btn-outline-secondary">
    <i class="fas fa-arrow-left"></i> Volver al Dashboard
</a>
```

---

### ✅ 4.4 - Vista de Edición (Formularios)
**Archivo:** `templates/perfil/editar.html`

**Sistema de Tabs (Bootstrap 5):**

#### 📑 Tab 1: Información Personal
```html
<form th:action="@{/perfil/actualizar}" method="post">
    <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">
    
    <!-- Nombre -->
    <input type="text" name="nombre" required minlength="3" maxlength="100">
    
    <!-- Teléfono -->
    <input type="tel" name="telefono" required pattern="[0-9+\-\s()]+" maxlength="20">
    
    <!-- Email -->
    <input type="email" name="email" maxlength="100">
    
    <!-- Rol (disabled) -->
    <input type="text" th:value="${usuario.rol}" disabled readonly>
</form>
```

**Validaciones HTML5:**
- `required` en campos obligatorios
- `minlength` y `maxlength` para longitud
- `pattern` para formato de teléfono
- `type="email"` para validación de email

#### 🔐 Tab 2: Cambiar Contraseña
```html
<form th:action="@{/perfil/cambiar-password}" method="post">
    <!-- Contraseña actual -->
    <input type="password" name="currentPassword" required>
    
    <!-- Nueva contraseña -->
    <input type="password" name="newPassword" required minlength="6">
    
    <!-- Confirmar contraseña -->
    <input type="password" name="confirmPassword" required minlength="6">
</form>
```

**Validaciones JavaScript:**

1. **Indicador de fortaleza:**
   ```javascript
   // 5 niveles de fortaleza
   if (strength <= 2) {
       strengthDiv.textContent = '⚠️ Contraseña débil';
       strengthDiv.className = 'strength-weak';
   } else if (strength <= 3) {
       strengthDiv.textContent = '✓ Contraseña media';
       strengthDiv.className = 'strength-medium';
   } else {
       strengthDiv.textContent = '✓✓ Contraseña fuerte';
       strengthDiv.className = 'strength-strong';
   }
   ```

2. **Validador de coincidencia:**
   ```javascript
   if (newPassword === confirmPassword) {
       matchDiv.textContent = '✓ Las contraseñas coinciden';
       matchDiv.style.color = '#28a745';
   } else {
       matchDiv.textContent = '✗ Las contraseñas no coinciden';
       matchDiv.style.color = '#dc3545';
   }
   ```

#### 🖼️ Tab 3: Foto de Perfil
```html
<!-- Upload Form -->
<form th:action="@{/perfil/subir-avatar}" method="post" enctype="multipart/form-data">
    <input type="file" name="avatar" accept="image/*" required>
    <button type="submit">Subir Foto</button>
</form>

<!-- Delete Form (solo si existe avatar) -->
<form th:if="${usuario.avatar != null}" th:action="@{/perfil/eliminar-avatar}" method="post">
    <button type="submit" onclick="return confirm('¿Estás seguro?')">
        Eliminar Foto Actual
    </button>
</form>
```

**Validaciones de archivo:**
```javascript
// Tamaño máximo: 2MB
const maxSize = 2 * 1024 * 1024;
if (file.size > maxSize) {
    alert('La imagen es demasiado grande. El tamaño máximo es 2MB.');
    return false;
}

// Tipos permitidos
const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif'];
if (!validTypes.includes(file.type)) {
    alert('Formato de imagen no válido.');
    return false;
}
```

**Preview de imagen:**
```javascript
const reader = new FileReader();
reader.onload = function(event) {
    const avatarImg = document.getElementById('avatarPreviewImg');
    avatarImg.src = event.target.result; // Muestra preview antes de subir
};
reader.readAsDataURL(file);
```

**Auto-switch de tabs según error:**
```javascript
// Si hay error de contraseña, ir a tab de contraseña
if (errorPassword) {
    const passwordTab = new bootstrap.Tab(document.getElementById('password-tab'));
    passwordTab.show();
}

// Si hay error de avatar, ir a tab de avatar
if (errorAvatar) {
    const avatarTab = new bootstrap.Tab(document.getElementById('avatar-tab'));
    avatarTab.show();
}
```

---

