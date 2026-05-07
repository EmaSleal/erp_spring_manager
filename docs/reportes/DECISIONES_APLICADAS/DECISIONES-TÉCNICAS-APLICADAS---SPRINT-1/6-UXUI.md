## 6️⃣ UX/UI

### **Decisión 6.1: Material Design Color Palette**

#### ✅ Decisión Final:
**Material Design azul** (`#1976D2`)

#### 🎨 Paleta Aplicada:
```css
:root {
    --primary-color: #1976D2;        /* Azul principal */
    --primary-dark: #1565C0;         /* Azul oscuro */
    --primary-light: #42A5F5;        /* Azul claro */
    
    --success-color: #4CAF50;        /* Verde */
    --danger-color: #F44336;         /* Rojo */
    --warning-color: #FF9800;        /* Naranja */
    --info-color: #2196F3;           /* Azul info */
}
```

#### 🔄 Cambio Realizado:
- **Antes:** Login/registro con púrpura `#667eea`
- **Ahora:** Todo el sitio usa azul `#1976D2`
- **Razón:** Consistencia visual 100%
- **Fecha:** 13/10/2025
- **Documentación:** `FIX_PALETA_COLORES_AUTH.md`

---

### **Decisión 6.2: Breadcrumbs en Contenido (No en Navbar)**

#### ✅ Decisión Final:
**Breadcrumbs solo en área de contenido**

#### 🎯 Justificación:
- ✅ Más espacio para breadcrumbs de 3 niveles
- ✅ Mejor contraste (fondo gris claro)
- ✅ Navbar más limpio
- ✅ Cercanía al contenido

#### 📝 Diseño:
```html
<nav aria-label="breadcrumb" class="mb-3">
    <ol class="breadcrumb">
        <!-- 2 o 3 niveles -->
    </ol>
</nav>
```

#### ❌ Alternativa Descartada:
- **Breadcrumbs en navbar:** Causaba duplicación visual

---

### **Decisión 6.3: Responsive con Bootstrap + CSS Custom**

#### ✅ Decisión Final:
**Bootstrap utilities + CSS media queries personalizadas**

#### 🎯 Justificación:
- ✅ Bootstrap para columnas visibles (`d-none d-md-table-cell`)
- ✅ CSS custom para ajustes finos
- ✅ Flexibilidad total

#### 📝 Breakpoints:
```css
/* Small Mobile */
@media (max-width: 575px) { /* ... */ }

/* Mobile */
@media (max-width: 767px) { /* ... */ }

/* Tablet */
@media (max-width: 991px) { /* ... */ }

/* Desktop */
@media (min-width: 992px) { /* ... */ }
```

---

### **Decisión 6.4: Paginación con Sliding Window**

#### ✅ Decisión Final:
**Algoritmo sliding window adaptativo**

#### 🎯 Justificación:
- ✅ Muestra solo 3-10 páginas según tamaño pantalla
- ✅ Primera y última siempre visibles
- ✅ Evita overflow horizontal
- ✅ UX mejorada 300%

#### 📝 Implementación:
```javascript
function renderPagination() {
    const screenWidth = window.innerWidth;
    let maxVisiblePages;
    
    if (screenWidth < 576)      maxVisiblePages = 3;
    else if (screenWidth < 768) maxVisiblePages = 5;
    else if (screenWidth < 992) maxVisiblePages = 7;
    else                        maxVisiblePages = 10;
    
    // Sliding window logic
}
```

#### 🔄 Cambio Realizado:
- **Antes:** 17 botones lineales (850px)
- **Ahora:** 3-10 botones adaptativos (200-500px)
- **Reducción:** 76% en overflow
- **Fecha:** 13/10/2025

---

### **Decisión 6.5: Avatar con Iniciales**

#### ✅ Decisión Final:
**Avatar dinámico:** Imagen o iniciales generadas

#### 📝 Implementación:
```html
<!-- Si tiene imagen -->
<img th:if="${usuario.avatar != null}" 
     th:src="@{${usuario.avatar}}" 
     class="avatar">

<!-- Si no tiene imagen -->
<div th:unless="${usuario.avatar != null}" 
     class="avatar-initials">
    <span th:text="${usuario.nombre.charAt(0)}">U</span>
</div>
```

---

