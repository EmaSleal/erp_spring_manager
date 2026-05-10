## 🔐 SEGURIDAD

### 1. Validación de Token

El webhook acepta un header `X-Webhook-Token` para autenticar las llamadas:

```http
POST /api/hacienda/callback HTTP/1.1
Host: tu-dominio.com
Content-Type: application/json
X-Webhook-Token: tu-token-secreto-aqui

{
  "claveNumerica": "50612202600100111234567890123456789012345678901234",
  "estado": "aceptado",
  "codigoRespuesta": "1",
  "mensaje": "Aceptado",
  "xmlRespuesta": "PD94bWwgdmVyc2lvbj0iMS4wIi...",
  "fechaRespuesta": "2026-01-24T15:30:00"
}
```

### 2. Validación de Firma Digital (Opcional)

Para mayor seguridad, validar la firma del XML de respuesta:

```java
// Validar que el XML venga firmado por Hacienda
if (!firmaDigitalService.validarFirma(xmlDecodificado)) {
    throw new SecurityException("Firma digital inválida en XML de respuesta");
}
```

---

