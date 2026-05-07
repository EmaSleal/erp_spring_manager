## 🎯 MÓDULOS A PROBAR

### 1. **NotificacionService** (PRIORITARIO ⭐⭐⭐)
**Ubicación:** `services/impl/NotificacionServiceImpl.java`

**Métodos críticos a probar:**
```java
✅ enviarNotificacion(Usuario, TipoNotificacion, CanalNotificacion, String, String)
✅ enviarNotificacionConAccion(Usuario, TipoNotificacion, CanalNotificacion, String, String, String, String)
✅ enviarNotificacionConPlantilla(Usuario, TipoNotificacion, CanalNotificacion, Integer, Map)
✅ marcarComoLeida(Integer idNotificacion)
✅ marcarTodasComoLeidas(Integer idUsuario)
✅ findNoLeidasByUsuarioId(Integer idUsuario, Pageable)
✅ countNoLeidasByUsuarioId(Integer idUsuario)
```

**¿Por qué es prioritario?**
- Es el núcleo del sistema de notificaciones
- Maneja la lógica de negocio principal
- Interactúa con múltiples servicios (Email, WebSocket)
- Gestiona la persistencia en BD

---

### 2. **NotificacionListener** (PRIORITARIO ⭐⭐⭐)
**Ubicación:** `listeners/NotificacionListener.java`

**Funcionalidades a probar:**
```java
✅ procesarNotificacion(NotificacionEvent) - @Async
✅ procesarNotificacionBroadcast(NotificacionEvent) - @Async
✅ resolverUsuario() - Obtiene usuario por ID o objeto
✅ determinarCanales() - Selecciona canales según configuración
✅ enviarPorCanal() - Delega envío según canal
```

**¿Por qué es prioritario?**
- Procesa eventos asíncronamente
- Distribuye notificaciones por múltiples canales
- Punto crítico de la arquitectura event-driven

---

### 3. **EmailService** (PRIORITARIO ⭐⭐)
**Ubicación:** `services/impl/EmailServiceImpl.java`

**Métodos a probar:**
```java
✅ enviarEmail(String destinatario, String asunto, String cuerpo)
✅ enviarEmailConPlantilla(String destinatario, String plantilla, Map variables)
✅ enviarRecordatorioPago(Factura) - Integración con scheduler
```

**¿Por qué es prioritario?**
- Canal principal de notificaciones
- Usa plantillas Thymeleaf
- Configuración SMTP crítica

---

### 4. **NotificacionWebSocketController** (MEDIO ⭐⭐)
**Ubicación:** `controllers/NotificacionWebSocketController.java`

**Endpoints WebSocket a probar:**
```java
✅ /app/notificaciones/broadcast → /topic/notificaciones
✅ /app/notificaciones/marcar-leida → /user/queue/notificaciones/leida
✅ /app/notificaciones/no-leidas → /user/queue/notificaciones/lista
✅ enviarNotificacionAUsuario(Integer, NotificacionDTO)
✅ notificarContadorNoLeidas(Integer, Long)
```

**¿Por qué es medio?**
- Requiere configuración de WebSocket test
- Menos crítico que lógica de negocio
- UI funciona sin WebSocket (polling fallback)

---

### 5. **NotificacionEvent + FacturaService** (MEDIO ⭐⭐)
**Ubicación:** 
- `events/NotificacionEvent.java`
- `services/impl/FacturaServiceImpl.java`
- `schedulers/RecordatorioPagoScheduler.java`

**Flujos de eventos a probar:**
```java
✅ Crear factura → Publicar evento FACTURA_CREADA
✅ Scheduler detecta vencida → Publicar FACTURA_VENCIDA
✅ Scheduler detecta próxima vencer → Publicar FACTURA_PROXIMA_VENCER
✅ Listener captura eventos → Envía notificaciones
```

**¿Por qué es medio?**
- Integración entre múltiples componentes
- Scheduler requiere configuración especial
- Eventos asíncronos dificultan testing

---

### 6. **PreferenciaNotificacionService** (BAJO ⭐)
**Ubicación:** `services/impl/PreferenciaNotificacionServiceImpl.java`

**Métodos a probar:**
```java
✅ findByUsuarioAndTipo(Integer, TipoNotificacion)
✅ estaActivo(Integer, TipoNotificacion, CanalNotificacion)
✅ actualizarPreferencia(Integer, TipoNotificacion, boolean, boolean, boolean)
✅ crearPreferenciasPorDefecto(Integer)
```

**¿Por qué es bajo?**
- CRUD simple
- Sin lógica de negocio compleja
- Ya probado manualmente

---

