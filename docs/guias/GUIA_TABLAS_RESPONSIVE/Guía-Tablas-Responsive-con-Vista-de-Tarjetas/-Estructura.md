## 📐 Estructura

### 1. Incluir CSS

```html
<link rel="stylesheet" th:href="@{/shared/css/responsive-table.css}">
```

### 2. Estructura HTML

```html
<div class="responsive-table-container">
    
    <!-- DESKTOP: Tabla (se oculta automáticamente en móvil) -->
    <div class="table-responsive">
        <table class="table table-hover align-middle mb-0">
            <thead class="table-light">
                <tr>
                    <th>#</th>
                    <th>Nombre</th>
                    <th>Email</th>
                    <th>Acciones</th>
                </tr>
            </thead>
            <tbody>
                <tr th:each="item : ${items}">
                    <td th:text="${item.id}">1</td>
                    <td th:text="${item.nombre}">Nombre</td>
                    <td th:text="${item.email}">email@example.com</td>
                    <td>
                        <button class="btn btn-sm btn-primary">Editar</button>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>

    <!-- MÓVIL: Tarjetas (se ocultan automáticamente en desktop) -->
    <div class="mobile-card-view">
        <div th:each="item : ${items}" class="mobile-card">
            
            <!-- Header -->
            <div class="mobile-card-header">
                <h3 class="mobile-card-title" th:text="${item.nombre}">Nombre</h3>
                <span class="mobile-card-badge badge bg-primary">#<span th:text="${item.id}">1</span></span>
            </div>

            <!-- Body -->
            <div class="mobile-card-body">
                <div class="mobile-card-row">
                    <span class="mobile-card-label">Email</span>
                    <span class="mobile-card-value" th:text="${item.email}">email</span>
                </div>
            </div>

            <!-- Footer -->
            <div class="mobile-card-footer">
                <button class="btn btn-sm btn-primary w-100">
                    <i class="bi bi-pencil me-1"></i> Editar
                </button>
            </div>
        </div>
    </div>

</div>
```

