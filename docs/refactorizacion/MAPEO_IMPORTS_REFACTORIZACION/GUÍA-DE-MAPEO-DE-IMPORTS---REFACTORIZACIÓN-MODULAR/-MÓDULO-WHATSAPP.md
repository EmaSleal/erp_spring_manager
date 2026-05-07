## 📦 MÓDULO WHATSAPP

### Controllers
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.controllers.WhatsAppViewController;` | `import api.astro.whats_orders_manager.modules.whatsapp.controller.WhatsAppViewController;` |
| `import api.astro.whats_orders_manager.controllers.WhatsAppMensajeController;` | `import api.astro.whats_orders_manager.modules.whatsapp.controller.WhatsAppMensajeController;` |
| `import api.astro.whats_orders_manager.controllers.WhatsAppPlantillaController;` | `import api.astro.whats_orders_manager.modules.whatsapp.controller.WhatsAppPlantillaController;` |
| `import api.astro.whats_orders_manager.controllers.WhatsAppFacturaController;` | `import api.astro.whats_orders_manager.modules.whatsapp.controller.WhatsAppFacturaController;` |
| `import api.astro.whats_orders_manager.controllers.WhatsAppWebhookController;` | `import api.astro.whats_orders_manager.modules.whatsapp.controller.WhatsAppWebhookController;` |

### Services
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.services.WhatsAppService;` | `import api.astro.whats_orders_manager.modules.whatsapp.service.WhatsAppService;` |
| `import api.astro.whats_orders_manager.services.MensajeWhatsAppService;` | `import api.astro.whats_orders_manager.modules.whatsapp.service.MensajeWhatsAppService;` |
| `import api.astro.whats_orders_manager.services.PlantillaWhatsAppService;` | `import api.astro.whats_orders_manager.modules.whatsapp.service.PlantillaWhatsAppService;` |
| `import api.astro.whats_orders_manager.services.WhatsAppFacturaService;` | `import api.astro.whats_orders_manager.modules.whatsapp.service.WhatsAppFacturaService;` |
| `import api.astro.whats_orders_manager.services.WebhookWhatsAppService;` | `import api.astro.whats_orders_manager.modules.whatsapp.service.WebhookWhatsAppService;` |

### Repositories
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.repositories.MensajeWhatsAppRepository;` | `import api.astro.whats_orders_manager.modules.whatsapp.repository.MensajeWhatsAppRepository;` |
| `import api.astro.whats_orders_manager.repositories.PlantillaWhatsAppRepository;` | `import api.astro.whats_orders_manager.modules.whatsapp.repository.PlantillaWhatsAppRepository;` |
| `import api.astro.whats_orders_manager.repositories.WebhookLogRepository;` | `import api.astro.whats_orders_manager.modules.whatsapp.repository.WebhookLogRepository;` |

### Models
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.models.MensajeWhatsApp;` | `import api.astro.whats_orders_manager.modules.whatsapp.model.MensajeWhatsApp;` |
| `import api.astro.whats_orders_manager.models.PlantillaWhatsApp;` | `import api.astro.whats_orders_manager.modules.whatsapp.model.PlantillaWhatsApp;` |
| `import api.astro.whats_orders_manager.models.WebhookLog;` | `import api.astro.whats_orders_manager.modules.whatsapp.model.WebhookLog;` |

### DTOs
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.models.dto.MensajeWhatsAppDTO;` | `import api.astro.whats_orders_manager.modules.whatsapp.dto.MensajeWhatsAppDTO;` |
| `import api.astro.whats_orders_manager.models.dto.PlantillaWhatsAppDTO;` | `import api.astro.whats_orders_manager.modules.whatsapp.dto.PlantillaWhatsAppDTO;` |

### Enums
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.enums.DireccionMensaje;` | `import api.astro.whats_orders_manager.modules.whatsapp.enums.DireccionMensaje;` |
| `import api.astro.whats_orders_manager.enums.EstadoMensaje;` | `import api.astro.whats_orders_manager.modules.whatsapp.enums.EstadoMensaje;` |

---

