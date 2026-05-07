## 🔧 SPRINT 7: PRODUCCIÓN + MEJORAS SISTEMA + SEGURIDAD

**Duración estimada:** 18-24 días  
**Prioridad:** ⭐⭐ MEDIA-ALTA  
**Dependencias:** Sprint 6 (Inventario)

### 🎯 Objetivos Estratégicos
Módulo de producción básico + mejoras técnicas del sistema + seguridad avanzada.

---

### FASE 7.1: Módulo de Producción (NUEVA)

**Duración:** 8-10 días | **Tareas:** 52 tareas  
**Prioridad:** ⭐⭐ MEDIA

#### Aplicabilidad
- ⚠️ **Solo para empresas manufactureras**
- ✅ Puede omitirse si solo es comercio/servicios

#### Tareas Principales (52)

**Base de Datos (10 tareas)**
- [ ] Crear tabla `producto_compuesto` (productos que se fabrican)
- [ ] Crear tabla `formula_produccion` (BOM - Bill of Materials)
- [ ] Crear tabla `linea_formula` (componentes del producto)
- [ ] Crear tabla `orden_produccion` (órdenes de fabricación)
- [ ] Crear tabla `proceso_produccion` (etapas del proceso)
- [ ] Crear tabla `consumo_materiales` (materiales usados)
- [ ] Crear tabla `produccion_terminada` (productos fabricados)
- [ ] SP: `sp_explosionar_formula(id_producto, cantidad)`
- [ ] SP: `sp_costo_produccion(id_orden)`
- [ ] Trigger: Descontar materiales al producir

**Backend (16 tareas)**
- [ ] Entidad `ProductoCompuesto.java`
- [ ] Entidad `FormulaProduccion.java`
- [ ] Entidad `LineaFormula.java`
- [ ] Entidad `OrdenProduccion.java`
- [ ] Enum `EstadoOrden` (PLANIFICADA, EN_PROCESO, FINALIZADA, CANCELADA)
- [ ] `FormulaRepository` + queries
- [ ] `OrdenProduccionRepository` + queries
- [ ] `FormulaService` + Impl (CRUD fórmulas, explosión BOM)
- [ ] `OrdenProduccionService` + Impl (planificar, ejecutar)
- [ ] `ProduccionService` + Impl (consumir materiales, producir)
- [ ] Integrar con Inventario (descontar materiales, agregar producto terminado)
- [ ] Integrar con Contabilidad (costeo de producción)
- [ ] `ProduccionController` (vistas)
- [ ] `ProduccionRestController` (API)
- [ ] Cálculo de costos estándar vs reales
- [ ] Reportes de eficiencia de producción

**Frontend (10 tareas)**
- [ ] Vista: `produccion/formulas.html` (CRUD fórmulas/BOM)
- [ ] Vista: `produccion/ordenes.html` (lista de órdenes)
- [ ] Vista: `produccion/crear-orden.html`
- [ ] Vista: `produccion/ejecutar-orden.html` (registrar producción)
- [ ] Vista: `produccion/reportes/costos.html`
- [ ] Vista: `produccion/reportes/eficiencia.html`
- [ ] JavaScript: `formulas.js`
- [ ] JavaScript: `ordenes-produccion.js`
- [ ] Dashboard: Widget de órdenes activas
- [ ] Calculadora de costos de producción

**Integraciones (8 tareas)**
- [ ] Validar disponibilidad de materiales antes de producir
- [ ] Actualizar stock de materias primas (salida)
- [ ] Actualizar stock de producto terminado (entrada)
- [ ] Generar asiento contable de producción
- [ ] Reportes: Costo de producción por orden
- [ ] Reportes: Materiales más usados
- [ ] Exportar órdenes a PDF
- [ ] Notificaciones: Orden finalizada

**Testing (6 tareas)**
- [ ] `FormulaServiceTest` - Explosión de BOM
- [ ] `OrdenProduccionServiceTest`
- [ ] Test: Consumir materiales correctamente
- [ ] Test: Generar producto terminado
- [ ] Test: Cálculo de costos
- [ ] Test: Stock insuficiente impide producción

**Documentación (2 tareas)**
- [ ] `SPRINT_7_FASE_1_PRODUCCION.md`
- [ ] `MANUAL_GESTION_PRODUCCION.md`

---

### FASE 7.2: Mejoras Técnicas del Sistema (IMPORTANTE)

**Duración:** 6-8 días | **Tareas:** 38 tareas  
**Prioridad:** ⭐⭐⭐ ALTA

#### Estado Actual Detectado (MEJORAS_FUTURAS.md)
- ⚠️ Username es teléfono (debería ser username independiente)
- ⚠️ Uso de `Timestamp` (debería ser `LocalDateTime`)
- ⚠️ Falta "Remember Me" en login
- ⚠️ Sin auditoría completa de cambios

#### Tareas Principales (38)

**Migración Username (13 tareas)**
- [ ] Alterar tabla `usuario` (agregar campo username UNIQUE)
- [ ] Migración de datos (generar usernames temporales)
- [ ] Actualizar `Usuario.java` (agregar campo username)
- [ ] Actualizar `UsuarioRepository` (findByUsername, existsByUsername)
- [ ] Actualizar `UserDetailsServiceImpl` (usar username en lugar de teléfono)
- [ ] Actualizar `SecurityConfig` (usernameParameter = "username")
- [ ] Actualizar `auth/login.html` (campo username)
- [ ] Actualizar `auth/register.html` (campo username + validación)
- [ ] Actualizar `perfil/ver.html` (mostrar username)
- [ ] Actualizar `perfil/editar.html` (editar username)
- [ ] JavaScript: Validar username en tiempo real
- [ ] Testing: Login con username
- [ ] Documentación: Guía de migración para usuarios

**Migración LocalDateTime (8 tareas)**
- [ ] Actualizar `Usuario.java` (Timestamp → LocalDateTime)
- [ ] Actualizar `Factura.java` (si aplica)
- [ ] Actualizar todas las entidades con fechas
- [ ] Actualizar templates (usar #temporals en lugar de #dates)
- [ ] Actualizar servicios (usar LocalDateTime.now())
- [ ] Testing: Fechas se guardan correctamente
- [ ] Verificar compatibilidad con MySQL
- [ ] Documentación de cambio

**Remember Me (4 tareas)**
- [ ] Configurar en `SecurityConfig` (rememberMe + token validity)
- [ ] Crear tabla `persistent_logins` para tokens
- [ ] Agregar checkbox en `auth/login.html`
- [ ] Testing: Remember me funciona

**Auditoría Avanzada (8 tareas)**
- [ ] Crear tabla `auditoria_cambios` (entidad, acción, usuario, antes, después)
- [ ] Crear `AuditoriaService` (registrar cambios)
- [ ] Aspect: Interceptar cambios automáticamente (@Auditable)
- [ ] Vista: `admin/auditoria.html` (consultar cambios)
- [ ] Filtros: Por entidad, usuario, fecha
- [ ] Exportar auditoría a Excel
- [ ] Testing: Auditoría se registra
- [ ] Documentación

**Otras Mejoras (5 tareas)**
- [ ] Rate limiting en endpoints críticos
- [ ] Configuración de CORS adecuada
- [ ] Compresión de respuestas HTTP (Gzip)
- [ ] Caché de recursos estáticos
- [ ] Documentación API con OpenAPI/Swagger

---

### FASE 7.3: Seguridad Avanzada (CRÍTICA)

**Duración:** 4-6 días | **Tareas:** 28 tareas  
**Prioridad:** ⭐⭐⭐ ALTA

#### Tareas Principales (28)

**Autenticación 2FA (10 tareas)**
- [ ] Agregar campo `secret_2fa` en tabla usuario
- [ ] Agregar dependencia Google Authenticator
- [ ] Servicio: Generar QR code para 2FA
- [ ] Servicio: Validar código 2FA
- [ ] Vista: Configurar 2FA en perfil
- [ ] Vista: Login con 2FA
- [ ] Permitir códigos de backup
- [ ] Testing: 2FA completo
- [ ] Documentación para usuarios
- [ ] Notificación: 2FA activado/desactivado

**Bloqueo de Cuenta (8 tareas)**
- [ ] Tabla: `intentos_login_fallidos`
- [ ] Servicio: Contar intentos fallidos
- [ ] Bloquear cuenta después de 5 intentos
- [ ] Desbloqueo automático después de 30 minutos
- [ ] Desbloqueo manual por admin
- [ ] Notificación: Cuenta bloqueada
- [ ] Vista: Gestión de cuentas bloqueadas (admin)
- [ ] Testing

**Tokens de Sesión (6 tareas)**
- [ ] Migrar a JWT para API REST
- [ ] Token de refresh
- [ ] Expiración configurable
- [ ] Revocar tokens
- [ ] Vista: Sesiones activas por usuario
- [ ] Testing

**Otras Seguridades (4 tareas)**
- [ ] Content Security Policy (CSP) headers
- [ ] HTTPS obligatorio en producción
- [ ] Sanitización de inputs (prevenir XSS)
- [ ] SQL Injection protection review

---

### 📊 Resumen Sprint 7

```
┌─────────────────────────────────────────────────────────────┐
│                    SPRINT 7 - RESUMEN                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  FASE 7.1: Producción (opcional) [52 tareas]  8-10 días ⭐⭐ │
│  FASE 7.2: Mejoras Técnicas       [38 tareas]  6-8 días ⭐⭐⭐│
│  FASE 7.3: Seguridad Avanzada     [28 tareas]  4-6 días ⭐⭐⭐│
│  Testing + Documentación          [10 tareas]  2-3 días ⭐⭐ │
│                                                              │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│  TOTAL SPRINT 7                   [128 tareas] 20-27 días   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

**Nota:** Producción puede omitirse si no aplica, reduciendo a 76 tareas / 12-17 días

---

