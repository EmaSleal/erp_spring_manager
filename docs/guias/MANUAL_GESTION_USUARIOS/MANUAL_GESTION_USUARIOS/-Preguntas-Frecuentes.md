## ❓ Preguntas Frecuentes

### ¿Cuántos usuarios puedo crear?

**Respuesta:** Ilimitados.

El sistema no tiene límite de usuarios. Puedes crear tantos como necesites.

**Recomendación:**
- Crear solo usuarios necesarios
- Eliminar cuentas de prueba
- Mantener el sistema ordenado

---

### ¿Puedo recuperar un usuario eliminado?

**Respuesta:** NO.

La eliminación es **permanente e irreversible**.

**Alternativas:**
1. Crear nuevo usuario con los mismos datos (tendrá ID diferente)
2. Restaurar desde backup (si existe)
3. **Mejor práctica:** DESACTIVAR en lugar de eliminar

---

### ¿Los usuarios pueden cambiar su propio rol?

**Respuesta:** NO.

Solo los administradores pueden cambiar roles.

Los usuarios normales pueden:
- ✅ Cambiar su nombre
- ✅ Cambiar su contraseña
- ✅ Actualizar su email
- ✅ Modificar su teléfono
- ❌ NO cambiar su rol
- ❌ NO desactivar su cuenta
- ❌ NO ver otros usuarios

---

### ¿Qué pasa si elimino un admin?

**Respuesta:** Se elimina normalmente.

**Precaución:**
- Asegúrate de tener otro admin activo
- No puedes eliminar el último SUPER_ADMIN
- No puedes eliminarte a ti mismo

**Verificación:**
```
Antes de eliminar, verifica:
[✓] ¿Hay otro SUPER_ADMIN activo?
[✓] ¿Hay otro ADMIN activo?
[✓] ¿No es mi propia cuenta?
```

---

### ¿Puedo tener múltiples usuarios con el mismo email?

**Respuesta:** NO.

El email debe ser **único** en todo el sistema.

**Razones:**
- El email es el username para login
- Se usa para enviar notificaciones
- Identifica unívocamente al usuario

**Solución si necesitas varios usuarios de la misma persona:**
```
Usar variaciones:
- juan.perez@empresa.com
- j.perez@empresa.com
- jperez@empresa.com
- juan.p@empresa.com
```

---

### ¿Cómo sé qué usuarios están activos?

**Respuesta:** Filtrar en el listado.

```
Estado: [Activo ▼]

Muestra solo usuarios activos (pueden iniciar sesión)
```

**Estadísticas:**
```
Panel superior muestra:
Activos: 12 ← Usuarios que pueden usar el sistema
```

**Identificación visual:**
```
🟢 Verde = Activo
🔴 Rojo = Inactivo
```

---

### ¿Puedo cambiar el email de un usuario?

**Respuesta:** SÍ.

**Pasos:**
1. Editar usuario
2. Cambiar el email
3. Guardar

**⚠️ Importante:**
- El email debe ser único
- El nuevo email será su nuevo username
- Se validará que no exista
- Informar al usuario del cambio

**Notificar al usuario:**
```
Hola Juan,

Tu email de acceso ha cambiado:

Antes: juan@empresa.com
Ahora: juan.perez@empresa.com

Usa el nuevo email para iniciar sesión.

Saludos,
Administración
```

---

### ¿Los usuarios ven el listado de otros usuarios?

**Respuesta:** NO.

Solo los **ADMIN** y **SUPER_ADMIN** pueden ver el módulo de usuarios.

**Usuarios normales (USER, VENDEDOR):**
- ❌ No ven el menú "Usuarios"
- ❌ No pueden acceder a /usuarios
- ❌ No conocen a otros usuarios del sistema
- ✅ Solo ven su propio perfil

---

### ¿Qué rol debo asignar a un nuevo empleado?

**Guía de decisión:**

```
┌─────────────────────────────────────────┐
│  Árbol de Decisión - Asignar Rol       │
├─────────────────────────────────────────┤
│                                         │
│  ¿Necesita gestionar usuarios?         │
│   ├─ SÍ → ADMIN                        │
│   └─ NO ↓                              │
│                                         │
│  ¿Necesita configurar el sistema?      │
│   ├─ SÍ → ADMIN                        │
│   └─ NO ↓                              │
│                                         │
│  ¿Necesita ver todos los reportes?     │
│   ├─ SÍ → ADMIN                        │
│   └─ NO ↓                              │
│                                         │
│  ¿Solo necesita facturar y vender?     │
│   ├─ SÍ → VENDEDOR                     │
│   └─ NO ↓                              │
│                                         │
│  ¿Necesita gestionar productos?        │
│   ├─ SÍ → USER                         │
│   └─ NO → VENDEDOR                     │
│                                         │
└─────────────────────────────────────────┘
```

**Ejemplos:**
```
Gerente General → ADMIN
Contador → ADMIN
Jefe de Ventas → ADMIN
Vendedor de tienda → VENDEDOR
Vendedor de campo → VENDEDOR
Asistente administrativo → USER
Almacenero → USER
Recepcionista → VENDEDOR
```

---

### ¿Cuánto tiempo se guardan los datos de usuarios eliminados?

**Respuesta:** Datos personales: Eliminados inmediatamente

**Detalles:**
```
Eliminados al instante:
❌ Nombre
❌ Email
❌ Teléfono
❌ Contraseña
❌ Avatar
❌ Preferencias

Se conservan (con referencia):
✅ Facturas creadas (autor: [eliminado])
✅ Logs del sistema (ID + [eliminado])
✅ Auditoría (para cumplimiento)
```

**Cumplimiento GDPR/LOPD:**
- Datos personales se eliminan (right to be forgotten)
- Se mantiene info operativa sin datos personales
- Logs se anonimizan

---

### ¿Puedo exportar la lista de usuarios?

**Respuesta:** SÍ.

```
Botón: [Exportar CSV]
Archivo: usuarios.csv
```

**Contenido:**
```csv
Nombre,Email,Teléfono,Rol,Estado,Fecha Creación
Juan Pérez,juan@empresa.com,987654321,ADMIN,Activo,2025-01-15
María García,maria@empresa.com,987654322,USER,Activo,2025-02-20
```

**Uso:**
- Auditoría
- Backup
- Análisis en Excel
- Reportes para gerencia

---

### ¿Hay límite de intentos de login?

**Respuesta:** Sí (configuración de seguridad).

**Por defecto:**
- 5 intentos fallidos
- Bloqueo temporal de 15 minutos
- Notificación al admin

**Desbloquear:**
```
Como admin:
1. Editar usuario
2. Botón "Desbloquear cuenta"
3. O resetear contraseña
```

---

