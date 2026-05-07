## 🏢 Configuración de Empresa

### Información General

Configure los datos básicos de su empresa que aparecerán en facturas, reportes y comunicaciones.

### Campos Disponibles

#### 1. Información Básica

| Campo | Descripción | Obligatorio | Ejemplo |
|-------|-------------|-------------|---------|
| **Nombre de la Empresa** | Razón social o nombre comercial | ✅ Sí | "Comercial ABC S.A.C." |
| **RUC/NIT** | Registro único de contribuyente | ✅ Sí | "20123456789" |
| **Dirección** | Dirección fiscal completa | ✅ Sí | "Av. Principal 123, Lima" |
| **Teléfono** | Número de contacto principal | ✅ Sí | "+51 999 888 777" |
| **Email** | Correo electrónico corporativo | ✅ Sí | "ventas@empresa.com" |

#### 2. Información Complementaria

| Campo | Descripción | Obligatorio | Ejemplo |
|-------|-------------|-------------|---------|
| **Sitio Web** | URL del sitio web corporativo | ❌ No | "www.empresa.com" |
| **Facebook** | Página de Facebook | ❌ No | "@empresaabc" |
| **Instagram** | Perfil de Instagram | ❌ No | "@empresaabc" |
| **Twitter** | Cuenta de Twitter/X | ❌ No | "@empresaabc" |

#### 3. Identidad Visual

| Elemento | Descripción | Formato | Tamaño Recomendado |
|----------|-------------|---------|-------------------|
| **Logo** | Logotipo de la empresa | PNG, JPG | 500x200px |
| **Favicon** | Ícono del navegador | PNG, ICO | 32x32px o 64x64px |

### Procedimiento: Editar Datos de Empresa

#### Paso 1: Acceder a la Pestaña
1. En la página de configuración, haga clic en la pestaña **"Empresa"**
2. Se mostrarán los datos actuales de la empresa

#### Paso 2: Modificar Información
1. Edite los campos que desee actualizar
2. Los campos con asterisco (*) son **obligatorios**
3. Respete los formatos indicados (email, teléfono, RUC)

#### Paso 3: Subir Logo (Opcional)

**Requisitos del Logo:**
- Formato: PNG o JPG
- Tamaño máximo: 2 MB
- Dimensiones recomendadas: 500x200 píxeles
- Fondo: Preferiblemente transparente (PNG)

**Pasos:**
1. Haga clic en el botón **"Seleccionar Logo"**
2. Navegue hasta el archivo en su computadora
3. Seleccione la imagen
4. Haga clic en **"Subir Logo"**
5. El logo se mostrará en la vista previa

**Para eliminar el logo:**
1. Haga clic en el botón **"Eliminar Logo"** (ícono de papelera)
2. Confirme la eliminación

#### Paso 4: Subir Favicon (Opcional)

**Requisitos del Favicon:**
- Formato: PNG o ICO
- Tamaño máximo: 500 KB
- Dimensiones recomendadas: 32x32 o 64x64 píxeles
- Diseño simple y reconocible en tamaño pequeño

**Pasos:**
1. Haga clic en el botón **"Seleccionar Favicon"**
2. Seleccione el archivo de ícono
3. Haga clic en **"Subir Favicon"**
4. El favicon se aplicará automáticamente

#### Paso 5: Guardar Cambios
1. Revise todos los datos ingresados
2. Haga clic en el botón **"Guardar Cambios"**
3. Espere el mensaje de confirmación

### Mensajes del Sistema

#### ✅ Mensajes de Éxito
- **"Empresa actualizada exitosamente"** - Los datos se guardaron correctamente
- **"Logo subido exitosamente"** - El logo se cargó y guardó
- **"Logo eliminado exitosamente"** - El logo se eliminó de la base de datos

#### ❌ Mensajes de Error
- **"Por favor corrige los errores en el formulario"** - Hay campos inválidos
- **"El archivo excede el tamaño máximo permitido"** - Archivo muy grande
- **"Formato de archivo no permitido"** - Tipo de archivo incorrecto
- **"Error al guardar los datos de la empresa"** - Error del servidor

### Validaciones Automáticas

El sistema valida automáticamente:

- ✅ **Email válido:** formato usuario@dominio.com
- ✅ **RUC/NIT:** solo números (longitud según país)
- ✅ **Teléfono:** formato con código de país
- ✅ **URL:** formato válido con http:// o https://
- ✅ **Tamaño de archivos:** máximo 2 MB para logo
- ✅ **Formato de imágenes:** PNG, JPG, ICO

---

