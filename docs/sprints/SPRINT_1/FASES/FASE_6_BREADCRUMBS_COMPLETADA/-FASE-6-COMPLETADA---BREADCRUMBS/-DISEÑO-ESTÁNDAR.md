## 🎨 DISEÑO ESTÁNDAR

### **Estructura HTML**

```html
<nav aria-label="breadcrumb" class="mb-3">
    <ol class="breadcrumb">
        <!-- Nivel 1: Dashboard (siempre enlace) -->
        <li class="breadcrumb-item">
            <a th:href="@{/dashboard}" class="text-decoration-none">
                <i class="fas fa-home me-1"></i>Dashboard
            </a>
        </li>
        
        <!-- Nivel 2: Módulo (enlace o activo) -->
        <li class="breadcrumb-item" th:classappend="${activo ? 'active' : ''}">
            <a th:href="@{/modulo}" th:unless="${activo}">
                <i class="fas fa-icon me-1"></i>Módulo
            </a>
            <span th:if="${activo}">
                <i class="fas fa-icon me-1"></i>Módulo
            </span>
        </li>
        
        <!-- Nivel 3: Acción (solo activo) -->
        <li class="breadcrumb-item active" aria-current="page" th:if="${nivel3}">
            <i class="fas fa-icon me-1"></i>Acción
        </li>
    </ol>
</nav>
```

### **CSS (common.css)**

```css
/* Contenedor */
.breadcrumb {
    background-color: #F8F9FA;    /* Gris claro */
    border: 1px solid #E9ECEF;    /* Borde sutil */
    padding: 0.75rem 1rem;
    border-radius: 0.375rem;
    margin-bottom: 1rem;
}

/* Enlaces */
.breadcrumb-item a {
    color: var(--primary-color);  /* Azul #1976D2 */
    font-weight: 500;
    text-decoration: none;
    transition: color 0.2s;
}

.breadcrumb-item a:hover {
    color: var(--primary-dark);   /* Azul oscuro */
    text-decoration: underline;
}

/* Elemento activo */
.breadcrumb-item.active {
    color: #495057;               /* Gris oscuro */
    font-weight: 600;
}

/* Separador */
.breadcrumb-item + .breadcrumb-item::before {
    content: "/";
    color: #ADB5BD;               /* Gris medio */
}
```

---

