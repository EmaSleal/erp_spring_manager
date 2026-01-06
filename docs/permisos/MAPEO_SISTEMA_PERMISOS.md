# 📋 MAPEO COMPLETO DEL SISTEMA DE PERMISOS
## WhatsApp Orders Manager - Documentación Técnica

**Fecha de creación:** 23 de diciembre de 2025  
**Versión:** 1.0 - Sprint 4  
**Propósito:** Documento de referencia para migración de permisos hardcodeados a base de datos

---

## 📌 ÍNDICE

1. [Resumen Ejecutivo](#resumen-ejecutivo)
2. [Arquitectura Actual](#arquitectura-actual)
3. [Inventario de Permisos](#inventario-de-permisos)
4. [Matriz de Asignación por Rol](#matriz-de-asignación-por-rol)
5. [Ubicaciones en Código](#ubicaciones-en-código)
6. [Plan de Migración](#plan-de-migración)

---

## 🎯 RESUMEN EJECUTIVO

### Estado Actual
- **Total de Permisos:** 48 permisos granulares
- **Total de Roles:** 3 roles (VENDEDOR, GERENTE, ADMIN)
- **Implementación:** Enum + Clase estática (hardcoded)
- **Ubicaciones afectadas:** 4 controllers + 5 templates + 1 service

### Objetivo de Migración
Convertir el sistema de permisos de **código estático** a **base de datos dinámica**, permitiendo:
- ✅ Crear/modificar permisos sin cambiar código
- ✅ Asignar permisos personalizados por usuario
- ✅ Auditoría completa de cambios de permisos
- ✅ Gestión UI de permisos y roles

---

## 🏗️ ARQUITECTURA ACTUAL

### Componentes del Sistema

```
┌─────────────────────────────────────────────────────────────┐
│                    CAPA DE PRESENTACIÓN                     │
├─────────────────────────────────────────────────────────────┤
│  Templates (Thymeleaf)                                      │
│  - sidebar.html                 [10 usos sec:authorize]     │
│  - clientes.html                [3 usos sec:authorize]      │
│  - productos.html               [1 uso sec:authorize]       │
│  - facturas.html                [5 usos sec:authorize]      │
│  - usuarios.html                [7 usos sec:authorize]      │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    CAPA DE CONTROLLERS                      │
├─────────────────────────────────────────────────────────────┤
│  @PreAuthorize en Métodos                                   │
│  - ClienteController            [3 métodos protegidos]      │
│  - ProductoController           [4 métodos protegidos]      │
│  - FacturaController            [6 métodos protegidos]      │
│  - ConfiguracionController      [2 métodos protegidos]      │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    CAPA DE SERVICIOS                        │
├─────────────────────────────────────────────────────────────┤
│  PermisoService                                             │
│  - tienePermisoByUsername()                                 │
│  - tieneAlgunPermisoByUsername()                            │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    CAPA DE DATOS (HARDCODED)                │
├─────────────────────────────────────────────────────────────┤
│  Permiso.java (Enum)            [48 constantes]            │
│  MatrizPermisos.java (Static)   [Asignación por rol]       │
└─────────────────────────────────────────────────────────────┘
```

---

## 📊 INVENTARIO DE PERMISOS

### Permisos por Categoría

#### 1️⃣ FACTURACIÓN (7 permisos)
| Código | Nombre | Descripción | ¿Crítico? |
|--------|--------|-------------|-----------|
| `FACTURA_VER` | Ver facturas | Visualizar listado y detalle de facturas | ❌ |
| `FACTURA_CREAR` | Crear facturas | Generar nuevas facturas de venta | ❌ |
| `FACTURA_EDITAR` | Editar facturas | Modificar facturas existentes (solo PENDIENTE) | ❌ |
| `FACTURA_ELIMINAR` | Eliminar facturas | Eliminar facturas del sistema | ✅ |
| `FACTURA_ANULAR` | Anular facturas | Anular facturas pagadas/completadas | ✅ |
| `FACTURA_EXPORTAR` | Exportar facturas | Exportar facturas a PDF/Excel | ❌ |
| `FACTURA_ENVIAR_EMAIL` | Enviar facturas | Enviar facturas por email a clientes | ❌ |

#### 2️⃣ CLIENTES (5 permisos)
| Código | Nombre | Descripción | ¿Crítico? |
|--------|--------|-------------|-----------|
| `CLIENTE_VER` | Ver clientes | Visualizar listado y detalle de clientes | ❌ |
| `CLIENTE_CREAR` | Crear clientes | Registrar nuevos clientes | ❌ |
| `CLIENTE_EDITAR` | Editar clientes | Modificar información de clientes | ❌ |
| `CLIENTE_ELIMINAR` | Eliminar clientes | Eliminar clientes del sistema | ❌ |
| `CLIENTE_EXPORTAR` | Exportar clientes | Exportar listado de clientes | ❌ |

#### 3️⃣ PRODUCTOS (6 permisos)
| Código | Nombre | Descripción | ¿Crítico? |
|--------|--------|-------------|-----------|
| `PRODUCTO_VER` | Ver productos | Visualizar catálogo de productos | ❌ |
| `PRODUCTO_CREAR` | Crear productos | Agregar nuevos productos al catálogo | ❌ |
| `PRODUCTO_EDITAR` | Editar productos | Modificar información de productos | ❌ |
| `PRODUCTO_ELIMINAR` | Eliminar productos | Eliminar productos del catálogo | ❌ |
| `PRODUCTO_AJUSTAR_INVENTARIO` | Ajustar inventario | Modificar cantidades en stock | ❌ |
| `PRODUCTO_EXPORTAR` | Exportar productos | Exportar catálogo de productos | ❌ |

#### 4️⃣ REPORTES (7 permisos)
| Código | Nombre | Descripción | ¿Crítico? |
|--------|--------|-------------|-----------|
| `REPORTE_VENTAS` | Reporte de ventas | Ver reporte de ventas y gráficas | ❌ |
| `REPORTE_PRODUCTOS` | Reporte de productos | Ver reporte de productos más vendidos | ❌ |
| `REPORTE_CLIENTES` | Reporte de clientes | Ver reporte de clientes frecuentes | ❌ |
| `REPORTE_DASHBOARD` | Dashboard | Acceso al dashboard principal | ❌ |
| `REPORTE_EXPORTAR_PDF` | Exportar PDF | Exportar reportes a PDF | ❌ |
| `REPORTE_EXPORTAR_EXCEL` | Exportar Excel | Exportar reportes a Excel | ❌ |
| `REPORTE_EXPORTAR_CSV` | Exportar CSV | Exportar reportes a CSV | ❌ |

#### 5️⃣ CONFIGURACIÓN (5 permisos)
| Código | Nombre | Descripción | ¿Crítico? |
|--------|--------|-------------|-----------|
| `CONFIG_VER` | Ver configuración | Visualizar configuración del sistema | ❌ |
| `CONFIG_EDITAR_EMPRESA` | Editar empresa | Modificar datos de la empresa | ✅ |
| `CONFIG_EDITAR_FACTURACION` | Editar facturación | Modificar configuración de facturación | ❌ |
| `CONFIG_EDITAR_EMAIL` | Editar email | Modificar configuración de email | ❌ |
| `CONFIG_EDITAR_WHATSAPP` | Editar WhatsApp | Modificar configuración de WhatsApp | ❌ |

#### 6️⃣ NOTIFICACIONES (5 permisos)
| Código | Nombre | Descripción | ¿Crítico? |
|--------|--------|-------------|-----------|
| `NOTIFICACION_VER` | Ver notificaciones | Ver notificaciones del sistema | ❌ |
| `NOTIFICACION_CREAR` | Crear notificaciones | Enviar notificaciones manuales | ❌ |
| `NOTIFICACION_MARCAR_LEIDA` | Marcar leída | Marcar notificaciones como leídas | ❌ |
| `NOTIFICACION_ELIMINAR` | Eliminar notificaciones | Eliminar notificaciones | ❌ |
| `NOTIFICACION_CONFIGURAR` | Configurar notificaciones | Gestionar preferencias de notificación | ❌ |

#### 7️⃣ USUARIOS (8 permisos) - SOLO ADMIN
| Código | Nombre | Descripción | ¿Crítico? |
|--------|--------|-------------|-----------|
| `USUARIO_VER` | Ver usuarios | Visualizar listado de usuarios | ✅ |
| `USUARIO_CREAR` | Crear usuarios | Registrar nuevos usuarios | ✅ |
| `USUARIO_EDITAR` | Editar usuarios | Modificar información de usuarios | ✅ |
| `USUARIO_ELIMINAR` | Eliminar usuarios | Eliminar usuarios del sistema | ✅ |
| `USUARIO_BLOQUEAR` | Bloquear usuarios | Bloquear/desbloquear usuarios | ✅ |
| `USUARIO_CAMBIAR_ROL` | Cambiar rol | Modificar rol de usuarios | ✅ |
| `USUARIO_VER_ACTIVIDAD` | Ver actividad | Ver registro de actividades de usuarios | ✅ |
| `USUARIO_RESETEAR_PASSWORD` | Resetear contraseña | Forzar cambio de contraseña | ✅ |

#### 8️⃣ AUDITORÍA (2 permisos) - SOLO ADMIN
| Código | Nombre | Descripción | ¿Crítico? |
|--------|--------|-------------|-----------|
| `AUDITORIA_VER` | Ver auditoría | Acceso al registro de auditoría | ✅ |
| `AUDITORIA_EXPORTAR` | Exportar auditoría | Exportar logs de auditoría | ✅ |

#### 9️⃣ SISTEMA (3 permisos) - SOLO ADMIN
| Código | Nombre | Descripción | ¿Crítico? |
|--------|--------|-------------|-----------|
| `SISTEMA_VER_LOGS` | Ver logs | Acceso a logs del sistema | ✅ |
| `SISTEMA_BACKUP` | Backup | Realizar respaldos del sistema | ✅ |
| `SISTEMA_MANTENIMIENTO` | Mantenimiento | Poner sistema en modo mantenimiento | ✅ |

---

## 🔐 MATRIZ DE ASIGNACIÓN POR ROL

### Resumen por Rol

| Rol | Total Permisos | Permisos Críticos | Módulos Accesibles |
|-----|----------------|-------------------|-------------------|
| **VENDEDOR** | 15 | 0 | Facturas (limitado), Clientes, Productos (solo ver), Reportes básicos |
| **GERENTE** | 30 | 3 | Todo lo de VENDEDOR + Productos completo, Reportes avanzados, Configuración (ver) |
| **ADMIN** | 48 | 19 | TODOS los módulos y operaciones |

### Distribución Detallada

#### ROL: VENDEDOR (15 permisos)

```java
// FACTURACIÓN (5 permisos)
✅ FACTURA_VER
✅ FACTURA_CREAR
✅ FACTURA_EDITAR
✅ FACTURA_EXPORTAR
✅ FACTURA_ENVIAR_EMAIL
❌ FACTURA_ELIMINAR (solo GERENTE+)
❌ FACTURA_ANULAR (solo GERENTE+)

// CLIENTES (4 permisos)
✅ CLIENTE_VER
✅ CLIENTE_CREAR
✅ CLIENTE_EDITAR
✅ CLIENTE_EXPORTAR
❌ CLIENTE_ELIMINAR (solo GERENTE+)

// PRODUCTOS (1 permiso)
✅ PRODUCTO_VER
❌ Resto de PRODUCTO_* (solo GERENTE+)

// REPORTES (2 permisos)
✅ REPORTE_VENTAS
✅ REPORTE_DASHBOARD
❌ Resto de REPORTE_* (solo GERENTE+)

// NOTIFICACIONES (3 permisos)
✅ NOTIFICACION_VER
✅ NOTIFICACION_MARCAR_LEIDA
✅ NOTIFICACION_CONFIGURAR
```

#### ROL: GERENTE (30 permisos)

```java
// Hereda TODOS los 15 permisos de VENDEDOR
// + Adicionales:

// FACTURACIÓN (+2 permisos)
✅ FACTURA_ELIMINAR
✅ FACTURA_ANULAR

// CLIENTES (+1 permiso)
✅ CLIENTE_ELIMINAR

// PRODUCTOS (+5 permisos)
✅ PRODUCTO_CREAR
✅ PRODUCTO_EDITAR
✅ PRODUCTO_ELIMINAR
✅ PRODUCTO_AJUSTAR_INVENTARIO
✅ PRODUCTO_EXPORTAR

// REPORTES (+5 permisos)
✅ REPORTE_PRODUCTOS
✅ REPORTE_CLIENTES
✅ REPORTE_EXPORTAR_PDF
✅ REPORTE_EXPORTAR_EXCEL
✅ REPORTE_EXPORTAR_CSV

// CONFIGURACIÓN (+1 permiso)
✅ CONFIG_VER

// NOTIFICACIONES (+1 permiso)
✅ NOTIFICACION_CREAR
```

#### ROL: ADMIN (48 permisos - TODOS)

```java
// Hereda TODOS los 30 permisos de GERENTE
// + Adicionales:

// CONFIGURACIÓN (+4 permisos)
✅ CONFIG_EDITAR_EMPRESA
✅ CONFIG_EDITAR_FACTURACION
✅ CONFIG_EDITAR_EMAIL
✅ CONFIG_EDITAR_WHATSAPP

// NOTIFICACIONES (+1 permiso)
✅ NOTIFICACION_ELIMINAR

// USUARIOS (+8 permisos - EXCLUSIVO)
✅ USUARIO_VER
✅ USUARIO_CREAR
✅ USUARIO_EDITAR
✅ USUARIO_ELIMINAR
✅ USUARIO_BLOQUEAR
✅ USUARIO_CAMBIAR_ROL
✅ USUARIO_VER_ACTIVIDAD
✅ USUARIO_RESETEAR_PASSWORD

// AUDITORÍA (+2 permisos - EXCLUSIVO)
✅ AUDITORIA_VER
✅ AUDITORIA_EXPORTAR

// SISTEMA (+3 permisos - EXCLUSIVO)
✅ SISTEMA_VER_LOGS
✅ SISTEMA_BACKUP
✅ SISTEMA_MANTENIMIENTO
```

---

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

## 🚀 PLAN DE MIGRACIÓN

### FASE 1: Diseño de Base de Datos

#### Nuevas Entidades

```sql
-- Tabla de Permisos
CREATE TABLE permiso (
    id_permiso BIGINT PRIMARY KEY AUTO_INCREMENT,
    codigo VARCHAR(100) UNIQUE NOT NULL,
    nombre VARCHAR(255) NOT NULL,
    descripcion TEXT,
    categoria VARCHAR(50) NOT NULL,
    es_critico BOOLEAN DEFAULT FALSE,
    activo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Tabla de Roles
CREATE TABLE rol (
    id_rol BIGINT PRIMARY KEY AUTO_INCREMENT,
    codigo VARCHAR(50) UNIQUE NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    activo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Tabla intermedia Rol-Permiso
CREATE TABLE rol_permiso (
    id_rol BIGINT NOT NULL,
    id_permiso BIGINT NOT NULL,
    PRIMARY KEY (id_rol, id_permiso),
    FOREIGN KEY (id_rol) REFERENCES rol(id_rol) ON DELETE CASCADE,
    FOREIGN KEY (id_permiso) REFERENCES permiso(id_permiso) ON DELETE CASCADE
);

-- Tabla intermedia Usuario-Permiso (permisos personalizados)
CREATE TABLE usuario_permiso (
    id_usuario BIGINT NOT NULL,
    id_permiso BIGINT NOT NULL,
    tipo ENUM('CONCEDIDO', 'DENEGADO') NOT NULL,
    concedido_por BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_usuario, id_permiso),
    FOREIGN KEY (id_usuario) REFERENCES usuario(id) ON DELETE CASCADE,
    FOREIGN KEY (id_permiso) REFERENCES permiso(id_permiso) ON DELETE CASCADE,
    FOREIGN KEY (concedido_por) REFERENCES usuario(id)
);
```

### FASE 2: Migración de Datos

```sql
-- Script de migración de 48 permisos del enum a DB
INSERT INTO permiso (codigo, nombre, descripcion, categoria, es_critico) VALUES
-- Facturación
('FACTURA_VER', 'Ver facturas', 'Visualizar listado y detalle de facturas', 'Facturación', FALSE),
('FACTURA_CREAR', 'Crear facturas', 'Generar nuevas facturas de venta', 'Facturación', FALSE),
-- ... (resto de 48 permisos)

-- Migración de roles
INSERT INTO rol (codigo, nombre, descripcion) VALUES
('VENDEDOR', 'Vendedor', 'Rol para personal de ventas'),
('GERENTE', 'Gerente', 'Rol para gerentes de área'),
('ADMIN', 'Administrador', 'Rol con acceso total al sistema');

-- Migración de asignaciones rol-permiso desde MatrizPermisos
INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r
CROSS JOIN permiso p
WHERE r.codigo = 'VENDEDOR'
AND p.codigo IN ('FACTURA_VER', 'FACTURA_CREAR', ...); -- 15 permisos
```

### FASE 3: Refactorización de Código

#### 3.1. Nuevas Entidades JPA
- `Permiso.java` (entidad, no enum)
- `Rol.java` (entidad)
- `RolPermiso.java` (entidad)
- `UsuarioPermiso.java` (entidad)

#### 3.2. Repositorios
- `PermisoRepository.java`
- `RolRepository.java`
- `RolPermisoRepository.java`
- `UsuarioPermisoRepository.java`

#### 3.3. Servicios Actualizados
- `PermisoServiceImpl.java` → Consultar BD en lugar de MatrizPermisos
- Nuevos métodos:
  - `crearPermiso()`
  - `asignarPermisoARol()`
  - `asignarPermisoAUsuario()`
  - `revocarPermiso()`

#### 3.4. Controllers Nuevos
- `PermisoAdminController.java` → CRUD de permisos
- `RolAdminController.java` → CRUD de roles

### FASE 4: Actualización de Referencias

#### Backend (15 archivos Java)
1. ❌ Eliminar: `Permiso.java` (enum)
2. ❌ Eliminar: `MatrizPermisos.java` (static)
3. ✅ Crear: `Permiso.java` (entidad JPA)
4. ✅ Actualizar: `PermisoServiceImpl.java`
5. ✅ Actualizar: 4 Controllers con @PreAuthorize

#### Frontend (5 archivos HTML)
1. ✅ Actualizar: `sidebar.html` (10 usos)
2. ✅ Actualizar: `clientes.html` (3 usos)
3. ✅ Actualizar: `productos.html` + JS (4 usos)
4. ✅ Actualizar: `facturas.html` (5 usos)
5. ✅ Actualizar: `usuarios.html` (7 usos)

**Cambio requerido:**
```html
<!-- ANTES -->
sec:authorize="@permisoService.tienePermisoByUsername(#authentication.name, T(api.astro.whats_orders_manager.enums.Permiso).CLIENTE_VER)"

<!-- DESPUÉS -->
sec:authorize="@permisoService.tienePermisoPorCodigo(#authentication.name, 'CLIENTE_VER')"
```

### FASE 5: Testing y Validación

#### Tests Unitarios
- ✅ Verificar permisos por rol
- ✅ Verificar permisos personalizados por usuario
- ✅ Verificar herencia de permisos
- ✅ Verificar denegación de permisos

#### Tests de Integración
- ✅ Endpoints protegidos responden 403 sin permiso
- ✅ Endpoints responden 200 con permiso
- ✅ UI oculta elementos correctamente

#### Tests E2E
- ✅ Crear usuario VENDEDOR → verificar accesos limitados
- ✅ Crear usuario ADMIN → verificar acceso total
- ✅ Asignar permiso personalizado → verificar aplicación

---

## 📝 CHECKLIST DE MIGRACIÓN

### Preparación
- [ ] Backup completo de base de datos
- [ ] Crear rama git: `feature/permisos-dinamicos`
- [ ] Documento de rollback preparado

### Ejecución
- [ ] Crear tablas nuevas (permiso, rol, rol_permiso, usuario_permiso)
- [ ] Ejecutar script de migración de datos
- [ ] Crear entidades JPA
- [ ] Crear repositorios
- [ ] Refactorizar PermisoService
- [ ] Actualizar controllers (backend)
- [ ] Actualizar templates (frontend)
- [ ] Eliminar enum y clase estática

### Validación
- [ ] Tests unitarios pasando
- [ ] Tests de integración pasando
- [ ] Pruebas manuales con cada rol
- [ ] Verificar logs de auditoría
- [ ] Performance acceptable

### Deployment
- [ ] Merge a develop
- [ ] Deploy a staging
- [ ] Validación en staging
- [ ] Deploy a producción
- [ ] Monitoreo post-deployment

---

## 🔍 IMPACTO ESTIMADO

### Archivos a Modificar
- **Backend:** 15 archivos Java
- **Frontend:** 5 archivos HTML + 1 archivo JS
- **Base de Datos:** 4 tablas nuevas + 1 script de migración
- **Tests:** 10+ archivos de test nuevos

### Esfuerzo Estimado
- **Desarrollo:** 40-60 horas
- **Testing:** 20-30 horas
- **Documentación:** 10-15 horas
- **Total:** ~70-105 horas (2-3 semanas)

### Riesgos
- 🔴 **Alto:** Perder permisos críticos de ADMIN
- 🟡 **Medio:** Inconsistencia entre roles y permisos
- 🟢 **Bajo:** Performance degradada

### Mitigación
- ✅ Mantener enum deprecado como fallback
- ✅ Migración gradual por módulo
- ✅ Feature flag para activar/desactivar
- ✅ Rollback plan documentado

---

## 📚 REFERENCIAS

### Archivos Clave
1. `Permiso.java` (enum actual)
2. `MatrizPermisos.java` (matriz actual)
3. `PermisoServiceImpl.java` (servicio actual)
4. `PermisosController.java` (matriz visual)

### Documentación Relacionada
- Sprint 4 - Fase 4.6: Implementación de Permisos
- DECISIONES_SPRINT_3.md
- ESTADO_PROYECTO.md

---

**Fin del Documento**  
_Generado automáticamente el 23/12/2025 para documentar el sistema actual antes de la migración a base de datos._
