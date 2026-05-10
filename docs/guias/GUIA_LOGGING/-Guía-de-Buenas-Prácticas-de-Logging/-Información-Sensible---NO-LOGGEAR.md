## 🔐 Información Sensible - NO LOGGEAR

### ⛔ **NUNCA loggear:**
- ❌ Contraseñas (plain text o encriptadas)
- ❌ Tokens de autenticación
- ❌ Números de tarjetas de crédito
- ❌ Datos personales sensibles (sin anonimizar)
- ❌ API Keys o secrets

### ✅ **Alternativas seguras:**

```java
// ❌ MAL
log.info("Login con password: {}", password);

// ✅ BIEN
log.info("Intento de login para usuario: {}", username);

// ❌ MAL
log.info("Token generado: {}", token);

// ✅ BIEN
log.info("Token generado para usuario: {} - Expira en: {}", username, expiresIn);
```

---

