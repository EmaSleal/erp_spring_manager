## 📋 Listar Usuarios

### Vista Principal

Al acceder al módulo verá la pantalla de listado:

```
┌───────────────────────────────────────────────────────────────┐
│  GESTIÓN DE USUARIOS                          [+ Nuevo Usuario]│
├───────────────────────────────────────────────────────────────┤
│                                                                │
│  📊 Estadísticas Rápidas:                                     │
│  ┌──────────┬──────────┬──────────┬──────────┬──────────┐    │
│  │ Total: 15│Activos: 12│Inactivos: 3│Admins: 3│Vendedores: 8││
│  └──────────┴──────────┴──────────┴──────────┴──────────┘    │
│                                                                │
│  🔍 Buscar: [________________]  🎯 Rol: [Todos ▼]            │
│      Estado: [Todos ▼]          Ordenar: [Nombre ▼]          │
│                                                                │
├───────────────────────────────────────────────────────────────┤
│  Nombre        │Email            │Rol    │Estado  │Acciones   │
├────────────────┼─────────────────┼───────┼────────┼───────────┤
│  Juan Pérez    │juan@empresa.com │ADMIN  │🟢 Activo│[✏️][🗑️][🔐]│
│  María García  │maria@empresa.com│USER   │🟢 Activo│[✏️][🗑️][🔐]│
│  Pedro López   │pedro@empresa.com│VEND.  │🔴 Inactivo│[✏️][🗑️][🔐]│
└───────────────────────────────────────────────────────────────┘
│  Mostrando 1-10 de 15    [◀ 1 2 ▶]                          │
└───────────────────────────────────────────────────────────────┘
```

### Componentes de la Pantalla

#### 1. Estadísticas Rápidas

Muestra un resumen general:

| Estadística | Descripción |
|-------------|-------------|
| **Total** | Cantidad total de usuarios registrados |
| **Activos** | Usuarios que pueden iniciar sesión |
| **Inactivos** | Usuarios bloqueados |
| **Admins** | Usuarios con rol ADMIN/SUPER_ADMIN |
| **Vendedores** | Usuarios con rol VENDEDOR |

**Actualización:** Las estadísticas se actualizan automáticamente al crear/eliminar usuarios.

---

#### 2. Barra de Búsqueda y Filtros

**Búsqueda por texto:**
- Busca en: nombre, email, teléfono
- En tiempo real (mientras escribe)
- No distingue mayúsculas/minúsculas

**Ejemplo:**
```
🔍 Buscar: juan

Resultados:
  - Juan Pérez
  - Juan Carlos Rojas
  - María Juana Torres
```

**Filtro por Rol:**

| Opción | Muestra |
|--------|---------|
| Todos | Todos los usuarios |
| ADMIN | Solo administradores |
| USER | Solo usuarios estándar |
| VENDEDOR | Solo vendedores |

**Filtro por Estado:**

| Opción | Muestra |
|--------|---------|
| Todos | Activos e inactivos |
| Activo | Solo usuarios activos |
| Inactivo | Solo usuarios bloqueados |

**Ordenamiento:**

| Campo | Orden |
|-------|-------|
| Nombre | A-Z / Z-A |
| Email | A-Z / Z-A |
| Fecha creación | Más reciente / Más antiguo |
| Rol | ADMIN → USER → VENDEDOR |

---

#### 3. Tabla de Usuarios

**Columnas:**

| Columna | Información |
|---------|-------------|
| **Nombre** | Nombre completo del usuario |
| **Email** | Correo electrónico (username) |
| **Rol** | SUPER_ADMIN / ADMIN / USER / VENDEDOR |
| **Estado** | 🟢 Activo / 🔴 Inactivo |
| **Acciones** | Botones de editar / eliminar / resetear |

**Íconos de estado:**
- 🟢 **Verde:** Usuario activo (puede iniciar sesión)
- 🔴 **Rojo:** Usuario inactivo (bloqueado)

---

#### 4. Botones de Acción

| Botón | Ícono | Función | Permiso |
|-------|-------|---------|---------|
| **Editar** | ✏️ | Modificar datos del usuario | ADMIN |
| **Eliminar** | 🗑️ | Borrar usuario (permanente) | ADMIN |
| **Resetear** | 🔐 | Cambiar contraseña | ADMIN |
| **Toggle** | 🔄 | Activar/desactivar | ADMIN |

**⚠️ Restricción:** No puedes editar/eliminar tu propia cuenta.

---

#### 5. Paginación

**Configuración por defecto:**
- **Tamaño de página:** 10 usuarios
- **Navegación:** Botones ◀ 1 2 3 ▶

**Opciones de tamaño:**
- 10 por página
- 25 por página
- 50 por página
- 100 por página

**Ejemplo de navegación:**
```
Mostrando 11-20 de 45    [◀ 1 [2] 3 4 5 ▶]
```

---

### Procedimiento: Buscar un Usuario

#### Búsqueda Simple

1. En el campo "🔍 Buscar", escriba el nombre o email
2. Los resultados se filtran automáticamente
3. No es necesario presionar Enter

**Ejemplo:**
```
Buscar: maria

Resultados filtrados:
  - María García
  - María José López
  - Ana María Torres
```

#### Búsqueda Avanzada (con filtros)

1. **Paso 1:** Seleccione el rol deseado
   ```
   Rol: [VENDEDOR ▼]
   ```

2. **Paso 2:** Seleccione el estado
   ```
   Estado: [Activo ▼]
   ```

3. **Paso 3:** (Opcional) Agregue texto de búsqueda
   ```
   🔍 Buscar: juan
   ```

4. **Resultado:** Solo vendedores activos llamados "Juan"

#### Limpiar Filtros

**Opción 1:** Botón "Limpiar Filtros"
- Resetea todos los filtros
- Vuelve a mostrar todos los usuarios

**Opción 2:** Recargar página (F5)

---

### Exportar Listado de Usuarios

**Formato:** CSV

**Pasos:**
1. Aplique los filtros deseados (opcional)
2. Haga clic en botón **"Exportar CSV"**
3. Se descarga archivo `usuarios.csv`

**Contenido del CSV:**
```csv
Nombre,Email,Teléfono,Rol,Estado,Fecha Creación
Juan Pérez,juan@empresa.com,987654321,ADMIN,Activo,2025-01-15
María García,maria@empresa.com,987654322,USER,Activo,2025-02-20
Pedro López,pedro@empresa.com,987654323,VENDEDOR,Inactivo,2025-03-10
```

**Uso:**
- Auditoría de usuarios
- Backup de información
- Importación a Excel

---

