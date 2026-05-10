## 🔐 Variables de Entorno

### Archivos de Configuración

| Archivo | Propósito | Git |
|---------|-----------|-----|
| `.env.example` | Plantilla con ejemplos | ✅ Commitear |
| `.env.local` | Credenciales reales | ❌ **NUNCA** commitear |

### Variables Definidas

#### Base de Datos
```bash
DB_URL=jdbc:mysql://host:port/database?params
DB_USERNAME=usuario
DB_PASSWORD=contraseña
```

#### Email (Gmail SMTP)
```bash
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_USERNAME=tu-email@gmail.com
EMAIL_PASSWORD=app-password-generado
```

#### WhatsApp Meta API
```bash
META_WHATSAPP_PHONE_NUMBER_ID=phone-id
META_WHATSAPP_ACCESS_TOKEN=token-temporal
META_WHATSAPP_API_VERSION=v18.0
META_WHATSAPP_API_URL=https://graph.facebook.com
META_WEBHOOK_VERIFY_TOKEN=token-verificacion
```

---

