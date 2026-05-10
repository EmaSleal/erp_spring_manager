## ✏️ Editar Usuario

### Acceso al Formulario de Edición

**Opción 1:** Desde el listado
1. Localice el usuario en la tabla
2. Haga clic en botón **✏️ Editar**
3. Se abre el formulario con datos pre-cargados

**Opción 2:** URL directa
```
/usuarios/form/{id}
```
Ejemplo: `/usuarios/form/5`

### Formulario de Edición

```
┌─────────────────────────────────────────────────────────┐
│  EDITAR USUARIO: Juan Pérez                [Volver]    │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  DATOS PERSONALES                                      │
│  ────────────────                                      │
│                                                         │
│  Nombre completo: *                                    │
│  [Juan Pérez__________________]                        │
│                                                         │
│  Email: *                                              │
│  [juan.perez@empresa.com______]                        │
│  ℹ️ Cambiar el email modificará su usuario de login   │
│                                                         │
│  Teléfono:                                             │
│  [987654321___________________]                        │
│                                                         │
│  ────────────────                                      │
│  CONFIGURACIÓN DE CUENTA                               │
│  ────────────────                                      │
│                                                         │
│  Rol: *                                                │
│  ( ) ADMIN      - Administrador del sistema           │
│  (•) USER       - Usuario estándar                    │
│  ( ) VENDEDOR   - Usuario vendedor                    │
│                                                         │
│  ────────────────                                      │
│  CAMBIAR CONTRASEÑA (Opcional)                         │
│  ────────────────                                      │
│  ℹ️ Deje en blanco para mantener la contraseña actual │
│                                                         │
│  Nueva contraseña:                                     │
│  [____________________________] [🎲 Generar]          │
│                                                         │
│  Confirmar contraseña:                                 │
│  [____________________________]                        │
│                                                         │
│  [✓] Usuario activo                                   │
│                                                         │
│  ────────────────                                      │
│                                                         │
│  Creado: 15/01/2025 10:30 AM por admin@empresa.com    │
│  Última modificación: 04/01/2026 02:45 PM              │
│                                                         │
│  ────────────────                                      │
│                                                         │
│  [Cancelar]                    [Guardar Cambios]       │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Diferencias con Crear Usuario

| Aspecto | Crear | Editar |
|---------|-------|--------|
| **Contraseña** | Obligatoria | Opcional (solo si se quiere cambiar) |
| **Email** | Nuevo | Puede modificarse (se valida que no exista) |
| **Auditoría** | No hay | Muestra fecha de creación y modificación |
| **Enviar email** | Opción disponible | No disponible |

### Campos Editables

✅ **Puede modificar:**
- Nombre completo
- Email
- Teléfono
- Rol
- Estado (activo/inactivo)
- Contraseña (opcional)

❌ **No puede modificar:**
- ID del usuario
- Fecha de creación
- Usuario que lo creó

### Cambiar Contraseña al Editar

**Escenario 1: NO cambiar contraseña**
```
Nueva contraseña: [____________] (dejar en blanco)
Confirmar:        [____________] (dejar en blanco)

Resultado: Se mantiene la contraseña actual
```

**Escenario 2: Cambiar contraseña**
```
Nueva contraseña: [NuevaPass123]
Confirmar:        [NuevaPass123]

Resultado: Se actualiza la contraseña
```

**⚠️ Importante:**
- Si llena "Nueva contraseña", DEBE llenar "Confirmar"
- Si solo llena uno de los dos, mostrará error
- La nueva contraseña debe cumplir los requisitos (6+ caracteres)

### Cambiar Rol de Usuario

**Ejemplo: Promover USER a ADMIN**

1. Localice el campo "Rol"
2. Seleccione `( ) ADMIN`
3. Guarde cambios
4. El usuario ahora tendrá permisos de administrador

**⚠️ Precaución:**
```
⚠️ Advertencia: Cambio de Rol
   Está cambiando el rol de USER a ADMIN.
   Esto le dará permisos administrativos al usuario.
   
   ¿Está seguro de continuar?
   
   [Cancelar]  [Sí, cambiar rol]
```

### Desactivar Usuario (sin eliminar)

**Uso:** Bloquear acceso temporalmente

**Pasos:**
1. En el formulario de edición
2. Desmarcar `[ ] Usuario activo`
3. Guardar cambios

**Resultado:**
```
✅ Usuario actualizado
   El usuario ha sido desactivado.
   No podrá iniciar sesión hasta que sea reactivado.
```

**Estado en el listado:**
```
│ Juan Pérez │juan@empresa.com│USER│🔴 Inactivo│[✏️][🗑️][🔐]│
```

**⚠️ Al intentar login:**
```
❌ Error de autenticación
   Tu cuenta ha sido desactivada.
   Contacta al administrador del sistema.
```

---

