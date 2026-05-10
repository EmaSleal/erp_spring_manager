## 📁 ARCHIVOS CREADOS

### 1. **UsuarioController.java** (~500 líneas)
**Ubicación:** `src/main/java/api/astro/facturacion/controller/UsuarioController.java`

**Descripción:** Controlador completo para gestión de usuarios con seguridad ADMIN

**Endpoints implementados:**
```java
GET  /usuarios                      → Lista con paginación manual
GET  /usuarios/form                 → Formulario nuevo usuario
GET  /usuarios/form/{id}            → Formulario editar usuario
POST /usuarios/save                 → Guardar/actualizar usuario
DELETE /usuarios/delete/{id}        → Eliminar usuario (AJAX)
POST /usuarios/toggle-active/{id}   → Activar/desactivar (AJAX)
POST /usuarios/reset-password/{id}  → Resetear contraseña (AJAX)
```

**Características principales:**
- ✅ Paginación manual con filtros (search, rol, activo, sortBy, sortDir)
- ✅ Estadísticas calculadas (total, activos, admins, inactivos)
- ✅ Validaciones de negocio (teléfono único, email único)
- ✅ Protección: usuario no puede eliminarse/desactivarse a sí mismo
- ✅ BCrypt para encriptar contraseñas
- ✅ Generador de contraseñas seguras (12 caracteres, SecureRandom)
- ✅ Manejo de errores y mensajes flash
- ✅ @PreAuthorize("hasRole('ADMIN')") en toda la clase

**Métodos destacados:**
```java
// Paginación manual implementada desde cero
listarUsuarios(search, rol, activo, sortBy, sortDir, page, size)

// Generador de contraseñas criptográficamente seguro
generarPasswordAleatoria() → 12 chars, A-Za-z0-9@#$%

// Validación de unicidad
if (usuarioRepository.findByTelefono(telefono).isPresent()) {
    throw new IllegalArgumentException("Ya existe un usuario con ese teléfono");
}
```

---

### 2. **usuarios/usuarios.html** (~480 líneas)
**Ubicación:** `src/main/resources/templates/usuarios/usuarios.html`

**Descripción:** Vista principal con tabla, filtros, paginación y estadísticas

**Componentes principales:**
- ✅ **Tarjetas de estadísticas** (4 cards con iconos y colores)
  - Total usuarios (azul)
  - Usuarios activos (verde)
  - Administradores (amarillo)
  - Usuarios inactivos (rojo)

- ✅ **Filtros avanzados** (formulario con 5 campos)
  - Búsqueda general (nombre, teléfono, email)
  - Filtro por rol (ADMIN/USER)
  - Filtro por estado (Activo/Inactivo)
  - Ordenamiento (createDate, nombre, rol)
  - Dirección (ASC/DESC)

- ✅ **Tabla de usuarios**
  - Avatares circulares con inicial
  - Badge "Tú" para usuario actual (fila destacada)
  - Badges de rol (ADMIN amarillo, USER azul)
  - Badges de estado (Activo verde, Inactivo rojo)
  - Botones de acción en grupo:
    - Editar (azul)
    - Toggle activo/inactivo (amarillo/verde)
    - Reset password (celeste)
    - Eliminar (rojo)

- ✅ **Paginación completa**
  - Primera página (<<)
  - Página anterior (<)
  - Páginas numeradas (máx 5 visibles)
  - Página siguiente (>)
  - Última página (>>)
  - Info: "Página X de Y (Mostrando N de Total usuarios)"

- ✅ **Modal Reset Password**
  - Confirmación con mensaje de advertencia
  - Campo de texto con nueva contraseña (monospace)
  - Botón copiar al portapapeles
  - Estados: antes/después de generar

**Características especiales:**
- Responsive design (oculta columnas en móvil)
- Tooltips en todos los botones
- Mensajes de éxito/error con dismissible alert
- Protección: botones deshabilitados para cuenta propia

---

### 3. **usuarios/form.html** (~380 líneas)
**Ubicación:** `src/main/resources/templates/usuarios/form.html`

**Descripción:** Formulario crear/editar con validaciones y ayuda contextual

**Campos del formulario:**
- ✅ **Nombre completo** (text, 3-100 caracteres, requerido)
- ✅ **Teléfono** (tel, 9 dígitos, pattern, requerido, único)
- ✅ **Email** (email, max 100, requerido, único)
- ✅ **Contraseña** (password, min 6, solo en creación)
  - Botón toggle visibilidad (ojo)
  - Botón generar contraseña segura
- ✅ **Confirmar contraseña** (password, debe coincidir)
- ✅ **Rol** (select: ADMIN/USER, requerido)
- ✅ **Estado** (switch activo/inactivo con label dinámico)

**Características especiales:**
- Validaciones HTML5 + backend
- Input groups con iconos (Bootstrap Icons)
- Preview en tiempo real del estado
- Sidebar con ayuda contextual:
  - Información sobre roles
  - Requisitos de contraseñas
  - Formato de teléfono
- Notas importantes en modo edición
- Breadcrumbs de navegación

**Validaciones implementadas:**
```html
<!-- Ejemplo de validación -->
<input type="tel" 
       pattern="[0-9]{9}" 
       required 
       minlength="9" 
       maxlength="9">
```

---

### 4. **usuarios.css** (~340 líneas)
**Ubicación:** `src/main/resources/static/css/usuarios.css`

**Descripción:** Estilos completos para módulo de usuarios

**Componentes estilizados:**

**a) Tarjetas de estadísticas:**
```css
.stats-card {
    border-radius: 10px;
    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    transition: transform 0.2s, box-shadow 0.2s;
}
.stats-card:hover {
    transform: translateY(-5px);
    box-shadow: 0 4px 8px rgba(0,0,0,0.15);
}
```

**b) Avatares circulares:**
```css
.avatar-circle {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background-color: #success / #secondary;
    display: flex;
    align-items: center;
    justify-content: center;
}
```

**c) Tabla con efectos:**
```css
#usuariosTable tbody tr:hover {
    background-color: rgba(0,123,255,0.05);
}
#usuariosTable tbody tr.table-info {
    background-color: rgba(13,202,240,0.15);
}
```

**d) Badges personalizados:**
- Rol ADMIN: bg-warning text-dark
- Rol USER: bg-info
- Activo: bg-success
- Inactivo: bg-danger

**e) Switch personalizado:**
```css
.form-check-input {
    width: 3rem;
    height: 1.5rem;
}
.form-check-input:checked {
    background-color: #198754;
}
```

**f) Animaciones:**
```css
@keyframes fadeIn {
    from { opacity: 0; transform: translateY(-10px); }
    to { opacity: 1; transform: translateY(0); }
}
@keyframes spinner {
    to { transform: rotate(360deg); }
}
```

**g) Responsive design:**
- Móvil: oculta iconos de estadísticas
- Móvil: reduce tamaño de avatares
- Móvil pequeño: oculta columnas teléfono y email

**h) Print styles:**
- Oculta botones, breadcrumbs, paginación
- Tabla en blanco y negro

---

### 5. **usuarios.js** (~550 líneas)
**Ubicación:** `src/main/resources/static/js/usuarios.js`

**Descripción:** JavaScript completo con validaciones, AJAX y generadores

**Funciones principales:**

**a) Inicialización:**
```javascript
$(document).ready(function() {
    initTooltips();
    setupFormValidation();
    setupTableEvents();
    setupResetPasswordModal();
    setupEstadoSwitch();
});
```

**b) Validación de formularios:**
- `validateField(field)` → Valida campo individual
- `validatePasswordMatch()` → Verifica coincidencia de contraseñas
- `validateForm()` → Valida formulario completo
- Validación en tiempo real (blur y input events)
- Estados: `.is-valid` y `.is-invalid`

**c) Generador de contraseñas:**
```javascript
function generarPasswordSegura() {
    const length = 12;
    const charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@#$%";
    
    // Asegurar al menos un carácter de cada tipo
    password += mayúscula + minúscula + número + símbolo;
    
    // Completar y mezclar aleatoriamente
    password = shuffle(password);
}
```

**d) AJAX para eliminar:**
```javascript
function eliminarUsuario(id) {
    $.ajax({
        url: `/usuarios/delete/${id}`,
        type: 'DELETE',
        beforeSend: function(xhr) {
            xhr.setRequestHeader(csrfHeader, csrfToken);
        },
        success: function() {
            Swal.fire('¡Eliminado!', ...).then(() => location.reload());
        },
        error: function(xhr) {
            Swal.fire('Error', xhr.responseText, 'error');
        }
    });
}
```

**e) AJAX para toggle estado:**
- Confirmación con SweetAlert2 (diferentes colores según acción)
- Mensajes contextuales (activar/desactivar)
- Reload automático después de éxito

**f) Modal reset password:**
- Abrir modal con datos del usuario
- Botón confirmar → AJAX POST
- Mostrar nueva contraseña generada
- Botón copiar al portapapeles con feedback
- Estados: antes/después de generar

**g) Toggle visibilidad de contraseña:**
```javascript
function setupPasswordToggle(btnId, inputId) {
    btn.click(() => {
        input.type = input.type === 'password' ? 'text' : 'password';
        icon.toggleClass('bi-eye-fill bi-eye-slash-fill');
    });
}
```

**h) Switch de estado dinámico:**
```javascript
estadoSwitch.addEventListener('change', function() {
    if (this.checked) {
        estadoLabel.textContent = 'Usuario activo';
        estadoLabel.classList.add('text-success');
    } else {
        estadoLabel.textContent = 'Usuario inactivo';
        estadoLabel.classList.add('text-danger');
    }
});
```

**i) Alertas toast:**
```javascript
function mostrarAlerta(mensaje, tipo = 'info') {
    Swal.fire({
        text: mensaje,
        icon: tipo,
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 3000,
        timerProgressBar: true
    });
}
```

---

