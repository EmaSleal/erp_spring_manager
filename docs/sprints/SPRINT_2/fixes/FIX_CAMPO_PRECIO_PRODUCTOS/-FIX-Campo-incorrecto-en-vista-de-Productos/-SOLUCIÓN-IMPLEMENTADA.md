## ✅ SOLUCIÓN IMPLEMENTADA

### Fix 1: Corrección del campo en la tabla

**Archivo:** `reportes/productos.html`

**Línea 158 - ANTES:**
```html
<td class="text-end text-money">
    S/ <span th:text="${#numbers.formatDecimal(producto.precioPublico ?: 0, 1, 2)}">0.00</span>
</td>
```

**Línea 158 - DESPUÉS:**
```html
<td class="text-end text-money">
    S/ <span th:text="${#numbers.formatDecimal(producto.precioInstitucional ?: 0, 1, 2)}">0.00</span>
</td>
```

### Fix 2: Corrección del header de la tabla

**Línea 139 - ANTES:**
```html
<th style="width: 12%;" class="text-end">Precio Público</th>
```

**Línea 139 - DESPUÉS:**
```html
<th style="width: 12%;" class="text-end">Precio Institucional</th>
```

---

