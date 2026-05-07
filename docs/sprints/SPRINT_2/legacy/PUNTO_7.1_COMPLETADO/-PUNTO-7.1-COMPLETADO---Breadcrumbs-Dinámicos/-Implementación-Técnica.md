## 🔧 Implementación Técnica

### **Archivo Modificado:**
- `src/main/resources/static/js/navbar.js` (función updateBreadcrumbs())

### **Función Principal:**

```javascript
function updateBreadcrumbs() {
    const breadcrumbsContainer = document.querySelector('.breadcrumbs');
    if (!breadcrumbsContainer) return;

    const path = window.location.pathname;
    const queryString = window.location.search;
    const urlParams = new URLSearchParams(queryString);
    
    // Limpiar breadcrumbs actuales
    breadcrumbsContainer.innerHTML = '';

    // Agregar Home (siempre primero)
    addBreadcrumb(breadcrumbsContainer, 'Dashboard', '/dashboard', false);

    // Mapeo completo de 30+ rutas...
    // [Ver código completo en navbar.js]
}
```

### **Características Clave:**

#### **1. Detección de Parámetros de Query:**
```javascript
const queryString = window.location.search;
const urlParams = new URLSearchParams(queryString);
const tab = urlParams.get('tab');
```

**Ejemplo:**
```
URL: /configuracion?tab=notificaciones
Breadcrumb: Dashboard > Configuración > Notificaciones
```

#### **2. Detección de IDs en URLs:**
```javascript
if (path.match(/\/clientes\/form\/\d+/)) {
    const id = path.split('/').pop();
    addBreadcrumb(breadcrumbsContainer, `Editar Cliente #${id}`, path, true);
}
```

**Ejemplo:**
```
URL: /clientes/form/15
Breadcrumb: Dashboard > Clientes > Editar Cliente #15
```

#### **3. Rutas Específicas por Módulo:**
```javascript
// === MÓDULO FACTURAS ===
if (path.startsWith('/facturas')) {
    addBreadcrumb(breadcrumbsContainer, 'Facturas', '/facturas', false);
    
    if (path === '/facturas/form') {
        addBreadcrumb(breadcrumbsContainer, 'Nueva Factura', path, true);
    } else if (path.match(/\/facturas\/editar\/\d+/)) {
        const id = path.split('/').pop();
        addBreadcrumb(breadcrumbsContainer, `Editar Factura #${id}`, path, true);
    } else if (path.match(/\/facturas\/ver\/\d+/)) {
        const id = path.split('/').pop();
        addBreadcrumb(breadcrumbsContainer, `Ver Factura #${id}`, path, true);
    }
    return;
}
```

#### **4. Fallback Genérico:**
```javascript
// Si no coincide con ninguna ruta específica
let currentPath = '';
segments.forEach((segment, index) => {
    if (segment === 'dashboard') return;
    
    currentPath += '/' + segment;
    
    // Si el segmento es un número (ID), mostrarlo como #ID
    const name = /^\d+$/.test(segment) 
        ? `#${segment}` 
        : (routeNames[segment] || capitalizeFirst(segment));
    
    const isLast = index === segments.length - 1;
    addBreadcrumb(breadcrumbsContainer, name, currentPath, isLast);
});
```

#### **5. Función Auxiliar capitalizeFirst():**
```javascript
/**
 * Capitalizar primera letra de un string
 */
function capitalizeFirst(str) {
    return str.charAt(0).toUpperCase() + str.slice(1);
}
```

**Uso:**
```javascript
capitalizeFirst('clientes') // "Clientes"
capitalizeFirst('form')     // "Form"
```

---

