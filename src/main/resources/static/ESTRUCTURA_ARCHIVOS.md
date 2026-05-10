# Estructura de Archivos - Sprint 1
## ERP Orders Manager

📅 **Fecha de creación:** 11 de octubre de 2025  
✅ **Estado:** Fase 1 completada (puntos 1.2 y 1.3)

---

## 📁 Estructura Creada

### CSS (static/css/)

| Archivo | Descripción | Líneas | Estado |
|---------|-------------|--------|--------|
| `common.css` | Variables CSS, reset, componentes base | ~400 | ✅ |
| `navbar.css` | Barra de navegación superior | ~280 | ✅ |
| `sidebar.css` | Menú lateral (Opción B aprobada) | ~350 | ✅ |
| `dashboard.css` | Página principal con widgets | ~300 | ✅ |
| `forms.css` | Formularios con validación | ~350 | ✅ |
| `tables.css` | Tablas de datos responsivas | ~450 | ✅ |
| `responsive.css` | Media queries globales | ~250 | ✅ |

**Total CSS:** ~2,380 líneas

### JavaScript (static/js/)

| Archivo | Descripción | Funciones Principales | Estado |
|---------|-------------|----------------------|--------|
| `common.js` | Utilidades globales | Toast, confirmaciones, validación, fetch | ✅ |
| `navbar.js` | Funcionalidad navbar | Dropdown usuario, logout, breadcrumbs | ✅ |
| `sidebar.js` | Menú lateral | Toggle, collapse, responsive, módulos | ✅ |
| `dashboard.js` | Dashboard principal | Estadísticas, auto-refresh, animaciones | ✅ |

**Total JS:** ~800 líneas

### Templates (templates/)

| Carpeta | Propósito | Estado |
|---------|-----------|--------|
| `components/` | Componentes reutilizables (navbar, sidebar) | ✅ Creada |
| `dashboard/` | Vistas del dashboard | ✅ Creada |
| `perfil/` | Vistas de perfil de usuario | ✅ Creada |

---

## 🎨 Características Principales

### CSS

#### Variables Globales (common.css)
```css
--primary-color: #1976D2
--background-color: #F5F5F5
--navbar-height: 64px
--sidebar-width: 260px
```

#### Colores por Módulo
- 🔵 Clientes: `#2196F3`
- 🟠 Productos: `#FF9800`
- 🟢 Facturas: `#4CAF50`
- 🟣 Pedidos: `#9C27B0`
- 🔷 Reportes: `#009688`
- 🟦 Inventario: `#3F51B5`

#### Componentes
- ✅ Cards con hover effect
- ✅ Botones con variantes (primary, success, warning, error)
- ✅ Badges de estado
- ✅ Skeleton loading
- ✅ Loading overlay
- ✅ Animaciones moderadas

### JavaScript

#### Utilidades Globales (common.js)
```javascript
AppUtils.showToast(type, message)
AppUtils.showConfirmDialog(title, text)
AppUtils.formatCurrency(amount)
AppUtils.formatDate(date)
AppUtils.fetchWithCsrf(url, options)
AppUtils.initFormValidation(form)
```

#### Navbar (navbar.js)
- Dropdown de usuario animado
- Logout con confirmación
- Breadcrumbs dinámicos

#### Sidebar (sidebar.js)
- Colapsable en desktop
- Hamburger menu en móvil
- Persistencia de estado (LocalStorage)
- Tooltips en modo colapsado
- Módulos con estado activo/deshabilitado

#### Dashboard (dashboard.js)
- Carga de estadísticas vía API
- Auto-refresh cada 60 segundos
- Animación de contadores
- Skeleton loading

---

## 📐 Responsive Design

### Breakpoints
- **xs:** < 576px (Móvil pequeño)
- **sm:** 576px - 767px (Móvil grande)
- **md:** 768px - 991px (Tablet)
- **lg:** 992px - 1199px (Desktop)
- **xl:** ≥ 1200px (Desktop grande)

### Comportamiento del Sidebar
- **Móvil (<768px):** Oculto por defecto, se abre con hamburger menu
- **Tablet (768px+):** Visible, ancho reducido a 200px
- **Desktop (992px+):** Visible, ancho completo 260px
- **Desktop:** Puede colapsarse a 60px

---

## 🔌 Integraciones Externas (CDN)

### Configuradas en layout.html (pendiente)
- ✅ Bootstrap 5.3.0 (planeado)
- ✅ Font Awesome 6.4.0 (planeado)
- ✅ SweetAlert2 11 (planeado)
- ⏳ Chart.js (Sprint futuro)
- ⏳ DataTables (Sprint futuro)
- ⏳ Flatpickr (Sprint futuro)

---

## ✅ Validaciones Incluidas

### CSS
- ✅ Estados de validación (is-valid, is-invalid)
- ✅ Feedback messages (valid-feedback, invalid-feedback)
- ✅ Estados disabled
- ✅ Focus states

### JavaScript
- ✅ Validación de email
- ✅ Validación de teléfono (10 dígitos México)
- ✅ Validación en tiempo real
- ✅ CSRF token en peticiones

---

## 🎯 Próximos Pasos (Fase 2)

### Pendiente de creación:
1. ⏳ `templates/layout.html` - Layout base con CDNs
2. ⏳ `templates/components/navbar.html` - Componente navbar
3. ⏳ `templates/components/sidebar.html` - Componente sidebar
4. ⏳ `DashboardController.java` - Controlador del dashboard
5. ⏳ `PerfilController.java` - Controlador de perfil

---

## 📊 Estadísticas

### Archivos Creados
- **CSS:** 7 archivos
- **JavaScript:** 4 archivos
- **Carpetas:** 3 carpetas templates
- **Total líneas:** ~3,200 líneas de código

### Tiempo Estimado de Desarrollo
- Planificación: ✅ Completada
- CSS: ✅ ~4 horas
- JavaScript: ✅ ~3 horas
- **Total Fase 1:** ✅ ~7 horas

---

## 🔧 Notas Técnicas

### Dependencias Java Actualizadas
```xml
<!-- Cambios en pom.xml -->
<java.version>21</java.version> <!-- Era 24 -->

<!-- Agregado -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- Eliminado (redundante) -->
<!-- jakarta.servlet-api -->
```

### Convenciones de Código
- **CSS:** kebab-case (`.module-card`)
- **CSS IDs:** camelCase (`#dashboardGrid`)
- **JavaScript:** camelCase (`showToast()`)
- **Rutas:** kebab-case (`/mi-perfil`)

---

## 📝 Checklist de Validación

✅ Todos los archivos CSS creados  
✅ Todos los archivos JS creados  
✅ Carpetas templates creadas  
✅ Variables CSS definidas  
✅ Colores por módulo asignados  
✅ Responsive breakpoints configurados  
✅ Utilidades JavaScript documentadas  
✅ LocalStorage para preferencias  
✅ CSRF tokens incluidos  
✅ Validaciones de formulario  
✅ Animaciones configuradas  
✅ Skeleton loading implementado  

---

## 🚀 Listo para Fase 2

La estructura base está completa. Se puede proceder con:
- Punto 1.4: Configuración de CDNs en layout.html
- Fase 2: Creación de componentes navbar y sidebar

---

**Generado automáticamente por GitHub Copilot**  
**Fecha:** 11 de octubre de 2025
