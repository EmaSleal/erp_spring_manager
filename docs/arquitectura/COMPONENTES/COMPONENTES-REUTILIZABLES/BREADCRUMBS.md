##  BREADCRUMBS

###  Ubicación
```
static/css/common.css (estilos)
```

###  Propósito
Navegación jerárquica que muestra:
- Ruta actual del usuario
- Links a niveles superiores
- 2 o 3 niveles de profundidad

###  Uso

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

###  Estilos (common.css)

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

### Iconos Font Awesome Recomendados

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

