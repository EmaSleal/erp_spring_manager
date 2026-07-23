# FASE 1: Recursos Humanos (RRHH)

**Sprint:** 8  
**Fase:** 1 de 5  
**Duración estimada:** 8-10 días  
**Prioridad:** CRÍTICA (si se gestiona personal)  
**Estado:** PENDIENTE  
**Última revisión:** 2026-07-21 — ajuste por análisis de codebase + cumplimiento normativo CR

---

## Convenciones del proyecto (obligatorias)

Estas reglas toman precedencia sobre cualquier ejemplo en este documento:

- **Lombok:** anotaciones individuales únicamente (`@Getter @Setter @ToString @NoArgsConstructor @AllArgsConstructor`). **Nunca `@Data`.**
- **IDs:** `Long id` con `@GeneratedValue(strategy = GenerationType.IDENTITY)`.
- **Audit:** `@PrePersist` / `@PreUpdate` con campos `createdAt` / `updatedAt`. No usar `@EntityListeners(AuditingEntityListener.class)`.
- **Servicios:** siempre interfaz + implementación separadas. Inyección por constructor (`@RequiredArgsConstructor`). Nunca `@Autowired`.
- **Enums:** `@Enumerated(EnumType.STRING)` siempre.
- **Permisos:** sistema existente en `Permiso.java` + `MatrizPermisos.java`. ADMIN recibe todos automáticamente; GERENTE necesita asignación explícita.
- **Flyway:** próxima versión es `V3`. Formato: `V{N}__{snake_case}.sql`. Solo migraciones aditivas.
- **Paquete:** `modules/rrhh/` (model, enums, repository, service, controller, dto) + `templates/modules/rrhh/`.

---

## Scope de esta fase

**Incluido:**
- Catálogos base: Departamento, Puesto
- Catálogos regulatorios versionables: ParametroCCSS, TramoImpuestoSalario, SalarioMinimo
- Entidad central: Empleado (con campos normativos CR)
- Historial laboral: ContratoEmpleado
- Permisos y ausencias: Ausencia (con tipos legales CR)

**Excluido (deferred):**
- Control de asistencia con marcas de tiempo — requiere hardware o app móvil
- Evaluaciones de desempeño — necesita diseño propio
- Portal del empleado
- Reportes SICERE y TRIBU-CR — pertenecen a Nómina (Fase 2)

---

## Progreso

```
Progreso: [0/38] 0%

├─ 1. Catálogos base                  [0/6]
├─ 2. Catálogos regulatorios          [0/4]
├─ 3. Empleado                        [0/8]
├─ 4. ContratoEmpleado                [0/6]
├─ 5. Ausencia                        [0/6]
├─ 6. Permisos + sidebar              [0/3]
└─ 7. Vistas principales              [0/5]
```

---

## 1. Catálogos base: Departamento y Puesto

### Entidad `Departamento`

```java
@Entity
@Table(name = "departamentos")
@Getter @Setter @ToString @NoArgsConstructor @AllArgsConstructor
public class Departamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(unique = true, length = 10)
    private String codigo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
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
```

**Nota:** el campo `jefe_id` (FK al Empleado jefe) se agrega en una segunda migración para evitar la dependencia circular con `Empleado`. El `presupuestoMensual` también es optional para V1.

---

### Entidad `Puesto`

```java
@Entity
@Table(name = "puestos")
@Getter @Setter @ToString @NoArgsConstructor @AllArgsConstructor
public class Puesto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departamento_id", nullable = false)
    private Departamento departamento;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal salarioBase;

    @Column(precision = 19, scale = 2)
    private BigDecimal salarioMaximo;

    /**
     * Código de categoría del Decreto MTSS para validar salario mínimo.
     * Ejemplos: TONC (Trabajador No Calificado), TOC, TES, TOE, TOSC.
     * Se actualiza con cada decreto semestral.
     */
    @Column(length = 20)
    private String categoriaSalarialMinima;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
```

**Validación de negocio:** `PuestoService.crear()` debe verificar que `salarioBase >= SalarioMinimo.vigente(categoriaSalarialMinima)`.

---

### Migración V3 — Catálogos base

```sql
-- V3__rrhh_catalogos_base.sql

CREATE TABLE departamentos (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre          VARCHAR(100) NOT NULL UNIQUE,
    codigo          VARCHAR(10)  UNIQUE,
    descripcion     TEXT,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_activo (activo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE puestos (
    id                        BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre                    VARCHAR(100) NOT NULL,
    descripcion               TEXT,
    departamento_id           BIGINT       NOT NULL,
    salario_base              DECIMAL(19,2) NOT NULL,
    salario_maximo            DECIMAL(19,2),
    categoria_salarial_minima VARCHAR(20),
    activo                    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (departamento_id) REFERENCES departamentos(id),
    INDEX idx_departamento (departamento_id),
    INDEX idx_activo (activo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Datos iniciales
INSERT INTO departamentos (nombre, codigo, descripcion) VALUES
('Administración',    'ADM',  'Departamento administrativo general'),
('Ventas',            'VNT',  'Ventas y atención al cliente'),
('Contabilidad',      'CONT', 'Contabilidad y finanzas'),
('Operaciones',       'OPE',  'Operaciones'),
('Recursos Humanos',  'RRHH', 'Gestión de personal');
```

---

## 2. Catálogos regulatorios versionables

**Regla crítica:** nunca hardcodear porcentajes ni tramos. El IVM cambia el 1/1/2026 y de nuevo el 1/1/2029. Los tramos de renta se ajustan cada año por IPC. El salario mínimo se actualiza cada enero y julio.

### Entidad `ParametroCCSS`

```java
@Entity
@Table(name = "parametros_ccss")
@Getter @Setter @ToString @NoArgsConstructor @AllArgsConstructor
public class ParametroCCSS {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Aporte obrero total: 10.83% desde 1-ene-2026 hasta 31-dic-2028
    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal porcentajeObrero;       // ej: 0.1083

    // Aporte patronal total: 26.83% (25.33% si < 5 empleados no agrícola)
    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal porcentajePatronal;     // ej: 0.2683

    // Desglose obrero
    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal porcentajeSem;          // SEM obrero: 0.0550
    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal porcentajeIvmObrero;    // IVM obrero: 0.0433
    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal porcentajeBpObrero;     // Banco Popular obrero: 0.0100

    // Desglose patronal (resumido)
    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal porcentajeFcl;          // FCL: 0.0150
    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal porcentajeRop;          // ROP patronal: 0.0200 + 0.0125 obrero

    // Base mínima contributiva
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal baseMinimaContributivaSem;   // 2026: ₡333,328
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal baseMinimaContributivaIvm;   // 2026: ₡311,990

    @Column(nullable = false)
    private LocalDate vigenciaDesde;

    @Column                                         // null = vigente indefinidamente
    private LocalDate vigenciaHasta;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
```

### Entidad `TramoImpuestoSalario`

```java
@Entity
@Table(name = "tramos_impuesto_salario")
@Getter @Setter @ToString @NoArgsConstructor @AllArgsConstructor
public class TramoImpuestoSalario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer anioVigencia;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal limiteInferior;          // 0 = tramo exento

    @Column(precision = 19, scale = 2)          // null = sin límite superior
    private BigDecimal limiteSuperior;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal porcentaje;              // 0.00 / 0.10 / 0.15 / 0.20 / 0.25

    // Créditos fiscales mensuales 2026
    // creditoPorHijo: ₡1,710 | creditoPorConyuge: ₡2,590
    // Se almacenan en la fila del tramo exento (limiteInferior=0) para no duplicar entidad
    @Column(precision = 19, scale = 2)
    private BigDecimal creditoPorHijo;
    @Column(precision = 19, scale = 2)
    private BigDecimal creditoPorConyuge;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
```

### Entidad `SalarioMinimo`

```java
@Entity
@Table(name = "salarios_minimos")
@Getter @Setter @ToString @NoArgsConstructor @AllArgsConstructor
public class SalarioMinimo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Código de categoría del Decreto MTSS.
     * Ejemplos: TONC, TOSC, TOC, TES, TOE.
     */
    @Column(nullable = false, length = 20)
    private String categoria;

    @Column(nullable = false, length = 200)
    private String descripcionCategoria;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal montoMensual;

    @Column(nullable = false)
    private LocalDate vigenciaDesde;

    @Column
    private LocalDate vigenciaHasta;            // null = vigente

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
```

### Migración V4 — Catálogos regulatorios

```sql
-- V4__rrhh_catalogos_regulatorios.sql

CREATE TABLE parametros_ccss (
    id                              BIGINT AUTO_INCREMENT PRIMARY KEY,
    porcentaje_obrero               DECIMAL(5,4) NOT NULL,
    porcentaje_patronal             DECIMAL(5,4) NOT NULL,
    porcentaje_sem                  DECIMAL(5,4) NOT NULL,
    porcentaje_ivm_obrero           DECIMAL(5,4) NOT NULL,
    porcentaje_bp_obrero            DECIMAL(5,4) NOT NULL,
    porcentaje_fcl                  DECIMAL(5,4) NOT NULL,
    porcentaje_rop                  DECIMAL(5,4) NOT NULL,
    base_minima_contributiva_sem    DECIMAL(19,2) NOT NULL,
    base_minima_contributiva_ivm    DECIMAL(19,2) NOT NULL,
    vigencia_desde                  DATE         NOT NULL,
    vigencia_hasta                  DATE,
    created_at                      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_vigencia (vigencia_desde, vigencia_hasta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tasas vigentes 1-ene-2026 hasta 31-dic-2028 (IVM sube de nuevo el 1-ene-2029)
INSERT INTO parametros_ccss (
    porcentaje_obrero, porcentaje_patronal,
    porcentaje_sem, porcentaje_ivm_obrero, porcentaje_bp_obrero,
    porcentaje_fcl, porcentaje_rop,
    base_minima_contributiva_sem, base_minima_contributiva_ivm,
    vigencia_desde, vigencia_hasta
) VALUES (
    0.1083, 0.2683,
    0.0550, 0.0433, 0.0100,
    0.0150, 0.0200,
    333328.00, 311990.00,
    '2026-01-01', '2028-12-31'
);

CREATE TABLE tramos_impuesto_salario (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    anio_vigencia    INT          NOT NULL,
    limite_inferior  DECIMAL(19,2) NOT NULL,
    limite_superior  DECIMAL(19,2),
    porcentaje       DECIMAL(5,4) NOT NULL,
    credito_por_hijo    DECIMAL(19,2),
    credito_por_conyuge DECIMAL(19,2),
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_anio (anio_vigencia)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tramos 2026 (Decreto 45333-H, vigente 1-ene-2026)
INSERT INTO tramos_impuesto_salario (anio_vigencia, limite_inferior, limite_superior, porcentaje, credito_por_hijo, credito_por_conyuge) VALUES
(2026,       0.00,   918000.00, 0.0000, 1710.00, 2590.00),
(2026,  918000.00,  1347000.00, 0.1000, NULL, NULL),
(2026, 1347000.00,  2364000.00, 0.1500, NULL, NULL),
(2026, 2364000.00,  4727000.00, 0.2000, NULL, NULL),
(2026, 4727000.00,        NULL, 0.2500, NULL, NULL);

CREATE TABLE salarios_minimos (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    categoria             VARCHAR(20)  NOT NULL,
    descripcion_categoria VARCHAR(200) NOT NULL,
    monto_mensual         DECIMAL(19,2) NOT NULL,
    vigencia_desde        DATE         NOT NULL,
    vigencia_hasta        DATE,
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_categoria (categoria),
    INDEX idx_vigencia (vigencia_desde, vigencia_hasta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Salarios mínimos 2026 (Decreto 45303-MTSS, vigente 1-ene-2026)
INSERT INTO salarios_minimos (categoria, descripcion_categoria, monto_mensual, vigencia_desde, vigencia_hasta) VALUES
('TONC',  'Trabajador No Calificado Genérico',             373092.30, '2026-01-01', '2026-06-30'),
('TOSC',  'Trabajador Semi Calificado',                    396000.00, '2026-01-01', '2026-06-30'),
('TOC',   'Trabajador Calificado',                         420000.00, '2026-01-01', '2026-06-30'),
('TES',   'Técnico de Educación Secundaria',               530000.00, '2026-01-01', '2026-06-30'),
('TOE',   'Universitario / Especializado',                 796921.00, '2026-01-01', '2026-06-30'),
('DOM',   'Trabajo doméstico',                             268731.31, '2026-01-01', '2026-06-30');
```

---

## 3. Entidad `Empleado`

### Enums necesarios

```java
// modules/rrhh/enums/
public enum EstadoEmpleado  { ACTIVO, DE_BAJA_TEMPORAL, BAJA_DEFINITIVA }
public enum Genero          { MASCULINO, FEMENINO, OTRO }
public enum EstadoCivil     { SOLTERO, CASADO, DIVORCIADO, VIUDO, UNION_LIBRE }
```

### Entidad

```java
@Entity
@Table(name = "empleados")
@Getter @Setter @ToString @NoArgsConstructor @AllArgsConstructor
public class Empleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Datos de identidad (Art. 24 CT) ---
    @Column(nullable = false, unique = true, length = 20)
    private String cedula;                      // cédula o pasaporte

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String primerApellido;

    @Column(length = 100)
    private String segundoApellido;

    @Column(length = 10)
    @Enumerated(EnumType.STRING)
    private Genero genero;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private EstadoCivil estadoCivil;

    // --- Contacto ---
    @Column(unique = true, length = 100)
    private String email;

    @Column(length = 15)
    private String telefono;

    @Column(columnDefinition = "TEXT")
    private String direccion;

    @Column
    private LocalDate fechaNacimiento;

    // --- Cargo ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departamento_id", nullable = false)
    private Departamento departamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "puesto_id", nullable = false)
    private Puesto puesto;

    // --- Información laboral ---
    @Column(nullable = false)
    private LocalDate fechaIngreso;

    @Column
    private LocalDate fechaSalida;              // null = activo

    @Column(length = 500)
    private String motivoSalida;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoEmpleado estado = EstadoEmpleado.ACTIVO;

    @Column(nullable = false)
    private Boolean activo = true;

    // --- Campos normativos CR (requeridos para SICERE / Nómina) ---
    @Column(unique = true, length = 20)
    private String numeroAseguradoCCSS;

    /**
     * Operadora de Pensiones Complementaria elegida por el empleado (Ley 7983).
     * Si no elige, se asigna OPC-CCSS por defecto.
     */
    @Column(length = 100)
    private String operadoraPensionesROP;

    /**
     * Número de hijos registrados para crédito fiscal (₡1,710/hijo mensual).
     */
    @Column(nullable = false)
    private Integer hijosCargaFamiliar = 0;

    /**
     * true si tiene cónyuge registrado para crédito fiscal (₡2,590 mensual).
     */
    @Column(nullable = false)
    private Boolean conyugeCargaFamiliar = false;

    /**
     * Porcentaje de deducción para asociación solidarista (típico 3-5% del bruto).
     * null si no está afiliado.
     */
    @Column(precision = 5, scale = 4)
    private BigDecimal porcentajeSolidarista;

    /**
     * Monto mensual de orden de pensión alimentaria (Art. 64 Ley 7654).
     * null si no aplica. Tiene prelación sobre embargos civiles.
     */
    @Column(precision = 19, scale = 2)
    private BigDecimal montoPensionAlimentaria;

    // --- Datos bancarios para depósito de salario ---
    @Column(length = 30)
    private String cuentaBancaria;

    @Column(length = 100)
    private String banco;

    // --- Contacto de emergencia ---
    @Column(length = 200)
    private String contactoEmergenciaNombre;

    @Column(length = 15)
    private String contactoEmergenciaTelefono;

    @Column(length = 50)
    private String contactoEmergenciaRelacion;

    // --- Foto ---
    @Column(length = 500)
    private String fotoUrl;

    // --- Usuario del sistema (opcional — no todo empleado tiene cuenta ERP) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")            // nullable por diseño
    private Usuario usuario;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    // --- Helpers ---
    public String getNombreCompleto() {
        String completo = nombre + " " + primerApellido;
        if (segundoApellido != null && !segundoApellido.isBlank()) {
            completo += " " + segundoApellido;
        }
        return completo;
    }

    public int getAntiguedadAnios() {
        LocalDate fin = (fechaSalida != null) ? fechaSalida : LocalDate.now();
        return Period.between(fechaIngreso, fin).getYears();
    }
}
```

### Migración V5 — Empleados

```sql
-- V5__rrhh_empleados.sql

CREATE TABLE empleados (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    cedula                      VARCHAR(20)  NOT NULL UNIQUE,
    nombre                      VARCHAR(200) NOT NULL,
    primer_apellido             VARCHAR(100) NOT NULL,
    segundo_apellido            VARCHAR(100),
    genero                      VARCHAR(10),
    estado_civil                VARCHAR(20),
    email                       VARCHAR(100) UNIQUE,
    telefono                    VARCHAR(15),
    direccion                   TEXT,
    fecha_nacimiento            DATE,
    departamento_id             BIGINT       NOT NULL,
    puesto_id                   BIGINT       NOT NULL,
    fecha_ingreso               DATE         NOT NULL,
    fecha_salida                DATE,
    motivo_salida               VARCHAR(500),
    estado                      VARCHAR(30)  NOT NULL DEFAULT 'ACTIVO',
    activo                      BOOLEAN      NOT NULL DEFAULT TRUE,
    numero_asegurado_ccss       VARCHAR(20)  UNIQUE,
    operadora_pensiones_rop     VARCHAR(100),
    hijos_carga_familiar        INT          NOT NULL DEFAULT 0,
    conyuge_carga_familiar      BOOLEAN      NOT NULL DEFAULT FALSE,
    porcentaje_solidarista      DECIMAL(5,4),
    monto_pension_alimentaria   DECIMAL(19,2),
    cuenta_bancaria             VARCHAR(30),
    banco                       VARCHAR(100),
    contacto_emergencia_nombre  VARCHAR(200),
    contacto_emergencia_telefono VARCHAR(15),
    contacto_emergencia_relacion VARCHAR(50),
    foto_url                    VARCHAR(500),
    usuario_id                  INT,           -- FK to usuario.id_usuario (INT, singular table name)
    created_at                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (departamento_id) REFERENCES departamentos(id),
    FOREIGN KEY (puesto_id)       REFERENCES puestos(id),
    FOREIGN KEY (usuario_id)      REFERENCES usuario(id_usuario),
    INDEX idx_cedula (cedula),
    INDEX idx_activo (activo),
    INDEX idx_departamento (departamento_id),
    INDEX idx_fecha_ingreso (fecha_ingreso)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 4. Entidad `ContratoEmpleado`

El salario vive en el contrato, no en el empleado. Un empleado puede tener varios contratos a lo largo del tiempo. Solo uno puede estar activo (`activo = true`).

### Enums

```java
public enum TipoContrato {
    INDEFINIDO,             // Regla general (Art. 18 CT)
    PLAZO_FIJO,             // Máx. 12 meses; requiere justificación (Art. 26)
    OBRA_DETERMINADA,       // Termina al completar la obra (Art. 26)
    TIEMPO_PARCIAL,         // Proporcional en salario y cargas
    APRENDIZAJE             // Régimen especial
}

public enum TipoJornada {
    DIURNA,     // 5:00-19:00, máx. 8h/día, 48h/semana
    NOCTURNA,   // 19:00-5:00, máx. 6h/día, 36h/semana
    MIXTA       // máx. 7h/día, 42h/semana
}

public enum FormaPago    { TIEMPO, OBRA, COMISION, MIXTA }
public enum PeriodicidadPago { SEMANAL, QUINCENAL, MENSUAL }

public enum CausaTerminacion {
    RENUNCIA_VOLUNTARIA,        // Empleado renuncia → paga vacaciones + aguinaldo
    DESPIDO_CON_RESPONSABILIDAD,// Sin justa causa → paga preaviso + cesantía + vacaciones + aguinaldo
    DESPIDO_SIN_RESPONSABILIDAD,// Falta grave (Art. 81) → no paga preaviso ni cesantía
    MUTUO_ACUERDO,
    FIN_PLAZO,                  // Contrato a plazo vencido
    FIN_OBRA,                   // Obra concluida
    FALLECIMIENTO
}
```

### Entidad

```java
@Entity
@Table(name = "contratos_empleado")
@Getter @Setter @ToString @NoArgsConstructor @AllArgsConstructor
public class ContratoEmpleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoContrato tipoContrato;

    /**
     * Obligatorio si tipoContrato = PLAZO_FIJO u OBRA_DETERMINADA (Art. 26 CT).
     * Debe justificar por qué no es indefinido.
     */
    @Column(length = 500)
    private String justificacionTemporalidad;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column                                     // null = indefinido
    private LocalDate fechaFin;

    /**
     * Fin del período de prueba (3 meses desde fechaInicio).
     * Durante este período el patrono puede rescindir sin responsabilidad.
     */
    @Column
    private LocalDate fechaFinPeriodoPrueba;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal salarioPactado;          // salario bruto mensual acordado

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoJornada tipoJornada = TipoJornada.DIURNA;

    @Column(nullable = false)
    private Integer horasSemanales = 48;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FormaPago formaPago = FormaPago.TIEMPO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PeriodicidadPago periodicidadPago = PeriodicidadPago.MENSUAL;

    @Column(length = 300)
    private String lugarTrabajo;                // requerido por Art. 24 CT

    // --- Terminación ---
    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private CausaTerminacion causaTerminacion;

    @Column
    private LocalDate fechaTerminacion;

    @Column(length = 1000)
    private String observaciones;

    @Column(nullable = false)
    private Boolean activo = true;              // solo un contrato activo por empleado

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
```

### Migración V6 — Contratos

```sql
-- V6__rrhh_contratos_ausencias.sql

CREATE TABLE contratos_empleado (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    empleado_id                 BIGINT       NOT NULL,
    tipo_contrato               VARCHAR(30)  NOT NULL,
    justificacion_temporalidad  VARCHAR(500),
    fecha_inicio                DATE         NOT NULL,
    fecha_fin                   DATE,
    fecha_fin_periodo_prueba    DATE,
    salario_pactado             DECIMAL(19,2) NOT NULL,
    tipo_jornada                VARCHAR(20)  NOT NULL DEFAULT 'DIURNA',
    horas_semanales             INT          NOT NULL DEFAULT 48,
    forma_pago                  VARCHAR(20)  NOT NULL DEFAULT 'TIEMPO',
    periodicidad_pago           VARCHAR(20)  NOT NULL DEFAULT 'MENSUAL',
    lugar_trabajo               VARCHAR(300),
    causa_terminacion           VARCHAR(40),
    fecha_terminacion           DATE,
    observaciones               TEXT,
    activo                      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (empleado_id) REFERENCES empleados(id),
    INDEX idx_empleado (empleado_id),
    INDEX idx_activo (activo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 5. Entidad `Ausencia`

Los tipos de ausencia en CR afectan distinto la nómina, el aguinaldo y la antigüedad. No es suficiente con un enum genérico.

### Enum `TipoAusencia`

```java
public enum TipoAusencia {
    // Incapacidades
    INCAPACIDAD_CCSS,           // Días 1-3: patrono 50%/CCSS 50%; día 4+: CCSS 60%/patrono 0%
    INCAPACIDAD_INS,            // Riesgo laboral: INS paga 60% desde día 1
    // Licencias con goce legal
    MATERNIDAD,                 // 4 meses (1 pre + 3 post), 50/50, SÍ computa aguinaldo y antigüedad
    PATERNIDAD,                 // 8 días distribuidos en 4 semanas (Ley 10211)
    DUELO,                      // 2 días con goce de salario
    // Vacaciones
    VACACIONES,                 // Art. 153 CT: mínimo 2 semanas por 50 semanas trabajadas
    // Permisos discrecionales
    PERMISO_CON_GOCE,           // Patrono paga 100%; SÍ computa
    PERMISO_SIN_GOCE            // Suspende contrato; NO computa aguinaldo ni antigüedad
}
```

### Entidad

```java
@Entity
@Table(name = "ausencias")
@Getter @Setter @ToString @NoArgsConstructor @AllArgsConstructor
public class Ausencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoAusencia tipo;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaFin;

    @Column(length = 500)
    private String motivo;

    // --- Datos de la entidad certificante ---
    /**
     * Quién certifica: CCSS, INS, MTSS, PATRONO.
     */
    @Column(length = 50)
    private String entidadCertificante;

    /**
     * Número de boleta médica (EDUS) o resolución INS.
     */
    @Column(length = 100)
    private String numeroBoleta;

    // --- Impacto económico (precalculado para facilitar nómina) ---
    /**
     * Porcentaje que paga el patrono sobre el salario (0.00 a 1.00).
     * Ej: INCAPACIDAD_CCSS días 1-3 → 0.50
     */
    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal porcentajePatrono = BigDecimal.ONE;

    /**
     * Porcentaje que paga CCSS/INS como subsidio (0.00 a 1.00).
     * Ej: INCAPACIDAD_CCSS días 4+ → 0.60
     */
    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal porcentajeSubsidio = BigDecimal.ZERO;

    // --- Flags para cálculos legales ---
    /**
     * ¿Esta ausencia cuenta para la base del aguinaldo?
     * MATERNIDAD = true; INCAPACIDAD_CCSS = false; PERMISO_SIN_GOCE = false.
     */
    @Column(nullable = false)
    private Boolean computaParaAguinaldo = true;

    /**
     * ¿Esta ausencia acumula antigüedad y vacaciones?
     * PERMISO_SIN_GOCE = false; INCAPACIDAD = false.
     */
    @Column(nullable = false)
    private Boolean computaAntiguedad = true;

    // --- Flujo de aprobación ---
    @Column(nullable = false)
    private Boolean aprobada = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprobado_por_id")
    private Usuario aprobadoPor;

    @Column
    private LocalDateTime fechaAprobacion;

    @Column(length = 1000)
    private String observaciones;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
```

### Migración V6 (continuación) — Ausencias

```sql
-- Agregar al mismo V6__rrhh_contratos_ausencias.sql

CREATE TABLE ausencias (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    empleado_id             BIGINT       NOT NULL,
    tipo                    VARCHAR(30)  NOT NULL,
    fecha_inicio            DATE         NOT NULL,
    fecha_fin               DATE         NOT NULL,
    motivo                  VARCHAR(500),
    entidad_certificante    VARCHAR(50),
    numero_boleta           VARCHAR(100),
    porcentaje_patrono      DECIMAL(5,4) NOT NULL DEFAULT 1.0000,
    porcentaje_subsidio     DECIMAL(5,4) NOT NULL DEFAULT 0.0000,
    computa_para_aguinaldo  BOOLEAN      NOT NULL DEFAULT TRUE,
    computa_antiguedad      BOOLEAN      NOT NULL DEFAULT TRUE,
    aprobada                BOOLEAN      NOT NULL DEFAULT FALSE,
    aprobado_por_id         INT,           -- FK to usuario.id_usuario (INT, singular table name)
    fecha_aprobacion        DATETIME,
    observaciones           TEXT,
    created_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (empleado_id)     REFERENCES empleados(id),
    FOREIGN KEY (aprobado_por_id) REFERENCES usuario(id_usuario),
    INDEX idx_empleado (empleado_id),
    INDEX idx_tipo (tipo),
    INDEX idx_periodo (fecha_inicio, fecha_fin),
    INDEX idx_aprobada (aprobada)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 6. Permisos y sidebar

### Permisos a agregar en `Permiso.java`

```java
// Agregar al enum Permiso existente
RRHH_VER,
RRHH_GESTIONAR_EMPLEADOS,
RRHH_GESTIONAR_CONTRATOS,
RRHH_APROBAR_AUSENCIAS,
RRHH_VER_CATALOGO_SALARIAL,
RRHH_GESTIONAR_CATALOGO_SALARIAL

// Also add to Permiso.getCategoria():
// if (name().startsWith("RRHH_")) return "Recursos Humanos";
```

### `MatrizPermisos.java` — agregar a GERENTE

```java
// En el bloque de GERENTE:
RRHH_VER,
RRHH_GESTIONAR_EMPLEADOS,
RRHH_GESTIONAR_CONTRATOS,
RRHH_APROBAR_AUSENCIAS,
RRHH_VER_CATALOGO_SALARIAL
// GERENTE NO tiene RRHH_GESTIONAR_CATALOGO_SALARIAL — solo ADMIN
```

### Entrada en `sidebar.html`

```html
<li class="menu-item"
    sec:authorize="@permisoService.tienePermisoPorCodigo(#authentication.name, 'RRHH_VER')">
    <a th:href="@{/rrhh/empleados}"
       class="menu-link"
       data-module="rrhh"
       data-tooltip="Recursos Humanos">
        <div class="menu-icon"><i class="fas fa-id-card"></i></div>
        <span class="menu-text">RRHH</span>
    </a>
</li>
```

---

## 7. Servicios y vistas

### Interfaces de servicio requeridas

Cada servicio sigue el patrón del proyecto: interfaz limpia + `@Service @RequiredArgsConstructor @Slf4j` en la implementación.

| Interfaz | Métodos clave |
|---|---|
| `DepartamentoService` | `findActivos()`, `crear(dto)`, `actualizar(id, dto)`, `desactivar(id)` |
| `PuestoService` | `findByDepartamento(id)`, `crear(dto)`, `actualizar(id, dto)`, `desactivar(id)` |
| `SalarioMinimoService` | `findVigenteByCategoria(categoria)` |
| `EmpleadoService` | `findAll(Pageable)`, `findActivos()`, `findById(id)`, `crear(dto)`, `actualizar(id, dto)`, `darDeBaja(id, fecha, motivo)` |
| `ContratoEmpleadoService` | `getContratoActivo(idEmpleado)`, `crearContrato(idEmpleado, dto)`, `findProximosAVencer(dias)` |
| `AusenciaService` | `solicitar(idEmpleado, dto)`, `aprobar(id, idUsuario)`, `rechazar(id, obs)`, `findPendientesAprobacion()` |

**Regla de negocio crítica:** `ContratoEmpleadoService.crearContrato()` debe desactivar el contrato previo antes de guardar el nuevo.

**Regla de negocio crítica:** `AusenciaService.solicitar()` debe validar que el rango de fechas no se solape con otra ausencia aprobada del mismo empleado.

**Regla de negocio crítica:** `PuestoService.crear()/actualizar()` debe validar que `salarioPactado >= SalarioMinimo.vigente(puesto.categoriaSalarialMinima)`.

### Vistas (templates)

| Ruta | Qué renderiza |
|---|---|
| `modules/rrhh/empleados/lista.html` | Tabla paginada con filtro por departamento/estado. Acciones gateadas por `RRHH_GESTIONAR_EMPLEADOS`. |
| `modules/rrhh/empleados/form.html` | Crear/editar. Selector de puesto cascadeado desde departamento. |
| `modules/rrhh/empleados/detalle.html` | Ficha completa: datos personales, contrato activo, historial de contratos, ausencias recientes. |
| `modules/rrhh/departamentos/lista.html` | CRUD vía modales (mismo patrón que `roles.html`). |
| `modules/rrhh/puestos/lista.html` | CRUD tabla con filtro por departamento. |
| `modules/rrhh/ausencias/gestionar.html` | Ausencias pendientes de aprobación. Aprobar/rechazar con SweetAlert2. |

Todos los templates usan `th:replace="~{shared/layout :: head}"` + fragmentos `navbar`, `sidebar`, `main.main-content`, `page-header`, `alerts`.

---

## Criterios de aceptación

- [ ] Departamentos y puestos gestionables (CRUD completo)
- [ ] Catálogos regulatorios cargados con datos 2026 (CCSS, tramos renta, salarios mínimos)
- [ ] Empleados con todos los campos normativos CR (cedula, CCSS, OPC, cargas familiares)
- [ ] ContratoEmpleado con historial y solo un contrato activo por empleado
- [ ] Ausencias con tipos legales CR y flags `computaParaAguinaldo` / `computaAntiguedad`
- [ ] Validación de salario mínimo al crear/actualizar puesto
- [ ] Permisos RRHH_ en `Permiso.java` + `MatrizPermisos.java` actualizados
- [ ] Sidebar muestra entrada RRHH gateada por permiso
- [ ] Build verde: `mvn compile` sin errores

---

## Dependencias

**Requiere:**
- Sprint 7 completado

**Habilita:**
- Fase 2: Nómina — consume `ContratoEmpleado.salarioPactado`, `ParametroCCSS`, `TramoImpuestoSalario`, `SalarioMinimo`, flags de `Ausencia`

---

## Migraciones resumen

| Versión | Descripción |
|---|---|
| V3 | Catálogos base: departamentos, puestos (con datos iniciales) |
| V4 | Catálogos regulatorios: parametros_ccss, tramos_impuesto_salario, salarios_minimos |
| V5 | Tabla empleados |
| V6 | Tablas contratos_empleado y ausencias |

---

**Fase revisada:** 2026-07-21  
**Cambios respecto a versión anterior:** convenciones Lombok corregidas; `@Data` eliminado; servicio separado en interfaz + impl; `salarioBruto` movido a `ContratoEmpleado`; enums `TipoContrato` y `TipoAusencia` alineados a normativa CR; campos regulatorios agregados a `Empleado`; catálogos regulatorios versionables añadidos como entidades propias; `categoriaSalarialMinima` agregado a `Puesto`.
