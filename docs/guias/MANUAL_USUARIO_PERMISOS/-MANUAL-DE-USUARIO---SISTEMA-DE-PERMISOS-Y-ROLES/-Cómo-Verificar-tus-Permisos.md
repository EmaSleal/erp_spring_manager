## 🔍 Cómo Verificar tus Permisos

### Opción 1: Menú de Navegación

Los módulos visibles en el menú lateral dependen de tus permisos:

**Si eres VENDEDOR verás:**
- 📊 Dashboard
- 👥 Clientes (solo lectura)
- 📦 Productos (solo lectura)
- 🧾 Facturas (crear/editar)
- 📈 Reportes (básicos)
- 🔔 Notificaciones

**Si eres GERENTE verás además:**
- ✏️ Editar clientes
- ✏️ Editar productos
- 📊 Reportes avanzados
- 🗑️ Eliminar facturas

**Si eres ADMIN verás todo:**
- ⚙️ Configuración
- 👤 Usuarios
- 🔐 Permisos
- 📋 Auditoría

---

### Opción 2: Matriz de Permisos (Solo ADMIN)

Si eres **ADMIN**, puedes ver la matriz completa en:

**URL:** `http://tu-servidor/admin/permisos`

**Características:**
- 🔍 Buscar permisos específicos
- 📊 Ver estadísticas por rol
- 💾 Exportar matriz a JSON
- 🖨️ Imprimir matriz

---

### Opción 3: Intentar Realizar una Acción

Si intentas realizar una acción sin permisos:

**Ejemplo:**
> María (VENDEDOR) intenta eliminar un cliente

**Resultado:**
```
❌ Acceso Denegado

No tienes permiso para realizar esta acción.
Permiso requerido: CLIENTE_ELIMINAR
Tu rol: VENDEDOR

Contacta a tu administrador si necesitas este permiso.
```

---

