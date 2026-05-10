## 👑 Roles y Permisos

### Roles Disponibles

El sistema cuenta con los siguientes roles:

#### 1. SUPER_ADMIN 🔴

**Descripción:** Administrador principal del sistema

**Permisos:**
- ✅ **Acceso total** al sistema
- ✅ Gestión completa de usuarios (incluidos otros admins)
- ✅ Configuración del sistema
- ✅ Acceso a todos los módulos
- ✅ Visualización de logs y auditoría
- ✅ Gestión de permisos dinámicos

**Cantidad recomendada:** 1-2 usuarios

**Uso típico:**
- Propietario del negocio
- CTO / Responsable de IT

---

#### 2. ADMIN 🟠

**Descripción:** Administrador del sistema

**Permisos:**
- ✅ Gestión de usuarios (excepto SUPER_ADMIN)
- ✅ Configuración de empresa y facturación
- ✅ Gestión de clientes, productos y facturas
- ✅ Acceso a reportes y estadísticas
- ✅ Configuración de notificaciones
- ❌ No puede modificar otros ADMIN
- ❌ No puede acceder a logs del sistema

**Cantidad recomendada:** 2-5 usuarios

**Uso típico:**
- Gerente general
- Contador
- Jefe de ventas

---

#### 3. USER 🟡

**Descripción:** Usuario estándar del sistema

**Permisos:**
- ✅ Gestión de clientes
- ✅ Gestión de productos
- ✅ Creación y edición de facturas
- ✅ Visualización de reportes propios
- ✅ Configuración de perfil personal
- ❌ No acceso a configuración del sistema
- ❌ No acceso a gestión de usuarios
- ❌ No acceso a reportes completos

**Cantidad:** Ilimitada

**Uso típico:**
- Vendedores
- Personal administrativo
- Asistentes

---

#### 4. VENDEDOR 🟢

**Descripción:** Usuario enfocado en ventas

**Permisos:**
- ✅ Gestión de clientes (crear, editar)
- ✅ Visualización de productos
- ✅ Creación de facturas
- ✅ Consulta de sus propias ventas
- ❌ No gestión de productos (solo lectura)
- ❌ No acceso a reportes generales
- ❌ No configuración del sistema

**Cantidad:** Ilimitada

**Uso típico:**
- Vendedores de campo
- Vendedores de tienda
- Agentes comerciales

---

### Comparativa de Roles

| Funcionalidad | SUPER_ADMIN | ADMIN | USER | VENDEDOR |
|---------------|:-----------:|:-----:|:----:|:--------:|
| Gestionar usuarios | ✅ | ✅ | ❌ | ❌ |
| Configuración sistema | ✅ | ✅ | ❌ | ❌ |
| Gestionar clientes | ✅ | ✅ | ✅ | ✅ |
| Gestionar productos | ✅ | ✅ | ✅ | 👁️ Ver |
| Crear facturas | ✅ | ✅ | ✅ | ✅ |
| Reportes completos | ✅ | ✅ | 👁️ Propios | 👁️ Propios |
| Logs del sistema | ✅ | ❌ | ❌ | ❌ |
| Eliminar datos | ✅ | ✅ | ⚠️ Limitado | ❌ |

**Leyenda:**
- ✅ Acceso completo
- 👁️ Solo lectura / Vista limitada
- ⚠️ Con restricciones
- ❌ Sin acceso

---

