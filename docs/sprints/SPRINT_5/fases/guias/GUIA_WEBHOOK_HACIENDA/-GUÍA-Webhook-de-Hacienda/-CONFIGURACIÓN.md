## ⚙️ CONFIGURACIÓN

### application.yml

Agregar configuración del webhook:

```yaml
facturacion:
  hacienda:
    webhook:
      # Token de seguridad para validar callbacks (opcional)
      # Si está vacío, no se valida token
      token: ${HACIENDA_WEBHOOK_TOKEN:}
      
      # URL pública del webhook (para registrar en Hacienda)
      url: ${HACIENDA_WEBHOOK_URL:https://tu-dominio.com/api/hacienda/callback}
```

### Variables de Entorno

```bash
# Token de seguridad compartido con Hacienda
HACIENDA_WEBHOOK_TOKEN=tu-token-secreto-aqui

# URL pública del webhook
HACIENDA_WEBHOOK_URL=https://tu-dominio.com/api/hacienda/callback
```

---

