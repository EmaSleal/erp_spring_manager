## 🧪 Casos de Prueba

### **Caso 1: Navegación en Clientes**

**Flujo:**
1. Usuario accede a `/clientes`
2. Breadcrumb: `Dashboard > Clientes`
3. Click en "Nuevo Cliente" → `/clientes/form`
4. Breadcrumb: `Dashboard > Clientes > Nuevo Cliente`
5. Después de guardar, redirige a `/clientes`
6. Click en "Editar" en fila ID 15 → `/clientes/form/15`
7. Breadcrumb: `Dashboard > Clientes > Editar Cliente #15`

**Resultado:** ✅ Breadcrumbs contextuales en cada paso

### **Caso 2: Navegación en Configuración**

**Flujo:**
1. Usuario accede a `/configuracion`
2. Breadcrumb: `Dashboard > Configuración > Empresa` (tab por defecto)
3. Click en tab "Facturación" → `/configuracion?tab=facturacion`
4. Breadcrumb: `Dashboard > Configuración > Facturación`
5. Click en tab "Notificaciones" → `/configuracion?tab=notificaciones`
6. Breadcrumb: `Dashboard > Configuración > Notificaciones`

**Resultado:** ✅ Breadcrumbs reflejan tab activo

### **Caso 3: Navegación en Reportes**

**Flujo:**
1. Usuario accede a `/reportes`
2. Breadcrumb: `Dashboard > Reportes`
3. Click en "Reporte de Ventas" → `/reportes/ventas`
4. Breadcrumb: `Dashboard > Reportes > Reporte de Ventas`
5. Aplica filtros (query params) → `/reportes/ventas?fechaInicio=2025-01-01`
6. Breadcrumb: `Dashboard > Reportes > Reporte de Ventas` (sin cambios)

**Resultado:** ✅ Filtros no afectan breadcrumbs

---

