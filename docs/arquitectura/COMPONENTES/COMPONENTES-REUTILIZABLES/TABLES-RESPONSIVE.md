##  TABLES RESPONSIVE

###  Ubicación
```
static/css/common.css
static/css/tables.css
```

###  Propósito
Tablas optimizadas para móvil con:
- Columnas ocultas en pantallas pequeñas
- Sticky column (acciones)
- Responsive breakpoints

###  Uso

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

###  Clases Bootstrap Responsive

| Clase | Breakpoint | Descripción |
|-------|------------|-------------|
| `d-none d-sm-table-cell` | ≥576px | Oculto en móvil pequeño |
| `d-none d-md-table-cell` | ≥768px | Oculto en móvil |
| `d-none d-lg-table-cell` | ≥992px | Oculto en tablet |
| `d-none d-xl-table-cell` | ≥1200px | Oculto en desktop pequeño |

###  CSS Media Queries (common.css)

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

