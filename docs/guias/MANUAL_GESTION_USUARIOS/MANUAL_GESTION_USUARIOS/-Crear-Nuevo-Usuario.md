## ➕ Crear Nuevo Usuario

### Acceso al Formulario

**Opción 1:** Desde el listado
1. Haga clic en botón **"+ Nuevo Usuario"** (esquina superior derecha)
2. Se abre el formulario de creación

**Opción 2:** URL directa
```
/usuarios/form
```

### Formulario de Nuevo Usuario

```
┌─────────────────────────────────────────────────────────┐
│  NUEVO USUARIO                           [Volver]      │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  DATOS PERSONALES                                      │
│  ────────────────                                      │
│                                                         │
│  Nombre completo: *                                    │
│  [____________________________]                        │
│                                                         │
│  Email: *                                              │
│  [____________________________]                        │
│  ℹ️ Este será su nombre de usuario                    │
│                                                         │
│  Teléfono:                                             │
│  [____________________________]                        │
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
│  Contraseña: *                                         │
│  [____________________________] [🎲 Generar]          │
│  ℹ️ Mínimo 6 caracteres                               │
│                                                         │
│  Confirmar contraseña: *                               │
│  [____________________________]                        │
│                                                         │
│  [✓] Enviar credenciales por email                    │
│  [✓] Usuario activo                                   │
│                                                         │
│  ────────────────                                      │
│                                                         │
│  [Cancelar]                      [Guardar Usuario]     │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Campos del Formulario

#### 1. Nombre completo *

**Obligatorio:** Sí  
**Validación:** 3-100 caracteres  
**Ejemplos válidos:**
- ✅ Juan Pérez
- ✅ María José García López
- ✅ Pedro

**Ejemplos inválidos:**
- ❌ AB (muy corto)
- ❌ (vacío)

---

#### 2. Email *

**Obligatorio:** Sí  
**Validación:** Formato de email válido  
**Único:** No puede repetirse  

**Ejemplos válidos:**
- ✅ juan.perez@empresa.com
- ✅ maria@gmail.com
- ✅ pedro_lopez@yahoo.com

**Ejemplos inválidos:**
- ❌ juan@empresa (sin dominio)
- ❌ @empresa.com (sin usuario)
- ❌ juan perez@empresa.com (espacios)

**⚠️ Importante:** 
- Este email será el **nombre de usuario** para login
- Debe ser único en el sistema
- Se usará para enviar notificaciones

---

#### 3. Teléfono

**Obligatorio:** No  
**Validación:** 7-15 dígitos  
**Formato:** Solo números (sin espacios ni guiones)

**Ejemplos válidos:**
- ✅ 987654321
- ✅ 51987654321 (con código país)
- ✅ 987654

**Ejemplos inválidos:**
- ❌ 987-654-321 (con guiones)
- ❌ +51 987 654 321 (con espacios y +)
- ❌ (123) 456-7890 (formato US)

**Uso:**
- Contacto del usuario
- Notificaciones por WhatsApp (si se configura)

---

#### 4. Rol *

**Obligatorio:** Sí  
**Opciones:**

| Rol | Descripción | ¿Cuándo usar? |
|-----|-------------|---------------|
| **ADMIN** | Administrador del sistema | Gerentes, contadores, jefes |
| **USER** | Usuario estándar | Personal administrativo |
| **VENDEDOR** | Usuario vendedor | Vendedores, agentes comerciales |

**Por defecto:** USER

**⚠️ Restricción:** 
- Solo SUPER_ADMIN puede crear usuarios con rol ADMIN
- Los ADMIN normales solo pueden crear USER y VENDEDOR

---

#### 5. Contraseña *

**Obligatorio:** Sí  
**Validación:** Mínimo 6 caracteres  
**Recomendación:** 8+ caracteres con números y símbolos

**Generador automático:**
1. Haga clic en botón **"🎲 Generar"**
2. Se genera una contraseña aleatoria segura
3. Ejemplo: `xK9m2#pL`

**Reglas de seguridad:**
```
✅ Mínimo 6 caracteres
✅ Puede contener letras, números, símbolos
⚠️ No puede contener espacios
💡 Recomendado: 8+ caracteres
```

**Ejemplos de contraseñas:**
```
Débiles:
❌ 123456
❌ password
❌ admin

Aceptables:
✅ Usuario123
✅ Empresa2024

Fuertes:
✅ xK9m2#pL
✅ MyP@ssw0rd!
✅ S3gur@2024$
```

---

#### 6. Confirmar contraseña *

**Obligatorio:** Sí  
**Validación:** Debe coincidir exactamente con la contraseña

**Mensaje de error:**
```
❌ Las contraseñas no coinciden
   Por favor, verifique que ambos campos sean idénticos.
```

---

#### 7. Opciones Adicionales

**🔲 Enviar credenciales por email**
- ✅ **Marcado:** Se envía email al usuario con su contraseña
- ☐ **Desmarcado:** Debe comunicar manualmente las credenciales

**Contenido del email:**
```
Asunto: Bienvenido al Sistema - Credenciales de Acceso

Hola Juan Pérez,

Se ha creado una cuenta para ti en el sistema:

Usuario: juan.perez@empresa.com
Contraseña: xK9m2#pL

Accede en: http://localhost:9090/auth/login

Por seguridad, te recomendamos cambiar tu contraseña 
en el primer inicio de sesión.

Saludos,
Equipo de Administración
```

**🔲 Usuario activo**
- ✅ **Marcado (por defecto):** Usuario puede iniciar sesión inmediatamente
- ☐ **Desmarcado:** Usuario creado pero bloqueado

---

### Procedimiento Completo: Crear Usuario

#### Ejemplo Práctico

**Objetivo:** Crear un vendedor llamado "Carlos Mendoza"

**Paso 1: Acceder al formulario**
- Clic en "➕ Nuevo Usuario"

**Paso 2: Llenar datos personales**
```
Nombre completo: Carlos Mendoza
Email: carlos.mendoza@empresa.com
Teléfono: 987654321
```

**Paso 3: Configurar cuenta**
```
Rol: (•) VENDEDOR
```

**Paso 4: Generar contraseña**
- Clic en "🎲 Generar"
- Contraseña generada: `pL5#kMx9`

**Paso 5: Confirmar contraseña**
```
Contraseña: pL5#kMx9
Confirmar: pL5#kMx9
```

**Paso 6: Opciones**
```
[✓] Enviar credenciales por email
[✓] Usuario activo
```

**Paso 7: Guardar**
- Clic en "Guardar Usuario"

**Resultado:**
```
✅ Usuario creado exitosamente
   Email enviado a carlos.mendoza@empresa.com
   El usuario puede iniciar sesión ahora.

[Ver Listado]
```

---

### Validaciones del Sistema

El sistema valida automáticamente:

#### 1. Email único
```
❌ Error: El email ya está registrado
   El email juan@empresa.com ya pertenece a otro usuario.
```

**Solución:** Use un email diferente

---

#### 2. Teléfono único
```
❌ Error: El teléfono ya está registrado
   El número 987654321 ya pertenece a otro usuario.
```

**Solución:** 
- Use un teléfono diferente
- Deje el campo vacío

---

#### 3. Contraseña muy corta
```
❌ Error: Contraseña inválida
   La contraseña debe tener al menos 6 caracteres.
```

**Solución:** Use una contraseña más larga

---

#### 4. Contraseñas no coinciden
```
❌ Error: Las contraseñas no coinciden
   Por favor, verifique que ambos campos sean idénticos.
```

**Solución:** Escriba nuevamente ambas contraseñas

---

### Mensajes de Éxito

**Creación exitosa:**
```
✅ Usuario creado exitosamente
   Se ha creado el usuario y se enviaron las credenciales por email.
```

**Creación exitosa (sin email):**
```
✅ Usuario creado exitosamente
   ⚠️ No se pudo enviar el email de notificación.
   Por favor, comunique las credenciales manualmente.
```

---

