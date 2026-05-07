##  NAVBAR COMPONENT

###  Ubicación
```
templates/components/navbar.html
static/css/navbar.css
static/js/navbar.js
```

###  Propósito
Barra de navegación superior con:
- Logo (link al dashboard)
- Dropdown de usuario
- Avatar con iniciales
- Responsive (hamburger menu en móvil)

###  Uso

#### **Incluir en tu plantilla:**
```html
<div th:replace="~{components/navbar :: navbar}"></div>
```

#### **Variables Thymeleaf necesarias:**
```java
// En tu Controller
model.addAttribute("usuario", usuarioActual);
```

###  Estructura HTML
```html
<nav class="navbar">
    <!-- Logo -->
    <div class="navbar-brand">
        <a href="/dashboard">
            <i class="fas fa-store"></i>
            <span>WhatsApp Orders</span>
        </a>
    </div>
    
    <!-- Usuario Dropdown -->
    <div class="navbar-user">
        <button class="user-dropdown-toggle">
            <div class="avatar">
                <span th:text="${usuario.nombre.charAt(0)}">U</span>
            </div>
            <span th:text="${usuario.nombre}">Usuario</span>
        </button>
        
        <div class="dropdown-menu">
            <a href="/perfil">Mi Perfil</a>
            <a href="/configuracion">Configuración</a>
            <hr>
            <a href="#" onclick="logout()">Cerrar Sesión</a>
        </div>
    </div>
</nav>
```

###  Configuración

#### **Colores (navbar.css):**
```css
:root {
    --navbar-bg: #1976D2;          /* Azul Material Design */
    --navbar-text: white;
    --navbar-hover: rgba(255,255,255,0.1);
}
```

#### **JavaScript (navbar.js):**
```javascript
// Logout con CSRF token
function logout() {
    const token = document.querySelector('meta[name="_csrf"]').content;
    const header = document.querySelector('meta[name="_csrf_header"]').content;
    
    fetch('/auth/logout', {
        method: 'POST',
        headers: {
            [header]: token
        }
    }).then(() => {
        window.location.href = '/auth/login?logout';
    });
}
```

###  Responsive

```css
/* Desktop: Full navbar */
@media (min-width: 768px) {
    .navbar { padding: 0 2rem; }
}

/* Mobile: Compact navbar */
@media (max-width: 767px) {
    .navbar { padding: 0 1rem; }
    .navbar-brand span { display: none; } /* Solo icono */
}
```

###  Checklist de Integración
- [ ] Incluir `navbar.html` con `th:replace`
- [ ] Pasar objeto `usuario` desde controller
- [ ] Incluir `navbar.css` en `layout.html`
- [ ] Incluir `navbar.js` en `layout.html`
- [ ] Verificar CSRF tokens en meta tags

---

