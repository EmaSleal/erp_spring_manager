# 📦 COMPONENTES REUTILIZABLES

**Proyecto:** WhatsApp Orders Manager  
**Versión:** 1.0.0  
**Fecha:** 13/10/2025  

---

## 📋 ÍNDICE

1. [Navbar Component](#navbar-component)
2. [Sidebar Component](#sidebar-component)
3. [Breadcrumbs](#breadcrumbs)
4. [Module Cards](#module-cards)
5. [Widgets Dashboard](#widgets-dashboard)
6. [Tables Responsive](#tables-responsive)
7. [Forms Validation](#forms-validation)
8. [Modales](#modales)
9. [Alerts y Notificaciones](#alerts-y-notificaciones)

---

## 1️⃣ NAVBAR COMPONENT

### 📍 Ubicación
```
templates/components/navbar.html
static/css/navbar.css
static/js/navbar.js
```

### 🎯 Propósito
Barra de navegación superior con:
- Logo (link al dashboard)
- Dropdown de usuario
- Avatar con iniciales
- Responsive (hamburger menu en móvil)

### 📝 Uso

#### **Incluir en tu plantilla:**
```html
<div th:replace="~{components/navbar :: navbar}"></div>
```

#### **Variables Thymeleaf necesarias:**
```java
// En tu Controller
model.addAttribute("usuario", usuarioActual);
```

### 🎨 Estructura HTML
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

### ⚙️ Configuración

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

### 📱 Responsive

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

### ✅ Checklist de Integración
- [ ] Incluir `navbar.html` con `th:replace`
- [ ] Pasar objeto `usuario` desde controller
- [ ] Incluir `navbar.css` en `layout.html`
- [ ] Incluir `navbar.js` en `layout.html`
- [ ] Verificar CSRF tokens en meta tags

---

## 2️⃣ SIDEBAR COMPONENT

### 📍 Ubicación
```
templates/components/sidebar.html
static/css/sidebar.css
static/js/sidebar.js
```

### 🎯 Propósito
Menú lateral colapsable con:
- Módulos principales del sistema
- Iconos Font Awesome
- Estados (activo, próximamente)
- Persistencia del estado (LocalStorage)

### 📝 Uso

#### **Incluir en tu plantilla:**
```html
<div th:replace="~{components/sidebar :: sidebar}"></div>
```

### 🎨 Estructura HTML
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

### ⚙️ Estados del Sidebar

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

### 🔧 JavaScript (sidebar.js)

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

### 🎨 Agregar Nuevo Módulo

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

### 🎨 Colores por Módulo

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

## 3️⃣ BREADCRUMBS

### 📍 Ubicación
```
static/css/common.css (estilos)
```

### 🎯 Propósito
Navegación jerárquica que muestra:
- Ruta actual del usuario
- Links a niveles superiores
- 2 o 3 niveles de profundidad

### 📝 Uso

#### **Breadcrumbs de 2 niveles:**
```html
<nav aria-label="breadcrumb" class="mb-3">
    <ol class="breadcrumb">
        <li class="breadcrumb-item">
            <a th:href="@{/dashboard}" class="text-decoration-none">
                <i class="fas fa-home me-1"></i>Dashboard
            </a>
        </li>
        <li class="breadcrumb-item active" aria-current="page">
            <i class="fas fa-users me-1"></i>Clientes
        </li>
    </ol>
</nav>
```

#### **Breadcrumbs de 3 niveles:**
```html
<nav aria-label="breadcrumb" class="mb-3">
    <ol class="breadcrumb">
        <!-- Nivel 1: Dashboard -->
        <li class="breadcrumb-item">
            <a th:href="@{/dashboard}">
                <i class="fas fa-home me-1"></i>Dashboard
            </a>
        </li>
        
        <!-- Nivel 2: Módulo -->
        <li class="breadcrumb-item">
            <a th:href="@{/clientes}">
                <i class="fas fa-users me-1"></i>Clientes
            </a>
        </li>
        
        <!-- Nivel 3: Acción (activo) -->
        <li class="breadcrumb-item active" aria-current="page">
            <i class="fas fa-edit me-1"></i>Editar Cliente
        </li>
    </ol>
</nav>
```

### 🎨 Estilos (common.css)

```css
.breadcrumb {
    background-color: #F8F9FA;    /* Gris claro */
    border: 1px solid #E9ECEF;    /* Borde sutil */
    padding: 0.75rem 1rem;
    border-radius: 0.375rem;
    margin-bottom: 1rem;
}

.breadcrumb-item a {
    color: var(--primary-color);  /* Azul #1976D2 */
    font-weight: 500;
    text-decoration: none;
}

.breadcrumb-item a:hover {
    color: var(--primary-dark);
    text-decoration: underline;
}

.breadcrumb-item.active {
    color: #495057;               /* Gris oscuro */
    font-weight: 600;
}

.breadcrumb-item + .breadcrumb-item::before {
    content: "/";
    color: #ADB5BD;
}
```

### 📦 Iconos Font Awesome Recomendados

| Módulo | Icono | Código |
|--------|-------|--------|
| Dashboard | 🏠 | `fa-home` |
| Clientes | 👥 | `fa-users` |
| Productos | 📦 | `fa-box` |
| Facturas | 📄 | `fa-file-invoice` |
| Perfil | 👤 | `fa-user` |
| Reportes | 📊 | `fa-chart-bar` |
| Configuración | ⚙️ | `fa-cog` |
| Agregar | ➕ | `fa-plus` |
| Editar | ✏️ | `fa-edit` |
| Ver | 🔍 | `fa-eye` |

---

## 4️⃣ MODULE CARDS

### 📍 Ubicación
```
templates/dashboard/dashboard.html
static/css/dashboard.css
```

### 🎯 Propósito
Tarjetas de módulos en el dashboard con:
- Icono representativo
- Nombre del módulo
- Estados (activo, próximamente)
- Responsive grid

### 📝 Uso

```html
<div class="modules-grid">
    <!-- Módulo Activo -->
    <a th:href="@{/clientes}" class="module-card">
        <div class="module-icon" style="background-color: #2196F3;">
            <i class="fas fa-users"></i>
        </div>
        <h3 class="module-title">Clientes</h3>
        <p class="module-description">Gestión de clientes</p>
    </a>
    
    <!-- Módulo Próximamente -->
    <div class="module-card disabled">
        <div class="module-icon" style="background-color: #9E9E9E;">
            <i class="fas fa-chart-bar"></i>
        </div>
        <h3 class="module-title">Reportes</h3>
        <p class="module-description">Estadísticas</p>
        <span class="badge-soon">Próximamente</span>
    </div>
</div>
```

### 🎨 Estilos (dashboard.css)

```css
.modules-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 1.5rem;
    margin-top: 2rem;
}

.module-card {
    background: white;
    border-radius: 12px;
    padding: 1.5rem;
    text-align: center;
    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
    transition: all 0.3s ease;
    cursor: pointer;
    text-decoration: none;
    color: inherit;
    position: relative;
}

.module-card:hover:not(.disabled) {
    transform: translateY(-5px);
    box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

.module-icon {
    width: 60px;
    height: 60px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 1rem;
}

.module-icon i {
    font-size: 1.8rem;
    color: white;
}

.module-title {
    font-size: 1.1rem;
    font-weight: 600;
    margin-bottom: 0.5rem;
    color: #212121;
}

.module-description {
    font-size: 0.9rem;
    color: #757575;
    margin: 0;
}

.module-card.disabled {
    opacity: 0.6;
    cursor: not-allowed;
}

.badge-soon {
    position: absolute;
    top: 10px;
    right: 10px;
    background: #FF9800;
    color: white;
    padding: 0.25rem 0.5rem;
    border-radius: 12px;
    font-size: 0.7rem;
    font-weight: 600;
}
```

### 🎨 Paleta de Colores Recomendada

| Módulo | Color | Hex |
|--------|-------|-----|
| Clientes | Azul | `#2196F3` |
| Productos | Verde | `#4CAF50` |
| Facturas | Naranja | `#FF9800` |
| Perfil | Morado | `#9C27B0` |
| Reportes | Cian | `#00BCD4` |
| Configuración | Gris | `#607D8B` |
| Inventario | Teal | `#009688` |
| Usuarios | Índigo | `#3F51B5` |

### 📱 Responsive

```css
/* Desktop: 4-5 columnas */
@media (min-width: 992px) {
    .modules-grid {
        grid-template-columns: repeat(5, 1fr);
    }
}

/* Tablet: 3 columnas */
@media (min-width: 768px) and (max-width: 991px) {
    .modules-grid {
        grid-template-columns: repeat(3, 1fr);
    }
}

/* Mobile: 2 columnas */
@media (max-width: 767px) {
    .modules-grid {
        grid-template-columns: repeat(2, 1fr);
        gap: 1rem;
    }
}
```

---

## 5️⃣ WIDGETS DASHBOARD

### 📍 Ubicación
```
templates/dashboard/dashboard.html
static/css/dashboard.css
static/js/dashboard.js
```

### 🎯 Propósito
Widgets de estadísticas con:
- Título
- Valor numérico
- Icono
- Color personalizado

### 📝 Uso

```html
<div class="stats-grid">
    <!-- Widget 1 -->
    <div class="stat-card">
        <div class="stat-icon" style="background-color: #2196F3;">
            <i class="fas fa-users"></i>
        </div>
        <div class="stat-content">
            <div class="stat-value" th:text="${totalClientes}">0</div>
            <div class="stat-label">Clientes</div>
        </div>
    </div>
    
    <!-- Widget 2 -->
    <div class="stat-card">
        <div class="stat-icon" style="background-color: #4CAF50;">
            <i class="fas fa-box"></i>
        </div>
        <div class="stat-content">
            <div class="stat-value" th:text="${totalProductos}">0</div>
            <div class="stat-label">Productos</div>
        </div>
    </div>
</div>
```

### 🎨 Estilos (dashboard.css)

```css
.stats-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 1.5rem;
    margin-bottom: 2rem;
}

.stat-card {
    background: white;
    border-radius: 12px;
    padding: 1.5rem;
    display: flex;
    align-items: center;
    gap: 1rem;
    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.stat-icon {
    width: 60px;
    height: 60px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
}

.stat-icon i {
    font-size: 1.8rem;
    color: white;
}

.stat-content {
    flex: 1;
}

.stat-value {
    font-size: 2rem;
    font-weight: 700;
    color: #212121;
    line-height: 1;
}

.stat-label {
    font-size: 0.9rem;
    color: #757575;
    margin-top: 0.25rem;
}
```

### 🔄 Auto-refresh (dashboard.js)

```javascript
// Actualizar estadísticas cada 30 segundos
setInterval(() => {
    fetch('/api/dashboard/stats')
        .then(res => res.json())
        .then(data => {
            document.querySelectorAll('.stat-value').forEach((el, index) => {
                el.textContent = Object.values(data)[index];
            });
        });
}, 30000);
```

---

## 6️⃣ TABLES RESPONSIVE

### 📍 Ubicación
```
static/css/common.css
static/css/tables.css
```

### 🎯 Propósito
Tablas optimizadas para móvil con:
- Columnas ocultas en pantallas pequeñas
- Sticky column (acciones)
- Responsive breakpoints

### 📝 Uso

```html
<table class="table table-hover">
    <thead>
        <tr>
            <th class="text-center">ID</th>
            <th class="d-none d-md-table-cell">Código</th>
            <th>Descripción</th>
            <th class="text-end">Precio</th>
            <th class="d-none d-lg-table-cell">Stock</th>
            <th class="text-center">Acciones</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td class="text-center">1</td>
            <td class="d-none d-md-table-cell">P001</td>
            <td>Producto Ejemplo</td>
            <td class="text-end">$100.00</td>
            <td class="d-none d-lg-table-cell">50</td>
            <td class="text-center">
                <button class="btn btn-sm btn-primary">
                    <i class="fas fa-edit"></i>
                </button>
            </td>
        </tr>
    </tbody>
</table>
```

### 🎨 Clases Bootstrap Responsive

| Clase | Breakpoint | Descripción |
|-------|------------|-------------|
| `d-none d-sm-table-cell` | ≥576px | Oculto en móvil pequeño |
| `d-none d-md-table-cell` | ≥768px | Oculto en móvil |
| `d-none d-lg-table-cell` | ≥992px | Oculto en tablet |
| `d-none d-xl-table-cell` | ≥1200px | Oculto en desktop pequeño |

### 🎨 CSS Media Queries (common.css)

```css
/* Tablet (≤991px) */
@media (max-width: 991px) {
    .table {
        font-size: 0.9rem;
    }
    
    .table th, .table td {
        padding: 0.75rem 0.5rem;
    }
}

/* Mobile (≤767px) */
@media (max-width: 767px) {
    .table {
        font-size: 0.8rem;
    }
    
    .table th, .table td {
        padding: 0.5rem 0.25rem;
    }
    
    /* Botones verticales */
    .table .btn-group {
        flex-direction: column;
        gap: 0.25rem;
    }
}

/* Small Mobile (≤575px) */
@media (max-width: 575px) {
    .table {
        min-width: 600px; /* Permite scroll horizontal */
    }
    
    /* Sticky last column (acciones) */
    .table td:last-child,
    .table th:last-child {
        position: sticky;
        right: 0;
        background-color: white;
        box-shadow: -2px 0 5px rgba(0,0,0,0.1);
    }
}
```

---

## 7️⃣ FORMS VALIDATION

### 📍 Ubicación
```
static/css/forms.css
static/js/common.js (initFormValidation)
```

### 🎯 Propósito
Formularios con validación en tiempo real:
- HTML5 validation
- Estados visuales (valid/invalid)
- Mensajes de error personalizados

### 📝 Uso

```html
<form th:action="@{/ruta}" method="post" class="needs-validation" novalidate>
    <!-- CSRF Token -->
    <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
    
    <!-- Campo con validación -->
    <div class="mb-3">
        <label for="nombre" class="form-label">
            <i class="fas fa-user me-1"></i>Nombre
        </label>
        <input 
            type="text" 
            class="form-control" 
            id="nombre" 
            name="nombre"
            required
            minlength="3"
            maxlength="100"
            placeholder="Ingrese el nombre">
        <div class="invalid-feedback">
            El nombre debe tener entre 3 y 100 caracteres
        </div>
        <div class="valid-feedback">
            ¡Correcto!
        </div>
    </div>
    
    <!-- Botones -->
    <div class="d-flex gap-2">
        <button type="submit" class="btn btn-primary">
            <i class="fas fa-save me-1"></i>Guardar
        </button>
        <a th:href="@{/ruta-volver}" class="btn btn-secondary">
            <i class="fas fa-times me-1"></i>Cancelar
        </a>
    </div>
</form>

<script>
    // Inicializar validación
    document.addEventListener('DOMContentLoaded', () => {
        AppUtils.initFormValidation();
    });
</script>
```

### 🔧 JavaScript (common.js)

```javascript
const AppUtils = {
    // Inicializar validación de formularios
    initFormValidation: function() {
        const forms = document.querySelectorAll('.needs-validation');
        
        forms.forEach(form => {
            form.addEventListener('submit', (event) => {
                if (!form.checkValidity()) {
                    event.preventDefault();
                    event.stopPropagation();
                }
                
                form.classList.add('was-validated');
            }, false);
            
            // Validación en tiempo real
            form.querySelectorAll('input, textarea, select').forEach(input => {
                input.addEventListener('blur', () => {
                    if (input.value) {
                        input.classList.add('was-validated');
                    }
                });
            });
        });
    }
};
```

### 📋 Validaciones Comunes

#### **Email:**
```html
<input 
    type="email" 
    pattern="[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}$"
    required>
<div class="invalid-feedback">
    Ingrese un email válido
</div>
```

#### **Teléfono (10 dígitos):**
```html
<input 
    type="tel" 
    pattern="[0-9]{10}"
    required>
<div class="invalid-feedback">
    El teléfono debe tener 10 dígitos
</div>
```

#### **Contraseña (mínimo 6 caracteres):**
```html
<input 
    type="password" 
    minlength="6"
    required>
<div class="invalid-feedback">
    La contraseña debe tener al menos 6 caracteres
</div>
```

#### **Número positivo:**
```html
<input 
    type="number" 
    min="0"
    step="0.01"
    required>
<div class="invalid-feedback">
    Ingrese un número positivo
</div>
```

---

## 8️⃣ MODALES

### 📍 Ubicación
```
Bootstrap 5 nativo
```

### 🎯 Propósito
Diálogos flotantes para:
- Formularios de agregar/editar
- Confirmaciones
- Información adicional

### 📝 Uso

```html
<!-- Trigger Button -->
<button 
    type="button" 
    class="btn btn-primary" 
    data-bs-toggle="modal" 
    data-bs-target="#miModal">
    <i class="fas fa-plus me-1"></i>Agregar
</button>

<!-- Modal -->
<div class="modal fade" id="miModal" tabindex="-1">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <!-- Header -->
            <div class="modal-header">
                <h5 class="modal-title">
                    <i class="fas fa-plus me-2"></i>Agregar Cliente
                </h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            
            <!-- Body -->
            <div class="modal-body">
                <form id="formModal">
                    <!-- Campos del formulario -->
                </form>
            </div>
            
            <!-- Footer -->
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
                    Cancelar
                </button>
                <button type="button" class="btn btn-primary" onclick="guardar()">
                    Guardar
                </button>
            </div>
        </div>
    </div>
</div>
```

### 🔧 JavaScript

```javascript
// Abrir modal programáticamente
const modal = new bootstrap.Modal(document.getElementById('miModal'));
modal.show();

// Cerrar modal
modal.hide();

// Llenar modal con datos (editar)
function editarCliente(id) {
    fetch(`/api/clientes/${id}`)
        .then(res => res.json())
        .then(data => {
            document.getElementById('nombre').value = data.nombre;
            document.getElementById('email').value = data.email;
            // ... otros campos
            
            modal.show();
        });
}
```

---

## 9️⃣ ALERTS Y NOTIFICACIONES

### 📍 Ubicación
```
static/js/common.js (AppUtils.showToast)
SweetAlert2 (CDN)
```

### 🎯 Propósito
Notificaciones para:
- Operaciones exitosas
- Errores
- Confirmaciones
- Información

### 📝 Uso

#### **Toast (notificación rápida):**
```javascript
// Éxito
AppUtils.showToast('Cliente guardado correctamente', 'success');

// Error
AppUtils.showToast('Error al guardar', 'error');

// Info
AppUtils.showToast('Procesando...', 'info');

// Warning
AppUtils.showToast('Revise los datos', 'warning');
```

#### **Confirmación (SweetAlert2):**
```javascript
AppUtils.showConfirmDialog(
    '¿Eliminar cliente?',
    'Esta acción no se puede deshacer',
    'warning'
).then((result) => {
    if (result.isConfirmed) {
        // Usuario confirmó
        eliminarCliente(id);
    }
});
```

#### **Alert Bootstrap:**
```html
<!-- Alert con auto-hide -->
<div class="alert alert-success alert-dismissible fade show" role="alert">
    <i class="fas fa-check-circle me-2"></i>
    ¡Operación exitosa!
    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
</div>

<script>
    // Auto-hide después de 5 segundos
    setTimeout(() => {
        const alert = document.querySelector('.alert');
        if (alert) {
            bootstrap.Alert.getOrCreateInstance(alert).close();
        }
    }, 5000);
</script>
```

### 🎨 Tipos de Alert

| Tipo | Clase | Color | Icono |
|------|-------|-------|-------|
| Éxito | `alert-success` | Verde | `fa-check-circle` |
| Error | `alert-danger` | Rojo | `fa-exclamation-circle` |
| Advertencia | `alert-warning` | Amarillo | `fa-exclamation-triangle` |
| Info | `alert-info` | Azul | `fa-info-circle` |

---

## ✅ CHECKLIST DE USO

Antes de usar cualquier componente, verifica:

- [ ] Componente incluido correctamente (`th:replace`)
- [ ] CSS necesario incluido en `layout.html`
- [ ] JavaScript necesario incluido
- [ ] Variables Thymeleaf pasadas desde controller
- [ ] CSRF tokens incluidos (si aplica)
- [ ] Bootstrap 5 y Font Awesome disponibles
- [ ] Responsive testeado en múltiples tamaños

---

## 📚 REFERENCIAS

- **Bootstrap 5:** https://getbootstrap.com/docs/5.3/
- **Font Awesome:** https://fontawesome.com/icons
- **SweetAlert2:** https://sweetalert2.github.io/
- **Thymeleaf:** https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html

---

**Autor:** GitHub Copilot  
**Fecha:** 13/10/2025  
**Versión:** 1.0.0  
**Estado:** ✅ DOCUMENTACIÓN COMPLETA
