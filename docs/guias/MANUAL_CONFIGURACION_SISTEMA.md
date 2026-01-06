# 📘 Manual de Usuario - Configuración del Sistema

**Versión:** 1.0  
**Fecha:** 4 de enero de 2026  
**Audiencia:** Administradores del sistema  
**Nivel de acceso requerido:** ROL_ADMIN

---

## 📑 Tabla de Contenidos

1. [Introducción](#introducción)
2. [Acceso a Configuración](#acceso-a-configuración)
3. [Configuración de Empresa](#configuración-de-empresa)
4. [Configuración de Facturación](#configuración-de-facturación)
5. [Configuración de Notificaciones](#configuración-de-notificaciones)
6. [Parámetros del Sistema](#parámetros-del-sistema)
7. [Solución de Problemas](#solución-de-problemas)
8. [Preguntas Frecuentes](#preguntas-frecuentes)

---

## 📖 Introducción

El módulo de **Configuración del Sistema** permite a los administradores personalizar y gestionar todos los aspectos fundamentales de la aplicación, incluyendo:

- Datos de la empresa
- Configuración de facturación
- Sistema de notificaciones por email
- Parámetros generales del sistema

### ⚠️ Requisitos Previos

- **Rol requerido:** ADMIN
- **Permisos necesarios:** 
  - `CONFIGURACION_EMPRESA_VER`
  - `CONFIGURACION_EMPRESA_EDITAR`
  - `CONFIGURACION_FACTURACION_VER`
  - `CONFIGURACION_FACTURACION_EDITAR`

---

## 🔐 Acceso a Configuración

### Paso 1: Iniciar Sesión

1. Acceda a la aplicación con credenciales de **Administrador**
2. Asegúrese de tener el rol `ADMIN` asignado

### Paso 2: Navegar a Configuración

**Opción 1: Desde el Menú Principal**
1. En la barra lateral izquierda, localice el menú **"Configuración"**
2. Haga clic en el ícono de engranaje ⚙️
3. Será redirigido a `/configuracion`

**Opción 2: URL Directa**
```
https://tu-dominio.com/configuracion
```

### Estructura de la Página

La página de configuración está organizada en **pestañas (tabs)**:

```
┌─────────────────────────────────────────────────────────┐
│  Empresa  │  Facturación  │  Notificaciones  │ ...     │
├─────────────────────────────────────────────────────────┤
│                                                         │
│           [Contenido de la pestaña activa]             │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

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

## 🧾 Configuración de Facturación

### Información General

Configure los parámetros de facturación que se aplicarán automáticamente a todas las facturas generadas en el sistema.

### Campos Disponibles

#### 1. Numeración de Facturas

| Campo | Descripción | Obligatorio | Ejemplo |
|-------|-------------|-------------|---------|
| **Serie de Factura** | Prefijo de las facturas | ✅ Sí | "F001" |
| **Número Actual** | Último número usado | ✅ Sí | "125" |
| **Prefijo** | Texto antes del número | ❌ No | "FAC-" |
| **Sufijo** | Texto después del número | ❌ No | "-2026" |

**Ejemplo de numeración completa:**
```
Prefijo + Serie + Número + Sufijo
"FAC-" + "F001" + "00125" + "-2026"
= FAC-F001-00125-2026
```

#### 2. Impuestos

| Campo | Descripción | Obligatorio | Ejemplo |
|-------|-------------|-------------|---------|
| **IGV/IVA (%)** | Porcentaje de impuesto | ✅ Sí | "18.00" (Perú) |
| **Incluir IGV** | Mostrar IGV en facturas | ✅ Sí | Sí / No |

**Cálculo automático:**
```
Subtotal:  S/ 1,000.00
IGV (18%):  S/   180.00
────────────────────────
Total:     S/ 1,180.00
```

#### 3. Términos y Condiciones

| Campo | Descripción | Obligatorio | Ejemplo |
|-------|-------------|-------------|---------|
| **Términos** | Condiciones de venta | ❌ No | "Pago a 30 días..." |
| **Pie de Página** | Texto al final | ❌ No | "Gracias por su compra" |

#### 4. Información de Pago

| Campo | Descripción | Obligatorio | Ejemplo |
|-------|-------------|-------------|---------|
| **Cuenta Bancaria** | Número de cuenta | ❌ No | "0011-0123-456789" |
| **Banco** | Nombre del banco | ❌ No | "Banco de Crédito BCP" |
| **CCI** | Código interbancario | ❌ No | "002-011-001234567890-12" |

### Procedimiento: Configurar Facturación

#### Paso 1: Acceder a la Pestaña
1. Haga clic en la pestaña **"Facturación"**
2. Se mostrarán los parámetros actuales

#### Paso 2: Configurar Numeración

1. **Serie de Factura:**
   - Ingrese el código de serie (ej: "F001", "B001")
   - Formato común: Letra + 3 dígitos

2. **Número Actual:**
   - Ingrese el último número de factura emitida
   - El sistema incrementará automáticamente

3. **Prefijo y Sufijo (opcional):**
   - Agregue texto personalizado
   - Se añadirá antes/después del número

**Vista Previa:**
```
┌──────────────────────────────────────┐
│  Vista Previa del Número:           │
│  FAC-F001-00126-2026                │
└──────────────────────────────────────┘
```

#### Paso 3: Configurar Impuestos

1. **IGV/IVA:**
   - Ingrese el porcentaje (ej: 18.00 para Perú)
   - Use punto decimal (no coma)
   - Rango válido: 0.00 - 100.00

2. **Incluir IGV:**
   - ✅ Marque para mostrar en facturas
   - ❌ Desmarque para ocultar

#### Paso 4: Términos y Condiciones

**Recomendaciones:**
- Sea claro y conciso
- Incluya políticas de pago
- Especifique garantías o devoluciones
- Mencione penalidades por mora (si aplica)

**Ejemplo de términos:**
```
- Pago a 30 días desde la fecha de emisión
- Intereses moratorios: 2% mensual
- No se aceptan devoluciones después de 7 días
- Productos sujetos a disponibilidad
```

#### Paso 5: Información Bancaria

Complete **solo si desea** que aparezca en las facturas:

1. Número de cuenta bancaria
2. Nombre del banco
3. CCI (Código de Cuenta Interbancario)

#### Paso 6: Guardar Configuración

1. Revise todos los parámetros
2. Haga clic en **"Guardar Configuración"**
3. Espere el mensaje de confirmación

### Mensajes del Sistema

#### ✅ Mensajes de Éxito
- **"Configuración de facturación actualizada exitosamente"**
- **"Configuración de facturación creada exitosamente"**

#### ❌ Mensajes de Error
- **"El porcentaje de IGV debe estar entre 0 y 100"**
- **"La serie de factura es obligatoria"**
- **"El número actual debe ser mayor a 0"**

### Ejemplo de Factura Generada

Con esta configuración:
```yaml
Serie: F001
Número Actual: 125
Prefijo: FAC-
Sufijo: -2026
IGV: 18%
```

Se generará:
```
┌─────────────────────────────────────────────────┐
│  FACTURA: FAC-F001-00126-2026                  │
├─────────────────────────────────────────────────┤
│  Comercial ABC S.A.C.                          │
│  RUC: 20123456789                              │
│                                                 │
│  Subtotal:        S/ 1,000.00                  │
│  IGV (18%):       S/   180.00                  │
│  ─────────────────────────────                  │
│  Total:           S/ 1,180.00                  │
│                                                 │
│  Pago a 30 días desde la fecha de emisión     │
│                                                 │
│  Banco: Banco de Crédito BCP                   │
│  Cuenta: 0011-0123-456789                      │
└─────────────────────────────────────────────────┘
```

---

## 📧 Configuración de Notificaciones

### Información General

Configure el servidor SMTP para enviar notificaciones por email automáticamente (facturas, recordatorios, alertas).

### Campos de Configuración

#### 1. Servidor SMTP

| Campo | Descripción | Ejemplo |
|-------|-------------|---------|
| **Host SMTP** | Dirección del servidor | "smtp.gmail.com" |
| **Puerto** | Puerto de conexión | "587" (TLS) o "465" (SSL) |
| **Usuario** | Email de envío | "noreply@empresa.com" |
| **Contraseña** | Contraseña del email | "********" |
| **Protocolo** | TLS o SSL | "TLS" (recomendado) |

#### 2. Proveedores Comunes

**Gmail:**
```
Host: smtp.gmail.com
Puerto: 587 (TLS) o 465 (SSL)
Usuario: tu-email@gmail.com
Contraseña: Contraseña de aplicación*
```

**Outlook/Hotmail:**
```
Host: smtp-mail.outlook.com
Puerto: 587 (TLS)
Usuario: tu-email@outlook.com
Contraseña: Contraseña de tu cuenta
```

**Office 365:**
```
Host: smtp.office365.com
Puerto: 587 (TLS)
Usuario: tu-email@tuempresa.com
Contraseña: Contraseña de tu cuenta
```

> **⚠️ Nota para Gmail:** Debe generar una "Contraseña de aplicación" desde la configuración de seguridad de Google.

### Procedimiento: Configurar Email

#### Paso 1: Generar Contraseña de Aplicación (Gmail)

**Si usa Gmail:**

1. Vaya a [myaccount.google.com](https://myaccount.google.com)
2. Navegue a **Seguridad** > **Verificación en 2 pasos**
3. Active la verificación en 2 pasos
4. Vaya a **Contraseñas de aplicaciones**
5. Seleccione "Correo" y "Otro" (escriba "ERP Sistema")
6. Google generará una contraseña de 16 caracteres
7. Copie esta contraseña (sin espacios)

#### Paso 2: Configurar en el Sistema

1. Acceda a la pestaña **"Notificaciones"**
2. Complete los campos:
   ```
   Host SMTP:     smtp.gmail.com
   Puerto:        587
   Usuario:       noreply@tuempresa.com
   Contraseña:    [Pegar contraseña de aplicación]
   Protocolo:     TLS
   ```

3. **Marque** "Habilitar notificaciones por email"

#### Paso 3: Probar Configuración

1. Ingrese un email de prueba en el campo **"Email de Prueba"**
2. Haga clic en el botón **"Probar Conexión"**
3. Espere la respuesta del sistema

**Resultado Exitoso:**
```
✅ Email de prueba enviado correctamente a: admin@empresa.com
Revise su bandeja de entrada.
```

**Resultado con Error:**
```
❌ Error al enviar email de prueba:
Autenticación fallida. Verifique usuario y contraseña.
```

#### Paso 4: Guardar Configuración

1. Si la prueba fue exitosa, haga clic en **"Guardar Configuración"**
2. Las notificaciones automáticas quedarán activadas

### Tipos de Notificaciones Automáticas

Una vez configurado, el sistema enviará automáticamente:

| Tipo | Descripción | Frecuencia |
|------|-------------|------------|
| **Facturas** | Envío de PDF de facturas | Manual o automático |
| **Recordatorios de Pago** | Alertas de facturas próximas a vencer | Diaria (scheduler) |
| **Facturas Vencidas** | Avisos de pagos atrasados | Diaria (scheduler) |
| **Nuevos Usuarios** | Credenciales de acceso | Al crear usuario |
| **Cambio de Contraseña** | Confirmación de cambio | Al cambiar password |

### Solución de Problemas - Email

#### Error: "Autenticación Fallida"

**Causas comunes:**
- Usuario o contraseña incorrectos
- No habilitó "Contraseña de aplicación" en Gmail
- Verificación en 2 pasos desactivada (Gmail)

**Solución:**
1. Verifique usuario y contraseña
2. Para Gmail, use contraseña de aplicación (no su contraseña normal)
3. Active verificación en 2 pasos en Gmail

#### Error: "Conexión Rechazada"

**Causas comunes:**
- Puerto incorrecto
- Firewall bloqueando la conexión
- Host SMTP incorrecto

**Solución:**
1. Verifique el puerto (587 para TLS, 465 para SSL)
2. Revise configuración de firewall
3. Confirme el host del proveedor

#### Error: "Timeout"

**Causas comunes:**
- Problema de red
- Servidor SMTP no responde

**Solución:**
1. Verifique conexión a internet
2. Intente nuevamente en unos minutos
3. Contacte a su proveedor de email

---

## ⚙️ Parámetros del Sistema

### Información General

Los parámetros del sistema son configuraciones globales que afectan el comportamiento de la aplicación.

### Categorías de Parámetros

#### 1. General
- Nombre del sistema
- Versión
- Modo de mantenimiento
- Idioma por defecto

#### 2. Seguridad
- Tiempo de sesión (minutos)
- Intentos de login fallidos permitidos
- Complejidad de contraseña
- Duración de tokens

#### 3. Notificaciones
- Habilitar/deshabilitar notificaciones
- Días antes de vencimiento para recordatorios
- Horario de envío de recordatorios

#### 4. Facturación
- Plazo de pago por defecto (días)
- Moneda por defecto
- Decimales en montos

### Procedimiento: Editar Parámetros

1. Acceda a la sección **"Parámetros"**
2. Localice el parámetro que desea editar
3. Haga clic en el botón **"Editar"** (ícono de lápiz)
4. Modifique el valor
5. Haga clic en **"Guardar"**

> **⚠️ Precaución:** Modificar parámetros del sistema puede afectar el funcionamiento global. Consulte con el equipo técnico si no está seguro.

---

## 🔧 Solución de Problemas

### Problema: No puedo acceder a Configuración

**Causa:** Falta de permisos

**Solución:**
1. Verifique que tiene el rol ADMIN
2. Contacte a un administrador para que le asigne permisos
3. Cierre sesión y vuelva a iniciar

### Problema: Los cambios no se guardan

**Causa:** Errores de validación

**Solución:**
1. Revise mensajes de error en pantalla (en rojo)
2. Complete todos los campos obligatorios (*)
3. Verifique formatos (email, teléfono, números)
4. Intente nuevamente

### Problema: Logo no se muestra

**Causas posibles:**
- Archivo muy grande (> 2 MB)
- Formato no compatible
- Error al subir

**Solución:**
1. Reduzca el tamaño de la imagen
2. Convierta a PNG o JPG
3. Asegúrese de hacer clic en "Subir Logo"
4. Actualice la página (F5)

### Problema: Emails no se envían

**Causa:** Configuración SMTP incorrecta

**Solución:**
1. Revise la configuración de notificaciones
2. Ejecute una prueba de email
3. Verifique credenciales del servidor SMTP
4. Consulte la sección "Solución de Problemas - Email"

---

## ❓ Preguntas Frecuentes

### ¿Puedo tener múltiples empresas configuradas?

No, actualmente el sistema soporta una sola empresa principal. Todos los documentos se generarán con los datos de esta empresa.

### ¿Puedo cambiar la numeración de facturas?

Sí, pero con precaución. Puede editar el "Número Actual" en Configuración de Facturación. El sistema usará este número como base y lo incrementará automáticamente.

**Recomendación:** Solo cambie al iniciar operaciones o al inicio de año fiscal.

### ¿Qué pasa si elimino el logo?

El logo se eliminará de la base de datos y no aparecerá en facturas ni reportes. Puede subir un nuevo logo en cualquier momento.

### ¿Los cambios afectan facturas ya emitidas?

**No.** Los cambios en configuración solo afectan a **nuevos documentos**. Las facturas ya emitidas mantienen los datos con los que fueron generadas.

### ¿Puedo usar mi email personal para notificaciones?

Sí, pero se recomienda usar un email corporativo o crear uno específico como `noreply@tuempresa.com` para mayor profesionalismo.

### ¿Es seguro guardar la contraseña de email?

Sí. La contraseña se guarda encriptada en la base de datos. Sin embargo, por seguridad:
- Use una "Contraseña de aplicación" (Gmail)
- No use su contraseña personal principal
- Limite el acceso a la configuración (solo ADMIN)

### ¿Cómo desactivo las notificaciones automáticas?

1. Vaya a Configuración > Notificaciones
2. Desmarque "Habilitar notificaciones por email"
3. Guarde los cambios

### ¿Puedo programar el horario de envío de recordatorios?

Actualmente, los recordatorios se envían diariamente en horarios fijos configurados en el sistema. Para cambiar estos horarios, contacte al equipo técnico.

---

## 📞 Soporte Técnico

Si encuentra problemas no cubiertos en este manual:

1. **Revise la documentación técnica** en la carpeta `docs/`
2. **Consulte los logs del sistema** para errores detallados
3. **Contacte al equipo de desarrollo** con:
   - Descripción del problema
   - Pasos para reproducirlo
   - Capturas de pantalla (si aplica)
   - Mensajes de error exactos

---

## 📚 Documentación Relacionada

- [Manual de Gestión de Usuarios](MANUAL_USUARIO_PERMISOS.md)
- [Manual de Reportes y Exportación](#) *(próximamente)*
- [Manual de Notificaciones](#) *(próximamente)*
- [Guía de Logging](GUIA_LOGGING.md)
- [Configuración de Email](../configuracion/CONFIGURACION_EMAIL.md)

---

**Documento actualizado:** 4 de enero de 2026  
**Versión del sistema:** 4.0 - Sprint 4  
**Autor:** Equipo de Desarrollo ERP Spring Manager  

---

*Este manual está sujeto a cambios conforme el sistema evoluciona. Consulte siempre la versión más reciente en la documentación oficial.*
