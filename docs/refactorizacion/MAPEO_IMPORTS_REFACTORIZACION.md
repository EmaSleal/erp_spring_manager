# 🔄 GUÍA DE MAPEO DE IMPORTS - REFACTORIZACIÓN MODULAR

**Proyecto:** WhatsApp Orders Manager  
**Fecha:** 27 de diciembre de 2025  
**Propósito:** Tabla de referencia rápida para actualizar imports

---

## 📋 CÓMO USAR ESTE DOCUMENTO

1. **En IntelliJ IDEA:** `Ctrl + Shift + R` (Replace in Files)
2. **Copiar** el "ANTES" en el campo "Find"
3. **Copiar** el "DESPUÉS" en el campo "Replace with"
4. **Scope:** Whole Project
5. **Click:** Replace All

⚠️ **IMPORTANTE:** Ejecuta los reemplazos en el orden listado (de arriba hacia abajo).

---

## 📦 MÓDULO PRODUCTO

### Controllers
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.controllers.ProductoController;` | `import api.astro.whats_orders_manager.modules.producto.controller.ProductoController;` |

### Services
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.services.ProductoService;` | `import api.astro.whats_orders_manager.modules.producto.service.ProductoService;` |

### Repositories
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.repositories.ProductoRepository;` | `import api.astro.whats_orders_manager.modules.producto.repository.ProductoRepository;` |

### Models
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.models.Producto;` | `import api.astro.whats_orders_manager.modules.producto.model.Producto;` |

### DTOs
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.models.dto.ProductoDTO;` | `import api.astro.whats_orders_manager.modules.producto.dto.ProductoDTO;` |

---

## 📦 MÓDULO CLIENTE

### Controllers
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.controllers.ClienteController;` | `import api.astro.whats_orders_manager.modules.cliente.controller.ClienteController;` |

### Services
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.services.ClienteService;` | `import api.astro.whats_orders_manager.modules.cliente.service.ClienteService;` |

### Repositories
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.repositories.ClienteRepository;` | `import api.astro.whats_orders_manager.modules.cliente.repository.ClienteRepository;` |

### Models
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.models.Cliente;` | `import api.astro.whats_orders_manager.modules.cliente.model.Cliente;` |

### DTOs
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.models.dto.ClienteDTO;` | `import api.astro.whats_orders_manager.modules.cliente.dto.ClienteDTO;` |

---

## 📦 MÓDULO FACTURACIÓN

### Controllers
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.controllers.FacturaController;` | `import api.astro.whats_orders_manager.modules.facturacion.controller.FacturaController;` |
| `import api.astro.whats_orders_manager.controllers.LineaFacturaController;` | `import api.astro.whats_orders_manager.modules.facturacion.controller.LineaFacturaController;` |

### Services
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.services.FacturaService;` | `import api.astro.whats_orders_manager.modules.facturacion.service.FacturaService;` |
| `import api.astro.whats_orders_manager.services.LineaFacturaService;` | `import api.astro.whats_orders_manager.modules.facturacion.service.LineaFacturaService;` |

### Repositories
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.repositories.FacturaRepository;` | `import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;` |
| `import api.astro.whats_orders_manager.repositories.LineaFacturaRepository;` | `import api.astro.whats_orders_manager.modules.facturacion.repository.LineaFacturaRepository;` |

### Models (⚠️ MUCHOS ARCHIVOS USAN ESTOS)
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.models.Factura;` | `import api.astro.whats_orders_manager.modules.facturacion.model.Factura;` |
| `import api.astro.whats_orders_manager.models.LineaFactura;` | `import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;` |

### DTOs
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.models.dto.FacturaDTO;` | `import api.astro.whats_orders_manager.modules.facturacion.dto.FacturaDTO;` |
| `import api.astro.whats_orders_manager.models.dto.LineaFacturaDTO;` | `import api.astro.whats_orders_manager.modules.facturacion.dto.LineaFacturaDTO;` |

### Enums
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.enums.EstadoFactura;` | `import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoFactura;` |

---

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

## 📦 MÓDULO SEGURIDAD

### Controllers
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.controllers.AuthController;` | `import api.astro.whats_orders_manager.modules.seguridad.controller.AuthController;` |
| `import api.astro.whats_orders_manager.controllers.UsuarioController;` | `import api.astro.whats_orders_manager.modules.seguridad.controller.UsuarioController;` |
| `import api.astro.whats_orders_manager.controllers.UsuarioAdminController;` | `import api.astro.whats_orders_manager.modules.seguridad.controller.UsuarioAdminController;` |
| `import api.astro.whats_orders_manager.controllers.PermisosController;` | `import api.astro.whats_orders_manager.modules.seguridad.controller.PermisosController;` |
| `import api.astro.whats_orders_manager.controllers.PermisoAdminController;` | `import api.astro.whats_orders_manager.modules.seguridad.controller.PermisoAdminController;` |
| `import api.astro.whats_orders_manager.controllers.RolAdminController;` | `import api.astro.whats_orders_manager.modules.seguridad.controller.RolAdminController;` |
| `import api.astro.whats_orders_manager.controllers.PerfilController;` | `import api.astro.whats_orders_manager.modules.seguridad.controller.PerfilController;` |

### Services
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.services.UsuarioService;` | `import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;` |
| `import api.astro.whats_orders_manager.services.PermisoService;` | `import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;` |
| `import api.astro.whats_orders_manager.services.RolService;` | `import api.astro.whats_orders_manager.modules.seguridad.service.RolService;` |
| `import api.astro.whats_orders_manager.services.UsuarioPermisoService;` | `import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioPermisoService;` |
| `import api.astro.whats_orders_manager.services.UsuarioActividadService;` | `import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioActividadService;` |

### Repositories
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.repositories.UsuarioRepository;` | `import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioRepository;` |
| `import api.astro.whats_orders_manager.repositories.PermisoRepository;` | `import api.astro.whats_orders_manager.modules.seguridad.repository.PermisoRepository;` |
| `import api.astro.whats_orders_manager.repositories.RolRepository;` | `import api.astro.whats_orders_manager.modules.seguridad.repository.RolRepository;` |
| `import api.astro.whats_orders_manager.repositories.UsuarioPermisoRepository;` | `import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioPermisoRepository;` |
| `import api.astro.whats_orders_manager.repositories.UsuarioActividadRepository;` | `import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioActividadRepository;` |

### Models (⚠️ MUCHOS ARCHIVOS USAN ESTOS)
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.models.Usuario;` | `import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;` |
| `import api.astro.whats_orders_manager.models.Permiso;` | `import api.astro.whats_orders_manager.modules.seguridad.model.Permiso;` |
| `import api.astro.whats_orders_manager.models.Rol;` | `import api.astro.whats_orders_manager.modules.seguridad.model.Rol;` |
| `import api.astro.whats_orders_manager.models.UsuarioPermiso;` | `import api.astro.whats_orders_manager.modules.seguridad.model.UsuarioPermiso;` |
| `import api.astro.whats_orders_manager.models.UsuarioActividad;` | `import api.astro.whats_orders_manager.modules.seguridad.model.UsuarioActividad;` |
| `import api.astro.whats_orders_manager.models.UsuarioSesion;` | `import api.astro.whats_orders_manager.modules.seguridad.model.UsuarioSesion;` |

### DTOs
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.models.dto.UsuarioDTO;` | `import api.astro.whats_orders_manager.modules.seguridad.dto.UsuarioDTO;` |
| `import api.astro.whats_orders_manager.models.dto.PermisoDTO;` | `import api.astro.whats_orders_manager.modules.seguridad.dto.PermisoDTO;` |

### Enums
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.enums.TipoPermiso;` | `import api.astro.whats_orders_manager.modules.seguridad.enums.TipoPermiso;` |

---

## 📦 MÓDULO CONFIGURACIÓN

### Controllers
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.controllers.ConfiguracionController;` | `import api.astro.whats_orders_manager.modules.configuracion.controller.ConfiguracionController;` |
| `import api.astro.whats_orders_manager.controllers.ConfiguracionEmailRestController;` | `import api.astro.whats_orders_manager.modules.configuracion.controller.ConfiguracionEmailRestController;` |
| `import api.astro.whats_orders_manager.controllers.ConfiguracionEmpresaRestController;` | `import api.astro.whats_orders_manager.modules.configuracion.controller.ConfiguracionEmpresaRestController;` |
| `import api.astro.whats_orders_manager.controllers.ConfiguracionFacturacionRestController;` | `import api.astro.whats_orders_manager.modules.configuracion.controller.ConfiguracionFacturacionRestController;` |
| `import api.astro.whats_orders_manager.controllers.ParametroSistemaRestController;` | `import api.astro.whats_orders_manager.modules.configuracion.controller.ParametroSistemaRestController;` |

### Services
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.services.ConfiguracionEmailService;` | `import api.astro.whats_orders_manager.modules.configuracion.service.ConfiguracionEmailService;` |
| `import api.astro.whats_orders_manager.services.ConfiguracionEmpresaService;` | `import api.astro.whats_orders_manager.modules.configuracion.service.ConfiguracionEmpresaService;` |
| `import api.astro.whats_orders_manager.services.ConfiguracionFacturacionService;` | `import api.astro.whats_orders_manager.modules.configuracion.service.ConfiguracionFacturacionService;` |
| `import api.astro.whats_orders_manager.services.EmpresaService;` | `import api.astro.whats_orders_manager.modules.configuracion.service.EmpresaService;` |
| `import api.astro.whats_orders_manager.services.ParametroSistemaService;` | `import api.astro.whats_orders_manager.modules.configuracion.service.ParametroSistemaService;` |

### Repositories
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.repositories.ConfiguracionEmailRepository;` | `import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmailRepository;` |
| `import api.astro.whats_orders_manager.repositories.ConfiguracionEmpresaRepository;` | `import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmpresaRepository;` |
| `import api.astro.whats_orders_manager.repositories.ConfiguracionFacturacionRepository;` | `import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionFacturacionRepository;` |
| `import api.astro.whats_orders_manager.repositories.EmpresaRepository;` | `import api.astro.whats_orders_manager.modules.configuracion.repository.EmpresaRepository;` |
| `import api.astro.whats_orders_manager.repositories.ParametroSistemaRepository;` | `import api.astro.whats_orders_manager.modules.configuracion.repository.ParametroSistemaRepository;` |

### Models
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.models.ConfiguracionEmail;` | `import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmail;` |
| `import api.astro.whats_orders_manager.models.ConfiguracionEmpresa;` | `import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;` |
| `import api.astro.whats_orders_manager.models.ConfiguracionFacturacion;` | `import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionFacturacion;` |
| `import api.astro.whats_orders_manager.models.Empresa;` | `import api.astro.whats_orders_manager.modules.configuracion.model.Empresa;` |
| `import api.astro.whats_orders_manager.models.ParametroSistema;` | `import api.astro.whats_orders_manager.modules.configuracion.model.ParametroSistema;` |

### DTOs
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.models.dto.ConfiguracionEmailDTO;` | `import api.astro.whats_orders_manager.modules.configuracion.dto.ConfiguracionEmailDTO;` |
| `import api.astro.whats_orders_manager.models.dto.EmpresaDTO;` | `import api.astro.whats_orders_manager.modules.configuracion.dto.EmpresaDTO;` |

---

## 📦 MÓDULO REPORTES

### Controllers
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.controllers.ReporteController;` | `import api.astro.whats_orders_manager.modules.reportes.controller.ReporteController;` |
| `import api.astro.whats_orders_manager.controllers.DashboardController;` | `import api.astro.whats_orders_manager.modules.reportes.controller.DashboardController;` |

### Services
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.services.ReporteService;` | `import api.astro.whats_orders_manager.modules.reportes.service.ReporteService;` |
| `import api.astro.whats_orders_manager.services.ExportService;` | `import api.astro.whats_orders_manager.modules.reportes.service.ExportService;` |

### DTOs
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.models.dto.ReporteDTO;` | `import api.astro.whats_orders_manager.modules.reportes.dto.ReporteDTO;` |

---

## 📦 CÓDIGO COMPARTIDO (shared/)

### Config
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.config.SecurityConfig;` | `import api.astro.whats_orders_manager.shared.config.SecurityConfig;` |
| `import api.astro.whats_orders_manager.config.WebConfig;` | `import api.astro.whats_orders_manager.shared.config.WebConfig;` |
| `import api.astro.whats_orders_manager.config.ThymeleafConfig;` | `import api.astro.whats_orders_manager.shared.config.ThymeleafConfig;` |
| `import api.astro.whats_orders_manager.config.DatabaseConfig;` | `import api.astro.whats_orders_manager.shared.config.DatabaseConfig;` |

### Exception
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.exception.GlobalExceptionHandler;` | `import api.astro.whats_orders_manager.shared.exception.GlobalExceptionHandler;` |
| `import api.astro.whats_orders_manager.exception.ResourceNotFoundException;` | `import api.astro.whats_orders_manager.shared.exception.ResourceNotFoundException;` |
| `import api.astro.whats_orders_manager.exception.BusinessException;` | `import api.astro.whats_orders_manager.shared.exception.BusinessException;` |

### Util
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.util.DateUtil;` | `import api.astro.whats_orders_manager.shared.util.DateUtil;` |
| `import api.astro.whats_orders_manager.util.FileUtil;` | `import api.astro.whats_orders_manager.shared.util.FileUtil;` |
| `import api.astro.whats_orders_manager.util.ValidationUtil;` | `import api.astro.whats_orders_manager.shared.util.ValidationUtil;` |

### DTOs Compartidos
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.models.dto.ApiResponse;` | `import api.astro.whats_orders_manager.shared.dto.ApiResponse;` |

---

## 📦 CORE (listeners/schedulers)

### Listeners
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.listeners.ApplicationStartupListener;` | `import api.astro.whats_orders_manager.core.listeners.ApplicationStartupListener;` |

### Schedulers
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.schedulers.CleanupScheduler;` | `import api.astro.whats_orders_manager.core.schedulers.CleanupScheduler;` |

### Events
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.events.BaseEvent;` | `import api.astro.whats_orders_manager.core.events.BaseEvent;` |

---

## 🔧 TIPS DE USO

### Para IntelliJ IDEA

1. **Find & Replace in Files:**
   ```
   Ctrl + Shift + R
   ```

2. **Scope recomendado:**
   ```
   Whole Project
   ```

3. **File mask:**
   ```
   *.java
   ```

4. **Opciones:**
   - ✅ Case sensitive
   - ✅ Match case
   - ❌ Regex (a menos que lo necesites)

### Orden de Ejecución

1. ✅ Ejecutar primero los **Models** (Factura, Usuario, Cliente, etc.)
2. ✅ Luego **Repositories**
3. ✅ Luego **Services**
4. ✅ Luego **Controllers**
5. ✅ Finalmente **DTOs y Enums**

### Verificar Cambios

Después de cada grupo de reemplazos:
```bash
mvn clean compile
```

Si hay errores, usa IntelliJ:
```
Ctrl + Shift + F → Buscar el import antiguo
Alt + Enter → Import class
```

---

## ⚠️ PRECAUCIONES

1. **Hacer backup antes de empezar**
2. **Ejecutar reemplazos de uno en uno** (no todos a la vez)
3. **Compilar después de cada grupo de cambios**
4. **Verificar que no se rompan tests**
5. **Hacer commits frecuentes**

---

## 📊 ESTADÍSTICAS ESPERADAS

Después de completar todos los reemplazos:

- **Total de archivos modificados:** ~100+
- **Total de imports actualizados:** ~300-500
- **Tiempo estimado:** 2-3 horas (para todos los reemplazos)

---

**Última actualización:** 27 de diciembre de 2025  
**Versión:** 1.0
