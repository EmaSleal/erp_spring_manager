# 30 días = 2592000 segundos
security.remember-me.validity-seconds=2592000
```

### Seguridad

- Cookie es **httpOnly** (no accesible por JavaScript)
- Cookie es **secure** (solo HTTPS en producción)
- Cookie se elimina al hacer logout
- Tokens se guardan en base de datos (revocables)

---

## ⏱️ Rate Limiting

### Límites por IP

Para prevenir abuso:

| Endpoint | Límite | Ventana |
|----------|--------|---------|
| `/login` | 10 intentos | 1 hora |
| `/api/**` | 100 requests | 1 minuto |
| `/registro` | 5 registros | 1 hora |

### Respuesta cuando se excede límite

```json
HTTP 429 Too Many Requests
{
    "error": "Rate limit exceeded",
    "message": "Demasiados intentos. Intenta en 45 minutos.",
    "retryAfter": 2700
}
```

---

## 🛡️ Headers de Seguridad

### Headers Implementados

```http
