# 🔐 CREDENCIALES META WHATSAPP - TEMPLATE

**Fecha de obtención:** [FECHA]  
**Cuenta:** Astro Desarrollo Aplicaciones  
**Número WhatsApp:** 15558362711

> ⚠️ **NUNCA COMMITEAR ESTE ARCHIVO CON VALORES REALES**
> Este es solo un template para documentar qué credenciales necesitas

---

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

## 🗂️ ESTRUCTURA DE VARIABLES DE ENTORNO

### Para desarrollo (.env.local)
```env
# WhatsApp Meta Configuration
META_WHATSAPP_PHONE_NUMBER_ID=TU_PHONE_NUMBER_ID
META_WHATSAPP_ACCESS_TOKEN=TU_TEMPORARY_TOKEN
META_WHATSAPP_API_VERSION=v18.0
META_WEBHOOK_VERIFY_TOKEN=tu_token_verificacion_aleatorio

# Meta App Configuration
META_APP_ID=TU_APP_ID
META_APP_SECRET=TU_APP_SECRET
META_BUSINESS_ACCOUNT_ID=TU_BUSINESS_ID
```

### Para application.yml
```yaml
whatsapp:
  meta:
    phone-number-id: ${META_WHATSAPP_PHONE_NUMBER_ID}
    access-token: ${META_WHATSAPP_ACCESS_TOKEN}
    api-version: ${META_WHATSAPP_API_VERSION:v18.0}
    webhook-verify-token: ${META_WEBHOOK_VERIFY_TOKEN}
  app:
    id: ${META_APP_ID}
    secret: ${META_APP_SECRET}
```

---

## 📍 UBICACIONES EN META FOR DEVELOPERS

### API Setup Page
```
URL: https://developers.facebook.com/apps/[TU_APP_ID]/whatsapp-business/wa-dev-console

Aquí encuentras:
✓ Phone Number ID
✓ Temporary Access Token
✓ Test Number
✓ Send Message Test
```

### App Settings
```
URL: https://developers.facebook.com/apps/[TU_APP_ID]/settings/basic

Aquí encuentras:
✓ App ID
✓ App Secret
✓ Display Name
```

### Webhook Configuration
```
URL: https://developers.facebook.com/apps/[TU_APP_ID]/webhooks

Aquí configuras:
✓ Callback URL
✓ Verify Token
✓ Webhook Fields (messages, message_status)
```

---

## ✅ CHECKLIST DE CREDENCIALES

### Para empezar desarrollo HOY:
- [ ] Phone Number ID copiado
- [ ] Temporary Access Token generado (24h)
- [ ] Webhook Verify Token creado (aleatorio)
- [ ] App ID copiado
- [ ] Archivo .env.local creado
- [ ] Variables configuradas en application.yml

### Después de aprobación:
- [ ] Permanent Access Token generado
- [ ] Token renovado en .env
- [ ] Webhook URL configurado (ngrok o staging)
- [ ] Webhook verificado exitosamente
- [ ] Plantillas aprobadas

---

## 🚫 SEGURIDAD - MUY IMPORTANTE

### ❌ NUNCA hacer esto:
```
❌ Commitear .env o .env.local
❌ Compartir Access Token en chat/email
❌ Pegar tokens en screenshots públicos
❌ Hardcodear tokens en código
❌ Subir tokens a GitHub/GitLab
```

### ✅ SIEMPRE hacer esto:
```
✅ Usar variables de entorno
✅ Agregar .env* a .gitignore
✅ Rotar tokens periódicamente
✅ Usar tokens temporales en desarrollo
✅ Guardar tokens en gestor de contraseñas
✅ Limitar permisos de tokens
```

---

## 📋 .gitignore (verificar)

```gitignore
# Environment variables
.env
.env.local
.env.production
.env.development

# Credentials
**/CREDENCIALES_META.md
**/credentials/
**/secrets/

# Spring Boot
application-local.yml
application-dev.yml
```

---

## 🔄 RENOVACIÓN DE TOKENS

### Access Token expira cada 60 días:
```
1. Meta enviará notificación antes de expirar
2. Ir a Graph API Explorer
3. Regenerar token con mismos permisos
4. Actualizar en variables de entorno
5. Reiniciar aplicación
```

### Tokens temporales (24h):
```
Regenerar diariamente durante desarrollo:
1. API Setup → Generate token
2. Copiar nuevo token
3. Actualizar .env.local
4. Reiniciar app
```

---

## 📞 SOPORTE

**Si tienes problemas:**
- Documentación: https://developers.facebook.com/docs/whatsapp
- Foro: https://developers.facebook.com/community
- Centro de ayuda: https://business.facebook.com/business/help

---

## 📝 EJEMPLO DE USO EN CÓDIGO

```java
@Configuration
@ConfigurationProperties(prefix = "whatsapp.meta")
@Data
public class WhatsAppMetaConfig {
    private String phoneNumberId;
    private String accessToken;
    private String apiVersion;
    private String webhookVerifyToken;
}

@Service
public class MetaWhatsAppServiceImpl implements WhatsAppService {
    
    @Autowired
    private WhatsAppMetaConfig config;
    
    private String getApiUrl() {
        return String.format("https://graph.facebook.com/%s/%s/messages",
            config.getApiVersion(),
            config.getPhoneNumberId()
        );
    }
    
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(config.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
```

---

**Creado:** 20 de octubre de 2025  
**Última actualización:** [ACTUALIZAR CUANDO OBTENGAS TOKENS]  
**Estado:** 📝 TEMPLATE - COMPLETAR CON VALORES REALES

⚠️ **RECORDATORIO:** Este archivo debe estar en .gitignore
