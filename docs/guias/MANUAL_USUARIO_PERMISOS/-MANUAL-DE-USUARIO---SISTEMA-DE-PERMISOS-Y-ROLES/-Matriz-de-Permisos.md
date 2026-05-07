## 📊 Matriz de Permisos

### 1. Dashboard

| Permiso | Descripción | ADMIN | GERENTE | VENDEDOR |
|---------|-------------|:-----:|:-------:|:--------:|
| `REPORTE_DASHBOARD` | Acceso al dashboard principal | ✅ | ✅ | ✅ |

**¿Qué significa?**  
Todos los roles pueden ver el dashboard con información resumida.

---

### 2. Clientes

| Permiso | Descripción | ADMIN | GERENTE | VENDEDOR |
|---------|-------------|:-----:|:-------:|:--------:|
| `CLIENTE_VER` | Ver listado y detalle de clientes | ✅ | ✅ | ✅ |
| `CLIENTE_CREAR` | Registrar nuevos clientes | ✅ | ✅ | ❌ |
| `CLIENTE_EDITAR` | Modificar datos de clientes | ✅ | ✅ | ❌ |
| `CLIENTE_ELIMINAR` | Eliminar clientes | ✅ | ✅ | ❌ |
| `CLIENTE_EXPORTAR` | Exportar listado a Excel/PDF | ✅ | ✅ | ❌ |

**Casos de uso:**
- **VENDEDOR** puede consultar datos de clientes para crear facturas
- **GERENTE** puede agregar, editar y eliminar clientes
- **ADMIN** tiene control total incluyendo exportación

---

### 3. Productos

| Permiso | Descripción | ADMIN | GERENTE | VENDEDOR |
|---------|-------------|:-----:|:-------:|:--------:|
| `PRODUCTO_VER` | Ver catálogo de productos | ✅ | ✅ | ✅ |
| `PRODUCTO_CREAR` | Agregar nuevos productos | ✅ | ✅ | ❌ |
| `PRODUCTO_EDITAR` | Modificar productos existentes | ✅ | ✅ | ❌ |
| `PRODUCTO_ELIMINAR` | Eliminar productos | ✅ | ✅ | ❌ |
| `PRODUCTO_AJUSTAR_INVENTARIO` | Ajustar cantidades en stock | ✅ | ✅ | ❌ |
| `PRODUCTO_EXPORTAR` | Exportar catálogo | ✅ | ✅ | ❌ |

**Casos de uso:**
- **VENDEDOR** consulta el catálogo para crear facturas
- **GERENTE** gestiona inventario y precios
- **ADMIN** tiene control total del catálogo

---

### 4. Facturación

| Permiso | Descripción | ADMIN | GERENTE | VENDEDOR |
|---------|-------------|:-----:|:-------:|:--------:|
| `FACTURA_VER` | Ver facturas | ✅ | ✅ | ✅ |
| `FACTURA_CREAR` | Generar nuevas facturas | ✅ | ✅ | ✅ |
| `FACTURA_EDITAR` | Modificar facturas pendientes | ✅ | ✅ | ✅ |
| `FACTURA_ELIMINAR` | Eliminar facturas | ✅ | ✅ | ❌ |
| `FACTURA_ANULAR` | Anular facturas pagadas | ✅ | ✅ | ❌ |
| `FACTURA_EXPORTAR` | Exportar a PDF/Excel | ✅ | ✅ | ❌ |
| `FACTURA_ENVIAR_EMAIL` | Enviar facturas por email | ✅ | ✅ | ✅ |

**Casos de uso:**
- **VENDEDOR** crea y edita sus facturas, puede enviarlas por email
- **GERENTE** puede eliminar y anular facturas
- **ADMIN** tiene control total

**⚠️ Nota importante:** Solo se pueden editar facturas con estado **PENDIENTE**

---

### 5. Reportes

| Permiso | Descripción | ADMIN | GERENTE | VENDEDOR |
|---------|-------------|:-----:|:-------:|:--------:|
| `REPORTE_VENTAS` | Ver reporte de ventas | ✅ | ✅ | ✅ |
| `REPORTE_PRODUCTOS` | Ver productos más vendidos | ✅ | ✅ | ❌ |
| `REPORTE_CLIENTES` | Ver clientes frecuentes | ✅ | ✅ | ❌ |
| `REPORTE_EXPORTAR_PDF` | Exportar reportes a PDF | ✅ | ✅ | ✅ |
| `REPORTE_EXPORTAR_EXCEL` | Exportar reportes a Excel | ✅ | ✅ | ✅ |
| `REPORTE_EXPORTAR_CSV` | Exportar reportes a CSV | ✅ | ✅ | ✅ |

**Casos de uso:**
- **VENDEDOR** ve sus estadísticas de ventas y puede exportarlas
- **GERENTE** accede a todos los reportes analíticos
- **ADMIN** tiene acceso completo a reportes

---

### 6. Configuración

| Permiso | Descripción | ADMIN | GERENTE | VENDEDOR |
|---------|-------------|:-----:|:-------:|:--------:|
| `CONFIG_VER` | Ver configuración del sistema | ✅ | ❌ | ❌ |
| `CONFIG_EDITAR_EMPRESA` | Modificar datos de la empresa | ✅ | ❌ | ❌ |
| `CONFIG_EDITAR_FACTURACION` | Modificar config. de facturación | ✅ | ❌ | ❌ |
| `CONFIG_EDITAR_EMAIL` | Modificar config. de email | ✅ | ❌ | ❌ |
| `CONFIG_EDITAR_WHATSAPP` | Modificar config. de WhatsApp | ✅ | ❌ | ❌ |

**🔒 Permisos exclusivos de ADMIN**

**Razón:** La configuración afecta a todo el sistema y debe ser manejada solo por administradores.

---

### 7. Notificaciones

| Permiso | Descripción | ADMIN | GERENTE | VENDEDOR |
|---------|-------------|:-----:|:-------:|:--------:|
| `NOTIFICACION_VER` | Ver notificaciones | ✅ | ✅ | ✅ |
| `NOTIFICACION_CREAR` | Enviar notificaciones manuales | ✅ | ✅ | ❌ |
| `NOTIFICACION_MARCAR_LEIDA` | Marcar como leída | ✅ | ✅ | ✅ |
| `NOTIFICACION_ELIMINAR` | Eliminar notificaciones | ✅ | ✅ | ❌ |
| `NOTIFICACION_CONFIGURAR` | Configurar preferencias | ✅ | ✅ | ✅ |

**Casos de uso:**
- Todos pueden ver y configurar sus notificaciones
- VENDEDOR puede marcar como leídas
- GERENTE y ADMIN pueden enviar notificaciones masivas

---

### 8. Usuarios (Solo ADMIN)

| Permiso | Descripción | ADMIN | GERENTE | VENDEDOR |
|---------|-------------|:-----:|:-------:|:--------:|
| `USUARIO_VER` | Ver listado de usuarios | ✅ | ❌ | ❌ |
| `USUARIO_CREAR` | Registrar nuevos usuarios | ✅ | ❌ | ❌ |
| `USUARIO_EDITAR` | Modificar datos de usuarios | ✅ | ❌ | ❌ |
| `USUARIO_ELIMINAR` | Eliminar usuarios | ✅ | ❌ | ❌ |
| `USUARIO_BLOQUEAR` | Bloquear/desbloquear usuarios | ✅ | ❌ | ❌ |
| `USUARIO_CAMBIAR_ROL` | Modificar rol de usuarios | ✅ | ❌ | ❌ |
| `USUARIO_VER_ACTIVIDAD` | Ver log de actividades | ✅ | ❌ | ❌ |
| `USUARIO_RESETEAR_PASSWORD` | Forzar cambio de contraseña | ✅ | ❌ | ❌ |

**🔒 Permisos críticos exclusivos de ADMIN**

**Razón:** La gestión de usuarios es sensible y solo debe ser manejada por administradores del sistema.

---

### 9. Auditoría (Solo ADMIN)

| Permiso | Descripción | ADMIN | GERENTE | VENDEDOR |
|---------|-------------|:-----:|:-------:|:--------:|
| `AUDITORIA_VER` | Ver registros de auditoría | ✅ | ❌ | ❌ |
| `AUDITORIA_EXPORTAR` | Exportar logs de auditoría | ✅ | ❌ | ❌ |

**🔒 Permisos exclusivos de ADMIN**

---

### 10. Sistema (Solo ADMIN)

| Permiso | Descripción | ADMIN | GERENTE | VENDEDOR |
|---------|-------------|:-----:|:-------:|:--------:|
| `SISTEMA_VER_LOGS` | Acceso a logs del sistema | ✅ | ❌ | ❌ |
| `SISTEMA_BACKUP` | Realizar respaldos | ✅ | ❌ | ❌ |
| `SISTEMA_MANTENIMIENTO` | Modo mantenimiento | ✅ | ❌ | ❌ |

**🔒 Permisos críticos exclusivos de ADMIN**

---

