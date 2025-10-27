# 🚀 MEJORAS FUTURAS Y CAMBIOS PLANIFICADOS

**Proyecto:** WhatsApp Orders Manager  
**Fecha de creación:** 12 de octubre de 2025  
**Última actualización:** 12 de octubre de 2025

---

## 📋 ÍNDICE

1. [Mejoras de Alta Prioridad](#mejoras-de-alta-prioridad)
2. [Mejoras de Media Prioridad](#mejoras-de-media-prioridad)
3. [Mejoras de Baja Prioridad](#mejoras-de-baja-prioridad)
4. [Refactorizaciones Técnicas](#refactorizaciones-técnicas)
5. [Nuevas Funcionalidades](#nuevas-funcionalidades)

---

## 🔴 MEJORAS DE ALTA PRIORIDAD

### 1. SISTEMA DE GESTIÓN MULTI-DIVISA

**Estado:** 📝 Planificado  
**Prioridad:** Alta  
**Sprint sugerido:** Sprint 3  
**Fecha de solicitud:** 20/10/2025

#### Descripción
Implementar un **sistema completo de gestión de múltiples divisas** que permita facturar en diferentes monedas, registrar tipos de cambio diarios, y mantener una divisa maestra para reportes consolidados.

#### Contexto Actual

Actualmente el sistema tiene configurada una divisa en `configuracion_facturacion` (campo `moneda` y `simbolo_moneda`), pero:

❌ **Solo soporta UNA divisa** para todo el sistema  
❌ **No permite facturar en diferentes monedas** según el cliente  
❌ **No registra tipos de cambio**  
❌ **No convierte valores** entre divisas  
❌ **Reportes en una sola moneda**  

#### Solución Propuesta

Implementar un sistema que permita:

✅ **Gestionar múltiples divisas** (USD, MXN, EUR, etc.)  
✅ **Definir una divisa maestra** para reportes consolidados  
✅ **Registrar tipos de cambio diarios** de forma manual o automática  
✅ **Facturar en cualquier divisa** disponible  
✅ **Ver facturas con divisa original** + valor convertido a divisa maestra  
✅ **Reportes consolidados** en divisa maestra  

---

#### 📊 Modelo de Base de Datos

##### 1. Tabla de Divisas

```sql
CREATE TABLE divisa (
    id_divisa INT PRIMARY KEY AUTO_INCREMENT,
    codigo VARCHAR(3) NOT NULL UNIQUE COMMENT 'Código ISO 4217: USD, MXN, EUR, etc.',
    nombre VARCHAR(50) NOT NULL COMMENT 'Nombre completo: Dólar Estadounidense, Peso Mexicano',
    simbolo VARCHAR(10) NOT NULL COMMENT 'Símbolo: $, MX$, €, etc.',
    es_maestra BOOLEAN DEFAULT FALSE COMMENT 'Divisa principal para reportes',
    activo BOOLEAN DEFAULT TRUE,
    decimales INT DEFAULT 2 COMMENT 'Cantidad de decimales para esta divisa',
    pais VARCHAR(50) COMMENT 'País de origen',
    
    -- Auditoría
    create_by INT,
    create_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by INT,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_codigo_divisa UNIQUE (codigo),
    CONSTRAINT chk_solo_una_maestra CHECK (
        (SELECT COUNT(*) FROM divisa WHERE es_maestra = TRUE AND activo = TRUE) <= 1
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Catálogo de divisas del sistema';

-- Datos iniciales
INSERT INTO divisa (codigo, nombre, simbolo, es_maestra, decimales, pais) VALUES
('MXN', 'Peso Mexicano', 'MX$', TRUE, 2, 'México'),
('USD', 'Dólar Estadounidense', '$', FALSE, 2, 'Estados Unidos'),
('EUR', 'Euro', '€', FALSE, 2, 'Unión Europea'),
('GTQ', 'Quetzal Guatemalteco', 'Q', FALSE, 2, 'Guatemala'),
('HNL', 'Lempira Hondureña', 'L', FALSE, 2, 'Honduras');
```

##### 2. Tabla de Tipos de Cambio

```sql
CREATE TABLE tipo_cambio (
    id_tipo_cambio INT PRIMARY KEY AUTO_INCREMENT,
    id_divisa_origen INT NOT NULL COMMENT 'Divisa desde la que se convierte',
    id_divisa_destino INT NOT NULL COMMENT 'Divisa a la que se convierte',
    
    -- Tipo de cambio
    fecha DATE NOT NULL COMMENT 'Fecha de vigencia del tipo de cambio',
    tasa DECIMAL(18, 6) NOT NULL COMMENT 'Tasa de conversión: 1 origen = X destino',
    tasa_compra DECIMAL(18, 6) COMMENT 'Tasa de compra (opcional)',
    tasa_venta DECIMAL(18, 6) COMMENT 'Tasa de venta (opcional)',
    
    -- Metadatos
    fuente VARCHAR(100) COMMENT 'Fuente del tipo de cambio: Manual, Banco Central, API, etc.',
    activo BOOLEAN DEFAULT TRUE,
    
    -- Auditoría
    create_by INT,
    create_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by INT,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (id_divisa_origen) REFERENCES divisa(id_divisa),
    FOREIGN KEY (id_divisa_destino) REFERENCES divisa(id_divisa),
    
    -- Solo un tipo de cambio activo por par de divisas por día
    CONSTRAINT uk_tipo_cambio_diario UNIQUE (id_divisa_origen, id_divisa_destino, fecha, activo),
    
    INDEX idx_fecha_tipo_cambio (fecha DESC),
    INDEX idx_divisas_tipo_cambio (id_divisa_origen, id_divisa_destino)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Histórico de tipos de cambio entre divisas';

-- Ejemplo de datos iniciales (USD -> MXN al 20/10/2025)
INSERT INTO tipo_cambio (id_divisa_origen, id_divisa_destino, fecha, tasa, tasa_compra, tasa_venta, fuente) 
VALUES 
(2, 1, '2025-10-20', 19.85, 19.75, 19.95, 'Banco de México'),
(3, 1, '2025-10-20', 21.20, 21.10, 21.30, 'Banco Central Europeo');
```

##### 3. Modificar Tabla Factura

```sql
-- Agregar campos de divisa a la tabla factura
ALTER TABLE factura 
ADD COLUMN id_divisa INT COMMENT 'Divisa en la que se emitió la factura',
ADD COLUMN tipo_cambio_registrado DECIMAL(18, 6) COMMENT 'Tipo de cambio al momento de crear la factura',
ADD COLUMN total_divisa_maestra DECIMAL(10, 2) COMMENT 'Total convertido a divisa maestra para reportes';

-- Relación con tabla divisa
ALTER TABLE factura
ADD CONSTRAINT fk_factura_divisa 
FOREIGN KEY (id_divisa) REFERENCES divisa(id_divisa);

-- Índice para reportes
CREATE INDEX idx_factura_divisa ON factura(id_divisa);

-- Trigger para calcular total_divisa_maestra automáticamente
DELIMITER $$

CREATE TRIGGER tr_factura_calcular_total_maestra
BEFORE INSERT ON factura
FOR EACH ROW
BEGIN
    DECLARE divisa_maestra_id INT;
    DECLARE tipo_cambio DECIMAL(18, 6);
    
    -- Obtener ID de divisa maestra
    SELECT id_divisa INTO divisa_maestra_id 
    FROM divisa 
    WHERE es_maestra = TRUE AND activo = TRUE 
    LIMIT 1;
    
    -- Si la factura es en divisa maestra, el total es el mismo
    IF NEW.id_divisa = divisa_maestra_id THEN
        SET NEW.total_divisa_maestra = NEW.total;
        SET NEW.tipo_cambio_registrado = 1.0;
    ELSE
        -- Obtener tipo de cambio del día
        SELECT tasa INTO tipo_cambio
        FROM tipo_cambio
        WHERE id_divisa_origen = NEW.id_divisa
          AND id_divisa_destino = divisa_maestra_id
          AND fecha = CURDATE()
          AND activo = TRUE
        LIMIT 1;
        
        -- Si no hay tipo de cambio, usar 1.0 (o lanzar error según lógica de negocio)
        IF tipo_cambio IS NULL THEN
            SET tipo_cambio = 1.0;
        END IF;
        
        -- Calcular total en divisa maestra
        SET NEW.tipo_cambio_registrado = tipo_cambio;
        SET NEW.total_divisa_maestra = NEW.total * tipo_cambio;
    END IF;
END$$

DELIMITER ;
```

---

#### 💻 Modelo Java

##### 1. Entidad Divisa

```java
package api.astro.whats_orders_manager.models;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entidad que representa una divisa (moneda) en el sistema.
 * Permite gestionar múltiples divisas y definir cuál es la maestra.
 */
@Entity
@Table(name = "divisa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Divisa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_divisa")
    private Integer idDivisa;

    @Column(name = "codigo", unique = true, nullable = false, length = 3)
    private String codigo; // USD, MXN, EUR

    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre; // Dólar Estadounidense, Peso Mexicano

    @Column(name = "simbolo", nullable = false, length = 10)
    private String simbolo; // $, MX$, €

    @Column(name = "es_maestra")
    private Boolean esMaestra = false; // Solo una puede ser true

    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "decimales")
    private Integer decimales = 2;

    @Column(name = "pais", length = 50)
    private String pais;

    // Auditoría
    @CreatedBy
    @Column(name = "create_by", updatable = false)
    private Integer createBy;

    @CreatedDate
    @Column(name = "create_date", updatable = false)
    private LocalDateTime createDate;

    @LastModifiedBy
    @Column(name = "update_by")
    private Integer updateBy;

    @LastModifiedDate
    @Column(name = "update_date")
    private LocalDateTime updateDate;
}
```

##### 2. Entidad TipoCambio

```java
package api.astro.whats_orders_manager.models;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad que representa el tipo de cambio entre dos divisas en una fecha específica.
 */
@Entity
@Table(name = "tipo_cambio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class TipoCambio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_cambio")
    private Integer idTipoCambio;

    @ManyToOne
    @JoinColumn(name = "id_divisa_origen", nullable = false)
    private Divisa divisaOrigen;

    @ManyToOne
    @JoinColumn(name = "id_divisa_destino", nullable = false)
    private Divisa divisaDestino;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "tasa", nullable = false, precision = 18, scale = 6)
    private BigDecimal tasa; // 1 divisaOrigen = X divisaDestino

    @Column(name = "tasa_compra", precision = 18, scale = 6)
    private BigDecimal tasaCompra;

    @Column(name = "tasa_venta", precision = 18, scale = 6)
    private BigDecimal tasaVenta;

    @Column(name = "fuente", length = 100)
    private String fuente; // "Manual", "Banco Central", "API XE", etc.

    @Column(name = "activo")
    private Boolean activo = true;

    // Auditoría
    @CreatedBy
    @Column(name = "create_by", updatable = false)
    private Integer createBy;

    @CreatedDate
    @Column(name = "create_date", updatable = false)
    private LocalDateTime createDate;

    @Column(name = "update_by")
    private Integer updateBy;

    @Column(name = "update_date")
    private LocalDateTime updateDate;
}
```

##### 3. Actualizar Entidad Factura

```java
@Entity
@Table(name = "factura")
public class Factura {
    
    // ... campos existentes ...
    
    @ManyToOne
    @JoinColumn(name = "id_divisa")
    private Divisa divisa; // Divisa de la factura
    
    @Column(name = "tipo_cambio_registrado", precision = 18, scale = 6)
    private BigDecimal tipoCambioRegistrado; // Tipo de cambio al momento de crear
    
    @Column(name = "total_divisa_maestra", precision = 10, scale = 2)
    private BigDecimal totalDivisaMaestra; // Total convertido para reportes
    
    // ... resto de campos ...
}
```

---

#### 🎨 Interfaz de Usuario

##### 1. Módulo de Gestión de Divisas

**Ubicación:** `/configuracion?tab=divisas`

```html
<!-- Tabla de divisas -->
<div class="table-responsive">
    <table class="table table-hover">
        <thead>
            <tr>
                <th>Código</th>
                <th>Nombre</th>
                <th>Símbolo</th>
                <th>País</th>
                <th>Decimales</th>
                <th>Maestra</th>
                <th>Estado</th>
                <th>Acciones</th>
            </tr>
        </thead>
        <tbody>
            <tr th:each="divisa : ${divisas}">
                <td><strong th:text="${divisa.codigo}">USD</strong></td>
                <td th:text="${divisa.nombre}">Dólar Estadounidense</td>
                <td th:text="${divisa.simbolo}">$</td>
                <td th:text="${divisa.pais}">Estados Unidos</td>
                <td th:text="${divisa.decimales}">2</td>
                <td>
                    <span th:if="${divisa.esMaestra}" class="badge bg-warning">
                        <i class="fas fa-star"></i> Maestra
                    </span>
                </td>
                <td>
                    <span th:if="${divisa.activo}" class="badge bg-success">Activa</span>
                    <span th:unless="${divisa.activo}" class="badge bg-secondary">Inactiva</span>
                </td>
                <td>
                    <button class="btn btn-sm btn-primary" onclick="editarDivisa(${divisa.idDivisa})">
                        <i class="fas fa-edit"></i>
                    </button>
                </td>
            </tr>
        </tbody>
    </table>
</div>

<!-- Botón agregar -->
<button class="btn btn-success" onclick="openModalNuevaDivisa()">
    <i class="fas fa-plus me-2"></i>Nueva Divisa
</button>
```

##### 2. Gestión de Tipos de Cambio

**Ubicación:** `/configuracion?tab=tipos-cambio`

```html
<!-- Formulario de registro de tipo de cambio -->
<div class="card">
    <div class="card-header bg-primary text-white">
        <h5><i class="fas fa-exchange-alt me-2"></i>Registrar Tipo de Cambio</h5>
    </div>
    <div class="card-body">
        <form id="formTipoCambio">
            <div class="row">
                <div class="col-md-3">
                    <label>Divisa Origen</label>
                    <select name="divisaOrigen" class="form-select" required>
                        <option th:each="d : ${divisas}" 
                                th:value="${d.idDivisa}" 
                                th:text="${d.codigo + ' - ' + d.nombre}">
                        </option>
                    </select>
                </div>
                
                <div class="col-md-3">
                    <label>Divisa Destino</label>
                    <select name="divisaDestino" class="form-select" required>
                        <option th:each="d : ${divisas}" 
                                th:value="${d.idDivisa}" 
                                th:text="${d.codigo + ' - ' + d.nombre}">
                        </option>
                    </select>
                </div>
                
                <div class="col-md-2">
                    <label>Fecha</label>
                    <input type="date" name="fecha" class="form-control" 
                           th:value="${#dates.format(#dates.createNow(), 'yyyy-MM-dd')}" required>
                </div>
                
                <div class="col-md-2">
                    <label>Tasa</label>
                    <input type="number" name="tasa" class="form-control" 
                           step="0.000001" min="0" placeholder="19.850000" required>
                </div>
                
                <div class="col-md-2">
                    <label class="d-block">&nbsp;</label>
                    <button type="submit" class="btn btn-success w-100">
                        <i class="fas fa-save me-1"></i>Guardar
                    </button>
                </div>
            </div>
        </form>
    </div>
</div>

<!-- Tabla de histórico -->
<div class="card mt-4">
    <div class="card-header">
        <h5><i class="fas fa-history me-2"></i>Histórico de Tipos de Cambio</h5>
    </div>
    <div class="card-body">
        <div class="table-responsive">
            <table class="table table-sm table-hover">
                <thead>
                    <tr>
                        <th>Fecha</th>
                        <th>De</th>
                        <th>A</th>
                        <th class="text-end">Tasa</th>
                        <th>Fuente</th>
                        <th>Acciones</th>
                    </tr>
                </thead>
                <tbody>
                    <tr th:each="tc : ${tiposCambio}">
                        <td th:text="${#temporals.format(tc.fecha, 'dd/MM/yyyy')}">20/10/2025</td>
                        <td>
                            <span class="badge bg-info" 
                                  th:text="${tc.divisaOrigen.codigo}">USD</span>
                        </td>
                        <td>
                            <span class="badge bg-success" 
                                  th:text="${tc.divisaDestino.codigo}">MXN</span>
                        </td>
                        <td class="text-end">
                            <strong th:text="${#numbers.formatDecimal(tc.tasa, 1, 6)}">19.850000</strong>
                        </td>
                        <td>
                            <small th:text="${tc.fuente}">Manual</small>
                        </td>
                        <td>
                            <button class="btn btn-sm btn-warning" 
                                    th:onclick="'editarTipoCambio(' + ${tc.idTipoCambio} + ')'">
                                <i class="fas fa-edit"></i>
                            </button>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>
</div>
```

##### 3. Formulario de Factura con Divisa

**Actualización:** `templates/facturas/add-form.html`

```html
<!-- Agregar selector de divisa en Paso 1 -->
<div class="row">
    <div class="col-md-6 mb-3">
        <label for="divisa" class="form-label">
            <i class="fas fa-dollar-sign text-primary me-2"></i>Divisa <span class="text-danger">*</span>
        </label>
        <select name="divisa" id="divisa" class="form-select" required onchange="actualizarSimboloDivisa()">
            <option value="">Seleccione divisa</option>
            <option th:each="d : ${divisas}" 
                    th:value="${d.idDivisa}" 
                    th:text="${d.codigo + ' - ' + d.simbolo + ' - ' + d.nombre}"
                    th:attr="data-simbolo=${d.simbolo}, data-decimales=${d.decimales}">
            </option>
        </select>
        <small class="text-muted">
            Divisa en la que se emitirá la factura
        </small>
    </div>
    
    <div class="col-md-6 mb-3">
        <label for="tipoCambio" class="form-label">
            <i class="fas fa-exchange-alt text-primary me-2"></i>Tipo de Cambio
        </label>
        <input type="text" id="tipoCambio" class="form-control" readonly 
               placeholder="Se obtendrá automáticamente">
        <small class="text-muted" id="textoConversion">
            <!-- Se mostrará: 1 USD = 19.85 MXN -->
        </small>
    </div>
</div>
```

##### 4. Detalle de Factura con Conversión

**Modal de detalle actualizado:**

```html
<!-- Información de Divisa -->
<div class="card mt-3">
    <div class="card-header bg-info text-white">
        <h6 class="mb-0">
            <i class="fas fa-money-bill-wave me-2"></i>Información de Divisa
        </h6>
    </div>
    <div class="card-body">
        <div class="row">
            <div class="col-md-6">
                <p class="mb-1">
                    <strong>Divisa de Facturación:</strong>
                    <span class="badge bg-primary" th:text="${factura.divisa.codigo}">USD</span>
                    <span th:text="${factura.divisa.nombre}">Dólar Estadounidense</span>
                </p>
                <p class="mb-1">
                    <strong>Total en Divisa Original:</strong>
                    <span class="text-success h5" 
                          th:text="${factura.divisa.simbolo + ' ' + #numbers.formatDecimal(factura.total, 1, factura.divisa.decimales)}">
                        $1,500.00
                    </span>
                </p>
            </div>
            
            <div class="col-md-6">
                <p class="mb-1">
                    <strong>Tipo de Cambio Registrado:</strong>
                    <span th:text="${#numbers.formatDecimal(factura.tipoCambioRegistrado, 1, 6)}">19.850000</span>
                </p>
                <p class="mb-1">
                    <strong>Total en Divisa Maestra:</strong>
                    <span class="text-primary h5" 
                          th:text="${divisaMaestra.simbolo + ' ' + #numbers.formatDecimal(factura.totalDivisaMaestra, 1, 2)}">
                        MX$29,775.00
                    </span>
                </p>
                <small class="text-muted">
                    *Para reportes consolidados
                </small>
            </div>
        </div>
    </div>
</div>
```

---

#### 🔧 Servicios y Lógica de Negocio

##### 1. DivisaService

```java
package api.astro.whats_orders_manager.services;

import api.astro.whats_orders_manager.models.Divisa;
import java.util.List;
import java.util.Optional;

public interface DivisaService {
    
    List<Divisa> findAllActivas();
    
    Optional<Divisa> findById(Integer id);
    
    Optional<Divisa> findByCodigo(String codigo);
    
    Optional<Divisa> findDivisaMaestra();
    
    Divisa save(Divisa divisa);
    
    void establecerDivisaMaestra(Integer idDivisa);
    
    boolean existsByCodigo(String codigo);
}
```

##### 2. TipoCambioService

```java
package api.astro.whats_orders_manager.services;

import api.astro.whats_orders_manager.models.TipoCambio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TipoCambioService {
    
    /**
     * Obtiene el tipo de cambio vigente para una fecha específica
     */
    Optional<TipoCambio> findTipoCambioVigente(Integer divisaOrigenId, Integer divisaDestinoId, LocalDate fecha);
    
    /**
     * Obtiene el tipo de cambio vigente para HOY
     */
    Optional<TipoCambio> findTipoCambioActual(Integer divisaOrigenId, Integer divisaDestinoId);
    
    /**
     * Convierte un monto de una divisa a otra usando el tipo de cambio del día
     */
    BigDecimal convertir(BigDecimal monto, Integer divisaOrigenId, Integer divisaDestinoId, LocalDate fecha);
    
    /**
     * Registra un nuevo tipo de cambio
     */
    TipoCambio save(TipoCambio tipoCambio);
    
    /**
     * Obtiene histórico de tipos de cambio
     */
    List<TipoCambio> findHistorico(Integer divisaOrigenId, Integer divisaDestinoId, int limit);
    
    /**
     * Obtiene todos los tipos de cambio de hoy
     */
    List<TipoCambio> findTodosTipoCambioHoy();
}
```

##### 3. Actualizar FacturaService

```java
@Service
public class FacturaServiceImpl implements FacturaService {
    
    @Autowired
    private TipoCambioService tipoCambioService;
    
    @Autowired
    private DivisaService divisaService;
    
    @Override
    @Transactional
    public Factura save(Factura factura) {
        // ... código existente ...
        
        // Obtener divisa maestra
        Divisa divisaMaestra = divisaService.findDivisaMaestra()
            .orElseThrow(() -> new RuntimeException("No hay divisa maestra configurada"));
        
        // Si la factura es en divisa diferente a la maestra, convertir
        if (!factura.getDivisa().getIdDivisa().equals(divisaMaestra.getIdDivisa())) {
            
            // Obtener tipo de cambio del día
            TipoCambio tipoCambio = tipoCambioService
                .findTipoCambioActual(factura.getDivisa().getIdDivisa(), divisaMaestra.getIdDivisa())
                .orElseThrow(() -> new RuntimeException(
                    "No hay tipo de cambio registrado para " + factura.getDivisa().getCodigo() + 
                    " a " + divisaMaestra.getCodigo()
                ));
            
            // Registrar tipo de cambio usado
            factura.setTipoCambioRegistrado(tipoCambio.getTasa());
            
            // Calcular total en divisa maestra
            BigDecimal totalConvertido = factura.getTotal().multiply(tipoCambio.getTasa());
            factura.setTotalDivisaMaestra(totalConvertido);
            
            log.info("Factura convertida: {} {} = {} {} (TC: {})",
                factura.getTotal(), factura.getDivisa().getCodigo(),
                totalConvertido, divisaMaestra.getCodigo(),
                tipoCambio.getTasa());
                
        } else {
            // Factura en divisa maestra
            factura.setTipoCambioRegistrado(BigDecimal.ONE);
            factura.setTotalDivisaMaestra(factura.getTotal());
        }
        
        return facturaRepository.save(factura);
    }
}
```

---

#### 📊 Reportes Consolidados

##### ReporteService Actualizado

```java
@Service
public class ReporteServiceImpl implements ReporteService {
    
    @Override
    public BigDecimal calcularVentasTotales(LocalDate inicio, LocalDate fin) {
        // Obtener divisa maestra
        Divisa divisaMaestra = divisaService.findDivisaMaestra()
            .orElseThrow(() -> new RuntimeException("No hay divisa maestra"));
        
        // Sumar todas las facturas usando total_divisa_maestra
        List<Factura> facturas = facturaRepository
            .findByFechaEntregaBetween(inicio, fin);
        
        BigDecimal totalConsolidado = facturas.stream()
            .map(Factura::getTotalDivisaMaestra) // Usa el total convertido
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        log.info("Ventas totales ({} - {}): {} {}", 
            inicio, fin, divisaMaestra.getSimbolo(), totalConsolidado);
        
        return totalConsolidado;
    }
    
    @Override
    public Map<String, BigDecimal> calcularVentasPorDivisa(LocalDate inicio, LocalDate fin) {
        List<Factura> facturas = facturaRepository.findByFechaEntregaBetween(inicio, fin);
        
        return facturas.stream()
            .collect(Collectors.groupingBy(
                f -> f.getDivisa().getCodigo(),
                Collectors.reducing(
                    BigDecimal.ZERO,
                    Factura::getTotal,
                    BigDecimal::add
                )
            ));
    }
}
```

---

#### 🧪 Casos de Uso

##### Caso 1: Registrar Tipo de Cambio Diario

```
Usuario: Admin
Acción: Registra tipo de cambio USD -> MXN para hoy
Datos:
  - Divisa Origen: USD
  - Divisa Destino: MXN
  - Fecha: 2025-10-20
  - Tasa: 19.850000
  - Fuente: Banco de México

Resultado:
✅ Tipo de cambio registrado
✅ Disponible para facturas del día
```

##### Caso 2: Crear Factura en USD

```
Usuario: Vendedor
Acción: Crea factura para cliente con divisa USD
Datos:
  - Cliente: Juan Pérez (USA)
  - Divisa: USD
  - Productos: $1,500.00 USD
  
Sistema automáticamente:
  1. Busca tipo de cambio USD -> MXN del día (19.85)
  2. Registra TC en factura: 19.850000
  3. Calcula total en divisa maestra: $29,775.00 MXN
  4. Guarda ambos valores

Resultado:
✅ Factura emitida en USD
✅ Total original: $1,500.00 USD
✅ Total convertido: $29,775.00 MXN
✅ TC registrado: 19.850000
```

##### Caso 3: Ver Detalle de Factura

```
Usuario: Admin
Acción: Abre detalle de factura #125

Vista muestra:
┌─────────────────────────────────────────┐
│ 💵 Información de Divisa                │
├─────────────────────────────────────────┤
│ Divisa de Facturación: USD              │
│ Total Original: $1,500.00 USD           │
│                                         │
│ Tipo de Cambio: 19.850000               │
│ Total Consolidado: MX$29,775.00         │
│ *Para reportes en divisa maestra        │
└─────────────────────────────────────────┘

Resultado:
✅ Usuario ve ambos valores
✅ Transparencia en conversión
```

##### Caso 4: Reporte Consolidado

```
Usuario: Admin
Acción: Genera reporte de ventas del mes

Sistema:
  - Suma total_divisa_maestra de todas las facturas
  - Muestra en divisa maestra (MXN)

Reporte muestra:
  Facturas en USD: 10 facturas = MX$297,750.00
  Facturas en EUR: 5 facturas  = MX$106,000.00
  Facturas en MXN: 15 facturas = MX$450,000.00
  ───────────────────────────────────────
  TOTAL CONSOLIDADO:            MX$853,750.00

Resultado:
✅ Reporte en una sola divisa
✅ Fácil comparación
```

---

#### 📁 Archivos a Crear/Modificar

| Archivo | Acción | Descripción |
|---------|--------|-------------|
| `MIGRATION_DIVISAS.sql` | Crear | Script de migración de BD |
| `models/Divisa.java` | Crear | Entidad Divisa |
| `models/TipoCambio.java` | Crear | Entidad TipoCambio |
| `models/Factura.java` | Modificar | Agregar campos divisa |
| `repositories/DivisaRepository.java` | Crear | Repository de divisas |
| `repositories/TipoCambioRepository.java` | Crear | Repository tipos cambio |
| `services/DivisaService.java` | Crear | Interfaz servicio |
| `services/DivisaServiceImpl.java` | Crear | Implementación |
| `services/TipoCambioService.java` | Crear | Interfaz servicio |
| `services/TipoCambioServiceImpl.java` | Crear | Implementación |
| `services/FacturaServiceImpl.java` | Modificar | Agregar conversión |
| `services/ReporteServiceImpl.java` | Modificar | Usar divisa maestra |
| `controllers/DivisaController.java` | Crear | CRUD divisas |
| `controllers/TipoCambioController.java` | Crear | CRUD tipos cambio |
| `controllers/FacturaController.java` | Modificar | Enviar divisas al form |
| `templates/configuracion/configuracion.html` | Modificar | Tab de divisas |
| `templates/configuracion/divisas.html` | Crear | Vista gestión divisas |
| `templates/configuracion/tipos-cambio.html` | Crear | Vista tipos cambio |
| `templates/facturas/add-form.html` | Modificar | Selector de divisa |
| `templates/facturas/facturas.html` | Modificar | Mostrar divisa en tabla |
| `static/js/divisas.js` | Crear | JS gestión divisas |
| `static/js/tipos-cambio.js` | Crear | JS tipos cambio |
| `static/js/editar-factura.js` | Modificar | Obtener TC automático |

**Total:** 23 archivos (10 nuevos, 13 modificados)

---

#### ⚙️ Configuración Adicional

##### application.yml

```yaml
app:
  divisas:
    # Código de divisa maestra por defecto
    maestra-defecto: MXN
    
    # API externa para obtener tipos de cambio (opcional)
    api-tipo-cambio:
      enabled: false
      url: https://api.exchangerate-api.com/v4/latest/
      api-key: ${EXCHANGE_RATE_API_KEY:}
      
    # Días de vigencia de tipo de cambio
    dias-vigencia: 1
    
    # Decimales por defecto
    decimales-defecto: 2
```

---

#### 🔮 Mejoras Futuras de este Módulo

##### Fase 2: Integración con APIs Externas

```java
/**
 * Servicio para obtener tipos de cambio de APIs externas
 */
@Service
public class ExchangeRateApiService {
    
    @Value("${app.divisas.api-tipo-cambio.url}")
    private String apiUrl;
    
    @Value("${app.divisas.api-tipo-cambio.api-key}")
    private String apiKey;
    
    /**
     * Obtiene tipos de cambio actuales de API externa
     */
    public Map<String, BigDecimal> obtenerTiposCambioActuales(String divisaBase) {
        // Llamada a API externa (ej: exchangerate-api.com, fixer.io)
        // Retorna Map con pares divisa -> tasa
    }
    
    /**
     * Actualiza automáticamente tipos de cambio en BD
     */
    @Scheduled(cron = "0 0 9 * * MON-FRI") // Cada día a las 9 AM
    public void actualizarTiposCambioAutomatico() {
        // Obtiene tipos de cambio de API
        // Guarda en BD
        // Notifica si hay error
    }
}
```

##### Fase 3: Historial de Variación

```java
/**
 * Gráfica de evolución del tipo de cambio
 */
@GetMapping("/api/divisas/grafica/{divisaOrigenId}/{divisaDestinoId}")
public ResponseEntity<List<TipoCambioDTO>> getGraficaTipoCambio(
    @PathVariable Integer divisaOrigenId,
    @PathVariable Integer divisaDestinoId,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
) {
    // Retorna datos para Chart.js
}
```

##### Fase 4: Alertas de Variación

```java
/**
 * Notifica si el tipo de cambio varía más del X%
 */
@Service
public class AlertaTipoCambioService {
    
    public void verificarVariacionSignificativa(TipoCambio nuevoTC) {
        TipoCambio anterior = tipoCambioService.findTipoCambioAnterior(
            nuevoTC.getDivisaOrigen(), 
            nuevoTC.getDivisaDestino()
        );
        
        if (anterior != null) {
            BigDecimal variacion = calcularVariacionPorcentual(anterior.getTasa(), nuevoTC.getTasa());
            
            if (variacion.abs().compareTo(new BigDecimal("5.0")) > 0) {
                // Variación mayor a 5%
                notificacionService.enviarAlerta(
                    "Variación significativa en tipo de cambio",
                    String.format("%s -> %s: %.2f%%", 
                        nuevoTC.getDivisaOrigen().getCodigo(),
                        nuevoTC.getDivisaDestino().getCodigo(),
                        variacion)
                );
            }
        }
    }
}
```

---

#### ⏱️ Estimación de Tiempo

| Tarea | Horas |
|-------|-------|
| Diseño de BD y migraciones | 3-4h |
| Modelos Java (Divisa, TipoCambio) | 2-3h |
| Repositories y Services | 4-6h |
| Controllers (CRUD completo) | 4-5h |
| Vistas HTML (gestión divisas + TC) | 6-8h |
| JavaScript (formularios, validaciones) | 4-5h |
| Integración con facturas | 4-6h |
| Actualizar reportes | 3-4h |
| Testing (unitario + integración) | 6-8h |
| Documentación | 2-3h |
| **TOTAL** | **38-52 horas** |

**Estimación:** 5-7 días de desarrollo

---

#### ✅ Checklist de Implementación

**Fase 1: Base de Datos**
- [ ] Crear tabla `divisa`
- [ ] Crear tabla `tipo_cambio`
- [ ] Modificar tabla `factura`
- [ ] Crear triggers de conversión automática
- [ ] Insertar divisas iniciales (MXN, USD, EUR, GTQ, HNL)
- [ ] Insertar tipos de cambio de prueba

**Fase 2: Backend - Modelos**
- [ ] Crear entidad `Divisa.java`
- [ ] Crear entidad `TipoCambio.java`
- [ ] Actualizar entidad `Factura.java`
- [ ] Crear DTOs necesarios

**Fase 3: Backend - Persistencia**
- [ ] Crear `DivisaRepository.java`
- [ ] Crear `TipoCambioRepository.java`
- [ ] Agregar queries personalizados

**Fase 4: Backend - Servicios**
- [ ] Crear `DivisaService` + Impl
- [ ] Crear `TipoCambioService` + Impl
- [ ] Actualizar `FacturaService`
- [ ] Actualizar `ReporteService`

**Fase 5: Backend - Controllers**
- [ ] Crear `DivisaController`
- [ ] Crear `TipoCambioController`
- [ ] Actualizar `FacturaController`
- [ ] Actualizar `ReporteController`
- [ ] Crear endpoints API REST

**Fase 6: Frontend - Vistas**
- [ ] Vista gestión de divisas
- [ ] Vista gestión de tipos de cambio
- [ ] Actualizar formulario de factura
- [ ] Actualizar modal de detalle
- [ ] Actualizar tabla de facturas
- [ ] Actualizar reportes

**Fase 7: Frontend - JavaScript**
- [ ] `divisas.js` - CRUD divisas
- [ ] `tipos-cambio.js` - CRUD tipos cambio
- [ ] Actualizar `editar-factura.js`
- [ ] Validaciones de formularios
- [ ] Cálculos automáticos de conversión

**Fase 8: Testing**
- [ ] Tests unitarios de servicios
- [ ] Tests de conversión de divisas
- [ ] Tests de triggers de BD
- [ ] Tests de validación de unicidad
- [ ] Tests de reportes consolidados
- [ ] Tests E2E de flujo completo

**Fase 9: Documentación**
- [ ] Documentar modelo de datos
- [ ] Documentar API REST
- [ ] Manual de usuario (gestión divisas)
- [ ] Manual de usuario (tipos de cambio)
- [ ] Guía de migración

---

#### 🎯 Beneficios Esperados

✅ **Facturación Internacional:** Emitir facturas en la divisa del cliente  
✅ **Reportes Consolidados:** Análisis en una sola divisa maestra  
✅ **Trazabilidad:** Histórico de tipos de cambio usados  
✅ **Transparencia:** Cliente ve factura en su divisa + conversión  
✅ **Flexibilidad:** Soporte para múltiples mercados  
✅ **Precisión:** Tipo de cambio registrado al momento de facturar  
✅ **Cumplimiento:** Registro histórico para auditorías  

---

**Notas Importantes:**
- Esta funcionalidad es **crítica** para negocios internacionales
- Requiere **coordinación** con equipo contable para definir divisa maestra
- Se recomienda **backup** de BD antes de migración
- Considerar **zona horaria** para tipos de cambio (UTC vs local)
- Evaluar si se necesita **API externa** o registro manual

---

### 2. CAMBIAR USERNAME DE TELÉFONO A NOMBRE DE USUARIO

**Estado:** 📝 Planificado  
**Prioridad:** Alta  
**Sprint sugerido:** Sprint 2 o 3  
**Fecha de solicitud:** 12/10/2025

#### Descripción
Actualmente el sistema usa el **número de teléfono** como username para autenticación. Se requiere implementar un **campo username único** independiente del teléfono.

#### Problema Actual
```java
// UserDetailsServiceImpl.java
@Override
public UserDetails loadUserByUsername(String nombre) {
    Usuario usuario = usuarioRepository.findByNombre(nombre)
        .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + nombre));
    
    return User.withUsername(usuario.getTelefono())  // ← USA TELÉFONO
            .password(usuario.getPassword())
            .roles(usuario.getRol())
            .build();
}
```

```java
// SecurityConfig.java
.formLogin(form -> form
    .loginPage("/auth/login")
    .usernameParameter("telefono")  // ← PARÁMETRO ES TELÉFONO
    .passwordParameter("password")
    // ...
)
```

#### Solución Propuesta

##### 1. Modificar Modelo Usuario

```java
// Usuario.java
@Entity
@Table(name = "usuario")
public class Usuario implements Serializable {
    
    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;  // ← NUEVO CAMPO
    
    @Column(name = "telefono", unique = true, length = 20)
    private String telefono;  // Deja de ser username, solo dato de contacto
    
    // ... resto de campos
}
```

##### 2. Migración SQL

```sql
-- Fase: Agregar campo username
ALTER TABLE usuario ADD COLUMN username VARCHAR(50) UNIQUE;

-- Generar username temporal basado en nombre o teléfono
UPDATE usuario 
SET username = CONCAT('user', id_usuario) 
WHERE username IS NULL;

-- Hacer NOT NULL después de poblar
ALTER TABLE usuario MODIFY COLUMN username VARCHAR(50) NOT NULL;

-- Crear índice
CREATE UNIQUE INDEX idx_usuario_username ON usuario(username);
```

##### 3. Actualizar Repository

```java
// UsuarioRepository.java
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    Optional<Usuario> findByUsername(String username);  // ← NUEVO
    Optional<Usuario> findByTelefono(String telefono);  // Mantener
    Optional<Usuario> findByEmail(String email);
    
    boolean existsByUsername(String username);  // ← NUEVO
    boolean existsByTelefono(String telefono);
    boolean existsByEmail(String email);
}
```

##### 4. Actualizar UserDetailsService

```java
// UserDetailsServiceImpl.java
@Override
public UserDetails loadUserByUsername(String username) {  // ← CAMBIA DE nombre a username
    Usuario usuario = usuarioRepository.findByUsername(username)  // ← USA USERNAME
        .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

    actualizarUltimoAcceso(usuario);

    return User.withUsername(usuario.getUsername())  // ← USA USERNAME
            .password(usuario.getPassword())
            .roles(usuario.getRol())
            .build();
}
```

##### 5. Actualizar SecurityConfig

```java
// SecurityConfig.java
.formLogin(form -> form
    .loginPage("/auth/login")
    .usernameParameter("username")  // ← CAMBIAR DE "telefono" A "username"
    .passwordParameter("password")
    .defaultSuccessUrl("/dashboard", true)
    .failureUrl("/auth/login?error=true")
    .permitAll()
)
```

##### 6. Actualizar Vista de Login

```html
<!-- templates/auth/login.html -->
<form method="POST" th:action="@{/auth/login}">
    <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
    
    <!-- ❌ ANTES -->
    <input type="tel" name="telefono" placeholder="Número de teléfono" required>
    
    <!-- ✅ DESPUÉS -->
    <input type="text" name="username" placeholder="Nombre de usuario" required 
           pattern="[a-zA-Z0-9_]{3,50}" 
           title="3-50 caracteres: letras, números y guión bajo">
    
    <input type="password" name="password" placeholder="Contraseña" required>
    <button type="submit">Iniciar Sesión</button>
</form>
```

##### 7. Actualizar Vista de Registro

```html
<!-- templates/auth/register.html -->
<form method="POST" th:action="@{/auth/register}" th:object="${usuario}">
    
    <!-- NUEVO CAMPO -->
    <div class="form-group">
        <label for="username">Nombre de Usuario *</label>
        <input type="text" id="username" th:field="*{username}" 
               class="form-control" required
               pattern="[a-zA-Z0-9_]{3,50}"
               minlength="3" maxlength="50"
               placeholder="ej. juan_perez">
        <small>3-50 caracteres: solo letras, números y guión bajo</small>
        <span th:if="${#fields.hasErrors('username')}" 
              th:errors="*{username}" 
              class="error"></span>
    </div>
    
    <!-- Teléfono ahora es solo un dato de contacto -->
    <div class="form-group">
        <label for="telefono">Teléfono *</label>
        <input type="tel" id="telefono" th:field="*{telefono}" 
               class="form-control" required>
    </div>
    
    <!-- ... resto del formulario -->
</form>
```

##### 8. Actualizar AuthController

```java
// AuthController.java
@PostMapping("/register")
public String register(@Valid @ModelAttribute Usuario usuario, 
                       BindingResult result, 
                       Model model) {
    
    // Validar username único
    if (usuarioService.existsByUsername(usuario.getUsername())) {
        result.rejectValue("username", "error.usuario", 
                          "El nombre de usuario ya está en uso");
    }
    
    // Validar teléfono único (ya no es username)
    if (usuarioService.existsByTelefono(usuario.getTelefono())) {
        result.rejectValue("telefono", "error.usuario", 
                          "El teléfono ya está registrado");
    }
    
    // Validar email único
    if (usuarioService.existsByEmail(usuario.getEmail())) {
        result.rejectValue("email", "error.usuario", 
                          "El email ya está registrado");
    }
    
    if (result.hasErrors()) {
        return "auth/register";
    }
    
    // ... resto del código
}
```

##### 9. Actualizar Perfil

```html
<!-- templates/perfil/ver.html -->
<div class="info-row">
    <div class="info-label">
        <i class="fas fa-user"></i>
        Nombre de Usuario
    </div>
    <div class="info-value">
        <span th:text="${usuario.username}">juan_perez</span>
    </div>
</div>

<div class="info-row">
    <div class="info-label">
        <i class="fas fa-phone"></i>
        Teléfono
    </div>
    <div class="info-value">
        <span th:text="${usuario.telefono}">+52 999 123 4567</span>
    </div>
</div>
```

#### Archivos a Modificar

| Archivo | Tipo | Cambio |
|---------|------|--------|
| `models/Usuario.java` | Backend | Agregar campo `username` |
| `repositories/UsuarioRepository.java` | Backend | Agregar métodos `findByUsername`, `existsByUsername` |
| `services/UsuarioService.java` | Backend | Agregar métodos username |
| `services/UsuarioServiceImpl.java` | Backend | Implementar métodos username |
| `security/UserDetailsServiceImpl.java` | Backend | Cambiar `findByNombre` a `findByUsername` |
| `config/SecurityConfig.java` | Backend | Cambiar `usernameParameter("telefono")` a `usernameParameter("username")` |
| `controllers/AuthController.java` | Backend | Validar `username` único en registro |
| `controllers/PerfilController.java` | Backend | Permitir editar `username` |
| `templates/auth/login.html` | Frontend | Cambiar campo teléfono a username |
| `templates/auth/register.html` | Frontend | Agregar campo username |
| `templates/perfil/ver.html` | Frontend | Mostrar username |
| `templates/perfil/editar.html` | Frontend | Editar username |
| `MIGRATION_USERNAME.sql` | Database | Script de migración |

**Total:** 13 archivos

#### Validaciones Necesarias

```java
// Usuario.java
@Column(name = "username", unique = true, nullable = false, length = 50)
@Pattern(regexp = "^[a-zA-Z0-9_]{3,50}$", 
         message = "Username debe tener 3-50 caracteres (solo letras, números y _)")
private String username;
```

#### Testing Requerido

- [ ] Login con username funciona
- [ ] Registro con username único
- [ ] Username duplicado rechazado
- [ ] Editar username en perfil
- [ ] Username inválido rechazado (caracteres especiales)
- [ ] Migración de datos existentes correcta
- [ ] Último acceso se actualiza con nuevo sistema
- [ ] Spring Security reconoce username

#### Estimación
- **Desarrollo:** 4-6 horas
- **Testing:** 2-3 horas
- **Migración datos:** 1 hora
- **Documentación:** 1 hora
- **TOTAL:** ~8-11 horas (1-1.5 días)

#### Riesgos
- ⚠️ Migración de usuarios existentes (necesitan username temporal)
- ⚠️ Sesiones activas pueden invalidarse
- ⚠️ Código que referencia teléfono como username

#### Dependencias
- Base de datos debe soportar ALTER TABLE
- Backup antes de migración
- Notificar usuarios del cambio

---

## 🟡 MEJORAS DE MEDIA PRIORIDAD

### 2. Migrar de Timestamp a LocalDateTime

**Estado:** 📝 Planificado  
**Sprint sugerido:** Sprint 3

#### Descripción
Cambiar de `java.sql.Timestamp` a `java.time.LocalDateTime` en el modelo `Usuario` para mejor compatibilidad con Thymeleaf y estándares modernos.

#### Cambios

```java
// Usuario.java - ANTES
@Column(name = "ultimo_acceso")
private Timestamp ultimoAcceso;

@CreatedDate
@Column(name = "createDate", updatable = false)
private Timestamp createDate;

@CreatedDate
@Column(name = "updateDate")
private Timestamp updateDate;
```

```java
// Usuario.java - DESPUÉS
@Column(name = "ultimo_acceso")
private LocalDateTime ultimoAcceso;

@CreatedDate
@Column(name = "createDate", updatable = false)
private LocalDateTime createDate;

@LastModifiedDate
@Column(name = "updateDate")
private LocalDateTime updateDate;
```

```java
// UserDetailsServiceImpl.java - ANTES
usuario.setUltimoAcceso(new Timestamp(System.currentTimeMillis()));

// DESPUÉS
usuario.setUltimoAcceso(LocalDateTime.now());
```

```html
<!-- Templates - Thymeleaf - CAMBIO -->
<!-- Poder usar #temporals en lugar de #dates -->
th:text="${#temporals.format(usuario.createDate, 'dd/MM/yyyy HH:mm')}"
```

**Archivos a modificar:** 3-4  
**Estimación:** 2-3 horas

---

### 3. Implementar Remember Me

**Estado:** 📝 Planificado  
**Sprint sugerido:** Sprint 2-3

#### Descripción
Agregar funcionalidad "Recordarme" en login para mantener sesión por más tiempo.

```java
// SecurityConfig.java
.rememberMe(remember -> remember
    .key("uniqueAndSecret")
    .tokenValiditySeconds(86400 * 7)  // 7 días
    .userDetailsService(userDetailsService)
)
```

```html
<!-- login.html -->
<div class="form-check">
    <input type="checkbox" name="remember-me" id="remember-me" class="form-check-input">
    <label for="remember-me" class="form-check-label">Recordarme</label>
</div>
```

**Archivos a modificar:** 2  
**Estimación:** 1-2 horas

---

### 4. Internacionalización (i18n)

**Estado:** 📝 Planificado  
**Sprint sugerido:** Sprint 4+

#### Descripción
Soporte para múltiples idiomas (Español, Inglés).

```java
// messages_es.properties
login.title=Iniciar Sesión
login.username=Nombre de Usuario
login.password=Contraseña
login.submit=Entrar

// messages_en.properties
login.title=Login
login.username=Username
login.password=Password
login.submit=Sign In
```

**Archivos a crear:** ~20 (messages properties)  
**Estimación:** 8-12 horas

---

## 🟢 MEJORAS DE BAJA PRIORIDAD

### 5. Historial de Accesos

**Estado:** 💭 Idea  
**Sprint sugerido:** Sprint 5+

Tabla `accesos` para registrar todos los logins:
```sql
CREATE TABLE accesos (
    id_acceso BIGINT PRIMARY KEY AUTO_INCREMENT,
    id_usuario BIGINT,
    fecha_acceso TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    exitoso BOOLEAN,
    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
);
```

---

### 6. Two-Factor Authentication (2FA)

**Estado:** 💭 Idea  
**Sprint sugerido:** Sprint 6+

Implementar autenticación de dos factores con TOTP (Google Authenticator).

---

### 7. Social Login (OAuth2)

**Estado:** 💭 Idea  
**Sprint sugerido:** Sprint 7+

Login con Google, Facebook, Microsoft.

---

## 🔧 REFACTORIZACIONES TÉCNICAS

### 8. Separar Configuración de Seguridad

**Estado:** 📝 Planificado  
**Sprint sugerido:** Sprint 3

Dividir `SecurityConfig.java` en múltiples archivos:
- `SecurityConfig.java` - Configuración principal
- `SecurityAuthenticationConfig.java` - Authentication manager
- `SecurityAuthorizationConfig.java` - Permisos y roles
- `SecuritySessionConfig.java` - Session management

**Estimación:** 3-4 horas

---

### 9. Crear DTOs para Responses

**Estado:** 📝 Planificado  
**Sprint sugerido:** Sprint 3

Separar modelos de base de datos de DTOs de respuesta:
```java
// UsuarioDTO.java
public class UsuarioDTO {
    private Long id;
    private String username;
    private String email;
    private String telefono;
    private String rol;
    // NO incluir password
}
```

**Estimación:** 4-6 horas

---

## 🆕 NUEVAS FUNCIONALIDADES

### 10. Dashboard con Gráficas Reales

**Estado:** 📝 Planificado  
**Sprint sugerido:** Sprint 2

Implementar gráficas con Chart.js:
- Ventas por mes
- Productos más vendidos
- Clientes frecuentes
- Estado de facturas (pie chart)

**Estimación:** 8-12 horas

---

### 11. Notificaciones en Tiempo Real

**Estado:** 💭 Idea  
**Sprint sugerido:** Sprint 5+

Implementar WebSocket para notificaciones:
- Nueva factura creada
- Pago recibido
- Producto sin stock
- Mensaje de cliente

**Estimación:** 16-20 horas

---

### 12. Exportación de Reportes

**Estado:** 📝 Planificado  
**Sprint sugerido:** Sprint 3-4

Exportar datos a:
- PDF (iText o JasperReports)
- Excel (Apache POI)
- CSV

**Estimación:** 12-16 horas

---

## 📊 RESUMEN DE PRIORIDADES

```
ALTA PRIORIDAD (Sprint 2-3):
├── ✅ #1: Username en lugar de teléfono (8-11 horas)
├── ⏳ #3: Remember Me (1-2 horas)
└── ⏳ #10: Gráficas reales en Dashboard (8-12 horas)

MEDIA PRIORIDAD (Sprint 3-4):
├── ⏳ #2: Migrar a LocalDateTime (2-3 horas)
├── ⏳ #8: Refactorizar SecurityConfig (3-4 horas)
├── ⏳ #9: Crear DTOs (4-6 horas)
└── ⏳ #12: Exportación de reportes (12-16 horas)

BAJA PRIORIDAD (Sprint 4+):
├── 💭 #4: Internacionalización (8-12 horas)
├── 💭 #5: Historial de accesos (6-8 horas)
├── 💭 #6: Two-Factor Authentication (16-20 horas)
├── 💭 #7: Social Login (20-24 horas)
└── 💭 #11: Notificaciones en tiempo real (16-20 horas)
```

---

## 📝 NOTAS

- Todas las estimaciones son aproximadas
- Prioridades pueden cambiar según necesidades del negocio
- Algunas mejoras pueden combinarse en un mismo sprint
- Testing y documentación incluidos en estimaciones

---

**Última actualización:** 12 de octubre de 2025  
**Responsable:** GitHub Copilot Agent  
**Estado del documento:** 🟢 Activo
