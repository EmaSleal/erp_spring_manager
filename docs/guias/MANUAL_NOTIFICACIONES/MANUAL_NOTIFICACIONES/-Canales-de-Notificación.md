## 📡 Canales de Notificación

### Canal Web 📱

**Características:**
- **Tiempo real:** Notificaciones instantáneas via WebSocket
- **Badge:** Contador de notificaciones sin leer en el navbar
- **Dropdown:** Lista desplegable con últimas notificaciones
- **Sin configuración:** Funciona automáticamente al iniciar sesión

**Ubicación:**
```
┌─────────────────────────────────────────────────┐
│  [Logo]  Inicio  Facturas  Clientes  [🔔 (5)]  │
│                                        ↓        │
│                              ┌─────────────────┐│
│                              │ Notificaciones  ││
│                              ├─────────────────┤│
│                              │ 🔴 Nueva fact...││
│                              │ 📧 Pago reci... ││
│                              │ ⚠️ Stock bajo...││
│                              │                 ││
│                              │ Ver todas →     ││
│                              └─────────────────┘│
└─────────────────────────────────────────────────┘
```

**Estados visuales:**
- 🔴 **Punto rojo:** Notificación nueva (no leída)
- 🟢 **Sin punto:** Notificación leída
- 🔢 **Badge (5):** Número total sin leer

---

### Canal Email 📧

**Características:**
- **Plantillas HTML:** Emails profesionales con formato
- **Adjuntos:** Puede incluir PDF (ej: facturas)
- **Personalización:** Contenido dinámico según el evento
- **Requiere configuración:** Email del usuario debe estar registrado

**Ejemplo visual de email:**
```
┌──────────────────────────────────────────┐
│  [Logo Empresa]                         │
│                                          │
│  Hola, Juan Pérez                        │
│                                          │
│  ¡Nueva Factura Creada!                  │
│                                          │
│  Se ha generado la factura F001-00125    │
│  para el cliente ABC Company.            │
│                                          │
│  Detalles:                               │
│  • Cliente: ABC Company                  │
│  • Subtotal: S/ 1,000.00                │
│  • IGV (18%): S/ 180.00                 │
│  • Total: S/ 1,180.00                   │
│                                          │
│  [Ver Factura]  [Descargar PDF]         │
│                                          │
│  ────────────────────────────────────    │
│  Este es un correo automático.           │
│  ERP Orders Manager © 2026          │
└──────────────────────────────────────────┘
```

**Configuración de Email:**
- En **Configuración > Notificaciones**
- Requiere servidor SMTP configurado
- Email del usuario en su perfil

---

### Canal WhatsApp 💬

**Características:**
- **API de Meta:** Integración con WhatsApp Business API
- **Plantillas aprobadas:** Solo plantillas pre-aprobadas por Meta
- **Requiere teléfono:** Cliente/usuario debe tener número
- **Confirmación:** Se registra el estado de entrega

**Plantillas disponibles:**
1. **recordatorio_pago:** Recordatorio de factura vencida
2. **factura_nueva:** Notificación de nueva factura
3. **confirmacion_pago:** Confirmación de pago recibido

**Ejemplo de mensaje:**
```
─────────────────────────────
  WhatsApp Notification
─────────────────────────────

🏢 *Tu Empresa*

Hola Juan,

Te recordamos que la factura 
F001-00125 vence el 10/01/2026.

💰 Monto: S/ 1,180.00

Puedes ver los detalles en:
https://app.empresa.com/facturas/125

Gracias por tu preferencia.
─────────────────────────────
```

---

