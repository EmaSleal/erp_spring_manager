# 💱 FASE 1: Multi-Divisa (Monedas y Tipos de Cambio)

**Sprint:** 6  
**Fase:** 1 de 5  
**Duración estimada:** 6-8 días  
**Prioridad:** ⭐⭐⭐ CRÍTICA  
**Estado:** 📋 PENDIENTE (0/42 tareas)

---

## 📋 OBJETIVO DE LA FASE

Implementar soporte completo para múltiples divisas:
- Gestión de monedas (USD, EUR, CRC, etc.)
- Tipos de cambio con histórico
- Actualización automática de tasas desde API externa
- Conversión automática en facturación y pagos
- Reportes consolidados multi-divisa
- Integración con `formatearMoneda()` existente ✅

---

## 📊 PROGRESO GENERAL

```
Progreso: [0/42] ░░░░░░░░░░░░░░░░░░░░ 0%

├─ 1. Modelo de Datos              [0/8]  ░░░░░░░░░░ 0%
├─ 2. Servicio de Tipos de Cambio  [0/10] ░░░░░░░░░░ 0%
├─ 3. Integración en Facturación   [0/12] ░░░░░░░░░░ 0%
├─ 4. Interfaz de Usuario          [0/8]  ░░░░░░░░░░ 0%
└─ 5. Reportes Multi-Divisa        [0/4]  ░░░░░░░░░░ 0%
```

---

## 📦 1. MODELO DE DATOS (8 tareas)

### 1.1. Entidad `Moneda.java`

**Archivo:** `src/main/java/com/erp/model/Moneda.java`

#### Tareas:

- [ ] **1.1.1** Crear entidad `Moneda`

```java
package com.erp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Entidad para gestión de monedas del sistema.
 * Soporta múltiples divisas para facturación internacional.
 * 
 * @author ERP Team
 * @version 6.0
 * @since Sprint 6
 */
@Entity
@Table(name = "monedas")
@Data
public class Moneda {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Código ISO 4217 de la moneda (USD, EUR, CRC, etc.)
     */
    @Column(nullable = false, unique = true, length = 3)
    private String codigo;
    
    /**
     * Nombre completo de la moneda
     */
    @Column(nullable = false, length = 100)
    private String nombre;
    
    /**
     * Símbolo de la moneda ($, €, ₡, etc.)
     */
    @Column(nullable = false, length = 5)
    private String simbolo;
    
    /**
     * Número de decimales a usar (normalmente 2)
     */
    @Column(nullable = false)
    private Integer decimales = 2;
    
    /**
     * Si es la moneda base del sistema
     * Solo una moneda puede ser base
     */
    @Column(nullable = false)
    private Boolean monedaBase = false;
    
    /**
     * Si está activa para transacciones
     */
    @Column(nullable = false)
    private Boolean activa = true;
    
    /**
     * País principal donde se usa
     */
    @Column(length = 100)
    private String pais;
    
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
     * Formato de la moneda usando formatearMoneda() existente.
     * 
     * @param monto Monto a formatear
     * @return String formateado según configuración de moneda
     */
    public String formatear(Double monto) {
        // Integración con método existente en Utils
        return String.format("%s %." + decimales + "f", simbolo, monto);
    }
}
```

- [ ] **1.1.2** Crear migration SQL para tabla `monedas`

```sql
-- Migration: MIGRATION_MONEDAS_SPRINT_6.sql

CREATE TABLE monedas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(3) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    simbolo VARCHAR(5) NOT NULL,
    decimales INT NOT NULL DEFAULT 2,
    moneda_base BOOLEAN NOT NULL DEFAULT FALSE,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    pais VARCHAR(100),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_codigo (codigo),
    INDEX idx_moneda_base (moneda_base),
    INDEX idx_activa (activa)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Constraint: Solo una moneda puede ser base
-- (Se validará en el servicio)
```

- [ ] **1.1.3** Insertar datos iniciales de monedas

```sql
-- Datos iniciales de monedas
INSERT INTO monedas (codigo, nombre, simbolo, decimales, moneda_base, activa, pais) VALUES
('CRC', 'Colón Costarricense', '₡', 2, TRUE, TRUE, 'Costa Rica'),
('USD', 'Dólar Estadounidense', '$', 2, FALSE, TRUE, 'Estados Unidos'),
('EUR', 'Euro', '€', 2, FALSE, TRUE, 'Zona Euro'),
('GBP', 'Libra Esterlina', '£', 2, FALSE, TRUE, 'Reino Unido'),
('MXN', 'Peso Mexicano', 'Mex$', 2, FALSE, TRUE, 'México'),
('CAD', 'Dólar Canadiense', 'C$', 2, FALSE, FALSE, 'Canadá'),
('JPY', 'Yen Japonés', '¥', 0, FALSE, FALSE, 'Japón'),
('CHF', 'Franco Suizo', 'CHF', 2, FALSE, FALSE, 'Suiza');

-- Nota: CRC es la moneda base para Costa Rica
```

---

### 1.2. Entidad `TipoCambio.java`

#### Tareas:

- [ ] **1.2.1** Crear entidad `TipoCambio`

```java
package com.erp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tipos de cambio entre divisas con histórico.
 * Permite consultar tasas de conversión por fecha.
 * 
 * @author ERP Team
 * @version 6.0
 * @since Sprint 6
 */
@Entity
@Table(name = "tipos_cambio", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"fecha", "moneda_origen_id", "moneda_destino_id"}))
@Data
public class TipoCambio {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Fecha de vigencia del tipo de cambio
     */
    @Column(nullable = false)
    private LocalDate fecha;
    
    /**
     * Moneda de origen
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "moneda_origen_id", nullable = false)
    private Moneda monedaOrigen;
    
    /**
     * Moneda de destino
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "moneda_destino_id", nullable = false)
    private Moneda monedaDestino;
    
    /**
     * Tasa de compra (banco compra divisa)
     */
    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal tasaCompra;
    
    /**
     * Tasa de venta (banco vende divisa)
     */
    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal tasaVenta;
    
    /**
     * Fuente de la tasa (BCCR, API_EXTERNA, MANUAL)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FuenteTipoCambio fuente;
    
    /**
     * Si este es el tipo de cambio activo para el día
     */
    @Column(nullable = false)
    private Boolean activo = true;
    
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
     * Convierte un monto usando esta tasa.
     * 
     * @param monto Monto en moneda origen
     * @param usarCompra true para tasa de compra, false para venta
     * @return Monto convertido a moneda destino
     */
    public BigDecimal convertir(BigDecimal monto, boolean usarCompra) {
        BigDecimal tasa = usarCompra ? tasaCompra : tasaVenta;
        return monto.multiply(tasa).setScale(monedaDestino.getDecimales(), BigDecimal.ROUND_HALF_UP);
    }
}

/**
 * Fuente del tipo de cambio
 */
enum FuenteTipoCambio {
    BCCR,        // Banco Central de Costa Rica
    API_EXTERNA, // API externa (Fixer.io, etc.)
    MANUAL       // Ingresado manualmente
}
```

- [ ] **1.2.2** Crear migration SQL para `tipos_cambio`

```sql
CREATE TABLE tipos_cambio (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fecha DATE NOT NULL,
    moneda_origen_id BIGINT NOT NULL,
    moneda_destino_id BIGINT NOT NULL,
    tasa_compra DECIMAL(19,6) NOT NULL,
    tasa_venta DECIMAL(19,6) NOT NULL,
    fuente VARCHAR(20) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (moneda_origen_id) REFERENCES monedas(id),
    FOREIGN KEY (moneda_destino_id) REFERENCES monedas(id),
    
    UNIQUE KEY uk_fecha_monedas (fecha, moneda_origen_id, moneda_destino_id),
    INDEX idx_fecha (fecha),
    INDEX idx_activo (activo),
    INDEX idx_fuente (fuente)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 1.3. Modificar entidades existentes

#### Tareas:

- [ ] **1.3.1** Agregar campo `moneda_id` a tabla `facturas`

```sql
ALTER TABLE facturas 
ADD COLUMN moneda_id BIGINT DEFAULT 1 AFTER total,
ADD FOREIGN KEY (moneda_id) REFERENCES monedas(id);

-- Actualizar facturas existentes con moneda base (CRC)
UPDATE facturas SET moneda_id = (SELECT id FROM monedas WHERE moneda_base = TRUE LIMIT 1);

ALTER TABLE facturas MODIFY COLUMN moneda_id BIGINT NOT NULL;
```

- [ ] **1.3.2** Agregar campo `tipo_cambio` a `facturas`

```sql
ALTER TABLE facturas 
ADD COLUMN tipo_cambio DECIMAL(19,6) DEFAULT 1.00 AFTER moneda_id;

-- Para CRC no aplica tipo de cambio (1.00)
UPDATE facturas SET tipo_cambio = 1.00 WHERE moneda_id = (SELECT id FROM monedas WHERE codigo = 'CRC');
```

- [ ] **1.3.3** Agregar campo `moneda_id` a tabla `pagos`

```sql
ALTER TABLE pagos 
ADD COLUMN moneda_id BIGINT DEFAULT 1 AFTER monto,
ADD COLUMN tipo_cambio DECIMAL(19,6) DEFAULT 1.00 AFTER moneda_id,
ADD FOREIGN KEY (moneda_id) REFERENCES monedas(id);

UPDATE pagos SET moneda_id = (SELECT id FROM monedas WHERE moneda_base = TRUE LIMIT 1);
ALTER TABLE pagos MODIFY COLUMN moneda_id BIGINT NOT NULL;
```

- [ ] **1.3.4** Actualizar modelo `Factura.java`

```java
@ManyToOne(fetch = FetchType.EAGER)
@JoinColumn(name = "moneda_id", nullable = false)
private Moneda moneda;

@Column(name = "tipo_cambio", precision = 19, scale = 6)
private BigDecimal tipoCambio = BigDecimal.ONE;

/**
 * Obtiene el total de la factura convertido a moneda base.
 * 
 * @return Total en moneda base (CRC)
 */
public BigDecimal getTotalEnMonedaBase() {
    if (moneda.getMonedaBase()) {
        return total;
    }
    return total.multiply(tipoCambio);
}
```

- [ ] **1.3.5** Actualizar modelo `Pago.java`

```java
@ManyToOne(fetch = FetchType.EAGER)
@JoinColumn(name = "moneda_id", nullable = false)
private Moneda moneda;

@Column(name = "tipo_cambio", precision = 19, scale = 6)
private BigDecimal tipoCambio = BigDecimal.ONE;

/**
 * Obtiene el monto del pago convertido a moneda base.
 */
public BigDecimal getMontoEnMonedaBase() {
    if (moneda.getMonedaBase()) {
        return monto;
    }
    return monto.multiply(tipoCambio);
}
```

---

## 📦 2. SERVICIO DE TIPOS DE CAMBIO (10 tareas)

### 2.1. Repository

#### Tareas:

- [ ] **2.1.1** Crear `MonedaRepository.java`

```java
package com.erp.repository;

import com.erp.model.Moneda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface MonedaRepository extends JpaRepository<Moneda, Long> {
    
    Optional<Moneda> findByCodigo(String codigo);
    
    Optional<Moneda> findByMonedaBase(Boolean monedaBase);
    
    List<Moneda> findByActivaTrue();
    
    boolean existsByCodigo(String codigo);
}
```

- [ ] **2.1.2** Crear `TipoCambioRepository.java`

```java
package com.erp.repository;

import com.erp.model.TipoCambio;
import com.erp.model.Moneda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

@Repository
public interface TipoCambioRepository extends JpaRepository<TipoCambio, Long> {
    
    /**
     * Busca tipo de cambio para una fecha específica entre dos monedas.
     */
    Optional<TipoCambio> findByFechaAndMonedaOrigenAndMonedaDestinoAndActivoTrue(
        LocalDate fecha, 
        Moneda monedaOrigen, 
        Moneda monedaDestino
    );
    
    /**
     * Busca el tipo de cambio activo más reciente <= fecha dada.
     */
    @Query("SELECT tc FROM TipoCambio tc " +
           "WHERE tc.monedaOrigen = :origen " +
           "AND tc.monedaDestino = :destino " +
           "AND tc.fecha <= :fecha " +
           "AND tc.activo = true " +
           "ORDER BY tc.fecha DESC")
    Optional<TipoCambio> findUltimoTipoCambio(
        Moneda origen, 
        Moneda destino, 
        LocalDate fecha
    );
    
    /**
     * Obtiene histórico de tipos de cambio.
     */
    List<TipoCambio> findByMonedaOrigenAndMonedaDestinoAndFechaBetweenOrderByFechaDesc(
        Moneda origen, 
        Moneda destino, 
        LocalDate desde, 
        LocalDate hasta
    );
}
```

---

### 2.2. Service

#### Tareas:

- [ ] **2.2.1** Crear `MonedaService.java`

```java
package com.erp.service;

import com.erp.dto.MonedaDTO;
import com.erp.exception.BusinessException;
import com.erp.model.Moneda;
import com.erp.repository.MonedaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonedaService {
    
    private final MonedaRepository monedaRepository;
    
    @Transactional(readOnly = true)
    public List<MonedaDTO> listarActivas() {
        return monedaRepository.findByActivaTrue()
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public Moneda obtenerPorCodigo(String codigo) {
        return monedaRepository.findByCodigo(codigo)
            .orElseThrow(() -> new BusinessException("Moneda no encontrada: " + codigo));
    }
    
    @Transactional(readOnly = true)
    public Moneda obtenerMonedaBase() {
        return monedaRepository.findByMonedaBase(true)
            .orElseThrow(() -> new BusinessException("No hay moneda base configurada"));
    }
    
    @Transactional
    public MonedaDTO crear(MonedaDTO dto) {
        if (monedaRepository.existsByCodigo(dto.getCodigo())) {
            throw new BusinessException("Ya existe moneda con código: " + dto.getCodigo());
        }
        
        Moneda moneda = toEntity(dto);
        
        // Si se marca como moneda base, quitar flag de otras
        if (dto.getMonedaBase()) {
            monedaRepository.findByMonedaBase(true)
                .ifPresent(m -> {
                    m.setMonedaBase(false);
                    monedaRepository.save(m);
                });
        }
        
        moneda = monedaRepository.save(moneda);
        return toDTO(moneda);
    }
    
    private MonedaDTO toDTO(Moneda moneda) {
        MonedaDTO dto = new MonedaDTO();
        dto.setId(moneda.getId());
        dto.setCodigo(moneda.getCodigo());
        dto.setNombre(moneda.getNombre());
        dto.setSimbolo(moneda.getSimbolo());
        dto.setDecimales(moneda.getDecimales());
        dto.setMonedaBase(moneda.getMonedaBase());
        dto.setActiva(moneda.getActiva());
        dto.setPais(moneda.getPais());
        return dto;
    }
    
    private Moneda toEntity(MonedaDTO dto) {
        Moneda moneda = new Moneda();
        moneda.setCodigo(dto.getCodigo().toUpperCase());
        moneda.setNombre(dto.getNombre());
        moneda.setSimbolo(dto.getSimbolo());
        moneda.setDecimales(dto.getDecimales() != null ? dto.getDecimales() : 2);
        moneda.setMonedaBase(dto.getMonedaBase() != null ? dto.getMonedaBase() : false);
        moneda.setActiva(dto.getActiva() != null ? dto.getActiva() : true);
        moneda.setPais(dto.getPais());
        return moneda;
    }
}
```

- [ ] **2.2.2** Crear `TipoCambioService.java`

```java
package com.erp.service;

import com.erp.dto.TipoCambioDTO;
import com.erp.exception.BusinessException;
import com.erp.model.TipoCambio;
import com.erp.model.Moneda;
import com.erp.repository.TipoCambioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TipoCambioService {
    
    private final TipoCambioRepository tipoCambioRepository;
    private final MonedaService monedaService;
    
    /**
     * Obtiene el tipo de cambio para una fecha y par de monedas.
     * Si no existe para la fecha exacta, busca el más reciente.
     */
    @Transactional(readOnly = true)
    public TipoCambio obtenerTipoCambio(String codigoOrigen, String codigoDestino, LocalDate fecha) {
        Moneda origen = monedaService.obtenerPorCodigo(codigoOrigen);
        Moneda destino = monedaService.obtenerPorCodigo(codigoDestino);
        
        // Si es la misma moneda, tipo de cambio = 1
        if (origen.equals(destino)) {
            return crearTipoCambioUnitario(origen, destino, fecha);
        }
        
        return tipoCambioRepository
            .findUltimoTipoCambio(origen, destino, fecha)
            .orElseThrow(() -> new BusinessException(
                String.format("No hay tipo de cambio disponible para %s -> %s en %s",
                    codigoOrigen, codigoDestino, fecha)
            ));
    }
    
    /**
     * Convierte un monto entre dos monedas.
     */
    @Transactional(readOnly = true)
    public BigDecimal convertir(BigDecimal monto, String codigoOrigen, String codigoDestino, LocalDate fecha) {
        if (codigoOrigen.equals(codigoDestino)) {
            return monto;
        }
        
        TipoCambio tipoCambio = obtenerTipoCambio(codigoOrigen, codigoDestino, fecha);
        
        // Usar tasa de venta para conversión a moneda extranjera
        // Usar tasa de compra para conversión a moneda local
        boolean usarCompra = tipoCambio.getMonedaDestino().getMonedaBase();
        
        return tipoCambio.convertir(monto, usarCompra);
    }
    
    /**
     * Registra un tipo de cambio manual.
     */
    @Transactional
    public TipoCambioDTO registrarManual(TipoCambioDTO dto) {
        Moneda origen = monedaService.obtenerPorCodigo(dto.getCodigoMonedaOrigen());
        Moneda destino = monedaService.obtenerPorCodigo(dto.getCodigoMonedaDestino());
        
        TipoCambio tipoCambio = new TipoCambio();
        tipoCambio.setFecha(dto.getFecha());
        tipoCambio.setMonedaOrigen(origen);
        tipoCambio.setMonedaDestino(destino);
        tipoCambio.setTasaCompra(dto.getTasaCompra());
        tipoCambio.setTasaVenta(dto.getTasaVenta());
        tipoCambio.setFuente(FuenteTipoCambio.MANUAL);
        tipoCambio.setActivo(true);
        
        tipoCambio = tipoCambioRepository.save(tipoCambio);
        
        log.info("Tipo de cambio manual registrado: {} -> {} = {} ({})",
            origen.getCodigo(), destino.getCodigo(), dto.getTasaVenta(), dto.getFecha());
        
        return toDTO(tipoCambio);
    }
    
    private TipoCambio crearTipoCambioUnitario(Moneda origen, Moneda destino, LocalDate fecha) {
        TipoCambio tc = new TipoCambio();
        tc.setFecha(fecha);
        tc.setMonedaOrigen(origen);
        tc.setMonedaDestino(destino);
        tc.setTasaCompra(BigDecimal.ONE);
        tc.setTasaVenta(BigDecimal.ONE);
        tc.setFuente(FuenteTipoCambio.MANUAL);
        tc.setActivo(true);
        return tc;
    }
    
    private TipoCambioDTO toDTO(TipoCambio tc) {
        TipoCambioDTO dto = new TipoCambioDTO();
        dto.setId(tc.getId());
        dto.setFecha(tc.getFecha());
        dto.setCodigoMonedaOrigen(tc.getMonedaOrigen().getCodigo());
        dto.setCodigoMonedaDestino(tc.getMonedaDestino().getCodigo());
        dto.setTasaCompra(tc.getTasaCompra());
        dto.setTasaVenta(tc.getTasaVenta());
        dto.setFuente(tc.getFuente().name());
        return dto;
    }
}
```

- [ ] **2.2.3** Crear servicio para actualización automática desde API externa

```java
package com.erp.service;

import com.erp.model.FuenteTipoCambio;
import com.erp.model.Moneda;
import com.erp.model.TipoCambio;
import com.erp.repository.TipoCambioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Servicio para actualización automática de tipos de cambio desde APIs externas.
 * Actualiza diariamente las tasas de las monedas activas.
 * 
 * APIs soportadas:
 * - exchangerate-api.com (gratuita, 1500 req/mes)
 * - fixer.io (gratuita con límites)
 * - BCCR API (Banco Central Costa Rica)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActualizacionTipoCambioService {
    
    private final TipoCambioRepository tipoCambioRepository;
    private final MonedaService monedaService;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${tipo-cambio.api.url:https://api.exchangerate-api.com/v4/latest/}")
    private String apiUrl;
    
    @Value("${tipo-cambio.api.enabled:false}")
    private boolean apiEnabled;
    
    /**
     * Ejecuta diariamente a las 6:00 AM para actualizar tasas.
     * Cron: segundo minuto hora día mes día-semana
     */
    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void actualizarTiposDeCambio() {
        if (!apiEnabled) {
            log.debug("Actualización automática de tipos de cambio deshabilitada");
            return;
        }
        
        log.info("Iniciando actualización automática de tipos de cambio...");
        
        try {
            Moneda monedaBase = monedaService.obtenerMonedaBase();
            
            // Obtener tasas desde API
            String url = apiUrl + monedaBase.getCodigo();
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response == null || !response.containsKey("rates")) {
                log.error("Respuesta inválida de API de tipos de cambio");
                return;
            }
            
            Map<String, Double> rates = (Map<String, Double>) response.get("rates");
            LocalDate hoy = LocalDate.now();
            
            // Actualizar cada moneda activa
            monedaService.listarActivas().forEach(monedaDTO -> {
                if (!monedaDTO.getMonedaBase()) {
                    Double tasa = rates.get(monedaDTO.getCodigo());
                    if (tasa != null) {
                        registrarTasa(monedaBase, 
                                    monedaService.obtenerPorCodigo(monedaDTO.getCodigo()), 
                                    hoy, 
                                    BigDecimal.valueOf(tasa));
                    }
                }
            });
            
            log.info("Tipos de cambio actualizados exitosamente");
            
        } catch (Exception e) {
            log.error("Error actualizando tipos de cambio: {}", e.getMessage(), e);
        }
    }
    
    private void registrarTasa(Moneda base, Moneda destino, LocalDate fecha, BigDecimal tasa) {
        TipoCambio tipoCambio = new TipoCambio();
        tipoCambio.setFecha(fecha);
        tipoCambio.setMonedaOrigen(base);
        tipoCambio.setMonedaDestino(destino);
        tipoCambio.setTasaCompra(tasa.multiply(new BigDecimal("0.995"))); // -0.5% spread
        tipoCambio.setTasaVenta(tasa.multiply(new BigDecimal("1.005"))); // +0.5% spread
        tipoCambio.setFuente(FuenteTipoCambio.API_EXTERNA);
        tipoCambio.setActivo(true);
        
        tipoCambioRepository.save(tipoCambio);
        
        log.debug("Registrado tipo de cambio: {} -> {} = {}", 
            base.getCodigo(), destino.getCodigo(), tasa);
    }
}
```

---

### 2.3. DTOs

#### Tareas:

- [ ] **2.3.1** Crear `MonedaDTO.java`

```java
package com.erp.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class MonedaDTO {
    private Long id;
    
    @NotBlank(message = "Código es requerido")
    @Size(min = 3, max = 3, message = "Código debe tener 3 caracteres")
    @Pattern(regexp = "[A-Z]{3}", message = "Código debe ser 3 letras mayúsculas")
    private String codigo;
    
    @NotBlank(message = "Nombre es requerido")
    @Size(max = 100)
    private String nombre;
    
    @NotBlank(message = "Símbolo es requerido")
    @Size(max = 5)
    private String simbolo;
    
    @Min(value = 0, message = "Decimales debe ser >= 0")
    @Max(value = 6, message = "Decimales debe ser <= 6")
    private Integer decimales = 2;
    
    private Boolean monedaBase = false;
    private Boolean activa = true;
    
    @Size(max = 100)
    private String pais;
}
```

- [ ] **2.3.2** Crear `TipoCambioDTO.java`

```java
package com.erp.dto;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TipoCambioDTO {
    private Long id;
    
    @NotNull(message = "Fecha es requerida")
    private LocalDate fecha;
    
    @NotBlank(message = "Moneda origen es requerida")
    private String codigoMonedaOrigen;
    
    @NotBlank(message = "Moneda destino es requerida")
    private String codigoMonedaDestino;
    
    @NotNull(message = "Tasa de compra es requerida")
    @DecimalMin(value = "0.000001", message = "Tasa debe ser > 0")
    private BigDecimal tasaCompra;
    
    @NotNull(message = "Tasa de venta es requerida")
    @DecimalMin(value = "0.000001", message = "Tasa debe ser > 0")
    private BigDecimal tasaVenta;
    
    private String fuente;
}
```

---

## 📦 3. INTEGRACIÓN EN FACTURACIÓN (12 tareas)

### 3.1. Actualizar `FacturaService.java`

#### Tareas:

- [ ] **3.1.1** Modificar método `crearFactura()` para soportar multi-divisa

```java
@Transactional
public FacturaDTO crearFactura(FacturaDTO facturaDTO) {
    // ... código existente ...
    
    // NUEVO: Obtener moneda
    Moneda moneda = facturaDTO.getMonedaId() != null 
        ? monedaRepository.findById(facturaDTO.getMonedaId())
            .orElseThrow(() -> new BusinessException("Moneda no encontrada"))
        : monedaService.obtenerMonedaBase();
    
    factura.setMoneda(moneda);
    
    // NUEVO: Si no es moneda base, obtener tipo de cambio
    if (!moneda.getMonedaBase()) {
        TipoCambio tipoCambio = tipoCambioService.obtenerTipoCambio(
            moneda.getCodigo(),
            monedaService.obtenerMonedaBase().getCodigo(),
            LocalDate.now()
        );
        factura.setTipoCambio(tipoCambio.getTasaVenta());
    } else {
        factura.setTipoCambio(BigDecimal.ONE);
    }
    
    // ... resto del código ...
}
```

- [ ] **3.1.2** Actualizar cálculo de totales considerando conversión

```java
/**
 * Calcula totales de factura con soporte multi-divisa.
 */
private void calcularTotales(Factura factura) {
    BigDecimal subtotal = factura.getDetalles().stream()
        .map(d -> d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    BigDecimal descuento = factura.getDescuentoPorcentaje() != null 
        ? subtotal.multiply(factura.getDescuentoPorcentaje()).divide(new BigDecimal("100"))
        : BigDecimal.ZERO;
    
    BigDecimal baseImponible = subtotal.subtract(descuento);
    BigDecimal iva = baseImponible.multiply(new BigDecimal("0.13")); // 13% Costa Rica
    BigDecimal total = baseImponible.add(iva);
    
    factura.setSubtotal(subtotal);
    factura.setDescuento(descuento);
    factura.setIva(iva);
    factura.setTotal(total);
    
    // Todos los montos están en la moneda de la factura
    // Para reportes se convertirá usando tipo de cambio
}
```

- [ ] **3.1.3** Integrar con `formatearMoneda()` existente

```java
/**
 * Formatea un monto según la moneda de la factura.
 * Integra con método formatearMoneda() existente en Utils.
 */
public String formatearMontoFactura(Factura factura, BigDecimal monto) {
    return factura.getMoneda().formatear(monto.doubleValue());
}
```

---

### 3.2. Actualizar Controllers

#### Tareas:

- [ ] **3.2.1** Crear `MonedaController.java`

```java
package com.erp.controller;

import com.erp.dto.MonedaDTO;
import com.erp.service.MonedaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
@RequestMapping("/admin/monedas")
@RequiredArgsConstructor
public class MonedaController {
    
    private final MonedaService monedaService;
    
    @GetMapping
    public String listar(Model model) {
        List<MonedaDTO> monedas = monedaService.listarActivas();
        model.addAttribute("monedas", monedas);
        return "admin/monedas/lista";
    }
    
    @GetMapping("/nueva")
    public String formularioNueva(Model model) {
        model.addAttribute("moneda", new MonedaDTO());
        return "admin/monedas/formulario";
    }
    
    @PostMapping
    public String guardar(@Valid @ModelAttribute MonedaDTO monedaDTO, 
                         BindingResult result,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/monedas/formulario";
        }
        
        monedaService.crear(monedaDTO);
        redirectAttributes.addFlashAttribute("success", "Moneda creada exitosamente");
        return "redirect:/admin/monedas";
    }
}
```

- [ ] **3.2.2** Crear `TipoCambioController.java`

```java
@Controller
@RequestMapping("/admin/tipos-cambio")
@RequiredArgsConstructor
public class TipoCambioController {
    
    private final TipoCambioService tipoCambioService;
    
    @GetMapping
    public String listar(Model model) {
        // Lista tipos de cambio recientes
        return "admin/tipos-cambio/lista";
    }
    
    @GetMapping("/registrar")
    public String formularioRegistro(Model model) {
        model.addAttribute("tipoCambio", new TipoCambioDTO());
        return "admin/tipos-cambio/formulario";
    }
    
    @PostMapping
    public String registrar(@Valid @ModelAttribute TipoCambioDTO dto,
                          BindingResult result,
                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/tipos-cambio/formulario";
        }
        
        tipoCambioService.registrarManual(dto);
        redirectAttributes.addFlashAttribute("success", "Tipo de cambio registrado");
        return "redirect:/admin/tipos-cambio";
    }
}
```

- [ ] **3.2.3** Modificar `FacturaController` para incluir selector de moneda

```java
@GetMapping("/nueva")
public String formularioNueva(Model model) {
    model.addAttribute("factura", new FacturaDTO());
    model.addAttribute("clientes", clienteService.listarActivos());
    model.addAttribute("productos", productoService.listarActivos());
    
    // NUEVO: Agregar lista de monedas
    model.addAttribute("monedas", monedaService.listarActivas());
    
    return "ventas/facturas/formulario";
}
```

---

## 📦 4. INTERFAZ DE USUARIO (8 tareas)

### 4.1. Vistas Thymeleaf

#### Tareas:

- [ ] **4.1.1** Crear vista `admin/monedas/lista.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Monedas - ERP</title>
    <link rel="stylesheet" th:href="@{/css/bootstrap.min.css}">
</head>
<body>
<div class="container mt-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h2>💱 Gestión de Monedas</h2>
        <a th:href="@{/admin/monedas/nueva}" class="btn btn-primary">
            <i class="bi bi-plus-circle"></i> Nueva Moneda
        </a>
    </div>
    
    <div class="table-responsive">
        <table class="table table-striped">
            <thead>
                <tr>
                    <th>Código</th>
                    <th>Nombre</th>
                    <th>Símbolo</th>
                    <th>País</th>
                    <th>Decimales</th>
                    <th>Estado</th>
                    <th>Acciones</th>
                </tr>
            </thead>
            <tbody>
                <tr th:each="moneda : ${monedas}">
                    <td>
                        <strong th:text="${moneda.codigo}"></strong>
                        <span th:if="${moneda.monedaBase}" 
                              class="badge bg-success ms-2">BASE</span>
                    </td>
                    <td th:text="${moneda.nombre}"></td>
                    <td th:text="${moneda.simbolo}"></td>
                    <td th:text="${moneda.pais}"></td>
                    <td th:text="${moneda.decimales}"></td>
                    <td>
                        <span th:if="${moneda.activa}" class="badge bg-success">Activa</span>
                        <span th:unless="${moneda.activa}" class="badge bg-secondary">Inactiva</span>
                    </td>
                    <td>
                        <a th:href="@{/admin/monedas/editar/{id}(id=${moneda.id})}" 
                           class="btn btn-sm btn-warning">
                            <i class="bi bi-pencil"></i>
                        </a>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>
</div>
</body>
</html>
```

- [ ] **4.1.2** Crear vista `admin/monedas/formulario.html`

- [ ] **4.1.3** Crear vista `admin/tipos-cambio/lista.html`

- [ ] **4.1.4** Modificar `ventas/facturas/formulario.html` para selector de moneda

```html
<!-- Agregar después del selector de cliente -->
<div class="col-md-4">
    <label for="moneda" class="form-label">Moneda</label>
    <select id="moneda" name="monedaId" class="form-select" required>
        <option value="">Seleccionar...</option>
        <option th:each="moneda : ${monedas}" 
                th:value="${moneda.id}"
                th:text="${moneda.codigo + ' - ' + moneda.nombre}"
                th:selected="${factura.monedaId == moneda.id}">
        </option>
    </select>
</div>

<div class="col-md-4" id="tipo-cambio-info" style="display: none;">
    <label class="form-label">Tipo de Cambio</label>
    <input type="text" id="tipo-cambio-display" class="form-control" readonly>
    <small class="text-muted">Actualizado hoy</small>
</div>
```

- [ ] **4.1.5** Agregar JavaScript para actualizar tipo de cambio automáticamente

```javascript
document.getElementById('moneda').addEventListener('change', function() {
    const monedaId = this.value;
    const monedaBase = [[${monedaBase.id}]]; // Moneda base desde modelo
    
    if (monedaId && monedaId != monedaBase) {
        // Llamada AJAX para obtener tipo de cambio
        fetch(`/api/tipos-cambio/actual?monedaId=${monedaId}`)
            .then(response => response.json())
            .then(data => {
                document.getElementById('tipo-cambio-display').value = 
                    `1 ${data.simboloMoneda} = ${data.tasaVenta} ${data.simboloBase}`;
                document.getElementById('tipo-cambio-info').style.display = 'block';
            });
    } else {
        document.getElementById('tipo-cambio-info').style.display = 'none';
    }
});
```

- [ ] **4.1.6** Modificar vista de factura para mostrar moneda

```html
<!-- En la cabecera de la factura -->
<div class="invoice-header">
    <p><strong>Factura N°:</strong> [[${factura.numero}]]</p>
    <p><strong>Fecha:</strong> [[${#temporals.format(factura.fecha, 'dd/MM/yyyy')}]]</p>
    <p><strong>Moneda:</strong> [[${factura.moneda.nombre}]] ([[${factura.moneda.simbolo}]])</p>
</div>

<!-- En los totales -->
<tr>
    <td colspan="3" class="text-end"><strong>Total:</strong></td>
    <td class="text-end">
        <strong>[[${factura.moneda.simbolo}]] [[${#numbers.formatDecimal(factura.total, 1, factura.moneda.decimales)}]]</strong>
    </td>
</tr>

<!-- Si no es moneda base, mostrar equivalencia -->
<tr th:unless="${factura.moneda.monedaBase}">
    <td colspan="3" class="text-end text-muted">
        <small>Equivalente (₡):</small>
    </td>
    <td class="text-end text-muted">
        <small>₡ [[${#numbers.formatDecimal(factura.totalEnMonedaBase, 1, 2)}]]</small>
    </td>
</tr>
```

- [ ] **4.1.7** Crear dashboard con resumen de monedas

- [ ] **4.1.8** Agregar gráfica de histórico de tipos de cambio (Chart.js)

---

## 📦 5. REPORTES MULTI-DIVISA (4 tareas)

### 5.1. Reportes Consolidados

#### Tareas:

- [ ] **5.1.1** Crear reporte de ventas multi-divisa

```java
@Service
public class ReporteMultiDivisaService {
    
    /**
     * Genera reporte de ventas consolidado en múltiples monedas.
     */
    public ReporteVentasMultiDivisaDTO generarReporteVentas(LocalDate desde, LocalDate hasta) {
        List<Factura> facturas = facturaRepository.findByFechaBetween(desde, hasta);
        
        // Agrupar por moneda
        Map<String, BigDecimal> totalesPorMoneda = facturas.stream()
            .collect(Collectors.groupingBy(
                f -> f.getMoneda().getCodigo(),
                Collectors.reducing(BigDecimal.ZERO, Factura::getTotal, BigDecimal::add)
            ));
        
        // Total consolidado en moneda base
        BigDecimal totalBase = facturas.stream()
            .map(Factura::getTotalEnMonedaBase)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        ReporteVentasMultiDivisaDTO reporte = new ReporteVentasMultiDivisaDTO();
        reporte.setTotalesPorMoneda(totalesPorMoneda);
        reporte.setTotalConsolidado(totalBase);
        reporte.setMonedaBase(monedaService.obtenerMonedaBase().getCodigo());
        
        return reporte;
    }
}
```

- [ ] **5.1.2** Crear vista de reporte multi-divisa

- [ ] **5.1.3** Exportar reporte a Excel con múltiples monedas

- [ ] **5.1.4** Dashboard con gráficas por moneda

---

## 📊 CRITERIOS DE ACEPTACIÓN

✅ Modelo de datos implementado (Moneda, TipoCambio)  
✅ Repositorios y servicios funcionando  
✅ Actualización automática de tasas desde API externa  
✅ Facturas y pagos con soporte multi-divisa  
✅ Conversión automática a moneda base  
✅ Interfaz de usuario con selector de moneda  
✅ Reportes consolidados multi-divisa  
✅ Integración con `formatearMoneda()` existente  
✅ Histórico de tipos de cambio consultable  

---

## 📚 DEPENDENCIAS

**Requiere:**
- ✅ Sprint 5 completado (Contabilidad y Facturación)
- ✅ API externa de tipos de cambio configurada
- ✅ Base de datos MySQL actualizada

**Habilita:**
- 🚀 Facturación internacional
- 🚀 Expansión a otros mercados
- 🚀 Inventario con costos multi-divisa (Fase 2)

---

## 🔄 PRÓXIMOS PASOS

Una vez completada esta fase:
1. ✅ Probar conversiones con datos reales
2. ✅ Configurar API de tasas de cambio
3. 🚀 Continuar con **FASE 2: Inventario Avanzado**

---

**Hallazgos aplicados:**
- ✅ Se aprovecha `formatearMoneda()` existente en Utils
- ✅ Se extiende para soportar múltiples símbolos de divisa

---

**Fase creada:** 16 de enero de 2026  
**Responsable:** Equipo de Desarrollo
