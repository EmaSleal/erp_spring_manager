## 🔗 PRUEBAS DE INTEGRACIÓN

### **Escenario 1: Flujo Completo de Factura Nueva**

```
INPUT: POST /api/facturas (crear factura)
       ↓
STEP 1: FacturaService.save()
       ↓
STEP 2: Publica NotificacionEvent(FACTURA_CREADA)
       ↓
STEP 3: NotificacionListener.procesarNotificacion()
       ↓
STEP 4: NotificacionService.enviarNotificacion()
       ↓
STEP 5: Guarda en BD (notificacion table)
       ↓
STEP 6: EmailService.enviarEmail() (si activo)
       ↓
STEP 7: WebSocketController.enviarNotificacionAUsuario()
       ↓
OUTPUT: 
- Notificación en BD ✅
- Email enviado ✅
- WebSocket broadcast ✅
- Cliente recibe en navbar ✅
```

**Validaciones:**
- ✅ Factura guardada con ID
- ✅ Notificación creada con idFactura en datosRelacionados
- ✅ Email recibido en bandeja (GreenMail)
- ✅ Contador de badge actualizado

---

### **Escenario 2: Scheduler de Recordatorios**

```
SETUP: Crear factura vencida (fechaPago = ayer)
       ↓
INPUT: Ejecutar scheduler manualmente
       RecordatorioPagoScheduler.enviarRecordatoriosPago()
       ↓
STEP 1: Busca facturas vencidas en BD
       ↓
STEP 2: Por cada factura:
        - Publica NotificacionEvent(FACTURA_VENCIDA)
       ↓
STEP 3: Listener procesa eventos
       ↓
STEP 4: Envía notificaciones por canales activos
       ↓
OUTPUT:
- Eventos publicados ✅
- Notificaciones en BD ✅
- Emails enviados ✅
```

**Validaciones:**
- ✅ Solo facturas vencidas procesadas
- ✅ Eventos con datos correctos (diasVencida)
- ✅ Notificaciones con tipo FACTURA_VENCIDA
- ✅ Usuario correcto como destinatario

---

### **Escenario 3: Configuración de Preferencias**

```
INPUT: PUT /api/preferencias-notificacion/guardar
       Body: [
         { tipo: "FACTURA_CREADA", 
           activoWeb: true, 
           activoEmail: false, 
           activoWhatsapp: false }
       ]
       ↓
STEP 1: PreferenciaNotificacionService.actualizarPreferencia()
       ↓
STEP 2: Guarda en BD
       ↓
INPUT 2: Crear factura (dispara notificación)
       ↓
STEP 3: NotificacionService verifica preferencias
       ↓
STEP 4: Envía SOLO por canal WEB (email desactivado)
       ↓
OUTPUT:
- Notificación guardada ✅
- Email NO enviado ✅
- WebSocket SI enviado ✅
```

**Validaciones:**
- ✅ Preferencias guardadas correctamente
- ✅ Solo canales activos usados
- ✅ Canales inactivos ignorados

---

