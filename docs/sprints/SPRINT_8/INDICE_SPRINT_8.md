# 📑 ÍNDICE - SPRINT 8: RRHH + Nómina + Reportes Financieros

**Proyecto:** WhatsApp Orders Manager - ERP Spring Boot  
**Sprint:** 8  
**Fecha Inicio:** 14 de abril de 2026  
**Fecha Finalización:** 15 de mayo de 2026 (estimado)  
**Estado:** 📋 PLANIFICADO  
**Tipo:** ⚠️ **CONDICIONAL** - Solo si se gestiona personal

---

## 📚 ESTRUCTURA DE DOCUMENTACIÓN

### 📄 Documentos Principales

#### 1. **CHECKLIST_SPRINT_8.md**
**Descripción:** Checklist maestro con todas las tareas del Sprint 8  
**Estado:** 📋 0/148 tareas (0%)  
**Contenido:**
- Progreso general (3 fases + testing + documentación)
- Checklist detallado por fase
- Estado de cada tarea
- Milestones críticos
- Métricas de rendimiento

**Ruta:** `docs/sprints/SPRINT_8/CHECKLIST_SPRINT_8.md`

---

#### 2. **RESUMEN_SPRINT_8.md**
**Descripción:** Resumen ejecutivo del Sprint 8  
**Contenido:**
- Objetivos del sprint (solo si hay personal)
- Métricas en números
- Resumen de cada fase
- Archivos a crear/modificar
- Próximos pasos

**Ruta:** `docs/sprints/SPRINT_8/RESUMEN_SPRINT_8.md`

---

#### 3. **SPRINT_8_PLAN_MAESTRO.md**
**Descripción:** Plan detallado de ejecución del Sprint 8  
**Contenido:**
- Análisis de situación actual
- Objetivos y alcance (RRHH + Nómina)
- Priorización de fases
- Análisis de riesgos
- Estrategia de implementación
- Normativa laboral de Costa Rica

**Ruta:** `docs/sprints/SPRINT_8/SPRINT_8_PLAN_MAESTRO.md`

---

### 📦 Documentación por Fases

#### **FASE 1: RRHH (Recursos Humanos)**
**Estado:** 📋 PENDIENTE (0/48 tareas)  
**Duración:** 7-9 días  
**Prioridad:** ⭐⭐⭐ CRÍTICA (si aplica)

**Documentación:**
- `fases/FASE_1_RRHH.md` - Sistema completo de gestión de personal

**Entregables:**
- Modelo `Empleado.java` completo
- Modelo `Departamento.java`
- Modelo `Puesto.java`
- Gestión de contratos laborales
- Control de asistencia
- Vacaciones y permisos
- Evaluaciones de desempeño
- Historial laboral
- Expediente digital

**Normativa Costa Rica:**
- Código de Trabajo de Costa Rica
- CCSS (Caja Costarricense de Seguro Social)
- INS (Instituto Nacional de Seguros)
- Aguinaldo, cesantía, preaviso

**Ruta:** `docs/sprints/SPRINT_8/fases/FASE_1_RRHH.md`

---

#### **FASE 2: Nómina (Planilla)**
**Estado:** 📋 PENDIENTE (0/56 tareas)  
**Duración:** 9-12 días  
**Prioridad:** ⭐⭐⭐ CRÍTICA (si aplica)

**Documentación:**
- `fases/FASE_2_NOMINA.md` - Sistema completo de nómina

**Entregables:**
- Modelo `Nomina.java`
- Modelo `DetalleNomina.java`
- Cálculo de salarios (ordinario, extra, nocturno)
- Deducciones CCSS (9.34% obrero, 26.67% patronal)
- Deducciones INS (1% aprox.)
- Impuesto sobre la renta (progresivo)
- Aguinaldo (1/12 del salario anual)
- Cesantía (8.33% mensual)
- Preaviso
- Préstamos y embargos
- Reporte para CCSS
- Comprobantes de pago
- Integración con contabilidad

**Cálculos Costa Rica:**
- Salario bruto
- Deducciones CCSS obrero: 9.34%
- Deducciones INS: ~1%
- Impuesto renta (según tabla progresiva 2026)
- Salario neto
- Cargas patronales CCSS: 26.67%
- Aguinaldo acumulado: salario/12
- Cesantía acumulada: salario × 8.33%

**Ruta:** `docs/sprints/SPRINT_8/fases/FASE_2_NOMINA.md`

---

#### **FASE 3: Reportes Financieros Avanzados**
**Estado:** 📋 PENDIENTE (0/32 tareas)  
**Duración:** 5-7 días  
**Prioridad:** ⭐⭐ ALTA

**Documentación:**
- `fases/FASE_3_REPORTES_FINANCIEROS.md` - Estados financieros completos

**Entregables:**
- Estado de Resultados (P&L)
- Balance General (Balance Sheet)
- Flujo de Caja (Cash Flow)
- Análisis de Ratios Financieros
- Dashboard ejecutivo
- Gráficos interactivos (Chart.js)
- Exportación a Excel/PDF
- Comparativos mensuales/anuales
- Proyecciones financieras

**Reportes:**
1. **Estado de Resultados:**
   - Ingresos (ventas)
   - Costo de ventas
   - Gastos operativos
   - Gastos de nómina
   - Utilidad/Pérdida neta

2. **Balance General:**
   - Activos (circulantes, fijos)
   - Pasivos (corto/largo plazo)
   - Capital contable

3. **Flujo de Caja:**
   - Entradas (cobros)
   - Salidas (pagos, nómina)
   - Saldo final

4. **Ratios:**
   - Liquidez, endeudamiento, rentabilidad

**Ruta:** `docs/sprints/SPRINT_8/fases/FASE_3_REPORTES_FINANCIEROS.md`

---

#### **FASE 4: Testing Automatizado**
**Estado:** 📋 PENDIENTE (0/8 tareas)  
**Duración:** 2-3 días  
**Prioridad:** ⭐⭐ ALTA

**Documentación:**
- `fases/FASE_4_TESTING.md` - Suite de tests

**Entregables:**
- Tests unitarios (20+ tests)
- Tests de cálculo de nómina (críticos)
- Tests de reportes financieros
- Cobertura mínima del 75%
- Tests de integración CCSS
- Validación de deducciones

**Ruta:** `docs/sprints/SPRINT_8/fases/FASE_4_TESTING.md`

---

#### **FASE 5: Documentación Técnica**
**Estado:** 📋 PENDIENTE (0/4 tareas)  
**Duración:** 1-2 días  
**Prioridad:** ⭐ MEDIA

**Documentación:**
- `fases/FASE_5_DOCUMENTACION.md` - Manuales completos

**Entregables:**
- Manual de RRHH (700+ líneas)
- Manual de Nómina (900+ líneas)
- Manual de Reportes Financieros (500+ líneas)
- Guía de Normativa Laboral CR (400+ líneas)

**Ruta:** `docs/sprints/SPRINT_8/fases/FASE_5_DOCUMENTACION.md`

---

### 🧪 Testing

**Estado:** 📋 PENDIENTE (0/8 tareas)

**Cobertura Objetivo:**
- ✅ Tests unitarios: >20 tests (Nómina, RRHH, Reportes)
- ✅ Tests de integración: 4 tests E2E
- ✅ Cobertura de código: 75%+
- ✅ **Tests críticos de cálculo de nómina** (CCSS, INS, Renta)
- ✅ Tests de aguinaldo y cesantía
- ✅ Tests de reportes financieros

**Documentación:** Ver `fases/FASE_4_TESTING.md`

---

### 📚 Documentación de Usuario

**Estado:** 📋 PENDIENTE (0/4 manuales)

**Manuales:**
1. 📋 `MANUAL_RRHH.md` (700+ líneas)
2. 📋 `MANUAL_NOMINA.md` (900+ líneas)
3. 📋 `MANUAL_REPORTES_FINANCIEROS.md` (500+ líneas)
4. 📋 `GUIA_NORMATIVA_LABORAL_CR.md` (400+ líneas)

**Total estimado:** ~2,500 líneas de documentación de usuario

---

## 📁 ESTRUCTURA DE ARCHIVOS

```
SPRINT_8/
├── CHECKLIST_SPRINT_8.md         (Checklist maestro)
├── RESUMEN_SPRINT_8.md            (Resumen ejecutivo)
├── SPRINT_8_PLAN_MAESTRO.md       (Plan detallado)
├── INDICE_SPRINT_8.md             (Este archivo)
├── README.md                       (Introducción al sprint)
│
├── fases/
│   ├── FASE_1_RRHH.md
│   ├── FASE_2_NOMINA.md
│   ├── FASE_3_REPORTES_FINANCIEROS.md
│   ├── FASE_4_TESTING.md
│   └── FASE_5_DOCUMENTACION.md
│
└── manuales/
    ├── MANUAL_RRHH.md
    ├── MANUAL_NOMINA.md
    ├── MANUAL_REPORTES_FINANCIEROS.md
    └── GUIA_NORMATIVA_LABORAL_CR.md
```

---

## 🎯 OBJETIVOS DEL SPRINT

### Objetivo Principal
Implementar un sistema completo de gestión de recursos humanos y nómina cumpliendo con la normativa laboral de Costa Rica, y desarrollar reportes financieros avanzados para análisis ejecutivo.

### Objetivos Específicos

1. **👥 RRHH (Recursos Humanos):**
   - Catálogo de empleados completo
   - Gestión de departamentos y puestos
   - Control de asistencia
   - Vacaciones y permisos
   - Evaluaciones de desempeño
   - Expediente digital

2. **💰 Nómina (Planilla):**
   - Cálculo automático de salarios
   - Deducciones CCSS (9.34% obrero, 26.67% patronal)
   - Deducciones INS (~1%)
   - Impuesto sobre la renta (tabla progresiva)
   - Aguinaldo (1/12 anual)
   - Cesantía (8.33% mensual)
   - Reportes para CCSS
   - Comprobantes de pago
   - Integración con contabilidad

3. **📊 Reportes Financieros Avanzados:**
   - Estado de Resultados (P&L)
   - Balance General
   - Flujo de Caja
   - Análisis de ratios financieros
   - Dashboard ejecutivo
   - Gráficos interactivos

4. **🧪 Testing:**
   - Cobertura del 75%+
   - Tests de cálculos de nómina
   - Validación de normativa CR

5. **📚 Documentación:**
   - Manuales técnicos completos
   - Guía de normativa laboral

---

## 📊 MÉTRICAS Y OBJETIVOS

### Métricas de Progreso

```
┌─────────────────────────────────────────────────────────────┐
│                 SPRINT 8 - MÉTRICAS OBJETIVO                 │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Total de Tareas:                     148 tareas            │
│  Duración Estimada:                   24-31 días            │
│  Velocidad Requerida:                 5-6 tareas/día        │
│                                                              │
│  Nuevas Tablas BD:                    10 tablas             │
│  Entidades Java:                      12 entidades          │
│  Services:                            8 servicios           │
│  Controllers:                         6 controllers         │
│  Templates HTML:                      15 vistas             │
│                                                              │
│  Tests Unitarios:                     20+ tests             │
│  Tests Integración:                   4 tests               │
│  Cobertura Objetivo:                  75%+                  │
│                                                              │
│  Líneas de Código (estimadas):       ~14,000 líneas        │
│  Líneas de Documentación:             ~3,000 líneas        │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Indicadores de Éxito

✅ **RRHH operativo** - Gestión completa de empleados  
✅ **Nómina funcional** - Cálculos automáticos según normativa CR  
✅ **Reportes financieros** - Estados financieros completos  
✅ **Testing > 75%** - Cobertura de código objetivo alcanzada  
✅ **Cumplimiento normativo CR** - CCSS, INS, Código de Trabajo  
✅ **Integración contable** - Asientos desde nómina  

---

## 🇨🇷 NORMATIVA LABORAL DE COSTA RICA

### Deducciones Obligatorias (2026)

#### 1. **CCSS (Caja Costarricense de Seguro Social)**
**Porcentaje obrero:** 9.34% del salario bruto
- SEM (Seguro de Enfermedad y Maternidad): 5.50%
- IVM (Invalidez, Vejez y Muerte): 2.84%
- Banco Popular: 1.00%

**Porcentaje patronal:** 26.67% del salario bruto
- SEM: 9.25%
- IVM: 5.08%
- Asignaciones Familiares: 5.00%
- Fondo de Desarrollo Social: 5.00%
- Banco Popular: 0.25%
- IMAS: 0.50%
- INA: 1.50%
- Pensiones Complementarias: 0.09%

**Total carga social:** ~36% del salario

---

#### 2. **INS (Instituto Nacional de Seguros)**
**Porcentaje:** ~1% del salario bruto
- Póliza de Riesgos de Trabajo (variable según actividad)

---

#### 3. **Impuesto sobre la Renta (Progresivo)**
**Tabla 2026 (estimada):**
- Hasta ₡941,000/mes: Exento
- De ₡941,001 a ₡1,381,000: 10%
- De ₡1,381,001 a ₡2,423,000: 15%
- De ₡2,423,001 a ₡4,846,000: 20%
- Más de ₡4,846,000: 25%

---

#### 4. **Prestaciones Laborales**

**Aguinaldo:**
- 1/12 del salario anual (8.33% mensual)
- Pago en diciembre (del 1 al 20)

**Cesantía:**
- 8.33% del salario mensual
- Acumulado mensual
- Pago al finalizar relación laboral

**Preaviso:**
- Variable según antigüedad (1-3 meses de salario)

**Vacaciones:**
- 2 semanas al año (14 días) después de 50 semanas trabajadas

---

### Jornadas Laborales

**Jornada ordinaria:**
- Diurna: 8 horas diarias, 48 semanales
- Nocturna: 6 horas diarias, 36 semanales
- Mixta: 7 horas diarias, 42 semanales

**Horas extra:**
- 50% adicional sobre salario ordinario
- Máximo 12 horas extra semanales

**Trabajo nocturno:**
- Entre 7:00 PM y 5:00 AM
- Recargo del 50%

---

## 🔗 DEPENDENCIAS

### Dependencias Técnicas

**Nuevas dependencias Maven:**
- Apache POI (generación de Excel)
- OpenPDF o iText (reportes PDF)
- Chart.js (frontend - ya incluido)

### Dependencias Externas

**Servicios opcionales:**
- ⚠️ API de CCSS (consulta de estado de pagos - si existe)
- ⚠️ Sistema bancario (ACH para depósitos)

### Dependencias de Sprints Anteriores

**Requiere completados:**
- ✅ Sprint 1-7: Sistema completo
- ✅ Sprint 5: Contabilidad (para integración)
- ✅ Sprint 5: Pagos (para nómina)
- ✅ ConfiguracionEmpresa

---

## ⚠️ RIESGOS Y MITIGACIONES

### Riesgos Identificados

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|---------|------------|
| Cálculos de nómina incorrectos | Alta | Crítico | Tests exhaustivos, validación con contador |
| Normativa CR desactualizada | Media | Alto | Consultar fuentes oficiales (CCSS, MTSS) |
| Complejidad de impuesto renta | Alta | Medio | Tabla configurable, validación manual inicial |
| Sprint innecesario (sin personal) | Alta | Bajo | Marcar como CONDICIONAL, evaluar antes |
| Integración contable compleja | Media | Medio | Documentar asientos, tests de integración |

---

## 📅 CRONOGRAMA ESTIMADO

```
Semana 1 (14-20 Abr):  FASE 1 - RRHH (Completa)
Semana 2 (21-27 Abr):  FASE 2 - Nómina (Parte 1: Modelos y cálculos básicos)
Semana 3 (28 Abr-4 May): FASE 2 - Nómina (Parte 2: Deducciones y prestaciones)
Semana 4 (5-11 May):   FASE 2 - Nómina (Parte 3: Reportes) + FASE 3 inicio
Semana 5 (12-15 May):  FASE 3 - Reportes Financieros + FASE 4 - Testing + FASE 5
```

**Fecha límite:** 15 de mayo de 2026

---

## 🔄 SIGUIENTES PASOS

### Decisión Crítica
1. ⚠️ **DECIDIR:** ¿Se gestiona personal en la empresa?
   - ✅ **SI:** Implementar Sprint 8 completo
   - ❌ **NO:** Saltar al Sprint 9 (UX/PWA), implementar solo FASE 3 (Reportes)

### Si se implementa (Antes de iniciar)
2. ✅ Revisar y aprobar ÍNDICE_SPRINT_8.md
3. 🔍 Consultar con contador sobre normativa laboral 2026
4. 🔍 Validar porcentajes de CCSS e INS actualizados
5. 🔍 Obtener tabla de impuesto sobre la renta 2026
6. 📋 Crear CHECKLIST_SPRINT_8.md detallado
7. 📋 Crear SPRINT_8_PLAN_MAESTRO.md

### Primera Fase
8. 🚀 Iniciar FASE 1: RRHH (Gestión de empleados)

---

### Si NO se implementa RRHH/Nómina
2. ✅ Implementar solo **FASE 3: Reportes Financieros** (sin nómina)
3. 🚀 Saltar al Sprint 9: UX/PWA/Mobile

---

## 📚 REFERENCIAS

- [Clasificación Sprints Futuros](../CLASIFICACION_SPRINTS_FUTUROS.md)
- [Estado Proyecto](../../reportes/ESTADO_PROYECTO.md)
- [Sprint 5 - Contabilidad](../SPRINT_5/) (para integración)
- [Sprint 7 - Seguridad](../SPRINT_7/)
- [CCSS - Caja Costarricense de Seguro Social](https://www.ccss.sa.cr/)
- [Código de Trabajo Costa Rica](http://www.mtss.go.cr/)
- [Ministerio de Hacienda CR - Renta](https://www.hacienda.go.cr/)

---

## 💡 NOTAS TÉCNICAS

### Cálculo de Nómina (Ejemplo)

**Empleado:** Juan Pérez  
**Salario bruto mensual:** ₡1,500,000  
**Jornada:** Ordinaria diurna  

#### Deducciones Obrero:
```
CCSS (9.34%):           ₡140,100
INS (1%):               ₡15,000
Impuesto Renta (15%):   ₡77,850  (sobre excedente de ₡941,000)
─────────────────────────────────
Total deducciones:      ₡232,950
```

#### Salario Neto:
```
Salario bruto:          ₡1,500,000
- Deducciones:          ₡232,950
═════════════════════════════════
SALARIO NETO:           ₡1,267,050
```

#### Cargas Patronales:
```
CCSS patronal (26.67%): ₡400,050
INS (~1%):              ₡15,000
Aguinaldo (8.33%):      ₡124,950
Cesantía (8.33%):       ₡124,950
─────────────────────────────────
Total carga patronal:   ₡664,950
```

#### Costo Total Empleado:
```
Salario bruto:          ₡1,500,000
+ Cargas patronales:    ₡664,950
═════════════════════════════════
COSTO TOTAL:            ₡2,164,950  (144% del salario bruto)
```

---

### Modelo de Datos

**Empleado:**
```java
@Entity
public class Empleado {
    @Id
    @GeneratedValue
    private Long id;
    
    private String nombre;
    private String cedula; // 1-1234-5678
    private String email;
    private String telefono;
    
    @ManyToOne
    private Departamento departamento;
    
    @ManyToOne
    private Puesto puesto;
    
    private BigDecimal salarioBruto;
    private LocalDate fechaIngreso;
    private LocalDate fechaSalida; // nullable
    private String tipoContrato; // PLAZO_FIJO, INDEFINIDO
    
    private Boolean activo;
}
```

**Nómina:**
```java
@Entity
public class Nomina {
    @Id
    @GeneratedValue
    private Long id;
    
    private Integer mes;
    private Integer anio;
    private LocalDate fechaProceso;
    private LocalDate fechaPago;
    
    private String estado; // BORRADOR, PROCESADA, PAGADA
    
    @OneToMany(mappedBy = "nomina")
    private List<DetalleNomina> detalles;
    
    private BigDecimal totalBruto;
    private BigDecimal totalDeducciones;
    private BigDecimal totalNeto;
    private BigDecimal totalCargasPatronales;
}
```

**DetalleNomina:**
```java
@Entity
public class DetalleNomina {
    @Id
    @GeneratedValue
    private Long id;
    
    @ManyToOne
    private Nomina nomina;
    
    @ManyToOne
    private Empleado empleado;
    
    // Ingresos
    private BigDecimal salarioBruto;
    private BigDecimal horasExtra;
    private BigDecimal bonificaciones;
    
    // Deducciones
    private BigDecimal deduccionCCSS;
    private BigDecimal deduccionINS;
    private BigDecimal deduccionRenta;
    private BigDecimal prestamos;
    private BigDecimal embargos;
    
    // Prestaciones
    private BigDecimal aguinaldoAcumulado;
    private BigDecimal cesantiaAcumulada;
    
    // Cargas patronales
    private BigDecimal ccssPatronal;
    private BigDecimal insPatronal;
    
    // Resultado
    private BigDecimal salarioNeto;
    private BigDecimal costoTotal;
}
```

---

### Integración con Contabilidad

**Asiento contable de nómina:**
```
DEBE:
  Gastos de Salarios           ₡1,500,000
  Gastos CCSS Patronal         ₡400,050
  Gastos INS Patronal          ₡15,000
  Gastos Aguinaldo             ₡124,950
  Gastos Cesantía              ₡124,950
                               ───────────
  Total DEBE                   ₡2,164,950

HABER:
  CCSS por Pagar (obrero)      ₡140,100
  CCSS por Pagar (patronal)    ₡400,050
  INS por Pagar (obrero)       ₡15,000
  INS por Pagar (patronal)     ₡15,000
  Renta por Pagar              ₡77,850
  Provisión Aguinaldo          ₡124,950
  Provisión Cesantía           ₡124,950
  Salarios por Pagar           ₡1,267,050
                               ───────────
  Total HABER                  ₡2,164,950
```

---

### Reportes Financieros

**Estado de Resultados (Ejemplo simplificado):**
```
══════════════════════════════════════════════════
  ESTADO DE RESULTADOS - Enero 2026
══════════════════════════════════════════════════

INGRESOS
  Ventas                              ₡15,000,000
  Otros ingresos                         ₡500,000
                                      ────────────
  Total Ingresos                      ₡15,500,000

COSTOS Y GASTOS
  Costo de Ventas                      ₡8,000,000
  Gastos de Personal                   ₡2,164,950
  Gastos Operativos                    ₡1,500,000
  Gastos Administrativos               ₡1,000,000
                                      ────────────
  Total Costos y Gastos               ₡12,664,950

                                      ════════════
UTILIDAD NETA                          ₡2,835,050
══════════════════════════════════════════════════
```

---

**Documento creado:** 16 de enero de 2026  
**Creado por:** GitHub Copilot  
**Versión:** 1.0  
**Estado:** 📋 PLANIFICADO  
**Tipo:** ⚠️ **CONDICIONAL** - Solo si se gestiona personal  
**Decisión pendiente:** ¿La empresa gestiona empleados en planilla?
