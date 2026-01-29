# 🏭 FASE 3: Proveedores (Cuentas por Pagar)

**Sprint:** 6  
**Fase:** 3 de 5  
**Duración estimada:** 6-8 días  
**Prioridad:** ⭐⭐ ALTA  
**Estado:** 📋 PENDIENTE (0/44 tareas)

---

## 📋 OBJETIVO DE LA FASE

Implementar gestión completa de proveedores y cuentas por pagar:
- Catálogo de proveedores
- Órdenes de compra
- Recepción de mercancía
- Cuentas por pagar
- Pagos a proveedores
- Historial de compras
- Evaluación de proveedores
- Conciliación de cuentas

---

## 📊 PROGRESO GENERAL

```
Progreso: [0/44] ░░░░░░░░░░░░░░░░░░░░ 0%

├─ 1. Modelo de Datos                   [0/12] ░░░░░░░░░░ 0%
├─ 2. Órdenes de Compra                 [0/10] ░░░░░░░░░░ 0%
├─ 3. Cuentas por Pagar                 [0/10] ░░░░░░░░░░ 0%
├─ 4. Pagos a Proveedores               [0/6]  ░░░░░░░░░░ 0%
├─ 5. Reportes y Análisis               [0/4]  ░░░░░░░░░░ 0%
└─ 6. Interfaz de Usuario               [0/2]  ░░░░░░░░░░ 0%
```

---

## 📦 1. MODELO DE DATOS (12 tareas)

### 1.1. Entidad `Proveedor.java`

**Archivo:** `src/main/java/com/erp/model/Proveedor.java`

#### Tareas:

- [ ] **1.1.1** Crear entidad `Proveedor`

```java
package com.erp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Entidad para gestión de proveedores.
 * 
 * @author ERP Team
 * @version 6.0
 * @since Sprint 6
 */
@Entity
@Table(name = "proveedores")
@Data
public class Proveedor {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Cédula jurídica del proveedor
     */
    @Column(unique = true, nullable = false, length = 20)
    private String cedula;
    
    /**
     * Nombre o razón social
     */
    @Column(nullable = false, length = 200)
    private String nombre;
    
    /**
     * Nombre comercial
     */
    @Column(name = "nombre_comercial", length = 200)
    private String nombreComercial;
    
    /**
     * Tipo de proveedor
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_proveedor", nullable = false, length = 30)
    private TipoProveedor tipoProveedor;
    
    /**
     * Categoría del proveedor
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CategoriaProveedor categoria;
    
    // Información de contacto
    @Column(length = 100)
    private String contacto;
    
    @Column(length = 15)
    private String telefono;
    
    @Column(length = 100)
    private String email;
    
    @Column(length = 100)
    private String sitioWeb;
    
    // Dirección
    @Column(length = 500)
    private String direccion;
    
    @Column(length = 100)
    private String ciudad;
    
    @Column(length = 50)
    private String pais = "Costa Rica";
    
    // Información financiera
    /**
     * Moneda preferida del proveedor
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "moneda_id")
    private Moneda moneda;
    
    /**
     * Días de crédito otorgados
     */
    @Column(name = "dias_credito")
    private Integer diasCredito = 0;
    
    /**
     * Límite de crédito
     */
    @Column(name = "limite_credito", precision = 19, scale = 2)
    private BigDecimal limiteCredito;
    
    /**
     * Saldo actual por pagar
     */
    @Column(name = "saldo_pendiente", precision = 19, scale = 2)
    private BigDecimal saldoPendiente = BigDecimal.ZERO;
    
    /**
     * Descuento habitual otorgado (%)
     */
    @Column(name = "descuento_habitual", precision = 5, scale = 2)
    private BigDecimal descuentoHabitual = BigDecimal.ZERO;
    
    // Evaluación del proveedor
    /**
     * Calificación del proveedor (1-5 estrellas)
     */
    @Column(precision = 3, scale = 2)
    private BigDecimal calificacion;
    
    /**
     * Número de compras realizadas
     */
    @Column(name = "total_compras")
    private Integer totalCompras = 0;
    
    // Información bancaria
    @Column(name = "banco", length = 100)
    private String banco;
    
    @Column(name = "numero_cuenta", length = 50)
    private String numeroCuenta;
    
    @Column(name = "cuenta_iban", length = 50)
    private String cuentaIban;
    
    @Column(name = "sinpe_movil", length = 15)
    private String sinpeMovil;
    
    /**
     * Estado del proveedor
     */
    @Column(nullable = false)
    private Boolean activo = true;
    
    /**
     * Notas adicionales
     */
    @Column(length = 1000)
    private String notas;
    
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
}

/**
 * Tipos de proveedor
 */
enum TipoProveedor {
    NACIONAL,       // Proveedor local
    INTERNACIONAL,  // Proveedor extranjero
    IMPORTADOR,     // Importador que revende
    FABRICANTE,     // Fabricante directo
    DISTRIBUIDOR    // Distribuidor autorizado
}

/**
 * Categorías de proveedor
 */
enum CategoriaProveedor {
    MATERIA_PRIMA,      // Provee materias primas
    PRODUCTOS_TERMINADOS, // Provee productos terminados
    SERVICIOS,          // Provee servicios
    INSUMOS,           // Provee insumos
    EQUIPOS,           // Provee equipos
    TECNOLOGIA,        // Provee tecnología
    OTROS              // Otros
}
```

- [ ] **1.1.2** Crear migration SQL para `proveedores`

```sql
-- Migration: MIGRATION_PROVEEDORES_SPRINT_6.sql

CREATE TABLE proveedores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cedula VARCHAR(20) UNIQUE NOT NULL,
    nombre VARCHAR(200) NOT NULL,
    nombre_comercial VARCHAR(200),
    tipo_proveedor VARCHAR(30) NOT NULL,
    categoria VARCHAR(30),
    
    -- Contacto
    contacto VARCHAR(100),
    telefono VARCHAR(15),
    email VARCHAR(100),
    sitio_web VARCHAR(100),
    
    -- Dirección
    direccion VARCHAR(500),
    ciudad VARCHAR(100),
    pais VARCHAR(50) DEFAULT 'Costa Rica',
    
    -- Financiero
    moneda_id BIGINT,
    dias_credito INT DEFAULT 0,
    limite_credito DECIMAL(19,2),
    saldo_pendiente DECIMAL(19,2) DEFAULT 0.00,
    descuento_habitual DECIMAL(5,2) DEFAULT 0.00,
    
    -- Evaluación
    calificacion DECIMAL(3,2),
    total_compras INT DEFAULT 0,
    
    -- Bancario
    banco VARCHAR(100),
    numero_cuenta VARCHAR(50),
    cuenta_iban VARCHAR(50),
    sinpe_movil VARCHAR(15),
    
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    notas TEXT,
    
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (moneda_id) REFERENCES monedas(id),
    
    INDEX idx_cedula (cedula),
    INDEX idx_nombre (nombre),
    INDEX idx_activo (activo),
    INDEX idx_tipo (tipo_proveedor)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

- [ ] **1.1.3** Insertar datos de proveedores de ejemplo

```sql
-- Datos de ejemplo de proveedores
INSERT INTO proveedores (cedula, nombre, nombre_comercial, tipo_proveedor, categoria, telefono, email, moneda_id, dias_credito, activo) VALUES
('3-101-123456', 'Distribuidora Nacional S.A.', 'DistriNacional', 'NACIONAL', 'PRODUCTOS_TERMINADOS', '2222-3333', 'ventas@distrinacional.cr', (SELECT id FROM monedas WHERE codigo = 'CRC'), 30, TRUE),
('3-101-234567', 'Importadora Costa Rica Ltda', 'ImportCR', 'IMPORTADOR', 'MATERIA_PRIMA', '2222-4444', 'compras@importcr.cr', (SELECT id FROM monedas WHERE codigo = 'USD'), 60, TRUE),
('3-101-345678', 'Servicios Tecnológicos S.A.', 'TechServices', 'NACIONAL', 'TECNOLOGIA', '2222-5555', 'info@techservices.cr', (SELECT id FROM monedas WHERE codigo = 'CRC'), 15, TRUE);
```

---

### 1.2. Entidad `OrdenCompra.java`

#### Tareas:

- [ ] **1.2.1** Crear entidad `OrdenCompra`

```java
package com.erp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Orden de compra a proveedor.
 * 
 * @author ERP Team
 * @version 6.0
 * @since Sprint 6
 */
@Entity
@Table(name = "ordenes_compra")
@Data
public class OrdenCompra {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Número consecutivo de la orden
     */
    @Column(unique = true, nullable = false, length = 20)
    private String numero;
    
    /**
     * Proveedor
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;
    
    /**
     * Fecha de emisión
     */
    @Column(nullable = false)
    private LocalDate fecha;
    
    /**
     * Fecha esperada de entrega
     */
    @Column(name = "fecha_entrega_esperada")
    private LocalDate fechaEntregaEsperada;
    
    /**
     * Fecha real de entrega
     */
    @Column(name = "fecha_entrega_real")
    private LocalDate fechaEntregaReal;
    
    /**
     * Moneda de la orden
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "moneda_id", nullable = false)
    private Moneda moneda;
    
    /**
     * Tipo de cambio al momento de la orden
     */
    @Column(name = "tipo_cambio", precision = 19, scale = 6)
    private BigDecimal tipoCambio = BigDecimal.ONE;
    
    /**
     * Subtotal de la orden
     */
    @Column(precision = 19, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;
    
    /**
     * Descuento aplicado
     */
    @Column(precision = 19, scale = 2)
    private BigDecimal descuento = BigDecimal.ZERO;
    
    /**
     * IVA (si aplica)
     */
    @Column(precision = 19, scale = 2)
    private BigDecimal iva = BigDecimal.ZERO;
    
    /**
     * Total de la orden
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;
    
    /**
     * Detalles de la orden
     */
    @OneToMany(mappedBy = "ordenCompra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleOrdenCompra> detalles = new ArrayList<>();
    
    /**
     * Estado de la orden
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoOrdenCompra estado = EstadoOrdenCompra.PENDIENTE;
    
    /**
     * Condiciones de pago
     */
    @Column(name = "condiciones_pago", length = 500)
    private String condicionesPago;
    
    /**
     * Observaciones
     */
    @Column(length = 1000)
    private String observaciones;
    
    /**
     * Usuario que creó la orden
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
    
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
}

/**
 * Estados de orden de compra
 */
enum EstadoOrdenCompra {
    PENDIENTE,        // Creada, no enviada
    ENVIADA,          // Enviada al proveedor
    CONFIRMADA,       // Confirmada por proveedor
    RECIBIDA_PARCIAL, // Recibida parcialmente
    RECIBIDA_TOTAL,   // Recibida completamente
    CANCELADA         // Cancelada
}
```

- [ ] **1.2.2** Crear `DetalleOrdenCompra.java`

```java
@Entity
@Table(name = "detalles_orden_compra")
@Data
public class DetalleOrdenCompra {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_compra_id", nullable = false)
    private OrdenCompra ordenCompra;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;
    
    /**
     * Cantidad solicitada
     */
    @Column(name = "cantidad_solicitada", nullable = false)
    private Integer cantidadSolicitada;
    
    /**
     * Cantidad recibida
     */
    @Column(name = "cantidad_recibida")
    private Integer cantidadRecibida = 0;
    
    /**
     * Precio unitario
     */
    @Column(name = "precio_unitario", nullable = false, precision = 19, scale = 2)
    private BigDecimal precioUnitario;
    
    /**
     * Descuento por línea (%)
     */
    @Column(name = "descuento_porcentaje", precision = 5, scale = 2)
    private BigDecimal descuentoPorcentaje = BigDecimal.ZERO;
    
    /**
     * Subtotal de la línea
     */
    @Column(precision = 19, scale = 2)
    private BigDecimal subtotal;
    
    @PrePersist
    @PreUpdate
    protected void calcularSubtotal() {
        BigDecimal precio = precioUnitario.multiply(BigDecimal.valueOf(cantidadSolicitada));
        BigDecimal descuento = precio.multiply(descuentoPorcentaje).divide(new BigDecimal("100"));
        subtotal = precio.subtract(descuento);
    }
}
```

- [ ] **1.2.3** Crear migration SQL para `ordenes_compra`

```sql
CREATE TABLE ordenes_compra (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero VARCHAR(20) UNIQUE NOT NULL,
    proveedor_id BIGINT NOT NULL,
    fecha DATE NOT NULL,
    fecha_entrega_esperada DATE,
    fecha_entrega_real DATE,
    moneda_id BIGINT NOT NULL,
    tipo_cambio DECIMAL(19,6) DEFAULT 1.00,
    subtotal DECIMAL(19,2) DEFAULT 0.00,
    descuento DECIMAL(19,2) DEFAULT 0.00,
    iva DECIMAL(19,2) DEFAULT 0.00,
    total DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    condiciones_pago VARCHAR(500),
    observaciones TEXT,
    usuario_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (proveedor_id) REFERENCES proveedores(id),
    FOREIGN KEY (moneda_id) REFERENCES monedas(id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    
    INDEX idx_numero (numero),
    INDEX idx_proveedor (proveedor_id),
    INDEX idx_estado (estado),
    INDEX idx_fecha (fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE detalles_orden_compra (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    orden_compra_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad_solicitada INT NOT NULL,
    cantidad_recibida INT DEFAULT 0,
    precio_unitario DECIMAL(19,2) NOT NULL,
    descuento_porcentaje DECIMAL(5,2) DEFAULT 0.00,
    subtotal DECIMAL(19,2),
    
    FOREIGN KEY (orden_compra_id) REFERENCES ordenes_compra(id),
    FOREIGN KEY (producto_id) REFERENCES productos(id),
    
    INDEX idx_orden (orden_compra_id),
    INDEX idx_producto (producto_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 1.3. Entidad `CuentaPorPagar.java`

#### Tareas:

- [ ] **1.3.1** Crear entidad `CuentaPorPagar`

```java
package com.erp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Cuenta por pagar a proveedor.
 * 
 * @author ERP Team
 * @version 6.0
 * @since Sprint 6
 */
@Entity
@Table(name = "cuentas_por_pagar")
@Data
public class CuentaPorPagar {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Número consecutivo
     */
    @Column(unique = true, nullable = false, length = 20)
    private String numero;
    
    /**
     * Proveedor
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;
    
    /**
     * Orden de compra asociada
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_compra_id")
    private OrdenCompra ordenCompra;
    
    /**
     * Número de factura del proveedor
     */
    @Column(name = "factura_proveedor", length = 50)
    private String facturaProveedor;
    
    /**
     * Fecha de emisión
     */
    @Column(nullable = false)
    private LocalDate fecha;
    
    /**
     * Fecha de vencimiento
     */
    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;
    
    /**
     * Moneda
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "moneda_id", nullable = false)
    private Moneda moneda;
    
    /**
     * Tipo de cambio
     */
    @Column(name = "tipo_cambio", precision = 19, scale = 6)
    private BigDecimal tipoCambio = BigDecimal.ONE;
    
    /**
     * Monto total
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;
    
    /**
     * Monto pagado
     */
    @Column(name = "monto_pagado", precision = 19, scale = 2)
    private BigDecimal montoPagado = BigDecimal.ZERO;
    
    /**
     * Saldo pendiente
     */
    @Column(name = "saldo_pendiente", precision = 19, scale = 2)
    private BigDecimal saldoPendiente;
    
    /**
     * Estado de pago
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false, length = 20)
    private EstadoPagoCuentaPorPagar estadoPago = EstadoPagoCuentaPorPagar.PENDIENTE;
    
    /**
     * Observaciones
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
        if (saldoPendiente == null) {
            saldoPendiente = monto;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        
        // Actualizar estado según saldo
        if (saldoPendiente.compareTo(BigDecimal.ZERO) == 0) {
            estadoPago = EstadoPagoCuentaPorPagar.PAGADO;
        } else if (montoPagado.compareTo(BigDecimal.ZERO) > 0) {
            estadoPago = EstadoPagoCuentaPorPagar.PAGADO_PARCIAL;
        }
    }
    
    /**
     * Verifica si la cuenta está vencida.
     */
    public boolean isVencida() {
        return fechaVencimiento.isBefore(LocalDate.now()) && 
               estadoPago != EstadoPagoCuentaPorPagar.PAGADO;
    }
    
    /**
     * Obtiene días de atraso.
     */
    public long getDiasAtraso() {
        if (!isVencida()) {
            return 0;
        }
        return LocalDate.now().toEpochDay() - fechaVencimiento.toEpochDay();
    }
}

/**
 * Estados de pago de cuenta por pagar
 */
enum EstadoPagoCuentaPorPagar {
    PENDIENTE,       // Sin pagos
    PAGADO_PARCIAL,  // Pagado parcialmente
    PAGADO,          // Pagado totalmente
    VENCIDO          // Vencido sin pagar
}
```

- [ ] **1.3.2** Crear migration SQL para `cuentas_por_pagar`

```sql
CREATE TABLE cuentas_por_pagar (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero VARCHAR(20) UNIQUE NOT NULL,
    proveedor_id BIGINT NOT NULL,
    orden_compra_id BIGINT,
    factura_proveedor VARCHAR(50),
    fecha DATE NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    moneda_id BIGINT NOT NULL,
    tipo_cambio DECIMAL(19,6) DEFAULT 1.00,
    monto DECIMAL(19,2) NOT NULL,
    monto_pagado DECIMAL(19,2) DEFAULT 0.00,
    saldo_pendiente DECIMAL(19,2),
    estado_pago VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    observaciones VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (proveedor_id) REFERENCES proveedores(id),
    FOREIGN KEY (orden_compra_id) REFERENCES ordenes_compra(id),
    FOREIGN KEY (moneda_id) REFERENCES monedas(id),
    
    INDEX idx_numero (numero),
    INDEX idx_proveedor (proveedor_id),
    INDEX idx_estado (estado_pago),
    INDEX idx_vencimiento (fecha_vencimiento)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 1.4. Entidad `PagoProveedor.java`

#### Tareas:

- [ ] **1.4.1** Crear entidad `PagoProveedor`

```java
@Entity
@Table(name = "pagos_proveedores")
@Data
public class PagoProveedor {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 20)
    private String numero;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_por_pagar_id")
    private CuentaPorPagar cuentaPorPagar;
    
    @Column(nullable = false)
    private LocalDate fecha;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "moneda_id", nullable = false)
    private Moneda moneda;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false, length = 30)
    private MetodoPagoProveedor metodoPago;
    
    @Column(length = 100)
    private String referencia;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPagoProveedor estado = EstadoPagoProveedor.PENDIENTE;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
    
    @Column(length = 500)
    private String observaciones;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

enum MetodoPagoProveedor {
    EFECTIVO,
    TRANSFERENCIA,
    CHEQUE,
    SINPE_MOVIL,
    TARJETA_CREDITO
}

enum EstadoPagoProveedor {
    PENDIENTE,
    CONFIRMADO,
    ANULADO
}
```

- [ ] **1.4.2** Crear migration SQL para `pagos_proveedores`

```sql
CREATE TABLE pagos_proveedores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero VARCHAR(20) UNIQUE NOT NULL,
    proveedor_id BIGINT NOT NULL,
    cuenta_por_pagar_id BIGINT,
    fecha DATE NOT NULL,
    moneda_id BIGINT NOT NULL,
    monto DECIMAL(19,2) NOT NULL,
    metodo_pago VARCHAR(30) NOT NULL,
    referencia VARCHAR(100),
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    usuario_id BIGINT,
    observaciones VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (proveedor_id) REFERENCES proveedores(id),
    FOREIGN KEY (cuenta_por_pagar_id) REFERENCES cuentas_por_pagar(id),
    FOREIGN KEY (moneda_id) REFERENCES monedas(id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    
    INDEX idx_numero (numero),
    INDEX idx_proveedor (proveedor_id),
    INDEX idx_estado (estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 1.5. Repositories

#### Tareas:

- [ ] **1.5.1** Crear repositories básicos

```java
@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    Optional<Proveedor> findByCedula(String cedula);
    List<Proveedor> findByActivoTrue();
    List<Proveedor> findByTipoProveedor(TipoProveedor tipo);
}

@Repository
public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long> {
    Optional<OrdenCompra> findByNumero(String numero);
    List<OrdenCompra> findByProveedorAndEstado(Proveedor proveedor, EstadoOrdenCompra estado);
    List<OrdenCompra> findByFechaBetween(LocalDate desde, LocalDate hasta);
}

@Repository
public interface CuentaPorPagarRepository extends JpaRepository<CuentaPorPagar, Long> {
    List<CuentaPorPagar> findByProveedorAndEstadoPago(Proveedor proveedor, EstadoPagoCuentaPorPagar estado);
    
    @Query("SELECT c FROM CuentaPorPagar c WHERE c.fechaVencimiento < :fecha AND c.estadoPago != 'PAGADO'")
    List<CuentaPorPagar> findCuentasVencidas(LocalDate fecha);
    
    @Query("SELECT SUM(c.saldoPendiente) FROM CuentaPorPagar c WHERE c.proveedor = :proveedor")
    BigDecimal calcularSaldoTotalProveedor(Proveedor proveedor);
}

@Repository
public interface PagoProveedorRepository extends JpaRepository<PagoProveedor, Long> {
    List<PagoProveedor> findByProveedor(Proveedor proveedor);
    List<PagoProveedor> findByFechaBetween(LocalDate desde, LocalDate hasta);
}
```

---

## 📦 2. ÓRDENES DE COMPRA (10 tareas)

### 2.1. Service

#### Tareas:

- [ ] **2.1.1** Crear `OrdenCompraService.java`

```java
@Service
@RequiredArgsConstructor
public class OrdenCompraService {
    
    private final OrdenCompraRepository ordenCompraRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final TipoCambioService tipoCambioService;
    
    @Transactional
    public OrdenCompraDTO crearOrden(OrdenCompraDTO dto) {
        Proveedor proveedor = proveedorRepository.findById(dto.getProveedorId())
            .orElseThrow(() -> new BusinessException("Proveedor no encontrado"));
        
        OrdenCompra orden = new OrdenCompra();
        orden.setNumero(generarNumero());
        orden.setProveedor(proveedor);
        orden.setFecha(dto.getFecha() != null ? dto.getFecha() : LocalDate.now());
        orden.setFechaEntregaEsperada(dto.getFechaEntregaEsperada());
        
        // Moneda y tipo de cambio
        Moneda moneda = proveedor.getMoneda() != null 
            ? proveedor.getMoneda() 
            : monedaService.obtenerMonedaBase();
        orden.setMoneda(moneda);
        
        if (!moneda.getMonedaBase()) {
            TipoCambio tc = tipoCambioService.obtenerTipoCambio(
                moneda.getCodigo(),
                monedaService.obtenerMonedaBase().getCodigo(),
                orden.getFecha()
            );
            orden.setTipoCambio(tc.getTasaCompra());
        }
        
        // Detalles
        dto.getDetalles().forEach(detalleDTO -> {
            Producto producto = productoRepository.findById(detalleDTO.getProductoId())
                .orElseThrow(() -> new BusinessException("Producto no encontrado"));
            
            DetalleOrdenCompra detalle = new DetalleOrdenCompra();
            detalle.setOrdenCompra(orden);
            detalle.setProducto(producto);
            detalle.setCantidadSolicitada(detalleDTO.getCantidad());
            detalle.setPrecioUnitario(detalleDTO.getPrecioUnitario());
            detalle.setDescuentoPorcentaje(detalleDTO.getDescuento() != null ? detalleDTO.getDescuento() : BigDecimal.ZERO);
            
            orden.getDetalles().add(detalle);
        });
        
        // Calcular totales
        calcularTotales(orden);
        
        orden = ordenCompraRepository.save(orden);
        
        log.info("Orden de compra creada: {} para proveedor {}", orden.getNumero(), proveedor.getNombre());
        
        return toDTO(orden);
    }
    
    private void calcularTotales(OrdenCompra orden) {
        BigDecimal subtotal = orden.getDetalles().stream()
            .map(DetalleOrdenCompra::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        orden.setSubtotal(subtotal);
        orden.setTotal(subtotal); // Simplificado, puede incluir IVA
    }
    
    private String generarNumero() {
        int year = LocalDate.now().getYear();
        Integer ultimo = ordenCompraRepository.findUltimoConsecutivo(year);
        int siguiente = (ultimo != null ? ultimo : 0) + 1;
        return String.format("OC-%d-%05d", year, siguiente);
    }
}
```

- [ ] **2.1.2** Implementar recepción de mercancía

```java
@Transactional
public void recibirMercancia(Long ordenId, List<RecepcionMercanciaDTO> recepciones) {
    OrdenCompra orden = ordenCompraRepository.findById(ordenId)
        .orElseThrow(() -> new BusinessException("Orden no encontrada"));
    
    recepciones.forEach(recepcion -> {
        DetalleOrdenCompra detalle = orden.getDetalles().stream()
            .filter(d -> d.getId().equals(recepcion.getDetalleId()))
            .findFirst()
            .orElseThrow(() -> new BusinessException("Detalle no encontrado"));
        
        // Actualizar cantidad recibida
        detalle.setCantidadRecibida(detalle.getCantidadRecibida() + recepcion.getCantidadRecibida());
        
        // Registrar entrada de inventario
        movimientoInventarioService.registrarEntrada(
            detalle.getProducto().getId(),
            recepcion.getCantidadRecibida(),
            detalle.getPrecioUnitario(),
            TipoMovimientoInventario.ENTRADA_COMPRA,
            "ORDEN_COMPRA",
            orden.getId(),
            usuarioActual,
            "Recepción orden " + orden.getNumero(),
            recepcion.getLote()
        );
    });
    
    // Actualizar estado de orden
    actualizarEstadoOrden(orden);
    
    ordenCompraRepository.save(orden);
}

private void actualizarEstadoOrden(OrdenCompra orden) {
    boolean todosRecibidos = orden.getDetalles().stream()
        .allMatch(d -> d.getCantidadRecibida().equals(d.getCantidadSolicitada()));
    
    boolean algunosRecibidos = orden.getDetalles().stream()
        .anyMatch(d -> d.getCantidadRecibida() > 0);
    
    if (todosRecibidos) {
        orden.setEstado(EstadoOrdenCompra.RECIBIDA_TOTAL);
        orden.setFechaEntregaReal(LocalDate.now());
        
        // Crear cuenta por pagar
        crearCuentaPorPagar(orden);
    } else if (algunosRecibidos) {
        orden.setEstado(EstadoOrdenCompra.RECIBIDA_PARCIAL);
    }
}
```

- [ ] **2.1.3** Generar PDF de orden de compra

- [ ] **2.1.4** Enviar orden por email al proveedor

- [ ] **2.1.5** Controller para órdenes de compra

- [ ] **2.1.6** Vista HTML de orden de compra

- [ ] **2.1.7** Formulario de recepción de mercancía

- [ ] **2.1.8** Listar órdenes pendientes

- [ ] **2.1.9** Dashboard de órdenes de compra

- [ ] **2.1.10** Reporte de órdenes por proveedor

---

## 📦 3. CUENTAS POR PAGAR (10 tareas)

### 3.1. Service

#### Tareas:

- [ ] **3.1.1** Crear `CuentaPorPagarService.java`

```java
@Service
@RequiredArgsConstructor
public class CuentaPorPagarService {
    
    private final CuentaPorPagarRepository cuentaRepository;
    private final ProveedorRepository proveedorRepository;
    
    /**
     * Crea cuenta por pagar desde orden de compra recibida.
     */
    @Transactional
    public CuentaPorPagar crearDesdeOrden(OrdenCompra orden) {
        CuentaPorPagar cuenta = new CuentaPorPagar();
        cuenta.setNumero(generarNumero());
        cuenta.setProveedor(orden.getProveedor());
        cuenta.setOrdenCompra(orden);
        cuenta.setFecha(LocalDate.now());
        
        // Calcular vencimiento según días de crédito del proveedor
        int diasCredito = orden.getProveedor().getDiasCredito() != null 
            ? orden.getProveedor().getDiasCredito() 
            : 0;
        cuenta.setFechaVencimiento(LocalDate.now().plusDays(diasCredito));
        
        cuenta.setMoneda(orden.getMoneda());
        cuenta.setTipoCambio(orden.getTipoCambio());
        cuenta.setMonto(orden.getTotal());
        cuenta.setSaldoPendiente(orden.getTotal());
        cuenta.setEstadoPago(EstadoPagoCuentaPorPagar.PENDIENTE);
        
        cuenta = cuentaRepository.save(cuenta);
        
        // Actualizar saldo del proveedor
        actualizarSaldoProveedor(orden.getProveedor());
        
        log.info("Cuenta por pagar creada: {} por {}", 
            cuenta.getNumero(), cuenta.getMonto());
        
        return cuenta;
    }
    
    /**
     * Lista cuentas vencidas.
     */
    public List<CuentaPorPagar> listarCuentasVencidas() {
        return cuentaRepository.findCuentasVencidas(LocalDate.now());
    }
    
    /**
     * Obtiene estado de cuenta con proveedor.
     */
    public EstadoCuentaProveedorDTO obtenerEstadoCuenta(Long proveedorId) {
        Proveedor proveedor = proveedorRepository.findById(proveedorId)
            .orElseThrow(() -> new BusinessException("Proveedor no encontrado"));
        
        List<CuentaPorPagar> cuentas = cuentaRepository
            .findByProveedorAndEstadoPago(proveedor, EstadoPagoCuentaPorPagar.PENDIENTE);
        
        BigDecimal saldoTotal = cuentaRepository.calcularSaldoTotalProveedor(proveedor);
        
        EstadoCuentaProveedorDTO estado = new EstadoCuentaProveedorDTO();
        estado.setProveedor(proveedor.getNombre());
        estado.setCuentas(cuentas);
        estado.setSaldoTotal(saldoTotal);
        
        return estado;
    }
    
    private void actualizarSaldoProveedor(Proveedor proveedor) {
        BigDecimal saldo = cuentaRepository.calcularSaldoTotalProveedor(proveedor);
        proveedor.setSaldoPendiente(saldo);
        proveedorRepository.save(proveedor);
    }
    
    private String generarNumero() {
        int year = LocalDate.now().getYear();
        Integer ultimo = cuentaRepository.findUltimoConsecutivo(year);
        int siguiente = (ultimo != null ? ultimo : 0) + 1;
        return String.format("CP-%d-%05d", year, siguiente);
    }
}
```

- [ ] **3.1.2** Listar cuentas por pagar por proveedor

- [ ] **3.1.3** Vista de cuentas por pagar

- [ ] **3.1.4** Reporte de antigüedad de saldos (proveedores)

- [ ] **3.1.5** Alerta de cuentas próximas a vencer

- [ ] **3.1.6** Dashboard de cuentas por pagar

- [ ] **3.1.7** Exportar estado de cuenta a PDF

- [ ] **3.1.8** Proyección de flujo de caja

- [ ] **3.1.9** Reporte de cuentas vencidas

- [ ] **3.1.10** Integración contable (asientos automáticos)

---

## 📦 4. PAGOS A PROVEEDORES (6 tareas)

### 4.1. Service

#### Tareas:

- [ ] **4.1.1** Crear `PagoProveedorService.java`

```java
@Service
@RequiredArgsConstructor
public class PagoProveedorService {
    
    private final PagoProveedorRepository pagoRepository;
    private final CuentaPorPagarRepository cuentaRepository;
    private final AsientoContableService asientoService;
    
    @Transactional
    public PagoProveedorDTO registrarPago(PagoProveedorDTO dto) {
        CuentaPorPagar cuenta = cuentaRepository.findById(dto.getCuentaPorPagarId())
            .orElseThrow(() -> new BusinessException("Cuenta no encontrada"));
        
        // Validar monto
        if (dto.getMonto().compareTo(cuenta.getSaldoPendiente()) > 0) {
            throw new BusinessException("El monto excede el saldo pendiente");
        }
        
        PagoProveedor pago = new PagoProveedor();
        pago.setNumero(generarNumero());
        pago.setProveedor(cuenta.getProveedor());
        pago.setCuentaPorPagar(cuenta);
        pago.setFecha(dto.getFecha() != null ? dto.getFecha() : LocalDate.now());
        pago.setMoneda(cuenta.getMoneda());
        pago.setMonto(dto.getMonto());
        pago.setMetodoPago(dto.getMetodoPago());
        pago.setReferencia(dto.getReferencia());
        pago.setEstado(EstadoPagoProveedor.CONFIRMADO);
        pago.setObservaciones(dto.getObservaciones());
        
        pago = pagoRepository.save(pago);
        
        // Actualizar cuenta por pagar
        cuenta.setMontoPagado(cuenta.getMontoPagado().add(dto.getMonto()));
        cuenta.setSaldoPendiente(cuenta.getMonto().subtract(cuenta.getMontoPagado()));
        cuentaRepository.save(cuenta);
        
        // Generar asiento contable
        asientoService.registrarAsientoPagoProveedor(pago);
        
        log.info("Pago a proveedor registrado: {} por {}", pago.getNumero(), pago.getMonto());
        
        return toDTO(pago);
    }
    
    private String generarNumero() {
        int year = LocalDate.now().getYear();
        Integer ultimo = pagoRepository.findUltimoConsecutivo(year);
        int siguiente = (ultimo != null ? ultimo : 0) + 1;
        return String.format("PP-%d-%05d", year, siguiente);
    }
}
```

- [ ] **4.1.2** Controller de pagos a proveedores

- [ ] **4.1.3** Vista de registro de pago

- [ ] **4.1.4** Listar pagos realizados

- [ ] **4.1.5** Generar comprobante de pago

- [ ] **4.1.6** Reporte de pagos por período

---

## 📦 5. REPORTES Y ANÁLISIS (4 tareas)

#### Tareas:

- [ ] **5.1** Reporte de compras por proveedor

- [ ] **5.2** Evaluación de proveedores (puntuación)

- [ ] **5.3** Análisis de cumplimiento de entregas

- [ ] **5.4** Dashboard analítico de proveedores

---

## 📦 6. INTERFAZ DE USUARIO (2 tareas)

#### Tareas:

- [ ] **6.1** Vista de catálogo de proveedores

- [ ] **6.2** Formulario de creación/edición de proveedor

---

## 📊 CRITERIOS DE ACEPTACIÓN

✅ CRUD completo de proveedores  
✅ Órdenes de compra con flujo completo  
✅ Recepción de mercancía actualiza inventario  
✅ Cuentas por pagar creadas automáticamente  
✅ Pagos a proveedores con asientos contables  
✅ Alertas de cuentas vencidas  
✅ Reportes de estado de cuenta por proveedor  
✅ Dashboard de compras y pagos  

---

## 📚 DEPENDENCIAS

**Requiere:**
- ✅ Sprint 6 Fase 1 (Multi-Divisa)
- ✅ Sprint 6 Fase 2 (Inventario)
- ✅ Sistema contable (Sprint 5)

**Habilita:**
- 🚀 Gestión completa de compras
- 🚀 Control de cuentas por pagar
- 🚀 Análisis de proveedores

---

## 🔄 PRÓXIMOS PASOS

1. ✅ Probar flujo completo: orden → recepción → cuenta por pagar → pago
2. 🚀 Continuar con **FASE 4: Testing**

---

**Fase creada:** 16 de enero de 2026  
**Responsable:** Equipo de Desarrollo
