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

