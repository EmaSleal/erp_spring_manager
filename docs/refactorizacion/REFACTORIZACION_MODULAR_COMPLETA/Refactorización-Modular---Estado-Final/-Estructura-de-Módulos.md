## 📁 Estructura de Módulos

### 1. **Módulo Producto** (6 archivos)
**Path:** `modules/producto/`

```
producto/
├── controller/
│   └── ProductoController.java
├── model/
│   ├── Producto.java
│   ├── CategoriaProducto.java
│   └── ProductoRecord.java
├── repository/
│   └── ProductoRepository.java
└── service/
    ├── ProductoService.java
    └── impl/
        └── ProductoServiceImpl.java
```

**Responsabilidad:** Gestión de productos, categorías y catálogo.

---

### 2. **Módulo Cliente** (5 archivos)
**Path:** `modules/cliente/`

```
cliente/
├── controller/
│   └── ClienteController.java
├── model/
│   └── Cliente.java
├── repository/
│   └── ClienteRepository.java
└── service/
    ├── ClienteService.java
    └── impl/
        └── ClienteServiceImpl.java
```

**Responsabilidad:** Gestión de clientes y contactos.

---

### 3. **Módulo Facturación** (19 archivos)
**Path:** `modules/facturacion/`

```
facturacion/
├── controller/
│   ├── FacturaController.java
│   ├── LineaFacturaController.java
│   └── ConfiguracionFacturacionController.java
├── model/
│   ├── Factura.java
│   ├── LineaFactura.java
│   ├── LineaFacturaR.java (record)
│   └── ConfiguracionFacturacion.java
├── repository/
│   ├── FacturaRepository.java
│   ├── LineaFacturaRepository.java
│   └── ConfiguracionFacturacionRepository.java
├── service/
│   ├── FacturaService.java
│   ├── LineaFacturaService.java
│   ├── ConfiguracionFacturacionService.java
│   └── impl/
│       ├── FacturaServiceImpl.java
│       ├── LineaFacturaServiceImpl.java
│       └── ConfiguracionFacturacionServiceImpl.java
├── scheduler/
│   └── RecordatorioPagoScheduler.java
└── enums/
    └── InvoiceType.java
```

**Responsabilidad:** Facturación, líneas de factura, configuración, recordatorios de pago.

---

### 4. **Módulo Reportes** (6 archivos)
**Path:** `modules/reportes/`

```
reportes/
├── controller/
│   ├── ReporteController.java
│   └── DashboardController.java
├── service/
│   ├── ReporteService.java
│   ├── ExportService.java
│   └── impl/
│       ├── ReporteServiceImpl.java
│       └── ExportServiceImpl.java
```

**Responsabilidad:** Generación de reportes, dashboard, exportación (PDF/Excel).

---

### 5. **Módulo Configuración** (20 archivos)
**Path:** `modules/configuracion/`

```
configuracion/
├── controller/
│   ├── ConfiguracionController.java
│   └── ParametroSistemaRestController.java
├── model/
│   ├── Empresa.java
│   ├── ParametroSistema.java
│   └── Presentacion.java
├── repository/
│   ├── EmpresaRepository.java
│   ├── ParametroSistemaRepository.java
│   └── PresentacionRepository.java
├── service/
│   ├── EmpresaService.java
│   ├── ParametroSistemaService.java
│   ├── PresentacionService.java
│   └── impl/
│       ├── EmpresaServiceImpl.java
│       ├── ParametroSistemaServiceImpl.java
│       └── PresentacionServiceImpl.java
├── dto/
│   └── ParametroSistemaDTO.java
├── enums/
│   ├── CategoriaParametro.java
│   └── TipoDatoParametro.java
└── event/
    └── ConfiguracionActualizadaEvent.java
```

**Responsabilidad:** Configuración de empresa, parámetros del sistema, presentación, eventos de configuración.

**Nota especial:** Se corrigió ubicación incorrecta de `CategoriaParametro` (estaba en `models/dto/` en lugar de `enums/`).

---

### 6. **Módulo WhatsApp** (26 archivos)
**Path:** `modules/whatsapp/`

```
whatsapp/
├── controller/
│   ├── WebhookWhatsAppController.java
│   ├── WhatsAppController.java
│   ├── PlantillaWhatsAppController.java
│   ├── MensajeWhatsAppController.java
│   └── WebhookLogController.java
├── model/
│   ├── PlantillaWhatsApp.java
│   ├── MensajeWhatsApp.java
│   └── WebhookLog.java
├── repository/
│   ├── PlantillaWhatsAppRepository.java
│   ├── MensajeWhatsAppRepository.java
│   └── WebhookLogRepository.java
├── service/
│   ├── PlantillaWhatsAppService.java
│   ├── MensajeWhatsAppService.java
│   ├── WebhookWhatsAppService.java
│   ├── WhatsAppIntegrationService.java
│   ├── WebhookLogService.java
│   └── impl/
│       ├── PlantillaWhatsAppServiceImpl.java
│       ├── MensajeWhatsAppServiceImpl.java
│       ├── WebhookWhatsAppServiceImpl.java
│       ├── WhatsAppIntegrationServiceImpl.java
│       └── WebhookLogServiceImpl.java
└── dto/
    ├── EnviarMensajeRequest.java
    ├── EnviarMensajeResponse.java
    ├── MetaApiErrorResponse.java
    ├── MetaWebhookRequest.java
    └── WebhookValidationDTO.java
```

**Responsabilidad:** Integración con WhatsApp Business API, gestión de plantillas, mensajes, webhooks y logs.

**Ecosistema WebhookLog:** 5 archivos migrados como unidad cohesiva.

---

### 7. **Módulo Notificación** (19 archivos)
**Path:** `modules/notificacion/`

```
notificacion/
├── controller/
│   ├── NotificacionRestController.java
│   ├── NotificacionViewController.java
│   └── NotificacionWebSocketController.java
├── model/
│   ├── Notificacion.java
│   ├── PreferenciaNotificacion.java
│   ├── ConfiguracionNotificaciones.java
│   └── AuditoriaNotificacion.java
├── repository/
│   ├── NotificacionRepository.java
│   ├── PreferenciaNotificacionRepository.java
│   ├── ConfiguracionNotificacionesRepository.java
│   └── AuditoriaNotificacionRepository.java
├── service/
│   ├── NotificacionService.java
│   └── impl/
│       └── NotificacionServiceImpl.java
├── dto/
│   └── NotificacionDTO.java
├── event/
│   └── NotificacionEvent.java
└── listener/
    └── NotificacionEventListener.java
```

**Responsabilidad:** Sistema de notificaciones, preferencias, configuración, auditoría, eventos y WebSockets.

---

### 8. **Módulo Seguridad** (36 archivos)
**Path:** `modules/seguridad/`

```
seguridad/
├── controller/
│   ├── UsuarioController.java
│   ├── RolController.java
│   ├── PermisoController.java
│   └── PerfilController.java
├── model/
│   ├── Usuario.java
│   ├── Rol.java
│   ├── Permiso.java
│   ├── PermisoPersonalizado.java
│   ├── RolPermisoPersonalizado.java
│   ├── PermisosUsuario.java
│   ├── PermisoModuloDinamico.java
│   ├── Modulo.java
│   ├── RolPermisoModuloDinamico.java
│   └── PermisoRolUsuario.java
├── repository/
│   ├── UsuarioRepository.java
│   ├── RolRepository.java
│   ├── PermisoRepository.java
│   ├── PermisoPersonalizadoRepository.java
│   ├── RolPermisoPersonalizadoRepository.java
│   ├── PermisosUsuarioRepository.java
│   ├── PermisoModuloDinamicoRepository.java
│   ├── ModuloRepository.java
│   ├── RolPermisoModuloDinamicoRepository.java
│   └── PermisoRolUsuarioRepository.java
├── service/
│   ├── UsuarioService.java
│   ├── RolService.java
│   ├── PermisoService.java
│   ├── PermisoPersonalizadoService.java
│   ├── PermisosUsuarioService.java
│   └── impl/
│       ├── UsuarioServiceImpl.java
│       ├── RolServiceImpl.java
│       ├── PermisoServiceImpl.java
│       ├── PermisoPersonalizadoServiceImpl.java
│       ├── PermisosUsuarioServiceImpl.java
│       └── UserDetailsServiceImpl.java
├── dto/
│   ├── ModuloDTO.java
│   ├── UsuarioAdminDTO.java
│   └── EstadisticasUsuariosDTO.java
└── config/
    └── SecurityConfig.java
```

**Responsabilidad:** Autenticación, autorización, gestión de usuarios, roles, permisos (estáticos, personalizados y dinámicos), módulos.

**Módulo más grande:** 36 archivos (22% del total).

---

### 9. **Módulo Shared** (6 archivos) - NUEVO
**Path:** `modules/shared/`

```
shared/
├── controller/
│   ├── HomeController.java
│   └── CustomErrorController.java
├── service/
│   ├── EmailService.java
│   └── impl/
│       └── EmailServiceImpl.java
└── dto/
    ├── ResponseDTO.java
    └── PaginacionDTO.java
```

**Responsabilidad:** Código compartido entre múltiples módulos (controladores generales, servicios de email, DTOs genéricos).

---

