## 📍 UBICACIONES EN CÓDIGO

### 1. BACKEND - JAVA

#### 1.1. Enum de Permisos
**Archivo:** `src/main/java/api/astro/whats_orders_manager/enums/Permiso.java`  
**Líneas:** 1-137  
**Permisos definidos:** 48

```java
public enum Permiso {
    // Categorías:
    // - FACTURA_* (líneas 18-24)
    // - CLIENTE_* (líneas 28-32)
    // - PRODUCTO_* (líneas 36-41)
    // - REPORTE_* (líneas 45-51)
    // - CONFIG_* (líneas 55-59)
    // - NOTIFICACION_* (líneas 63-67)
    // - USUARIO_* (líneas 71-78)
    // - AUDITORIA_* (líneas 82-83)
    // - SISTEMA_* (líneas 87-89)
}
```

#### 1.2. Matriz de Permisos
**Archivo:** `src/main/java/api/astro/whats_orders_manager/config/MatrizPermisos.java`  
**Líneas:** 1-271  
**Métodos clave:**
- `getPermisos(String rol)` - línea 153
- `tienePermiso(String rol, Permiso permiso)` - línea 162
- `tieneAlgunPermiso(String rol, Permiso... permisos)` - línea 172
- `tieneTodosLosPermisos(String rol, Permiso... permisos)` - línea 185

#### 1.3. PermisoService
**Archivo:** `src/main/java/api/astro/whats_orders_manager/services/impl/PermisoServiceImpl.java`  
**Métodos clave:**
- `tienePermisoByUsername(String username, Permiso permiso)` - línea ~50
- `tieneAlgunPermisoByUsername(String username, Permiso... permisos)` - línea ~70
- `obtenerEstadisticasPermisos()` - línea ~90

#### 1.4. Controllers con @PreAuthorize

**ClienteController.java** (3 usos)
```java
// Línea 44
@PreAuthorize("@permisoService.tienePermisoByUsername(#authentication.name, T(Permiso).CLIENTE_VER)")

// Línea 89
@PreAuthorize("@permisoService.tienePermisoByUsername(#authentication.name, T(Permiso).CLIENTE_CREAR)")

// Línea 151
@PreAuthorize("@permisoService.tienePermisoByUsername(#authentication.name, T(Permiso).CLIENTE_EDITAR)")
```

**ProductoController.java** (4 usos)
```java
// Línea 47
@PreAuthorize("@permisoService.tienePermisoByUsername(#authentication.name, T(Permiso).PRODUCTO_VER)")

// Línea 99
@PreAuthorize("@permisoService.tienePermisoByUsername(#authentication.name, T(Permiso).PRODUCTO_EDITAR)")

// Línea 111
@PreAuthorize("@permisoService.tienePermisoByUsername(#authentication.name, T(Permiso).PRODUCTO_ELIMINAR)")

// Línea 130
@PreAuthorize("@permisoService.tienePermisoByUsername(#authentication.name, T(Permiso).PRODUCTO_CREAR)")
```

**FacturaController.java** (6 usos)
```java
// Línea 58, 110 - FACTURA_VER (duplicado)
// Línea 133, 153 - FACTURA_CREAR (duplicado)
// Línea 173 - FACTURA_ELIMINAR
// Línea 216 - FACTURA_EDITAR
```

**ConfiguracionController.java** (2 usos)
```java
// Línea 63 - CONFIG_VER
// Línea 125 - CONFIG_EDITAR_EMPRESA
```

### 2. FRONTEND - THYMELEAF

#### 2.1. sidebar.html (10 usos)

| Línea | Permiso | Elemento |
|-------|---------|----------|
| 26 | `CLIENTE_VER` | Menú Clientes |
| 40 | `PRODUCTO_VER` | Menú Productos |
| 54 | `FACTURA_VER` | Menú Facturas |
| 68 | `REPORTE_DASHBOARD` | Menú Reportes |
| 82 | `CONFIG_EDITAR_WHATSAPP` | Menú WhatsApp |
| 163-169 | `tieneAlgunPermiso(USUARIO_VER, CONFIG_VER)` | Divider Administración |
| 176 | `USUARIO_VER` | Menú Usuarios |
| 190 | `USUARIO_VER` | Menú Permisos |
| 204 | `CONFIG_VER` | Menú Configuración |

#### 2.2. clientes/clientes.html (3 usos)

| Línea | Permiso | Elemento |
|-------|---------|----------|
| 43 | `CLIENTE_CREAR` | Botón "Agregar Cliente" |
| 81 | `CLIENTE_EDITAR` | Botón "Editar" (tabla) |
| 87 | `CLIENTE_ELIMINAR` | Botón "Eliminar" (tabla) |

#### 2.3. productos/productos.html (1 uso + JS)

| Archivo | Línea | Permiso | Elemento |
|---------|-------|---------|----------|
| productos.html | 43 | `PRODUCTO_CREAR` | Botón "Agregar Producto" |
| productos.html | 155-157 | JS variables | `permisos.editar`, `permisos.eliminar` |
| productos.js | 227-229 | `permisos.editar` | Botón "Editar" (renderizado JS) |
| productos.js | 233-235 | `permisos.eliminar` | Botón "Eliminar" (renderizado JS) |

#### 2.4. facturas/facturas.html (5 usos)

| Línea | Permiso | Elemento |
|-------|---------|----------|
| 43 | `FACTURA_CREAR` | Botón "Nueva Factura" |
| 132 | `FACTURA_EDITAR` | Botón "Editar" |
| 140 | `FACTURA_ENVIAR` | Botón "Enviar Email" |
| 148 | `CONFIG_EDITAR_WHATSAPP` | Botón "Enviar WhatsApp" |
| 155 | `FACTURA_ELIMINAR` | Botón "Eliminar" |

#### 2.5. usuarios/usuarios.html (7 usos)

| Línea | Permiso | Elemento |
|-------|---------|----------|
| 33 | `USUARIO_CREAR` | Botón "Nuevo Usuario" |
| 215-218 | `tieneAlgunPermiso(...)` | Columna "Acciones" (header) |
| 275-280 | `tieneAlgunPermiso(...)` | Columna "Acciones" (td) |
| 292 | `USUARIO_EDITAR` | Botón "Editar" |
| 306 | `USUARIO_BLOQUEAR` | Botón "Toggle Activo/Inactivo" |
| 317, 330 | `USUARIO_EDITAR` | Botones "Reset Password" y "Reenviar Credenciales" |
| 342 | `USUARIO_ELIMINAR` | Botón "Eliminar" |

---

