## 🔧 CONFIGURACIÓN TÉCNICA (Día 6-7)

### Generar Access Token Permanente

1. **Ir a:** Graph API Explorer
2. **Seleccionar app:** ERP Orders Manager
3. **Permisos necesarios:**
   - `whatsapp_business_messaging`
   - `whatsapp_business_management`
4. **Generar token**
5. **Guardar en variables de entorno:**

```env
# .env (NO COMMITEAR)
META_WHATSAPP_PHONE_NUMBER_ID=123456789012345
META_WHATSAPP_ACCESS_TOKEN=EAAxxxxxxxxxxxxxxxxxxxx
META_WEBHOOK_VERIFY_TOKEN=mi_token_secreto_12345
```

### Configurar Webhook

1. **Preparar URL pública:**
   - **Opción A:** ngrok (desarrollo)
     ```bash
     ngrok http 8080
     # URL: https://abc123.ngrok.io
     ```
   - **Opción B:** Servidor staging
     ```
     https://staging.tudominio.com
     ```

2. **En Meta for Developers:**
   - WhatsApp → Configuration → Webhook
   - **Callback URL:** `https://TU-URL/api/whatsapp/webhook`
   - **Verify Token:** `mi_token_secreto_12345`
   - **Webhook fields:** ☑️ messages, ☑️ message_status

3. **Probar webhook:**
   - Meta enviará GET con challenge
   - Nuestro endpoint debe responder con el challenge

---

