## 📋 Descripción del Problema

Al acceder a `/configuracion`, la aplicación lanzaba un error:

```
java.lang.IllegalArgumentException: Cannot find cache named 'empresa' 
for Builder[public api.astro.whats_orders_manager.models.Empresa 
api.astro.whats_orders_manager.services.impl.EmpresaServiceImpl.getEmpresaPrincipal()] 
caches=[empresa] | key=''principal''
```

**Causa raíz:** El archivo `CacheConfig.java` solo tenía configurados 4 cachés para reportes, pero varios servicios estaban usando otros cachés no declarados:

- ❌ `empresa` (usado por `EmpresaServiceImpl`)
- ❌ `plantillas`, `plantillas-activas`, `plantillas-aprobadas` (usado por `PlantillaWhatsAppService`)
- ❌ `configuracionFacturacion` (usado por `ConfiguracionFacturacionServiceImpl`)
- ❌ `configuracionNotificaciones` (usado por `ConfiguracionNotificacionesServiceImpl`)

---

