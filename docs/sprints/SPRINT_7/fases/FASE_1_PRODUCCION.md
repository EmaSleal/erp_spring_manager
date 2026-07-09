# 🏭 FASE 1: Módulo de Producción (OPCIONAL)

**Sprint:** 7  
**Fase:** 1 de 5  
**Duración estimada:** 8-10 días  
**Prioridad:** ⭐ BAJA (Solo para empresas manufactureras)  
**Estado:** 📋 PENDIENTE (0/52 tareas)

---

## ⚠️ IMPORTANTE: ESTA FASE ES OPCIONAL

**Esta fase solo debe implementarse si:**
- ✅ La empresa fabrica o ensambla productos
- ✅ Necesita control de consumo de materias primas
- ✅ Requiere costeo de producción
- ✅ Maneja órdenes de trabajo

**Si NO es una empresa manufacturera, SALTAR a FASE 2: Mejoras Técnicas** 🚀

---

## 📋 OBJETIVO DE LA FASE

Implementar sistema completo de producción:
- Órdenes de producción
- Recetas (BOM - Bill of Materials)
- Procesos de producción
- Consumo de materiales
- Costeo de producción
- Control de producción en proceso
- Productos terminados

---

## 📊 PROGRESO GENERAL

```
Progreso: [0/52] ░░░░░░░░░░░░░░░░░░░░ 0%

├─ 1. Modelo de Datos              [0/12] ░░░░░░░░░░ 0%
├─ 2. Recetas de Producción (BOM)  [0/10] ░░░░░░░░░░ 0%
├─ 3. Órdenes de Producción        [0/12] ░░░░░░░░░░ 0%
├─ 4. Consumo de Materiales        [0/8]  ░░░░░░░░░░ 0%
├─ 5. Costeo de Producción         [0/6]  ░░░░░░░░░░ 0%
└─ 6. Interfaz de Usuario          [0/4]  ░░░░░░░░░░ 0%
```

---

## 📦 1. MODELO DE DATOS (12 tareas)

### 1.1. Entidad `RecetaProduccion.java`

**Archivo:** `src/main/java/com/erp/model/RecetaProduccion.java`

#### Tareas:

- [ ] **1.1.1** Crear entidad `RecetaProduccion` (BOM)

```java
package com.erp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Receta de producción (Bill of Materials - BOM).
 * Define qué materiales se necesitan para producir un producto.
 * 
 * @author ERP Team
 * @version 7.0
 * @since Sprint 7
 */
@Entity
@Table(name = "recetas_produccion")
@Data
public class RecetaProduccion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Código de la receta
     */
    @Column(unique = true, nullable = false, length = 20)
    private String codigo;
    
    /**
     * Producto final que se produce
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_final_id", nullable = false)
    private Producto productoFinal;
    
    /**
     * Cantidad que produce esta receta
     */
    @Column(name = "cantidad_producida", nullable = false)
    private Integer cantidadProducida = 1;
    
    /**
     * Versión de la receta
     */
    @Column(nullable = false)
    private Integer version = 1;
    
    /**
     * Si es la receta activa para este producto
     */
    @Column(nullable = false)
    private Boolean activa = true;
    
    /**
     * Ingredientes/materias primas
     */
    @OneToMany(mappedBy = "receta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IngredienteReceta> ingredientes = new ArrayList<>();
    
    /**
     * Procesos de producción
     */
    @OneToMany(mappedBy = "receta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProcesoProduccion> procesos = new ArrayList<>();
    
    /**
     * Tiempo estimado de producción (minutos)
     */
    @Column(name = "tiempo_produccion_minutos")
    private Integer tiempoProduccionMinutos;
    
    /**
     * Costo estimado de mano de obra
     */
    @Column(name = "costo_mano_obra", precision = 19, scale = 2)
    private BigDecimal costoManoObra = BigDecimal.ZERO;
    
    /**
     * Descripción de la receta
     */
    @Column(length = 1000)
    private String descripcion;
    
    // Auditoría
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Calcula el costo total de materiales.
     */
    public BigDecimal calcularCostoMateriales() {
        return ingredientes.stream()
            .map(IngredienteReceta::calcularCostoTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Calcula el costo total de producción.
     */
    public BigDecimal calcularCostoTotal() {
        return calcularCostoMateriales().add(costoManoObra);
    }
}
```

- [ ] **1.1.2** Crear migration SQL para `recetas_produccion`

```sql
-- Migration: MIGRATION_PRODUCCION_SPRINT_7.sql

CREATE TABLE recetas_produccion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(20) UNIQUE NOT NULL,
    producto_final_id BIGINT NOT NULL,
    cantidad_producida INT NOT NULL DEFAULT 1,
    version INT NOT NULL DEFAULT 1,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    tiempo_produccion_minutos INT,
    costo_mano_obra DECIMAL(19,2) DEFAULT 0.00,
    descripcion TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (producto_final_id) REFERENCES productos(id),
    
    INDEX idx_codigo (codigo),
    INDEX idx_producto_final (producto_final_id),
    INDEX idx_activa (activa)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 1.2. Entidad `IngredienteReceta.java`

#### Tareas:

- [ ] **1.2.1** Crear entidad `IngredienteReceta`

```java
@Entity
@Table(name = "ingredientes_receta")
@Data
public class IngredienteReceta {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receta_id", nullable = false)
    private RecetaProduccion receta;
    
    /**
     * Producto/materia prima requerida
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;
    
    /**
     * Cantidad requerida
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal cantidad;
    
    /**
     * Unidad de medida
     */
    @Column(length = 20)
    private String unidadMedida;
    
    /**
     * Si es ingrediente opcional
     */
    @Column(nullable = false)
    private Boolean opcional = false;
    
    /**
     * Costo unitario del ingrediente
     */
    @Column(name = "costo_unitario", precision = 19, scale = 2)
    private BigDecimal costoUnitario;
    
    /**
     * Calcula el costo total del ingrediente.
     */
    public BigDecimal calcularCostoTotal() {
        if (costoUnitario == null) {
            costoUnitario = producto.getCosto();
        }
        return costoUnitario.multiply(cantidad);
    }
}
```

- [ ] **1.2.2** Crear migration SQL para `ingredientes_receta`

```sql
CREATE TABLE ingredientes_receta (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    receta_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad DECIMAL(19,4) NOT NULL,
    unidad_medida VARCHAR(20),
    opcional BOOLEAN NOT NULL DEFAULT FALSE,
    costo_unitario DECIMAL(19,2),
    
    FOREIGN KEY (receta_id) REFERENCES recetas_produccion(id) ON DELETE CASCADE,
    FOREIGN KEY (producto_id) REFERENCES productos(id),
    
    INDEX idx_receta (receta_id),
    INDEX idx_producto (producto_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 1.3. Entidad `ProcesoProduccion.java`

#### Tareas:

- [ ] **1.3.1** Crear entidad `ProcesoProduccion`

```java
@Entity
@Table(name = "procesos_produccion")
@Data
public class ProcesoProduccion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receta_id", nullable = false)
    private RecetaProduccion receta;
    
    /**
     * Número de secuencia del proceso
     */
    @Column(nullable = false)
    private Integer secuencia;
    
    /**
     * Nombre del proceso
     */
    @Column(nullable = false, length = 200)
    private String nombre;
    
    /**
     * Descripción detallada
     */
    @Column(length = 1000)
    private String descripcion;
    
    /**
     * Tiempo estimado en minutos
     */
    @Column(name = "tiempo_minutos")
    private Integer tiempoMinutos;
    
    /**
     * Estación de trabajo o área
     */
    @Column(name = "estacion_trabajo", length = 100)
    private String estacionTrabajo;
    
    /**
     * Personal requerido
     */
    @Column(name = "personal_requerido")
    private Integer personalRequerido = 1;
}
```

- [ ] **1.3.2** Crear migration SQL para `procesos_produccion`

```sql
CREATE TABLE procesos_produccion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    receta_id BIGINT NOT NULL,
    secuencia INT NOT NULL,
    nombre VARCHAR(200) NOT NULL,
    descripcion TEXT,
    tiempo_minutos INT,
    estacion_trabajo VARCHAR(100),
    personal_requerido INT DEFAULT 1,
    
    FOREIGN KEY (receta_id) REFERENCES recetas_produccion(id) ON DELETE CASCADE,
    
    INDEX idx_receta (receta_id),
    INDEX idx_secuencia (receta_id, secuencia)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 1.4. Entidad `OrdenProduccion.java`

#### Tareas:

- [ ] **1.4.1** Crear entidad `OrdenProduccion`

```java
@Entity
@Table(name = "ordenes_produccion")
@Data
public class OrdenProduccion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 20)
    private String numero;
    
    /**
     * Receta a producir
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receta_id", nullable = false)
    private RecetaProduccion receta;
    
    /**
     * Cantidad a producir
     */
    @Column(name = "cantidad_planificada", nullable = false)
    private Integer cantidadPlanificada;
    
    /**
     * Cantidad producida
     */
    @Column(name = "cantidad_producida")
    private Integer cantidadProducida = 0;
    
    /**
     * Fecha de inicio planificada
     */
    @Column(name = "fecha_inicio_planificada", nullable = false)
    private LocalDate fechaInicioPlanificada;
    
    /**
     * Fecha de inicio real
     */
    @Column(name = "fecha_inicio_real")
    private LocalDateTime fechaInicioReal;
    
    /**
     * Fecha de finalización planificada
     */
    @Column(name = "fecha_fin_planificada")
    private LocalDate fechaFinPlanificada;
    
    /**
     * Fecha de finalización real
     */
    @Column(name = "fecha_fin_real")
    private LocalDateTime fechaFinReal;
    
    /**
     * Estado de la orden
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoOrdenProduccion estado = EstadoOrdenProduccion.PLANIFICADA;
    
    /**
     * Prioridad
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PrioridadProduccion prioridad = PrioridadProduccion.NORMAL;
    
    /**
     * Costo total de materiales consumidos
     */
    @Column(name = "costo_materiales", precision = 19, scale = 2)
    private BigDecimal costoMateriales = BigDecimal.ZERO;
    
    /**
     * Costo de mano de obra
     */
    @Column(name = "costo_mano_obra", precision = 19, scale = 2)
    private BigDecimal costoManoObra = BigDecimal.ZERO;
    
    /**
     * Costo total
     */
    @Column(name = "costo_total", precision = 19, scale = 2)
    private BigDecimal costoTotal = BigDecimal.ZERO;
    
    /**
     * Consumos de materiales
     */
    @OneToMany(mappedBy = "ordenProduccion", cascade = CascadeType.ALL)
    private List<ConsumoMaterial> consumos = new ArrayList<>();
    
    @Column(length = 1000)
    private String observaciones;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

enum EstadoOrdenProduccion {
    PLANIFICADA,    // Creada, no iniciada
    EN_PROCESO,     // En producción
    PAUSADA,        // Pausada temporalmente
    COMPLETADA,     // Finalizada
    CANCELADA       // Cancelada
}

enum PrioridadProduccion {
    BAJA,
    NORMAL,
    ALTA,
    URGENTE
}
```

- [ ] **1.4.2** Crear migration SQL para `ordenes_produccion`

```sql
CREATE TABLE ordenes_produccion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero VARCHAR(20) UNIQUE NOT NULL,
    receta_id BIGINT NOT NULL,
    cantidad_planificada INT NOT NULL,
    cantidad_producida INT DEFAULT 0,
    fecha_inicio_planificada DATE NOT NULL,
    fecha_inicio_real DATETIME,
    fecha_fin_planificada DATE,
    fecha_fin_real DATETIME,
    estado VARCHAR(20) NOT NULL DEFAULT 'PLANIFICADA',
    prioridad VARCHAR(20) DEFAULT 'NORMAL',
    costo_materiales DECIMAL(19,2) DEFAULT 0.00,
    costo_mano_obra DECIMAL(19,2) DEFAULT 0.00,
    costo_total DECIMAL(19,2) DEFAULT 0.00,
    observaciones TEXT,
    usuario_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (receta_id) REFERENCES recetas_produccion(id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    
    INDEX idx_numero (numero),
    INDEX idx_estado (estado),
    INDEX idx_fecha_inicio (fecha_inicio_planificada)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 1.5. Entidad `ConsumoMaterial.java`

#### Tareas:

- [ ] **1.5.1** Crear entidad `ConsumoMaterial`

```java
@Entity
@Table(name = "consumos_materiales")
@Data
public class ConsumoMaterial {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_produccion_id", nullable = false)
    private OrdenProduccion ordenProduccion;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;
    
    /**
     * Cantidad planificada según receta
     */
    @Column(name = "cantidad_planificada", precision = 19, scale = 4)
    private BigDecimal cantidadPlanificada;
    
    /**
     * Cantidad realmente consumida
     */
    @Column(name = "cantidad_consumida", precision = 19, scale = 4)
    private BigDecimal cantidadConsumida;
    
    /**
     * Costo unitario al momento del consumo
     */
    @Column(name = "costo_unitario", precision = 19, scale = 2)
    private BigDecimal costoUnitario;
    
    /**
     * Costo total del consumo
     */
    @Column(name = "costo_total", precision = 19, scale = 2)
    private BigDecimal costoTotal;
    
    @Column(name = "fecha_consumo")
    private LocalDateTime fechaConsumo;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id")
    private LoteProducto lote;
}
```

- [ ] **1.5.2** Crear migration SQL para `consumos_materiales`

```sql
CREATE TABLE consumos_materiales (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    orden_produccion_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad_planificada DECIMAL(19,4),
    cantidad_consumida DECIMAL(19,4),
    costo_unitario DECIMAL(19,2),
    costo_total DECIMAL(19,2),
    fecha_consumo DATETIME,
    lote_id BIGINT,
    
    FOREIGN KEY (orden_produccion_id) REFERENCES ordenes_produccion(id),
    FOREIGN KEY (producto_id) REFERENCES productos(id),
    FOREIGN KEY (lote_id) REFERENCES lotes_productos(id),
    
    INDEX idx_orden (orden_produccion_id),
    INDEX idx_producto (producto_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 1.6. Repositories

#### Tareas:

- [ ] **1.6.1** Crear repositories

```java
@Repository
public interface RecetaProduccionRepository extends JpaRepository<RecetaProduccion, Long> {
    Optional<RecetaProduccion> findByCodigo(String codigo);
    List<RecetaProduccion> findByProductoFinalAndActivaTrue(Producto producto);
}

@Repository
public interface OrdenProduccionRepository extends JpaRepository<OrdenProduccion, Long> {
    Optional<OrdenProduccion> findByNumero(String numero);
    List<OrdenProduccion> findByEstado(EstadoOrdenProduccion estado);
    List<OrdenProduccion> findByFechaInicioPlanificadaBetween(LocalDate desde, LocalDate hasta);
}
```

---

## 📦 2. RECETAS DE PRODUCCIÓN (10 tareas)

### 2.1. Service

#### Tareas:

- [ ] **2.1.1** Crear `RecetaProduccionService.java`

```java
@Service
@RequiredArgsConstructor
public class RecetaProduccionService {
    
    private final RecetaProduccionRepository recetaRepository;
    private final ProductoRepository productoRepository;
    
    @Transactional
    public RecetaProduccionDTO crearReceta(RecetaProduccionDTO dto) {
        Producto productoFinal = productoRepository.findById(dto.getProductoFinalId())
            .orElseThrow(() -> new BusinessException("Producto final no encontrado"));
        
        RecetaProduccion receta = new RecetaProduccion();
        receta.setCodigo(generarCodigo());
        receta.setProductoFinal(productoFinal);
        receta.setCantidadProducida(dto.getCantidadProducida());
        receta.setVersion(1);
        receta.setActiva(true);
        receta.setTiempoProduccionMinutos(dto.getTiempoProduccionMinutos());
        receta.setCostoManoObra(dto.getCostoManoObra());
        receta.setDescripcion(dto.getDescripcion());
        
        // Ingredientes
        dto.getIngredientes().forEach(ingredienteDTO -> {
            Producto producto = productoRepository.findById(ingredienteDTO.getProductoId())
                .orElseThrow(() -> new BusinessException("Producto ingrediente no encontrado"));
            
            IngredienteReceta ingrediente = new IngredienteReceta();
            ingrediente.setReceta(receta);
            ingrediente.setProducto(producto);
            ingrediente.setCantidad(ingredienteDTO.getCantidad());
            ingrediente.setUnidadMedida(ingredienteDTO.getUnidadMedida());
            ingrediente.setOpcional(ingredienteDTO.getOpcional());
            ingrediente.setCostoUnitario(producto.getCosto());
            
            receta.getIngredientes().add(ingrediente);
        });
        
        receta = recetaRepository.save(receta);
        
        log.info("Receta creada: {} para producto {}", receta.getCodigo(), productoFinal.getNombre());
        
        return toDTO(receta);
    }
    
    private String generarCodigo() {
        int year = LocalDate.now().getYear();
        Integer ultimo = recetaRepository.findUltimoConsecutivo(year);
        int siguiente = (ultimo != null ? ultimo : 0) + 1;
        return String.format("REC-%d-%05d", year, siguiente);
    }
}
```

- [ ] **2.1.2** Controller de recetas

- [ ] **2.1.3** Vista de lista de recetas

- [ ] **2.1.4** Formulario de creación de receta

- [ ] **2.1.5** Vista de detalle de receta (BOM)

- [ ] **2.1.6** Calcular costo de receta

- [ ] **2.1.7** Validar disponibilidad de materiales

- [ ] **2.1.8** Copiar receta (nueva versión)

- [ ] **2.1.9** Activar/desactivar receta

- [ ] **2.1.10** Exportar BOM a PDF

---

## 📦 3. ÓRDENES DE PRODUCCIÓN (12 tareas)

### 3.1. Service

#### Tareas:

- [ ] **3.1.1** Crear `OrdenProduccionService.java`

```java
@Service
@RequiredArgsConstructor
public class OrdenProduccionService {
    
    private final OrdenProduccionRepository ordenRepository;
    private final RecetaProduccionRepository recetaRepository;
    private final MovimientoInventarioService movimientoService;
    
    @Transactional
    public OrdenProduccionDTO crearOrden(OrdenProduccionDTO dto) {
        RecetaProduccion receta = recetaRepository.findById(dto.getRecetaId())
            .orElseThrow(() -> new BusinessException("Receta no encontrada"));
        
        // Validar disponibilidad de materiales
        validarDisponibilidadMateriales(receta, dto.getCantidad());
        
        OrdenProduccion orden = new OrdenProduccion();
        orden.setNumero(generarNumero());
        orden.setReceta(receta);
        orden.setCantidadPlanificada(dto.getCantidad());
        orden.setFechaInicioPlanificada(dto.getFechaInicio());
        orden.setEstado(EstadoOrdenProduccion.PLANIFICADA);
        orden.setPrioridad(dto.getPrioridad());
        
        // Calcular fecha fin estimada
        int minutosTotal = receta.getTiempoProduccionMinutos() * dto.getCantidad();
        orden.setFechaFinPlanificada(dto.getFechaInicio().plusDays(minutosTotal / 480)); // 8h/día
        
        // Crear consumos planificados
        receta.getIngredientes().forEach(ingrediente -> {
            BigDecimal cantidadNecesaria = ingrediente.getCantidad()
                .multiply(BigDecimal.valueOf(dto.getCantidad()));
            
            ConsumoMaterial consumo = new ConsumoMaterial();
            consumo.setOrdenProduccion(orden);
            consumo.setProducto(ingrediente.getProducto());
            consumo.setCantidadPlanificada(cantidadNecesaria);
            consumo.setCostoUnitario(ingrediente.getProducto().getCosto());
            
            orden.getConsumos().add(consumo);
        });
        
        orden = ordenRepository.save(orden);
        
        log.info("Orden de producción creada: {}", orden.getNumero());
        
        return toDTO(orden);
    }
    
    @Transactional
    public void iniciarProduccion(Long ordenId) {
        OrdenProduccion orden = ordenRepository.findById(ordenId)
            .orElseThrow(() -> new BusinessException("Orden no encontrada"));
        
        if (orden.getEstado() != EstadoOrdenProduccion.PLANIFICADA) {
            throw new BusinessException("Orden ya iniciada o completada");
        }
        
        // Consumir materiales del inventario
        orden.getConsumos().forEach(consumo -> {
            movimientoService.registrarSalida(
                consumo.getProducto().getId(),
                consumo.getCantidadPlanificada().intValue(),
                TipoMovimientoInventario.SALIDA_PRODUCCION,
                "ORDEN_PRODUCCION",
                orden.getId(),
                usuarioActual,
                "Consumo para orden " + orden.getNumero()
            );
            
            consumo.setCantidadConsumida(consumo.getCantidadPlanificada());
            consumo.setFechaConsumo(LocalDateTime.now());
            consumo.setCostoTotal(consumo.getCostoUnitario().multiply(consumo.getCantidadPlanificada()));
        });
        
        orden.setEstado(EstadoOrdenProduccion.EN_PROCESO);
        orden.setFechaInicioReal(LocalDateTime.now());
        
        ordenRepository.save(orden);
        
        log.info("Producción iniciada: {}", orden.getNumero());
    }
    
    @Transactional
    public void completarProduccion(Long ordenId, Integer cantidadProducida) {
        OrdenProduccion orden = ordenRepository.findById(ordenId)
            .orElseThrow(() -> new BusinessException("Orden no encontrada"));
        
        if (orden.getEstado() != EstadoOrdenProduccion.EN_PROCESO) {
            throw new BusinessException("Orden no está en proceso");
        }
        
        // Registrar productos terminados en inventario
        Producto productoFinal = orden.getReceta().getProductoFinal();
        
        movimientoService.registrarEntrada(
            productoFinal.getId(),
            cantidadProducida,
            calcularCostoUnitarioProducido(orden, cantidadProducida),
            TipoMovimientoInventario.ENTRADA_PRODUCCION,
            "ORDEN_PRODUCCION",
            orden.getId(),
            usuarioActual,
            "Productos terminados de orden " + orden.getNumero(),
            null
        );
        
        orden.setCantidadProducida(cantidadProducida);
        orden.setEstado(EstadoOrdenProduccion.COMPLETADA);
        orden.setFechaFinReal(LocalDateTime.now());
        
        // Calcular costos finales
        BigDecimal costoMateriales = orden.getConsumos().stream()
            .map(ConsumoMaterial::getCostoTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        orden.setCostoMateriales(costoMateriales);
        orden.setCostoManoObra(orden.getReceta().getCostoManoObra().multiply(BigDecimal.valueOf(cantidadProducida)));
        orden.setCostoTotal(costoMateriales.add(orden.getCostoManoObra()));
        
        ordenRepository.save(orden);
        
        log.info("Producción completada: {} ({} unidades)", orden.getNumero(), cantidadProducida);
    }
    
    private BigDecimal calcularCostoUnitarioProducido(OrdenProduccion orden, Integer cantidad) {
        BigDecimal costoTotal = orden.getCostoMateriales().add(orden.getCostoManoObra());
        return costoTotal.divide(BigDecimal.valueOf(cantidad), 2, BigDecimal.ROUND_HALF_UP);
    }
    
    private void validarDisponibilidadMateriales(RecetaProduccion receta, Integer cantidad) {
        receta.getIngredientes().forEach(ingrediente -> {
            BigDecimal cantidadNecesaria = ingrediente.getCantidad().multiply(BigDecimal.valueOf(cantidad));
            Producto producto = ingrediente.getProducto();
            
            if (producto.getStock() < cantidadNecesaria.intValue()) {
                throw new BusinessException(
                    String.format("Stock insuficiente de %s. Necesario: %.2f, Disponible: %d",
                        producto.getNombre(), cantidadNecesaria, producto.getStock())
                );
            }
        });
    }
    
    private String generarNumero() {
        int year = LocalDate.now().getYear();
        Integer ultimo = ordenRepository.findUltimoConsecutivo(year);
        int siguiente = (ultimo != null ? ultimo : 0) + 1;
        return String.format("OP-%d-%05d", year, siguiente);
    }
}
```

- [ ] **3.1.2** Controller de órdenes

- [ ] **3.1.3** Vista de lista de órdenes

- [ ] **3.1.4** Formulario de nueva orden

- [ ] **3.1.5** Vista de detalle de orden

- [ ] **3.1.6** Iniciar producción

- [ ] **3.1.7** Registrar avance de producción

- [ ] **3.1.8** Completar producción

- [ ] **3.1.9** Pausar/reanudar producción

- [ ] **3.1.10** Cancelar orden

- [ ] **3.1.11** Dashboard de órdenes activas

- [ ] **3.1.12** Reporte de órdenes completadas

---

## 📦 4. CONSUMO DE MATERIALES (8 tareas)

_Ya integrado en sección 3.1_

---

## 📦 5. COSTEO DE PRODUCCIÓN (6 tareas)

#### Tareas:

- [ ] **5.1** Calcular costo de materiales

- [ ] **5.2** Calcular costo de mano de obra

- [ ] **5.3** Calcular costo total por unidad

- [ ] **5.4** Reporte de costos de producción

- [ ] **5.5** Análisis de variaciones (planificado vs real)

- [ ] **5.6** Dashboard de costeo

---

## 📦 6. INTERFAZ DE USUARIO (4 tareas)

#### Tareas:

- [ ] **6.1** Dashboard de producción

- [ ] **6.2** Vista de calendario de producción

- [ ] **6.3** Gráficas de eficiencia

- [ ] **6.4** Reportes en PDF

---

## 📊 CRITERIOS DE ACEPTACIÓN

✅ CRUD completo de recetas (BOM)  
✅ Validación de materiales antes de iniciar producción  
✅ Consumo automático de inventario al iniciar  
✅ Registro de productos terminados al completar  
✅ Costeo preciso de producción  
✅ Dashboard con órdenes activas  
✅ Reportes de eficiencia  

---

## 📚 DEPENDENCIAS

**Requiere:**
- ✅ Sprint 6 completado (Inventario)
- ✅ Sistema de movimientos de inventario

**Habilita:**
- 🚀 Manufactura completa
- 🚀 Control de costos de producción
- 🚀 Planificación de producción

---

## 🔄 PRÓXIMOS PASOS

Si implementó esta fase:
1. ✅ Probar flujo completo: receta → orden → producción → productos terminados
2. 🚀 Continuar con **FASE 2: Mejoras Técnicas**

Si NO implementó (RECOMENDADO para la mayoría):
1. 🚀 Saltar directamente a **FASE 2: Mejoras Técnicas** (CRÍTICO)

---

**Fase creada:** 16 de enero de 2026  
**Responsable:** Equipo de Desarrollo  
**Nota:** Esta fase es OPCIONAL y solo para empresas manufactureras
