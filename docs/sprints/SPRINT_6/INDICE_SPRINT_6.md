# 📑 ÍNDICE - SPRINT 6: Multi-Divisa + Inventario + Proveedores

**Proyecto:** WhatsApp Orders Manager - ERP Spring Boot  
**Sprint:** 6  
**Fecha Inicio:** 17 de febrero de 2026  
**Fecha Finalización:** 16 de marzo de 2026 (estimado)  
**Estado:** 📋 PLANIFICADO

---

## 📚 ESTRUCTURA DE DOCUMENTACIÓN

### 📄 Documentos Principales

#### 1. **CHECKLIST_SPRINT_6.md**
**Descripción:** Checklist maestro con todas las tareas del Sprint 6  
**Estado:** 📋 0/146 tareas (0%)  
**Contenido:**
- Progreso general (3 fases + testing + documentación)
- Checklist detallado por fase
- Estado de cada tarea
- Milestones críticos
- Métricas de rendimiento

**Ruta:** `docs/sprints/SPRINT_6/CHECKLIST_SPRINT_6.md`

---

#### 2. **RESUMEN_SPRINT_6.md**
**Descripción:** Resumen ejecutivo del Sprint 6  
**Contenido:**
- Objetivos del sprint
- Métricas en números
- Resumen de cada fase
- Archivos a crear/modificar
- Próximos pasos

**Ruta:** `docs/sprints/SPRINT_6/RESUMEN_SPRINT_6.md`

---

#### 3. **SPRINT_6_PLAN_MAESTRO.md**
**Descripción:** Plan detallado de ejecución del Sprint 6  
**Contenido:**
- Análisis de situación actual
- Objetivos y alcance
- Priorización de fases
- Análisis de riesgos
- Estrategia de implementación
- Integración con hallazgos del código

**Ruta:** `docs/sprints/SPRINT_6/SPRINT_6_PLAN_MAESTRO.md`

---

### 📦 Documentación por Fases

#### **FASE 1: Multi-Divisa (Monedas y Tipos de Cambio)**
**Estado:** 📋 PENDIENTE (0/42 tareas)  
**Duración:** 6-8 días  
**Prioridad:** ⭐⭐⭐ CRÍTICA

**Documentación:**
- `fases/FASE_1_MULTI_DIVISA.md` - Implementación completa

**Entregables:**
- Modelo `Moneda.java` (USD, EUR, CRC, etc.)
- Modelo `TipoCambio.java` con histórico
- Servicio de actualización de tasas (API externa)
- Conversión automática en transacciones
- Reportes multi-divisa
- **Integración con `formatearMoneda()` existente** ✅

**Hallazgos aplicados:**
- ✅ Aprovechar método `formatearMoneda()` ya implementado
- 🔧 Extenderlo para soportar múltiples símbolos de divisa

**Ruta:** `docs/sprints/SPRINT_6/fases/FASE_1_MULTI_DIVISA.md`

---

#### **FASE 2: Inventario Avanzado (Control de Stock)**
**Estado:** 📋 PENDIENTE (0/48 tareas)  
**Duración:** 7-9 días  
**Prioridad:** ⭐⭐⭐ CRÍTICA

**Documentación:**
- `fases/FASE_2_INVENTARIO.md` - Sistema completo de inventario

**Entregables:**
- Modelo `MovimientoInventario.java`
- Kardex detallado por producto
- Gestión de lotes y vencimientos
- Alertas de stock mínimo/bajo
- Reporte de rotación de inventario
- Ajustes de inventario (positivos/negativos)
- **Activar enum `PRODUCTO_AJUSTAR_INVENTARIO`** ⚠️

**Hallazgos aplicados:**
- ⚠️ Activar y usar `PRODUCTO_AJUSTAR_INVENTARIO` (actualmente no se usa)
- 🔧 Implementar filtro `stockBajo` en reportes (actualmente TODO)
- 🔧 Completar funcionalidad de alertas de stock

**Ruta:** `docs/sprints/SPRINT_6/fases/FASE_2_INVENTARIO.md`

---

#### **FASE 3: Proveedores (Cuentas por Pagar)**
**Estado:** 📋 PENDIENTE (0/44 tareas)  
**Duración:** 6-8 días  
**Prioridad:** ⭐⭐ ALTA

**Documentación:**
- `fases/FASE_3_PROVEEDORES.md` - Gestión completa de proveedores

**Entregables:**
- Modelo `Proveedor.java` completo
- Modelo `OrdenCompra.java`
- Modelo `CuentaPorPagar.java`
- Gestión de pagos a proveedores
- Historial de compras y pagos
- Evaluación de proveedores
- Conciliación de cuentas

**Ruta:** `docs/sprints/SPRINT_6/fases/FASE_3_PROVEEDORES.md`

---

#### **FASE 4: Testing Automatizado**
**Estado:** 📋 PENDIENTE (0/8 tareas)  
**Duración:** 2-3 días  
**Prioridad:** ⭐⭐ ALTA

**Documentación:**
- `fases/FASE_4_TESTING.md` - Suite de tests

**Entregables:**
- Tests unitarios (20+ tests)
- Tests de integración (4 tests E2E)
- Cobertura mínima del 75%
- Tests de conversión de divisas
- Tests de movimientos de inventario
- Tests de integración con proveedores

**Ruta:** `docs/sprints/SPRINT_6/fases/FASE_4_TESTING.md`

---

#### **FASE 5: Documentación Técnica**
**Estado:** 📋 PENDIENTE (0/4 tareas)  
**Duración:** 1-2 días  
**Prioridad:** ⭐ MEDIA

**Documentación:**
- `fases/FASE_5_DOCUMENTACION.md` - Manuales de usuario

**Entregables:**
- Manual de Multi-Divisa (400+ líneas)
- Manual de Inventario (600+ líneas)
- Manual de Proveedores (500+ líneas)
- Guía de Configuración de APIs de Tasas

**Ruta:** `docs/sprints/SPRINT_6/fases/FASE_5_DOCUMENTACION.md`

---

### 🧪 Testing

**Estado:** 📋 PENDIENTE (0/8 tareas)

**Cobertura Objetivo:**
- ✅ Tests unitarios: >20 tests (Multi-divisa, Inventario, Proveedores)
- ✅ Tests de integración: 4 tests E2E
- ✅ Cobertura de código: 75%+
- ✅ Tests de conversión de divisas con tasas históricas
- ✅ Tests de movimientos de inventario (kardex)
- ✅ Tests de cuentas por pagar

**Documentación:** Ver `fases/FASE_4_TESTING.md`

---

### 📚 Documentación de Usuario

**Estado:** 📋 PENDIENTE (0/4 manuales)

**Manuales:**
1. 📋 `MANUAL_MULTI_DIVISA.md` (400+ líneas)
2. 📋 `MANUAL_INVENTARIO.md` (600+ líneas)
3. 📋 `MANUAL_PROVEEDORES.md` (500+ líneas)
4. 📋 `GUIA_CONFIGURACION_API_TASAS.md` (300+ líneas)

**Total estimado:** ~1,800 líneas de documentación de usuario

---

## 📁 ESTRUCTURA DE ARCHIVOS

```
SPRINT_6/
├── CHECKLIST_SPRINT_6.md         (Checklist maestro)
├── RESUMEN_SPRINT_6.md            (Resumen ejecutivo)
├── SPRINT_6_PLAN_MAESTRO.md       (Plan detallado)
├── INDICE_SPRINT_6.md             (Este archivo)
├── README.md                       (Introducción al sprint)
│
├── fases/
│   ├── FASE_1_MULTI_DIVISA.md
│   ├── FASE_2_INVENTARIO.md
│   ├── FASE_3_PROVEEDORES.md
│   ├── FASE_4_TESTING.md
│   └── FASE_5_DOCUMENTACION.md
│
└── manuales/
    ├── MANUAL_MULTI_DIVISA.md
    ├── MANUAL_INVENTARIO.md
    ├── MANUAL_PROVEEDORES.md
    └── GUIA_CONFIGURACION_API_TASAS.md
```

---

## 🎯 OBJETIVOS DEL SPRINT

### Objetivo Principal
Expandir las capacidades comerciales del sistema mediante soporte multi-divisa para facturación internacional, implementar un sistema robusto de control de inventario, y establecer la gestión de proveedores con cuentas por pagar.

### Objetivos Específicos

1. **💱 Multi-Divisa:**
   - Soporte para múltiples monedas (USD, EUR, CRC, etc.)
   - Tipos de cambio históricos
   - Actualización automática de tasas (API externa)
   - Conversión automática en transacciones
   - Reportes consolidados multi-divisa
   - **Aprovechar `formatearMoneda()` existente**

2. **📦 Inventario Avanzado:**
   - Kardex detallado por producto
   - Gestión de lotes y fechas de vencimiento
   - Movimientos de entrada/salida
   - Alertas de stock mínimo y bajo
   - Ajustes de inventario
   - **Activar `PRODUCTO_AJUSTAR_INVENTARIO`**
   - **Implementar filtro `stockBajo` en reportes**

3. **🏭 Proveedores y Compras:**
   - Catálogo de proveedores completo
   - Órdenes de compra
   - Cuentas por pagar
   - Historial de pagos
   - Evaluación de proveedores

4. **🧪 Testing:**
   - Cobertura del 75%+
   - Tests de conversión de divisas
   - Tests de movimientos de inventario

5. **📚 Documentación:**
   - Manuales técnicos completos
   - Guías de configuración

---

## 📊 MÉTRICAS Y OBJETIVOS

### Métricas de Progreso

```
┌─────────────────────────────────────────────────────────────┐
│                 SPRINT 6 - MÉTRICAS OBJETIVO                 │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Total de Tareas:                     146 tareas            │
│  Duración Estimada:                   20-27 días            │
│  Velocidad Requerida:                 5-7 tareas/día        │
│                                                              │
│  Nuevas Tablas BD:                    6 tablas              │
│  Entidades Java:                      9 entidades           │
│  Services:                            6 servicios           │
│  Controllers:                         5 controllers         │
│  Templates HTML:                      12 vistas             │
│                                                              │
│  Tests Unitarios:                     20+ tests             │
│  Tests Integración:                   4 tests               │
│  Cobertura Objetivo:                  75%+                  │
│                                                              │
│  Líneas de Código (estimadas):       ~10,000 líneas        │
│  Líneas de Documentación:             ~2,500 líneas        │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Indicadores de Éxito

✅ **Multi-divisa operativo** - Conversión automática en transacciones  
✅ **Inventario funcional** - Kardex y alertas de stock activas  
✅ **Proveedores integrados** - Gestión completa de cuentas por pagar  
✅ **Testing > 75%** - Cobertura de código objetivo alcanzada  
✅ **Documentación completa** - Manuales disponibles  
✅ **Hallazgos del código resueltos** - `formatearMoneda()` integrado, `PRODUCTO_AJUSTAR_INVENTARIO` activo, filtro `stockBajo` implementado  

---

## 🔍 HALLAZGOS DEL CÓDIGO A INTEGRAR

### ✅ Código Existente a Aprovechar

#### 1. **Método `formatearMoneda()` ya existe**
**Ubicación:** Utilidad de formateo (a identificar)  
**Estado:** ✅ Implementado  
**Acción Sprint 6:**
- 🔧 Extender para soportar símbolo de múltiples divisas
- 🔧 Integrar con nuevo modelo `Moneda.java`
- 🔧 Usar en todos los reportes multi-divisa

**Tareas relacionadas:**
- FASE 1.2.5: Integrar `formatearMoneda()` con multi-divisa
- FASE 1.3.4: Actualizar reportes para usar formato multi-divisa

---

### ⚠️ Funcionalidad Existente No Utilizada

#### 2. **Enum `PRODUCTO_AJUSTAR_INVENTARIO` no se usa**
**Ubicación:** Modelo `Producto.java` o enums relacionados  
**Estado:** ⚠️ Definido pero no utilizado  
**Acción Sprint 6:**
- 🔧 Activar en formularios de ajuste de inventario
- 🔧 Vincular con `MovimientoInventario.java`
- 🔧 Validar en servicios de inventario

**Tareas relacionadas:**
- FASE 2.1.3: Activar enum `PRODUCTO_AJUSTAR_INVENTARIO`
- FASE 2.2.6: Integrar con flujo de ajustes de inventario
- FASE 2.3.2: Validar en `InventarioService`

**Valor:** Evita crear nueva funcionalidad, aprovecha código existente

---

#### 3. **Filtro `stockBajo` comentado como TODO en reportes**
**Ubicación:** Controllers o servicios de reportes  
**Estado:** ⚠️ TODO pendiente  
**Acción Sprint 6:**
- 🔧 Completar implementación del filtro
- 🔧 Crear endpoint `/reportes/stock-bajo`
- 🔧 Añadir parámetro de umbral configurable

**Tareas relacionadas:**
- FASE 2.4.1: Implementar filtro `stockBajo` en reportes
- FASE 2.4.2: Crear vista de productos con stock bajo
- FASE 2.4.3: Configurar umbral de stock mínimo por producto

**Valor:** Funcionalidad crítica para alertas de inventario

---

## 🔗 DEPENDENCIAS

### Dependencias Técnicas

**Nuevas dependencias Maven:**
- API de tasas de cambio (e.g., Exchange Rates API, Fixer.io)
- Spring Cache (para cachear tasas de cambio)
- Resilience4j (circuit breaker para API externa)

### Dependencias Externas

**APIs de Tasas de Cambio:**
- ⚠️ Clave API de Exchange Rates (gratuita hasta 1,000 req/mes)
- ⚠️ O API del Banco Central de Costa Rica (BCCR)
- ⚠️ Configuración de caché (Redis opcional)

### Dependencias de Sprints Anteriores

**Requiere completados:**
- ✅ Sprint 1-4: Productos y Clientes
- ✅ Sprint 5: Facturación y Contabilidad
- ✅ ConfiguracionEmpresa con moneda base

---

## ⚠️ RIESGOS Y MITIGACIONES

### Riesgos Identificados

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|---------|------------|
| API de tasas caída | Media | Medio | Caché de 24h, tasas manuales de respaldo |
| Complejidad conversión histórica | Alta | Medio | Validación exhaustiva, tests unitarios |
| Kardex con alto volumen de datos | Media | Medio | Paginación, índices en BD |
| Integración multi-divisa con FE CR | Baja | Alto | Convertir todo a CRC antes de facturar |
| Enum `AJUSTAR_INVENTARIO` con lógica no documentada | Alta | Bajo | Revisar código, documentar antes de usar |

---

## 📅 CRONOGRAMA ESTIMADO

```
Semana 1 (17-23 Feb):  FASE 1 - Multi-Divisa (Completa)
Semana 2 (24 Feb-2 Mar): FASE 2 - Inventario (Parte 1: Kardex y movimientos)
Semana 3 (3-9 Mar):    FASE 2 - Inventario (Parte 2: Alertas y ajustes)
Semana 4 (10-16 Mar):  FASE 3 - Proveedores + FASE 4 - Testing + FASE 5 - Docs
```

**Fecha límite:** 16 de marzo de 2026

---

## 🔄 SIGUIENTES PASOS

### Inmediatos (Antes de iniciar)
1. ✅ Revisar y aprobar ÍNDICE_SPRINT_6.md
2. 🔍 Identificar ubicación exacta de `formatearMoneda()`
3. 🔍 Localizar definición de enum `PRODUCTO_AJUSTAR_INVENTARIO`
4. 🔍 Encontrar código del filtro `stockBajo` comentado
5. 📋 Crear CHECKLIST_SPRINT_6.md detallado
6. 📋 Crear SPRINT_6_PLAN_MAESTRO.md

### Primera Fase
7. 🔧 Seleccionar API de tasas de cambio
8. 🔧 Configurar credenciales de API
9. 🚀 Iniciar FASE 1: Multi-Divisa

---

## 📚 REFERENCIAS

- [Clasificación Sprints Futuros](../CLASIFICACION_SPRINTS_FUTUROS.md)
- [Hallazgos del Código](../../refactorizacion/HALLAZGOS_CODIGO.md) *(si existe)*
- [Estado Proyecto](../../reportes/ESTADO_PROYECTO.md)
- [Sprint 5 - Contabilidad](../SPRINT_5/)

---

## 💡 NOTAS TÉCNICAS

### Integración con Hallazgos

**`formatearMoneda()` existente:**
```java
// Código actual (ejemplo):
String formatearMoneda(BigDecimal monto) {
    return "₡" + NumberFormat.getInstance().format(monto);
}

// Extensión Sprint 6:
String formatearMoneda(BigDecimal monto, Moneda moneda) {
    return moneda.getSimbolo() + " " + NumberFormat.getInstance().format(monto);
}
```

**Enum `PRODUCTO_AJUSTAR_INVENTARIO`:**
```java
// Ubicación: Producto.java o enum separado
// Valores posibles: SI, NO, AUTOMATICO (?)
// Sprint 6: Activar en formularios y validaciones
```

**Filtro `stockBajo`:**
```java
// TODO actual en ReporteController:
// @GetMapping("/stock-bajo")
// public String reporteStockBajo() { ... }

// Sprint 6: Implementar completamente
```

---

**Documento creado:** 16 de enero de 2026  
**Creado por:** GitHub Copilot  
**Versión:** 1.0  
**Estado:** 📋 PLANIFICADO
