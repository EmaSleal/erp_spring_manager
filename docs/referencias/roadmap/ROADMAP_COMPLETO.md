# 🚀 ROADMAP COMPLETO DEL PROYECTO
# WhatsApp Orders Manager - Sistema ERP

**Última actualización:** 20 de octubre de 2025  
**Estado del proyecto:** Sprint 2 COMPLETADO (100%)  
**Próximo Sprint:** Sprint 3 (En Planificación)

---

## 📊 ESTADO GENERAL

```
✅ SPRINT 1: COMPLETADO (87%) - Base del Sistema
✅ SPRINT 2: COMPLETADO (100%) - Usuarios, Roles, Notificaciones, Reportes, Optimizaciones
🎯 SPRINT 3: EN PLANIFICACIÓN - WhatsApp API, Dashboard Avanzado, Multi-Divisa
📋 SPRINT 4+: PLANIFICADO - Funcionalidades ERP Avanzadas
```

**Progreso Global:** 🟢🟢🟢🟢🟢🟢🟢⚪⚪⚪ 70% completado

---

## 📚 ÍNDICE

1. [Funcionalidades Implementadas (Sprint 1-2)](#funcionalidades-implementadas)
2. [Sprint 3: Próximo Sprint](#sprint-3-próximo-sprint)
3. [Sprints Futuros (4-10)](#sprints-futuros-4-10)
4. [Funcionalidades ERP Esenciales](#funcionalidades-erp-esenciales)
5. [Mejoras Técnicas Pendientes](#mejoras-técnicas-pendientes)
6. [Roadmap Visual](#roadmap-visual)

---

## ✅ FUNCIONALIDADES IMPLEMENTADAS

### Sprint 1 (87% completado)
- [x] Sistema de autenticación (login/registro)
- [x] Spring Security 6.x configurado
- [x] Dashboard principal funcional
- [x] Gestión de perfil de usuario
- [x] CRUD de Clientes
- [x] CRUD de Productos
- [x] CRUD de Facturas
- [x] Componentes reutilizables (navbar, sidebar)
- [x] Diseño responsive

### Sprint 2 (100% completado)
- [x] Gestión completa de usuarios (CRUD)
- [x] Sistema de roles (ADMIN, AGENTE, CONTADOR, VIEWER)
- [x] Control de acceso granular (36 configuraciones)
- [x] Sistema de notificaciones por email
- [x] Recordatorios automáticos de facturas
- [x] Sistema de reportes (PDF, Excel, CSV)
- [x] 10 índices de base de datos
- [x] 24 Stored Procedures
- [x] Paginación en 3 módulos
- [x] Sistema de caché (3 servicios)
- [x] Reducción del 62.5% en queries

**Total implementado:** ~22,000 líneas de código, 90+ archivos, 45+ endpoints

---

## 🎯 SPRINT 3: PRÓXIMO SPRINT

**Objetivo:** Integración WhatsApp + Dashboard Avanzado + Multi-Divisa  
**Duración estimada:** 12-15 días  
**Inicio previsto:** 22 de octubre de 2025  
**Fin previsto:** 5 de noviembre de 2025

### 🔥 Prioridad ALTA (Obligatorio)

#### 1. INTEGRACIÓN CON WHATSAPP API ⭐ OBJETIVO PRINCIPAL
**Prioridad:** CRÍTICA  
**Estimación:** 40-50 horas (5-7 días)

**Funcionalidades:**
- Configuración de WhatsApp Business API
- Webhooks para recibir mensajes
- Envío de mensajes automáticos
- Plantillas de mensajes predefinidas
- Notificaciones de facturas por WhatsApp
- Confirmación de pedidos por WhatsApp
- Estado de pagos por WhatsApp

**Componentes:**
```
Backend:
├── models/MensajeWhatsApp.java
├── services/WhatsAppService.java
├── controllers/WhatsAppController.java (webhooks)
├── config/WhatsAppConfig.java
└── templates/MensajePredefinido.java

Frontend:
├── templates/configuracion/whatsapp.html
├── templates/facturas/enviar-whatsapp.html
└── static/js/whatsapp.js

Base de Datos:
├── tabla: mensaje_whatsapp
├── tabla: plantilla_mensaje
└── tabla: configuracion_whatsapp
```

**Casos de Uso:**
1. Cliente solicita estado de factura → Bot responde automático
2. Factura generada → Envío automático de PDF por WhatsApp
3. Recordatorio de pago → Mensaje programado
4. Confirmación de pedido → Mensaje de confirmación

**Tecnologías:**
- Twilio WhatsApp API o Meta WhatsApp Business API
- Webhooks con Spring Boot
- WebClient para llamadas API
- Plantillas HTML para mensajes

---

#### 2. DASHBOARD AVANZADO CON GRÁFICAS REALES
**Prioridad:** ALTA  
**Estimación:** 16-20 horas (2-3 días)

**Funcionalidades:**
- Gráficas de ventas (Chart.js)
- Indicadores clave de rendimiento (KPIs)
- Estadísticas en tiempo real
- Comparativas mes a mes
- Top 5 productos más vendidos
- Top 5 clientes frecuentes
- Análisis de tendencias

**Componentes:**
```
Backend:
├── services/DashboardService.java
├── services/EstadisticasService.java
├── controllers/api/DashboardApiController.java
└── dto/DashboardDTO.java

Frontend:
├── templates/dashboard/dashboard-v2.html
├── static/js/dashboard-charts.js
├── static/js/dashboard-kpis.js
└── static/css/dashboard-v2.css

Base de Datos:
└── 8 Stored Procedures para estadísticas
```

**Gráficas a Implementar:**
1. **Ventas Mensuales** (Line Chart)
   - Últimos 12 meses
   - Comparativa con año anterior

2. **Estado de Facturas** (Pie Chart)
   - Pagadas: 65%
   - Pendientes: 25%
   - Vencidas: 10%

3. **Top Productos** (Bar Chart)
   - 5 productos más vendidos
   - Con cantidad y monto

4. **Clientes Activos** (Doughnut Chart)
   - Nuevos vs. recurrentes

5. **Ingresos vs. Gastos** (Mixed Chart)
   - Comparativa mensual

**KPIs del Dashboard:**
```
┌──────────────────────────────────────────────┐
│ 📊 VENTAS DEL MES                            │
│ MX$125,450.00          ↑ 15.3%              │
├──────────────────────────────────────────────┤
│ 📦 PRODUCTOS VENDIDOS                        │
│ 1,234 unidades         ↑ 8.7%               │
├──────────────────────────────────────────────┤
│ 👥 NUEVOS CLIENTES                           │
│ 28 clientes            ↑ 23.1%              │
├──────────────────────────────────────────────┤
│ 💰 TASA DE COBRO                             │
│ 87.5%                  ↑ 5.2%               │
└──────────────────────────────────────────────┘
```

---

#### 3. SISTEMA MULTI-DIVISA
**Prioridad:** ALTA  
**Estimación:** 38-52 horas (5-7 días)

**Funcionalidades:**
- Gestión de múltiples divisas (USD, MXN, EUR, GTQ, HNL)
- Definir divisa maestra para reportes
- Registro de tipos de cambio diarios
- Facturación en cualquier divisa
- Conversión automática a divisa maestra
- Reportes consolidados en divisa maestra
- Histórico de tipos de cambio

**Componentes:**
```
Backend:
├── models/Divisa.java
├── models/TipoCambio.java
├── services/DivisaService.java
├── services/TipoCambioService.java
├── controllers/DivisaController.java
└── controllers/TipoCambioController.java

Frontend:
├── templates/configuracion/divisas.html
├── templates/configuracion/tipos-cambio.html
├── templates/facturas/add-form.html (modificado)
└── static/js/divisas.js

Base de Datos:
├── tabla: divisa
├── tabla: tipo_cambio
├── ALTER TABLE factura (agregar campos divisa)
└── triggers para conversión automática
```

**Flujo de Facturación:**
1. Usuario selecciona divisa (ej: USD)
2. Sistema obtiene tipo de cambio del día
3. Factura se guarda en USD + conversión a MXN
4. Reportes se muestran en divisa maestra (MXN)

**Ver detalles completos:** [MEJORAS_FUTURAS.md - Sección Multi-Divisa](#)

---

### 🟡 Prioridad MEDIA (Recomendado)

#### 4. MEJORA DE AUTENTICACIÓN
**Estimación:** 8-11 horas (1-2 días)

**Cambios:**
- [x] Username en lugar de teléfono para login
- [x] Remember Me (mantener sesión 7 días)
- [x] Migrar Timestamp a LocalDateTime
- [x] Validaciones mejoradas

---

#### 5. EXPORTACIÓN AVANZADA DE REPORTES
**Estimación:** 12-16 horas (2 días)

**Funcionalidades:**
- Mejorar diseño de PDF
- Agregar logo de empresa
- Gráficas en reportes PDF
- Excel con múltiples hojas
- Filtros avanzados

---

### 🟢 Prioridad BAJA (Opcional)

#### 6. REFACTORIZACIONES TÉCNICAS
**Estimación:** 6-10 horas

- Separar SecurityConfig en módulos
- Crear DTOs para responses
- Mejorar manejo de errores
- Logging estructurado

---

### 📊 Distribución de Tiempo Sprint 3

| Funcionalidad | Horas | % Sprint |
|---------------|-------|----------|
| WhatsApp API | 40-50h | 45% |
| Dashboard Avanzado | 16-20h | 18% |
| Multi-Divisa | 38-52h | 35% |
| Otros (Auth, Reportes) | 20h | 2% |
| **TOTAL** | **114-142h** | **100%** |

**Duración:** 12-15 días laborales (considerando 8-10h/día de trabajo efectivo)

---

## 📋 SPRINTS FUTUROS (4-10)

### SPRINT 4: MÓDULO DE INVENTARIO AVANZADO
**Duración estimada:** 12-15 días

**Funcionalidades:**
- [x] Control de stock por almacén
- [x] Alertas de stock mínimo
- [x] Kardex de movimientos
- [x] Trazabilidad de lotes
- [x] Números de serie
- [x] Valorización de inventario (PEPS, Promedio)
- [x] Transferencias entre almacenes
- [x] Ajustes de inventario
- [x] Reportes de rotación

**Tablas nuevas:**
- `almacen`
- `movimiento_inventario`
- `lote`
- `numero_serie`
- `transferencia_almacen`

---

### SPRINT 5: MÓDULO DE PRODUCCIÓN
**Duración estimada:** 15-20 días

**Funcionalidades:**
- [x] Listas de materiales (BOM)
- [x] Órdenes de producción
- [x] Rutas de fabricación
- [x] Control de procesos
- [x] Costeo de producción
- [x] Planificación de materiales (MRP)
- [x] Centros de trabajo
- [x] Eficiencia de planta

**Tablas nuevas:**
- `lista_materiales`
- `orden_produccion`
- `ruta_fabricacion`
- `centro_trabajo`
- `movimiento_produccion`

---

### SPRINT 6: MÓDULO DE COMPRAS
**Duración estimada:** 12-15 días

**Funcionalidades:**
- [x] Gestión de proveedores
- [x] Cotizaciones de compra
- [x] Órdenes de compra
- [x] Recepciones de mercancía
- [x] Devoluciones a proveedores
- [x] Facturas de compra
- [x] Cuentas por pagar
- [x] Análisis de proveedores

**Tablas nuevas:**
- `proveedor`
- `cotizacion_compra`
- `orden_compra`
- `recepcion_mercancia`
- `factura_compra`
- `cuenta_por_pagar`

---

### SPRINT 7: MÓDULO DE CONTABILIDAD
**Duración estimada:** 20-25 días

**Funcionalidades:**
- [x] Plan de cuentas
- [x] Libro mayor
- [x] Asientos contables
- [x] Balance general
- [x] Estado de resultados
- [x] Flujo de caja
- [x] Conciliación bancaria
- [x] Impuestos (IVA, ISR)
- [x] Cierre contable

**Tablas nuevas:**
- `plan_cuentas`
- `asiento_contable`
- `movimiento_contable`
- `cuenta_bancaria`
- `conciliacion_bancaria`

---

### SPRINT 8: MÓDULO DE RECURSOS HUMANOS
**Duración estimada:** 15-20 días

**Funcionalidades:**
- [x] Gestión de empleados
- [x] Contratos
- [x] Asistencias
- [x] Vacaciones y permisos
- [x] Evaluaciones de desempeño
- [x] Capacitación
- [x] Organigramas
- [x] Expedientes digitales

**Tablas nuevas:**
- `empleado`
- `contrato`
- `asistencia`
- `vacacion`
- `permiso`
- `evaluacion_desempeno`

---

### SPRINT 9: MÓDULO DE NÓMINA
**Duración estimada:** 15-20 días

**Funcionalidades:**
- [x] Cálculo de nómina
- [x] Percepciones y deducciones
- [x] Impuestos sobre nómina
- [x] IMSS y INFONAVIT
- [x] Finiquitos
- [x] Recibos de nómina
- [x] Timbrado fiscal (CFDI)
- [x] Reportes legales

**Tablas nuevas:**
- `nomina`
- `concepto_nomina`
- `percepcion`
- `deduccion`
- `recibo_nomina`

---

### SPRINT 10: GESTIÓN DOCUMENTAL Y CRM
**Duración estimada:** 12-15 días

**Funcionalidades:**
- [x] Repositorio de documentos
- [x] Control de versiones
- [x] Flujos de aprobación
- [x] Búsqueda avanzada
- [x] CRM básico
- [x] Seguimiento de leads
- [x] Pipeline de ventas
- [x] Cotizaciones automatizadas

**Tablas nuevas:**
- `documento`
- `version_documento`
- `flujo_aprobacion`
- `lead`
- `oportunidad_venta`

---

## 🏢 FUNCIONALIDADES ERP ESENCIALES

### Módulo de Contabilidad ⭐
**Sprint sugerido:** Sprint 7  
**Prioridad:** ALTA

**Funciones básicas:**
- ✅ Control de cuentas por pagar (AP) y por cobrar (AR)
- ✅ Libro mayor general
- ✅ Balance general
- ✅ Estado de resultados
- ✅ Flujo de caja
- ✅ Conciliación bancaria
- ✅ Gestión de impuestos

**Procesos automatizados:**
- Facturación automática
- Pagos a proveedores
- Gestión de gastos
- Gestión de activos fijos
- Cierre de libros contables

**Finanzas integrales:**
- Contabilidad financiera y analítica
- Control de costos
- Gestión de cobros/pagos
- Tesorería (bancos, caja)
- Presupuestación

---

### Módulo de Ventas ⭐
**Sprint sugerido:** Mejoras en Sprint 4  
**Prioridad:** ALTA

**Gestión de clientes:**
- ✅ Registro de clientes (integrado con CRM)
- ✅ Cotizaciones/presupuestos
- ✅ Seguimiento de prospectos
- ✅ Ofertas formales

**Pedidos y despachos:**
- ✅ Control de pedidos desde entrada hasta entrega
- Órdenes de venta
- Reserva y asignación de inventario en tiempo real
- Albaranes/guías de despacho
- Facturación total o parcial
- Gestión de backorders (pedidos pendientes)

**Facturación y postventa:**
- ✅ Emisión de facturas de venta
- ✅ Notas de crédito
- ✅ Registro de pagos recibidos
- ✅ Trazabilidad del documento comercial
- Informes de ventas (por producto, cliente, vendedor)
- Gestión de devoluciones
- Servicio postventa

---

### Módulo de Inventario ⭐
**Sprint sugerido:** Sprint 4  
**Prioridad:** ALTA

**Control de existencias:**
- ✅ Administración centralizada del stock
- Múltiples almacenes/bodegas
- Catálogo de productos con categorías
- Visión general del inventario
- Ubicaciones de almacén
- Movimiento de mercancías

**Gestión de niveles de stock:**
- Planificación de inventarios
- Parámetros de reorden
- Alertas de reabastecimiento
- Control de fechas de caducidad (alimentos)
- Evitar sobreinventarios

**Operaciones y trazabilidad:**
- Registro de entradas y salidas
- Recepciones con órdenes de compra
- Despachos contra ventas
- Transferencias internas
- Manejo de lotes y números de serie
- Inventarios físicos y ajustes
- Metodologías de valoración (PEPS, costo promedio, estándar)
- Reportes: kardex, movimientos, niveles de stock valorizado

---

### Módulo de Producción
**Sprint sugerido:** Sprint 5  
**Prioridad:** MEDIA

**Planificación:**
- Planificación de la producción
- MRP (Material Requirements Planning)
- CRP (Capacity Requirements Planning)
- Secuenciación de carga de trabajo

**Órdenes de fabricación:**
- Creación y seguimiento de órdenes
- Fórmulas y listas de materiales (BOM)
- Rutas de producción
- Explosión de BOM
- Control de estaciones de trabajo
- Monitoreo de avance por fases
- Registro de lotes producidos

**Costeo y calidad:**
- Cálculo de costos en tiempo real
- Comparación costos estándar vs. reales
- Análisis de variaciones
- Estadísticas de eficiencia
- Integración con MES
- Gestión de calidad en producción
- Inspecciones y control de rechazo

---

### Módulo de Recursos Humanos
**Sprint sugerido:** Sprint 8  
**Prioridad:** MEDIA

**Administración de personal:**
- Registro maestro de empleados
- Datos personales y de contrato
- Puestos y departamentos
- Historial laboral
- Estructura organizativa
- Gestión de documentos

**Asistencia y ausencias:**
- Control de ausencias
- Bajas médicas
- Vacaciones
- Horas trabajadas
- Permisos solicitados
- Licencias por enfermedad
- Horas extra
- Portal de autoservicio

**Desarrollo de talento:**
- Reclutamiento y selección
- Seguimiento de candidatos
- Vacantes y contrataciones
- Evaluaciones de desempeño
- Planificación de ascensos
- Ajustes salariales
- Planes de capacitación
- Promoción interna

---

### Módulo de Nómina
**Sprint sugerido:** Sprint 9  
**Prioridad:** MEDIA

**Cálculo de nóminas:**
- Generación automatizada de nómina
- Cálculo de sueldos brutos y netos
- Conversión líquido a bruto
- Aplicación de fórmulas
- Cálculo de impuestos
- Deducciones legales

**Gestión de deducciones:**
- Retenciones (impuestos sobre la renta)
- Aportes de seguridad social/pensiones
- Anticipos de salario
- Préstamos a empleados
- Embargos judiciales
- Seguros de cesantía
- Tasas correspondientes
- Cumplimiento de obligaciones legales

**Emisión y cumplimiento:**
- Generación de recibos de nómina
- Envío electrónico según normas locales
- Registro automático en contabilidad
- Distribución por centro de costo
- Historial de pagos
- Calendarios de pago
- Tipos de nómina (semanal, quincenal, mensual)
- Manejo de finiquitos
- Depósito directo a bancos
- Reportes legales (libro de remuneraciones, declaraciones fiscales)

---

### Módulo de Gestión Documental
**Sprint sugerido:** Sprint 10  
**Prioridad:** BAJA

**Repositorio central:**
- Almacenamiento y organización de documentos
- Imágenes digitales
- Ubicación centralizada
- Acceso para empleados
- Adjuntar documentos a procesos del ERP
- Evitar manejo de papel
- Búsqueda fácil de información

**Control de versiones:**
- Registro de cambios
- Historial de modificaciones
- Quién y cuándo modificó
- Historial en contratos y especificaciones
- Flujos de trabajo (workflows)
- Revisión y aprobación de documentos
- Múltiples responsables

**Búsqueda y seguridad:**
- Búsqueda avanzada por metadatos
- Búsqueda por contenido
- Palabras clave
- Controles de acceso y permisos
- Protección de información sensible
- Solo personal autorizado
- Colaboración en tiempo real
- Edición colaborativa
- Comentarios
- Mensajería interna
- Integración con suites ofimáticas

---

## 🔧 MEJORAS TÉCNICAS PENDIENTES

### Alta Prioridad
- [ ] Username en lugar de teléfono (Sprint 3)
- [ ] Remember Me en login (Sprint 3)
- [ ] Migrar Timestamp a LocalDateTime (Sprint 3)
- [ ] Separar SecurityConfig en módulos (Sprint 3-4)
- [ ] Crear DTOs para responses (Sprint 3-4)

### Media Prioridad
- [ ] Internacionalización (i18n) - Español/Inglés (Sprint 5+)
- [ ] Historial de accesos (Sprint 5+)
- [ ] Logging estructurado (Sprint 4-5)
- [ ] Manejo de errores mejorado (Sprint 4)
- [ ] Validaciones centralizadas (Sprint 4)

### Baja Prioridad
- [ ] Two-Factor Authentication (2FA) (Sprint 6+)
- [ ] Social Login (OAuth2) (Sprint 7+)
- [ ] Notificaciones en tiempo real (WebSocket) (Sprint 5+)
- [ ] Modo offline (PWA) (Sprint 8+)
- [ ] API pública REST (Sprint 7+)

---

## 📈 ROADMAP VISUAL

```
2025
├── OCT [██████████] Sprint 1 COMPLETADO + Sprint 2 COMPLETADO
├── NOV [████░░░░░░] Sprint 3: WhatsApp API + Dashboard + Multi-Divisa
├── DIC [░░░░░░░░░░] Sprint 4: Inventario Avanzado

2026
├── ENE [░░░░░░░░░░] Sprint 4 continuación + Sprint 5: Producción
├── FEB [░░░░░░░░░░] Sprint 5 continuación
├── MAR [░░░░░░░░░░] Sprint 6: Módulo de Compras
├── ABR [░░░░░░░░░░] Sprint 7: Contabilidad (Parte 1)
├── MAY [░░░░░░░░░░] Sprint 7: Contabilidad (Parte 2)
├── JUN [░░░░░░░░░░] Sprint 8: Recursos Humanos
├── JUL [░░░░░░░░░░] Sprint 9: Nómina
├── AGO [░░░░░░░░░░] Sprint 10: Gestión Documental + CRM
├── SEP [░░░░░░░░░░] Refinamiento y Testing
├── OCT [░░░░░░░░░░] Preparación para Producción
├── NOV [░░░░░░░░░░] Despliegue v1.0
└── DIC [░░░░░░░░░░] Estabilización y Soporte
```

---

## 🎯 HITOS PRINCIPALES

### 2025 Q4
- ✅ **Octubre:** Sprint 1 + Sprint 2 completados
- 🎯 **Noviembre:** Sprint 3 - WhatsApp integrado
- 📋 **Diciembre:** Sprint 4 - Inventario completo

### 2026 Q1
- 📋 **Enero-Febrero:** Sprint 5 - Producción operativa
- 📋 **Marzo:** Sprint 6 - Módulo de compras funcional

### 2026 Q2
- 📋 **Abril-Mayo:** Sprint 7 - Contabilidad completa
- 📋 **Junio:** Sprint 8 - RRHH implementado

### 2026 Q3
- 📋 **Julio:** Sprint 9 - Nómina automatizada
- 📋 **Agosto:** Sprint 10 - Gestión documental y CRM
- 📋 **Septiembre:** Testing integral y correcciones

### 2026 Q4
- 📋 **Octubre:** Preparación para producción
- 📋 **Noviembre:** Despliegue v1.0 🚀
- 📋 **Diciembre:** Soporte post-lanzamiento

---

## 📊 MÉTRICAS OBJETIVO FINALES

### Funcionalidades
- **Módulos totales:** 12 módulos ERP
- **Endpoints API:** 200+ endpoints
- **Entidades de BD:** 60+ tablas
- **Líneas de código:** ~150,000+ líneas
- **Reportes:** 50+ tipos de reportes

### Rendimiento
- **Tiempo de respuesta:** < 200ms (promedio)
- **Consultas optimizadas:** Reducción del 80% vs. queries sin optimizar
- **Usuarios concurrentes:** 100+ usuarios simultáneos
- **Uptime:** 99.5% de disponibilidad

### Usuarios
- **Roles definidos:** 8+ roles de usuario
- **Permisos configurables:** 200+ permisos granulares
- **Idiomas soportados:** 2 (Español, Inglés)
- **Divisas soportadas:** 5+ divisas internacionales

---

## 📝 NOTAS IMPORTANTES

### Prioridades Flexibles
- El orden de los sprints 4-10 puede cambiar según necesidades del negocio
- Funcionalidades pueden moverse entre sprints
- Estimaciones de tiempo son aproximadas

### Dependencias
- Algunos módulos requieren otros completados primero
- ej: Nómina requiere RRHH
- ej: Producción requiere Inventario

### Recursos Necesarios
- Equipo de desarrollo: 2-3 desarrolladores full-stack
- QA/Testing: 1 tester
- Product Owner: 1 PO/PM
- Infraestructura: Servidor de producción, BD, APIs externas

### Tecnologías Base
- Backend: Spring Boot 3.3.4, Java 17
- Frontend: Thymeleaf, Bootstrap 5, Chart.js
- Base de Datos: MySQL 8.0
- Reportes: Apache PDFBox, Apache POI, OpenCSV
- Email: JavaMailSender (Gmail SMTP)
- WhatsApp: Twilio API o Meta WhatsApp Business API

---

## 🔗 DOCUMENTOS RELACIONADOS

- **Plan Maestro:** `PLAN_MAESTRO.txt`
- **Decisiones Técnicas:** `planificacion/decisiones/`
- **Sprint 1:** `sprints/SPRINT_1/`
- **Sprint 2:** `sprints/SPRINT_2/RESUMEN_SPRINT_2.md`
- **Sprint 3:** `sprints/SPRINT_3/` (en creación)
- **Funcionalidades ERP:** Este documento
- **Mejoras Futuras:** `planificacion/MEJORAS_FUTURAS.md` (DEPRECADO - ver este archivo)

---

**Última actualización:** 20 de octubre de 2025  
**Versión del roadmap:** 2.0  
**Estado:** 🟢 ACTUALIZADO Y CONSOLIDADO  
**Responsable:** GitHub Copilot + EmaSleal
