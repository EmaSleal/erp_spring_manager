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

