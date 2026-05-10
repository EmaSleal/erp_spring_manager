## 📋 Problema Identificado

Los archivos HTML del módulo de Reportes no mostraban:
- ❌ **Barra de navegación superior (Navbar)**
- ❌ **Barra lateral de navegación (Sidebar)**

### **Archivos Afectados:**
- `reportes/index.html` - Dashboard de reportes
- `reportes/ventas.html` - Reporte de ventas
- `reportes/clientes.html` - Reporte de clientes
- `reportes/productos.html` - Reporte de productos

### **Problema Técnico:**
Los archivos estaban importando CSS manualmente en lugar de usar el layout estándar:
```html
<!-- ANTES (Incorrecto) -->
<head>
    <meta charset="UTF-8">
    <title>Reporte...</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
    <!-- Más CSS manual... -->
</head>
<body>
    <!-- Navbar -->
    <div th:replace="~{components/navbar :: navbar}"></div>
    <!-- ❌ FALTA SIDEBAR -->
    <div class="container-fluid mt-4">
        <!-- Contenido -->
    </div>
</body>
```

---

