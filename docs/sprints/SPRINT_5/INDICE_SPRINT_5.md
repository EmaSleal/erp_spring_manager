# 📑 ÍNDICE - SPRINT 5: Contabilidad + Facturación Electrónica CR + Pagos

**Proyecto:** WhatsApp Orders Manager - ERP Spring Boot  
**Sprint:** 5  
**Fecha Inicio:** 17 de enero de 2026  
**Fecha Finalización:** 14 de febrero de 2026 (estimado)  
**Estado:** 🔄 EN PROGRESO  
**Progreso Total:** 113/199 tareas (56.8%)  
**Última Actualización:** 23 de enero de 2026 - 23:00
---

## 📚 ESTRUCTURA DE DOCUMENTACIÓN

### 📄 Documentos Principales

#### 1. **CHECKLIST_SPRINT_5.md**
**Descripción:** Checklist maestro con todas las tareas del Sprint 5  
**Estado:** 🔄 113/199 tareas (56.8%)  
**Contenido:**
- Progreso general (5 fases + testing + documentación)
- ✅ FASE 2 completada: Contabilidad (58/58 - 100%)
- 🔄 FASE 3 en progreso: Facturación Electrónica CR (55/58 - 95%)
- 📋 FASE 1 pendiente: Pagos (0/45)
- 📋 FASE 4 pendiente: Testing (0/32)
- 📋 FASE 5 pendiente: Documentación (0/12)
- Milestones críticos
- Métricas de rendimiento

**Ruta:** `docs/sprints/SPRINT_5/CHECKLIST_SPRINT_5.md`

---

#### 2. **RESUMEN_SPRINT_5.md**
**Descripción:** Resumen ejecutivo del Sprint 5  
**Contenido:**
- Objetivos del sprint
- Métricas en números
- Resumen de cada fase
- Archivos a crear/modificar
- Próximos pasos

**Ruta:** `docs/sprints/SPRINT_5/RESUMEN_SPRINT_5.md`

---

#### 3. **SPRINT_5_PLAN_MAESTRO.md**
**Descripción:** Plan detallado de ejecución del Sprint 5  
**Contenido:**
- Análisis de situación actual
- Objetivos y alcance
- Priorización de fases
- Análisis de riesgos
- Estrategia de implementación

**Ruta:** `docs/sprints/SPRINT_5/SPRINT_5_PLAN_MAESTRO.md`

---

### 📦 Documentación por Fases

#### **FASE 1: Módulo de Pagos (Base Financiera)**
**Estado:** 📋 PENDIENTE (0/45 tareas)  
**Duración:** 5-7 días  
**Prioridad:** ⭐⭐⭐ CRÍTICA

**Documentación:**
- `fases/FASE_1_PAGOS.md` - Implementación completa

**Entregables:**
- Modelo `Pago.java` con validaciones
- Gestión de métodos de pago (catálogo Hacienda CR)
- Estado de cuenta por cliente
- Conciliación básica
- Integración con Factura y Contabilidad

**Ruta:** `docs/sprints/SPRINT_5/fases/FASE_1_PAGOS.md`

---

#### **FASE 2: Contabilidad (Doble Partida)**
**Estado:** ✅ COMPLETADA (58/58 tareas - 100%)  
**Duración:** 7-9 días  
**Prioridad:** ⭐⭐⭐ CRÍTICA  
**Fecha Completada:** 18 de enero de 2026

**Documentación:**
- `fases/FASE_2_CONTABILIDAD.md` - Sistema completo

**Entregables Completados:**
- ✅ Plan de cuentas adaptado a Costa Rica (5 niveles jerárquicos)
- ✅ Asientos contables (manual y automático)
- ✅ Libro diario y mayor
- ✅ Balance de comprobación y general
- ✅ Estado de resultados
- ✅ Integración con Pagos y Facturas
- ✅ Catálogo de cuentas jerárquico
- ✅ 3 Enums (TipoCuenta, NaturalezaCuenta, EstadoAsiento, TipoAsiento)
- ✅ 4 Entidades JPA (CuentaContable, AsientoContable, DetalleAsiento, PeriodoContable)
- ✅ 4 Repositories
- ✅ 4 Services (CuentaService, AsientoService, CalculoService, ReporteService)
- ✅ 3 Controllers (CuentaController, AsientoController, ReporteController)
- ✅ 4 DTOs + 3 Mappers
- ✅ MonedaService para manejo centralizado de símbolos de moneda
- ✅ 10 Vistas Thymeleaf (cuentas: listar, form, detalle + asientos: listar, form, detalle + reportes: index, libro-mayor, balance-comprobacion, balance-general, estado-resultados)
- ✅ 2 Archivos JavaScript (cuentas.js 450 líneas, asientos.js 585 líneas, reportes.js 135 líneas)
- ✅ Validación de balance de doble partida en tiempo real
- ✅ Árbol de cuentas interactivo
- ✅ Sistema completo de reportes financieros

**Ruta:** `docs/sprints/SPRINT_5/fases/FASE_2_CONTABILIDAD.md`

---

#### **FASE 3: Facturación Electrónica Costa Rica v4.4**
**Estado:** 🔄 EN PROGRESO (55/58 tareas - 95%)  
**Duración:** 6-8 días  
**Prioridad:** ⭐⭐⭐ CRÍTICA  
**Fecha Inicio:** 23 de enero de 2026  
**Última Actualización:** 23 de enero de 2026 - 23:00

**Documentación:**
- `fases/FASE_3_FACTURACION_ELECTRONICA_CR.md` - Integración completa

**Entregables Completados:**
- ✅ **Sección 1: Configuración y Setup (10/10 - 100%)**
  - 3 Entidades JPA completas con validaciones
  - 4 Enums bien definidos
  - 3 Repositories + 9 Stored Procedures
  - 5 Service interfaces
  - 2 Controllers REST + 2 Vistas HTML
  
- ✅ **Sección 2: Generación de XML (8/8 - 100%)**
  - XmlGeneratorServiceImpl completo
  - ClaveNumericaGenerator (50 dígitos)
  - XmlValidator con validación XSD
  - Soporte: Factura, Tiquete, NC, ND
  - Directorio XSD + instrucciones descarga

- ✅ **Sección 3: Firma Digital (6/6 - 100%)**
  - FirmaDigitalServiceImpl completo
  - Firma XMLSignature con RSA-SHA256
  - Validación de firma digital
  - Gestión de certificados .p12
  - Verificación de vigencia

- ✅ **Sección 4: Integración API Hacienda (12/12 - 100%)**
  - HaciendaApiServiceImpl completo (397 líneas)
  - OAuth2 con grant_type password y refresh_token
  - Cache de tokens con @Cacheable
  - enviarComprobante() con POST JSON + Base64 XML
  - consultarComprobante() con GET
  - consultarMensajes() con parseo JSON
  - HttpClientConfig con RestTemplate (timeouts: 10s/30s)
  - Enum AmbienteHacienda actualizado con urlToken
  - Enum MensajeHacienda con PROCESANDO y ERROR
  - Manejo completo de errores HTTP

**Pendientes:**
- ⏳ Sección 5: Gestión Respuestas (6/10 - 60%)
- ⏳ Circuit Breaker con Resilience4j (pendiente)
- ⏳ Job de reintentos programado (pendiente)

**Ruta:** `docs/sprints/SPRINT_5/fases/FASE_3_FACTURACION_ELECTRONICA_CR.md`

---

#### **FASE 4: Testing Automatizado**
**Estado:** 📋 PENDIENTE (0/32 tareas)  
**Duración:** 4-5 días  
**Prioridad:** ⭐⭐ ALTA

**Documentación:**
- `fases/FASE_4_TESTING.md` - Suite completa de tests

**Entregables:**
- Tests unitarios (JUnit 5 + Mockito)
- Tests de integración (TestContainers)
- Cobertura mínima del 80%
- CI/CD con GitHub Actions
- Tests de servicios críticos
- Tests de integración con Hacienda (Sandbox)

**Ruta:** `docs/sprints/SPRINT_5/fases/FASE_4_TESTING.md`

---

#### **FASE 5: Documentación y Manuales**
**Estado:** 📋 PENDIENTE (0/12 tareas)  
**Duración:** 2-3 días  
**Prioridad:** ⭐⭐ MEDIA

**Documentación:**
- `fases/FASE_5_DOCUMENTACION.md` - Documentación técnica y manuales

**Entregables:**
- Manual de Pagos (500+ líneas)
- Manual de Contabilidad (800+ líneas)
- Manual de Facturación Electrónica CR (1,000+ líneas)
- Guía de Configuración Hacienda
- Guía de Obtención de Certificado de Firma
- FAQ de Facturación Electrónica

**Ruta:** `docs/sprints/SPRINT_5/fases/FASE_5_DOCUMENTACION.md`

---

### 🧪 Testing

**Estado:** 📋 PENDIENTE (0/32 tareas)

**Cobertura Objetivo:**
- ✅ Tests unitarios: >30 tests (Pagos, Contabilidad, FE)
- ✅ Tests de integración: 6 tests E2E
- ✅ Cobertura de código: 80%+
- ✅ Integration tests con TestContainers (MySQL)
- ✅ Tests de integración con Hacienda Sandbox

**Documentación:** Ver `fases/FASE_4_TESTING.md`

---

### 📚 Documentación de Usuario

**Estado:** 📋 PENDIENTE (0/6 manuales)

**Manuales:**
1. 📋 `MANUAL_PAGOS.md` (500+ líneas)
2. 📋 `MANUAL_CONTABILIDAD.md` (800+ líneas)
3. 📋 `MANUAL_FACTURACION_ELECTRONICA_CR.md` (1,000+ líneas)
4. 📋 `GUIA_CONFIGURACION_HACIENDA.md` (400+ líneas)
5. 📋 `GUIA_OBTENCION_CERTIFICADO_FIRMA.md` (300+ líneas)
6. 📋 `FAQ_FACTURACION_ELECTRONICA.md` (400+ líneas)

**Total estimado:** ~3,400 líneas de documentación de usuario

---

## 📁 ESTRUCTURA DE ARCHIVOS

```
SPRINT_5/
├── CHECKLIST_SPRINT_5.md         (Checklist maestro)
├── RESUMEN_SPRINT_5.md            (Resumen ejecutivo)
├── SPRINT_5_PLAN_MAESTRO.md       (Plan detallado)
├── INDICE_SPRINT_5.md             (Este archivo)
├── README.md                       (Introducción al sprint)
│
├── fases/
│   ├── FASE_1_PAGOS.md
│   ├── FASE_2_CONTABILIDAD.md
│   ├── FASE_3_FACTURACION_ELECTRONICA_CR.md
│   ├── FASE_4_TESTING.md
│   └── FASE_5_DOCUMENTACION.md
│
└── manuales/
    ├── MANUAL_PAGOS.md
    ├── MANUAL_CONTABILIDAD.md
    ├── MANUAL_FACTURACION_ELECTRONICA_CR.md
    ├── GUIA_CONFIGURACION_HACIENDA.md
    ├── GUIA_OBTENCION_CERTIFICADO_FIRMA.md
    └── FAQ_FACTURACION_ELECTRONICA.md
```

---

## 🎯 OBJETIVOS DEL SPRINT

### Objetivo Principal
Implementar un sistema financiero y contable completo, cumpliendo con la normativa fiscal de Costa Rica mediante la facturación electrónica v4.4, y estableciendo una base sólida de testing automatizado.

### Objetivos Específicos

1. **💰 Sistema de Pagos:**
   - Gestión completa de pagos (parciales/totales)
   - Soporte para 8 métodos de pago (catálogo Hacienda CR)
   - Estado de cuenta por cliente
   - Conciliación básica

2. **📊 Contabilidad de Doble Partida:**
   - Plan de cuentas adaptado a Costa Rica
   - Asientos automáticos desde facturas y pagos
   - Libros contables (diario, mayor, balance)
   - Reportes financieros

3. **🇨🇷 Facturación Electrónica CR:**
   - Cumplimiento con anexo v4.4 de Hacienda
   - Firma digital XAdES-EPES
   - Integración completa con API de Hacienda
   - Gestión de respuestas y reintentos

4. **🧪 Testing Automatizado:**
   - Cobertura del 80%+
   - Tests unitarios e integración
   - CI/CD con GitHub Actions

5. **📚 Documentación Completa:**
   - Manuales técnicos y de usuario
   - Guías de configuración
   - FAQs

---

## 📊 MÉTRICAS Y OBJETIVOS

### Métricas de Progreso

```
┌─────────────────────────────────────────────────────────────┐
│                 SPRINT 5 - MÉTRICAS OBJETIVO                 │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Total de Tareas:                     199 tareas            │
│  Duración Estimada:                   24-32 días            │
│  Velocidad Requerida:                 6-8 tareas/día        │
│                                                              │
│  Nuevas Tablas BD:                    8 tablas              │
│  Entidades Java:                      12 entidades          │
│  Services:                            8 servicios           │
│  Controllers:                         6 controllers         │
│  Templates HTML:                      15 vistas             │
│                                                              │
│  Tests Unitarios:                     30+ tests             │
│  Tests Integración:                   6 tests               │
│  Cobertura Objetivo:                  80%+                  │
│                                                              │
│  Líneas de Código (estimadas):       ~15,000 líneas        │
│  Líneas de Documentación:             ~4,500 líneas        │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Indicadores de Éxito

✅ **Sistema de Pagos operativo** - Registro y conciliación de pagos  
✅ **Contabilidad funcional** - Asientos automáticos y libros contables  
✅ **Facturación electrónica CR** - Envío exitoso a Hacienda Sandbox  
✅ **Testing > 80%** - Cobertura de código objetivo alcanzada  
✅ **Documentación completa** - Manuales y guías disponibles  
✅ **0 bugs críticos** - Sistema estable para producción  

---

## 🔗 DEPENDENCIAS

### Dependencias Técnicas

**Nuevas dependencias Maven:**
- XmlSec (firma digital)
- JAXB (generación XML)
- Resilience4j (circuit breaker)
- TestContainers (testing)
- JUnit 5 Jupiter
- Mockito

### Dependencias Externas

**Requisitos para Facturación Electrónica CR:**
- ⚠️ Certificado de firma digital (BCCR)
- ⚠️ Credenciales de ATV (Hacienda)
- ⚠️ Acceso a API Sandbox de Hacienda
- ⚠️ Configuración de OIDC/OAuth2

### Dependencias de Sprints Anteriores

**Requiere completados:**
- ✅ Sprint 1-4: Facturación básica
- ✅ Cliente con datos completos
- ✅ Producto con precios
- ✅ ConfiguracionEmpresa (Sprint 4)

---

## ⚠️ RIESGOS Y MITIGACIONES

### Riesgos Identificados

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|---------|------------|
| Certificado de firma no disponible | Media | Alto | Trabajar con Sandbox sin firma temporal |
| Complejidad del XML v4.4 | Alta | Medio | Usar ejemplos de Hacienda, validar XSD |
| API de Hacienda caída | Baja | Alto | Circuit breaker, cola de reintentos |
| Testing complejo de FE | Alta | Medio | Mocks extensivos, Sandbox dedicado |
| Tiempo insuficiente | Media | Alto | Priorizar Fase 1 y 2, FE puede ser Sprint 6 |

---

## 📅 CRONOGRAMA ESTIMADO

```
Semana 1 (17-23 Ene):  FASE 1 - Pagos (Completa)
Semana 2 (24-30 Ene):  FASE 2 - Contabilidad (Parte 1)
Semana 3 (31 Ene-6 Feb): FASE 2 - Contabilidad (Parte 2) + FASE 3 inicio
Semana 4 (7-13 Feb):   FASE 3 - FE CR (Completa)
Semana 5 (14-16 Feb):  FASE 4 - Testing + FASE 5 - Documentación
```

**Fecha límite:** 14 de febrero de 2026

---

## 🔄 SIGUIENTES PASOS

1. ✅ Revisar y aprobar ÍNDICE_SPRINT_5.md
2. 📋 Crear CHECKLIST_SPRINT_5.md detallado
3. 📋 Crear SPRINT_5_PLAN_MAESTRO.md
4. 📋 Crear documentos de fases individuales
5. 📋 Configurar entorno de Hacienda Sandbox
6. 📋 Obtener certificado de firma (o mock)
7. 🚀 Iniciar FASE 1: Módulo de Pagos

---

## 📚 REFERENCIAS

- [Propuesta Sprint 5](../PROPUESTA_SPRINT_5.md)
- [Clasificación Sprints Futuros](../CLASIFICACION_SPRINTS_FUTUROS.md)
- [Documentación Hacienda CR](../../referencias/facturacion%20cr.txt)
- [Estado Proyecto](../../reportes/ESTADO_PROYECTO.md)
- [Mejoras Futuras](../../planificacion/MEJORAS_FUTURAS.md)

---

**Documento creado:** 16 de enero de 2026  
**Creado por:** GitHub Copilot  
**Versión:** 1.0  
**Estado:** 📋 PLANIFICADO
