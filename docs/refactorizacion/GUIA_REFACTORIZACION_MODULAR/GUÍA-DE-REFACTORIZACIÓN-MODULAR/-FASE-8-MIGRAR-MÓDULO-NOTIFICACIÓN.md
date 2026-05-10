## 📦 FASE 8: MIGRAR MÓDULO NOTIFICACIÓN

**Duración:** 4 horas  
**Complejidad:** ⭐⭐⭐ Alta

### Archivos a Migrar

```
Controllers (3):
├── NotificacionRestController.java
├── NotificacionViewController.java
└── NotificacionWebSocketController.java

Services (4):
├── NotificacionService.java
├── PlantillaNotificacionService.java
├── PreferenciaNotificacionService.java
└── ConfiguracionNotificacionesService.java

Repositories (4):
├── NotificacionRepository.java
├── PlantillaNotificacionRepository.java
├── PreferenciaNotificacionRepository.java
└── ConfiguracionNotificacionesRepository.java

Models (4):
├── Notificacion.java
├── PlantillaNotificacion.java
├── PreferenciaNotificacion.java
└── ConfiguracionNotificaciones.java

DTOs:
├── NotificacionDTO.java
└── (otros DTOs relacionados)

Enums:
├── CanalNotificacion.java
├── TipoNotificacion.java
└── EstadoNotificacion.java

Events (si existen):
├── NotificacionEvent.java
└── NotificacionEventListener.java
```

### Consideraciones Especiales

⚠️ **EVENTOS:**
- Si usas eventos de Spring, moverlos a `modules/notificacion/events/`
- Listener puede quedarse en `core/listeners/` si es global

### Pasos

1. ✅ Mover events primero (si existen)
2. ✅ Mover enums
3. ✅ Mover models
4. ✅ Mover DTOs
5. ✅ Mover repositories
6. ✅ Mover services
7. ✅ Mover controllers
8. ✅ Actualizar packages y imports
9. ✅ Compilar y testear
10. ✅ Commit

---

