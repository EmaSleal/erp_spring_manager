# 📚 FASE 5: Documentación de Usuario y Normativa

**Sprint:** 8  
**Fase:** 5 de 5  
**Duración estimada:** 1-2 días  
**Prioridad:** ⭐⭐ ALTA  
**Estado:** 📋 PENDIENTE (0/4 tareas)

---

## 📋 OBJETIVO DE LA FASE

Crear documentación completa de:
- **Manual de RRHH** (gestión de empleados)
- **Manual de Nómina** (cálculo y procesamiento)
- **Manual de Reportes Financieros**
- **Guía de Normativa Laboral de Costa Rica**

---

## 📊 PROGRESO GENERAL

```
Progreso: [0/4] ░░░░░░░░░░░░░░░░░░░░ 0%

├─ 1. Manual de RRHH                  [0/1]  ░░░░░░░░░░ 0%
├─ 2. Manual de Nómina                [0/1]  ░░░░░░░░░░ 0%
├─ 3. Manual de Reportes Financieros  [0/1]  ░░░░░░░░░░ 0%
└─ 4. Guía Normativa Laboral CR       [0/1]  ░░░░░░░░░░ 0%
```

---

## 📦 1. MANUAL DE RRHH (1 tarea)

- [ ] **1.1** Crear `docs/guias/MANUAL_RRHH.md`

### Contenido del Manual (700+ líneas):

```markdown
# 👥 Manual de Gestión de Recursos Humanos

**Sistema:** WhatsApp Orders Manager - Módulo RRHH  
**Versión:** 1.0  
**Fecha:** Enero 2026  
**Sprint:** 8 - Fase 1

---

## 📋 ÍNDICE

1. [Introducción](#introducción)
2. [Gestión de Departamentos](#gestión-de-departamentos)
3. [Gestión de Puestos](#gestión-de-puestos)
4. [Gestión de Empleados](#gestión-de-empleados)
5. [Control de Asistencia](#control-de-asistencia)
6. [Gestión de Vacaciones](#gestión-de-vacaciones)
7. [Gestión de Permisos](#gestión-de-permisos)
8. [Evaluaciones de Desempeño](#evaluaciones-de-desempeño)
9. [Expediente Digital](#expediente-digital)
10. [Reportes de RRHH](#reportes-de-rrhh)

---

## 1. INTRODUCCIÓN

El módulo de **Recursos Humanos** permite gestionar:
- ✅ Estructura organizacional (departamentos, puestos)
- ✅ Registro de empleados con datos completos
- ✅ Control de asistencia y horarios
- ✅ Gestión de vacaciones y permisos
- ✅ Evaluaciones de desempeño
- ✅ Expediente digital por empleado

### Acceso al módulo

**Ruta:** Sistema → Recursos Humanos  
**Permisos requeridos:** `RRHH_VIEW`, `RRHH_EDIT`, `RRHH_ADMIN`

---

## 2. GESTIÓN DE DEPARTAMENTOS

### 2.1. Crear Departamento

**Pasos:**
1. Ir a **RRHH → Departamentos**
2. Click en **Nuevo Departamento**
3. Completar formulario:
   - **Nombre:** Nombre del departamento
   - **Código:** Código único (ej: "VENTAS", "ADM")
   - **Descripción:** Descripción breve
   - **Jefe de Departamento:** Seleccionar empleado
   - **Presupuesto Anual:** Monto asignado
   - **Estado:** Activo/Inactivo
4. Click en **Guardar**

### 2.2. Editar Departamento

1. Ir a **RRHH → Departamentos**
2. Click en icono **✏️ Editar**
3. Modificar datos
4. Click en **Guardar**

### 2.3. Asignar Jefe de Departamento

1. Editar departamento
2. En campo **Jefe de Departamento**, seleccionar empleado
3. Guardar

⚠️ **Nota:** El empleado debe existir previamente.

---

## 3. GESTIÓN DE PUESTOS

### 3.1. Crear Puesto

**Pasos:**
1. Ir a **RRHH → Puestos**
2. Click en **Nuevo Puesto**
3. Completar:
   - **Nombre:** Nombre del puesto
   - **Departamento:** Seleccionar
   - **Nivel:** Junior/Mid/Senior/Manager
   - **Salario Mínimo:** Rango salarial mínimo
   - **Salario Máximo:** Rango salarial máximo
   - **Descripción:** Funciones y responsabilidades
   - **Requisitos:** Educación, experiencia, habilidades
4. Guardar

### Ejemplo:

```
Puesto: Desarrollador de Software
Departamento: Tecnología
Nivel: Mid
Salario Mínimo: ₡800,000
Salario Máximo: ₡1,500,000
Descripción: Desarrollo de aplicaciones Java Spring Boot
Requisitos: Ing. Sistemas, 2+ años experiencia Java
```

---

## 4. GESTIÓN DE EMPLEADOS

### 4.1. Registrar Nuevo Empleado

**Ruta:** RRHH → Empleados → Nuevo Empleado

#### Datos Personales:
- **Nombre Completo:** Primer y segundo apellido
- **Cédula:** Formato CR (1-1234-5678)
- **Fecha de Nacimiento**
- **Género:** M/F/Otro
- **Estado Civil**
- **Dirección Completa**
- **Teléfono**
- **Email Personal**
- **Email Corporativo**

#### Datos Laborales:
- **Departamento:** Seleccionar
- **Puesto:** Seleccionar
- **Fecha de Ingreso**
- **Tipo de Contrato:** Indefinido/Temporal/Por Obra
- **Jornada:** Tiempo Completo/Medio Tiempo
- **Salario Base:** Monto mensual

#### Datos Seguridad Social (Costa Rica):
- **Número CCSS:** Número de asegurado CCSS
- **Número INS:** (Opcional)

#### Datos Bancarios:
- **Banco:** Seleccionar banco CR
- **Tipo de Cuenta:** Ahorros/Corriente
- **Número de Cuenta IBAN**

#### Contacto de Emergencia:
- **Nombre Completo**
- **Relación:** (Padre/Madre/Cónyuge/Hermano/Otro)
- **Teléfono**

### 4.2. Editar Empleado

1. Buscar empleado en lista
2. Click en **✏️ Editar**
3. Modificar datos necesarios
4. **Guardar**

### 4.3. Activar/Desactivar Empleado

Para empleados que ya no laboran:
1. Editar empleado
2. Cambiar **Estado** a "Inactivo"
3. Registrar **Fecha de Salida**
4. Guardar

---

## 5. CONTROL DE ASISTENCIA

### 5.1. Registrar Asistencia

**Opciones:**
- **Registro Manual:** Administrador registra entrada/salida
- **Auto-registro:** Empleado registra desde su perfil

**Pasos (Manual):**
1. Ir a **RRHH → Asistencia → Registrar**
2. Seleccionar **Empleado**
3. Seleccionar **Fecha**
4. Registrar **Hora de Entrada**
5. Registrar **Hora de Salida**
6. Guardar

### 5.2. Marcar Ausencia

1. RRHH → Asistencia → Registrar
2. Seleccionar empleado y fecha
3. Marcar como **Ausencia**
4. Indicar **Motivo:** (Enfermedad/Permiso/Falta injustificada)
5. Guardar

### 5.3. Calcular Horas Extras

El sistema calcula automáticamente:
- **Horas Ordinarias:** Hasta 8 horas diarias
- **Horas Extra 50%:** De 8 a 12 horas
- **Horas Extra Doble:** Más de 12 horas
- **Horas Nocturnas:** 7pm - 5am

---

## 6. GESTIÓN DE VACACIONES

### 6.1. Solicitar Vacaciones (Empleado)

1. Mi Perfil → Vacaciones → **Solicitar**
2. Seleccionar **Fecha Inicio** y **Fecha Fin**
3. Indicar **Días solicitados**
4. Agregar **Observaciones** (opcional)
5. Click en **Enviar Solicitud**

El sistema valida:
✅ Días disponibles  
✅ No solapamiento con otras solicitudes  
✅ Aprobación de jefe inmediato

### 6.2. Aprobar/Rechazar Vacaciones (Jefe)

1. RRHH → Vacaciones → **Pendientes de Aprobación**
2. Revisar solicitud
3. Click en **✅ Aprobar** o **❌ Rechazar**
4. Si rechaza, indicar **Motivo**

### 6.3. Consultar Saldo de Vacaciones

**Empleado:**
- Mi Perfil → Vacaciones → Ver Saldo

**Administrador:**
- RRHH → Empleados → [Empleado] → Vacaciones

El sistema muestra:
- Días acumulados
- Días tomados
- Días disponibles
- Próximo vencimiento

---

## 7. GESTIÓN DE PERMISOS

Similar a vacaciones:
- Solicitud por empleado
- Aprobación por jefe
- Tipos: Con goce/Sin goce de salario

---

## 8. EVALUACIONES DE DESEMPEÑO

### 8.1. Crear Evaluación

1. RRHH → Evaluaciones → **Nueva Evaluación**
2. Seleccionar **Empleado**
3. Seleccionar **Período** (ej: Enero-Junio 2026)
4. Completar **Calificación:** (1-5 estrellas)
5. Agregar **Comentarios y Observaciones**
6. Definir **Objetivos para próximo período**
7. Guardar

### 8.2. Historial de Evaluaciones

- RRHH → Empleados → [Empleado] → Evaluaciones
- Ver todas las evaluaciones históricas

---

## 9. EXPEDIENTE DIGITAL

### 9.1. Subir Documentos

1. RRHH → Empleados → [Empleado] → Expediente
2. Click en **Subir Documento**
3. Seleccionar archivo (PDF, DOC, JPG)
4. Indicar **Tipo:** (Contrato/Título/Certificación/Otro)
5. Agregar **Descripción**
6. Guardar

### 9.2. Descargar Documentos

- Click en nombre del archivo
- Se descarga automáticamente

---

## 10. REPORTES DE RRHH

### 10.1. Reporte de Empleados Activos

**Ruta:** RRHH → Reportes → Empleados Activos

**Incluye:**
- Lista de todos los empleados activos
- Departamento, Puesto, Salario
- Fecha de ingreso
- Antigüedad

**Exportar:** Excel, PDF

### 10.2. Reporte de Asistencia

Filtros:
- Fecha desde/hasta
- Departamento
- Empleado

Muestra:
- Días trabajados
- Horas extras
- Ausencias

---

## ⚠️ NORMATIVA LABORAL DE COSTA RICA

Este módulo cumple con:
- ✅ Código de Trabajo de Costa Rica
- ✅ Ley de Protección de Datos Personales
- ✅ Normativa CCSS e INS

**Más información:** Ver [GUIA_NORMATIVA_LABORAL_CR.md]

---

**Fin del Manual de RRHH**
```

---

## 📦 2. MANUAL DE NÓMINA (1 tarea)

- [ ] **2.1** Crear `docs/guias/MANUAL_NOMINA.md`

### Contenido (900+ líneas):

```markdown
# 💰 Manual de Procesamiento de Nómina

**Sistema:** WhatsApp Orders Manager - Módulo Nómina  
**Versión:** 1.0  
**Cumplimiento:** Normativa Laboral de Costa Rica 2026

---

## 📋 ÍNDICE

1. [Introducción](#introducción)
2. [Crear Nueva Nómina](#crear-nueva-nómina)
3. [Cálculo de Salarios](#cálculo-de-salarios)
4. [Deducciones CCSS](#deducciones-ccss)
5. [Deducciones INS](#deducciones-ins)
6. [Impuesto sobre la Renta](#impuesto-sobre-la-renta)
7. [Aguinaldo](#aguinaldo)
8. [Cesantía](#cesantía)
9. [Aprobar y Pagar Nómina](#aprobar-y-pagar-nómina)
10. [Reportes de Nómina](#reportes-de-nómina)

---

## 1. INTRODUCCIÓN

El módulo de **Nómina** permite:
- ✅ Cálculo automático de salarios
- ✅ Deducciones CCSS (9.34% obrero, 26.67% patronal)
- ✅ Deducciones INS (~1%)
- ✅ Cálculo de Impuesto sobre la Renta (progresivo)
- ✅ Provisión de Aguinaldo (8.33%)
- ✅ Provisión de Cesantía (8.33%)
- ✅ Generación de recibos de pago
- ✅ Integración contable automática

---

## 2. CREAR NUEVA NÓMINA

**Pasos:**
1. Ir a **Nómina → Gestión de Nómina**
2. Click en **Nueva Nómina**
3. Completar:
   - **Período:** Seleccionar mes y año
   - **Tipo:** Ordinaria/Extraordinaria/Aguinaldo
   - **Fecha de Pago:** Cuándo se pagará
   - **Descripción:** "Nómina Enero 2026"
4. Click en **Crear**

El sistema carga automáticamente todos los empleados activos.

---

## 3. CÁLCULO DE SALARIOS

### 3.1. Componentes del Salario Bruto

```
SALARIO BRUTO = Salario Base + Horas Extras + Otros Ingresos
```

**Tipos de Horas Extras:**
- **Horas Extra 50%:** Pago a 1.5x (de 8 a 12 horas diarias)
- **Horas Extra Doble:** Pago a 2x (más de 12 horas)
- **Horas Nocturnas:** Pago a 1.5x (7pm - 5am)

**Ejemplo:**

```
Empleado: Juan Pérez
Salario Base: ₡1,000,000

Horas trabajadas en el mes:
- Ordinarias: 176 horas
- Extra 50%: 10 horas
- Nocturnas: 8 horas

Cálculo:
Valor hora = 1,000,000 / 176 = ₡5,681.82
Horas extra 50% = 10 * 5,681.82 * 1.5 = ₡85,227.30
Horas nocturnas = 8 * 5,681.82 * 1.5 = ₡68,181.84

SALARIO BRUTO = 1,000,000 + 85,227 + 68,182 = ₡1,153,409
```

### 3.2. Calcular Nómina

1. Nómina → Gestión → [Seleccionar Nómina]
2. Click en **Calcular Nómina**
3. El sistema calcula automáticamente:
   - Salarios brutos
   - Deducciones CCSS e INS
   - Impuesto Renta
   - Salario neto
   - Cargas patronales
   - Provisiones (aguinaldo, cesantía)

---

## 4. DEDUCCIONES CCSS

### 4.1. CCSS Obrero (9.34%)

**Fórmula:**
```
Deducción CCSS Obrero = Salario Bruto * 9.34%
```

**Ejemplo:**
```
Salario Bruto: ₡1,153,409
CCSS Obrero: 1,153,409 * 9.34% = ₡107,728.40
```

Esta deducción se **descuenta** del salario del empleado.

### 4.2. CCSS Patronal (26.67%)

**Fórmula:**
```
Carga Patronal CCSS = Salario Bruto * 26.67%
```

**Ejemplo:**
```
Salario Bruto: ₡1,153,409
CCSS Patronal: 1,153,409 * 26.67% = ₡307,614.20
```

Esta es un **costo adicional** para la empresa.

---

## 5. DEDUCCIONES INS

### 5.1. INS Riesgos del Trabajo (~1%)

**Fórmula:**
```
Deducción INS = Salario Bruto * 1%
```

**Ejemplo:**
```
Salario Bruto: ₡1,153,409
INS: 1,153,409 * 1% = ₡11,534.09
```

---

## 6. IMPUESTO SOBRE LA RENTA

### 6.1. Tabla Progresiva 2026 (Costa Rica)

| Rango Salarial             | Impuesto |
|----------------------------|----------|
| ₡0 - ₡941,000              | Exento   |
| ₡941,001 - ₡1,381,000      | 10%      |
| ₡1,381,001 - ₡2,423,000    | 15%      |
| ₡2,423,001 - ₡4,845,000    | 20%      |
| Más de ₡4,845,000          | 25%      |

### 6.2. Ejemplo de Cálculo

**Salario:** ₡1,153,409

```
Tramo 1: ₡0 - ₡941,000 → Exento
Tramo 2: ₡1,153,409 - ₡941,000 = ₡212,409 → 212,409 * 10% = ₡21,240.90

IMPUESTO TOTAL = ₡21,240.90
```

---

## 7. AGUINALDO

### 7.1. Cálculo del Aguinaldo (8.33%)

El **aguinaldo** es un salario adicional que se paga en diciembre.  
Se provisiona mensualmente al **8.33%** del salario bruto.

**Fórmula:**
```
Provisión Aguinaldo = Salario Bruto * 8.33%
```

**Ejemplo:**
```
Salario Bruto: ₡1,153,409
Aguinaldo: 1,153,409 * 8.33% = ₡96,078.97
```

### 7.2. Pago del Aguinaldo

El aguinaldo se paga en **diciembre** de cada año.  
El sistema acumula las provisiones mensuales.

---

## 8. CESANTÍA

### 8.1. Cálculo de Cesantía (8.33%)

La **cesantía** se paga al finalizar la relación laboral.  
Se provisiona mensualmente al **8.33%** del salario bruto.

**Fórmula:**
```
Provisión Cesantía = Salario Bruto * 8.33%
```

---

## 9. APROBAR Y PAGAR NÓMINA

### 9.1. Revisar Detalle

1. Nómina → Gestión → [Seleccionar Nómina]
2. Ver **Detalles por Empleado**
3. Revisar:
   - Salarios brutos
   - Deducciones
   - Salarios netos
   - Total a pagar

### 9.2. Aprobar Nómina

1. Click en **Aprobar Nómina**
2. Confirmar aprobación
3. El sistema genera automáticamente:
   - ✅ Asiento contable
   - ✅ Recibos de pago
   - ✅ Archivo SINPE para bancos

### 9.3. Descargar Recibos

- Nómina → Recibos
- Seleccionar empleado
- Descargar PDF

---

## 10. REPORTES DE NÓMINA

### 10.1. Reporte de Nómina Mensual

**Incluye:**
- Total salarios brutos
- Total deducciones CCSS
- Total deducciones INS
- Total impuestos
- Total salarios netos
- Total cargas patronales

**Exportar:** Excel, PDF

### 10.2. Reporte de Provisiones

Muestra provisiones acumuladas de:
- Aguinaldo
- Cesantía

---

**Fin del Manual de Nómina**
```

---

## 📦 3. MANUAL DE REPORTES FINANCIEROS (1 tarea)

- [ ] **3.1** Crear `docs/guias/MANUAL_REPORTES_FINANCIEROS.md` (500+ líneas)

---

## 📦 4. GUÍA DE NORMATIVA LABORAL CR (1 tarea)

- [ ] **4.1** Crear `docs/guias/GUIA_NORMATIVA_LABORAL_CR.md`

### Contenido (400+ líneas):

```markdown
# 🇨🇷 Guía de Normativa Laboral de Costa Rica

**Actualizado:** Enero 2026  
**Fuentes:** Código de Trabajo CR, CCSS, MTSS

---

## 📋 ÍNDICE

1. [Seguridad Social (CCSS)](#seguridad-social-ccss)
2. [INS - Riesgos del Trabajo](#ins-riesgos-del-trabajo)
3. [Impuesto sobre la Renta](#impuesto-sobre-la-renta)
4. [Aguinaldo](#aguinaldo)
5. [Cesantía](#cesantía)
6. [Vacaciones](#vacaciones)
7. [Jornada Laboral](#jornada-laboral)

---

## 1. SEGURIDAD SOCIAL (CCSS)

### 1.1. Cuota Obrera (9.34%)

El **empleado** aporta **9.34%** de su salario bruto.

**Desglose:**
- Enfermedad y Maternidad: 5.50%
- Invalidez, Vejez y Muerte (IVM): 3.84%

### 1.2. Cuota Patronal (26.67%)

El **empleador** aporta **26.67%** del salario bruto del empleado.

**Desglose:**
- Enfermedad y Maternidad: 9.25%
- IVM: 5.25%
- Fondo de Pensiones Complementarias: 1.50%
- Banco Popular: 0.25%
- IMAS: 0.50%
- INA: 1.50%
- Asignaciones Familiares: 5.00%
- Fondo de Capitalización Laboral: 3.00%
- Otras contribuciones: 0.42%

---

## 2. INS - RIESGOS DEL TRABAJO

### 2.1. Porcentaje

El **empleador** paga al INS aproximadamente **1%** del salario bruto.

El porcentaje exacto varía según el **tipo de actividad** de la empresa (riesgo bajo, medio, alto).

---

## 3. IMPUESTO SOBRE LA RENTA

### 3.1. Tabla Progresiva 2026

| Rango Mensual              | Impuesto |
|----------------------------|----------|
| ₡0 - ₡941,000              | Exento   |
| ₡941,001 - ₡1,381,000      | 10%      |
| ₡1,381,001 - ₡2,423,000    | 15%      |
| ₡2,423,001 - ₡4,845,000    | 20%      |
| Más de ₡4,845,000          | 25%      |

**Base Legal:** Ley del Impuesto sobre la Renta (Artículo 33)

---

## 4. AGUINALDO

### 4.1. Monto

El empleado tiene derecho a un **aguinaldo** equivalente a **1 mes de salario** por año trabajado.

**Cálculo:** Promedio de salarios de diciembre a noviembre.

### 4.2. Fecha de Pago

Se paga a más tardar el **20 de diciembre** de cada año.

### 4.3. Provisión Mensual

Las empresas provisionan **8.33%** mensualmente (1/12).

**Base Legal:** Código de Trabajo, Artículo 229

---

## 5. CESANTÍA

### 5.1. Monto

Al finalizar la relación laboral, el empleado tiene derecho a **cesantía**:

| Antigüedad            | Días de Salario |
|-----------------------|-----------------|
| 3 meses - 6 meses     | 7 días          |
| 6 meses - 1 año       | 14 días         |
| 1 año - 3 años        | 19.5 días/año   |
| 3 años - 6 años       | 20 días/año     |
| Más de 6 años         | 21.5 días/año   |

### 5.2. Provisión

Las empresas provisionan **8.33%** mensualmente.

**Base Legal:** Código de Trabajo, Artículo 29

---

## 6. VACACIONES

### 6.1. Derecho

Los empleados tienen derecho a **2 semanas de vacaciones** por cada **50 semanas** laboradas (aproximadamente 1 año).

**Cálculo:** 1 día por cada mes trabajado.

### 6.2. Pago

Las vacaciones se pagan al **salario ordinario**.

**Base Legal:** Código de Trabajo, Artículos 153-163

---

## 7. JORNADA LABORAL

### 7.1. Jornada Ordinaria

- **Diurna:** 8 horas diarias, 48 horas semanales
- **Nocturna:** 6 horas diarias, 36 horas semanales
- **Mixta:** 7 horas diarias, 42 horas semanales

### 7.2. Horas Extras

- **Hasta 12 horas:** Pago a **1.5x** (50% adicional)
- **Más de 12 horas:** Pago a **2x** (doble)

**Base Legal:** Código de Trabajo, Artículos 136-143

---

**Fin de la Guía**
```

---

## 📊 CRITERIOS DE ACEPTACIÓN

✅ **Manual de RRHH completo** (700+ líneas)  
✅ **Manual de Nómina completo** (900+ líneas)  
✅ **Manual de Reportes Financieros** (500+ líneas)  
✅ **Guía de Normativa Laboral CR** (400+ líneas)  
✅ **Ejemplos prácticos incluidos**  
✅ **Capturas de pantalla** (opcional)  

---

## 📚 DEPENDENCIAS

**Requiere:**
- ✅ Sprint 8 Fase 1: RRHH
- ✅ Sprint 8 Fase 2: Nómina
- ✅ Sprint 8 Fase 3: Reportes
- ✅ Sprint 8 Fase 4: Testing

**Habilita:**
- 🚀 Sprint 9: UX/UI + PWA (OPCIONAL)

---

## 🎯 RESUMEN DEL SPRINT 8

Al completar esta fase, el **Sprint 8** estará **100% finalizado**:

- ✅ **FASE 1:** RRHH (48 tareas)
- ✅ **FASE 2:** Nómina (56 tareas)
- ✅ **FASE 3:** Reportes Financieros (32 tareas)
- ✅ **FASE 4:** Testing (8 tareas)
- ✅ **FASE 5:** Documentación (4 tareas)

**TOTAL:** 148 tareas | 24-31 días

---

**Fase creada:** 16 de enero de 2026  
**Responsable:** Technical Writers  
**Prioridad:** ALTA - Capacitación de usuarios
