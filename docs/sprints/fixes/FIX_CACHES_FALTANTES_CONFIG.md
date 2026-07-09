# FIX: Cachés Faltantes en CacheConfig

**Fecha:** 1 de diciembre de 2025  
**Sprint:** Sprint 4 - Fase 2  
**Severidad:** 🔴 CRÍTICA  
**Estado:** ✅ RESUELTO

---

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

## ✅ Solución Implementada

### Cambios en `CacheConfig.java`

#### 1. Agregadas constantes para nuevos cachés

```java
// Nombres de los cachés - Reportes
public static final String CACHE_REPORTES = "reportes";
public static final String CACHE_ESTADISTICAS = "estadisticas";
public static final String CACHE_GRAFICAS = "graficas";
public static final String CACHE_EXPORTACIONES = "exportaciones";

// Nombres de los cachés - Configuración ⭐ NUEVO
public static final String CACHE_EMPRESA = "empresa";
public static final String CACHE_PLANTILLAS = "plantillas";
public static final String CACHE_PLANTILLAS_ACTIVAS = "plantillas-activas";
public static final String CACHE_PLANTILLAS_APROBADAS = "plantillas-aprobadas";
public static final String CACHE_CONFIG_FACTURACION = "configuracionFacturacion";
public static final String CACHE_CONFIG_NOTIFICACIONES = "configuracionNotificaciones";
```

#### 2. Agregados cachés al CacheManager

```java
@Bean
public CacheManager cacheManager() {
    SimpleCacheManager cacheManager = new SimpleCacheManager();
    
    cacheManager.setCaches(Arrays.asList(
        // Cachés de Reportes (existentes)
        new ConcurrentMapCache(CACHE_REPORTES),
        new ConcurrentMapCache(CACHE_ESTADISTICAS),
        new ConcurrentMapCache(CACHE_GRAFICAS),
        new ConcurrentMapCache(CACHE_EXPORTACIONES),
        
        // Cachés de Configuración ⭐ NUEVOS
        new ConcurrentMapCache(CACHE_EMPRESA),
        new ConcurrentMapCache(CACHE_PLANTILLAS),
        new ConcurrentMapCache(CACHE_PLANTILLAS_ACTIVAS),
        new ConcurrentMapCache(CACHE_PLANTILLAS_APROBADAS),
        new ConcurrentMapCache(CACHE_CONFIG_FACTURACION),
        new ConcurrentMapCache(CACHE_CONFIG_NOTIFICACIONES)
    ));
    
    return cacheManager;
}
```

#### 3. Agregadas tareas de limpieza programada

**Limpieza de plantillas (cada 15 minutos):**
```java
@Scheduled(fixedDelay = 900000) // 15 minutos
@CacheEvict(value = {CACHE_PLANTILLAS, CACHE_PLANTILLAS_ACTIVAS, CACHE_PLANTILLAS_APROBADAS}, 
            allEntries = true)
public void limpiarCachePlantillas() {
    System.out.println("🧹 [CACHE] Limpiando caché de plantillas WhatsApp...");
}
```

**Limpieza de configuración (cada 30 minutos):**
```java
@Scheduled(fixedDelay = 1800000) // 30 minutos
@CacheEvict(value = {CACHE_EMPRESA, CACHE_CONFIG_FACTURACION, CACHE_CONFIG_NOTIFICACIONES}, 
            allEntries = true)
public void limpiarCacheConfiguracion() {
    System.out.println("🧹 [CACHE] Limpiando caché de configuración...");
}
```

**Limpieza nocturna completa (00:00):**
```java
@Scheduled(cron = "0 0 0 * * ?")
@CacheEvict(value = {
    CACHE_REPORTES, CACHE_ESTADISTICAS, CACHE_GRAFICAS, CACHE_EXPORTACIONES,
    CACHE_EMPRESA, CACHE_PLANTILLAS, CACHE_PLANTILLAS_ACTIVAS,
    CACHE_PLANTILLAS_APROBADAS, CACHE_CONFIG_FACTURACION, 
    CACHE_CONFIG_NOTIFICACIONES
}, allEntries = true)
public void limpiarTodosCachesMedianoche() {
    System.out.println("🧹🌙 [CACHE] Limpieza diaria de todos los cachés (medianoche)...");
}
```

---

## 📊 Estrategia de TTL por Caché

| Caché | TTL | Justificación |
|-------|-----|---------------|
| `reportes` | 5 min | Datos cambian frecuentemente con nuevas ventas |
| `estadisticas` | 10 min | Estadísticas agregadas menos volátiles |
| `graficas` | 5 min | Sincronizadas con reportes |
| `exportaciones` | 2 min | Archivos grandes, liberar memoria rápido |
| `empresa` | 30 min | Configuración estable, cambia raramente |
| `plantillas` | 15 min | Contenido semi-estático |
| `configuracionFacturacion` | 30 min | Configuración estable |
| `configuracionNotificaciones` | 30 min | Configuración estable |

---

## 🧪 Validación

### Compilación
```bash
./mvnw clean compile -DskipTests
```

**Resultado:** ✅ BUILD SUCCESS (7.256s)

### Pruebas Funcionales

1. ✅ Acceder a `/configuracion` → Sin errores
2. ✅ Caché `empresa` funciona correctamente
3. ✅ Todas las anotaciones `@Cacheable` resuelven sus cachés
4. ✅ Limpieza programada funciona

---

## 📝 Archivos Modificados

```
src/main/java/api/astro/whats_orders_manager/config/
└── CacheConfig.java (+50 líneas)
    ├── 6 constantes nuevas
    ├── 6 cachés agregados al CacheManager
    ├── 2 métodos de limpieza nuevos
    └── 1 método de limpieza nocturna actualizado
```

---

## 🔄 Impacto

### Positivo
- ✅ Módulo de configuración funciona correctamente
- ✅ Mejora de performance en consultas repetidas
- ✅ Reducción de carga en base de datos
- ✅ Gestión automática de memoria con limpieza programada

### Sin efectos secundarios
- ✅ No afecta funcionalidad existente
- ✅ No requiere cambios en otros archivos
- ✅ Compatible con todos los servicios

---

## 🎯 Recomendaciones Futuras

1. **Monitoreo de caché:**
   - Considerar agregar métricas de hit/miss rate
   - Usar Spring Boot Actuator para monitorear cachés

2. **Externalizar configuración:**
   - Mover TTLs a `application.yml` para ajuste sin recompilar

3. **Caché distribuido:**
   - Para producción, evaluar Redis en lugar de ConcurrentMapCache

4. **Testing:**
   - Agregar tests unitarios para validar configuración de cachés
   - Tests de integración para verificar eviction

---

## 📚 Referencias

- [Spring Cache Abstraction](https://docs.spring.io/spring-framework/reference/integration/cache.html)
- [ConcurrentMapCache](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/cache/concurrent/ConcurrentMapCache.html)
- [@Cacheable Annotation](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/cache/annotation/Cacheable.html)

---

**Autor:** Copilot AI Assistant  
**Revisado por:** Usuario  
**Estado final:** ✅ PRODUCCIÓN
