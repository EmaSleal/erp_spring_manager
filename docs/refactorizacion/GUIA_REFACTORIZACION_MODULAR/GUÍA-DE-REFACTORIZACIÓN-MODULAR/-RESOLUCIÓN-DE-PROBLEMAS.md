## 🛠️ RESOLUCIÓN DE PROBLEMAS

### Problema 1: Errores de Compilación

**Síntoma:**
```
[ERROR] cannot find symbol
symbol:   class Factura
location: class FacturaService
```

**Solución:**
```java
// Verificar que el import está correcto
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;

// No debe ser:
import api.astro.whats_orders_manager.models.Factura; // ❌ Antigua ubicación
```

**Usar IntelliJ:**
1. Click en el error
2. `Alt + Enter`
3. Seleccionar "Import class"

---

### Problema 2: Circular Dependencies

**Síntoma:**
```
The dependencies of some of the beans in the application context form a cycle
```

**Causas comunes:**
- Módulo A usa Módulo B
- Módulo B usa Módulo A

**Solución:**
```java
// Usar @Lazy para romper el ciclo
@Service
public class FacturaService {
    
    @Autowired
    @Lazy
    private NotificacionService notificacionService;
}
```

---

### Problema 3: Component Scan No Encuentra Beans

**Síntoma:**
```
Field xxxService in xxxController required a bean of type 'xxxService' that could not be found.
```

**Solución 1: Verificar @ComponentScan**
```java
@SpringBootApplication
@ComponentScan(basePackages = {
    "api.astro.whats_orders_manager",
    "api.astro.whats_orders_manager.modules",
    "api.astro.whats_orders_manager.shared",
    "api.astro.whats_orders_manager.core"
})
public class WhatsOrdersManagerApplication {
    // ...
}
```

**Solución 2: Verificar anotaciones**
```java
// El service debe tener @Service
@Service
public class ProductoService {
    // ...
}

// El repository debe tener @Repository
@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    // ...
}
```

---

### Problema 4: Tests No Encuentran Clases

**Síntoma:**
```
java.lang.ClassNotFoundException: api.astro.whats_orders_manager.models.Producto
```

**Solución:**
Actualizar imports en los tests:

```java
// En ProductoTest.java
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.service.ProductoService;
import api.astro.whats_orders_manager.modules.producto.repository.ProductoRepository;
```

---

### Problema 5: Thymeleaf No Encuentra Templates

**Síntoma:**
```
Error resolving template [whatsapp/mensajes], template might not exist
```

**Solución:**
Los templates HTML no cambian de ubicación, siguen en:
```
src/main/resources/templates/
```

Solo cambia el Java, no los templates.

---

### Problema 6: Git Muestra Muchos Cambios

**Síntoma:**
`git status` muestra 100+ archivos modificados

**Solución:**
Es normal, estás moviendo muchos archivos. Hacer commits frecuentes:

```bash
# Commit por módulo
git add modules/producto/
git commit -m "refactor: Migrar módulo Producto"

git add modules/cliente/
git commit -m "refactor: Migrar módulo Cliente"

# etc.
```

---

### Problema 7: IntelliJ No Detecta Cambios

**Síntoma:**
IntelliJ sigue mostrando imports antiguos como válidos

**Solución:**
```
1. File → Invalidate Caches → Invalidate and Restart
2. Esperar a que IntelliJ re-indexe el proyecto
```

---

