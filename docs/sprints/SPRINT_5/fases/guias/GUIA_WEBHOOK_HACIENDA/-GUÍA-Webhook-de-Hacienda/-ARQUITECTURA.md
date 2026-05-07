## 🏗️ ARQUITECTURA

```
┌─────────────────┐
│   Hacienda CR   │
│   API v4.4      │
└────────┬────────┘
         │ HTTP POST
         │ (Callback)
         ▼
┌─────────────────────────────────┐
│  HaciendaWebhookController      │
│  POST /api/hacienda/callback    │
└────────┬────────────────────────┘
         │
         ▼
┌─────────────────────────────────┐
│  ComprobanteElectronicoService  │
│  procesarCallbackHacienda()     │
└────────┬────────────────────────┘
         │
         ├──► Actualizar BD (ComprobanteElectronico)
         ├──► Guardar XML en filesystem
         ├──► Notificar WebSocket
         └──► Enviar Email (si configurado)
```

---

