## 📝 CÓDIGO DE EJEMPLO

### Proteger un endpoint en el Controller:
```java
@Controller
@PreAuthorize("hasRole('ADMIN')")
public class ConfiguracionController {
    // Solo ADMIN puede acceder a todos los métodos
}
```

### Proteger un método específico:
```java
@PostMapping("/clientes/delete/{id}")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public String eliminarCliente(@PathVariable Integer id) {
    // Solo ADMIN y USER pueden eliminar
}
```

### Ocultar elementos en la vista según rol:
```html
<!-- Botón solo visible para ADMIN y USER -->
<a th:href="@{/clientes/form}" 
   sec:authorize="hasAnyRole('ADMIN', 'USER')"
   class="btn btn-primary">
    Nuevo Cliente
</a>

<!-- Botón solo para ADMIN -->
<a th:href="@{/configuracion}" 
   sec:authorize="hasRole('ADMIN')"
   class="btn btn-secondary">
    Configuración
</a>
```

---

