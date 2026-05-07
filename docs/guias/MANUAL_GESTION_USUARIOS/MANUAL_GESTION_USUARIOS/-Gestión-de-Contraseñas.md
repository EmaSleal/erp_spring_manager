## 🔐 Gestión de Contraseñas

### Resetear Contraseña de Usuario

**¿Cuándo usar?**
- Usuario olvidó su contraseña
- Necesita generar una contraseña temporal
- Por seguridad (sospecha de cuenta comprometida)

**Acceso:**
- Desde el listado de usuarios
- Botón 🔐 **"Resetear Contraseña"**

### Procedimiento de Reset

**Paso 1: Hacer clic en botón de reseteo**
```
│ Juan Pérez │juan@empresa.com│USER│🟢 Activo│[✏️][🗑️][🔐]│
                                                    ↑
                                                   AQUÍ
```

**Paso 2: Confirmación**
```
┌──────────────────────────────────────────────┐
│  ⚠️ Resetear Contraseña                     │
├──────────────────────────────────────────────┤
│                                              │
│  Se generará una nueva contraseña aleatoria │
│  para el usuario:                            │
│                                              │
│  Usuario: Juan Pérez                         │
│  Email: juan.perez@empresa.com               │
│                                              │
│  La nueva contraseña se enviará por email.  │
│                                              │
│  ¿Continuar?                                 │
│                                              │
│  [Cancelar]           [Sí, resetear]        │
└──────────────────────────────────────────────┘
```

**Paso 3: Sistema genera nueva contraseña**
- Se genera contraseña aleatoria (ej: `mK8#xP2q`)
- Se encripta y guarda en base de datos
- Se envía por email al usuario

**Paso 4: Mensaje de éxito**
```
✅ Contraseña reseteada exitosamente
   
   Nueva contraseña: mK8#xP2q
   
   ℹ️ Se ha enviado un email a juan.perez@empresa.com
      con las nuevas credenciales.
   
   💡 Recomendación: El usuario debe cambiarla en su
      primer inicio de sesión.
   
   [Copiar contraseña]  [Cerrar]
```

### Email de Reset de Contraseña

**Asunto:** `Contraseña Reseteada - Sistema de Gestión`

**Contenido:**
```
Hola Juan Pérez,

Tu contraseña ha sido reseteada por un administrador.

Nuevas credenciales:
━━━━━━━━━━━━━━━━━━━━━━━━
Usuario: juan.perez@empresa.com
Contraseña: mK8#xP2q
━━━━━━━━━━━━━━━━━━━━━━━━

Accede aquí:
http://localhost:8080/auth/login

🔒 Por seguridad, te recomendamos cambiar tu 
   contraseña en Perfil > Cambiar Contraseña

Si no solicitaste este cambio, contacta 
inmediatamente al administrador.

Saludos,
Equipo de Administración
```

### Reenviar Credenciales

**¿Cuándo usar?**
- Usuario no recibió el email de bienvenida
- Email original fue eliminado
- Usuario olvidó sus credenciales

**Acceso:**
- Desde el listado o formulario de edición
- Botón **"Reenviar Credenciales"**

**Proceso:**
1. Genera nueva contraseña aleatoria
2. Reemplaza la anterior
3. Envía email con credenciales

**⚠️ Importante:** Esto INVALIDARÁ la contraseña actual del usuario.

### Cambiar Contraseña desde Perfil (Usuario)

**Nota:** Los usuarios pueden cambiar su propia contraseña desde su perfil:
- Menú → **Mi Perfil** → Tab **"Seguridad"**
- Requiere contraseña actual
- No requiere intervención del admin

**Beneficio:** El admin NO conoce las contraseñas de los usuarios.

---

