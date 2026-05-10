## 🔧 FIX #3: Campo Incorrecto en Vista de Productos

### Problema
```
org.springframework.expression.spel.SpelEvaluationException: EL1008E: 
Property or field 'precioPublico' cannot be found on object of type 'Producto'
```

### Causa
- HTML intentaba acceder a `producto.precioPublico`
- El campo correcto en el modelo es `precioInstitucional`
- Campos disponibles: `precioMayorista`, `precioInstitucional`

### Solución
1. ✅ Línea 158: Cambiar `precioPublico` → `precioInstitucional`
2. ✅ Línea 139: Cambiar header "Precio Público" → "Precio Institucional"

**ANTES:**
```html
<th>Precio Público</th>
...
<span th:text="${producto.precioPublico}">0.00</span>
```

**DESPUÉS:**
```html
<th>Precio Institucional</th>
...
<span th:text="${producto.precioInstitucional}">0.00</span>
```

### Archivos Modificados
- reportes/productos.html (2 líneas)

### Impacto
- **Severidad:** Media (impedía ver reporte de productos)
- **Usuarios afectados:** ADMIN, USER
- **Tiempo de fix:** 5 minutos

### Documentación
📄 `docs/sprints/SPRINT_2/fixes/FIX_CAMPO_PRECIO_PRODUCTOS.md`

---

