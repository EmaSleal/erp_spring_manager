# 📊 Guía de Optimización de Memoria - Spring Boot

**Autor:** Sistema de Optimización  
**Fecha:** 28 de diciembre de 2025  
**Aplicación:** WhatsApp Orders Manager  
**Consumo Actual:** ~1.4 GB RAM (3 instancias Java)  
**Objetivo:** Reducir a < 512 MB por instancia

---

## 📋 Índice

1. [Análisis del Consumo Actual](#análisis-del-consumo-actual)
2. [Optimizaciones Inmediatas (Sin código)](#optimizaciones-inmediatas)
3. [Optimizaciones de Configuración](#optimizaciones-de-configuración)
4. [Optimizaciones de Código](#optimizaciones-de-código)
5. [Optimizaciones Avanzadas](#optimizaciones-avanzadas)
6. [Monitoreo y Medición](#monitoreo-y-medición)

---

## 1. 📊 Análisis del Consumo Actual

### Estado Actual
```
Java(TM) Platform SE binary (1): 775.8 MB
Java(TM) Platform SE binary (2): 478.1 MB  
Java(TM) Platform SE binary (3): 297.8 MB
TOTAL: ~1.55 GB
```

### ¿Por qué 3 instancias?
- **Instancia 1 (775 MB):** Probablemente tu aplicación principal
- **Instancia 2 (478 MB):** Puede ser IDE (IntelliJ/Eclipse) ejecutando procesos
- **Instancia 3 (297 MB):** Maven/Gradle daemon o herramientas de build

### Componentes que consumen memoria en Spring Boot:
1. **JVM Heap** (40-60%): Objetos de la aplicación
2. **Metaspace** (10-20%): Clases cargadas, metadata
3. **Thread Stacks** (5-10%): Threads activos
4. **Direct Memory** (5-10%): NIO buffers
5. **Code Cache** (3-5%): Código compilado por JIT

---

## 2. ⚡ Optimizaciones Inmediatas (Sin código)

### A. Limitar Memoria JVM

#### Opción 1: Variables de Entorno
Crea un archivo `setenv.ps1` en la raíz del proyecto:

```powershell
# setenv.ps1 - Configuración de memoria JVM optimizada

# Establecer variables de entorno para Java
$env:JAVA_OPTS = @"
-Xms256m
-Xmx512m
-XX:MetaspaceSize=128m
-XX:MaxMetaspaceSize=256m
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+UseStringDeduplication
-Djava.awt.headless=true
"@

Write-Host "✓ Variables JAVA_OPTS configuradas" -ForegroundColor Green
Write-Host "  Heap inicial: 256 MB" -ForegroundColor Cyan
Write-Host "  Heap máximo: 512 MB" -ForegroundColor Cyan
Write-Host "  Metaspace máximo: 256 MB" -ForegroundColor Cyan
```

**Uso:**
```powershell
# Ejecutar antes de iniciar la aplicación
.\setenv.ps1
.\mvnw spring-boot:run
```

#### Opción 2: Modificar `start.ps1`
Si ya tienes un script de inicio, agrégale esto al principio:

```powershell
# Configuración de memoria
$env:JAVA_OPTS = "-Xms256m -Xmx512m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m"
```

#### Opción 3: Maven Plugin (Recomendado)
Modifica tu `pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <jvmArguments>
                    -Xms256m -Xmx512m
                    -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m
                    -XX:+UseG1GC
                    -XX:MaxGCPauseMillis=200
                    -XX:+UseStringDeduplication
                </jvmArguments>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### B. Usar Perfil de Producción

En `application-prod.yml`:

```yaml
spring:
  jpa:
    show-sql: false
    properties:
      hibernate:
        format_sql: false
        use_sql_comments: false
        jdbc:
          batch_size: 20  # Batch processing
  
  devtools:
    restart:
      enabled: false  # Desactivar hot reload en producción

logging:
  level:
    root: WARN
    api.astro: INFO
```

**Ejecutar con perfil:**
```powershell
.\mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

---

## 3. 🔧 Optimizaciones de Configuración

### A. Optimizar Pool de Conexiones (HikariCP)

Agregar a `application.yml`:

```yaml
spring:
  datasource:
    hikari:
      # Pool de conexiones optimizado
      maximum-pool-size: 5          # Reducir de 10 (default) a 5
      minimum-idle: 2                # Mínimo de conexiones idle
      connection-timeout: 20000      # 20 segundos
      idle-timeout: 300000           # 5 minutos
      max-lifetime: 1200000          # 20 minutos
      leak-detection-threshold: 60000 # Detectar leaks
      
      # Optimizaciones adicionales
      auto-commit: true
      
      # Propiedades MySQL optimizadas
      data-source-properties:
        cachePrepStmts: true
        prepStmtCacheSize: 250
        prepStmtCacheSqlLimit: 2048
        useServerPrepStmts: true
```

**Reducción esperada:** ~50-100 MB

### B. Optimizar Hibernate

```yaml
spring:
  jpa:
    properties:
      hibernate:
        # Reducir uso de memoria
        jdbc:
          batch_size: 20
          fetch_size: 50
        
        # Optimizar cache de segundo nivel
        cache:
          use_second_level_cache: true
          use_query_cache: true
          region:
            factory_class: org.hibernate.cache.jcache.JCacheRegionFactory
        
        # Optimizar generación de IDs
        id:
          new_generator_mappings: true
        
        # Evitar cargas innecesarias
        enable_lazy_load_no_trans: false
```

**Reducción esperada:** ~30-50 MB

### C. Optimizar Thread Pool

```yaml
server:
  tomcat:
    threads:
      max: 50          # Reducir de 200 (default)
      min-spare: 10    # Reducir de 10 (default)
    max-connections: 5000  # Reducir de 10000
    accept-count: 100
    
    # Optimizar uso de recursos
    connection-timeout: 20000
    
    # Comprimir respuestas
    compression:
      enabled: true
      mime-types: text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json
      min-response-size: 1024
```

**Reducción esperada:** ~50-80 MB

---

## 4. 💻 Optimizaciones de Código

### A. Lazy Loading en Entidades

**ANTES:**
```java
@Entity
public class Factura {
    @ManyToOne(fetch = FetchType.EAGER)  // ❌ Carga siempre
    private Cliente cliente;
    
    @OneToMany(fetch = FetchType.EAGER)  // ❌ Carga todos los items
    private List<FacturaItem> items;
}
```

**DESPUÉS:**
```java
@Entity
public class Factura {
    @ManyToOne(fetch = FetchType.LAZY)  // ✅ Carga solo cuando se necesita
    private Cliente cliente;
    
    @OneToMany(fetch = FetchType.LAZY)  // ✅ Carga bajo demanda
    private List<FacturaItem> items;
}
```

### B. Proyecciones en lugar de Entidades Completas

**ANTES:**
```java
// Carga toda la entidad (todos los campos)
List<Cliente> clientes = clienteRepository.findAll();
```

**DESPUÉS:**
```java
// Solo carga campos necesarios
public interface ClienteProjection {
    Long getId();
    String getNombre();
    String getEmail();
}

List<ClienteProjection> clientes = clienteRepository.findAllProjectedBy();
```

### C. Paginación Obligatoria

**ANTES:**
```java
// Carga TODOS los registros en memoria
@GetMapping("/facturas")
public List<Factura> getFacturas() {
    return facturaRepository.findAll();  // ❌ Puede ser 10,000 registros
}
```

**DESPUÉS:**
```java
// Carga solo una página
@GetMapping("/facturas")
public Page<FacturaDTO> getFacturas(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
) {
    Pageable pageable = PageRequest.of(page, size);
    return facturaRepository.findAll(pageable)
        .map(FacturaDTO::fromEntity);  // ✅ Solo 20 registros
}
```

### D. Cerrar Streams y Resources

```java
@Service
public class ReporteService {
    
    // ❌ ANTES: No cierra el stream
    public byte[] generarPDF(Long facturaId) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // ... generar PDF
        return baos.toByteArray();
        // Memoria no liberada!
    }
    
    // ✅ DESPUÉS: Try-with-resources
    public byte[] generarPDF(Long facturaId) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // ... generar PDF
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error generando PDF", e);
        }
    }
}
```

### E. Caché para Consultas Frecuentes

```java
@Service
public class PreferenciaService {
    
    // Cachear configuraciones que no cambian frecuentemente
    @Cacheable(value = "preferencias", key = "#usuarioId")
    public PreferenciasNotificacion getPreferencias(Long usuarioId) {
        return preferenciaRepository.findByUsuarioId(usuarioId)
            .orElseGet(() -> crearPreferenciasDefault(usuarioId));
    }
    
    @CacheEvict(value = "preferencias", key = "#usuarioId")
    public void actualizarPreferencias(Long usuarioId, PreferenciasDTO dto) {
        // ... actualizar
    }
}
```

Habilitar caché en `application.yml`:

```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=10m
```

Agregar dependencia en `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

---

## 5. 🚀 Optimizaciones Avanzadas

### A. Actuator para Monitoreo

Agregar dependencia:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Configurar en `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,heapdump,threaddump
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    export:
      simple:
        enabled: true
```

**Acceder a métricas:**
- `http://localhost:8080/actuator/metrics/jvm.memory.used`
- `http://localhost:8080/actuator/metrics/jvm.threads.live`
- `http://localhost:8080/actuator/heapdump` (descargar heap dump)

### B. GraalVM Native Image (Reducción Dramática)

Si estás dispuesto a experimentar, GraalVM reduce el consumo a ~50-100 MB:

**Configuración:**

1. Instalar GraalVM
2. Agregar plugin a `pom.xml`:

```xml
<plugin>
    <groupId>org.graalvm.buildtools</groupId>
    <artifactId>native-maven-plugin</artifactId>
</plugin>
```

3. Compilar:
```bash
.\mvnw -Pnative native:compile
```

**Pros:** Consumo mínimo, inicio instantáneo  
**Contras:** Compilación lenta, algunas limitaciones de reflexión

### C. Usar Perfiles de Memoria JVM

Crear `jvm-options.txt`:

```
# Heap Memory
-Xms256m
-Xmx512m

# Metaspace
-XX:MetaspaceSize=128m
-XX:MaxMetaspaceSize=256m

# Garbage Collector (G1GC optimizado)
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:InitiatingHeapOccupancyPercent=45
-XX:G1ReservePercent=20

# String Deduplication (ahorra ~10-15%)
-XX:+UseStringDeduplication

# Optimizaciones adicionales
-XX:+UseCompressedOops
-XX:+UseCompressedClassPointers
-XX:+OptimizeStringConcat

# Logging GC (para análisis)
-Xlog:gc*:file=logs/gc.log:time,uptime,level,tags

# Headless mode (sin GUI)
-Djava.awt.headless=true
```

**Usar:**
```powershell
.\mvnw spring-boot:run -Dspring-boot.run.jvmArguments="@jvm-options.txt"
```

---

## 6. 📈 Monitoreo y Medición

### A. Script de Monitoreo

Crear `monitor-memory.ps1`:

```powershell
# monitor-memory.ps1 - Monitorear uso de memoria de la aplicación

param(
    [int]$Intervalo = 5,  # Segundos entre mediciones
    [int]$Duracion = 60   # Duración total en segundos
)

Write-Host "=== MONITOR DE MEMORIA ===" -ForegroundColor Cyan
Write-Host "Intervalo: $Intervalo segundos" -ForegroundColor Yellow
Write-Host "Duración: $Duracion segundos" -ForegroundColor Yellow
Write-Host ""

$inicio = Get-Date
$fin = $inicio.AddSeconds($Duracion)

$resultados = @()

while ((Get-Date) -lt $fin) {
    $procesos = Get-Process -Name java -ErrorAction SilentlyContinue
    
    if ($procesos) {
        $total = 0
        Write-Host "$(Get-Date -Format 'HH:mm:ss')" -NoNewline -ForegroundColor Green
        Write-Host " | " -NoNewline
        
        foreach ($proceso in $procesos) {
            $memoriaMB = [math]::Round($proceso.WorkingSet64 / 1MB, 2)
            $total += $memoriaMB
            Write-Host "PID $($proceso.Id): $memoriaMB MB" -NoNewline -ForegroundColor Cyan
            Write-Host " | " -NoNewline
        }
        
        Write-Host "TOTAL: $([math]::Round($total, 2)) MB" -ForegroundColor Yellow
        
        $resultados += [PSCustomObject]@{
            Timestamp = Get-Date
            TotalMB = [math]::Round($total, 2)
        }
    }
    
    Start-Sleep -Seconds $Intervalo
}

# Estadísticas finales
Write-Host "`n=== ESTADÍSTICAS ===" -ForegroundColor Cyan
$promedio = ($resultados | Measure-Object -Property TotalMB -Average).Average
$maximo = ($resultados | Measure-Object -Property TotalMB -Maximum).Maximum
$minimo = ($resultados | Measure-Object -Property TotalMB -Minimum).Minimum

Write-Host "Promedio: $([math]::Round($promedio, 2)) MB" -ForegroundColor Green
Write-Host "Máximo: $([math]::Round($maximo, 2)) MB" -ForegroundColor Red
Write-Host "Mínimo: $([math]::Round($minimo, 2)) MB" -ForegroundColor Blue
```

**Uso:**
```powershell
.\monitor-memory.ps1 -Intervalo 5 -Duracion 300  # 5 min
```

### B. Analizar Heap Dump

1. **Generar heap dump:**
```powershell
# Encontrar PID del proceso Java
jps -l

# Generar dump
jmap -dump:live,format=b,file=heapdump.hprof <PID>
```

2. **Analizar con VisualVM:**
   - Descargar VisualVM: https://visualvm.github.io/
   - File → Load → Seleccionar `heapdump.hprof`
   - Revisar "Classes" y "Instances" para ver qué consume más memoria

### C. Endpoint de Métricas Personalizado

```java
@RestController
@RequestMapping("/api/admin/metrics")
public class MetricsController {
    
    @GetMapping("/memory")
    public Map<String, Object> getMemoryMetrics() {
        Runtime runtime = Runtime.getRuntime();
        
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("maxMemoryMB", maxMemory / 1024 / 1024);
        metrics.put("totalMemoryMB", totalMemory / 1024 / 1024);
        metrics.put("usedMemoryMB", usedMemory / 1024 / 1024);
        metrics.put("freeMemoryMB", freeMemory / 1024 / 1024);
        metrics.put("usagePercent", (usedMemory * 100.0) / maxMemory);
        
        return metrics;
    }
}
```

**Acceder:**
```
http://localhost:8080/api/admin/metrics/memory
```

---

## 📊 Resumen de Reducciones Esperadas

| Optimización | Reducción Estimada | Dificultad | Prioridad |
|--------------|-------------------|------------|-----------|
| Limitar heap JVM (-Xmx512m) | 200-400 MB | Fácil | ⭐⭐⭐ |
| Optimizar HikariCP | 50-100 MB | Fácil | ⭐⭐⭐ |
| Reducir thread pool | 50-80 MB | Fácil | ⭐⭐ |
| Lazy loading entidades | 30-50 MB | Media | ⭐⭐⭐ |
| Usar proyecciones | 20-40 MB | Media | ⭐⭐ |
| Caché con Caffeine | 10-30 MB | Media | ⭐⭐ |
| G1GC + StringDeduplication | 50-100 MB | Fácil | ⭐⭐⭐ |
| Paginación obligatoria | 100-200 MB | Media | ⭐⭐⭐ |
| **TOTAL ESPERADO** | **510-1000 MB** | - | - |

---

## 🎯 Plan de Acción Recomendado

### Fase 1: Inmediato (5 minutos)
1. ✅ Crear `setenv.ps1` con límites de memoria
2. ✅ Modificar `start.ps1` para usar `setenv.ps1`
3. ✅ Reiniciar aplicación y medir

### Fase 2: Corto Plazo (1 hora)
1. ✅ Configurar HikariCP en `application.yml`
2. ✅ Configurar thread pool de Tomcat
3. ✅ Habilitar compresión de respuestas
4. ✅ Agregar caché con Caffeine

### Fase 3: Mediano Plazo (1 día)
1. ✅ Revisar y cambiar relaciones a LAZY
2. ✅ Implementar proyecciones en repositorios
3. ✅ Agregar paginación a endpoints sin ella
4. ✅ Implementar try-with-resources

### Fase 4: Largo Plazo (opcional)
1. ⏳ Explorar GraalVM Native Image
2. ⏳ Implementar caché distribuido (Redis)
3. ⏳ Microservicios si la app crece mucho

---

## 📚 Referencias

- [Spring Boot Performance Tuning](https://spring.io/blog/2015/12/10/spring-boot-memory-performance)
- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
- [G1GC Tuning Guide](https://www.oracle.com/technical-resources/articles/java/g1gc.html)
- [JVM Memory Management](https://docs.oracle.com/en/java/javase/21/gctuning/)

---

**Próximo paso:** Implementar Fase 1 y medir resultados con `monitor-memory.ps1`
