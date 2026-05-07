## 👥 SPRINT 8: RECURSOS HUMANOS + NÓMINA + REPORTES AVANZADOS

**Duración estimada:** 22-28 días  
**Prioridad:** ⭐⭐ MEDIA  
**Dependencias:** Sprint 5 (Contabilidad)

### 🎯 Objetivos Estratégicos
Gestión completa de RRHH + Nómina + Reportes financieros avanzados.

---

### FASE 8.1: Módulo de Recursos Humanos (NUEVA)

**Duración:** 8-10 días | **Tareas:** 48 tareas  
**Prioridad:** ⭐⭐ MEDIA

#### Tareas Principales (48)

**Base de Datos (10 tareas)**
- [ ] Crear tabla `empleado` (separada de usuario)
- [ ] Crear tabla `departamento`
- [ ] Crear tabla `puesto`
- [ ] Crear tabla `contrato_empleado`
- [ ] Crear tabla `asistencia`
- [ ] Crear tabla `ausencia` (vacaciones, permisos, bajas)
- [ ] Crear tabla `evaluacion_desempeno`
- [ ] Crear tabla `capacitacion`
- [ ] Vista: `v_empleados_activos`
- [ ] SP: `sp_reporte_asistencias(mes, anio)`

**Backend (14 tareas)**
- [ ] Entidad `Empleado.java`
- [ ] Entidad `Departamento.java`
- [ ] Entidad `Puesto.java`
- [ ] Entidad `ContratoEmpleado.java`
- [ ] Entidad `Asistencia.java`
- [ ] Entidad `Ausencia.java`
- [ ] Enum `TipoContrato` (INDEFINIDO, PLAZO_FIJO, EVENTUAL)
- [ ] Enum `TipoAusencia` (VACACIONES, PERMISO, BAJA_MEDICA, INCAPACIDAD)
- [ ] `EmpleadoRepository` + queries
- [ ] `AsistenciaRepository` + queries
- [ ] `EmpleadoService` + Impl
- [ ] `AsistenciaService` + Impl
- [ ] `EmpleadoController` (vistas)
- [ ] `EmpleadoRestController` (API)

**Frontend (10 tareas)**
- [ ] Vista: `rrhh/empleados/lista.html`
- [ ] Vista: `rrhh/empleados/form.html`
- [ ] Vista: `rrhh/empleados/detalle.html` (ficha completa)
- [ ] Vista: `rrhh/asistencias/registro.html`
- [ ] Vista: `rrhh/asistencias/reporte.html`
- [ ] Vista: `rrhh/ausencias/gestionar.html`
- [ ] Vista: `rrhh/departamentos.html`
- [ ] Vista: `rrhh/puestos.html`
- [ ] JavaScript: `empleados.js`
- [ ] JavaScript: `asistencias.js`

**Integraciones (8 tareas)**
- [ ] Integrar con Usuario (un empleado puede tener usuario)
- [ ] Portal del empleado (ver recibos, solicitar vacaciones)
- [ ] Reportes: Empleados por departamento
- [ ] Reportes: Contratos próximos a vencer
- [ ] Reportes: Ausencias por tipo
- [ ] Exportar ficha de empleado a PDF
- [ ] Notificaciones: Contrato por vencer
- [ ] Dashboard: KPIs de RRHH

**Testing (4 tareas)**
- [ ] `EmpleadoServiceTest`
- [ ] `AsistenciaServiceTest`
- [ ] Test: Registro de asistencias
- [ ] Test: Gestión de ausencias

**Documentación (2 tareas)**
- [ ] `SPRINT_8_FASE_1_RRHH.md`
- [ ] `MANUAL_RECURSOS_HUMANOS.md`

---

### FASE 8.2: Módulo de Nómina (NUEVA)

**Duración:** 10-12 días | **Tareas:** 56 tareas  
**Prioridad:** ⭐⭐⭐ ALTA (si aplica)

#### Tareas Principales (56)

**Base de Datos (12 tareas)**
- [ ] Crear tabla `nomina` (periodo, estado, total)
- [ ] Crear tabla `concepto_nomina` (catálogo: sueldo, bono, deducción)
- [ ] Crear tabla `detalle_nomina` (empleado, concepto, monto)
- [ ] Crear tabla `regla_calculo` (fórmulas de nómina)
- [ ] Crear tabla `retencion` (impuestos, seguridad social)
- [ ] Crear tabla `prestamo_empleado`
- [ ] Crear tabla `anticipo_salario`
- [ ] Vista: `v_nomina_actual`
- [ ] SP: `sp_calcular_nomina(id_periodo)`
- [ ] SP: `sp_generar_recibos(id_nomina)`
- [ ] SP: `sp_reporte_costos_nomina(anio)`
- [ ] Trigger: Registrar asiento contable de nómina

**Backend (18 tareas)**
- [ ] Entidad `Nomina.java`
- [ ] Entidad `ConceptoNomina.java`
- [ ] Entidad `DetalleNomina.java`
- [ ] Entidad `ReglaCalculo.java`
- [ ] Entidad `Retencion.java`
- [ ] Enum `TipoConcepto` (PERCEPCION, DEDUCCION)
- [ ] Enum `EstadoNomina` (BORRADOR, CALCULADA, PAGADA, CERRADA)
- [ ] `NominaRepository` + queries
- [ ] `ConceptoNominaRepository`
- [ ] `NominaService` + Impl (cálculo, generación)
- [ ] `CalculoNominaService` (motor de cálculo)
- [ ] Integrar con RRHH (obtener empleados activos)
- [ ] Integrar con Asistencia (horas trabajadas, extras)
- [ ] Integrar con Contabilidad (asientos de nómina)
- [ ] Integrar con Pagos (pago de nómina)
- [ ] `NominaController` (vistas)
- [ ] `NominaRestController` (API)
- [ ] Generador de recibos de pago (PDF)

**Frontend (12 tareas)**
- [ ] Vista: `nomina/periodos.html` (gestión de periodos)
- [ ] Vista: `nomina/calcular.html` (cálculo de nómina)
- [ ] Vista: `nomina/revision.html` (revisar antes de pagar)
- [ ] Vista: `nomina/recibos.html` (generar recibos)
- [ ] Vista: `nomina/conceptos.html` (CRUD conceptos)
- [ ] Vista: `nomina/retenciones.html` (configurar retenciones)
- [ ] Vista: `nomina/reportes/costos.html`
- [ ] Vista: `nomina/reportes/deducciones.html`
- [ ] Vista: `portal-empleado/mi-recibo.html`
- [ ] JavaScript: `nomina.js`
- [ ] JavaScript: `calculo-nomina.js`
- [ ] Dashboard: Widget de nómina actual

**Integraciones (8 tareas)**
- [ ] Depósito directo a bancos (archivo SEPA/ACH)
- [ ] Envío de recibos por email
- [ ] Cálculo de impuestos según tablas
- [ ] Reportes legales (libro de remuneraciones)
- [ ] Exportar nómina a Excel
- [ ] Integración con banco (pago electrónico)
- [ ] Notificaciones: Nómina lista
- [ ] Archivo para declaraciones fiscales

**Testing (4 tareas)**
- [ ] `NominaServiceTest`
- [ ] `CalculoNominaServiceTest`
- [ ] Test: Cálculo correcto de nómina
- [ ] Test: Asiento contable generado

**Documentación (2 tareas)**
- [ ] `SPRINT_8_FASE_2_NOMINA.md`
- [ ] `MANUAL_NOMINA.md`

---

### FASE 8.3: Reportes Financieros Avanzados (AMPLIAR)

**Duración:** 4-6 días | **Tareas:** 32 tareas  
**Prioridad:** ⭐⭐⭐ ALTA

#### Estado Actual
- ✅ Reportes básicos implementados (Sprint 4)
- ⚠️ Faltan estados financieros completos
- ⚠️ Falta análisis financiero

#### Tareas Principales (32)

**Base de Datos (6 tareas)**
- [ ] SP: `sp_estado_resultados(fecha_inicio, fecha_fin)`
- [ ] SP: `sp_balance_general(fecha_corte)`
- [ ] SP: `sp_flujo_efectivo(periodo)`
- [ ] SP: `sp_razones_financieras(periodo)`
- [ ] SP: `sp_punto_equilibrio()`
- [ ] Vista: `v_kpis_financieros`

**Backend (8 tareas)**
- [ ] `EstadosFinancierosService` + Impl
- [ ] DTO: `EstadoResultadosDTO`
- [ ] DTO: `BalanceGeneralDTO`
- [ ] DTO: `FlujoEfectivoDTO`
- [ ] DTO: `RazonesFinancierasDTO`
- [ ] `EstadosFinancierosController`
- [ ] Exportar estados financieros a PDF
- [ ] Exportar estados financieros a Excel

**Frontend (10 tareas)**
- [ ] Vista: `reportes/financieros/estado-resultados.html`
- [ ] Vista: `reportes/financieros/balance-general.html`
- [ ] Vista: `reportes/financieros/flujo-efectivo.html`
- [ ] Vista: `reportes/financieros/razones-financieras.html`
- [ ] Vista: `reportes/financieros/punto-equilibrio.html`
- [ ] Dashboard financiero ejecutivo
- [ ] Gráficas: Tendencias financieras
- [ ] Gráficas: Comparativas periodo a periodo
- [ ] JavaScript: `estados-financieros.js`
- [ ] Exportación personalizada

**Análisis Avanzado (4 tareas)**
- [ ] Proyecciones financieras (básicas)
- [ ] Análisis de tendencias
- [ ] Comparativas año vs año
- [ ] Alertas de salud financiera

**Testing (2 tareas)**
- [ ] `EstadosFinancierosServiceTest`
- [ ] Test: Cálculo correcto de estados

**Documentación (2 tareas)**
- [ ] `SPRINT_8_FASE_3_REPORTES_AVANZADOS.md`
- [ ] `MANUAL_ESTADOS_FINANCIEROS.md`

---

### 📊 Resumen Sprint 8

```
┌─────────────────────────────────────────────────────────────┐
│                    SPRINT 8 - RESUMEN                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  FASE 8.1: RRHH                   [48 tareas]  8-10 días ⭐⭐ │
│  FASE 8.2: Nómina                 [56 tareas] 10-12 días ⭐⭐⭐│
│  FASE 8.3: Reportes Avanzados     [32 tareas]  4-6 días ⭐⭐⭐│
│  Testing + Documentación          [12 tareas]  2-3 días ⭐⭐ │
│                                                              │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│  TOTAL SPRINT 8                   [148 tareas] 24-31 días   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

