## 🏗️ ARQUITECTURA DEL SISTEMA

### Visión General

```
┌─────────────────────────────────────────────────────────────────┐
│                    EVENTOS DISPARADORES                          │
├─────────────────────────────────────────────────────────────────┤
│  - Nueva factura creada                                         │
│  - Factura pagada                                               │
│  - Factura vencida                                              │
│  - Pedido confirmado                                            │
│  - Nuevo usuario registrado                                     │
│  - Contraseña restablecida                                      │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│                  NOTIFICACION SERVICE (Orquestador)             │
├─────────────────────────────────────────────────────────────────┤
│  notificar(tipoEvento, destinatario, datos)                    │
│    │                                                             │
│    ├─→ Obtiene plantilla según evento                          │
│    ├─→ Reemplaza variables dinámicas                           │
│    ├─→ Consulta preferencias del usuario                       │
│    └─→ Dispara canales habilitados en paralelo                │
└─────────────────────────────────────────────────────────────────┘
                            ↓
        ┌───────────────────┼───────────────────┐
        ↓                   ↓                   ↓
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ WEB SOCKET  │    │   EMAIL     │    │  WHATSAPP   │
│             │    │   SERVICE   │    │   SERVICE   │
│ • En tiempo │    │             │    │             │
│   real      │    │ • SMTP      │    │ • API REST  │
│ • In-app    │    │ • HTML      │    │ • Templates │
│ • Badges    │    │ • Adjuntos  │    │ • Media     │
└─────────────┘    └─────────────┘    └─────────────┘
        ↓                   ↓                   ↓
┌─────────────────────────────────────────────────────────────────┐
│                    REGISTRO DE NOTIFICACIONES                    │
├─────────────────────────────────────────────────────────────────┤
│  Tabla: notificaciones                                          │
│  - Estado: ENVIADO, ENTREGADO, LEIDO, FALLIDO                  │
│  - Timestamps de cada estado                                    │
│  - Mensaje de error (si aplica)                                │
│  - Referencia a entidad (factura_id, pedido_id)                │
└─────────────────────────────────────────────────────────────────┘
```

### Componentes Principales

1. **NotificacionService** (Orquestador)
2. **WhatsAppService** (Integración API)
3. **EmailService** (SMTP)
4. **NotificacionWebSocketController** (WebSocket)
5. **PlantillaService** (Gestión de plantillas)
6. **PreferenciaNotificacionService** (Configuración usuario)

---

