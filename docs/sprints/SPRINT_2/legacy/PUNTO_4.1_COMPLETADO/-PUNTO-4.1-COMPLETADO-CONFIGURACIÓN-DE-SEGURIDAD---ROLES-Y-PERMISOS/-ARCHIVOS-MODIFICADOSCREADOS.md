## 📁 ARCHIVOS MODIFICADOS/CREADOS

### 1. **SecurityConfig.java** (Modificado)
**Ubicación:** `src/main/java/api/astro/whats_orders_manager/config/SecurityConfig.java`

**Cambios realizados:**

#### a) Configuración granular de permisos:
```java
// Clientes - Visualización para todos, edición solo ADMIN y USER
.requestMatchers("/clientes", "/clientes/").hasAnyRole("ADMIN", "USER", "VENDEDOR", "VISUALIZADOR")
.requestMatchers("/clientes/form", "/clientes/save", "/clientes/delete/**")
    .hasAnyRole("ADMIN", "USER")

// Productos - Visualización para todos, edición solo ADMIN y USER
.requestMatchers("/productos", "/productos/").hasAnyRole("ADMIN", "USER", "VENDEDOR", "VISUALIZADOR")
.requestMatchers("/productos/form", "/productos/save", "/productos/delete/**")
    .hasAnyRole("ADMIN", "USER")

// Facturas - VENDEDOR puede crear, todos pueden ver
.requestMatchers("/facturas", "/facturas/").hasAnyRole("ADMIN", "USER", "VENDEDOR", "VISUALIZADOR")
.requestMatchers("/facturas/form", "/facturas/save")
    .hasAnyRole("ADMIN", "USER", "VENDEDOR")
.requestMatchers("/facturas/delete/**", "/facturas/anular/**")
    .hasAnyRole("ADMIN", "USER")
```

#### b) Módulos administrativos:
```java
// Solo ADMIN
.requestMatchers("/configuracion/**", "/usuarios/**", "/admin/**").hasRole("ADMIN")
```

#### c) Reportes:
```java
// ADMIN y USER
.requestMatchers("/reportes/**").hasAnyRole("ADMIN", "USER")
```

#### d) Manejo de excepciones:
```java
.exceptionHandling(exception -> exception
    .accessDeniedPage("/error/403")
)
```

---

### 2. **CustomErrorController.java** (Creado)
**Ubicación:** `src/main/java/api/astro/whats_orders_manager/controller/CustomErrorController.java`

**Descripción:** Controlador para manejar errores HTTP personalizados

**Métodos implementados:**
- `handleError()` - Maneja todos los errores según código HTTP
- `error403()` - Página específica para acceso denegado
- `error404()` - Página para recurso no encontrado (preparado)
- `error500()` - Página para error interno (preparado)

**Código destacado:**
```java
@Controller
public class CustomErrorController implements ErrorController {
    
    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        
        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            
            if (statusCode == 403) return "error/403";
            if (statusCode == 404) return "error/404";
            if (statusCode == 500) return "error/500";
        }
        
        return "error/error";
    }
    
    @RequestMapping("/error/403")
    public String error403() {
        return "error/403";
    }
}
```

---

### 3. **error/403.html** (Creado)
**Ubicación:** `src/main/resources/templates/error/403.html`

**Descripción:** Página de error personalizada para acceso denegado (HTTP 403)

**Componentes:**

#### a) Diseño visual:
- Icono de candado grande (6rem)
- Código de error "403" en rojo
- Título "Acceso Denegado"
- Mensaje explicativo

#### b) Información del usuario:
```html
<div class="card bg-light mb-4" sec:authorize="isAuthenticated()">
    <div class="card-body">
        <small class="text-muted">
            <strong>Usuario actual:</strong> <span sec:authentication="name"></span><br>
            <strong>Rol:</strong> <span sec:authentication="principal.authorities"></span>
        </small>
    </div>
</div>
```

#### c) Botones de acción:
- "Ir al Dashboard" (botón primario)
- "Volver Atrás" (botón secundario con `history.back()`)

#### d) Tarjeta informativa de roles:
- Lista con los 4 roles del sistema
- Iconos y colores específicos por rol
- Descripción de permisos de cada rol

**Características:**
- ✅ Responsive (móvil, tablet, desktop)
- ✅ Integrado con layout principal (navbar + sidebar)
- ✅ Muestra información del usuario autenticado
- ✅ Mensaje de contacto para usuarios sin privilegios
- ✅ Estilos profesionales con Bootstrap

---

### 4. **usuarios/form.html** (Modificado)
**Ubicación:** `src/main/resources/templates/usuarios/form.html`

**Cambios realizados:**

#### Dropdown de roles actualizado:
```html
<select class="form-select" id="rol" th:field="*{rol}" required>
    <option value="">Selecciona un rol</option>
    <option value="ADMIN">Administrador</option>
    <option value="USER">Usuario</option>
    <option value="VENDEDOR">Vendedor</option>
    <option value="VISUALIZADOR">Visualizador</option>
</select>

<small class="form-text text-muted">
    <strong>Administrador:</strong> Acceso completo al sistema<br>
    <strong>Usuario:</strong> Acceso a módulos operativos y reportes<br>
    <strong>Vendedor:</strong> Solo puede crear facturas y consultar catálogos<br>
    <strong>Visualizador:</strong> Solo lectura de información
</small>
```

---

### 5. **usuarios.css** (Modificado)
**Ubicación:** `src/main/resources/static/css/usuarios.css`

**Cambios realizados:**

#### Badges de rol con colores específicos:
```css
/* Badges de rol con colores específicos */
.badge-rol-admin {
    background-color: #dc3545 !important; /* Rojo */
    color: white;
}

.badge-rol-user {
    background-color: #0d6efd !important; /* Azul */
    color: white;
}

.badge-rol-vendedor {
    background-color: #198754 !important; /* Verde */
    color: white;
}

.badge-rol-visualizador {
    background-color: #6c757d !important; /* Gris */
    color: white;
}
```

---

### 6. **usuarios/usuarios.html** (Modificado)
**Ubicación:** `src/main/resources/templates/usuarios/usuarios.html`

**Cambios realizados:**

#### Badge de rol con color dinámico:
```html
<span class="badge" 
      th:classappend="${usuario.rol == 'ADMIN' ? 'badge-rol-admin' : 
                       usuario.rol == 'USER' ? 'badge-rol-user' : 
                       usuario.rol == 'VENDEDOR' ? 'badge-rol-vendedor' : 
                       'badge-rol-visualizador'}"
      th:text="${usuario.rol}">
    USER
</span>
```

---

