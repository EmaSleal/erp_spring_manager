## 🔄 PROCESO DE PRUEBAS (Orden Recomendado)

### **FASE 1: Unit Tests (Aislados)** ⏱️ 2-3 horas

```
1. NotificacionServiceTest
   ├─ Test de creación de notificación
   ├─ Test de marcar como leída
   ├─ Test de contador no leídas
   └─ Test con usuario null (validación)

2. PreferenciaNotificacionServiceTest
   ├─ Test de creación por defecto
   ├─ Test de consulta por usuario/tipo
   └─ Test de actualización

3. NotificacionEventTest
   ├─ Test de Builder pattern
   ├─ Test de getDatoRelacionado()
   └─ Test de validación de campos
```

**Herramientas:**
- JUnit 5
- Mockito (para mock de repositories)
- AssertJ (assertions fluidas)

---

### **FASE 2: Integration Tests (Con BD)** ⏱️ 2-3 horas

```
4. NotificacionServiceIntegrationTest
   ├─ Test completo con BD H2
   ├─ Test de persistencia
   ├─ Test de consultas paginadas
   └─ Test de transacciones

5. EmailServiceIntegrationTest
   ├─ Test con MockMail (GreenMail)
   ├─ Test de plantillas Thymeleaf
   └─ Test de adjuntos
```

**Herramientas:**
- @SpringBootTest
- H2 Database (en memoria)
- GreenMail (SMTP mock)
- @Transactional

---

### **FASE 3: Event & Listener Tests** ⏱️ 1-2 horas

```
6. NotificacionListenerTest
   ├─ Test de procesamiento asíncrono (@Async)
   ├─ Test de resolución de usuario
   ├─ Test de determinación de canales
   └─ Test de manejo de errores

7. FacturaEventIntegrationTest
   ├─ Test: Crear factura → Evento publicado
   ├─ Test: Listener captura evento
   └─ Test: Notificación enviada
```

**Herramientas:**
- @EnableAsync
- ApplicationEventPublisher (mock)
- Awaitility (esperar eventos async)

---

### **FASE 4: WebSocket Tests** ⏱️ 1-2 horas

```
8. NotificacionWebSocketTest
   ├─ Test de conexión WebSocket
   ├─ Test de envío a usuario específico
   ├─ Test de broadcast a /topic
   └─ Test de actualización de contador
```

**Herramientas:**
- Spring WebSocket Test
- StompSession (cliente test)
- @WebSocketTest (si disponible)

---

### **FASE 5: E2E Tests (Flujo Completo)** ⏱️ 2-3 horas

```
9. NotificacionE2ETest
   └─ Flujo completo:
      1. Crear factura
      2. Evento publicado
      3. Listener procesa
      4. Notificación guardada en BD
      5. Email enviado (mock)
      6. WebSocket notifica (mock)
      7. Usuario recibe en frontend

10. SchedulerE2ETest
    └─ Flujo scheduler:
       1. Crear factura vencida (fecha pasada)
       2. Ejecutar scheduler manualmente
       3. Evento FACTURA_VENCIDA publicado
       4. Notificación enviada
       5. Verificar en BD
```

**Herramientas:**
- @SpringBootTest(webEnvironment = RANDOM_PORT)
- TestRestTemplate
- Selenium (opcional, para frontend)

---

