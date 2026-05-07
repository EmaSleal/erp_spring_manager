## 3️⃣ FRONTEND

### **Decisión 3.1: Thymeleaf como Motor de Plantillas**

#### ✅ Decisión Final:
**Thymeleaf 3.x**

#### 🎯 Justificación:
- ✅ Integración perfecta con Spring Boot
- ✅ Server-side rendering (SEO friendly)
- ✅ Sintaxis natural HTML
- ✅ No requiere compilación separada

#### 📝 Ejemplo:
```html
<div th:text="${usuario.nombre}">Nombre</div>
<div th:if="${usuario.activo}">Activo</div>
<div th:replace="~{components/navbar :: navbar}"></div>
```

#### ❌ Alternativas Descartadas:
- **React/Vue:** Complejidad innecesaria para v1.0
- **JSP:** Tecnología obsoleta

---

### **Decisión 3.2: Bootstrap 5 sobre Tailwind CSS**

#### ✅ Decisión Final:
**Bootstrap 5.3.0** + CSS personalizado

#### 🎯 Justificación:
- ✅ Componentes pre-construidos (navbar, modals, forms)
- ✅ Grid system responsive robusto
- ✅ Documentación extensa
- ✅ Compatible con Thymeleaf

#### 📝 CDN Utilizado:
```html
<!-- Bootstrap CSS -->
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" 
      rel="stylesheet">

<!-- Bootstrap JS Bundle -->
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js">
</script>
```

#### ❌ Alternativas Descartadas:
- **Tailwind CSS:** Verboso en templates Thymeleaf
- **Material UI:** Requiere React
- **CSS puro:** Demasiado tiempo de desarrollo

#### 🔄 Cambio Realizado:
- **Antes:** Tailwind CSS configurado
- **Ahora:** Bootstrap 5 + CSS custom
- **Razón:** Mejor integración con Thymeleaf
- **Fecha:** 11/10/2025

---

### **Decisión 3.3: Font Awesome para Iconos**

#### ✅ Decisión Final:
**Font Awesome 6.4.0 Free**

#### 🎯 Justificación:
- ✅ 2000+ iconos gratuitos
- ✅ Sintaxis simple (`<i class="fas fa-user"></i>`)
- ✅ Escalable (SVG)
- ✅ Compatible con todos los navegadores

#### 📝 CDN Utilizado:
```html
<link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" 
      rel="stylesheet">
```

---

### **Decisión 3.4: SweetAlert2 para Notificaciones**

#### ✅ Decisión Final:
**SweetAlert2 11**

#### 🎯 Justificación:
- ✅ Alertas y confirmaciones elegantes
- ✅ Totalmente personalizable
- ✅ Compatible con async/await
- ✅ Responsive por defecto

#### 📝 Uso:
```javascript
AppUtils.showToast('Cliente guardado', 'success');

AppUtils.showConfirmDialog('¿Eliminar?', 'No se puede deshacer', 'warning')
    .then(result => {
        if (result.isConfirmed) {
            // Eliminar
        }
    });
```

---

### **Decisión 3.5: Arquitectura CSS Modular**

#### ✅ Decisión Final:
**7 archivos CSS especializados**

#### 📂 Estructura:
```
static/css/
├── common.css       # Variables, reset, utilidades
├── navbar.css       # Barra superior
├── sidebar.css      # Menú lateral (no usado finalmente)
├── dashboard.css    # Página principal
├── forms.css        # Formularios
├── tables.css       # Tablas
└── responsive.css   # Media queries
```

#### 🎯 Justificación:
- ✅ Separación por responsabilidad
- ✅ Fácil mantenimiento
- ✅ Carga selectiva según página
- ✅ Evita conflictos de estilos

---

### **Decisión 3.6: Sidebar NO Implementado**

#### ❌ Decisión Final:
**Sidebar descartado** del diseño final

#### 🎯 Justificación:
- ✅ Navbar es suficiente para navegación
- ✅ Módulos en dashboard más intuitivos
- ✅ Mejor experiencia en móvil sin sidebar
- ✅ Reduce complejidad visual

#### 📝 Estado:
- **Archivos creados:** `sidebar.css`, `sidebar.js`
- **Estado:** No integrados en layout
- **Razón:** Navbar + breadcrumbs ofrecen navegación suficiente

---

