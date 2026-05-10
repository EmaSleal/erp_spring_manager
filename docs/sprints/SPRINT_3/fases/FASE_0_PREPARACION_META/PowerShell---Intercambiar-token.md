  # PowerShell - Intercambiar token
  $appId = "TU_APP_ID"
  $appSecret = "TU_APP_SECRET"
  $shortToken = "TU_TOKEN_TEMPORAL"
  
  $url = "https://graph.facebook.com/v18.0/oauth/access_token?grant_type=fb_exchange_token&client_id=$appId&client_secret=$appSecret&fb_exchange_token=$shortToken"
  
- [x] **Paso 4.2.6:** Guardar token permanente
  ```bash
