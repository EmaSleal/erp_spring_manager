## 🎯 ROLES IMPLEMENTADOS

### 1. **ADMIN (Administrador)**
**Color del badge:** Rojo (#dc3545)

**Permisos:**
- ✅ Acceso **TOTAL** a todos los módulos
- ✅ Configuración del sistema
- ✅ Gestión de usuarios
- ✅ Reportes
- ✅ CRUD completo de Clientes, Productos, Facturas

**Uso:**
```java
@PreAuthorize("hasRole('ADMIN')")
.requestMatchers("/configuracion/**", "/usuarios/**").hasRole("ADMIN")
```

---

### 2. **USER (Usuario)**
**Color del badge:** Azul (#0d6efd)

**Permisos:**
- ✅ Módulos operativos (Clientes, Productos, Facturas)
- ✅ Reportes
- ✅ CRUD completo en módulos operativos
- ❌ NO tiene acceso a Configuración
- ❌ NO puede gestionar usuarios

**Uso:**
```java
.requestMatchers("/clientes/form/**", "/productos/save").hasAnyRole("ADMIN", "USER")
.requestMatchers("/reportes/**").hasAnyRole("ADMIN", "USER")
```

---

### 3. **VENDEDOR**
**Color del badge:** Verde (#198754)

**Permisos:**
- ✅ Ver catálogo de Clientes
- ✅ Ver catálogo de Productos
- ✅ **Crear** facturas
- ✅ Ver facturas existentes
- ❌ NO puede editar/eliminar clientes
- ❌ NO puede editar/eliminar productos
- ❌ NO puede eliminar/anular facturas
- ❌ NO tiene acceso a Configuración
- ❌ NO tiene acceso a Reportes

**Uso:**
```java
.requestMatchers("/facturas/form", "/facturas/save").hasAnyRole("ADMIN", "USER", "VENDEDOR")
.requestMatchers("/clientes", "/productos").hasAnyRole("ADMIN", "USER", "VENDEDOR", "VISUALIZADOR")
```

---

### 4. **VISUALIZADOR**
**Color del badge:** Gris (#6c757d)

**Permisos:**
- ✅ **Solo lectura** de todos los módulos operativos
- ✅ Ver lista de Clientes
- ✅ Ver lista de Productos
- ✅ Ver lista de Facturas
- ❌ NO puede crear nada
- ❌ NO puede editar nada
- ❌ NO puede eliminar nada
- ❌ NO tiene acceso a Configuración
- ❌ NO tiene acceso a Reportes

**Uso:**
```java
.requestMatchers("/clientes", "/productos", "/facturas").hasAnyRole("ADMIN", "USER", "VENDEDOR", "VISUALIZADOR")
```

---

