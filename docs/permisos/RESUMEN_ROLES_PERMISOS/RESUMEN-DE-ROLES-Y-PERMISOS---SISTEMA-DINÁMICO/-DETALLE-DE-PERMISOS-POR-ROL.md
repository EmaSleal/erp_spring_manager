## 📋 DETALLE DE PERMISOS POR ROL

### 1️⃣ ROL: ADMIN (48 permisos - TODOS)

**Acceso Total al Sistema**

```
✅ FACTURACIÓN: 7/7 permisos (100%)
✅ CLIENTES: 5/5 permisos (100%)
✅ PRODUCTOS: 6/6 permisos (100%)
✅ REPORTES: 7/7 permisos (100%)
✅ CONFIGURACIÓN: 5/5 permisos (100%)
✅ NOTIFICACIONES: 5/5 permisos (100%)
✅ USUARIOS: 8/8 permisos (100%) - EXCLUSIVO
✅ AUDITORÍA: 2/2 permisos (100%) - EXCLUSIVO
✅ SISTEMA: 3/3 permisos (100%) - EXCLUSIVO
```

**Permisos Críticos:** 19 permisos exclusivos de ADMIN

---

### 2️⃣ ROL: GERENTE (30 permisos)

**Gestión Completa de Operaciones**

```
✅ FACTURACIÓN: 7/7 permisos (100%)
   - Ver, Crear, Editar, Eliminar, Anular, Exportar, Enviar Email

✅ CLIENTES: 5/5 permisos (100%)
   - Ver, Crear, Editar, Eliminar, Exportar

✅ PRODUCTOS: 6/6 permisos (100%)
   - Ver, Crear, Editar, Eliminar, Ajustar Inventario, Exportar

✅ REPORTES: 7/7 permisos (100%)
   - Ventas, Productos, Clientes, Dashboard, Exportar (PDF, Excel, CSV)

⚠️ CONFIGURACIÓN: 1/5 permisos (20%)
   - ✅ Ver configuración
   - ❌ Editar (requiere ADMIN)

✅ NOTIFICACIONES: 4/5 permisos (80%)
   - Ver, Crear, Marcar Leída, Configurar
   - ❌ Eliminar (requiere ADMIN)

❌ USUARIOS: 0/8 permisos (Solo ADMIN)
❌ AUDITORÍA: 0/2 permisos (Solo ADMIN)
❌ SISTEMA: 0/3 permisos (Solo ADMIN)
```

---

### 3️⃣ ROL: VENDEDOR (15 permisos)

**Operaciones Básicas de Venta**

```
✅ FACTURACIÓN: 5/7 permisos (71%)
   - Ver, Crear, Editar, Exportar, Enviar Email
   - ❌ Eliminar, Anular (requiere GERENTE)

✅ CLIENTES: 4/5 permisos (80%)
   - Ver, Crear, Editar, Exportar
   - ❌ Eliminar (requiere GERENTE)

⚠️ PRODUCTOS: 1/6 permisos (17%)
   - ✅ Ver productos (solo lectura)
   - ❌ Crear, Editar, Eliminar (requiere GERENTE)

⚠️ REPORTES: 2/7 permisos (29%)
   - Ver Ventas, Dashboard
   - ❌ Otros reportes (requiere GERENTE)

✅ NOTIFICACIONES: 3/5 permisos (60%)
   - Ver, Marcar Leída, Configurar

❌ CONFIGURACIÓN: 0/5 permisos
❌ USUARIOS: 0/8 permisos
❌ AUDITORÍA: 0/2 permisos
❌ SISTEMA: 0/3 permisos
```

---

### 4️⃣ ROL: USER (10 permisos)

**Usuario Básico con Permisos Limitados**

```
⚠️ FACTURACIÓN: 3/7 permisos (43%)
   - Ver, Crear, Exportar
   - ❌ Editar, Eliminar, Anular, Enviar Email

⚠️ CLIENTES: 2/5 permisos (40%)
   - Ver, Crear
   - ❌ Editar, Eliminar, Exportar

⚠️ PRODUCTOS: 1/6 permisos (17%)
   - ✅ Ver productos (solo lectura)

⚠️ REPORTES: 2/7 permisos (29%)
   - Ver Ventas, Dashboard

⚠️ NOTIFICACIONES: 2/5 permisos (40%)
   - Ver, Marcar Leída

❌ CONFIGURACIÓN: 0/5 permisos
❌ USUARIOS: 0/8 permisos
❌ AUDITORÍA: 0/2 permisos
❌ SISTEMA: 0/3 permisos
```

**Diferencia con VENDEDOR:**
- No puede editar facturas ni clientes
- No puede enviar emails de facturas
- No puede exportar clientes
- No puede configurar notificaciones

---

### 5️⃣ ROL: VISUALIZADOR (8 permisos)

**Solo Lectura - Sin Modificación de Datos**

```
⚠️ FACTURACIÓN: 2/7 permisos (29%)
   - ✅ Ver, Exportar
   - ❌ Crear, Editar, Eliminar, Anular, Enviar Email

⚠️ CLIENTES: 2/5 permisos (40%)
   - ✅ Ver, Exportar
   - ❌ Crear, Editar, Eliminar

⚠️ PRODUCTOS: 2/6 permisos (33%)
   - ✅ Ver, Exportar
   - ❌ Crear, Editar, Eliminar, Ajustar Inventario

⚠️ REPORTES: 1/7 permisos (14%)
   - ✅ Dashboard
   - ❌ Otros reportes

⚠️ NOTIFICACIONES: 1/5 permisos (20%)
   - ✅ Ver
   - ❌ Crear, Marcar Leída, Configurar, Eliminar

❌ CONFIGURACIÓN: 0/5 permisos
❌ USUARIOS: 0/8 permisos
❌ AUDITORÍA: 0/2 permisos
❌ SISTEMA: 0/3 permisos
```

**Uso Típico:** Auditores externos, analistas de datos, consultores

---

### 6️⃣ ROL: CLIENTE (3 permisos)

**Acceso Externo - Solo Facturas Propias**

```
⚠️ FACTURACIÓN: 2/7 permisos (29%)
   - ✅ Ver (solo sus facturas), Exportar
   - ❌ Todo lo demás

⚠️ NOTIFICACIONES: 1/5 permisos (20%)
   - ✅ Ver (solo sus notificaciones)

❌ CLIENTES: 0/5 permisos
❌ PRODUCTOS: 0/6 permisos
❌ REPORTES: 0/7 permisos
❌ CONFIGURACIÓN: 0/5 permisos
❌ USUARIOS: 0/8 permisos
❌ AUDITORÍA: 0/2 permisos
❌ SISTEMA: 0/3 permisos
```

**Uso Típico:** Clientes externos que necesitan consultar sus facturas

**⚠️ NOTA IMPORTANTE:** Este rol requiere lógica adicional en el backend para filtrar:
- Solo mostrar facturas del cliente autenticado
- Solo mostrar notificaciones del cliente autenticado

---

