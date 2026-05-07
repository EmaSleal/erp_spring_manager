##  SIDEBAR COMPONENT

###  Ubicación
```
templates/components/sidebar.html
static/css/sidebar.css
static/js/sidebar.js
```

###  Propósito
Menú lateral colapsable con:
- Módulos principales del sistema
- Iconos Font Awesome
- Estados (activo, próximamente)
- Persistencia del estado (LocalStorage)

### Uso

#### **Incluir en tu plantilla:**
```html
<div th:replace="~{components/sidebar :: sidebar}"></div>
```

### Estructura HTML
```html
<aside class="sidebar" id="sidebar">
    <!-- Toggle Button -->
    <button class="sidebar-toggle" id="sidebarToggle">
        <i class="fas fa-bars"></i>
    </button>
    
    <!-- Menu Items -->
    <nav class="sidebar-nav">
        <!-- Módulo Activo -->
        <a href="/clientes" class="menu-link">
            <i class="fas fa-users"></i>
            <span class="menu-text">Clientes</span>
        </a>
        
        <!-- Módulo Próximamente -->
        <a href="#" class="menu-link disabled" 
           data-tooltip="Próximamente">
            <i class="fas fa-chart-bar"></i>
            <span class="menu-text">Reportes</span>
            <span class="badge-soon">Pronto</span>
        </a>
    </nav>
</aside>
```

###  Estados del Sidebar

#### **1. Expandido (por defecto):**
```css
.sidebar {
    width: 260px;
    transition: width 0.3s;
}
```

#### **2. Colapsado:**
```css
.sidebar.collapsed {
    width: 60px;
}

.sidebar.collapsed .menu-text,
.sidebar.collapsed .badge-soon {
    display: none;
}
```

#### **3. Oculto en móvil:**
```css
@media (max-width: 767px) {
    .sidebar {
        transform: translateX(-100%);
    }
    
    .sidebar.active {
        transform: translateX(0);
    }
}
```

###  JavaScript (sidebar.js)

```javascript
// Toggle sidebar
const toggle = document.getElementById('sidebarToggle');
const sidebar = document.getElementById('sidebar');

toggle.addEventListener('click', () => {
    sidebar.classList.toggle('collapsed');
    
    // Guardar estado en LocalStorage
    const isCollapsed = sidebar.classList.contains('collapsed');
    localStorage.setItem('sidebarCollapsed', isCollapsed);
});

// Restaurar estado al cargar
window.addEventListener('DOMContentLoaded', () => {
    const isCollapsed = localStorage.getItem('sidebarCollapsed') === 'true';
    if (isCollapsed) {
        sidebar.classList.add('collapsed');
    }
});
```

###  Agregar Nuevo Módulo

```html
<!-- Módulo activo -->
<a href="/ruta-modulo" 
   class="menu-link" 
   th:classappend="${#strings.startsWith(#httpServletRequest.requestURI, '/ruta-modulo')} ? 'active' : ''">
    <i class="fas fa-[icono]"></i>
    <span class="menu-text">Nombre Módulo</span>
</a>

<!-- Módulo próximamente -->
<a href="#" 
   class="menu-link disabled" 
   data-tooltip="Próximamente"
   onclick="mostrarAlertaProximamente(); return false;">
    <i class="fas fa-[icono]"></i>
    <span class="menu-text">Nombre Módulo</span>
    <span class="badge-soon">Pronto</span>
</a>
```

###  Colores por Módulo

```css
/* Clientes - Azul */
.menu-link[href="/clientes"]:hover {
    background-color: rgba(33, 150, 243, 0.1);
    border-left-color: #2196F3;
}

/* Productos - Verde */
.menu-link[href="/productos"]:hover {
    background-color: rgba(76, 175, 80, 0.1);
    border-left-color: #4CAF50;
}

/* Facturas - Naranja */
.menu-link[href="/facturas"]:hover {
    background-color: rgba(255, 152, 0, 0.1);
    border-left-color: #FF9800;
}
```

---

