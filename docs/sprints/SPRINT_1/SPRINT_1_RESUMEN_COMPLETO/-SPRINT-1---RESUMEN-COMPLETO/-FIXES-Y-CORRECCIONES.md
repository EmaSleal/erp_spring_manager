## 🐛 FIXES Y CORRECCIONES

### Fix 1: Dashboard - Thymeleaf Security Error
**Fecha:** 12/10/2025  
**Problema:** `th:onclick` con expresiones bloqueado en Thymeleaf 3.1+  
**Solución:** Data attributes + JavaScript externo  
**Archivos:** `dashboard.html`, `dashboard.js`

### Fix 2: Factura - Estado No Persistía
**Fecha:** 12/10/2025  
**Problema:** Campo `entregado` no se guardaba  
**Solución:** Endpoint separado `PUT /facturas/actualizar-estado/{id}`  
**Archivos:** `FacturaController.java`, `editar-factura.js`

### Fix 3: Logout 403 Forbidden
**Fecha:** 12/10/2025  
**Problema:** URL `/auth/logout` no reconocida por Spring Security  
**Solución:**
- Cambiar a `/logout` (default de Spring Security)
- Corregir CSRF token name a `'_csrf'` estático

**Archivos:** `SecurityConfig.java`, `navbar.js`

**Detalles:**
```javascript
// ❌ ANTES (ERROR)
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
const input = document.createElement('input');
input.name = csrfHeader.replace('X-', '').toLowerCase();  // Dinámico

// ✅ DESPUÉS (OK)
const input = document.createElement('input');
input.name = '_csrf';  // Estático (Spring Security espera esto)
```

### Fix 4: Template Field Names
**Fecha:** 12/10/2025  
**Problema:** Error Thymeleaf "Property 'fechaCreacion' not found"  
**Solución:** Corregir nombres de español a inglés (coincide con modelo)

**Archivos:** `perfil/ver.html`

**Cambios:**
```html
<!-- ❌ ANTES (ERROR) -->
th:text="${usuario.fechaCreacion}"
th:text="${usuario.fechaModificacion}"

<!-- ✅ DESPUÉS (OK) -->
th:text="${usuario.createDate}"
th:text="${usuario.updateDate}"
```

### Fix 5: Temporals Format Error
**Fecha:** 12/10/2025  
**Problema:** `#temporals.format()` no soporta `java.sql.Timestamp`  
**Solución:** Usar `#dates.format()` para tipos legacy

**Archivos:** `perfil/ver.html`

**Explicación:**
- `#temporals` → Para Java 8+ (`LocalDateTime`, `LocalDate`, `Instant`)
- `#dates` → Para tipos legacy (`Date`, `Timestamp`, `Calendar`)

**Cambios:**
```html
<!-- ❌ ANTES (ERROR) -->
th:text="${#temporals.format(usuario.createDate, 'dd/MM/yyyy HH:mm')}"

<!-- ✅ DESPUÉS (OK) -->
th:text="${#dates.format(usuario.createDate, 'dd/MM/yyyy HH:mm')}"
```

---

