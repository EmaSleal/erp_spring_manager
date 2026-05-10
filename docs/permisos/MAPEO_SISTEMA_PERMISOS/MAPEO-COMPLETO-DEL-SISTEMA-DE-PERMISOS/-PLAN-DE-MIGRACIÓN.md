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

