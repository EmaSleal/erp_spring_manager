## ✅ Uso en Páginas

Todas las páginas que usen el layout estándar incluyen automáticamente:

```html
<!-- En el body -->
<div th:replace="~{shared/components/sidebar :: sidebar}"></div>

<!-- Scripts necesarios -->
<script th:src="@{/shared/js/sidebar.js}"></script>
```

