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

