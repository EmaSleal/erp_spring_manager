## 🔍 Archivos que Requerirán Actualización

### Controladores Java (~15 archivos)
Buscar y reemplazar rutas de templates:
```bash
# Buscar
return "clientes/form";

# Reemplazar
return "modules/cliente/form";
```

### Templates HTML (~80 archivos)
Buscar y reemplazar referencias a CSS/JS:
```bash
# Buscar
th:href="@{/css/clientes.css}"

# Reemplazar
th:href="@{/modules/cliente/css/clientes.css}"
```

### Layout.html y templates base
Actualizar referencias a recursos compartidos:
```html
<!-- Compartidos -->
<link th:href="@{/shared/css/common.css}" rel="stylesheet">
<link th:href="@{/shared/css/navbar.css}" rel="stylesheet">
<link th:href="@{/shared/css/sidebar.css}" rel="stylesheet">
```

---

