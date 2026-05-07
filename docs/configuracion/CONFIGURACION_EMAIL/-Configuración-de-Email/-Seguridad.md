## 🔒 Seguridad

### ✅ Mejores prácticas

1. **NUNCA** subas archivos `.env` al repositorio
2. **NUNCA** hardcodees credenciales en el código
3. **SIEMPRE** usa contraseñas de aplicación (Gmail)
4. **SIEMPRE** mantén `.env` en `.gitignore`
5. **ROTACIÓN:** Cambia las contraseñas periódicamente

### 🚫 NO hacer

```java
// ❌ MAL - Hardcodear credenciales
spring.mail.username=mi-email@gmail.com
spring.mail.password=mi-password-secreta
```

```java
// ✅ BIEN - Usar variables de entorno
spring.mail.username=${EMAIL_USERNAME}
spring.mail.password=${EMAIL_PASSWORD}
```

---

