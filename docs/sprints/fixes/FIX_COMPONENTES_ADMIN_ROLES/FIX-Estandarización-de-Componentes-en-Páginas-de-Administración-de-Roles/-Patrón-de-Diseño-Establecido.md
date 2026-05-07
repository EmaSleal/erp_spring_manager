## 🎨 Patrón de Diseño Establecido

### Para Futuras Páginas Administrativas

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security"
      lang="es">
<head th:replace="~{layout :: head}">
    <title>Título de la Página</title>
</head>
<body>
    <!-- Navbar -->
    <div th:replace="~{components/navbar :: navbar}"></div>

    <!-- Sidebar -->
    <div th:replace="~{components/sidebar :: sidebar}"></div>

    <!-- Main Content -->
    <main class="main-content">
        <div class="container-fluid py-4">
            <!-- Breadcrumbs -->
            <nav aria-label="breadcrumb" class="mb-3">
                <ol class="breadcrumb">
                    <!-- Breadcrumbs items -->
                </ol>
            </nav>

            <!-- Header Section -->
            <div class="d-flex justify-content-between align-items-center mb-4">
                <div>
                    <h2 class="h3 mb-1">
                        <i class="fas fa-icon text-color me-2"></i>
                        Título Principal
                    </h2>
                    <p class="text-muted mb-0">Descripción breve</p>
                </div>
                <div class="d-flex gap-2">
                    <!-- Botones de acción -->
                </div>
            </div>

            <!-- Contenido de la página -->
        </div>
    </main>

    <!-- Footer -->
    <div th:replace="~{components/footer :: footer}"></div>
</body>
</html>
```

---

