## 🔍 Análisis del Error

### Stack Trace Clave

```java
at org.springframework.cache.interceptor.AbstractCacheResolver.resolveCaches(AbstractCacheResolver.java:92)
at org.springframework.cache.interceptor.CacheAspectSupport.getCaches(CacheAspectSupport.java:317)
...
at api.astro.whats_orders_manager.services.impl.EmpresaServiceImpl$$SpringCGLIB$$0.getEmpresaPrincipal(<generated>)
at api.astro.whats_orders_manager.controllers.ConfiguracionController.index(ConfiguracionController.java:71)
```

### Servicios Afectados

1. **EmpresaServiceImpl.java** (línea 77):
   ```java
   @Cacheable(value = "empresa", key = "'principal'")
   public Empresa getEmpresaPrincipal() { ... }
   ```

2. **PlantillaWhatsAppService.java** (líneas 55, 86, 108):
   ```java
   @Cacheable(value = "plantillas", key = "#nombre")
   @Cacheable(value = "plantillas-activas")
   @Cacheable(value = "plantillas-aprobadas")
   ```

3. **ConfiguracionFacturacionServiceImpl.java** (líneas 43, 51):
   ```java
   @Cacheable(value = "configuracionFacturacion", key = "'activa'")
   ```

4. **ConfiguracionNotificacionesServiceImpl.java** (líneas 36, 44):
   ```java
   @Cacheable(value = "configuracionNotificaciones", key = "'activa'")
   ```

---

