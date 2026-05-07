## 🔗 INTEGRACIÓN CON OTROS COMPONENTES

### **SecurityConfig.java**
```java
// Dashboard accesible por todos los roles autenticados
.requestMatchers("/dashboard/**").authenticated()

// Usuarios solo ADMIN
.requestMatchers("/usuarios/**").hasRole("ADMIN")

// Configuración solo ADMIN
.requestMatchers("/configuracion/**").hasRole("ADMIN")
```

### **@PreAuthorize en Controladores**
```java
// UsuarioController.java
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController { ... }

// ConfiguracionController.java
@PreAuthorize("hasRole('ADMIN')")
public class ConfiguracionController { ... }
```

### **Vistas con sec:authorize**
```html
<!-- Módulo visible solo si el usuario tiene permiso -->
<div th:if="${modulo.visible}">
    <!-- Contenido del módulo -->
</div>
```

---

