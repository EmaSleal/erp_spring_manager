  # Simular verificación de Meta
  $verifyToken = "TU_VERIFY_TOKEN"
  $challenge = "test123"
- [x] **Paso 4.3.5:** Probar webhook manualmente
  - Pruebas locales exitosas ✅
  - Pruebas con Meta exitosas ✅b.mode=subscribe&hub.verify_token=$verifyToken&hub.challenge=$challenge"
  
  Invoke-RestMethod -Uri $url -Method Get
