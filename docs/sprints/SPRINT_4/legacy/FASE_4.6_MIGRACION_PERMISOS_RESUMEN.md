# 🎯 MIGRACIÓN A PERMISOS DINÁMICOS - RESUMEN EJECUTIVO

**Fecha:** 23 de diciembre de 2025  
**Sprint:** Sprint 4 - Fase 4.6  
**Estado:** ✅ Entidades y Scripts Creados - Listos para Ejecutar

---

## 📋 RESUMEN DE CAMBIOS

### ✅ Archivos Creados

#### 1. Entidades JPA (4 archivos)

| Archivo | Ubicación | Descripción |
|---------|-----------|-------------|
| **Permiso.java** | `src/main/java/api/astro/whats_orders_manager/entities/` | Entidad para los 48 permisos del sistema |
| **Rol.java** | `src/main/java/api/astro/whats_orders_manager/entities/` | Entidad para los 3 roles (VENDEDOR, GERENTE, ADMIN) |
| **UsuarioPermiso.java** | `src/main/java/api/astro/whats_orders_manager/entities/` | Entidad para permisos personalizados por usuario |
| **Usuario.java** (actualizado) | `src/main/java/api/astro/whats_orders_manager/models/` | Agregada relación con Rol y UsuarioPermiso |

#### 2. Repositorios JPA (3 archivos)

| Archivo | Ubicación | Métodos Clave |
|---------|-----------|---------------|
| **PermisoRepository.java** | `src/main/java/api/astro/whats_orders_manager/repositories/` | `findByCodigo()`, `findByCategoria()`, `findByEsCriticoTrue()` |
| **RolRepository.java** | `src/main/java/api/astro/whats_orders_manager/repositories/` | `findByCodigo()`, `findByCodigoWithPermisos()` |
| **UsuarioPermisoRepository.java** | `src/main/java/api/astro/whats_orders_manager/repositories/` | `findByUsuario()`, `findPermisosConcedidos()` |

#### 3. Scripts SQL (1 archivo)

| Archivo | Ubicación | Contenido |
|---------|-----------|-----------|
| **MIGRATION_PERMISOS_DINAMICOS.sql** | `docs/base de datos/` | Scripts de INSERT para 48 permisos, 3 roles y asignaciones |

#### 4. Documentación (1 archivo)

| Archivo | Ubicación | Contenido |
|---------|-----------|-----------|
| **MAPEO_SISTEMA_PERMISOS.md** | `docs/` | Mapeo completo del sistema actual + plan de migración |

---

## 🗄️ ESTRUCTURA DE BASE DE DATOS

### Tablas que se Crearán Automáticamente

Al arrancar Spring Boot, JPA/Hibernate creará automáticamente estas tablas:

#### 1. **permiso**
```sql
CREATE TABLE permiso (
    id_permiso BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(100) UNIQUE NOT NULL,
    nombre VARCHAR(255) NOT NULL,
    descripcion TEXT,
    categoria VARCHAR(50) NOT NULL,
    es_critico BOOLEAN DEFAULT FALSE,
    activo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

#### 2. **rol**
```sql
CREATE TABLE rol (
    id_rol BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(50) UNIQUE NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    activo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

#### 3. **rol_permiso** (Many-to-Many)
```sql
CREATE TABLE rol_permiso (
    id_rol BIGINT NOT NULL,
    id_permiso BIGINT NOT NULL,
    PRIMARY KEY (id_rol, id_permiso),
    FOREIGN KEY (id_rol) REFERENCES rol(id_rol) ON DELETE CASCADE,
    FOREIGN KEY (id_permiso) REFERENCES permiso(id_permiso) ON DELETE CASCADE
);
```

#### 4. **usuario_permiso**
```sql
CREATE TABLE usuario_permiso (
    id_usuario_permiso BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_usuario BIGINT NOT NULL,
    id_permiso BIGINT NOT NULL,
    tipo ENUM('CONCEDIDO', 'DENEGADO') NOT NULL,
    concedido_por BIGINT,
    created_at TIMESTAMP,
    FOREIGN KEY (id_usuario) REFERENCES usuario(id) ON DELETE CASCADE,
    FOREIGN KEY (id_permiso) REFERENCES permiso(id_permiso) ON DELETE CASCADE,
    FOREIGN KEY (concedido_por) REFERENCES usuario(id)
);
```

#### 5. **usuario** (columna adicional)
```sql
ALTER TABLE usuario 
ADD COLUMN id_rol BIGINT,
ADD FOREIGN KEY (id_rol) REFERENCES rol(id_rol);
```

---

## 🚀 INSTRUCCIONES DE EJECUCIÓN

### PASO 1: Arrancar Spring Boot

```bash
mvn spring-boot:run
```

**¿Qué sucede?**
- ✅ Spring Boot arranca la aplicación
- ✅ JPA/Hibernate crea automáticamente las 4 tablas nuevas
- ✅ Se agrega la columna `id_rol` a la tabla `usuario`
- ⚠️ Las tablas estarán VACÍAS (sin datos)

### PASO 2: Ejecutar Script SQL

**Abrir tu cliente MySQL favorito** (MySQL Workbench, HeidiSQL, DBeaver, etc.) y ejecutar:

```bash
docs/base de datos/MIGRATION_PERMISOS_DINAMICOS.sql
```

**¿Qué sucede?**
- ✅ Se insertan **48 permisos** en la tabla `permiso`
- ✅ Se insertan **3 roles** en la tabla `rol`
- ✅ Se asignan **15 permisos** al rol VENDEDOR
- ✅ Se asignan **30 permisos** al rol GERENTE
- ✅ Se asignan **48 permisos** al rol ADMIN
- ✅ (Opcional) Se migran usuarios existentes al nuevo sistema

### PASO 3: Verificar Migración

Ejecutar estas consultas para verificar:

```sql
-- Verificar permisos por rol
SELECT r.nombre, COUNT(rp.id_permiso) as total_permisos
FROM rol r
LEFT JOIN rol_permiso rp ON r.id_rol = rp.id_rol
GROUP BY r.nombre;

-- Resultado esperado:
-- VENDEDOR: 15 permisos
-- GERENTE: 30 permisos
-- ADMIN: 48 permisos
```

---

## 📊 DATOS MIGRADOS

### Permisos por Categoría

| Categoría | Cantidad | Permisos Críticos |
|-----------|----------|-------------------|
| Facturación | 7 | 2 (ELIMINAR, ANULAR) |
| Clientes | 5 | 0 |
| Productos | 6 | 0 |
| Reportes | 7 | 0 |
| Configuración | 5 | 1 (EDITAR_EMPRESA) |
| Notificaciones | 5 | 0 |
| Usuarios | 8 | 8 (TODOS) |
| Auditoría | 2 | 2 (TODOS) |
| Sistema | 3 | 3 (TODOS) |
| **TOTAL** | **48** | **19** |

### Permisos por Rol

| Rol | Permisos | Módulos Accesibles |
|-----|----------|-------------------|
| **VENDEDOR** | 15 | Facturas (básico), Clientes, Productos (solo ver), Reportes básicos |
| **GERENTE** | 30 | Todo VENDEDOR + Productos completo + Reportes avanzados + Config (ver) |
| **ADMIN** | 48 | TODOS los módulos (incluyendo Usuarios, Auditoría, Sistema) |

---

## ⚠️ IMPORTANTE - COMPATIBILIDAD

### Sistema Actual (NO TOCAR TODAVÍA)

Estos archivos **aún se están usando** y NO deben modificarse:

- ❌ **NO eliminar** `Permiso.java` (enum)
- ❌ **NO eliminar** `MatrizPermisos.java`
- ❌ **NO modificar** `PermisoService` todavía

**Razón:** El sistema actual sigue funcionando con el enum. La migración completa requiere actualizar:
- Controllers con @PreAuthorize
- Templates con sec:authorize
- PermisoService para consultar base de datos

### Campo Deprecado

- ⚠️ `usuario.rol` (String) → Se considera **deprecado**
- ✅ `usuario.id_rol` (FK a tabla rol) → **Nuevo campo** a usar

**Transición:** Ambos campos coexistirán hasta completar la migración del código.

---

## 📝 PRÓXIMOS PASOS

### Fase 1: Verificación ✅ COMPLETADA
- [x] Crear entidades JPA
- [x] Crear repositorios
- [x] Generar scripts SQL
- [x] Compilar sin errores

### Fase 2: Migración de Datos (SIGUIENTE)
- [ ] Arrancar Spring Boot (crea tablas)
- [ ] Ejecutar script SQL (inserta datos)
- [ ] Verificar 48 permisos + 3 roles
- [ ] Migrar usuarios existentes

### Fase 3: Refactorización de Código (PENDIENTE)
- [ ] Actualizar `PermisoService` para usar BD
- [ ] Actualizar Controllers (@PreAuthorize)
- [ ] Actualizar Templates (sec:authorize)
- [ ] Testing exhaustivo

### Fase 4: Limpieza (PENDIENTE)
- [ ] Marcar `Permiso.java` (enum) como @Deprecated
- [ ] Marcar `MatrizPermisos.java` como @Deprecated
- [ ] Crear admin UI para gestión de permisos
- [ ] Documentar cambios

---

## 🎯 VENTAJAS DEL NUEVO SISTEMA

### Antes (Hardcoded)
❌ Agregar permiso = Modificar código + recompilar  
❌ Cambiar rol = Modificar MatrizPermisos.java  
❌ Permisos personalizados = Imposible  
❌ Auditoría de cambios = No existe  

### Ahora (Base de Datos)
✅ Agregar permiso = INSERT en base de datos  
✅ Cambiar rol = UPDATE en tabla rol_permiso  
✅ Permisos personalizados = Tabla usuario_permiso  
✅ Auditoría de cambios = Timestamps automáticos  
✅ Admin UI = Gestión sin tocar código  

---

## 📞 SOPORTE

### Archivos de Referencia
- **Mapeo completo:** `docs/MAPEO_SISTEMA_PERMISOS.md`
- **Script SQL:** `docs/base de datos/MIGRATION_PERMISOS_DINAMICOS.sql`
- **Entidades:** `src/main/java/api/astro/whats_orders_manager/entities/`

### Verificación de Compilación
```bash
mvn clean compile
# Resultado: BUILD SUCCESS
```

---

**Estado Final:** ✅ **LISTO PARA MIGRACIÓN**  
**Próxima Acción:** Arrancar Spring Boot y ejecutar script SQL
