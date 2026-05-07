## 📋 CREDENCIALES NECESARIAS

### 1. Phone Number ID ⭐
```
Ubicación: Meta for Developers → WhatsApp → API Setup
Formato: Número largo (ej: 123456789012345)
Uso: Identificar tu número de WhatsApp en la API

🔑 PHONE_NUMBER_ID = [COMPLETAR AQUÍ]
```

**Cómo obtenerlo:**
1. Ve a: https://developers.facebook.com
2. Selecciona tu app
3. WhatsApp → API Setup
4. Copia el "Phone number ID"

---

### 2. Access Token (Temporal) ⏱️
```
Ubicación: Meta for Developers → WhatsApp → API Setup
Duración: 24 horas
Uso: Desarrollo y pruebas iniciales

🔑 TEMPORARY_ACCESS_TOKEN = [COMPLETAR AQUÍ]
```

**Cómo obtenerlo:**
1. En la misma página de API Setup
2. Busca "Temporary access token"
3. Click en "Generate token"
4. **Expira en 24h** - regenerar cuando sea necesario

---

### 3. Access Token (Permanente) 🔒
```
Ubicación: Graph API Explorer (después de aprobación)
Duración: 60 días (renovable automáticamente)
Uso: Producción

🔑 PERMANENT_ACCESS_TOKEN = [OBTENER DESPUÉS DE APROBACIÓN]
```

**Cómo obtenerlo (DESPUÉS de aprobación):**
1. Ve a: https://developers.facebook.com/tools/explorer
2. Selecciona tu app
3. Permisos: `whatsapp_business_messaging`, `whatsapp_business_management`
4. Generate Access Token
5. Intercambiar por token permanente

---

### 4. Webhook Verify Token 🎯
```
Ubicación: Lo defines TÚ
Uso: Verificar que los webhooks vienen de Meta
Seguridad: Debe ser un string aleatorio seguro

🔑 WEBHOOK_VERIFY_TOKEN = [CREAR UN TOKEN ALEATORIO]
```

**Ejemplo de generación:**
```bash
# PowerShell
$token = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes([Guid]::NewGuid().ToString()))
Write-Host $token

# Resultado ejemplo: "ZjM0YTIxNGYtODI5ZC00NGE0LTk3YjItOGY4YTQ1YjA2YzNj"
```

---

### 5. App ID y App Secret 🔐
```
Ubicación: Meta for Developers → App Settings → Basic
Uso: Autenticación avanzada

🔑 APP_ID = [COMPLETAR AQUÍ]
🔑 APP_SECRET = [COMPLETAR AQUÍ - NO COMPARTIR]
```

---

### 6. Business Account ID 🏢
```
Ubicación: Meta Business Suite
Uso: Gestión de cuenta empresarial

🔑 BUSINESS_ACCOUNT_ID = [COMPLETAR AQUÍ]
```

---

