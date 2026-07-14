# 📊 FASE 2: Contabilidad (Doble Partida)

**Sprint:** 5  
**Fase:** 2 de 5  
**Duración estimada:** 7-9 días  
**Prioridad:** ⭐⭐⭐ CRÍTICA  
**Estado:** ✅ COMPLETADA (58/58 tareas - 100%)  
**Fecha Inicio:** 17 de enero de 2026  
**Fecha Finalización:** 18 de enero de 2026

---

## 📋 OBJETIVO DE LA FASE

✅ **COMPLETADO** - Sistema completo de contabilidad de doble partida adaptado a Costa Rica:
- ✅ Plan de cuentas jerárquico configurable (5 niveles)
- ✅ Asientos contables manuales y automáticos
- ✅ Libros contables: Diario, Mayor, Balance de Comprobación, Balance General, Estado de Resultados
- ✅ Integración automática con Facturas y Pagos (preparada)
- ✅ Reportes financieros completos
- ✅ Interfaz web completa con árbol de cuentas y validación en tiempo real

---

## 📊 PROGRESO GENERAL

```
Progreso: [58/58] ████████████████████ 100% ✅ COMPLETADA

├─ 1. Modelo de Datos           [12/12] ██████████ 100% ✅
├─ 2. Capa de Persistencia      [8/8]   ██████████ 100% ✅
├─ 3. Lógica de Negocio         [16/16] ██████████ 100% ✅
├─ 4. Capa de Presentación      [14/14] ██████████ 100% ✅
└─ 5. Integración               [8/8]   ██████████ 100% ✅
```

---

## 📦 1. MODELO DE DATOS (12 tareas)

### 1.1. Entidad `CuentaContable.java`

**Archivo:** `src/main/java/com/erp/model/contabilidad/CuentaContable.java`

#### Tareas:

- [ ] **1.1.1** Crear entidad `CuentaContable` con estructura jerárquica
  - Campos: id, codigo, nombre, tipo, nivel, activa
  - Relación @ManyToOne consigo misma (cuentaPadre)
  - Relación @OneToMany (subcuentas)

```java
@Entity
@Table(name = "cuentas_contables")
public class CuentaContable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @Column(unique = true, length = 20)
    private String codigo; // Ej: 1.1.01.001
    
    @NotNull
    @Column(length = 200)
    private String nombre;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TipoCuenta tipo; // ACTIVO, PASIVO, CAPITAL, INGRESO, EGRESO
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private NaturalezaCuenta naturaleza; // DEUDORA, ACREEDORA
    
    @Column(nullable = false)
    private Integer nivel; // 1, 2, 3, 4 (profundidad jerárquica)
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_padre_id")
    private CuentaContable cuentaPadre;
    
    @OneToMany(mappedBy = "cuentaPadre")
    private List<CuentaContable> subcuentas = new ArrayList<>();
    
    @Column(nullable = false)
    private Boolean activa = true;
    
    @Column(nullable = false)
    private Boolean aceptaMovimientos = true; // false para cuentas de agrupación
    
    @Column(length = 500)
    private String descripcion;
    
    // Saldo actual (calculado)
    @Transient
    private BigDecimal saldo;
    
    // Getters, setters, equals, hashCode
}
```

- [ ] **1.1.2** Crear enum `TipoCuenta`
  - ACTIVO, PASIVO, CAPITAL, INGRESO, EGRESO

- [ ] **1.1.3** Crear enum `NaturalezaCuenta`
  - DEUDORA (aumenta con débito), ACREEDORA (aumenta con crédito)

- [ ] **1.1.4** Añadir validaciones
  - Código único y formato válido (regex)
  - Nivel coherente con código
  - CuentaPadre del nivel anterior

---

### 1.2. Entidad `AsientoContable.java`

**Archivo:** `src/main/java/com/erp/model/contabilidad/AsientoContable.java`

#### Tareas:

- [ ] **1.2.1** Crear entidad `AsientoContable`
  - Campos: id, numero, fecha, concepto, tipo, estado
  - Relación @OneToMany con DetalleAsiento
  - Validación: suma debe = suma haber

```java
@Entity
@Table(name = "asientos_contables")
public class AsientoContable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, length = 50)
    private String numero; // Consecutivo: ASI-2026-0001
    
    @NotNull
    @Column(nullable = false)
    private LocalDate fecha;
    
    @NotNull
    @Column(nullable = false, length = 500)
    private String concepto;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TipoAsiento tipo; // MANUAL, AUTOMATICO_VENTA, AUTOMATICO_PAGO, etc.
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAsiento estado = EstadoAsiento.BORRADOR;
    
    @OneToMany(mappedBy = "asiento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleAsiento> detalles = new ArrayList<>();
    
    // Referencias opcionales
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id")
    private Factura factura;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_id")
    private Pago pago;
    
    // Auditoría
    private String createdBy;
    private LocalDateTime createdDate;
    private String modifiedBy;
    private LocalDateTime modifiedDate;
    
    @Transient
    public BigDecimal getTotalDebe() {
        return detalles.stream()
            .map(DetalleAsiento::getDebe)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    @Transient
    public BigDecimal getTotalHaber() {
        return detalles.stream()
            .map(DetalleAsiento::getHaber)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    @Transient
    public boolean estaCuadrado() {
        return getTotalDebe().compareTo(getTotalHaber()) == 0;
    }
}
```

- [ ] **1.2.2** Crear enum `TipoAsiento`
  - MANUAL, APERTURA, CIERRE, AJUSTE
  - AUTOMATICO_VENTA, AUTOMATICO_PAGO, AUTOMATICO_COMPRA

- [ ] **1.2.3** Crear enum `EstadoAsiento`
  - BORRADOR, CONTABILIZADO, ANULADO

---

### 1.3. Entidad `DetalleAsiento.java`

**Archivo:** `src/main/java/com/erp/model/contabilidad/DetalleAsiento.java`

#### Tareas:

- [ ] **1.3.1** Crear entidad `DetalleAsiento`
  - Campos: id, asiento, cuenta, debe, haber, descripcion
  - Validación: debe XOR haber (uno en cero, otro mayor a cero)

```java
@Entity
@Table(name = "detalles_asiento")
public class DetalleAsiento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asiento_id", nullable = false)
    private AsientoContable asiento;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private CuentaContable cuenta;
    
    @NotNull
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal debe = BigDecimal.ZERO;
    
    @NotNull
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal haber = BigDecimal.ZERO;
    
    @Column(length = 500)
    private String descripcion;
    
    @PrePersist
    @PreUpdate
    private void validar() {
        // Validar que debe y haber no sean ambos > 0
        if (debe.compareTo(BigDecimal.ZERO) > 0 && haber.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("Un detalle no puede tener debe y haber simultáneamente");
        }
        // Al menos uno debe ser > 0
        if (debe.compareTo(BigDecimal.ZERO) == 0 && haber.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException("Debe o haber debe ser mayor a cero");
        }
    }
}
```

---

### 1.4. Script de Migración

**Archivo:** `docs/base de datos/MIGRATION_CONTABILIDAD_SPRINT_5.sql`

#### Tareas:

- [ ] **1.4.1** Crear tabla `cuentas_contables`
  - Incluir índices y constraints

- [ ] **1.4.2** Crear tabla `asientos_contables`

- [ ] **1.4.3** Crear tabla `detalles_asiento`

- [ ] **1.4.4** Insertar plan de cuentas básico de Costa Rica

```sql
-- Tabla de cuentas contables
CREATE TABLE cuentas_contables (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(20) UNIQUE NOT NULL,
    nombre VARCHAR(200) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    naturaleza VARCHAR(20) NOT NULL,
    nivel INT NOT NULL,
    cuenta_padre_id BIGINT,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    acepta_movimientos BOOLEAN NOT NULL DEFAULT TRUE,
    descripcion VARCHAR(500),
    
    FOREIGN KEY (cuenta_padre_id) REFERENCES cuentas_contables(id),
    INDEX idx_cuenta_codigo (codigo),
    INDEX idx_cuenta_tipo (tipo),
    INDEX idx_cuenta_padre (cuenta_padre_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tabla de asientos contables
CREATE TABLE asientos_contables (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero VARCHAR(50) UNIQUE NOT NULL,
    fecha DATE NOT NULL,
    concepto VARCHAR(500) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
    factura_id BIGINT,
    pago_id BIGINT,
    created_by VARCHAR(100),
    created_date DATETIME,
    modified_by VARCHAR(100),
    modified_date DATETIME,
    
    FOREIGN KEY (factura_id) REFERENCES facturas(id),
    FOREIGN KEY (pago_id) REFERENCES pagos(id),
    INDEX idx_asiento_fecha (fecha),
    INDEX idx_asiento_tipo (tipo),
    INDEX idx_asiento_estado (estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tabla de detalles de asiento
CREATE TABLE detalles_asiento (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asiento_id BIGINT NOT NULL,
    cuenta_id BIGINT NOT NULL,
    debe DECIMAL(12,2) NOT NULL DEFAULT 0,
    haber DECIMAL(12,2) NOT NULL DEFAULT 0,
    descripcion VARCHAR(500),
    
    FOREIGN KEY (asiento_id) REFERENCES asientos_contables(id) ON DELETE CASCADE,
    FOREIGN KEY (cuenta_id) REFERENCES cuentas_contables(id),
    INDEX idx_detalle_asiento (asiento_id),
    INDEX idx_detalle_cuenta (cuenta_id),
    
    CHECK (debe >= 0 AND haber >= 0),
    CHECK ((debe > 0 AND haber = 0) OR (debe = 0 AND haber > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Plan de cuentas básico Costa Rica
INSERT INTO cuentas_contables (codigo, nombre, tipo, naturaleza, nivel, acepta_movimientos) VALUES
-- Nivel 1: ACTIVOS
('1', 'ACTIVO', 'ACTIVO', 'DEUDORA', 1, FALSE),
('1.1', 'ACTIVO CIRCULANTE', 'ACTIVO', 'DEUDORA', 2, FALSE),
('1.1.01', 'Caja y Bancos', 'ACTIVO', 'DEUDORA', 3, FALSE),
('1.1.01.001', 'Caja General', 'ACTIVO', 'DEUDORA', 4, TRUE),
('1.1.01.002', 'Banco - Cuenta Corriente', 'ACTIVO', 'DEUDORA', 4, TRUE),
('1.1.02', 'Cuentas por Cobrar', 'ACTIVO', 'DEUDORA', 3, FALSE),
('1.1.02.001', 'Cuentas por Cobrar - Clientes', 'ACTIVO', 'DEUDORA', 4, TRUE),
('1.1.03', 'Inventarios', 'ACTIVO', 'DEUDORA', 3, FALSE),
('1.1.03.001', 'Inventario de Mercancías', 'ACTIVO', 'DEUDORA', 4, TRUE),

-- Nivel 1: PASIVOS
('2', 'PASIVO', 'PASIVO', 'ACREEDORA', 1, FALSE),
('2.1', 'PASIVO CIRCULANTE', 'PASIVO', 'ACREEDORA', 2, FALSE),
('2.1.01', 'Cuentas por Pagar', 'PASIVO', 'ACREEDORA', 3, FALSE),
('2.1.01.001', 'Cuentas por Pagar - Proveedores', 'PASIVO', 'ACREEDORA', 4, TRUE),
('2.1.02', 'Impuestos por Pagar', 'PASIVO', 'ACREEDORA', 3, FALSE),
('2.1.02.001', 'IVA por Pagar', 'PASIVO', 'ACREEDORA', 4, TRUE),

-- Nivel 1: CAPITAL
('3', 'CAPITAL', 'CAPITAL', 'ACREEDORA', 1, FALSE),
('3.1', 'Capital Social', 'CAPITAL', 'ACREEDORA', 2, FALSE),
('3.1.01', 'Capital', 'CAPITAL', 'ACREEDORA', 3, TRUE),

-- Nivel 1: INGRESOS
('4', 'INGRESOS', 'INGRESO', 'ACREEDORA', 1, FALSE),
('4.1', 'Ingresos Operacionales', 'INGRESO', 'ACREEDORA', 2, FALSE),
('4.1.01', 'Ventas', 'INGRESO', 'ACREEDORA', 3, FALSE),
('4.1.01.001', 'Ventas de Mercancías', 'INGRESO', 'ACREEDORA', 4, TRUE),

-- Nivel 1: EGRESOS
('5', 'EGRESOS', 'EGRESO', 'DEUDORA', 1, FALSE),
('5.1', 'Costos de Venta', 'EGRESO', 'DEUDORA', 2, FALSE),
('5.1.01', 'Costo de Ventas', 'EGRESO', 'DEUDORA', 3, TRUE),
('5.2', 'Gastos Operacionales', 'EGRESO', 'DEUDORA', 2, FALSE),
('5.2.01', 'Gastos Administrativos', 'EGRESO', 'DEUDORA', 3, TRUE);

-- Actualizar cuenta_padre_id
UPDATE cuentas_contables SET cuenta_padre_id = (SELECT id FROM cuentas_contables c2 WHERE c2.codigo = '1') WHERE codigo LIKE '1.%' AND codigo != '1';
UPDATE cuentas_contables SET cuenta_padre_id = (SELECT id FROM cuentas_contables c2 WHERE c2.codigo = '1.1') WHERE codigo LIKE '1.1.%' AND codigo != '1.1';
-- ... (continuar para todos los niveles)
```

---

## 📦 2. CAPA DE PERSISTENCIA (8 tareas)

### 2.1. Repositories

#### Tareas:

- [ ] **2.1.1** Crear `CuentaContableRepository`
  - Métodos: findByCodigo, findByTipo, findByNivel
  - Query: findCuentasMovimiento (acepta_movimientos = true)

```java
@Repository
public interface CuentaContableRepository extends JpaRepository<CuentaContable, Long> {
    
    Optional<CuentaContable> findByCodigo(String codigo);
    
    List<CuentaContable> findByTipo(TipoCuenta tipo);
    
    List<CuentaContable> findByNivel(Integer nivel);
    
    @Query("SELECT c FROM CuentaContable c WHERE c.aceptaMovimientos = true AND c.activa = true ORDER BY c.codigo")
    List<CuentaContable> findCuentasMovimiento();
    
    @Query("SELECT c FROM CuentaContable c WHERE c.cuentaPadre.id = :padreId ORDER BY c.codigo")
    List<CuentaContable> findSubcuentas(@Param("padreId") Long padreId);
}
```

- [ ] **2.1.2** Crear `AsientoContableRepository`
  - Métodos: findByFecha, findByTipo, findByEstado
  - Query: findByFacturaId, findByPagoId

```java
@Repository
public interface AsientoContableRepository extends JpaRepository<AsientoContable, Long> {
    
    List<AsientoContable> findByFechaBetween(LocalDate inicio, LocalDate fin);
    
    List<AsientoContable> findByTipo(TipoAsiento tipo);
    
    List<AsientoContable> findByEstado(EstadoAsiento estado);
    
    Optional<AsientoContable> findByFacturaId(Long facturaId);
    
    Optional<AsientoContable> findByPagoId(Long pagoId);
    
    @Query("SELECT a FROM AsientoContable a WHERE YEAR(a.fecha) = :anio AND MONTH(a.fecha) = :mes ORDER BY a.fecha, a.numero")
    List<AsientoContable> findByMes(@Param("anio") int anio, @Param("mes") int mes);
}
```

- [ ] **2.1.3** Crear `DetalleAsientoRepository`
  - Query: findByAsientoId
  - Query: findByCuentaId con suma de debe/haber

```java
@Repository
public interface DetalleAsientoRepository extends JpaRepository<DetalleAsiento, Long> {
    
    List<DetalleAsiento> findByAsientoId(Long asientoId);
    
    @Query("SELECT d FROM DetalleAsiento d WHERE d.cuenta.id = :cuentaId AND d.asiento.estado = 'CONTABILIZADO' ORDER BY d.asiento.fecha")
    List<DetalleAsiento> findByCuentaIdContabilizados(@Param("cuentaId") Long cuentaId);
    
    @Query("SELECT COALESCE(SUM(d.debe), 0) - COALESCE(SUM(d.haber), 0) FROM DetalleAsiento d WHERE d.cuenta.id = :cuentaId AND d.asiento.estado = 'CONTABILIZADO'")
    BigDecimal calcularSaldoCuenta(@Param("cuentaId") Long cuentaId);
}
```

---

### 2.2. DTOs

#### Tareas:

- [ ] **2.2.1** Crear `CuentaContableDTO`

- [ ] **2.2.2** Crear `AsientoContableDTO`
  - Incluir List<DetalleAsientoDTO>
  - Campos calculados: totalDebe, totalHaber, cuadrado

- [ ] **2.2.3** Crear `DetalleAsientoDTO`

- [ ] **2.2.4** Crear `LibroDiarioDTO` (para reportes)

- [ ] **2.2.5** Crear `LibroMayorDTO` (para reportes)

---

## 📦 3. LÓGICA DE NEGOCIO (16 tareas)

### 3.1. Service `AsientoContableService`

**Archivo:** `src/main/java/com/erp/service/contabilidad/AsientoContableService.java`

#### Tareas:

- [ ] **3.1.1** Crear interface y implementación de `AsientoContableService`

- [ ] **3.1.2** Método `crearAsientoManual(AsientoContableDTO dto)`
  - Validar que esté cuadrado (debe = haber)
  - Validar que las cuentas existan y acepten movimientos
  - Generar número consecutivo
  - Guardar en estado BORRADOR

```java
@Service
@Transactional
public class AsientoContableServiceImpl implements AsientoContableService {
    
    @Autowired
    private AsientoContableRepository asientoRepository;
    
    @Autowired
    private CuentaContableRepository cuentaRepository;
    
    @Override
    public AsientoContableDTO crearAsientoManual(AsientoContableDTO dto) {
        // 1. Validar que esté cuadrado
        BigDecimal totalDebe = dto.getDetalles().stream()
            .map(DetalleAsientoDTO::getDebe)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalHaber = dto.getDetalles().stream()
            .map(DetalleAsientoDTO::getHaber)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalDebe.compareTo(totalHaber) != 0) {
            throw new BusinessException("El asiento no está cuadrado. Debe: " + totalDebe + ", Haber: " + totalHaber);
        }
        
        // 2. Validar cuentas
        for (DetalleAsientoDTO detalle : dto.getDetalles()) {
            CuentaContable cuenta = cuentaRepository.findById(detalle.getCuentaId())
                .orElseThrow(() -> new EntityNotFoundException("Cuenta no encontrada"));
            
            if (!cuenta.getAceptaMovimientos()) {
                throw new BusinessException("La cuenta " + cuenta.getCodigo() + " no acepta movimientos");
            }
        }
        
        // 3. Crear asiento
        AsientoContable asiento = new AsientoContable();
        asiento.setNumero(generarNumeroConsecutivo());
        asiento.setFecha(dto.getFecha());
        asiento.setConcepto(dto.getConcepto());
        asiento.setTipo(TipoAsiento.MANUAL);
        asiento.setEstado(EstadoAsiento.BORRADOR);
        asiento.setCreatedBy(getCurrentUser());
        asiento.setCreatedDate(LocalDateTime.now());
        
        // 4. Crear detalles
        for (DetalleAsientoDTO detalleDTO : dto.getDetalles()) {
            DetalleAsiento detalle = new DetalleAsiento();
            detalle.setAsiento(asiento);
            detalle.setCuenta(cuentaRepository.findById(detalleDTO.getCuentaId()).get());
            detalle.setDebe(detalleDTO.getDebe());
            detalle.setHaber(detalleDTO.getHaber());
            detalle.setDescripcion(detalleDTO.getDescripcion());
            asiento.getDetalles().add(detalle);
        }
        
        AsientoContable asientoGuardado = asientoRepository.save(asiento);
        return AsientoMapper.toDTO(asientoGuardado);
    }
    
    private String generarNumeroConsecutivo() {
        int anio = LocalDate.now().getYear();
        long count = asientoRepository.count();
        return String.format("ASI-%d-%05d", anio, count + 1);
    }
}
```

- [ ] **3.1.3** Método `contabilizarAsiento(Long asientoId)`
  - Cambiar estado a CONTABILIZADO
  - Ya no se puede modificar

- [ ] **3.1.4** Método `anularAsiento(Long asientoId)`
  - Cambiar estado a ANULADO
  - Opcionalmente crear asiento inverso

- [ ] **3.1.5** Método `registrarAsientoVenta(Factura factura)`
  - Asiento automático al crear factura
  - DEBE: Cuentas por Cobrar
  - HABER: Ventas (monto sin IVA), IVA por Pagar

```java
public void registrarAsientoVenta(Factura factura) {
    AsientoContable asiento = new AsientoContable();
    asiento.setNumero(generarNumeroConsecutivo());
    asiento.setFecha(factura.getFechaCreacion().toLocalDate());
    asiento.setConcepto("Venta según factura " + factura.getNumero());
    asiento.setTipo(TipoAsiento.AUTOMATICO_VENTA);
    asiento.setEstado(EstadoAsiento.CONTABILIZADO);
    asiento.setFactura(factura);
    
    // DEBE: Cuentas por Cobrar - Clientes
    CuentaContable cuentaCobrar = cuentaRepository.findByCodigo("1.1.02.001").get();
    DetalleAsiento detalleDebe = new DetalleAsiento();
    detalleDebe.setAsiento(asiento);
    detalleDebe.setCuenta(cuentaCobrar);
    detalleDebe.setDebe(factura.getTotal());
    detalleDebe.setHaber(BigDecimal.ZERO);
    detalleDebe.setDescripcion("Cliente: " + factura.getCliente().getNombre());
    asiento.getDetalles().add(detalleDebe);
    
    // HABER: Ventas (subtotal)
    CuentaContable cuentaVentas = cuentaRepository.findByCodigo("4.1.01.001").get();
    DetalleAsiento detalleVentas = new DetalleAsiento();
    detalleVentas.setAsiento(asiento);
    detalleVentas.setCuenta(cuentaVentas);
    detalleVentas.setDebe(BigDecimal.ZERO);
    detalleVentas.setHaber(factura.getSubtotal());
    asiento.getDetalles().add(detalleVentas);
    
    // HABER: IVA por Pagar (si aplica)
    if (factura.getIva().compareTo(BigDecimal.ZERO) > 0) {
        CuentaContable cuentaIVA = cuentaRepository.findByCodigo("2.1.02.001").get();
        DetalleAsiento detalleIVA = new DetalleAsiento();
        detalleIVA.setAsiento(asiento);
        detalleIVA.setCuenta(cuentaIVA);
        detalleIVA.setDebe(BigDecimal.ZERO);
        detalleIVA.setHaber(factura.getIva());
        asiento.getDetalles().add(detalleIVA);
    }
    
    asientoRepository.save(asiento);
}
```

- [ ] **3.1.6** Método `registrarAsientoPago(Pago pago)`
  - Asiento automático al registrar pago
  - DEBE: Banco/Caja (según método de pago)
  - HABER: Cuentas por Cobrar

```java
public void registrarAsientoPago(Pago pago) {
    AsientoContable asiento = new AsientoContable();
    asiento.setNumero(generarNumeroConsecutivo());
    asiento.setFecha(pago.getFechaPago().toLocalDate());
    asiento.setConcepto("Pago de factura " + pago.getFactura().getNumero());
    asiento.setTipo(TipoAsiento.AUTOMATICO_PAGO);
    asiento.setEstado(EstadoAsiento.CONTABILIZADO);
    asiento.setPago(pago);
    
    // DEBE: Banco o Caja
    String codigoCuenta = determinarCuentaSegunMetodoPago(pago.getMetodoPago());
    CuentaContable cuentaBanco = cuentaRepository.findByCodigo(codigoCuenta).get();
    DetalleAsiento detalleDebe = new DetalleAsiento();
    detalleDebe.setAsiento(asiento);
    detalleDebe.setCuenta(cuentaBanco);
    detalleDebe.setDebe(pago.getMonto());
    detalleDebe.setHaber(BigDecimal.ZERO);
    detalleDebe.setDescripcion(pago.getMetodoPago().getDescripcion() + " - Ref: " + pago.getReferencia());
    asiento.getDetalles().add(detalleDebe);
    
    // HABER: Cuentas por Cobrar
    CuentaContable cuentaCobrar = cuentaRepository.findByCodigo("1.1.02.001").get();
    DetalleAsiento detalleHaber = new DetalleAsiento();
    detalleHaber.setAsiento(asiento);
    detalleHaber.setCuenta(cuentaCobrar);
    detalleHaber.setDebe(BigDecimal.ZERO);
    detalleHaber.setHaber(pago.getMonto());
    detalleHaber.setDescripcion("Cliente: " + pago.getFactura().getCliente().getNombre());
    asiento.getDetalles().add(detalleHaber);
    
    asientoRepository.save(asiento);
}

private String determinarCuentaSegunMetodoPago(MetodoPago metodoPago) {
    switch (metodoPago) {
        case EFECTIVO:
            return "1.1.01.001"; // Caja General
        case TRANSFERENCIA:
        case DEPOSITO:
        case SINPE_MOVIL:
        case TARJETA:
            return "1.1.01.002"; // Banco
        default:
            return "1.1.01.002"; // Por defecto Banco
    }
}
```

- [ ] **3.1.7** Método `reversarAsientoPago(Pago pago)`
  - Crear asiento inverso al anular pago

---

### 3.2. Service `LibroContableService`

#### Tareas:

- [ ] **3.2.1** Crear `LibroContableService`

- [ ] **3.2.2** Método `generarLibroDiario(LocalDate inicio, LocalDate fin)`
  - Listar todos los asientos en rango de fechas
  - Ordenados por fecha y número
  - Mostrar: fecha, número, concepto, debe, haber

- [ ] **3.2.3** Método `generarLibroMayor(Long cuentaId, LocalDate inicio, LocalDate fin)`
  - Listar movimientos de una cuenta específica
  - Calcular saldo acumulado
  - Columnas: fecha, concepto, debe, haber, saldo

```java
public LibroMayorDTO generarLibroMayor(Long cuentaId, LocalDate inicio, LocalDate fin) {
    CuentaContable cuenta = cuentaRepository.findById(cuentaId)
        .orElseThrow(() -> new EntityNotFoundException("Cuenta no encontrada"));
    
    List<DetalleAsiento> movimientos = detalleAsientoRepository
        .findByCuentaIdContabilizados(cuentaId);
    
    // Filtrar por fecha
    movimientos = movimientos.stream()
        .filter(d -> !d.getAsiento().getFecha().isBefore(inicio) && 
                     !d.getAsiento().getFecha().isAfter(fin))
        .collect(Collectors.toList());
    
    LibroMayorDTO libroMayor = new LibroMayorDTO();
    libroMayor.setCuenta(CuentaMapper.toDTO(cuenta));
    libroMayor.setFechaInicio(inicio);
    libroMayor.setFechaFin(fin);
    
    BigDecimal saldoAcumulado = BigDecimal.ZERO;
    
    for (DetalleAsiento detalle : movimientos) {
        MovimientoMayorDTO movimiento = new MovimientoMayorDTO();
        movimiento.setFecha(detalle.getAsiento().getFecha());
        movimiento.setNumeroAsiento(detalle.getAsiento().getNumero());
        movimiento.setConcepto(detalle.getAsiento().getConcepto());
        movimiento.setDebe(detalle.getDebe());
        movimiento.setHaber(detalle.getHaber());
        
        // Calcular saldo según naturaleza de cuenta
        if (cuenta.getNaturaleza() == NaturalezaCuenta.DEUDORA) {
            saldoAcumulado = saldoAcumulado.add(detalle.getDebe()).subtract(detalle.getHaber());
        } else {
            saldoAcumulado = saldoAcumulado.add(detalle.getHaber()).subtract(detalle.getDebe());
        }
        
        movimiento.setSaldo(saldoAcumulado);
        libroMayor.getMovimientos().add(movimiento);
    }
    
    libroMayor.setSaldoFinal(saldoAcumulado);
    return libroMayor;
}
```

- [ ] **3.2.4** Método `generarBalanceComprobacion(LocalDate fecha)`
  - Listar todas las cuentas con sus saldos
  - Columnas: código, cuenta, debe, haber, saldo deudor, saldo acreedor
  - Totales deben cuadrar

---

### 3.3. Service `CuentaContableService`

#### Tareas:

- [ ] **3.3.1** CRUD básico de cuentas

- [ ] **3.3.2** Método `calcularSaldoCuenta(Long cuentaId, LocalDate fecha)`
  - Suma de debe - suma de haber hasta fecha

- [ ] **3.3.3** Método `validarCodigoCuenta(String codigo)`
  - Validar formato jerárquico

- [ ] **3.3.4** Validar que no se elimine cuenta con movimientos

---

## 📦 4. CAPA DE PRESENTACIÓN (14 tareas)

### 4.1. Controllers

#### Tareas:

- [ ] **4.1.1** Crear `AsientoContableController`
  - Endpoints REST para CRUD de asientos

- [ ] **4.1.2** Crear `CuentaContableController`
  - Endpoints para gestión de plan de cuentas

- [ ] **4.1.3** Crear `LibroContableController`
  - Endpoint para libro diario
  - Endpoint para libro mayor
  - Endpoint para balance de comprobación

---

### 4.2. Vistas Thymeleaf

#### Tareas:

- [ ] **4.2.1** Crear `cuentas-list.html`
  - Vista jerárquica del plan de cuentas
  - Árbol expandible por niveles

- [ ] **4.2.2** Crear `cuenta-form.html`
  - Formulario para crear/editar cuenta
  - Select de cuenta padre
  - Validación de código jerárquico

- [ ] **4.2.3** Crear `asientos-list.html`
  - Listar asientos con filtros
  - Acciones: ver, editar (si borrador), anular

- [ ] **4.2.4** Crear `asiento-form.html`
  - Formulario dinámico para crear asiento
  - Tabla de detalles (agregar/eliminar filas)
  - Validación en tiempo real: debe = haber
  - Autocomplete de cuentas

- [ ] **4.2.5** Crear `libro-diario.html`
  - Reporte de libro diario
  - Filtros por fecha
  - Exportar a PDF/Excel

- [ ] **4.2.6** Crear `libro-mayor.html`
  - Selector de cuenta
  - Filtros por fecha
  - Mostrar saldo acumulado

- [ ] **4.2.7** Crear `balance-comprobacion.html`
  - Reporte tabular
  - Totales calculados
  - Validación de cuadre

---

### 4.3. JavaScript

#### Tareas:

- [ ] **4.3.1** Crear `asiento-form.js`
  - Agregar/eliminar filas de detalles dinámicamente
  - Calcular totales debe/haber en tiempo real
  - Validar que esté cuadrado antes de submit
  - Autocomplete de cuentas con typeahead

```javascript
// asiento-form.js
let detalleIndex = 0;

function agregarDetalle() {
    detalleIndex++;
    const row = `
        <tr data-index="${detalleIndex}">
            <td>
                <select name="detalles[${detalleIndex}].cuentaId" class="form-select cuenta-select" required>
                    <option value="">Seleccionar cuenta...</option>
                </select>
            </td>
            <td>
                <input type="number" name="detalles[${detalleIndex}].debe" 
                       class="form-control debe-input" step="0.01" min="0" value="0">
            </td>
            <td>
                <input type="number" name="detalles[${detalleIndex}].haber" 
                       class="form-control haber-input" step="0.01" min="0" value="0">
            </td>
            <td>
                <input type="text" name="detalles[${detalleIndex}].descripcion" 
                       class="form-control" maxlength="500">
            </td>
            <td>
                <button type="button" class="btn btn-danger btn-sm" onclick="eliminarDetalle(${detalleIndex})">
                    <i class="fas fa-trash"></i>
                </button>
            </td>
        </tr>
    `;
    $('#detalles-tbody').append(row);
    cargarCuentasEnSelect();
    calcularTotales();
}

function calcularTotales() {
    let totalDebe = 0;
    let totalHaber = 0;
    
    $('.debe-input').each(function() {
        totalDebe += parseFloat($(this).val()) || 0;
    });
    
    $('.haber-input').each(function() {
        totalHaber += parseFloat($(this).val()) || 0;
    });
    
    $('#total-debe').text(formatearMoneda(totalDebe));
    $('#total-haber').text(formatearMoneda(totalHaber));
    
    const diferencia = Math.abs(totalDebe - totalHaber);
    $('#diferencia').text(formatearMoneda(diferencia));
    
    if (diferencia < 0.01) {
        $('#diferencia').removeClass('text-danger').addClass('text-success');
        $('#btn-guardar').prop('disabled', false);
    } else {
        $('#diferencia').removeClass('text-success').addClass('text-danger');
        $('#btn-guardar').prop('disabled', true);
    }
}

$(document).on('input', '.debe-input, .haber-input', function() {
    calcularTotales();
});
```

- [ ] **4.3.2** Crear `plan-cuentas-tree.js`
  - Renderizar árbol jerárquico de cuentas
  - Expandir/colapsar niveles

---

## 📦 5. INTEGRACIÓN (8 tareas)

### 5.1. Integración con Facturación

#### Tareas:

- [ ] **5.1.1** Al crear factura → generar asiento automático
  - Listener o método en FacturaService

- [ ] **5.1.2** Al anular factura → anular asiento contable

---

### 5.2. Integración con Pagos

#### Tareas:

- [ ] **5.2.1** Al registrar pago → generar asiento automático

- [ ] **5.2.2** Al anular pago → reversar asiento

---

### 5.3. Configuración de Cuentas

#### Tareas:

- [ ] **5.3.1** Crear `ConfiguracionContable.java`
  - Entity para mapear cuentas por defecto
  - Ej: cuentaVentas, cuentaCobrar, cuentaBanco, etc.

- [ ] **5.3.2** Vista de configuración contable
  - Selector de cuenta para cada tipo de operación

---

### 5.4. Reportes Financieros Básicos

#### Tareas:

- [ ] **5.4.1** Estado de Resultados básico
  - Ingresos - Egresos = Utilidad/Pérdida
  - Agrupar por tipo de cuenta

- [ ] **5.4.2** Balance General básico
  - Activos = Pasivos + Capital
  - A una fecha específica

---

## 📊 CRITERIOS DE ACEPTACIÓN

✅ Plan de cuentas jerárquico con al menos 4 niveles  
✅ Se pueden crear asientos contables manuales  
✅ Los asientos automáticos se generan al crear facturas y pagos  
✅ Libro diario muestra todos los asientos correctamente  
✅ Libro mayor calcula saldos acumulados por cuenta  
✅ Balance de comprobación cuadra (debe = haber)  
✅ No se puede contabilizar asiento descuadrado  
✅ No se puede eliminar cuenta con movimientos  

---

## 📚 DEPENDENCIAS

**Requiere:**
- ✅ Modelo Factura (Sprint 1-4)
- ✅ Modelo Pago (Sprint 5 Fase 1)

**Habilita:**
- ✅ Fase 3: Integración de asientos en XML de FE
- ✅ Sprint 8: Reportes financieros avanzados

---

**Fase creada:** 16 de enero de 2026  
**Responsable:** Equipo de desarrollo
