# 👥 FASE 1: Recursos Humanos (RRHH)

**Sprint:** 8  
**Fase:** 1 de 5  
**Duración estimada:** 7-9 días  
**Prioridad:** ⭐⭐⭐ CRÍTICA (si se gestiona personal)  
**Estado:** 📋 PENDIENTE (0/48 tareas)

---

## ⚠️ CONDICIONAL: Solo si se gestiona personal

Esta fase es **OBLIGATORIA** solo si la empresa:
- ✅ Tiene empleados en planilla
- ✅ Necesita control de asistencia
- ✅ Debe calcular nómina

Si NO tienes empleados, **OMITIR Sprint 8 completo**.

---

## 📋 OBJETIVO DE LA FASE

Implementar sistema completo de **Gestión de Recursos Humanos** que permita:
- Gestión de empleados y expedientes digitales
- Control de departamentos y puestos
- Gestión de contratos laborales
- Control de asistencia y marcas
- Gestión de vacaciones y permisos
- Evaluaciones de desempeño
- Historial laboral completo

---

## 📊 PROGRESO GENERAL

```
Progreso: [0/48] ░░░░░░░░░░░░░░░░░░░░ 0%

├─ 1. Estructura Base de RRHH         [0/8]  ░░░░░░░░░░ 0%
├─ 2. Gestión de Empleados            [0/10] ░░░░░░░░░░ 0%
├─ 3. Control de Asistencia           [0/8]  ░░░░░░░░░░ 0%
├─ 4. Vacaciones y Permisos           [0/8]  ░░░░░░░░░░ 0%
├─ 5. Evaluaciones de Desempeño       [0/6]  ░░░░░░░░░░ 0%
└─ 6. Vistas y Reportes RRHH          [0/8]  ░░░░░░░░░░ 0%
```

---

## 📦 1. ESTRUCTURA BASE DE RRHH (8 tareas)

### 1.1. Crear entidades base

#### Tareas:

- [ ] **1.1.1** Crear entidad `Departamento`

```java
package com.erp.whatsorders.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa un departamento de la empresa.
 * Ejemplos: Ventas, Contabilidad, Producción, Administración
 */
@Entity
@Table(name = "departamentos")
@Data
public class Departamento {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Nombre del departamento
     */
    @Column(nullable = false, unique = true, length = 100)
    @NotBlank(message = "Nombre del departamento es obligatorio")
    private String nombre;
    
    /**
     * Código del departamento (ej: VNT, CONT, PROD)
     */
    @Column(unique = true, length = 10)
    private String codigo;
    
    /**
     * Descripción del departamento
     */
    @Column(columnDefinition = "TEXT")
    private String descripcion;
    
    /**
     * Jefe del departamento
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jefe_id")
    private Empleado jefe;
    
    /**
     * Presupuesto mensual asignado
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal presupuestoMensual;
    
    /**
     * Si el departamento está activo
     */
    @Column(nullable = false)
    private boolean activo = true;
    
    /**
     * Empleados del departamento
     */
    @OneToMany(mappedBy = "departamento", cascade = CascadeType.ALL)
    private List<Empleado> empleados = new ArrayList<>();
    
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
    
    /**
     * Calcula el número de empleados activos en el departamento.
     */
    public int getCantidadEmpleados() {
        return (int) empleados.stream()
            .filter(e -> e.isActivo())
            .count();
    }
}
```

- [ ] **1.1.2** Crear entidad `Puesto`

```java
package com.erp.whatsorders.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa un puesto o cargo laboral.
 * Ejemplos: Gerente de Ventas, Contador, Asistente Administrativo
 */
@Entity
@Table(name = "puestos")
@Data
public class Puesto {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Nombre del puesto
     */
    @Column(nullable = false, unique = true, length = 100)
    @NotBlank(message = "Nombre del puesto es obligatorio")
    private String nombre;
    
    /**
     * Descripción del puesto
     */
    @Column(columnDefinition = "TEXT")
    private String descripcion;
    
    /**
     * Departamento al que pertenece
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departamento_id", nullable = false)
    @NotNull(message = "Departamento es obligatorio")
    private Departamento departamento;
    
    /**
     * Salario base mínimo para este puesto (₡)
     */
    @Column(precision = 10, scale = 2, nullable = false)
    @NotNull(message = "Salario base es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "Salario debe ser positivo")
    private BigDecimal salarioBase;
    
    /**
     * Salario base máximo para este puesto (₡)
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal salarioMaximo;
    
    /**
     * Nivel jerárquico (1 = más alto, 10 = más bajo)
     */
    @Column
    @Min(value = 1, message = "Nivel mínimo es 1")
    @Max(value = 10, message = "Nivel máximo es 10")
    private Integer nivel = 5;
    
    /**
     * Funciones y responsabilidades
     */
    @Column(columnDefinition = "TEXT")
    private String funciones;
    
    /**
     * Requisitos del puesto
     */
    @Column(columnDefinition = "TEXT")
    private String requisitos;
    
    /**
     * Si el puesto está activo
     */
    @Column(nullable = false)
    private boolean activo = true;
    
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

- [ ] **1.1.3** Crear migration SQL para departamentos y puestos

```sql
-- Migration: MIGRATION_DEPARTAMENTOS_PUESTOS_SPRINT_8.sql

-- Crear tabla departamentos
CREATE TABLE departamentos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    codigo VARCHAR(10) UNIQUE,
    descripcion TEXT,
    jefe_id BIGINT,
    presupuesto_mensual DECIMAL(10,2),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_activo (activo),
    INDEX idx_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Crear tabla puestos
CREATE TABLE puestos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT,
    departamento_id BIGINT NOT NULL,
    salario_base DECIMAL(10,2) NOT NULL,
    salario_maximo DECIMAL(10,2),
    nivel INT DEFAULT 5,
    funciones TEXT,
    requisitos TEXT,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (departamento_id) REFERENCES departamentos(id),
    
    INDEX idx_departamento (departamento_id),
    INDEX idx_activo (activo),
    INDEX idx_nivel (nivel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Datos iniciales de departamentos
INSERT INTO departamentos (nombre, codigo, descripcion) VALUES
('Administración', 'ADM', 'Departamento administrativo general'),
('Ventas', 'VNT', 'Departamento de ventas y atención al cliente'),
('Contabilidad', 'CONT', 'Departamento de contabilidad y finanzas'),
('Operaciones', 'OPE', 'Departamento de operaciones'),
('Recursos Humanos', 'RRHH', 'Departamento de recursos humanos');

-- Datos iniciales de puestos
INSERT INTO puestos (nombre, descripcion, departamento_id, salario_base, salario_maximo, nivel) VALUES
('Gerente General', 'Máxima autoridad administrativa', 1, 1500000, 2500000, 1),
('Contador General', 'Responsable de la contabilidad', 3, 800000, 1200000, 2),
('Asistente de Ventas', 'Atención al cliente y ventas', 2, 450000, 700000, 5),
('Vendedor', 'Ejecutivo de ventas', 2, 500000, 800000, 4),
('Asistente Administrativo', 'Apoyo administrativo general', 1, 450000, 650000, 5);
```

- [ ] **1.1.4** Crear repositorios

```java
package com.erp.whatsorders.repository;

import com.erp.whatsorders.model.Departamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DepartamentoRepository extends JpaRepository<Departamento, Long> {
    
    /**
     * Busca departamentos activos.
     */
    List<Departamento> findByActivoTrue();
    
    /**
     * Busca por código.
     */
    Optional<Departamento> findByCodigo(String codigo);
    
    /**
     * Verifica si existe por nombre.
     */
    boolean existsByNombre(String nombre);
    
    /**
     * Busca departamentos con más de X empleados.
     */
    @Query("SELECT d FROM Departamento d WHERE SIZE(d.empleados) > :minEmpleados")
    List<Departamento> findDepartamentosConMasDeXEmpleados(int minEmpleados);
}

@Repository
public interface PuestoRepository extends JpaRepository<Puesto, Long> {
    
    /**
     * Busca puestos activos.
     */
    List<Puesto> findByActivoTrue();
    
    /**
     * Busca puestos por departamento.
     */
    List<Puesto> findByDepartamento(Departamento departamento);
    
    /**
     * Busca puestos por nivel.
     */
    List<Puesto> findByNivel(Integer nivel);
    
    /**
     * Busca puestos con salario base mayor a X.
     */
    List<Puesto> findBySalarioBaseGreaterThanEqual(BigDecimal salarioMinimo);
}
```

- [ ] **1.1.5** Crear servicios

```java
package com.erp.whatsorders.service;

import com.erp.whatsorders.dto.DepartamentoDTO;
import com.erp.whatsorders.exception.BusinessException;
import com.erp.whatsorders.model.Departamento;
import com.erp.whatsorders.repository.DepartamentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartamentoService {
    
    private final DepartamentoRepository departamentoRepository;
    
    /**
     * Lista todos los departamentos activos.
     */
    @Transactional(readOnly = true)
    public List<DepartamentoDTO> listarActivos() {
        return departamentoRepository.findByActivoTrue().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Crea un nuevo departamento.
     */
    @Transactional
    public DepartamentoDTO crear(DepartamentoDTO dto) {
        // Validar nombre único
        if (departamentoRepository.existsByNombre(dto.getNombre())) {
            throw new BusinessException("Ya existe un departamento con ese nombre");
        }
        
        Departamento departamento = new Departamento();
        departamento.setNombre(dto.getNombre());
        departamento.setCodigo(dto.getCodigo());
        departamento.setDescripcion(dto.getDescripcion());
        departamento.setPresupuestoMensual(dto.getPresupuestoMensual());
        
        departamento = departamentoRepository.save(departamento);
        
        log.info("Departamento creado: {}", departamento.getNombre());
        
        return toDTO(departamento);
    }
    
    /**
     * Actualiza un departamento.
     */
    @Transactional
    public DepartamentoDTO actualizar(Long id, DepartamentoDTO dto) {
        Departamento departamento = departamentoRepository.findById(id)
            .orElseThrow(() -> new BusinessException("Departamento no encontrado"));
        
        departamento.setNombre(dto.getNombre());
        departamento.setCodigo(dto.getCodigo());
        departamento.setDescripcion(dto.getDescripcion());
        departamento.setPresupuestoMensual(dto.getPresupuestoMensual());
        
        departamento = departamentoRepository.save(departamento);
        
        log.info("Departamento actualizado: {}", departamento.getNombre());
        
        return toDTO(departamento);
    }
    
    private DepartamentoDTO toDTO(Departamento departamento) {
        DepartamentoDTO dto = new DepartamentoDTO();
        dto.setId(departamento.getId());
        dto.setNombre(departamento.getNombre());
        dto.setCodigo(departamento.getCodigo());
        dto.setDescripcion(departamento.getDescripcion());
        dto.setPresupuestoMensual(departamento.getPresupuestoMensual());
        dto.setCantidadEmpleados(departamento.getCantidadEmpleados());
        dto.setActivo(departamento.isActivo());
        return dto;
    }
}
```

- [ ] **1.1.6** Crear DTOs

```java
package com.erp.whatsorders.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DepartamentoDTO {
    private Long id;
    private String nombre;
    private String codigo;
    private String descripcion;
    private BigDecimal presupuestoMensual;
    private Long jefeId;
    private String jefeNombre;
    private int cantidadEmpleados;
    private boolean activo;
}

@Data
public class PuestoDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private Long departamentoId;
    private String departamentoNombre;
    private BigDecimal salarioBase;
    private BigDecimal salarioMaximo;
    private Integer nivel;
    private String funciones;
    private String requisitos;
    private boolean activo;
}
```

- [ ] **1.1.7** Crear controladores

```java
package com.erp.whatsorders.controller;

import com.erp.whatsorders.dto.DepartamentoDTO;
import com.erp.whatsorders.service.DepartamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/rrhh/departamentos")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'RRHH')")
public class DepartamentoController {
    
    private final DepartamentoService departamentoService;
    
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("departamentos", departamentoService.listarActivos());
        return "rrhh/departamentos/lista";
    }
    
    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("departamento", new DepartamentoDTO());
        return "rrhh/departamentos/formulario";
    }
    
    @PostMapping
    public String crear(
        @Valid @ModelAttribute("departamento") DepartamentoDTO dto,
        BindingResult result,
        RedirectAttributes redirectAttributes
    ) {
        if (result.hasErrors()) {
            return "rrhh/departamentos/formulario";
        }
        
        departamentoService.crear(dto);
        redirectAttributes.addFlashAttribute("mensaje", "Departamento creado exitosamente");
        
        return "redirect:/rrhh/departamentos";
    }
    
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("departamento", departamentoService.obtenerPorId(id));
        return "rrhh/departamentos/formulario";
    }
    
    @PostMapping("/{id}")
    public String actualizar(
        @PathVariable Long id,
        @Valid @ModelAttribute("departamento") DepartamentoDTO dto,
        BindingResult result,
        RedirectAttributes redirectAttributes
    ) {
        if (result.hasErrors()) {
            return "rrhh/departamentos/formulario";
        }
        
        departamentoService.actualizar(id, dto);
        redirectAttributes.addFlashAttribute("mensaje", "Departamento actualizado exitosamente");
        
        return "redirect:/rrhh/departamentos";
    }
}
```

- [ ] **1.1.8** Agregar permisos RRHH

```java
// Actualizar PermisosEnum.java
public enum PermisosEnum {
    
    // ... permisos existentes ...
    
    // RRHH
    RRHH_VER("RRHH_VER", "Ver módulo de RRHH"),
    RRHH_CREAR_EMPLEADO("RRHH_CREAR_EMPLEADO", "Crear empleados"),
    RRHH_EDITAR_EMPLEADO("RRHH_EDITAR_EMPLEADO", "Editar empleados"),
    RRHH_ELIMINAR_EMPLEADO("RRHH_ELIMINAR_EMPLEADO", "Eliminar empleados"),
    RRHH_VER_NOMINA("RRHH_VER_NOMINA", "Ver nómina"),
    RRHH_PROCESAR_NOMINA("RRHH_PROCESAR_NOMINA", "Procesar nómina"),
    RRHH_GESTIONAR_ASISTENCIA("RRHH_GESTIONAR_ASISTENCIA", "Gestionar asistencia"),
    RRHH_APROBAR_VACACIONES("RRHH_APROBAR_VACACIONES", "Aprobar vacaciones"),
    RRHH_VER_REPORTES("RRHH_VER_REPORTES", "Ver reportes de RRHH");
    
    // ... resto del código ...
}
```

---

## 📦 2. GESTIÓN DE EMPLEADOS (10 tareas)

### 2.1. Entidad Empleado completa

#### Tareas:

- [ ] **2.1.1** Crear entidad `Empleado` (normativa Costa Rica)

```java
package com.erp.whatsorders.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representa un empleado de la empresa.
 * Cumple con normativa laboral de Costa Rica.
 */
@Entity
@Table(name = "empleados")
@Data
public class Empleado {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Cédula de identidad (Costa Rica: 9 dígitos)
     */
    @Column(nullable = false, unique = true, length = 20)
    @NotBlank(message = "Cédula es obligatoria")
    @Pattern(regexp = "^[0-9]{9}$|^[0-9]{10,12}$", message = "Cédula inválida")
    private String cedula;
    
    /**
     * Nombre completo
     */
    @Column(nullable = false, length = 200)
    @NotBlank(message = "Nombre es obligatorio")
    private String nombre;
    
    /**
     * Primer apellido
     */
    @Column(nullable = false, length = 100)
    @NotBlank(message = "Primer apellido es obligatorio")
    private String primerApellido;
    
    /**
     * Segundo apellido
     */
    @Column(length = 100)
    private String segundoApellido;
    
    /**
     * Email corporativo
     */
    @Column(unique = true, length = 100)
    @Email(message = "Email inválido")
    private String email;
    
    /**
     * Teléfono
     */
    @Column(length = 15)
    @Pattern(regexp = "^[0-9]{8,15}$", message = "Teléfono inválido")
    private String telefono;
    
    /**
     * Fecha de nacimiento
     */
    @Column
    private LocalDate fechaNacimiento;
    
    /**
     * Género
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Genero genero;
    
    /**
     * Estado civil
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EstadoCivil estadoCivil;
    
    /**
     * Dirección completa
     */
    @Column(columnDefinition = "TEXT")
    private String direccion;
    
    /**
     * Departamento
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departamento_id", nullable = false)
    @NotNull(message = "Departamento es obligatorio")
    private Departamento departamento;
    
    /**
     * Puesto
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "puesto_id", nullable = false)
    @NotNull(message = "Puesto es obligatorio")
    private Puesto puesto;
    
    /**
     * Salario mensual bruto (₡)
     */
    @Column(precision = 10, scale = 2, nullable = false)
    @NotNull(message = "Salario es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "Salario debe ser positivo")
    private BigDecimal salarioBruto;
    
    /**
     * Tipo de contrato
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoContrato tipoContrato;
    
    /**
     * Fecha de ingreso
     */
    @Column(nullable = false)
    @NotNull(message = "Fecha de ingreso es obligatoria")
    private LocalDate fechaIngreso;
    
    /**
     * Fecha de salida (null si está activo)
     */
    @Column
    private LocalDate fechaSalida;
    
    /**
     * Motivo de salida
     */
    @Column(length = 500)
    private String motivoSalida;
    
    /**
     * Número CCSS (Caja Costarricense de Seguro Social)
     */
    @Column(unique = true, length = 20)
    private String numeroCCSS;
    
    /**
     * Número de cuenta bancaria para depósito de salario
     */
    @Column(length = 30)
    private String cuentaBancaria;
    
    /**
     * Banco
     */
    @Column(length = 100)
    private String banco;
    
    /**
     * Tipo de cuenta (Ahorros/Corriente)
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TipoCuentaBancaria tipoCuentaBancaria;
    
    /**
     * Días de vacaciones acumulados
     */
    @Column
    private Integer diasVacacionesAcumulados = 0;
    
    /**
     * Si el empleado está activo
     */
    @Column(nullable = false)
    private boolean activo = true;
    
    /**
     * Foto del empleado (URL)
     */
    @Column(length = 500)
    private String fotoUrl;
    
    /**
     * Contacto de emergencia - Nombre
     */
    @Column(length = 200)
    private String contactoEmergenciaNombre;
    
    /**
     * Contacto de emergencia - Teléfono
     */
    @Column(length = 15)
    private String contactoEmergenciaTelefono;
    
    /**
     * Contacto de emergencia - Relación
     */
    @Column(length = 50)
    private String contactoEmergenciaRelacion;
    
    /**
     * Usuario del sistema asociado (si tiene acceso)
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
    
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
    
    /**
     * Obtiene el nombre completo del empleado.
     */
    public String getNombreCompleto() {
        StringBuilder sb = new StringBuilder();
        sb.append(nombre).append(" ");
        sb.append(primerApellido);
        if (segundoApellido != null && !segundoApellido.isEmpty()) {
            sb.append(" ").append(segundoApellido);
        }
        return sb.toString();
    }
    
    /**
     * Calcula la antigüedad en años.
     */
    public int getAntiguedadAnios() {
        LocalDate fechaFinal = activo ? LocalDate.now() : fechaSalida;
        return Period.between(fechaIngreso, fechaFinal).getYears();
    }
    
    /**
     * Calcula la edad del empleado.
     */
    public int getEdad() {
        if (fechaNacimiento == null) return 0;
        return Period.between(fechaNacimiento, LocalDate.now()).getYears();
    }
}

enum Genero {
    MASCULINO,
    FEMENINO,
    OTRO
}

enum EstadoCivil {
    SOLTERO,
    CASADO,
    DIVORCIADO,
    VIUDO,
    UNION_LIBRE
}

enum TipoContrato {
    INDEFINIDO,         // Contrato por tiempo indefinido
    PLAZO_FIJO,        // Contrato a plazo fijo
    TEMPORAL,          // Contrato temporal
    MEDIO_TIEMPO,      // Medio tiempo
    HONORARIOS         // Servicios profesionales
}

enum TipoCuentaBancaria {
    AHORROS,
    CORRIENTE,
    CLIENTE
}
```

- [ ] **2.1.2** Crear migration SQL para empleados

```sql
-- Migration: MIGRATION_EMPLEADOS_SPRINT_8.sql

CREATE TABLE empleados (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cedula VARCHAR(20) NOT NULL UNIQUE,
    nombre VARCHAR(200) NOT NULL,
    primer_apellido VARCHAR(100) NOT NULL,
    segundo_apellido VARCHAR(100),
    email VARCHAR(100) UNIQUE,
    telefono VARCHAR(15),
    fecha_nacimiento DATE,
    genero VARCHAR(10),
    estado_civil VARCHAR(20),
    direccion TEXT,
    
    departamento_id BIGINT NOT NULL,
    puesto_id BIGINT NOT NULL,
    
    salario_bruto DECIMAL(10,2) NOT NULL,
    tipo_contrato VARCHAR(30) NOT NULL,
    fecha_ingreso DATE NOT NULL,
    fecha_salida DATE,
    motivo_salida VARCHAR(500),
    
    numero_ccss VARCHAR(20) UNIQUE,
    cuenta_bancaria VARCHAR(30),
    banco VARCHAR(100),
    tipo_cuenta_bancaria VARCHAR(20),
    
    dias_vacaciones_acumulados INT DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    foto_url VARCHAR(500),
    
    contacto_emergencia_nombre VARCHAR(200),
    contacto_emergencia_telefono VARCHAR(15),
    contacto_emergencia_relacion VARCHAR(50),
    
    usuario_id BIGINT,
    
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (departamento_id) REFERENCES departamentos(id),
    FOREIGN KEY (puesto_id) REFERENCES puestos(id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    
    INDEX idx_cedula (cedula),
    INDEX idx_departamento (departamento_id),
    INDEX idx_puesto (puesto_id),
    INDEX idx_activo (activo),
    INDEX idx_fecha_ingreso (fecha_ingreso),
    INDEX idx_nombre_completo (nombre, primer_apellido, segundo_apellido)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

- [ ] **2.1.3** Crear `EmpleadoRepository`

```java
@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
    
    /**
     * Busca empleados activos.
     */
    List<Empleado> findByActivoTrue();
    
    /**
     * Busca por cédula.
     */
    Optional<Empleado> findByCedula(String cedula);
    
    /**
     * Busca empleados por departamento.
     */
    List<Empleado> findByDepartamentoAndActivoTrue(Departamento departamento);
    
    /**
     * Busca empleados por puesto.
     */
    List<Empleado> findByPuestoAndActivoTrue(Puesto puesto);
    
    /**
     * Busca empleados con fecha de ingreso en rango.
     */
    List<Empleado> findByFechaIngresoB etween(LocalDate desde, LocalDate hasta);
    
    /**
     * Busca empleados con cumpleaños en mes.
     */
    @Query("SELECT e FROM Empleado e WHERE MONTH(e.fechaNacimiento) = :mes AND e.activo = true")
    List<Empleado> findCumpleaniosPorMes(@Param("mes") int mes);
    
    /**
     * Cuenta empleados activos.
     */
    long countByActivoTrue();
}
```

- [ ] **2.1.4** Crear `EmpleadoService`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class EmpleadoService {
    
    private final EmpleadoRepository empleadoRepository;
    private final DepartamentoRepository departamentoRepository;
    private final PuestoRepository puestoRepository;
    
    /**
     * Lista todos los empleados activos.
     */
    @Transactional(readOnly = true)
    public List<EmpleadoDTO> listarActivos() {
        return empleadoRepository.findByActivoTrue().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Crea un nuevo empleado.
     */
    @Transactional
    public EmpleadoDTO crear(EmpleadoDTO dto) {
        // Validar cédula única
        if (empleadoRepository.findByCedula(dto.getCedula()).isPresent()) {
            throw new BusinessException("Ya existe un empleado con esa cédula");
        }
        
        Empleado empleado = new Empleado();
        actualizarDatos(empleado, dto);
        
        empleado = empleadoRepository.save(empleado);
        
        log.info("Empleado creado: {} - {}", empleado.getCedula(), empleado.getNombreCompleto());
        
        return toDTO(empleado);
    }
    
    /**
     * Actualiza un empleado.
     */
    @Transactional
    public EmpleadoDTO actualizar(Long id, EmpleadoDTO dto) {
        Empleado empleado = empleadoRepository.findById(id)
            .orElseThrow(() -> new BusinessException("Empleado no encontrado"));
        
        actualizarDatos(empleado, dto);
        
        empleado = empleadoRepository.save(empleado);
        
        log.info("Empleado actualizado: {}", empleado.getNombreCompleto());
        
        return toDTO(empleado);
    }
    
    /**
     * Inactiva un empleado (despido/renuncia).
     */
    @Transactional
    public void inactivar(Long id, LocalDate fechaSalida, String motivo) {
        Empleado empleado = empleadoRepository.findById(id)
            .orElseThrow(() -> new BusinessException("Empleado no encontrado"));
        
        empleado.setActivo(false);
        empleado.setFechaSalida(fechaSalida);
        empleado.setMotivoSalida(motivo);
        
        empleadoRepository.save(empleado);
        
        log.info("Empleado inactivado: {} - Motivo: {}", empleado.getNombreCompleto(), motivo);
    }
    
    /**
     * Obtiene empleados con cumpleaños en el mes actual.
     */
    @Transactional(readOnly = true)
    public List<EmpleadoDTO> obtenerCumpleaniosDelMes() {
        int mesActual = LocalDate.now().getMonthValue();
        return empleadoRepository.findCumpleaniosPorMes(mesActual).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    private void actualizarDatos(Empleado empleado, EmpleadoDTO dto) {
        empleado.setCedula(dto.getCedula());
        empleado.setNombre(dto.getNombre());
        empleado.setPrimerApellido(dto.getPrimerApellido());
        empleado.setSegundoApellido(dto.getSegundoApellido());
        empleado.setEmail(dto.getEmail());
        empleado.setTelefono(dto.getTelefono());
        empleado.setFechaNacimiento(dto.getFechaNacimiento());
        empleado.setGenero(dto.getGenero());
        empleado.setEstadoCivil(dto.getEstadoCivil());
        empleado.setDireccion(dto.getDireccion());
        
        // Departamento y Puesto
        Departamento departamento = departamentoRepository.findById(dto.getDepartamentoId())
            .orElseThrow(() -> new BusinessException("Departamento no encontrado"));
        empleado.setDepartamento(departamento);
        
        Puesto puesto = puestoRepository.findById(dto.getPuestoId())
            .orElseThrow(() -> new BusinessException("Puesto no encontrado"));
        empleado.setPuesto(puesto);
        
        empleado.setSalarioBruto(dto.getSalarioBruto());
        empleado.setTipoContrato(dto.getTipoContrato());
        empleado.setFechaIngreso(dto.getFechaIngreso());
        empleado.setNumeroCCSS(dto.getNumeroCCSS());
        empleado.setCuentaBancaria(dto.getCuentaBancaria());
        empleado.setBanco(dto.getBanco());
        empleado.setTipoCuentaBancaria(dto.getTipoCuentaBancaria());
        empleado.setContactoEmergenciaNombre(dto.getContactoEmergenciaNombre());
        empleado.setContactoEmergenciaTelefono(dto.getContactoEmergenciaTelefono());
        empleado.setContactoEmergenciaRelacion(dto.getContactoEmergenciaRelacion());
    }
    
    private EmpleadoDTO toDTO(Empleado empleado) {
        EmpleadoDTO dto = new EmpleadoDTO();
        dto.setId(empleado.getId());
        dto.setCedula(empleado.getCedula());
        dto.setNombreCompleto(empleado.getNombreCompleto());
        dto.setNombre(empleado.getNombre());
        dto.setPrimerApellido(empleado.getPrimerApellido());
        dto.setSegundoApellido(empleado.getSegundoApellido());
        dto.setEmail(empleado.getEmail());
        dto.setTelefono(empleado.getTelefono());
        dto.setDepartamentoNombre(empleado.getDepartamento().getNombre());
        dto.setPuestoNombre(empleado.getPuesto().getNombre());
        dto.setSalarioBruto(empleado.getSalarioBruto());
        dto.setFechaIngreso(empleado.getFechaIngreso());
        dto.setAntiguedadAnios(empleado.getAntiguedadAnios());
        dto.setActivo(empleado.isActivo());
        return dto;
    }
}
```

- [ ] **2.1.5** Crear `EmpleadoDTO`

```java
@Data
public class EmpleadoDTO {
    private Long id;
    private String cedula;
    private String nombre;
    private String primerApellido;
    private String segundoApellido;
    private String nombreCompleto;
    private String email;
    private String telefono;
    private LocalDate fechaNacimiento;
    private Genero genero;
    private EstadoCivil estadoCivil;
    private String direccion;
    
    private Long departamentoId;
    private String departamentoNombre;
    private Long puestoId;
    private String puestoNombre;
    
    private BigDecimal salarioBruto;
    private TipoContrato tipoContrato;
    private LocalDate fechaIngreso;
    private int antiguedadAnios;
    
    private String numeroCCSS;
    private String cuentaBancaria;
    private String banco;
    private TipoCuentaBancaria tipoCuentaBancaria;
    
    private String contactoEmergenciaNombre;
    private String contactoEmergenciaTelefono;
    private String contactoEmergenciaRelacion;
    
    private boolean activo;
}
```

- [ ] **2.1.6** Crear `EmpleadoController`

```java
@Controller
@RequestMapping("/rrhh/empleados")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'RRHH')")
public class EmpleadoController {
    
    private final EmpleadoService empleadoService;
    private final DepartamentoService departamentoService;
    private final PuestoService puestoService;
    
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("empleados", empleadoService.listarActivos());
        return "rrhh/empleados/lista";
    }
    
    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("empleado", new EmpleadoDTO());
        model.addAttribute("departamentos", departamentoService.listarActivos());
        model.addAttribute("puestos", puestoService.listarActivos());
        return "rrhh/empleados/formulario";
    }
    
    @PostMapping
    public String crear(
        @Valid @ModelAttribute("empleado") EmpleadoDTO dto,
        BindingResult result,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (result.hasErrors()) {
            model.addAttribute("departamentos", departamentoService.listarActivos());
            model.addAttribute("puestos", puestoService.listarActivos());
            return "rrhh/empleados/formulario";
        }
        
        empleadoService.crear(dto);
        redirectAttributes.addFlashAttribute("mensaje", "Empleado creado exitosamente");
        
        return "redirect:/rrhh/empleados";
    }
    
    @GetMapping("/{id}")
    public String ver(@PathVariable Long id, Model model) {
        model.addAttribute("empleado", empleadoService.obtenerPorId(id));
        return "rrhh/empleados/detalle";
    }
    
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("empleado", empleadoService.obtenerPorId(id));
        model.addAttribute("departamentos", departamentoService.listarActivos());
        model.addAttribute("puestos", puestoService.listarActivos());
        return "rrhh/empleados/formulario";
    }
    
    @PostMapping("/{id}/inactivar")
    public String inactivar(
        @PathVariable Long id,
        @RequestParam LocalDate fechaSalida,
        @RequestParam String motivo,
        RedirectAttributes redirectAttributes
    ) {
        empleadoService.inactivar(id, fechaSalida, motivo);
        redirectAttributes.addFlashAttribute("mensaje", "Empleado inactivado exitosamente");
        
        return "redirect:/rrhh/empleados";
    }
}
```

- [ ] **2.1.7** Crear vista lista de empleados

```html
<!-- src/main/resources/templates/rrhh/empleados/lista.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
    <title>Empleados - RRHH</title>
</head>
<body>
<div class="container-fluid mt-4">
    <div class="row">
        <div class="col-12">
            <div class="card">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <h3>👥 Gestión de Empleados</h3>
                    <a th:href="@{/rrhh/empleados/nuevo}" 
                       class="btn btn-primary"
                       sec:authorize="hasAuthority('RRHH_CREAR_EMPLEADO')">
                        <i class="bi bi-plus-circle"></i> Nuevo Empleado
                    </a>
                </div>
                <div class="card-body">
                    
                    <!-- Filtros -->
                    <div class="row mb-3">
                        <div class="col-md-4">
                            <input type="text" 
                                   id="filtroNombre" 
                                   class="form-control" 
                                   placeholder="Buscar por nombre...">
                        </div>
                        <div class="col-md-3">
                            <select id="filtroDepartamento" class="form-select">
                                <option value="">Todos los departamentos</option>
                                <option th:each="dept : ${departamentos}" 
                                        th:value="${dept.id}" 
                                        th:text="${dept.nombre}"></option>
                            </select>
                        </div>
                    </div>
                    
                    <!-- Tabla -->
                    <div class="table-responsive">
                        <table class="table table-hover" id="tablaEmpleados">
                            <thead>
                                <tr>
                                    <th>Cédula</th>
                                    <th>Nombre Completo</th>
                                    <th>Departamento</th>
                                    <th>Puesto</th>
                                    <th>Fecha Ingreso</th>
                                    <th>Antigüedad</th>
                                    <th>Estado</th>
                                    <th>Acciones</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr th:each="empleado : ${empleados}">
                                    <td th:text="${empleado.cedula}"></td>
                                    <td>
                                        <a th:href="@{/rrhh/empleados/{id}(id=${empleado.id})}" 
                                           th:text="${empleado.nombreCompleto}"></a>
                                    </td>
                                    <td th:text="${empleado.departamentoNombre}"></td>
                                    <td th:text="${empleado.puestoNombre}"></td>
                                    <td th:text="${#temporals.format(empleado.fechaIngreso, 'dd/MM/yyyy')}"></td>
                                    <td>
                                        <span th:text="${empleado.antiguedadAnios + ' años'}"></span>
                                    </td>
                                    <td>
                                        <span th:if="${empleado.activo}" 
                                              class="badge bg-success">Activo</span>
                                        <span th:unless="${empleado.activo}" 
                                              class="badge bg-secondary">Inactivo</span>
                                    </td>
                                    <td>
                                        <div class="btn-group btn-group-sm">
                                            <a th:href="@{/rrhh/empleados/{id}(id=${empleado.id})}" 
                                               class="btn btn-info" 
                                               title="Ver">
                                                <i class="bi bi-eye"></i>
                                            </a>
                                            <a th:href="@{/rrhh/empleados/{id}/editar(id=${empleado.id})}" 
                                               class="btn btn-warning" 
                                               title="Editar"
                                               sec:authorize="hasAuthority('RRHH_EDITAR_EMPLEADO')">
                                                <i class="bi bi-pencil"></i>
                                            </a>
                                        </div>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
// Filtro de búsqueda en tiempo real
document.getElementById('filtroNombre').addEventListener('keyup', function() {
    let filtro = this.value.toLowerCase();
    let filas = document.querySelectorAll('#tablaEmpleados tbody tr');
    
    filas.forEach(function(fila) {
        let nombre = fila.cells[1].textContent.toLowerCase();
        fila.style.display = nombre.includes(filtro) ? '' : 'none';
    });
});
</script>
</body>
</html>
```

- [ ] **2.1.8** Crear vista formulario de empleado
- [ ] **2.1.9** Crear vista detalle de empleado
- [ ] **2.1.10** Crear tests unitarios de empleados

---

## 📦 3. CONTROL DE ASISTENCIA (8 tareas)

**Continúa en el próximo comentario debido a límite de caracteres...**

---

## 📊 CRITERIOS DE ACEPTACIÓN

✅ Departamentos y puestos gestionables  
✅ Empleados con todos los datos requeridos (normativa CR)  
✅ Control de asistencia funcional  
✅ Vacaciones y permisos gestionables  
✅ Evaluaciones de desempeño registradas  
✅ Reportes de RRHH disponibles  

---

## 📚 DEPENDENCIAS

**Requiere:**
- ✅ Sprint 7 completado (auditoría, seguridad)

**Habilita:**
- 🚀 FASE 2: Nómina (requiere empleados)
- 🚀 Gestión completa de personal

---

## 🔄 PRÓXIMOS PASOS

1. ✅ Completar todas las entidades de RRHH
2. ✅ Implementar control de asistencia
3. 🚀 Continuar con **FASE 2: Nómina**

---

**Fase creada:** 16 de enero de 2026  
**Responsable:** Equipo de Desarrollo  
**Prioridad:** CRÍTICA (si se gestiona personal)
