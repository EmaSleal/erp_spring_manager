## 🎯 Mejores Prácticas Aplicadas

### 1. Orden de Carga de Scripts

**Regla de Oro:** Dependencias SIEMPRE antes que código que las usa.

```html
<!-- ✅ CORRECTO -->
<script src="jquery.js"></script>      <!-- Librería -->
<script src="mi-codigo.js"></script>   <!-- Código que usa jQuery -->

<!-- ❌ INCORRECTO -->
<script src="mi-codigo.js"></script>   <!-- Error: jQuery no definido -->
<script src="jquery.js"></script>      <!-- Demasiado tarde -->
```

### 2. Estructura HTML Recomendada

```html
<!DOCTYPE html>
<html>
<head>
    <!-- CSS aquí -->
</head>
<body>
    <!-- Contenido de la página -->
    
    <!-- Scripts al FINAL del body -->
    <!-- 1. Librerías externas -->
    <script src="jquery.js"></script>
    <script src="bootstrap.js"></script>
    <script src="sweetalert2.js"></script>
    
    <!-- 2. Scripts comunes de la app -->
    <script src="common.js"></script>
    
    <!-- 3. Scripts específicos de la página -->
    <script src="pagina-especifica.js"></script>
</body>
</html>
```

### 3. Uso de Fragmentos Thymeleaf

```html
<!-- layout.html - Define fragmento scripts -->
<th:block th:fragment="scripts">
    <script src="jquery.js"></script>
    <script src="bootstrap.js"></script>
    <!-- ... más scripts comunes ... -->
</th:block>

<!-- pagina.html - Usa el fragmento -->
<body>
    <!-- Contenido -->
    
    <!-- 1. Incluye scripts comunes -->
    <th:block th:replace="~{layout :: scripts}"></th:block>
    
    <!-- 2. Scripts específicos DESPUÉS -->
    <script th:src="@{/js/mi-script.js}"></script>
</body>
```

