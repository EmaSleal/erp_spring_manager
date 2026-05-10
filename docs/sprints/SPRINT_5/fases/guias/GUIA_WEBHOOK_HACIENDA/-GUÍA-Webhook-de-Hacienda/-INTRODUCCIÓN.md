## 📋 INTRODUCCIÓN

Esta guía documenta la implementación del endpoint webhook para recibir notificaciones asíncronas de Hacienda de Costa Rica cuando el estado de un comprobante electrónico cambia.

### ¿Qué es un Webhook?

Un webhook es un endpoint HTTP que Hacienda llama automáticamente cuando:
- Un comprobante es aceptado ✅
- Un comprobante es rechazado ❌
- Ocurre un error en el procesamiento ⚠️

**Ventajas sobre Polling:**
- ⚡ Actualizaciones en tiempo real
- 📉 Menos llamadas a la API de Hacienda
- 🔔 Notificaciones instantáneas al usuario
- 💰 Menor consumo de recursos

---

