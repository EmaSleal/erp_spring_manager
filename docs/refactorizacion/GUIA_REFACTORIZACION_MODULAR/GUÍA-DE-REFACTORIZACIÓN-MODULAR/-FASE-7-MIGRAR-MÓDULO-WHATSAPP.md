## 📦 FASE 7: MIGRAR MÓDULO WHATSAPP

**Duración:** 4 horas  
**Complejidad:** ⭐⭐⭐ Alta

### Archivos a Migrar

```
Controllers (5):
├── WhatsAppViewController.java
├── WhatsAppMensajeController.java
├── WhatsAppPlantillaController.java
├── WhatsAppFacturaController.java
└── WhatsAppWebhookController.java

Services (5):
├── WhatsAppService.java
├── MensajeWhatsAppService.java
├── PlantillaWhatsAppService.java
├── WhatsAppFacturaService.java
└── WebhookWhatsAppService.java

Repositories (3):
├── MensajeWhatsAppRepository.java
├── PlantillaWhatsAppRepository.java
└── WebhookLogRepository.java

Models (3):
├── MensajeWhatsApp.java
├── PlantillaWhatsApp.java
└── WebhookLog.java

DTOs:
├── MensajeWhatsAppDTO.java
└── PlantillaWhatsAppDTO.java

Enums:
├── DireccionMensaje.java (si existe)
└── EstadoMensaje.java (si existe)
```

### Consideraciones Especiales

⚠️ **DEPENDENCIAS:**
- Usa `Factura` del módulo facturación
- Usa `Usuario` del módulo seguridad
- Usa `NotificacionService` del módulo notificación

### Script PowerShell de Ayuda

```powershell
cd "D:\programacion\java\spring-boot\whats_orders_manager\src\main\java\api\astro\whats_orders_manager"

# Controllers
$controllers = @(
    "WhatsAppViewController",
    "WhatsAppMensajeController",
    "WhatsAppPlantillaController",
    "WhatsAppFacturaController",
    "WhatsAppWebhookController"
)

foreach ($controller in $controllers) {
    Move-Item "controllers/$controller.java" "modules/whatsapp/controller/"
}

# Services
$services = @(
    "WhatsAppService",
    "MensajeWhatsAppService",
    "PlantillaWhatsAppService",
    "WhatsAppFacturaService",
    "WebhookWhatsAppService"
)

foreach ($service in $services) {
    Move-Item "services/$service.java" "modules/whatsapp/service/"
}

# Continuar con repositories, models, etc.
```

### Orden de Migración

1. ✅ Models (3 archivos)
2. ✅ Enums (si existen)
3. ✅ DTOs (2 archivos)
4. ✅ Repositories (3 archivos)
5. ✅ Services (5 archivos)
6. ✅ Controllers (5 archivos)
7. ✅ Actualizar packages
8. ✅ Actualizar imports
9. ✅ Compilar después de cada grupo
10. ✅ Test final

---

