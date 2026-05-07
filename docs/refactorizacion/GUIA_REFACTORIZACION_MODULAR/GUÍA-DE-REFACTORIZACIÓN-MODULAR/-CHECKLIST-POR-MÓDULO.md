## 🔍 CHECKLIST POR MÓDULO

### Módulo Producto ✅
```
□ ProductoController.java → modules/producto/controller/
□ ProductoService.java → modules/producto/service/
□ ProductoRepository.java → modules/producto/repository/
□ Producto.java → modules/producto/model/
□ ProductoDTO.java → modules/producto/dto/
□ Actualizar packages
□ Actualizar imports
□ Compilar
□ Tests
□ Commit
```

### Módulo Cliente ✅
```
□ ClienteController.java → modules/cliente/controller/
□ ClienteService.java → modules/cliente/service/
□ ClienteRepository.java → modules/cliente/repository/
□ Cliente.java → modules/cliente/model/
□ ClienteDTO.java → modules/cliente/dto/
□ Actualizar packages
□ Actualizar imports
□ Compilar
□ Tests
□ Commit
```

### Módulo Reportes ✅
```
□ ReporteController.java → modules/reportes/controller/
□ DashboardController.java → modules/reportes/controller/
□ ReporteService.java → modules/reportes/service/
□ ExportService.java → modules/reportes/service/
□ ReporteDTO.java → modules/reportes/dto/
□ Actualizar packages
□ Actualizar imports
□ Compilar
□ Tests
□ Commit
```

### Módulo Configuración ✅
```
□ ConfiguracionController.java
□ ConfiguracionEmailRestController.java
□ ConfiguracionEmpresaRestController.java
□ ConfiguracionFacturacionRestController.java
□ ParametroSistemaRestController.java
□ 5 Services relacionados
□ 5 Repositories relacionados
□ 5 Models relacionados
□ DTOs relacionados
□ Actualizar packages
□ Actualizar imports
□ Compilar
□ Tests
□ Commit
```

### Módulo Facturación ✅
```
□ FacturaController.java
□ LineaFacturaController.java
□ FacturaService.java
□ LineaFacturaService.java
□ FacturaRepository.java
□ LineaFacturaRepository.java
□ Factura.java (MODEL - MOVER PRIMERO)
□ LineaFactura.java (MODEL - MOVER PRIMERO)
□ FacturaDTO.java
□ LineaFacturaDTO.java
□ EstadoFactura.java (enum)
□ Actualizar packages
□ Actualizar imports en TODO el proyecto (muchos archivos usan Factura)
□ Compilar
□ Tests
□ Commit
```

### Módulo WhatsApp ✅
```
□ 5 Controllers (WhatsApp*)
□ 5 Services (WhatsApp*, MensajeWhatsApp*, PlantillaWhatsApp*, Webhook*)
□ 3 Repositories
□ 3 Models (MensajeWhatsApp, PlantillaWhatsApp, WebhookLog)
□ 2 DTOs
□ 2 Enums (si existen)
□ Actualizar packages
□ Actualizar imports
□ Compilar
□ Tests
□ Commit
```

### Módulo Notificación ✅
```
□ 3 Controllers (Notificacion*)
□ 4 Services
□ 4 Repositories
□ 4 Models
□ DTOs relacionados
□ 3 Enums (CanalNotificacion, TipoNotificacion, EstadoNotificacion)
□ Events (si existen)
□ Actualizar packages
□ Actualizar imports
□ Compilar
□ Tests
□ Commit
```

### Módulo Seguridad ✅ (CRÍTICO)
```
□ 8 Controllers (Auth, Usuario*, Permiso*, Rol*, Perfil)
□ 5 Services
□ 5 Repositories
□ 6 Models (Usuario, Permiso, Rol, UsuarioPermiso, UsuarioActividad, UsuarioSesion)
□ DTOs relacionados
□ Enums (si existen)
□ Actualizar packages
□ Actualizar imports en TODO el proyecto
□ Compilar después de cada grupo
□ Tests críticos (Auth, Login, Permisos)
□ Verificar login en navegador
□ Commit
```

### Código Compartido (shared/) ✅
```
□ Config → shared/config/
□ Exception handlers → shared/exception/
□ Utils → shared/util/
□ DTOs compartidos → shared/dto/
□ Actualizar packages
□ Actualizar imports
□ Compilar
□ Tests
□ Commit
```

### Core (listeners/schedulers) ✅
```
□ Listeners → core/listeners/
□ Schedulers → core/schedulers/
□ Events base → core/events/
□ Actualizar packages
□ Actualizar imports
□ Compilar
□ Tests
□ Commit
```

### Limpieza Final ✅
```
□ Eliminar carpetas vacías antiguas
□ Optimize imports (Ctrl + Alt + O)
□ Reformat code (Ctrl + Alt + L)
□ mvn clean compile
□ mvn test
□ mvn package
□ mvn spring-boot:run
□ Verificar endpoints
□ Commit final
```

---

