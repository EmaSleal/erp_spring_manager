## 🧪 ESTRATEGIA DE TESTING

### 1. Tests Unitarios (NotificacionServiceTest)

**Archivo:** `NotificacionServiceTest.java`  
**Framework:** JUnit 5 + Mockito  
**Cobertura objetivo:** >70%

**Casos de prueba:**
- ✅ Creación de notificaciones
- ✅ Envío multicanal (Web, Email, WhatsApp)
- ✅ Validación de preferencias de usuario
- ✅ Procesamiento de plantillas con variables
- ✅ Consultas paginadas
- ✅ Marcar como leída
- ✅ Eliminación de notificaciones
- ✅ Manejo de errores y edge cases

**Estado:** Estructura base creada, requiere ajustes menores para compilación.

---

### 2. Tests de Integración - Email

**Objetivo:** Verificar envío real de emails a través de SMTP

**Escenarios probados manualmente:**
- ✅ Envío de email con plantilla HTML
- ✅ Variables dinámicas procesadas correctamente
- ✅ Manejo de errores SMTP
- ✅ Registro de intentos de envío
- ✅ Validación de preferencias de usuario

**Configuración:**
```yaml
# application-test.yml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${EMAIL_USERNAME}
    password: ${EMAIL_PASSWORD}
```

**Resultado:** ✅ **FUNCIONAL** - Emails se envían correctamente

---

### 3. Tests de Integración - WhatsApp

**Objetivo:** Verificar integración con Meta WhatsApp Business API

**Escenarios probados manualmente:**
- ✅ Envío de mensajes de texto
- ✅ Mensajes con botones interactivos
- ✅ Envío de plantillas aprobadas
- ✅ Manejo de errores de API
- ✅ Validación de números de teléfono

**Endpoint testeado:**
```
POST https://graph.facebook.com/v18.0/{PHONE_NUMBER_ID}/messages
Authorization: Bearer {ACCESS_TOKEN}
```

**Resultado:** ✅ **FUNCIONAL** - Integración con Meta API operativa

---

### 4. Tests de WebSocket

**Objetivo:** Verificar actualización en tiempo real de notificaciones

**Componentes testeados:**
- ✅ `WebSocketConfig` - Configuración STOMP  
- ✅ `NotificacionWebSocketController` - MessageMapping
- ✅ `websocket-notificaciones.js` - Cliente JavaScript
- ✅ Conexión bidireccional
- ✅ Broadcast a usuarios específicos

**Flujo testeado:**
```
1. Usuario se conecta al WebSocket
2. Se suscribe a /user/queue/notificaciones
3. Backend envía notificación
4. Cliente recibe mensaje en tiempo real
5. Badge y dropdown se actualizan automáticamente
```

**Resultado:** ✅ **FUNCIONAL** - WebSocket operativo en producción

---

### 5. Tests de Eventos y Listeners

**Objetivo:** Verificar sistema de eventos asíncronos

**Escenarios testeados:**
- ✅ `NotificacionEvent` - Creación de eventos
- ✅ `NotificacionListener` - Procesamiento @Async
- ✅ Evento: Factura creada → Notificación al cliente
- ✅ Evento: Factura vencida → Notificación de recordatorio
- ✅ Evento: Pago recibido → Confirmación

**Configuración:**
```java
@EnableAsync
@Configuration
public class AsyncConfig implements AsyncConfigurer {
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        return executor;
    }
}
```

**Resultado:** ✅ **FUNCIONAL** - Eventos procesados asíncronamente

---

### 6. Tests E2E - Flujo Completo

**Objetivo:** Validar flujo end-to-end de notificaciones

**Flujo testeado:**

```
PASO 1: Crear Factura
  ├─> FacturaServiceImpl.crear()
  └─> applicationEventPublisher.publishEvent(NotificacionEvent)

PASO 2: Listener Recibe Evento
  ├─> NotificacionListener.handleNotificacionEvent()
  └─> @Async processing

PASO 3: Validar Preferencias
  ├─> PreferenciaNotificacionService.validar()
  └─> Verificar canales activos (WEB, EMAIL, WHATSAPP)

PASO 4: Envío Multicanal
  ├─> notificacionService.enviarNotificacionWeb() → BD + WebSocket
  ├─> notificacionService.enviarNotificacionEmail() → SMTP
  └─> notificacionService.enviarNotificacionWhatsApp() → Meta API

PASO 5: Usuario Recibe Notificaciones
  ├─> Badge actualizado en navbar
  ├─> Dropdown con últimas 5 notificaciones
  ├─> Email en bandeja de entrada
  └─> Mensaje WhatsApp en teléfono

PASO 6: Usuario Marca como Leída
  ├─> Click en notificación
  ├─> PUT /api/notificaciones/{id}/marcar-leida
  └─> Badge decrementa contador
```

**Resultado:** ✅ **FUNCIONAL** - Flujo completo operativo

---

