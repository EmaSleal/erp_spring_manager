## 📝 CASOS DE PRUEBA POR MÓDULO

### **1. NotificacionServiceTest**

```java
@Test
void deberiaCrearNotificacionExitosamente()
    // GIVEN: Usuario válido y datos de notificación
    // WHEN: Llamar enviarNotificacion()
    // THEN: Notificación guardada en BD, retorna DTO

@Test
void deberiaMarcarComoLeida()
    // GIVEN: Notificación no leída existente
    // WHEN: Llamar marcarComoLeida(id)
    // THEN: Campo 'leida' = true, fechaLeida != null

@Test
void deberiaContarNoLeidas()
    // GIVEN: 3 notificaciones no leídas para usuario
    // WHEN: Llamar countNoLeidasByUsuarioId()
    // THEN: Retorna 3

@Test
void deberiaMarcarTodasComoLeidas()
    // GIVEN: 5 notificaciones no leídas
    // WHEN: Llamar marcarTodasComoLeidas()
    // THEN: Todas con leida = true

@Test
void deberiaRechazarUsuarioNull()
    // GIVEN: Usuario = null
    // WHEN: Llamar enviarNotificacion()
    // THEN: Lanza IllegalArgumentException

@Test
void deberiaEnviarPorCanalActivo()
    // GIVEN: Preferencia WEB=true, EMAIL=false
    // WHEN: Llamar enviarNotificacion() con WEB
    // THEN: Notificación enviada por WEB solamente

@Test
void deberiaUsarPlantillaCorrectamente()
    // GIVEN: Plantilla con variables {nombre}, {total}
    // WHEN: Llamar enviarNotificacionConPlantilla()
    // THEN: Variables reemplazadas correctamente
```

---

### **2. NotificacionListenerTest**

```java
@Test
void deberiaProcesarEventoAsincronamente()
    // GIVEN: Evento FACTURA_CREADA
    // WHEN: Publicar evento
    // THEN: Listener procesa en thread separado

@Test
void deberiaResolverUsuarioPorId()
    // GIVEN: Evento con idUsuario=1
    // WHEN: Listener resuelve usuario
    // THEN: Usuario cargado desde BD

@Test
void deberiaDeterminarCanalesSegunPreferencias()
    // GIVEN: Preferencias WEB + EMAIL activos
    // WHEN: Determinar canales
    // THEN: Retorna [WEB, EMAIL]

@Test
void deberiaManejarErrorGracefully()
    // GIVEN: Usuario inexistente
    // WHEN: Procesar evento
    // THEN: Log error pero no lanza excepción

@Test
void deberiaProcesarBroadcast()
    // GIVEN: Evento sin usuario específico
    // WHEN: Procesar evento
    // THEN: Enviado solo por canal WEB
```

---

### **3. EmailServiceTest**

```java
@Test
void deberiaEnviarEmailSimple()
    // GIVEN: Destinatario, asunto, cuerpo
    // WHEN: Llamar enviarEmail()
    // THEN: Email enviado (verificar con GreenMail)

@Test
void deberiaUsarPlantillaThymeleaf()
    // GIVEN: Plantilla 'recordatorio-pago.html'
    // WHEN: Enviar con variables
    // THEN: HTML generado correctamente

@Test
void deberiaRechazarEmailInvalido()
    // GIVEN: Email = "invalido"
    // WHEN: Intentar enviar
    // THEN: Lanza MessagingException

@Test
void deberiaEnviarRecordatorioPago()
    // GIVEN: Factura vencida
    // WHEN: Llamar enviarRecordatorioPago()
    // THEN: Email enviado con datos de factura
```

---

### **4. WebSocket Tests**

```java
@Test
void deberiaConectarWebSocket()
    // GIVEN: Cliente WebSocket
    // WHEN: Conectar a /ws-notificaciones
    // THEN: Conexión establecida

@Test
void deberiaEnviarAUsuarioEspecifico()
    // GIVEN: Usuario ID=1 conectado
    // WHEN: Enviar a /user/queue/notificaciones
    // THEN: Solo usuario 1 recibe

@Test
void deberiaBroadcastATodos()
    // GIVEN: 3 usuarios conectados
    // WHEN: Enviar a /topic/notificaciones
    // THEN: Los 3 reciben mensaje

@Test
void deberiaActualizarContador()
    // GIVEN: 5 notificaciones no leídas
    // WHEN: Enviar contador a usuario
    // THEN: Recibe contador=5
```

---

### **5. Event Integration Tests**

```java
@Test
void deberiaPublicarEventoAlCrearFactura()
    // GIVEN: Nueva factura
    // WHEN: Llamar facturaService.save()
    // THEN: Evento FACTURA_CREADA publicado

@Test
void deberiaListenerCapturarEvento()
    // GIVEN: Listener configurado
    // WHEN: Publicar NotificacionEvent
    // THEN: Método procesarNotificacion() llamado

@Test
void deberiaCrearNotificacionDesdeEvento()
    // GIVEN: Evento con datos completos
    // WHEN: Listener procesa
    // THEN: Notificación guardada en BD

@Test
void schedulerDeberiaPublicarEventoVencida()
    // GIVEN: Factura con fechaPago < hoy
    // WHEN: Ejecutar scheduler
    // THEN: Evento FACTURA_VENCIDA publicado

@Test
void schedulerDeberiaPublicarEventoProximaVencer()
    // GIVEN: Factura con fechaPago = hoy + 2 días
    // WHEN: Ejecutar scheduler
    // THEN: Evento FACTURA_PROXIMA_VENCER publicado
```

---

