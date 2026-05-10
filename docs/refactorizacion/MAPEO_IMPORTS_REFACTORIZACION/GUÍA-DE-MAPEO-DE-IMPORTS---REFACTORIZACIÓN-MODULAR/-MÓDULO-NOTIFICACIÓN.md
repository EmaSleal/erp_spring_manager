## 📦 MÓDULO NOTIFICACIÓN

### Controllers
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.controllers.NotificacionRestController;` | `import api.astro.whats_orders_manager.modules.notificacion.controller.NotificacionRestController;` |
| `import api.astro.whats_orders_manager.controllers.NotificacionViewController;` | `import api.astro.whats_orders_manager.modules.notificacion.controller.NotificacionViewController;` |
| `import api.astro.whats_orders_manager.controllers.NotificacionWebSocketController;` | `import api.astro.whats_orders_manager.modules.notificacion.controller.NotificacionWebSocketController;` |

### Services
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.services.NotificacionService;` | `import api.astro.whats_orders_manager.modules.notificacion.service.NotificacionService;` |
| `import api.astro.whats_orders_manager.services.PlantillaNotificacionService;` | `import api.astro.whats_orders_manager.modules.notificacion.service.PlantillaNotificacionService;` |
| `import api.astro.whats_orders_manager.services.PreferenciaNotificacionService;` | `import api.astro.whats_orders_manager.modules.notificacion.service.PreferenciaNotificacionService;` |
| `import api.astro.whats_orders_manager.services.ConfiguracionNotificacionesService;` | `import api.astro.whats_orders_manager.modules.notificacion.service.ConfiguracionNotificacionesService;` |

### Repositories
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.repositories.NotificacionRepository;` | `import api.astro.whats_orders_manager.modules.notificacion.repository.NotificacionRepository;` |
| `import api.astro.whats_orders_manager.repositories.PlantillaNotificacionRepository;` | `import api.astro.whats_orders_manager.modules.notificacion.repository.PlantillaNotificacionRepository;` |
| `import api.astro.whats_orders_manager.repositories.PreferenciaNotificacionRepository;` | `import api.astro.whats_orders_manager.modules.notificacion.repository.PreferenciaNotificacionRepository;` |
| `import api.astro.whats_orders_manager.repositories.ConfiguracionNotificacionesRepository;` | `import api.astro.whats_orders_manager.modules.notificacion.repository.ConfiguracionNotificacionesRepository;` |

### Models
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.models.Notificacion;` | `import api.astro.whats_orders_manager.modules.notificacion.model.Notificacion;` |
| `import api.astro.whats_orders_manager.models.PlantillaNotificacion;` | `import api.astro.whats_orders_manager.modules.notificacion.model.PlantillaNotificacion;` |
| `import api.astro.whats_orders_manager.models.PreferenciaNotificacion;` | `import api.astro.whats_orders_manager.modules.notificacion.model.PreferenciaNotificacion;` |
| `import api.astro.whats_orders_manager.models.ConfiguracionNotificaciones;` | `import api.astro.whats_orders_manager.modules.notificacion.model.ConfiguracionNotificaciones;` |

### DTOs
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.models.dto.NotificacionDTO;` | `import api.astro.whats_orders_manager.modules.notificacion.dto.NotificacionDTO;` |

### Enums
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.enums.CanalNotificacion;` | `import api.astro.whats_orders_manager.modules.notificacion.enums.CanalNotificacion;` |
| `import api.astro.whats_orders_manager.enums.TipoNotificacion;` | `import api.astro.whats_orders_manager.modules.notificacion.enums.TipoNotificacion;` |
| `import api.astro.whats_orders_manager.enums.EstadoNotificacion;` | `import api.astro.whats_orders_manager.modules.notificacion.enums.EstadoNotificacion;` |

### Events
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.events.NotificacionEvent;` | `import api.astro.whats_orders_manager.modules.notificacion.events.NotificacionEvent;` |
| `import api.astro.whats_orders_manager.listeners.NotificacionEventListener;` | `import api.astro.whats_orders_manager.modules.notificacion.events.NotificacionEventListener;` |

---

