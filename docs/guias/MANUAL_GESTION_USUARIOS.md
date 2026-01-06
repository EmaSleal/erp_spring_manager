# 👥 Manual de Administrador - Gestión de Usuarios

**Versión:** 1.0  
**Fecha:** 4 de enero de 2026  
**Audiencia:** Administradores del sistema  
**Nivel de acceso requerido:** ROL_ADMIN

---

## 📑 Tabla de Contenidos

1. [Introducción](#introducción)
2. [Roles y Permisos](#roles-y-permisos)
3. [Acceso al Módulo de Usuarios](#acceso-al-módulo-de-usuarios)
4. [Listar Usuarios](#listar-usuarios)
5. [Crear Nuevo Usuario](#crear-nuevo-usuario)
6. [Editar Usuario](#editar-usuario)
7. [Gestión de Contraseñas](#gestión-de-contraseñas)
8. [Activar/Desactivar Usuarios](#activardesactivar-usuarios)
9. [Eliminar Usuarios](#eliminar-usuarios)
10. [Solución de Problemas](#solución-de-problemas)
11. [Preguntas Frecuentes](#preguntas-frecuentes)

---

## 📖 Introducción

El **Módulo de Gestión de Usuarios** permite a los administradores crear, modificar y gestionar las cuentas de usuario del sistema. Este manual explica cómo realizar todas las operaciones administrativas sobre usuarios.

### Funcionalidades Principales

- ✅ **Crear usuarios** - Registrar nuevos usuarios del sistema
- ✅ **Editar usuarios** - Modificar datos de usuarios existentes
- ✅ **Gestionar contraseñas** - Resetear y generar contraseñas
- ✅ **Activar/Desactivar** - Bloquear acceso sin eliminar usuario
- ✅ **Eliminar usuarios** - Borrar cuentas permanentemente
- ✅ **Asignar roles** - Controlar permisos de acceso
- ✅ **Búsqueda y filtros** - Localizar usuarios rápidamente
- ✅ **Envío de credenciales** - Notificar por email

### ⚠️ Requisitos de Acceso

**Para acceder a este módulo necesitas:**
- Rol: **ADMIN** o **SUPER_ADMIN**
- Permiso: `USUARIOS_GESTIONAR`

⚠️ **Importante:** Los usuarios con rol `USER` o `VENDEDOR` NO pueden acceder a este módulo.

---

## 👑 Roles y Permisos

### Roles Disponibles

El sistema cuenta con los siguientes roles:

#### 1. SUPER_ADMIN 🔴

**Descripción:** Administrador principal del sistema

**Permisos:**
- ✅ **Acceso total** al sistema
- ✅ Gestión completa de usuarios (incluidos otros admins)
- ✅ Configuración del sistema
- ✅ Acceso a todos los módulos
- ✅ Visualización de logs y auditoría
- ✅ Gestión de permisos dinámicos

**Cantidad recomendada:** 1-2 usuarios

**Uso típico:**
- Propietario del negocio
- CTO / Responsable de IT

---

#### 2. ADMIN 🟠

**Descripción:** Administrador del sistema

**Permisos:**
- ✅ Gestión de usuarios (excepto SUPER_ADMIN)
- ✅ Configuración de empresa y facturación
- ✅ Gestión de clientes, productos y facturas
- ✅ Acceso a reportes y estadísticas
- ✅ Configuración de notificaciones
- ❌ No puede modificar otros ADMIN
- ❌ No puede acceder a logs del sistema

**Cantidad recomendada:** 2-5 usuarios

**Uso típico:**
- Gerente general
- Contador
- Jefe de ventas

---

#### 3. USER 🟡

**Descripción:** Usuario estándar del sistema

**Permisos:**
- ✅ Gestión de clientes
- ✅ Gestión de productos
- ✅ Creación y edición de facturas
- ✅ Visualización de reportes propios
- ✅ Configuración de perfil personal
- ❌ No acceso a configuración del sistema
- ❌ No acceso a gestión de usuarios
- ❌ No acceso a reportes completos

**Cantidad:** Ilimitada

**Uso típico:**
- Vendedores
- Personal administrativo
- Asistentes

---

#### 4. VENDEDOR 🟢

**Descripción:** Usuario enfocado en ventas

**Permisos:**
- ✅ Gestión de clientes (crear, editar)
- ✅ Visualización de productos
- ✅ Creación de facturas
- ✅ Consulta de sus propias ventas
- ❌ No gestión de productos (solo lectura)
- ❌ No acceso a reportes generales
- ❌ No configuración del sistema

**Cantidad:** Ilimitada

**Uso típico:**
- Vendedores de campo
- Vendedores de tienda
- Agentes comerciales

---

### Comparativa de Roles

| Funcionalidad | SUPER_ADMIN | ADMIN | USER | VENDEDOR |
|---------------|:-----------:|:-----:|:----:|:--------:|
| Gestionar usuarios | ✅ | ✅ | ❌ | ❌ |
| Configuración sistema | ✅ | ✅ | ❌ | ❌ |
| Gestionar clientes | ✅ | ✅ | ✅ | ✅ |
| Gestionar productos | ✅ | ✅ | ✅ | 👁️ Ver |
| Crear facturas | ✅ | ✅ | ✅ | ✅ |
| Reportes completos | ✅ | ✅ | 👁️ Propios | 👁️ Propios |
| Logs del sistema | ✅ | ❌ | ❌ | ❌ |
| Eliminar datos | ✅ | ✅ | ⚠️ Limitado | ❌ |

**Leyenda:**
- ✅ Acceso completo
- 👁️ Solo lectura / Vista limitada
- ⚠️ Con restricciones
- ❌ Sin acceso

---

## 🚀 Acceso al Módulo de Usuarios

### Opción 1: Desde el Menú Principal

1. En la barra lateral izquierda, localice **"Usuarios"**
2. Haga clic en el ícono 👥
3. Será redirigido al listado de usuarios

```
┌─────────────────────────────────┐
│  MENÚ PRINCIPAL                 │
├─────────────────────────────────┤
│  🏠 Inicio                      │
│  👥 Usuarios ← AQUÍ            │
│  👥 Clientes                    │
│  📦 Productos                   │
│  📄 Facturas                    │
│  📊 Reportes                    │
│  ⚙️ Configuración              │
└─────────────────────────────────┘
```

### Opción 2: URL Directa

```
https://tu-dominio.com/usuarios
```

### ⚠️ Mensaje de "Acceso Denegado"

Si al intentar acceder ve este mensaje:

```
❌ ACCESO DENEGADO
No tienes permisos para acceder a este módulo.
Contacta al administrador del sistema.
```

**Causas:**
- No tienes rol ADMIN o SUPER_ADMIN
- Tu cuenta está desactivada
- Tu sesión expiró

**Solución:**
1. Verifica que tienes el rol correcto
2. Contacta al super administrador
3. Cierra sesión y vuelve a entrar

---

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

Accede en: http://localhost:8080/auth/login

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

## 🔄 Activar/Desactivar Usuarios

### Toggle de Estado

**¿Qué hace?**
- Alterna entre Activo ↔ Inactivo
- Sin eliminar el usuario
- Reversible en cualquier momento

**Acceso:**
- Botón **🔄 Toggle** en el listado
- O desde formulario de edición

### Desactivar Usuario

**Escenario:** Empleado de vacaciones o permiso temporal

**Paso 1: Clic en toggle**
```
│ Juan Pérez │juan@empresa.com│USER│🟢 Activo│[🔄]│
                                          ↑
                                        CLIC
```

**Paso 2: Confirmación**
```
┌──────────────────────────────────────────────┐
│  ⚠️ Desactivar Usuario                      │
├──────────────────────────────────────────────┤
│                                              │
│  ¿Desactivar a Juan Pérez?                  │
│                                              │
│  El usuario no podrá iniciar sesión hasta   │
│  que sea reactivado.                         │
│                                              │
│  Sus datos se conservarán.                   │
│                                              │
│  [Cancelar]        [Sí, desactivar]         │
└──────────────────────────────────────────────┘
```

**Paso 3: Usuario desactivado**
```
✅ Usuario desactivado
   Juan Pérez no podrá acceder al sistema.
```

**Nuevo estado en listado:**
```
│ Juan Pérez │juan@empresa.com│USER│🔴 Inactivo│[🔄]│
```

### Reactivar Usuario

**Proceso idéntico al desactivar:**

1. Clic en toggle del usuario inactivo
2. Confirmar reactivación
3. Usuario vuelve a estado activo

```
✅ Usuario reactivado
   Juan Pérez puede acceder nuevamente al sistema.
```

### Efectos de Desactivación

**Usuario desactivado NO puede:**
- ❌ Iniciar sesión
- ❌ Recibir notificaciones
- ❌ Aparecer en asignaciones

**Se CONSERVA:**
- ✅ Todos sus datos personales
- ✅ Historial de facturas creadas
- ✅ Clientes asignados
- ✅ Registros de auditoría

### Desactivación Masiva

**Para desactivar múltiples usuarios:**

1. **Opción A:** Usar filtros
   ```
   Estado: [Inactivo ▼]
   ```
   Muestra solo inactivos

2. **Opción B:** Checkbox (si está implementado)
   ```
   [✓] Juan Pérez
   [✓] María García  
   [ ] Pedro López
   
   [Desactivar seleccionados]
   ```

---

## 🗑️ Eliminar Usuarios

### ⚠️ Advertencia Importante

**La eliminación es PERMANENTE e IRREVERSIBLE**

```
╔════════════════════════════════════════════╗
║  ⚠️ PRECAUCIÓN: ELIMINACIÓN PERMANENTE    ║
╠════════════════════════════════════════════╣
║                                            ║
║  • Los datos del usuario se borrarán       ║
║  • No se puede deshacer                    ║
║  • Afecta registros relacionados           ║
║                                            ║
║  💡 Recomendación:                         ║
║     Mejor DESACTIVAR en lugar de eliminar  ║
║                                            ║
╚════════════════════════════════════════════╝
```

### ¿Cuándo Eliminar?

**✅ Eliminar SI:**
- Usuario creado por error
- Cuenta de prueba
- Usuario duplicado
- Empleado que nunca usó el sistema

**❌ NO Eliminar SI:**
- Usuario tiene facturas creadas
- Usuario tiene clientes asignados
- Solo necesita bloquear acceso → **DESACTIVAR**
- Empleado renunció pero tiene historial → **DESACTIVAR**

### Procedimiento de Eliminación

**Paso 1: Clic en eliminar**
```
│ Pedro López│pedro@empresa.com│VEND.│🔴 Inactivo│[🗑️]│
                                                  ↑
                                                AQUÍ
```

**Paso 2: Primera confirmación**
```
┌──────────────────────────────────────────────┐
│  🗑️ Eliminar Usuario                        │
├──────────────────────────────────────────────┤
│                                              │
│  ¿Eliminar a Pedro López?                    │
│                                              │
│  ⚠️ ESTA ACCIÓN NO SE PUEDE DESHACER        │
│                                              │
│  Usuario: pedro@empresa.com                  │
│  Rol: VENDEDOR                               │
│  Estado: Inactivo                            │
│                                              │
│  Se eliminarán:                              │
│  • Datos personales                          │
│  • Configuración de cuenta                   │
│  • Preferencias                              │
│                                              │
│  Se CONSERVARÁN (con referencia):            │
│  • Facturas creadas (autor: [eliminado])    │
│  • Clientes asignados (sin asignar)         │
│                                              │
│  [Cancelar]          [Continuar]             │
└──────────────────────────────────────────────┘
```

**Paso 3: Segunda confirmación (seguridad)**
```
┌──────────────────────────────────────────────┐
│  ⚠️ CONFIRMACIÓN FINAL                      │
├──────────────────────────────────────────────┤
│                                              │
│  Para confirmar la eliminación, escriba:     │
│                                              │
│  ELIMINAR                                    │
│                                              │
│  [____________________]                      │
│                                              │
│  [Cancelar]          [Eliminar Usuario]      │
│                                (deshabilitado hasta escribir)│
└──────────────────────────────────────────────┘
```

**Paso 4: Usuario eliminado**
```
✅ Usuario eliminado exitosamente
   
   Pedro López ha sido eliminado del sistema.
   
   ℹ️ Los registros relacionados se han actualizado
      para mantener la integridad de los datos.
```

### Restricciones de Eliminación

#### 1. No puedes eliminarte a ti mismo

```
❌ Error: Auto-eliminación no permitida
   
   No puedes eliminar tu propia cuenta.
   
   Solicita a otro administrador que lo haga.
```

**Solución:** Otro admin debe eliminarte

---

#### 2. Último SUPER_ADMIN

```
❌ Error: No se puede eliminar
   
   Este es el único SUPER_ADMIN del sistema.
   
   Debe haber al menos un SUPER_ADMIN activo.
```

**Solución:** 
1. Promover a otro usuario a SUPER_ADMIN
2. Luego eliminar este usuario

---

#### 3. Usuario con datos críticos

```
⚠️ Advertencia: Usuario con registros
   
   Este usuario tiene:
   • 45 facturas creadas
   • 12 clientes asignados
   • 8 productos modificados
   
   ¿Seguro que desea eliminar?
   
   💡 Recomendación: DESACTIVAR en su lugar
   
   [Cancelar] [Desactivar] [Eliminar de todos modos]
```

---

### Impacto de Eliminación

**Datos eliminados:**
```
❌ Datos personales (nombre, email, teléfono)
❌ Credenciales de acceso
❌ Preferencias de notificaciones
❌ Avatar/foto de perfil
❌ Configuración personal
```

**Datos que se preservan (con referencia nula):**
```
✅ Facturas creadas → autor: [Usuario eliminado]
✅ Clientes asignados → sin asignar
✅ Logs del sistema → ID de usuario + [eliminado]
✅ Auditoría → Se mantiene para historial
```

**Ejemplo de factura después de eliminar usuario:**
```
Factura F001-00125
Cliente: ABC Company
Total: S/ 1,250.00
Creado por: [Usuario eliminado] (ID: 5)
Fecha: 15/01/2025
```

### Recuperación de Usuario Eliminado

**⚠️ NO ES POSIBLE**

Una vez eliminado, el usuario **no puede recuperarse**.

**Alternativas:**
1. **Crear nuevo usuario** con los mismos datos
   - Tendrá un ID diferente
   - No tendrá el historial anterior
   
2. **Restaurar desde backup** (si existe)
   - Requiere intervención técnica
   - Solo si hay backup reciente

**💡 Por eso recomendamos DESACTIVAR en lugar de eliminar**

---

## 🔧 Solución de Problemas

### Problema: No puedo crear usuarios

**Síntoma:**
```
❌ Acceso denegado
   No tienes permisos para esta acción
```

**Causas posibles:**
- No tienes rol ADMIN
- Tu sesión expiró
- Tu cuenta fue desactivada

**Solución:**
1. Verifica tu rol (debe ser ADMIN o SUPER_ADMIN)
2. Cierra sesión y vuelve a entrar
3. Contacta al super administrador

---

### Problema: Email "ya registrado"

**Síntoma:**
```
❌ El email ya está registrado
   El email juan@empresa.com ya pertenece a otro usuario
```

**Causas:**
- El email ya existe en el sistema
- Usuario fue eliminado pero email sigue en uso (raro)

**Solución:**

**Opción 1:** Verificar en el listado
```
1. Ir a listado de usuarios
2. Buscar: juan@empresa.com
3. Si aparece:
   a) Editar ese usuario en lugar de crear nuevo
   b) O usar otro email
```

**Opción 2:** Agregar variación
```
Original: juan@empresa.com
Variaciones:
  - juan.perez@empresa.com
  - jperez@empresa.com  
  - juan.p@empresa.com
```

---

### Problema: No se envían emails

**Síntoma:**
```
✅ Usuario creado exitosamente
⚠️ No se pudo enviar el email de notificación
```

**Causas:**
1. Configuración SMTP incorrecta
2. Email del usuario inválido
3. Servidor de correo caído

**Verificar:**

**1. Configuración SMTP (Admin)**
```
application.properties:
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=tu-email@gmail.com
spring.mail.password=tu-app-password ← Verificar
```

**2. Email válido**
```
✅ juan@empresa.com
❌ juan@empresa (sin dominio)
```

**3. Probar envío**
```
Configuración > Notificaciones > [Probar Email]
```

**Solución temporal:**
- Desmarcar "Enviar credenciales por email"
- Comunicar credenciales manualmente

---

### Problema: Usuario no puede iniciar sesión

**Síntoma:**
```
Usuario: juan@empresa.com
Contraseña: *******
[Iniciar Sesión]

❌ Credenciales inválidas
```

**Causas y soluciones:**

#### Causa 1: Usuario inactivo
```
Estado del usuario: 🔴 Inactivo

Solución:
1. Editar usuario
2. Marcar [✓] Usuario activo
3. Guardar
```

#### Causa 2: Contraseña incorrecta
```
Solución:
1. Resetear contraseña (botón 🔐)
2. Enviar nuevas credenciales
3. Usuario intenta con la nueva contraseña
```

#### Causa 3: Email incorrecto
```
Email registrado: juan.perez@empresa.com
Usuario intenta: juan@empresa.com ← Diferente

Solución:
- Verificar email exacto en el listado
- Usar el email correcto para login
```

#### Causa 4: Mayúsculas/minúsculas
```
Nota: El sistema NO distingue mayúsculas en email

Juan@Empresa.com = juan@empresa.com ✅
```

---

### Problema: Error al eliminar usuario

**Síntoma:**
```
❌ Error al eliminar usuario
   No se pudo completar la operación
```

**Causas:**

#### 1. Intentas eliminarte a ti mismo
```
❌ No puedes eliminar tu propia cuenta

Solución:
- Solicita a otro admin que te elimine
```

#### 2. Último SUPER_ADMIN
```
❌ Debe haber al menos un SUPER_ADMIN

Solución:
1. Promover a otro usuario a SUPER_ADMIN
2. Luego eliminar este usuario
```

#### 3. Usuario con relaciones críticas
```
⚠️ Usuario tiene facturas asociadas

Solución recomendada:
- DESACTIVAR en lugar de eliminar
- Se preserva el historial
```

---

### Problema: Contraseña generada muy compleja

**Síntoma:**
```
Contraseña generada: xK9m2#pL$wQ&

Usuario dice: "Es muy difícil de recordar"
```

**Soluciones:**

**Opción 1:** Generar otra contraseña
```
1. Clic en 🎲 Generar varias veces
2. Hasta obtener una más simple
```

**Opción 2:** Crear contraseña manual
```
En lugar de generar:
1. Escribir contraseña personalizada
2. Ejemplo: Empresa2025
3. Confirmar la misma
```

**Opción 3:** Usuario cambia después
```
1. Crear con contraseña generada
2. Usuario inicia sesión
3. Usuario va a Perfil > Cambiar Contraseña
4. Pone una contraseña que recuerde
```

---

### Problema: Muchos usuarios inactivos

**Síntoma:**
```
Estadísticas:
Total: 50
Activos: 20
Inactivos: 30 ← Muchos
```

**Recomendaciones:**

**Limpieza periódica:**
```
1. Filtrar por: Estado = Inactivo
2. Revisar usuarios inactivos
3. Eliminar los que no se necesitan
4. Mantener solo los relevantes
```

**Criterios de eliminación:**
```
✅ Eliminar:
   - Usuarios de prueba
   - Duplicados
   - Empleados que nunca usaron el sistema
   - Cuentas sin facturas ni clientes

❌ NO Eliminar:
   - Empleados con historial
   - Usuarios con facturas creadas
   - Cuentas con datos importantes
```

**Automatización futura:**
```
💡 Configurar:
   - Desactivar automáticamente usuarios sin login en 6 meses
   - Alertas de usuarios inactivos
   - Revisión trimestral
```

---

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

## 📚 Casos de Uso Prácticos

### Caso 1: Nuevo empleado ingresa

**Objetivo:** Crear cuenta para vendedor nuevo

**Pasos:**
1. Usuarios → ➕ Nuevo Usuario
2. Datos:
   ```
   Nombre: Carlos Mendoza
   Email: carlos.mendoza@empresa.com
   Teléfono: 987654321
   Rol: VENDEDOR
   ```
3. Generar contraseña (clic en 🎲)
4. Marcar: [✓] Enviar credenciales por email
5. Guardar
6. Verificar que recibió el email
7. Ayudarlo en su primer login

---

### Caso 2: Empleado de vacaciones

**Objetivo:** Bloquear acceso temporalmente (1 mes)

**Pasos:**
1. Buscar usuario: "Juan Pérez"
2. Clic en 🔄 Toggle
3. Usuario queda 🔴 Inactivo
4. Guardar fecha de reactivación: 01/02/2026
5. En esa fecha: 🔄 Toggle nuevamente
6. Usuario vuelve a 🟢 Activo

**No hacer:**
- ❌ Eliminar usuario
- ❌ Cambiar contraseña

---

### Caso 3: Promover empleado

**Objetivo:** Cambiar vendedor a admin

**Pasos:**
1. Usuarios → Buscar: "María García"
2. Clic en ✏️ Editar
3. Cambiar rol: VENDEDOR → ADMIN
4. Guardar
5. Notificar al usuario:
   ```
   Hola María,
   
   Has sido promovida a Administradora.
   Ahora tienes acceso a:
   - Gestión de usuarios
   - Configuración del sistema
   - Reportes completos
   
   Saludos,
   Gerencia
   ```

---

### Caso 4: Empleado olvidó contraseña

**Objetivo:** Resetear y enviar nueva contraseña

**Pasos:**
1. Usuarios → Buscar empleado
2. Clic en 🔐 Resetear
3. Confirmar
4. Sistema genera: `mK8#xP2q`
5. Se envía email automático
6. Informar al empleado:
   ```
   Hola Pedro,
   
   Se ha reseteado tu contraseña.
   Revisa tu email para las nuevas credenciales.
   
   Te recomiendo cambiarla en tu primer login:
   Perfil > Seguridad > Cambiar Contraseña
   ```

---

### Caso 5: Auditoría trimestral

**Objetivo:** Limpiar usuarios inactivos

**Pasos:**
1. Filtrar: Estado = Inactivo
2. Revisar lista de inactivos
3. Verificar última actividad
4. Criterios:
   ```
   ✅ Eliminar si:
      - Sin login en 6+ meses
      - Sin facturas creadas
      - Cuenta de prueba
      
   ❌ Mantener si:
      - Tiene historial
      - Empleado temporal
      - Puede volver
   ```
5. Eliminar usuarios innecesarios
6. Documentar en reporte
7. Exportar listado final

---

## 🔗 Enlaces Relacionados

- [Manual de Configuración del Sistema](MANUAL_CONFIGURACION_SISTEMA.md)
- [Manual de Permisos y Roles](MANUAL_USUARIO_PERMISOS.md)
- [Manual de Notificaciones](MANUAL_NOTIFICACIONES.md)
- [Guía de Logging](../guias/GUIA_LOGGING.md)

---

## 📝 Registro de Cambios

| Versión | Fecha | Cambios |
|---------|-------|---------|
| 1.0 | 04/01/2026 | Versión inicial del manual |

---

**Documento actualizado:** 4 de enero de 2026  
**Versión del sistema:** 4.0 - Sprint 4  
**Autor:** Equipo de Desarrollo ERP Spring Manager  

---

*Este manual está sujeto a cambios conforme el sistema evoluciona. Consulte siempre la versión más reciente en la documentación oficial.*
