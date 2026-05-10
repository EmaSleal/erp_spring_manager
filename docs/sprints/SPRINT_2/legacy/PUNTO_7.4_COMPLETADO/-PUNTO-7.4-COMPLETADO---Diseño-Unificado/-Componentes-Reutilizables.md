## 🔧 Componentes Reutilizables

### Fragments Thymeleaf Identificados

| Fragment | Ubicación | Uso |
|----------|-----------|-----|
| layout | `layout.html` | Layout base |
| navbar | `components/navbar.html` | Barra de navegación |
| sidebar | `components/sidebar.html` | Menú lateral |
| empresaForm | `configuracion/empresa.html` | Form de empresa |
| facturacionForm | `configuracion/facturacion.html` | Form de facturación |
| notificacionesForm | `configuracion/notificaciones.html` | Form de notificaciones |

### Oportunidades de Componentización

**Componentes que podrían crearse:**

1. **stats-card.html**
```html
<th:block th:fragment="statsCard(title, value, icon, color)">
    <div th:class="'card stats-card ' + ${color} + ' text-white'">
        <div class="card-body">
            <h6 class="card-subtitle mb-2 text-white-50" th:text="${title}">Title</h6>
            <h3 class="card-title mb-0" th:text="${value}">0</h3>
            <i th:class="'bi ' + ${icon} + ' fs-1'"></i>
        </div>
    </div>
</th:block>
```

2. **table-wrapper.html**
```html
<th:block th:fragment="tableWrapper(title, content)">
    <div class="card">
        <div class="card-header bg-light">
            <h5 class="mb-0" th:text="${title}">Table Title</h5>
        </div>
        <div class="card-body p-0">
            <div class="table-responsive">
                <th:block th:replace="${content}"></th:block>
            </div>
        </div>
    </div>
</th:block>
```

**Impacto:** Reduciría código duplicado en ~20%.

---

