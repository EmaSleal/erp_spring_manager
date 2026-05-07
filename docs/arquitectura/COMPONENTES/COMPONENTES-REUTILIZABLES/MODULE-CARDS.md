##  MODULE CARDS

###  Ubicación
```
templates/dashboard/dashboard.html
static/css/dashboard.css
```

### Propósito
Tarjetas de módulos en el dashboard con:
- Icono representativo
- Nombre del módulo
- Estados (activo, próximamente)
- Responsive grid

###  Uso

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

###  Estilos (dashboard.css)

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

###  Paleta de Colores Recomendada

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

###  Responsive

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

