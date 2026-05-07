## 🔍 ESTRUCTURA ACTUAL VS PROPUESTA

### 📦 Estructura Actual (Package by Layer)

```
src/main/java/api/astro/whats_orders_manager/
├── controllers/              (30 archivos mezclados)
│   ├── FacturaController.java
│   ├── ClienteController.java
│   ├── WhatsAppViewController.java
│   ├── NotificacionRestController.java
│   └── ...
├── services/                 (28 archivos mezclados)
│   ├── FacturaService.java
│   ├── ClienteService.java
│   ├── WhatsAppService.java
│   └── ...
├── repositories/
├── models/                   (25+ archivos mezclados)
│   ├── Factura.java
│   ├── Cliente.java
│   ├── MensajeWhatsApp.java
│   └── ...
├── config/
├── util/
└── WhatsOrdersManagerApplication.java
```

**❌ Problemas:**
- Difícil encontrar todo lo relacionado con WhatsApp
- 30 controllers en una sola carpeta
- Cambios en facturación requieren navegar 4+ carpetas
- Difícil de escalar

---

### ✅ Estructura Propuesta (Package by Feature)

```
api.astro.whats_orders_manager/
│
├── WhatsOrdersManagerApplication.java
│
├── shared/                          # Código compartido
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── WebConfig.java
│   │   └── ThymeleafConfig.java
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java
│   │   └── exceptions/
│   ├── util/
│   │   ├── DateUtil.java
│   │   └── FileUtil.java
│   └── dto/                         # DTOs compartidos
│       └── ApiResponse.java
│
├── modules/                         # Módulos de negocio
│   │
│   ├── facturacion/                 # Módulo de Facturación
│   │   ├── controller/
│   │   │   ├── FacturaController.java
│   │   │   └── LineaFacturaController.java
│   │   ├── service/
│   │   │   ├── FacturaService.java
│   │   │   └── LineaFacturaService.java
│   │   ├── repository/
│   │   │   ├── FacturaRepository.java
│   │   │   └── LineaFacturaRepository.java
│   │   ├── model/
│   │   │   ├── Factura.java
│   │   │   └── LineaFactura.java
│   │   ├── dto/
│   │   │   ├── FacturaDTO.java
│   │   │   └── LineaFacturaDTO.java
│   │   └── enums/
│   │       └── EstadoFactura.java
│   │
│   ├── cliente/                     # Módulo de Clientes
│   │   ├── controller/
│   │   │   └── ClienteController.java
│   │   ├── service/
│   │   │   └── ClienteService.java
│   │   ├── repository/
│   │   │   └── ClienteRepository.java
│   │   ├── model/
│   │   │   └── Cliente.java
│   │   └── dto/
│   │       └── ClienteDTO.java
│   │
│   ├── producto/                    # Módulo de Productos
│   │   ├── controller/
│   │   │   └── ProductoController.java
│   │   ├── service/
│   │   │   └── ProductoService.java
│   │   ├── repository/
│   │   │   └── ProductoRepository.java
│   │   ├── model/
│   │   │   └── Producto.java
│   │   └── dto/
│   │       └── ProductoDTO.java
│   │
│   ├── whatsapp/                    # Módulo de WhatsApp
│   │   ├── controller/
│   │   │   ├── WhatsAppViewController.java
│   │   │   ├── WhatsAppMensajeController.java
│   │   │   ├── WhatsAppPlantillaController.java
│   │   │   ├── WhatsAppFacturaController.java
│   │   │   └── WhatsAppWebhookController.java
│   │   ├── service/
│   │   │   ├── WhatsAppService.java
│   │   │   ├── MensajeWhatsAppService.java
│   │   │   ├── PlantillaWhatsAppService.java
│   │   │   ├── WhatsAppFacturaService.java
│   │   │   └── WebhookWhatsAppService.java
│   │   ├── repository/
│   │   │   ├── MensajeWhatsAppRepository.java
│   │   │   ├── PlantillaWhatsAppRepository.java
│   │   │   └── WebhookLogRepository.java
│   │   ├── model/
│   │   │   ├── MensajeWhatsApp.java
│   │   │   ├── PlantillaWhatsApp.java
│   │   │   └── WebhookLog.java
│   │   ├── dto/
│   │   │   ├── MensajeWhatsAppDTO.java
│   │   │   └── PlantillaWhatsAppDTO.java
│   │   └── enums/
│   │       ├── DireccionMensaje.java
│   │       └── EstadoMensaje.java
│   │
│   ├── notificacion/                # Módulo de Notificaciones
│   │   ├── controller/
│   │   │   ├── NotificacionRestController.java
│   │   │   ├── NotificacionViewController.java
│   │   │   └── NotificacionWebSocketController.java
│   │   ├── service/
│   │   │   ├── NotificacionService.java
│   │   │   ├── PlantillaNotificacionService.java
│   │   │   ├── PreferenciaNotificacionService.java
│   │   │   └── ConfiguracionNotificacionesService.java
│   │   ├── repository/
│   │   │   ├── NotificacionRepository.java
│   │   │   ├── PlantillaNotificacionRepository.java
│   │   │   └── PreferenciaNotificacionRepository.java
│   │   ├── model/
│   │   │   ├── Notificacion.java
│   │   │   ├── PlantillaNotificacion.java
│   │   │   ├── PreferenciaNotificacion.java
│   │   │   └── ConfiguracionNotificaciones.java
│   │   ├── dto/
│   │   │   └── NotificacionDTO.java
│   │   ├── enums/
│   │   │   ├── CanalNotificacion.java
│   │   │   ├── TipoNotificacion.java
│   │   │   └── EstadoNotificacion.java
│   │   └── events/
│   │       ├── NotificacionEvent.java
│   │       └── NotificacionEventListener.java
│   │
│   ├── seguridad/                   # Módulo de Seguridad (Auth + Permisos)
│   │   ├── controller/
│   │   │   ├── AuthController.java
│   │   │   ├── UsuarioController.java
│   │   │   ├── UsuarioAdminController.java
│   │   │   ├── PermisosController.java
│   │   │   ├── PermisoAdminController.java
│   │   │   ├── RolAdminController.java
│   │   │   └── PerfilController.java
│   │   ├── service/
│   │   │   ├── UsuarioService.java
│   │   │   ├── PermisoService.java
│   │   │   ├── RolService.java
│   │   │   ├── UsuarioPermisoService.java
│   │   │   └── UsuarioActividadService.java
│   │   ├── repository/
│   │   │   ├── UsuarioRepository.java
│   │   │   ├── PermisoRepository.java
│   │   │   ├── RolRepository.java
│   │   │   ├── UsuarioPermisoRepository.java
│   │   │   └── UsuarioActividadRepository.java
│   │   ├── model/
│   │   │   ├── Usuario.java
│   │   │   ├── Permiso.java
│   │   │   ├── Rol.java
│   │   │   ├── UsuarioPermiso.java
│   │   │   ├── UsuarioActividad.java
│   │   │   └── UsuarioSesion.java
│   │   ├── dto/
│   │   │   ├── UsuarioDTO.java
│   │   │   └── PermisoDTO.java
│   │   └── enums/
│   │       └── TipoPermiso.java
│   │
│   ├── configuracion/               # Módulo de Configuración
│   │   ├── controller/
│   │   │   ├── ConfiguracionController.java
│   │   │   ├── ConfiguracionEmailRestController.java
│   │   │   ├── ConfiguracionEmpresaRestController.java
│   │   │   ├── ConfiguracionFacturacionRestController.java
│   │   │   └── ParametroSistemaRestController.java
│   │   ├── service/
│   │   │   ├── ConfiguracionEmailService.java
│   │   │   ├── ConfiguracionEmpresaService.java
│   │   │   ├── ConfiguracionFacturacionService.java
│   │   │   ├── EmpresaService.java
│   │   │   └── ParametroSistemaService.java
│   │   ├── repository/
│   │   │   ├── ConfiguracionEmailRepository.java
│   │   │   ├── ConfiguracionEmpresaRepository.java
│   │   │   ├── ConfiguracionFacturacionRepository.java
│   │   │   ├── EmpresaRepository.java
│   │   │   └── ParametroSistemaRepository.java
│   │   ├── model/
│   │   │   ├── ConfiguracionEmail.java
│   │   │   ├── ConfiguracionEmpresa.java
│   │   │   ├── ConfiguracionFacturacion.java
│   │   │   ├── Empresa.java
│   │   │   └── ParametroSistema.java
│   │   └── dto/
│   │       ├── ConfiguracionEmailDTO.java
│   │       └── EmpresaDTO.java
│   │
│   ├── reportes/                    # Módulo de Reportes
│   │   ├── controller/
│   │   │   ├── ReporteController.java
│   │   │   └── DashboardController.java
│   │   ├── service/
│   │   │   ├── ReporteService.java
│   │   │   └── ExportService.java
│   │   ├── dto/
│   │   │   └── ReporteDTO.java
│   │   └── enums/
│   │       └── TipoReporte.java
│   │
│   └── presentacion/                # Módulo de Presentación (si aplica)
│       ├── service/
│       │   └── PresentacionService.java
│       └── model/
│           └── Presentacion.java
│
└── core/                            # Infraestructura técnica (opcional)
    ├── listeners/
    │   └── ApplicationStartupListener.java
    ├── schedulers/
    │   └── CleanupScheduler.java
    └── events/
        └── BaseEvent.java
```

---

