# ⚡ FASE 3: Optimizaciones de Rendimiento

**Sprint:** 9  
**Fase:** 3 de 5  
**Duración estimada:** 4-6 días  
**Prioridad:** ⭐⭐ ALTA  
**Estado:** 📋 PENDIENTE (0/28 tareas)

---

## 📋 OBJETIVO DE LA FASE

Optimizar rendimiento para tiempos de carga rápidos:
- **Lighthouse score > 90**
- **FCP < 1.8s** (First Contentful Paint)
- **LCP < 2.5s** (Largest Contentful Paint)
- **TTI < 3.8s** (Time to Interactive)
- **CLS < 0.1** (Cumulative Layout Shift)
- Lazy loading de recursos
- Code splitting
- Compresión Gzip/Brotli
- Optimización de queries SQL

---

## 📊 PROGRESO GENERAL

```
Progreso: [0/28] ░░░░░░░░░░░░░░░░░░░░ 0%

├─ 1. Optimizaciones Frontend       [0/10] ░░░░░░░░░░ 0%
├─ 2. Optimizaciones Backend        [0/8]  ░░░░░░░░░░ 0%
├─ 3. Optimizaciones de BD          [0/6]  ░░░░░░░░░░ 0%
└─ 4. Medición y Monitoreo          [0/4]  ░░░░░░░░░░ 0%
```

---

## 📦 1. OPTIMIZACIONES FRONTEND (10 tareas)

### 1.1. Descripción

Optimizar carga de recursos frontend:
- Lazy loading de imágenes
- Code splitting en JavaScript
- Minificación de CSS/JS
- Preload de recursos críticos
- Optimización de fuentes web

#### Tareas:

- [ ] **1.1** Implementar lazy loading de imágenes

```html
<!-- Usar loading="lazy" en imágenes -->
<img src="/images/producto.jpg" 
     alt="Producto" 
     loading="lazy"
     width="300" 
     height="200">

<!-- Placeholder con blur-up -->
<div class="image-wrapper">
    <img src="/images/producto-thumb.jpg" 
         data-src="/images/producto.jpg" 
         alt="Producto" 
         class="lazy blur-up"
         loading="lazy">
</div>
```

```javascript
// Intersection Observer para lazy loading avanzado
document.addEventListener('DOMContentLoaded', () => {
    const lazyImages = document.querySelectorAll('img.lazy');
    
    const imageObserver = new IntersectionObserver((entries, observer) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const img = entry.target;
                img.src = img.dataset.src;
                img.classList.remove('lazy');
                img.classList.add('loaded');
                observer.unobserve(img);
            }
        });
    });
    
    lazyImages.forEach(img => imageObserver.observe(img));
});
```

```css
/* Efecto blur-up */
img.lazy {
    filter: blur(10px);
    transition: filter 0.3s;
}

img.loaded {
    filter: blur(0);
}
```

- [ ] **1.2** Optimizar imágenes a formato WebP

```java
@Service
public class ImageOptimizationService {
    
    /**
     * Convertir imagen a WebP
     */
    public byte[] convertToWebP(byte[] imageBytes) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "webp", baos);
        
        return baos.toByteArray();
    }
    
    /**
     * Redimensionar y optimizar imagen
     */
    public byte[] optimizeImage(byte[] imageBytes, int maxWidth, int maxHeight) 
            throws IOException {
        
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
        
        int width = original.getWidth();
        int height = original.getHeight();
        
        // Calcular nuevas dimensiones manteniendo aspect ratio
        if (width > maxWidth || height > maxHeight) {
            double ratio = Math.min(
                (double) maxWidth / width,
                (double) maxHeight / height
            );
            width = (int) (width * ratio);
            height = (int) (height * ratio);
        }
        
        // Redimensionar
        Image scaled = original.getScaledInstance(
            width, height, Image.SCALE_SMOOTH
        );
        
        BufferedImage optimized = new BufferedImage(
            width, height, BufferedImage.TYPE_INT_RGB
        );
        
        Graphics2D g2d = optimized.createGraphics();
        g2d.drawImage(scaled, 0, 0, null);
        g2d.dispose();
        
        // Convertir a bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(optimized, "jpg", baos);
        
        return baos.toByteArray();
    }
}
```

- [ ] **1.3** Minificar CSS y JavaScript

```xml
<!-- pom.xml - Agregar plugin de minificación -->
<plugin>
    <groupId>com.github.eirslett</groupId>
    <artifactId>frontend-maven-plugin</artifactId>
    <version>1.12.1</version>
    <configuration>
        <nodeVersion>v18.17.0</nodeVersion>
        <npmVersion>9.6.7</npmVersion>
    </configuration>
    <executions>
        <execution>
            <id>install node and npm</id>
            <goals>
                <goal>install-node-and-npm</goal>
            </goals>
        </execution>
        <execution>
            <id>npm install</id>
            <goals>
                <goal>npm</goal>
            </goals>
            <configuration>
                <arguments>install</arguments>
            </configuration>
        </execution>
        <execution>
            <id>npm run build</id>
            <goals>
                <goal>npm</goal>
            </goals>
            <configuration>
                <arguments>run build</arguments>
            </configuration>
        </execution>
    </executions>
</plugin>
```

```json
// package.json
{
  "name": "wom-erp-frontend",
  "version": "1.0.0",
  "scripts": {
    "build": "npm run build:css && npm run build:js",
    "build:css": "postcss src/main/resources/static/css/*.css --dir target/classes/static/css --use cssnano",
    "build:js": "terser src/main/resources/static/js/*.js --compress --mangle -o target/classes/static/js/bundle.min.js"
  },
  "devDependencies": {
    "cssnano": "^6.0.1",
    "postcss": "^8.4.31",
    "postcss-cli": "^10.1.0",
    "terser": "^5.19.4"
  }
}
```

- [ ] **1.4** Implementar code splitting

```javascript
// Cargar módulos bajo demanda
async function loadModule(moduleName) {
    try {
        const module = await import(`/js/modules/${moduleName}.js`);
        return module;
    } catch (error) {
        console.error(`Error cargando módulo ${moduleName}:`, error);
    }
}

// Ejemplo: Cargar chart.js solo en páginas de reportes
if (document.getElementById('chart-container')) {
    loadModule('chart-handler').then(module => {
        module.initCharts();
    });
}
```

- [ ] **1.5** Preload de recursos críticos

```html
<!-- Preload de CSS crítico -->
<link rel="preload" href="/css/main.css" as="style">
<link rel="preload" href="/css/bootstrap.min.css" as="style">

<!-- Preload de fuentes web -->
<link rel="preload" href="/fonts/Inter-Regular.woff2" as="font" type="font/woff2" crossorigin>

<!-- Preload de JavaScript crítico -->
<link rel="preload" href="/js/bootstrap.bundle.min.js" as="script">

<!-- Prefetch de rutas comunes -->
<link rel="prefetch" href="/facturas">
<link rel="prefetch" href="/clientes">
```

- [ ] **1.6** Optimizar carga de fuentes web

```css
/* Usar font-display: swap para evitar FOIT */
@font-face {
    font-family: 'Inter';
    src: url('/fonts/Inter-Regular.woff2') format('woff2');
    font-weight: 400;
    font-style: normal;
    font-display: swap;
}

@font-face {
    font-family: 'Inter';
    src: url('/fonts/Inter-Bold.woff2') format('woff2');
    font-weight: 700;
    font-style: normal;
    font-display: swap;
}

/* Subsetting de fuentes (solo caracteres usados) */
/* Usar herramienta: https://everythingfonts.com/subsetter */
```

- [ ] **1.7** Reducir bundle size

```javascript
// Usar imports específicos en lugar de importar toda la librería
// ❌ MAL
import _ from 'lodash';

// ✅ BIEN
import debounce from 'lodash/debounce';
import throttle from 'lodash/throttle';

// Tree shaking en build
// Eliminar código no usado automáticamente
```

- [ ] **1.8** Implementar critical CSS inline

```html
<!DOCTYPE html>
<html>
<head>
    <!-- CSS crítico inline (above the fold) -->
    <style>
        /* Estilos críticos para primera carga */
        body { margin: 0; font-family: -apple-system, sans-serif; }
        .navbar { background: #fff; height: 60px; }
        .container { max-width: 1200px; margin: 0 auto; }
    </style>
    
    <!-- CSS completo cargado async -->
    <link rel="preload" href="/css/main.css" as="style" onload="this.onload=null;this.rel='stylesheet'">
    <noscript><link rel="stylesheet" href="/css/main.css"></noscript>
</head>
<body>
    <!-- ... -->
</body>
</html>
```

- [ ] **1.9** Reducir tamaño de assets con compresión

```java
@Configuration
public class CompressionConfig {
    
    @Bean
    public FilterRegistrationBean<CompressingFilter> compressingFilter() {
        FilterRegistrationBean<CompressingFilter> registrationBean = 
            new FilterRegistrationBean<>();
        
        CompressingFilter filter = new CompressingFilter();
        registrationBean.setFilter(filter);
        registrationBean.addUrlPatterns("/*");
        
        return registrationBean;
    }
}
```

```properties
# application.properties - Habilitar compresión Gzip
server.compression.enabled=true
server.compression.mime-types=text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json,application/xml
server.compression.min-response-size=1024
```

- [ ] **1.10** Optimizar renderizado (evitar CLS)

```css
/* Reservar espacio para imágenes (evitar layout shift) */
.image-container {
    position: relative;
    width: 100%;
    padding-bottom: 66.67%; /* Aspect ratio 3:2 */
}

.image-container img {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    object-fit: cover;
}

/* Skeleton loaders para evitar CLS */
.skeleton {
    background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
    background-size: 200% 100%;
    animation: loading 1.5s infinite;
}

@keyframes loading {
    0% { background-position: 200% 0; }
    100% { background-position: -200% 0; }
}
```

---

## 📦 2. OPTIMIZACIONES BACKEND (8 tareas)

### 2.1. Descripción

Optimizar rendimiento del backend:
- Caché HTTP
- Caché de datos (Redis)
- Paginación eficiente
- Compresión de respuestas
- Connection pooling

#### Tareas:

- [ ] **2.1** Configurar caché HTTP

```java
@Configuration
public class CacheConfig implements WebMvcConfigurer {
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Cachear assets estáticos por 1 año
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCachePeriod(31536000); // 1 año
        
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCachePeriod(31536000);
        
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCachePeriod(31536000);
    }
}
```

- [ ] **2.2** Implementar caché de datos con Redis (opcional)

```java
@Configuration
@EnableCaching
public class RedisCacheConfig {
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .disableCachingNullValues()
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()
                )
            );
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build();
    }
}
```

```java
@Service
public class FacturaService {
    
    @Cacheable(value = "facturas", key = "#id")
    public FacturaDTO obtenerPorId(Long id) {
        // Se cachea el resultado
        return facturaRepository.findById(id)
            .map(facturaMapper::toDTO)
            .orElseThrow(() -> new NotFoundException("Factura no encontrada"));
    }
    
    @CacheEvict(value = "facturas", key = "#id")
    public void eliminar(Long id) {
        // Invalida la caché al eliminar
        facturaRepository.deleteById(id);
    }
}
```

- [ ] **2.3** Optimizar paginación

```java
@Service
public class FacturaService {
    
    /**
     * Paginación eficiente con Specification
     */
    public Page<FacturaDTO> listar(FacturaFiltroDTO filtro, Pageable pageable) {
        Specification<Factura> spec = FacturaSpecification.fromFiltro(filtro);
        
        Page<Factura> page = facturaRepository.findAll(spec, pageable);
        
        return page.map(facturaMapper::toDTO);
    }
}
```

```java
@RestController
@RequestMapping("/api/facturas")
public class FacturaRestController {
    
    @GetMapping
    public ResponseEntity<Page<FacturaDTO>> listar(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "id,desc") String[] sort
    ) {
        // Limitar tamaño máximo de página
        if (size > 100) {
            size = 100;
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(parseSort(sort)));
        
        Page<FacturaDTO> result = facturaService.listar(new FacturaFiltroDTO(), pageable);
        
        return ResponseEntity.ok(result);
    }
}
```

- [ ] **2.4** Implementar compresión Gzip/Brotli

```properties
# application.properties
server.compression.enabled=true
server.compression.mime-types=text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json,application/xml
server.compression.min-response-size=1024
```

- [ ] **2.5** Optimizar Connection Pooling (HikariCP)

```properties
# application.properties - HikariCP optimizado

# Pool size
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.maximum-pool-size=20

# Connection timeout
spring.datasource.hikari.connection-timeout=30000

# Idle timeout
spring.datasource.hikari.idle-timeout=600000

# Max lifetime
spring.datasource.hikari.max-lifetime=1800000

# Auto-commit
spring.datasource.hikari.auto-commit=true

# Pool name
spring.datasource.hikari.pool-name=WOMHikariPool

# Leak detection threshold
spring.datasource.hikari.leak-detection-threshold=60000
```

- [ ] **2.6** Implementar ETags para caché condicional

```java
@GetMapping("/api/facturas/{id}")
public ResponseEntity<FacturaDTO> obtener(
    @PathVariable Long id,
    @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
) {
    FacturaDTO factura = facturaService.obtenerPorId(id);
    
    // Calcular ETag
    String etag = generateETag(factura);
    
    // Si el cliente tiene la versión actual, retornar 304
    if (etag.equals(ifNoneMatch)) {
        return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
            .eTag(etag)
            .build();
    }
    
    // Retornar factura con ETag
    return ResponseEntity.ok()
        .eTag(etag)
        .body(factura);
}

private String generateETag(FacturaDTO factura) {
    return "\"" + DigestUtils.md5DigestAsHex(
        factura.toString().getBytes()
    ) + "\"";
}
```

- [ ] **2.7** Lazy loading de relaciones JPA

```java
@Entity
public class Factura {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Lazy loading por defecto para OneToMany
    @OneToMany(mappedBy = "factura", fetch = FetchType.LAZY)
    private List<DetalleFactura> detalles = new ArrayList<>();
    
    // Eager fetch solo cuando sea necesario
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;
}
```

```java
// Usar EntityGraph para optimizar queries
@EntityGraph(attributePaths = {"cliente", "detalles"})
@Query("SELECT f FROM Factura f WHERE f.id = :id")
Optional<Factura> findByIdWithDetails(@Param("id") Long id);
```

- [ ] **2.8** Implementar async processing para tareas pesadas

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}
```

```java
@Service
public class ReporteService {
    
    @Async("taskExecutor")
    public CompletableFuture<byte[]> generarReportePDF(Long facturaId) {
        // Procesamiento pesado en segundo plano
        byte[] pdf = pdfGenerator.generar(facturaId);
        return CompletableFuture.completedFuture(pdf);
    }
}
```

---

## 📦 3. OPTIMIZACIONES DE BASE DE DATOS (6 tareas)

- [ ] **3.1** Crear índices en columnas frecuentemente consultadas

```sql
-- Índices para mejorar performance

-- Facturas
CREATE INDEX idx_factura_fecha ON factura(fecha);
CREATE INDEX idx_factura_cliente_id ON factura(cliente_id);
CREATE INDEX idx_factura_estado ON factura(estado);
CREATE INDEX idx_factura_fecha_estado ON factura(fecha, estado);

-- Clientes
CREATE INDEX idx_cliente_email ON cliente(email);
CREATE INDEX idx_cliente_nombre ON cliente(nombre);

-- Productos
CREATE INDEX idx_producto_codigo ON producto(codigo);
CREATE INDEX idx_producto_nombre ON producto(nombre);

-- Detalles de factura
CREATE INDEX idx_detalle_factura_id ON detalle_factura(factura_id);
CREATE INDEX idx_detalle_producto_id ON detalle_factura(producto_id);

-- Usuarios
CREATE INDEX idx_usuario_email ON usuario(email);
CREATE INDEX idx_usuario_username ON usuario(username);
```

- [ ] **3.2** Optimizar queries N+1

```java
// ❌ MAL - Query N+1
List<Factura> facturas = facturaRepository.findAll();
facturas.forEach(factura -> {
    Cliente cliente = factura.getCliente(); // Query adicional por cada factura
    System.out.println(cliente.getNombre());
});

// ✅ BIEN - Fetch JOIN
@Query("SELECT f FROM Factura f JOIN FETCH f.cliente")
List<Factura> findAllWithCliente();
```

- [ ] **3.3** Implementar proyecciones para queries selectivas

```java
// Interface-based projection
public interface FacturaResumenProjection {
    Long getId();
    String getNumero();
    BigDecimal getTotal();
    LocalDate getFecha();
    String getClienteNombre();
}

@Query("SELECT f.id AS id, f.numero AS numero, f.total AS total, " +
       "f.fecha AS fecha, c.nombre AS clienteNombre " +
       "FROM Factura f JOIN f.cliente c")
List<FacturaResumenProjection> findAllResumen();
```

- [ ] **3.4** Implementar batch inserts/updates

```java
@Service
public class FacturaService {
    
    @Transactional
    public void crearLote(List<FacturaDTO> facturas) {
        int batchSize = 50;
        
        for (int i = 0; i < facturas.size(); i++) {
            Factura factura = facturaMapper.toEntity(facturas.get(i));
            facturaRepository.save(factura);
            
            if (i % batchSize == 0 && i > 0) {
                // Flush batch
                entityManager.flush();
                entityManager.clear();
            }
        }
    }
}
```

```properties
# application.properties - Habilitar batch processing
spring.jpa.properties.hibernate.jdbc.batch_size=50
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
spring.jpa.properties.hibernate.batch_versioned_data=true
```

- [ ] **3.5** Analizar y optimizar queries lentas

```properties
# Habilitar logging de queries lentas
spring.jpa.properties.hibernate.show_sql=false
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.use_sql_comments=true

# Log de queries lentas (> 2 segundos)
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# MySQL slow query log
# En application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/erp?logger=Slf4JLogger&profileSQL=true
```

- [ ] **3.6** Implementar particionamiento de tablas grandes (opcional)

```sql
-- Particionar tabla de facturas por año
ALTER TABLE factura
PARTITION BY RANGE (YEAR(fecha)) (
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p2026 VALUES LESS THAN (2027),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

---

## 📦 4. MEDICIÓN Y MONITOREO (4 tareas)

- [ ] **4.1** Configurar Google Lighthouse CI

```yaml
# .github/workflows/lighthouse.yml
name: Lighthouse CI
on: [push, pull_request]

jobs:
  lighthouse:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run Lighthouse CI
        uses: treosh/lighthouse-ci-action@v9
        with:
          urls: |
            http://localhost:8080
            http://localhost:8080/facturas
            http://localhost:8080/clientes
          budgetPath: ./lighthouse-budget.json
          uploadArtifacts: true
```

- [ ] **4.2** Implementar métricas de rendimiento

```java
@Component
@Aspect
public class PerformanceMonitoringAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitoringAspect.class);
    
    @Around("@annotation(MonitorPerformance)")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            
            if (duration > 1000) {
                logger.warn("⚠️ Método lento: {} - {}ms", 
                    joinPoint.getSignature().getName(), duration);
            }
            
            return result;
        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - start;
            logger.error("❌ Error en método: {} - {}ms", 
                joinPoint.getSignature().getName(), duration);
            throw e;
        }
    }
}
```

- [ ] **4.3** Dashboard de métricas con Actuator

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```properties
# application.properties
management.endpoints.web.exposure.include=health,metrics,prometheus
management.metrics.export.prometheus.enabled=true
```

- [ ] **4.4** Tests de carga con JMeter/Gatling

```scala
// Gatling load test
import io.gatling.core.Predef._
import io.gatling.http.Predef._

class FacturaLoadTest extends Simulation {
  
  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
  
  val scn = scenario("Facturas Load Test")
    .exec(http("Listar Facturas")
      .get("/api/facturas?page=0&size=20"))
    .pause(1)
    .exec(http("Ver Factura")
      .get("/api/facturas/1"))
  
  setUp(
    scn.inject(
      rampUsers(100) during (60) // 100 usuarios en 60 segundos
    )
  ).protocols(httpProtocol)
}
```

---

## 📊 CRITERIOS DE ACEPTACIÓN

✅ **Lighthouse Performance score > 90**  
✅ **FCP < 1.8s**  
✅ **LCP < 2.5s**  
✅ **TTI < 3.8s**  
✅ **CLS < 0.1**  
✅ **Lazy loading implementado**  
✅ **Imágenes optimizadas (WebP)**  
✅ **CSS/JS minificados**  
✅ **Caché HTTP configurada**  
✅ **Índices de BD creados**  
✅ **Queries optimizadas**  

---

## 📚 DEPENDENCIAS

**Requiere:**
- ✅ Sprint 9 Fase 1: UX/UI
- ✅ Sprint 9 Fase 2: PWA

**Habilita:**
- 🚀 Fase 4: Testing

---

**Fase creada:** 16 de enero de 2026  
**Responsable:** Performance Team  
**Prioridad:** ALTA - Experiencia rápida
