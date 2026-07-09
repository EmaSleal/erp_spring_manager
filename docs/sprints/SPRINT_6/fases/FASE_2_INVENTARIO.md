# 📦 FASE 2: Inventario Avanzado (Control de Stock)

**Sprint:** 6  
**Fase:** 2 de 5  
**Duración estimada:** 7-9 días  
**Prioridad:** ⭐⭐⭐ CRÍTICA  
**Estado:** 📋 PENDIENTE (0/48 tareas)

---

## 📋 OBJETIVO DE LA FASE

Implementar sistema robusto de control de inventario:
- Kardex detallado por producto
- Movimientos de entrada/salida automáticos
- Gestión de lotes y fechas de vencimiento
- Alertas de stock mínimo y bajo
- Ajustes de inventario (positivos/negativos)
- Reporte de rotación de inventario
- **Activar enum `PRODUCTO_AJUSTAR_INVENTARIO`** ⚠️
- **Implementar filtro `stockBajo` en reportes** 🔧

---

## 📊 PROGRESO GENERAL

```
Progreso: [0/48] ░░░░░░░░░░░░░░░░░░░░ 0%

├─ 1. Modelo de Datos                [0/10] ░░░░░░░░░░ 0%
├─ 2. Movimientos de Inventario      [0/12] ░░░░░░░░░░ 0%
├─ 3. Kardex y Reportes              [0/8]  ░░░░░░░░░░ 0%
├─ 4. Lotes y Vencimientos           [0/8]  ░░░░░░░░░░ 0%
├─ 5. Alertas y Notificaciones       [0/6]  ░░░░░░░░░░ 0%
└─ 6. Interfaz de Usuario            [0/4]  ░░░░░░░░░░ 0%
```

---

## 📦 1. MODELO DE DATOS (10 tareas)

### 1.1. Actualizar Entidad `Producto.java`

**Archivo:** `src/main/java/com/erp/model/Producto.java`

#### Tareas:

- [ ] **1.1.1** Agregar campos de control de inventario

```java
/**
 * Stock mínimo permitido antes de generar alerta
 */
@Column(name = "stock_minimo")
private Integer stockMinimo = 10;

/**
 * Stock bajo que genera alerta preventiva
 */
@Column(name = "stock_bajo")
private Integer stockBajo = 20;

/**
 * Stock máximo recomendado
 */
@Column(name = "stock_maximo")
private Integer stockMaximo = 1000;

/**
 * Punto de reorden (cuando pedir más inventario)
 */
@Column(name = "punto_reorden")
private Integer puntoReorden = 15;

/**
 * Si el producto maneja lotes
 */
@Column(name = "maneja_lotes")
private Boolean manejaLotes = false;

/**
 * Si el producto tiene fecha de vencimiento
 */
@Column(name = "maneja_vencimiento")
private Boolean manejaVencimiento = false;

/**
 * Días de anticipación para alerta de vencimiento
 */
@Column(name = "dias_alerta_vencimiento")
private Integer diasAlertaVencimiento = 30;

/**
 * Verifica si el stock está bajo.
 * Integración con hallazgo: filtro stockBajo
 */
public boolean isStockBajo() {
    return this.stock != null && this.stockBajo != null && this.stock <= this.stockBajo;
}

/**
 * Verifica si se alcanzó el punto de reorden.
 */
public boolean alcanzoPuntoReorden() {
    return this.stock != null && this.puntoReorden != null && this.stock <= this.puntoReorden;
}

/**
 * Verifica si el stock está bajo el mínimo (crítico).
 */
public boolean isStockCritico() {
    return this.stock != null && this.stockMinimo != null && this.stock <= this.stockMinimo;
}
```

- [ ] **1.1.2** Crear migration SQL para nuevos campos

```sql
-- Migration: MIGRATION_INVENTARIO_SPRINT_6.sql

ALTER TABLE productos
ADD COLUMN stock_minimo INT DEFAULT 10 AFTER stock,
ADD COLUMN stock_bajo INT DEFAULT 20 AFTER stock_minimo,
ADD COLUMN stock_maximo INT DEFAULT 1000 AFTER stock_bajo,
ADD COLUMN punto_reorden INT DEFAULT 15 AFTER stock_maximo,
ADD COLUMN maneja_lotes BOOLEAN DEFAULT FALSE AFTER punto_reorden,
ADD COLUMN maneja_vencimiento BOOLEAN DEFAULT FALSE AFTER maneja_lotes,
ADD COLUMN dias_alerta_vencimiento INT DEFAULT 30 AFTER maneja_vencimiento;

-- Actualizar productos existentes con valores por defecto
UPDATE productos 
SET stock_minimo = 10,
    stock_bajo = 20,
    stock_maximo = 1000,
    punto_reorden = 15
WHERE stock_minimo IS NULL;

-- Índices para búsquedas de stock bajo
CREATE INDEX idx_stock_bajo ON productos(stock, stock_bajo);
CREATE INDEX idx_stock_critico ON productos(stock, stock_minimo);
```

---

### 1.2. Entidad `MovimientoInventario.java`

#### Tareas:

- [ ] **1.2.1** Crear entidad `MovimientoInventario`

```java
package com.erp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registra todos los movimientos de inventario.
 * Permite generar Kardex detallado por producto.
 * 
 * @author ERP Team
 * @version 6.0
 * @since Sprint 6
 */
@Entity
@Table(name = "movimientos_inventario")
@Data
public class MovimientoInventario {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Producto del movimiento
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;
    
    /**
     * Tipo de movimiento
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoMovimientoInventario tipo;
    
    /**
     * Cantidad del movimiento (positiva para entradas, negativa para salidas)
     */
    @Column(nullable = false)
    private Integer cantidad;
    
    /**
     * Costo unitario en el momento del movimiento
     */
    @Column(precision = 19, scale = 2)
    private BigDecimal costoUnitario;
    
    /**
     * Costo total del movimiento
     */
    @Column(precision = 19, scale = 2)
    private BigDecimal costoTotal;
    
    /**
     * Stock antes del movimiento
     */
    @Column(name = "stock_anterior", nullable = false)
    private Integer stockAnterior;
    
    /**
     * Stock después del movimiento
     */
    @Column(name = "stock_nuevo", nullable = false)
    private Integer stockNuevo;
    
    /**
     * Fecha y hora del movimiento
     */
    @Column(nullable = false)
    private LocalDateTime fecha;
    
    /**
     * Documento origen (factura, orden compra, ajuste, etc.)
     */
    @Column(name = "documento_origen", length = 50)
    private String documentoOrigen;
    
    /**
     * ID del documento origen
     */
    @Column(name = "documento_origen_id")
    private Long documentoOrigenId;
    
    /**
     * Observaciones del movimiento
     */
    @Column(length = 500)
    private String observaciones;
    
    /**
     * Lote del producto (si aplica)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id")
    private LoteProducto lote;
    
    /**
     * Usuario que realizó el movimiento
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
    
    // Auditoría
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
        
        // Calcular costo total
        if (costoUnitario != null && cantidad != null) {
            costoTotal = costoUnitario.multiply(BigDecimal.valueOf(Math.abs(cantidad)));
        }
    }
}

/**
 * Tipos de movimientos de inventario
 */
enum TipoMovimientoInventario {
    ENTRADA_COMPRA,          // Compra a proveedor
    ENTRADA_DEVOLUCION,      // Devolución de cliente
    ENTRADA_AJUSTE,          // Ajuste positivo
    ENTRADA_TRANSFERENCIA,   // Recepción de otra bodega
    SALIDA_VENTA,           // Venta a cliente
    SALIDA_DEVOLUCION,      // Devolución a proveedor
    SALIDA_AJUSTE,          // Ajuste negativo (merma, robo, etc.)
    SALIDA_TRANSFERENCIA,   // Envío a otra bodega
    SALIDA_PRODUCCION       // Consumo en producción
}
```

- [ ] **1.2.2** Crear migration SQL para `movimientos_inventario`

```sql
CREATE TABLE movimientos_inventario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    producto_id BIGINT NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    cantidad INT NOT NULL,
    costo_unitario DECIMAL(19,2),
    costo_total DECIMAL(19,2),
    stock_anterior INT NOT NULL,
    stock_nuevo INT NOT NULL,
    fecha DATETIME NOT NULL,
    documento_origen VARCHAR(50),
    documento_origen_id BIGINT,
    observaciones VARCHAR(500),
    lote_id BIGINT,
    usuario_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (producto_id) REFERENCES productos(id),
    FOREIGN KEY (lote_id) REFERENCES lotes_productos(id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    
    INDEX idx_producto (producto_id),
    INDEX idx_fecha (fecha),
    INDEX idx_tipo (tipo),
    INDEX idx_documento (documento_origen, documento_origen_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 1.3. Entidad `LoteProducto.java`

#### Tareas:

- [ ] **1.3.1** Crear entidad `LoteProducto`

```java
package com.erp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Gestión de lotes de productos.
 * Permite rastreabilidad y control de vencimientos.
 * 
 * @author ERP Team
 * @version 6.0
 * @since Sprint 6
 */
@Entity
@Table(name = "lotes_productos")
@Data
public class LoteProducto {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Producto del lote
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;
    
    /**
     * Código del lote (puede venir del proveedor)
     */
    @Column(nullable = false, length = 50)
    private String codigo;
    
    /**
     * Fecha de fabricación
     */
    @Column(name = "fecha_fabricacion")
    private LocalDate fechaFabricacion;
    
    /**
     * Fecha de vencimiento
     */
    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;
    
    /**
     * Cantidad actual en el lote
     */
    @Column(nullable = false)
    private Integer cantidad = 0;
    
    /**
     * Cantidad inicial del lote
     */
    @Column(name = "cantidad_inicial", nullable = false)
    private Integer cantidadInicial;
    
    /**
     * Costo unitario del lote
     */
    @Column(name = "costo_unitario", precision = 19, scale = 2)
    private BigDecimal costoUnitario;
    
    /**
     * Estado del lote
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoLote estado = EstadoLote.ACTIVO;
    
    /**
     * Proveedor del lote
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;
    
    /**
     * Observaciones del lote
     */
    @Column(length = 500)
    private String observaciones;
    
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
        
        // Auto-actualizar estado si se agotó
        if (cantidad != null && cantidad == 0) {
            estado = EstadoLote.AGOTADO;
        }
    }
    
    /**
     * Verifica si el lote está próximo a vencer.
     */
    public boolean isPorVencer(int diasAnticipacion) {
        if (fechaVencimiento == null) {
            return false;
        }
        LocalDate fechaAlerta = LocalDate.now().plusDays(diasAnticipacion);
        return fechaVencimiento.isBefore(fechaAlerta) || fechaVencimiento.isEqual(fechaAlerta);
    }
    
    /**
     * Verifica si el lote está vencido.
     */
    public boolean isVencido() {
        return fechaVencimiento != null && fechaVencimiento.isBefore(LocalDate.now());
    }
}

/**
 * Estados de un lote
 */
enum EstadoLote {
    ACTIVO,      // Lote disponible
    AGOTADO,     // Sin existencias
    VENCIDO,     // Fecha de vencimiento superada
    BLOQUEADO    // No disponible para venta (problemas de calidad, etc.)
}
```

- [ ] **1.3.2** Crear migration SQL para `lotes_productos`

```sql
CREATE TABLE lotes_productos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    producto_id BIGINT NOT NULL,
    codigo VARCHAR(50) NOT NULL,
    fecha_fabricacion DATE,
    fecha_vencimiento DATE,
    cantidad INT NOT NULL DEFAULT 0,
    cantidad_inicial INT NOT NULL,
    costo_unitario DECIMAL(19,2),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    proveedor_id BIGINT,
    observaciones VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (producto_id) REFERENCES productos(id),
    FOREIGN KEY (proveedor_id) REFERENCES proveedores(id),
    
    UNIQUE KEY uk_producto_codigo (producto_id, codigo),
    INDEX idx_fecha_vencimiento (fecha_vencimiento),
    INDEX idx_estado (estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 1.4. Entidad `AjusteInventario.java`

#### Tareas:

- [ ] **1.4.1** Crear entidad `AjusteInventario` (para activar enum PRODUCTO_AJUSTAR_INVENTARIO)

```java
package com.erp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Ajustes de inventario (positivos o negativos).
 * Activa el uso del permiso PRODUCTO_AJUSTAR_INVENTARIO.
 * 
 * @author ERP Team
 * @version 6.0
 * @since Sprint 6
 */
@Entity
@Table(name = "ajustes_inventario")
@Data
public class AjusteInventario {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Número consecutivo del ajuste
     */
    @Column(unique = true, nullable = false, length = 20)
    private String numero;
    
    /**
     * Fecha del ajuste
     */
    @Column(nullable = false)
    private LocalDateTime fecha;
    
    /**
     * Tipo de ajuste
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoAjuste tipoAjuste;
    
    /**
     * Motivo del ajuste
     */
    @Column(nullable = false, length = 500)
    private String motivo;
    
    /**
     * Detalles del ajuste
     */
    @OneToMany(mappedBy = "ajuste", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleAjusteInventario> detalles = new ArrayList<>();
    
    /**
     * Usuario que realizó el ajuste
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    
    /**
     * Estado del ajuste
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAjuste estado = EstadoAjuste.PENDIENTE;
    
    /**
     * Usuario que aprobó el ajuste
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprobado_por_id")
    private Usuario aprobadoPor;
    
    /**
     * Fecha de aprobación
     */
    @Column(name = "fecha_aprobacion")
    private LocalDateTime fechaAprobacion;
    
    // Auditoría
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

/**
 * Tipos de ajuste
 */
enum TipoAjuste {
    POSITIVO,  // Aumenta inventario (faltante en conteo)
    NEGATIVO   // Disminuye inventario (merma, robo, daño)
}

/**
 * Estados de ajuste
 */
enum EstadoAjuste {
    PENDIENTE,   // Creado, esperando aprobación
    APROBADO,    // Aprobado y aplicado
    RECHAZADO,   // Rechazado, no se aplicará
    ANULADO      // Anulado después de aprobado
}
```

- [ ] **1.4.2** Crear `DetalleAjusteInventario.java`

```java
@Entity
@Table(name = "detalles_ajuste_inventario")
@Data
public class DetalleAjusteInventario {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ajuste_id", nullable = false)
    private AjusteInventario ajuste;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;
    
    /**
     * Cantidad física contada
     */
    @Column(name = "cantidad_fisica", nullable = false)
    private Integer cantidadFisica;
    
    /**
     * Cantidad en sistema antes del ajuste
     */
    @Column(name = "cantidad_sistema", nullable = false)
    private Integer cantidadSistema;
    
    /**
     * Diferencia (física - sistema)
     * Positivo: faltaba en sistema
     * Negativo: sobraba en sistema
     */
    @Column(nullable = false)
    private Integer diferencia;
    
    /**
     * Costo unitario del producto
     */
    @Column(name = "costo_unitario", precision = 19, scale = 2)
    private BigDecimal costoUnitario;
    
    /**
     * Lote afectado (si aplica)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id")
    private LoteProducto lote;
    
    @PrePersist
    protected void onCreate() {
        // Calcular diferencia
        diferencia = cantidadFisica - cantidadSistema;
    }
}
```

- [ ] **1.4.3** Crear migration SQL para ajustes

```sql
CREATE TABLE ajustes_inventario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero VARCHAR(20) UNIQUE NOT NULL,
    fecha DATETIME NOT NULL,
    tipo_ajuste VARCHAR(20) NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    usuario_id BIGINT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    aprobado_por_id BIGINT,
    fecha_aprobacion DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    FOREIGN KEY (aprobado_por_id) REFERENCES usuarios(id),
    
    INDEX idx_numero (numero),
    INDEX idx_estado (estado),
    INDEX idx_fecha (fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE detalles_ajuste_inventario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ajuste_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad_fisica INT NOT NULL,
    cantidad_sistema INT NOT NULL,
    diferencia INT NOT NULL,
    costo_unitario DECIMAL(19,2),
    lote_id BIGINT,
    
    FOREIGN KEY (ajuste_id) REFERENCES ajustes_inventario(id),
    FOREIGN KEY (producto_id) REFERENCES productos(id),
    FOREIGN KEY (lote_id) REFERENCES lotes_productos(id),
    
    INDEX idx_ajuste (ajuste_id),
    INDEX idx_producto (producto_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

- [ ] **1.4.4** Agregar permiso `PRODUCTO_AJUSTAR_INVENTARIO` a tabla de permisos

```sql
-- Activar permiso PRODUCTO_AJUSTAR_INVENTARIO
INSERT INTO permisos (nombre, descripcion, modulo, recurso, accion, activo)
VALUES 
('PRODUCTO_AJUSTAR_INVENTARIO', 'Ajustar inventario de productos', 'INVENTARIO', 'PRODUCTO', 'AJUSTAR', TRUE);

-- Asignar a rol ADMIN
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r, permisos p
WHERE r.nombre = 'ADMIN' AND p.nombre = 'PRODUCTO_AJUSTAR_INVENTARIO';

-- Asignar a rol GERENTE
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r, permisos p
WHERE r.nombre = 'GERENTE' AND p.nombre = 'PRODUCTO_AJUSTAR_INVENTARIO';
```

---

### 1.5. Actualizar Repositorios

#### Tareas:

- [ ] **1.5.1** Crear `MovimientoInventarioRepository.java`

```java
package com.erp.repository;

import com.erp.model.MovimientoInventario;
import com.erp.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {
    
    /**
     * Busca movimientos por producto ordenados por fecha (Kardex)
     */
    List<MovimientoInventario> findByProductoOrderByFechaDesc(Producto producto);
    
    /**
     * Busca movimientos en un rango de fechas
     */
    List<MovimientoInventario> findByFechaBetween(LocalDateTime desde, LocalDateTime hasta);
    
    /**
     * Busca movimientos por tipo
     */
    List<MovimientoInventario> findByTipo(TipoMovimientoInventario tipo);
    
    /**
     * Busca movimientos por documento origen
     */
    List<MovimientoInventario> findByDocumentoOrigenAndDocumentoOrigenId(String documentoOrigen, Long id);
}
```

- [ ] **1.5.2** Crear `LoteProductoRepository.java`

```java
@Repository
public interface LoteProductoRepository extends JpaRepository<LoteProducto, Long> {
    
    /**
     * Busca lotes activos de un producto ordenados por fecha de vencimiento (FIFO)
     */
    List<LoteProducto> findByProductoAndEstadoOrderByFechaVencimientoAsc(
        Producto producto, 
        EstadoLote estado
    );
    
    /**
     * Busca lotes próximos a vencer
     */
    @Query("SELECT l FROM LoteProducto l " +
           "WHERE l.estado = 'ACTIVO' " +
           "AND l.fechaVencimiento IS NOT NULL " +
           "AND l.fechaVencimiento BETWEEN :desde AND :hasta " +
           "ORDER BY l.fechaVencimiento ASC")
    List<LoteProducto> findLotesPorVencer(LocalDate desde, LocalDate hasta);
    
    /**
     * Busca lotes vencidos
     */
    @Query("SELECT l FROM LoteProducto l " +
           "WHERE l.estado = 'ACTIVO' " +
           "AND l.fechaVencimiento IS NOT NULL " +
           "AND l.fechaVencimiento < :fecha")
    List<LoteProducto> findLotesVencidos(LocalDate fecha);
}
```

- [ ] **1.5.3** Crear `AjusteInventarioRepository.java`

```java
@Repository
public interface AjusteInventarioRepository extends JpaRepository<AjusteInventario, Long> {
    
    Optional<AjusteInventario> findByNumero(String numero);
    
    List<AjusteInventario> findByEstado(EstadoAjuste estado);
    
    List<AjusteInventario> findByFechaBetween(LocalDateTime desde, LocalDateTime hasta);
    
    @Query("SELECT MAX(CAST(SUBSTRING(a.numero, 5) AS int)) FROM AjusteInventario a " +
           "WHERE a.numero LIKE CONCAT('AJ-', :anio, '-%')")
    Integer findUltimoConsecutivo(int anio);
}
```

- [ ] **1.5.4** Actualizar `ProductoRepository` con query de stock bajo

```java
/**
 * Busca productos con stock bajo.
 * Implementa hallazgo: filtro stockBajo en reportes
 */
@Query("SELECT p FROM Producto p WHERE p.stock <= p.stockBajo AND p.activo = true")
List<Producto> findProductosStockBajo();

/**
 * Busca productos en nivel crítico (por debajo del mínimo)
 */
@Query("SELECT p FROM Producto p WHERE p.stock <= p.stockMinimo AND p.activo = true")
List<Producto> findProductosStockCritico();

/**
 * Busca productos que alcanzaron punto de reorden
 */
@Query("SELECT p FROM Producto p WHERE p.stock <= p.puntoReorden AND p.activo = true")
List<Producto> findProductosPuntoReorden();
```

---

## 📦 2. MOVIMIENTOS DE INVENTARIO (12 tareas)

### 2.1. Service

#### Tareas:

- [ ] **2.1.1** Crear `MovimientoInventarioService.java`

```java
package com.erp.service;

import com.erp.dto.MovimientoInventarioDTO;
import com.erp.model.*;
import com.erp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovimientoInventarioService {
    
    private final MovimientoInventarioRepository movimientoRepository;
    private final ProductoRepository productoRepository;
    private final LoteProductoRepository loteRepository;
    
    /**
     * Registra una salida de inventario (venta, ajuste negativo, etc.)
     */
    @Transactional
    public MovimientoInventario registrarSalida(
        Long productoId,
        Integer cantidad,
        TipoMovimientoInventario tipo,
        String documentoOrigen,
        Long documentoOrigenId,
        Usuario usuario,
        String observaciones
    ) {
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new BusinessException("Producto no encontrado"));
        
        // Verificar stock disponible
        if (producto.getStock() < cantidad) {
            throw new BusinessException(
                String.format("Stock insuficiente. Disponible: %d, Solicitado: %d",
                    producto.getStock(), cantidad)
            );
        }
        
        // Crear movimiento
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setProducto(producto);
        movimiento.setTipo(tipo);
        movimiento.setCantidad(-cantidad); // Negativo para salida
        movimiento.setCostoUnitario(producto.getCosto());
        movimiento.setStockAnterior(producto.getStock());
        movimiento.setStockNuevo(producto.getStock() - cantidad);
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setDocumentoOrigen(documentoOrigen);
        movimiento.setDocumentoOrigenId(documentoOrigenId);
        movimiento.setObservaciones(observaciones);
        movimiento.setUsuario(usuario);
        
        // Si maneja lotes, aplicar FIFO
        if (producto.getManejaLotes()) {
            aplicarSalidaConLotes(movimiento, cantidad);
        }
        
        // Actualizar stock del producto
        producto.setStock(producto.getStock() - cantidad);
        productoRepository.save(producto);
        
        movimientoRepository.save(movimiento);
        
        log.info("Salida de inventario registrada: {} unidades de {} ({})",
            cantidad, producto.getNombre(), tipo);
        
        return movimiento;
    }
    
    /**
     * Registra una entrada de inventario (compra, ajuste positivo, etc.)
     */
    @Transactional
    public MovimientoInventario registrarEntrada(
        Long productoId,
        Integer cantidad,
        BigDecimal costoUnitario,
        TipoMovimientoInventario tipo,
        String documentoOrigen,
        Long documentoOrigenId,
        Usuario usuario,
        String observaciones,
        LoteProducto lote
    ) {
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new BusinessException("Producto no encontrado"));
        
        // Crear movimiento
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setProducto(producto);
        movimiento.setTipo(tipo);
        movimiento.setCantidad(cantidad); // Positivo para entrada
        movimiento.setCostoUnitario(costoUnitario);
        movimiento.setStockAnterior(producto.getStock());
        movimiento.setStockNuevo(producto.getStock() + cantidad);
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setDocumentoOrigen(documentoOrigen);
        movimiento.setDocumentoOrigenId(documentoOrigenId);
        movimiento.setObservaciones(observaciones);
        movimiento.setUsuario(usuario);
        movimiento.setLote(lote);
        
        // Actualizar stock del producto
        producto.setStock(producto.getStock() + cantidad);
        
        // Actualizar costo promedio ponderado
        actualizarCostoPromedio(producto, cantidad, costoUnitario);
        
        productoRepository.save(producto);
        movimientoRepository.save(movimiento);
        
        // Si maneja lotes, actualizar cantidad del lote
        if (lote != null) {
            lote.setCantidad(lote.getCantidad() + cantidad);
            loteRepository.save(lote);
        }
        
        log.info("Entrada de inventario registrada: {} unidades de {} ({})",
            cantidad, producto.getNombre(), tipo);
        
        return movimiento;
    }
    
    /**
     * Actualiza el costo promedio ponderado del producto.
     */
    private void actualizarCostoPromedio(Producto producto, Integer cantidadNueva, BigDecimal costoNuevo) {
        BigDecimal stockAnterior = BigDecimal.valueOf(producto.getStock());
        BigDecimal costoAnterior = producto.getCosto();
        BigDecimal cantidadNuevaDec = BigDecimal.valueOf(cantidadNueva);
        
        // Costo total anterior
        BigDecimal costoTotalAnterior = stockAnterior.multiply(costoAnterior);
        
        // Costo total nuevo
        BigDecimal costoTotalNuevo = cantidadNuevaDec.multiply(costoNuevo);
        
        // Cantidad total
        BigDecimal cantidadTotal = stockAnterior.add(cantidadNuevaDec);
        
        // Costo promedio
        BigDecimal costoPromedio = costoTotalAnterior.add(costoTotalNuevo)
            .divide(cantidadTotal, 2, BigDecimal.ROUND_HALF_UP);
        
        producto.setCosto(costoPromedio);
    }
    
    /**
     * Aplica salida de inventario usando método FIFO para lotes.
     */
    private void aplicarSalidaConLotes(MovimientoInventario movimiento, Integer cantidad) {
        Producto producto = movimiento.getProducto();
        
        // Obtener lotes activos ordenados por fecha de vencimiento (FIFO)
        List<LoteProducto> lotes = loteRepository
            .findByProductoAndEstadoOrderByFechaVencimientoAsc(producto, EstadoLote.ACTIVO);
        
        int cantidadRestante = cantidad;
        
        for (LoteProducto lote : lotes) {
            if (cantidadRestante == 0) break;
            
            int cantidadLote = Math.min(lote.getCantidad(), cantidadRestante);
            lote.setCantidad(lote.getCantidad() - cantidadLote);
            cantidadRestante -= cantidadLote;
            
            loteRepository.save(lote);
            
            // Asociar movimiento con primer lote usado
            if (movimiento.getLote() == null) {
                movimiento.setLote(lote);
            }
        }
        
        if (cantidadRestante > 0) {
            throw new BusinessException("No hay lotes suficientes para cubrir la salida");
        }
    }
}
```

- [ ] **2.1.2** Integrar con `FacturaService` para registrar salidas automáticas

```java
// En FacturaService.crearFactura()

@Autowired
private MovimientoInventarioService movimientoInventarioService;

// Después de guardar la factura
factura.getDetalles().forEach(detalle -> {
    movimientoInventarioService.registrarSalida(
        detalle.getProducto().getId(),
        detalle.getCantidad(),
        TipoMovimientoInventario.SALIDA_VENTA,
        "FACTURA",
        factura.getId(),
        usuarioActual,
        "Venta factura " + factura.getNumero()
    );
});
```

- [ ] **2.1.3** Crear endpoint REST para consultar kardex

```java
@RestController
@RequestMapping("/api/inventario/kardex")
@RequiredArgsConstructor
public class KardexController {
    
    private final MovimientoInventarioService movimientoService;
    
    @GetMapping("/{productoId}")
    public ResponseEntity<List<MovimientoInventarioDTO>> obtenerKardex(
        @PathVariable Long productoId
    ) {
        List<MovimientoInventarioDTO> kardex = movimientoService.obtenerKardex(productoId);
        return ResponseEntity.ok(kardex);
    }
}
```

---

### 2.2. Gestión de Lotes

#### Tareas:

- [ ] **2.2.1** Crear `LoteProductoService.java`

```java
@Service
@RequiredArgsConstructor
public class LoteProductoService {
    
    private final LoteProductoRepository loteRepository;
    private final ProductoRepository productoRepository;
    
    @Transactional
    public LoteProducto crearLote(Long productoId, String codigo, LocalDate fechaVencimiento, 
                                  Integer cantidad, BigDecimal costoUnitario) {
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new BusinessException("Producto no encontrado"));
        
        if (!producto.getManejaLotes()) {
            throw new BusinessException("El producto no maneja lotes");
        }
        
        LoteProducto lote = new LoteProducto();
        lote.setProducto(producto);
        lote.setCodigo(codigo);
        lote.setFechaVencimiento(fechaVencimiento);
        lote.setCantidad(cantidad);
        lote.setCantidadInicial(cantidad);
        lote.setCostoUnitario(costoUnitario);
        lote.setEstado(EstadoLote.ACTIVO);
        
        return loteRepository.save(lote);
    }
    
    /**
     * Obtiene lotes próximos a vencer.
     */
    public List<LoteProducto> obtenerLotesPorVencer(int diasAnticipacion) {
        LocalDate hoy = LocalDate.now();
        LocalDate fechaLimite = hoy.plusDays(diasAnticipacion);
        
        return loteRepository.findLotesPorVencer(hoy, fechaLimite);
    }
}
```

- [ ] **2.2.2** Crear vista para gestión de lotes

- [ ] **2.2.3** Implementar alerta de lotes próximos a vencer

---

## 📦 3. KARDEX Y REPORTES (8 tareas)

### 3.1. Reporte de Kardex

#### Tareas:

- [ ] **3.1.1** Crear `KardexService.java`

```java
@Service
@RequiredArgsConstructor
public class KardexService {
    
    private final MovimientoInventarioRepository movimientoRepository;
    
    /**
     * Genera reporte de Kardex para un producto.
     */
    public KardexDTO generarKardex(Long productoId, LocalDate desde, LocalDate hasta) {
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new BusinessException("Producto no encontrado"));
        
        List<MovimientoInventario> movimientos = movimientoRepository
            .findByProductoOrderByFechaDesc(producto);
        
        KardexDTO kardex = new KardexDTO();
        kardex.setProducto(producto.getNombre());
        kardex.setCodigoProducto(producto.getCodigo());
        kardex.setMovimientos(new ArrayList<>());
        
        BigDecimal saldoValor = BigDecimal.ZERO;
        
        for (MovimientoInventario mov : movimientos) {
            KardexMovimientoDTO movDTO = new KardexMovimientoDTO();
            movDTO.setFecha(mov.getFecha());
            movDTO.setDocumento(mov.getDocumentoOrigen() + "-" + mov.getDocumentoOrigenId());
            
            if (mov.getCantidad() > 0) {
                // Entrada
                movDTO.setEntradaCantidad(mov.getCantidad());
                movDTO.setEntradaCosto(mov.getCostoUnitario());
                movDTO.setEntradaTotal(mov.getCostoTotal());
            } else {
                // Salida
                movDTO.setSalidaCantidad(Math.abs(mov.getCantidad()));
                movDTO.setSalidaCosto(mov.getCostoUnitario());
                movDTO.setSalidaTotal(mov.getCostoTotal());
            }
            
            movDTO.setSaldoCantidad(mov.getStockNuevo());
            
            saldoValor = saldoValor.add(mov.getCostoTotal());
            movDTO.setSaldoValor(saldoValor);
            
            kardex.getMovimientos().add(movDTO);
        }
        
        return kardex;
    }
}
```

- [ ] **3.1.2** Crear vista HTML de Kardex con tabla detallada

- [ ] **3.1.3** Exportar Kardex a PDF

- [ ] **3.1.4** Exportar Kardex a Excel

---

### 3.2. Reportes de Inventario

#### Tareas:

- [ ] **3.2.1** Crear reporte de productos con stock bajo (implementa hallazgo `stockBajo`)

```java
@Service
public class ReporteInventarioService {
    
    /**
     * Genera reporte de productos con stock bajo.
     * Implementa hallazgo: filtro stockBajo en reportes
     */
    public ReporteStockBajoDTO generarReporteStockBajo() {
        List<Producto> productosStockBajo = productoRepository.findProductosStockBajo();
        List<Producto> productosCriticos = productoRepository.findProductosStockCritico();
        List<Producto> puntosReorden = productoRepository.findProductosPuntoReorden();
        
        ReporteStockBajoDTO reporte = new ReporteStockBajoDTO();
        reporte.setProductosStockBajo(productosStockBajo);
        reporte.setProductosCriticos(productosCriticos);
        reporte.setProductosPuntoReorden(puntosReorden);
        
        return reporte;
    }
}
```

- [ ] **3.2.2** Crear dashboard de inventario con gráficas

- [ ] **3.2.3** Reporte de rotación de inventario

- [ ] **3.2.4** Reporte de valorización de inventario

---

## 📦 4. LOTES Y VENCIMIENTOS (8 tareas)

_Las tareas de lotes ya están incluidas en secciones anteriores (1.3 y 2.2)_

---

## 📦 5. ALERTAS Y NOTIFICACIONES (6 tareas)

### 5.1. Sistema de Alertas

#### Tareas:

- [ ] **5.1.1** Crear servicio de alertas de inventario

```java
@Service
@RequiredArgsConstructor
public class AlertaInventarioService {
    
    private final ProductoRepository productoRepository;
    private final LoteProductoRepository loteRepository;
    private final NotificacionService notificacionService;
    
    /**
     * Verifica y genera alertas de stock bajo.
     * Se ejecuta diariamente.
     */
    @Scheduled(cron = "0 0 8 * * *") // 8:00 AM diario
    public void verificarAlertasStockBajo() {
        List<Producto> productosBajos = productoRepository.findProductosStockBajo();
        
        productosBajos.forEach(producto -> {
            String mensaje = String.format(
                "⚠️ Stock bajo: %s (Disponible: %d, Mínimo: %d)",
                producto.getNombre(),
                producto.getStock(),
                producto.getStockBajo()
            );
            
            notificacionService.enviarNotificacion(
                TipoNotificacion.STOCK_BAJO,
                mensaje,
                producto.getId()
            );
        });
        
        log.info("Verificación de stock bajo completada: {} alertas generadas",
            productosBajos.size());
    }
    
    /**
     * Verifica lotes próximos a vencer.
     */
    @Scheduled(cron = "0 0 9 * * *") // 9:00 AM diario
    public void verificarLotesPorVencer() {
        List<LoteProducto> lotesPorVencer = loteRepository
            .findLotesPorVencer(LocalDate.now(), LocalDate.now().plusDays(30));
        
        lotesPorVencer.forEach(lote -> {
            String mensaje = String.format(
                "⏰ Lote por vencer: %s (Lote: %s, Vence: %s, Cantidad: %d)",
                lote.getProducto().getNombre(),
                lote.getCodigo(),
                lote.getFechaVencimiento(),
                lote.getCantidad()
            );
            
            notificacionService.enviarNotificacion(
                TipoNotificacion.LOTE_POR_VENCER,
                mensaje,
                lote.getId()
            );
        });
    }
}
```

- [ ] **5.1.2** Agregar tipos de notificación de inventario

- [ ] **5.1.3** Crear vista de alertas de inventario

- [ ] **5.1.4** Notificación por email de stock crítico

- [ ] **5.1.5** Notificación por WhatsApp de lotes por vencer (integración Bot)

- [ ] **5.1.6** Dashboard de alertas en tiempo real

---

## 📦 6. INTERFAZ DE USUARIO (4 tareas)

#### Tareas:

- [ ] **6.1** Vista de lista de ajustes de inventario

- [ ] **6.2** Formulario de ajuste de inventario (activar PRODUCTO_AJUSTAR_INVENTARIO)

- [ ] **6.3** Vista de gestión de lotes por producto

- [ ] **6.4** Dashboard de inventario con indicadores

---

## 📊 CRITERIOS DE ACEPTACIÓN

✅ Movimientos automáticos al vender (SALIDA_VENTA)  
✅ Kardex detallado consultable por producto  
✅ Gestión de lotes con FIFO automático  
✅ Alertas de stock bajo funcionando  
✅ Alertas de lotes por vencer  
✅ Ajustes de inventario con aprobación  
✅ Permiso `PRODUCTO_AJUSTAR_INVENTARIO` activado  
✅ Filtro `stockBajo` implementado en reportes  
✅ Reporte de rotación de inventario  
✅ Costo promedio ponderado actualizado automáticamente  

---

## 📚 DEPENDENCIAS

**Requiere:**
- ✅ Sprint 5 completado
- ✅ Entidad `Producto` existente
- ✅ Sistema de permisos implementado

**Habilita:**
- 🚀 Control preciso de stock
- 🚀 Trazabilidad completa
- 🚀 Gestión de proveedores (Fase 3)

---

## 🔄 PRÓXIMOS PASOS

Una vez completada esta fase:
1. ✅ Verificar movimientos automáticos
2. ✅ Probar alertas de stock
3. 🚀 Continuar con **FASE 3: Proveedores**

---

**Hallazgos aplicados:**
- ⚠️ Permiso `PRODUCTO_AJUSTAR_INVENTARIO` ahora se usa en ajustes
- 🔧 Filtro `stockBajo` implementado en `ProductoRepository`
- 🔧 Método `isStockBajo()` agregado a `Producto.java`

---

**Fase creada:** 16 de enero de 2026  
**Responsable:** Equipo de Desarrollo
