package api.astro.whats_orders_manager.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Arrays;

/**
 * Configuración de caché para la aplicación.
 * Utiliza ConcurrentMapCache para almacenamiento en memoria.
 * 
 * Cachés configurados:
 * - reportes: Para resultados de reportes (TTL: 5 minutos)
 * - estadisticas: Para estadísticas del dashboard (TTL: 10 minutos)
 * - graficas: Para datos de gráficas (TTL: 5 minutos)
 * - exportaciones: Para archivos PDF/Excel generados (TTL: 2 minutos)
 * - empresa: Para datos de empresa principal (TTL: 30 minutos)
 * - plantillas: Para plantillas de WhatsApp (TTL: 15 minutos)
 * - configuracionFacturacion: Para configuración de facturación (TTL: 30 minutos)
 * - configuracionNotificaciones: Para configuración de notificaciones (TTL: 30 minutos)
 * 
 * @author Astro Dev Team
 * @version 1.0
 * @since Sprint 4 - Fase 2
 */
@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfig {

    // Nombres de los cachés - Reportes
    public static final String CACHE_REPORTES = "reportes";
    public static final String CACHE_ESTADISTICAS = "estadisticas";
    public static final String CACHE_GRAFICAS = "graficas";
    public static final String CACHE_EXPORTACIONES = "exportaciones";
    
    // Nombres de los cachés - Configuración
    public static final String CACHE_EMPRESA = "empresa";
    public static final String CACHE_PLANTILLAS = "plantillas";
    public static final String CACHE_PLANTILLAS_ACTIVAS = "plantillas-activas";
    public static final String CACHE_PLANTILLAS_APROBADAS = "plantillas-aprobadas";
    public static final String CACHE_CONFIG_FACTURACION = "configuracionFacturacion";
    public static final String CACHE_CONFIG_NOTIFICACIONES = "configuracionNotificaciones";

    /**
     * Configura el CacheManager con cachés en memoria.
     * 
     * @return CacheManager configurado
     */
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        
        // Crear cachés individuales
        cacheManager.setCaches(Arrays.asList(
            // Cachés de Reportes
            new ConcurrentMapCache(CACHE_REPORTES),
            new ConcurrentMapCache(CACHE_ESTADISTICAS),
            new ConcurrentMapCache(CACHE_GRAFICAS),
            new ConcurrentMapCache(CACHE_EXPORTACIONES),
            
            // Cachés de Configuración
            new ConcurrentMapCache(CACHE_EMPRESA),
            new ConcurrentMapCache(CACHE_PLANTILLAS),
            new ConcurrentMapCache(CACHE_PLANTILLAS_ACTIVAS),
            new ConcurrentMapCache(CACHE_PLANTILLAS_APROBADAS),
            new ConcurrentMapCache(CACHE_CONFIG_FACTURACION),
            new ConcurrentMapCache(CACHE_CONFIG_NOTIFICACIONES)
        ));
        
        return cacheManager;
    }

    /**
     * Limpia el caché de reportes cada 5 minutos.
     * Esto asegura que los datos no sean muy antiguos.
     */
    @Scheduled(fixedDelay = 300000) // 5 minutos = 300,000 ms
    @CacheEvict(value = {CACHE_REPORTES, CACHE_GRAFICAS}, allEntries = true)
    public void limpiarCacheReportes() {
        System.out.println("🧹 [CACHE] Limpiando caché de reportes y gráficas...");
    }

    /**
     * Limpia el caché de estadísticas cada 10 minutos.
     */
    @Scheduled(fixedDelay = 600000) // 10 minutos = 600,000 ms
    @CacheEvict(value = CACHE_ESTADISTICAS, allEntries = true)
    public void limpiarCacheEstadisticas() {
        System.out.println("🧹 [CACHE] Limpiando caché de estadísticas...");
    }

    /**
     * Limpia el caché de exportaciones cada 2 minutos.
     * Las exportaciones son más pesadas y ocupan más memoria.
     */
    @Scheduled(fixedDelay = 120000) // 2 minutos = 120,000 ms
    @CacheEvict(value = CACHE_EXPORTACIONES, allEntries = true)
    public void limpiarCacheExportaciones() {
        System.out.println("🧹 [CACHE] Limpiando caché de exportaciones...");
    }

    /**
     * Limpia el caché de plantillas cada 15 minutos.
     */
    @Scheduled(fixedDelay = 900000) // 15 minutos = 900,000 ms
    @CacheEvict(value = {CACHE_PLANTILLAS, CACHE_PLANTILLAS_ACTIVAS, CACHE_PLANTILLAS_APROBADAS}, allEntries = true)
    public void limpiarCachePlantillas() {
        System.out.println("🧹 [CACHE] Limpiando caché de plantillas WhatsApp...");
    }

    /**
     * Limpia el caché de configuración cada 30 minutos.
     * Las configuraciones cambian con poca frecuencia.
     */
    @Scheduled(fixedDelay = 1800000) // 30 minutos = 1,800,000 ms
    @CacheEvict(value = {CACHE_EMPRESA, CACHE_CONFIG_FACTURACION, CACHE_CONFIG_NOTIFICACIONES}, allEntries = true)
    public void limpiarCacheConfiguracion() {
        System.out.println("🧹 [CACHE] Limpiando caché de configuración (empresa, facturación, notificaciones)...");
    }

    /**
     * Limpia todos los cachés al inicio de cada día (00:00).
     * Esto asegura que los reportes diarios empiecen con datos frescos.
     */
    @Scheduled(cron = "0 0 0 * * ?") // A las 00:00 todos los días
    @CacheEvict(value = {
        CACHE_REPORTES, 
        CACHE_ESTADISTICAS, 
        CACHE_GRAFICAS, 
        CACHE_EXPORTACIONES,
        CACHE_EMPRESA,
        CACHE_PLANTILLAS,
        CACHE_PLANTILLAS_ACTIVAS,
        CACHE_PLANTILLAS_APROBADAS,
        CACHE_CONFIG_FACTURACION,
        CACHE_CONFIG_NOTIFICACIONES
    }, allEntries = true)
    public void limpiarTodosCachesMedianoche() {
        System.out.println("🧹🌙 [CACHE] Limpieza diaria de todos los cachés (medianoche)...");
    }
}
