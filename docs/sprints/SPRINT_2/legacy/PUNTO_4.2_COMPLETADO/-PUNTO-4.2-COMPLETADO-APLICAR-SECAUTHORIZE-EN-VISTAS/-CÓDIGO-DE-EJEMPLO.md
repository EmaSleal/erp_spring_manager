## 📝 CÓDIGO DE EJEMPLO

### Ejemplo 1: Ocultar botón según rol (Thymeleaf)
```html
<!-- Solo visible para ADMIN y USER -->
<button class="btn btn-primary" sec:authorize="hasAnyRole('ADMIN', 'USER')">
    <i class="fas fa-plus"></i> Agregar
</button>
```

### Ejemplo 2: Mostrar badge según rol (Thymeleaf)
```html
<!-- Solo visible para VENDEDOR y VISUALIZADOR -->
<span sec:authorize="hasAnyRole('VENDEDOR', 'VISUALIZADOR')" 
      class="badge bg-secondary">
    <i class="bi bi-eye-fill"></i> Solo lectura
</span>
```

### Ejemplo 3: Ocultar botón según rol (JavaScript)
```javascript
// Pasar rol desde Thymeleaf
const userRole = /*[[${#authentication.principal.authorities[0].authority}]]*/ 'ROLE_USER';

// Usar en template strings
<button style="${(userRole === 'ROLE_VENDEDOR' || userRole === 'ROLE_VISUALIZADOR') 
                 ? 'display:none;' : ''}">
    Editar
</button>
```

### Ejemplo 4: Badge condicional en Sidebar
```html
<!-- Badge diferente para VENDEDOR vs VISUALIZADOR -->
<span class="menu-badge bg-success" sec:authorize="hasRole('VENDEDOR')">
    <i class="bi bi-plus-circle-fill"></i>
</span>
<span class="menu-badge bg-secondary" sec:authorize="hasRole('VISUALIZADOR')">
    <i class="bi bi-eye-fill"></i>
</span>
```

---

