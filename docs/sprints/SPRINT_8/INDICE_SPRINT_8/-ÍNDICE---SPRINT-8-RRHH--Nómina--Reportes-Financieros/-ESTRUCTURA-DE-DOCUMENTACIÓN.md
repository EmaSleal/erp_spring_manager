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

