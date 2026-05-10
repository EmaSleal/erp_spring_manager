## 🧪 TESTING

### Prueba Manual con cURL

```bash
curl -X POST http://localhost:8080/api/hacienda/callback \
  -H "Content-Type: application/json" \
  -H "X-Webhook-Token: tu-token-secreto-aqui" \
  -d '{
    "claveNumerica": "50612202600100111234567890123456789012345678901234",
    "estado": "aceptado",
    "codigoRespuesta": "1",
    "mensaje": "Comprobante aceptado",
    "fechaRespuesta": "2026-01-24T15:30:00"
  }'
```

### Respuesta Esperada

```json
{
  "message": "Callback procesado exitosamente"
}
```

---

