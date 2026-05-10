## 🔄 Migración de Tablas Existentes

### Paso 1: Agregar contenedor

```html
<!-- ANTES -->
<div class="table-responsive">
    <table class="table">...</table>
</div>

<!-- DESPUÉS -->
<div class="responsive-table-container">
    <div class="table-responsive">
        <table class="table">...</table>
    </div>
    <div class="mobile-card-view">
        <!-- Agregar tarjetas aquí -->
    </div>
</div>
```

### Paso 2: Duplicar datos en formato tarjeta

Para cada fila `<tr th:each="item : ${items}">`, crear una tarjeta correspondiente:

```html
<div th:each="item : ${items}" class="mobile-card">
    <!-- Mapear columnas a mobile-card-row -->
</div>
```

### Paso 3: Incluir CSS

```html
<link rel="stylesheet" th:href="@{/shared/css/responsive-table.css}">
```

