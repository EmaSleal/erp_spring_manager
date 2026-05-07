## 🎯 MATRIZ DE ROLES Y PERMISOS

### Roles Implementados

| Rol | Color Badge | Descripción | Casos de Uso |
|-----|-------------|-------------|--------------|
| **ADMIN** | 🔴 Rojo | Administrador con acceso total | Dueño, Gerente General |
| **AGENTE** | 🔵 Azul | Usuario operativo completo | Vendedores, Ejecutivos de cuenta |
| **CONTADOR** | 🟢 Verde | Acceso a reportes y facturas (solo lectura) | Contador, Auditor |
| **VIEWER** | 🟡 Amarillo | Solo visualización de información básica | Invitados, Consultores externos |

### Matriz de Permisos por Módulo

| Módulo | ADMIN | AGENTE | CONTADOR | VIEWER |
|--------|-------|--------|----------|--------|
| **Dashboard** | ✅ Total | ✅ Total | ✅ Total | ✅ Solo métricas básicas |
| **Clientes** | ✅ CRUD | ✅ CRUD | ❌ | ❌ |
| **Productos** | ✅ CRUD | ✅ CRUD | ❌ | ❌ |
| **Facturas** | ✅ CRUD | ✅ CRUD | 👁️ Solo lectura | 👁️ Solo lectura |
| **Reportes** | ✅ Todos | ✅ Todos | ✅ Todos | 👁️ Básicos |
| **Usuarios** | ✅ CRUD | ❌ | ❌ | ❌ |
| **Configuración** | ✅ Total | ❌ | 👁️ Solo lectura | ❌ |
| **Notificaciones** | ✅ Configurar | ✅ Ver historial | ❌ | ❌ |

**Leyenda:**
- ✅ = Acceso completo (CRUD)
- 👁️ = Solo lectura
- ❌ = Sin acceso

---

