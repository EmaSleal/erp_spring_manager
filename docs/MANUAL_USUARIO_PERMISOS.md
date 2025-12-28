# 📖 MANUAL DE USUARIO - SISTEMA DE PERMISOS Y ROLES

**WhatsApp Orders Manager - Sistema ERP**  
**Versión:** 1.0 - Sprint 4  
**Fecha:** 22 de diciembre de 2025  
**Audiencia:** Administradores y Usuarios del Sistema

---

## 📑 Índice

1. [Introducción](#introducción)
2. [¿Qué son los Roles y Permisos?](#qué-son-los-roles-y-permisos)
3. [Roles Disponibles](#roles-disponibles)
4. [Matriz de Permisos](#matriz-de-permisos)
5. [Cómo Verificar tus Permisos](#cómo-verificar-tus-permisos)
6. [Solicitar Cambios de Permisos](#solicitar-cambios-de-permisos)
7. [Preguntas Frecuentes](#preguntas-frecuentes)

---

## 🎯 Introducción

El **Sistema de Permisos y Roles** de WhatsApp Orders Manager garantiza que cada usuario tenga acceso únicamente a las funcionalidades necesarias para realizar su trabajo. Esto mejora la seguridad y organización del sistema.

### ¿Por qué es importante?

- ✅ **Seguridad**: Protege información sensible
- ✅ **Organización**: Cada usuario tiene herramientas específicas para su rol
- ✅ **Auditoría**: Se registra quién hace qué en el sistema
- ✅ **Eficiencia**: Interfaces simplificadas según responsabilidades

---

## 🔐 ¿Qué son los Roles y Permisos?

### Rol
Un **rol** es un conjunto de permisos asignado a un usuario según su función en la empresa.

**Ejemplo:**
> María es Vendedora → Tiene rol **VENDEDOR**  
> Carlos es Gerente → Tiene rol **GERENTE**  
> Ana es Dueña → Tiene rol **ADMIN**

### Permiso
Un **permiso** es una autorización específica para realizar una acción en el sistema.

**Ejemplos de permisos:**
- `FACTURA_CREAR` - Crear nuevas facturas
- `CLIENTE_EDITAR` - Modificar datos de clientes
- `USUARIO_VER` - Ver listado de usuarios del sistema

---

## 👥 Roles Disponibles

El sistema tiene **3 roles principales** con diferentes niveles de acceso:

### 🔴 ADMIN (Administrador)
**Para:** Dueños, Gerente General, Administrador del Sistema

**Características:**
- ✅ Acceso total al sistema (48 permisos)
- ✅ Gestionar usuarios y roles
- ✅ Modificar configuración global
- ✅ Realizar backups del sistema
- ✅ Ver auditorías y logs

**Ejemplo de usuario:**
> **Ana Rodríguez** - Dueña de la empresa  
> Necesita acceso completo para supervisar operaciones y configurar el sistema

---

### 🟢 GERENTE (Gerente Operativo)
**Para:** Gerentes, Supervisores, Coordinadores

**Características:**
- ✅ Operaciones completas (30+ permisos)
- ✅ CRUD de clientes, productos y facturas
- ✅ Acceso a todos los reportes
- ✅ Exportar datos a PDF/Excel
- ❌ NO puede gestionar usuarios
- ❌ NO puede cambiar configuración del sistema

**Ejemplo de usuario:**
> **Carlos Martínez** - Gerente de Ventas  
> Puede gestionar todo el proceso de ventas, ver reportes y exportar datos, pero no puede crear usuarios ni cambiar configuración global

---

### 🔵 VENDEDOR (Vendedor/Ejecutivo)
**Para:** Vendedores, Ejecutivos de Cuenta, Personal de Ventas

**Características:**
- ✅ Acceso básico (15 permisos)
- ✅ Ver clientes, productos y facturas
- ✅ Crear y editar facturas
- ✅ Ver reportes de ventas
- ✅ Configurar sus notificaciones
- ❌ NO puede eliminar registros
- ❌ NO puede acceder a configuración
- ❌ NO puede ver usuarios del sistema

**Ejemplo de usuario:**
> **María López** - Vendedora  
> Puede consultar productos, crear facturas para sus clientes y ver sus estadísticas de ventas

---

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

## 📝 Solicitar Cambios de Permisos

### ¿Cuándo solicitar un cambio?

Solicita un cambio de permisos si:
- ✅ Necesitas acceso a un módulo para hacer tu trabajo
- ✅ Te cambiaron de puesto/responsabilidades
- ✅ Recibes error de "Acceso Denegado" frecuentemente

### ¿Cómo solicitarlo?

**Paso 1:** Identifica qué permiso necesitas
- Anota el nombre del permiso del mensaje de error
- Ejemplo: `PRODUCTO_EDITAR`

**Paso 2:** Contacta a tu administrador
- Por email, chat o teléfono
- Explica por qué necesitas el permiso

**Paso 3:** Espera aprobación
- El admin evaluará tu solicitud
- Te notificará cuando se apruebe

### ¿Quién puede aprobar cambios?

Solo usuarios con rol **ADMIN** pueden:
- ✅ Cambiar roles de usuarios
- ✅ Asignar permisos personalizados
- ✅ Crear nuevos usuarios

---

## ❓ Preguntas Frecuentes

### 1. ¿Puedo tener múltiples roles?

**Respuesta:** Actualmente, cada usuario tiene **un solo rol** asignado. Si necesitas permisos de múltiples roles, contacta a tu administrador para evaluar crear un rol personalizado.

---

### 2. ¿Qué pasa si me bloquean la cuenta?

**Respuesta:** Si tu cuenta está bloqueada:
- ❌ No podrás iniciar sesión
- ❌ Todas tus sesiones activas se cerrarán
- ✅ Tus datos y facturas se mantienen intactos

**Razones comunes de bloqueo:**
- Intentos fallidos de login (seguridad)
- Incumplimiento de políticas de la empresa
- Usuario inactivo por tiempo prolongado

**Solución:** Contacta a tu administrador para desbloquear tu cuenta.

---

### 3. ¿Puedo ver facturas de otros vendedores?

**Respuesta:**
- **VENDEDOR:** Solo ve sus propias facturas
- **GERENTE:** Ve todas las facturas
- **ADMIN:** Ve todas las facturas

---

### 4. ¿Por qué no puedo eliminar un cliente?

**Respuesta:** El permiso `CLIENTE_ELIMINAR` solo está disponible para **GERENTE** y **ADMIN**. 

**Razón:** Eliminar clientes es una acción irreversible que puede afectar reportes históricos.

**Alternativa:** Si necesitas "ocultar" un cliente, contacta a tu gerente o admin.

---

### 5. ¿Puedo cambiar mi propio rol?

**Respuesta:** ❌ No. Solo usuarios **ADMIN** pueden cambiar roles.

**Razón:** Esto previene escalación de privilegios no autorizada.

---

### 6. ¿Qué significa "Permiso Crítico"?

**Respuesta:** Los **permisos críticos** son aquellos que pueden:
- Eliminar datos importantes
- Modificar configuración global
- Afectar la seguridad del sistema
- Crear/eliminar usuarios

**Ejemplos:**
- 🔴 `USUARIO_ELIMINAR` - Eliminar usuarios
- 🔴 `USUARIO_CAMBIAR_ROL` - Cambiar roles
- 🔴 `CONFIG_EDITAR_EMPRESA` - Modificar datos de empresa
- 🔴 `SISTEMA_BACKUP` - Realizar backups

**Estos permisos son exclusivos de ADMIN.**

---

### 7. ¿Se registra lo que hago en el sistema?

**Respuesta:** ✅ Sí. El sistema registra:
- ✅ Inicios de sesión
- ✅ Acciones importantes (crear, editar, eliminar)
- ✅ Cambios en configuración
- ✅ Intentos de acceso denegado

**Razón:** Auditoría y seguridad.

**¿Quién puede ver estos registros?** Solo **ADMIN** con permiso `AUDITORIA_VER`.

---

### 8. ¿Puedo exportar reportes si soy VENDEDOR?

**Respuesta:** ✅ Sí. Los VENDEDORES pueden:
- Ver reportes de sus propias ventas
- Exportar a PDF, Excel y CSV
- Ver estadísticas del dashboard

❌ No pueden:
- Ver reportes de otros vendedores
- Acceder a reportes analíticos avanzados

---

### 9. ¿Cómo sé si mi rol cambió?

**Respuesta:** Recibirás una **notificación** cuando:
- Tu rol cambie
- Se te asignen nuevos permisos
- Tu cuenta sea bloqueada/desbloqueada

Además, verás los cambios reflejados en:
- 📱 Menú de navegación (nuevos módulos)
- 🔔 Centro de notificaciones
- 🏷️ Badge de tu perfil

---

### 10. ¿Qué hago si recibo "Acceso Denegado" incorrectamente?

**Pasos:**
1. **Verifica tu rol** en tu perfil de usuario
2. **Anota el error** exacto (incluye nombre de permiso)
3. **Contacta a soporte** o administrador
4. **Proporciona:**
   - Captura de pantalla del error
   - Fecha y hora exacta
   - Qué intentabas hacer

**Posibles causas:**
- Error de configuración
- Sesión expirada
- Cambio reciente de rol no sincronizado

---

## 📚 Recursos Adicionales

### Para Usuarios

- 📖 [Manual de Usuario General](./MANUAL_USUARIO_GENERAL.md)
- 🎥 [Videos Tutoriales](./TUTORIALES.md)
- 💬 [Soporte Técnico](mailto:soporte@empresa.com)

### Para Administradores

- 🔧 [Guía de Administración](./GUIA_ADMINISTRACION.md)
- 🔐 [Matriz de Permisos Completa](http://localhost:8080/admin/permisos)
- 📊 [Dashboard de Auditoría](http://localhost:8080/admin/auditoria)

---

## 📞 Contacto

**¿Tienes preguntas sobre permisos?**

📧 **Email:** admin@tuempresa.com  
📱 **Teléfono:** +52 xxx-xxx-xxxx  
💬 **Chat:** Soporte técnico en sistema

---

## 📝 Registro de Cambios

| Versión | Fecha | Cambios |
|---------|-------|---------|
| 1.0 | 22/12/2025 | Manual inicial - 48 permisos, 3 roles |

---

**Última actualización:** 22 de diciembre de 2025  
**Versión del sistema:** Sprint 4  
**Elaborado por:** Equipo de Desarrollo WhatsApp Orders Manager
