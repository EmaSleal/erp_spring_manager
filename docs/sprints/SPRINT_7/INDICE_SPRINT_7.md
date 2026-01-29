# 📑 ÍNDICE - SPRINT 7: Producción + Mejoras Técnicas + Seguridad

**Proyecto:** WhatsApp Orders Manager - ERP Spring Boot  
**Sprint:** 7  
**Fecha Inicio:** 17 de marzo de 2026  
**Fecha Finalización:** 13 de abril de 2026 (estimado)  
**Estado:** 📋 PLANIFICADO

---

## 📚 ESTRUCTURA DE DOCUMENTACIÓN

### 📄 Documentos Principales

#### 1. **CHECKLIST_SPRINT_7.md**
**Descripción:** Checklist maestro con todas las tareas del Sprint 7  
**Estado:** 📋 0/128 tareas (0% - versión completa) o 0/76 tareas (versión sin producción)  
**Contenido:**
- Progreso general (3 fases + testing + documentación)
- Checklist detallado por fase
- Estado de cada tarea
- Milestones críticos
- Métricas de rendimiento

**Ruta:** `docs/sprints/SPRINT_7/CHECKLIST_SPRINT_7.md`

---

#### 2. **RESUMEN_SPRINT_7.md**
**Descripción:** Resumen ejecutivo del Sprint 7  
**Contenido:**
- Objetivos del sprint (con/sin producción)
- Métricas en números
- Resumen de cada fase
- Archivos a crear/modificar
- Próximos pasos

**Ruta:** `docs/sprints/SPRINT_7/RESUMEN_SPRINT_7.md`

---

#### 3. **SPRINT_7_PLAN_MAESTRO.md**
**Descripción:** Plan detallado de ejecución del Sprint 7  
**Contenido:**
- Análisis de situación actual
- Objetivos y alcance (producción opcional)
- Priorización de fases
- Análisis de riesgos
- Estrategia de implementación
- **Integración con hallazgos técnicos del código**

**Ruta:** `docs/sprints/SPRINT_7/SPRINT_7_PLAN_MAESTRO.md`

---

### 📦 Documentación por Fases

#### **FASE 1: Módulo de Producción (OPCIONAL)**
**Estado:** 📋 PENDIENTE (0/52 tareas)  
**Duración:** 8-10 días  
**Prioridad:** ⭐ BAJA (solo para empresas manufactureras)

**Documentación:**
- `fases/FASE_1_PRODUCCION.md` - Sistema completo de manufactura

**Entregables:**
- Modelo `OrdenProduccion.java`
- Modelo `RecetaProduccion.java` (BOM - Bill of Materials)
- Modelo `ProcesoProduccion.java`
- Gestión de materias primas
- Costeo de producción
- Ordenes de trabajo
- Consumo de materiales
- Productos terminados

**Nota:** ⚠️ Esta fase es **OPCIONAL** - Solo implementar si el negocio lo requiere

**Ruta:** `docs/sprints/SPRINT_7/fases/FASE_1_PRODUCCION.md`

---

#### **FASE 2: Mejoras Técnicas (CRÍTICO)**
**Estado:** 📋 PENDIENTE (0/38 tareas)  
**Duración:** 6-8 días  
**Prioridad:** ⭐⭐⭐ CRÍTICA

**Documentación:**
- `fases/FASE_2_MEJORAS_TECNICAS.md` - Refactoring y optimizaciones

**Entregables:**
- **Migración: Username → Email/Usuario real** ⚠️
- **Migración: Timestamp → LocalDateTime** ⚠️
- Implementar "Remember Me" en login ⚠️
- Sistema de auditoría completo ⚠️
- Optimización de queries (N+1)
- Caché de segundo nivel (Redis)
- Paginación mejorada
- Validaciones exhaustivas
- Manejo de excepciones centralizado

**Hallazgos aplicados:**
- ⚠️ **CRÍTICO:** Cambiar username (actualmente teléfono) a email o usuario real
- ⚠️ **CRÍTICO:** Migrar Timestamp a LocalDateTime en toda la app
- ⚠️ Implementar "Remember Me" en formulario de login
- ⚠️ Completar sistema de auditoría (actualmente parcial)

**Ruta:** `docs/sprints/SPRINT_7/fases/FASE_2_MEJORAS_TECNICAS.md`

---

#### **FASE 3: Seguridad Avanzada (CRÍTICO)**
**Estado:** 📋 PENDIENTE (0/28 tareas)  
**Duración:** 5-7 días  
**Prioridad:** ⭐⭐⭐ CRÍTICA

**Documentación:**
- `fases/FASE_3_SEGURIDAD.md` - Hardening de seguridad

**Entregables:**
- Autenticación de dos factores (2FA/MFA)
- JWT con refresh tokens
- Bloqueo de cuenta por intentos fallidos
- Políticas de contraseñas robustas
- Encriptación de datos sensibles
- Rate limiting (prevención DDoS)
- CORS configurado
- Headers de seguridad (CSP, X-Frame-Options)
- Sanitización de inputs (XSS, SQL Injection)
- Logging de seguridad

**Ruta:** `docs/sprints/SPRINT_7/fases/FASE_3_SEGURIDAD.md`

---

#### **FASE 4: Testing Automatizado**
**Estado:** 📋 PENDIENTE (0/6 tareas)  
**Duración:** 2-3 días  
**Prioridad:** ⭐⭐ ALTA

**Documentación:**
- `fases/FASE_4_TESTING.md` - Suite de tests

**Entregables:**
- Tests de seguridad (15+ tests)
- Tests de mejoras técnicas (10+ tests)
- Tests de producción (opcional, 8+ tests)
- Cobertura mínima del 75%
- Tests de autenticación y autorización
- Tests de bloqueo de cuentas

**Ruta:** `docs/sprints/SPRINT_7/fases/FASE_4_TESTING.md`

---

#### **FASE 5: Documentación Técnica**
**Estado:** 📋 PENDIENTE (0/4 tareas)  
**Duración:** 1-2 días  
**Prioridad:** ⭐ MEDIA

**Documentación:**
- `fases/FASE_5_DOCUMENTACION.md` - Manuales y guías

**Entregables:**
- Manual de Producción (solo si se implementa - 600+ líneas)
- Guía de Seguridad Avanzada (500+ líneas)
- Manual de Mejoras Técnicas (400+ líneas)
- Guía de Configuración 2FA (300+ líneas)

**Ruta:** `docs/sprints/SPRINT_7/fases/FASE_5_DOCUMENTACION.md`

---

### 🧪 Testing

**Estado:** 📋 PENDIENTE (0/6 tareas)

**Cobertura Objetivo:**
- ✅ Tests unitarios: >25 tests (Seguridad, Mejoras, Producción opcional)
- ✅ Tests de integración: 4 tests E2E
- ✅ Cobertura de código: 75%+
- ✅ Tests de seguridad (2FA, bloqueos, JWT)
- ✅ Tests de migraciones (username, timestamp)
- ✅ Tests de auditoría

**Documentación:** Ver `fases/FASE_4_TESTING.md`

---

### 📚 Documentación de Usuario

**Estado:** 📋 PENDIENTE (0/4 manuales)

**Manuales:**
1. 📋 `MANUAL_PRODUCCION.md` (opcional - 600+ líneas)
2. 📋 `MANUAL_SEGURIDAD_AVANZADA.md` (500+ líneas)
3. 📋 `GUIA_MEJORAS_TECNICAS.md` (400+ líneas)
4. 📋 `GUIA_CONFIGURACION_2FA.md` (300+ líneas)

**Total estimado:** ~1,800 líneas de documentación de usuario

---

## 📁 ESTRUCTURA DE ARCHIVOS

```
SPRINT_7/
├── CHECKLIST_SPRINT_7.md         (Checklist maestro)
├── RESUMEN_SPRINT_7.md            (Resumen ejecutivo)
├── SPRINT_7_PLAN_MAESTRO.md       (Plan detallado)
├── INDICE_SPRINT_7.md             (Este archivo)
├── README.md                       (Introducción al sprint)
│
├── fases/
│   ├── FASE_1_PRODUCCION.md       (OPCIONAL)
│   ├── FASE_2_MEJORAS_TECNICAS.md (CRÍTICO)
│   ├── FASE_3_SEGURIDAD.md        (CRÍTICO)
│   ├── FASE_4_TESTING.md
│   └── FASE_5_DOCUMENTACION.md
│
└── manuales/
    ├── MANUAL_PRODUCCION.md       (opcional)
    ├── MANUAL_SEGURIDAD_AVANZADA.md
    ├── GUIA_MEJORAS_TECNICAS.md
    └── GUIA_CONFIGURACION_2FA.md
```

---

## 🎯 OBJETIVOS DEL SPRINT

### Objetivo Principal
Mejorar la robustez técnica y seguridad del sistema mediante refactoring de código crítico, implementación de seguridad avanzada, y opcionalmente agregar capacidades de manufactura para empresas productoras.

### Objetivos Específicos

1. **🏭 Producción (OPCIONAL):**
   - Sistema completo de órdenes de producción
   - Recetas y BOM (Bill of Materials)
   - Costeo de producción
   - Consumo de materiales
   - Productos terminados

2. **🔧 Mejoras Técnicas (CRÍTICO):**
   - **Migrar username de teléfono a email/usuario** ⚠️
   - **Migrar Timestamp a LocalDateTime** ⚠️
   - **Implementar "Remember Me"** ⚠️
   - **Completar sistema de auditoría** ⚠️
   - Optimizar queries (N+1)
   - Implementar caché de segundo nivel
   - Mejorar paginación y validaciones
   - Centralizar manejo de excepciones

3. **🔒 Seguridad Avanzada (CRÍTICO):**
   - Autenticación de dos factores (2FA)
   - JWT con refresh tokens
   - Bloqueo de cuentas por intentos fallidos
   - Políticas de contraseñas robustas
   - Encriptación de datos sensibles
   - Rate limiting y protección DDoS
   - Headers de seguridad
   - Sanitización de inputs

4. **🧪 Testing:**
   - Cobertura del 75%+
   - Tests de seguridad exhaustivos
   - Tests de migraciones

5. **📚 Documentación:**
   - Guías de seguridad
   - Manuales técnicos

---

## 📊 MÉTRICAS Y OBJETIVOS

### Métricas de Progreso (Versión Completa)

```
┌─────────────────────────────────────────────────────────────┐
│            SPRINT 7 - MÉTRICAS OBJETIVO (COMPLETO)           │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Total de Tareas:                     128 tareas            │
│  Duración Estimada:                   20-27 días            │
│  Velocidad Requerida:                 5-6 tareas/día        │
│                                                              │
│  Nuevas Tablas BD:                    8 tablas              │
│  Entidades Java:                      12 entidades          │
│  Services:                            8 servicios           │
│  Controllers:                         6 controllers         │
│  Templates HTML:                      10 vistas             │
│                                                              │
│  Tests Unitarios:                     25+ tests             │
│  Tests Integración:                   4 tests               │
│  Cobertura Objetivo:                  75%+                  │
│                                                              │
│  Líneas de Código (estimadas):       ~12,000 líneas        │
│  Líneas de Documentación:             ~2,200 líneas        │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Métricas de Progreso (Sin Producción - Recomendado)

```
┌─────────────────────────────────────────────────────────────┐
│         SPRINT 7 - MÉTRICAS OBJETIVO (SIN PRODUCCIÓN)        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Total de Tareas:                     76 tareas             │
│  Duración Estimada:                   12-17 días            │
│  Velocidad Requerida:                 4-6 tareas/día        │
│                                                              │
│  Nuevas Tablas BD:                    3 tablas              │
│  Entidades Java:                      5 entidades           │
│  Services:                            4 servicios           │
│  Controllers:                         3 controllers         │
│  Templates HTML:                      5 vistas              │
│                                                              │
│  Tests Unitarios:                     17+ tests             │
│  Tests Integración:                   3 tests               │
│  Cobertura Objetivo:                  75%+                  │
│                                                              │
│  Líneas de Código (estimadas):       ~6,000 líneas         │
│  Líneas de Documentación:             ~1,600 líneas        │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Indicadores de Éxito

✅ **Mejoras técnicas implementadas** - Username migrado, LocalDateTime activo  
✅ **Seguridad avanzada activa** - 2FA, bloqueos, JWT implementados  
✅ **Auditoría completa** - Todos los eventos críticos registrados  
✅ **Testing > 75%** - Cobertura de código objetivo alcanzada  
✅ **Producción funcional (opcional)** - Sistema de manufactura operativo  
✅ **Hallazgos del código resueltos** - Username, Timestamp, Remember Me, Auditoría  

---

## 🔍 HALLAZGOS DEL CÓDIGO A CORREGIR

### ⚠️ CRÍTICO: Problemas Detectados en Código Actual

#### 1. **Username es Teléfono (Debería Cambiarse)**
**Ubicación:** Modelo `Usuario.java`, Spring Security config  
**Estado:** ⚠️ **CRÍTICO** - Diseño no escalable  
**Problema:**
- Username está almacenado como teléfono
- Dificulta autenticación por email
- No sigue buenas prácticas de seguridad

**Acción Sprint 7:**
- 🔧 **Migración de BD:** Añadir columna `username` separada de `telefono`
- 🔧 Permitir login con email o username (no teléfono)
- 🔧 Mantener `telefono` solo para contacto
- 🔧 Actualizar Spring Security para usar nuevo campo
- 🔧 Migrar usuarios existentes (script SQL)

**Tareas relacionadas:**
- FASE 2.1.1: Migrar modelo Usuario (username separado)
- FASE 2.1.2: Actualizar Spring Security config
- FASE 2.1.3: Script de migración de datos
- FASE 2.1.4: Actualizar formularios de login/registro

**Valor:** Mejora seguridad y usabilidad, sigue estándares de la industria

---

#### 2. **Uso de Timestamp (Migrar a LocalDateTime)**
**Ubicación:** Múltiples entidades (Factura, Pago, Auditoria, etc.)  
**Estado:** ⚠️ **CRÍTICO** - Tecnología obsoleta  
**Problema:**
- `java.sql.Timestamp` está deprecated desde Java 8
- `LocalDateTime` es más moderno y type-safe
- Incompatibilidad con Java Time API

**Acción Sprint 7:**
- 🔧 **Migración masiva:** Cambiar todos los `Timestamp` a `LocalDateTime`
- 🔧 Actualizar repositorios y queries
- 🔧 Actualizar DTOs y mappers
- 🔧 Configurar Jackson para serialización de fechas
- 🔧 Tests de regresión extensivos

**Tareas relacionadas:**
- FASE 2.2.1: Auditar uso de Timestamp en el proyecto
- FASE 2.2.2: Migrar entidades a LocalDateTime
- FASE 2.2.3: Actualizar queries JPQL/HQL
- FASE 2.2.4: Configurar serialización JSON
- FASE 2.2.5: Script de migración de BD (ALTER TABLE)

**Archivos afectados (estimados):**
- `Factura.java`, `Pago.java`, `Usuario.java`, `Auditoria.java`
- Todos los `*Repository.java` con queries de fecha
- Todos los `*DTO.java` con campos de fecha

**Valor:** Moderniza el código, mejor compatibilidad con Java moderno

---

#### 3. **Sin "Remember Me" en Login**
**Ubicación:** Login form, Spring Security config  
**Estado:** ⚠️ **FALTANTE** - Feature esperada  
**Problema:**
- No existe checkbox "Recordarme" en login
- Usuarios deben autenticarse en cada sesión
- Mala experiencia de usuario

**Acción Sprint 7:**
- 🔧 Implementar "Remember Me" de Spring Security
- 🔧 Añadir checkbox en formulario de login
- 🔧 Configurar cookie persistente (14-30 días)
- 🔧 Opción de "Cerrar todas las sesiones" en perfil

**Tareas relacionadas:**
- FASE 2.3.1: Configurar Remember Me en SecurityConfig
- FASE 2.3.2: Actualizar formulario login.html
- FASE 2.3.3: Crear tabla `persistent_logins` (si se usa DB)
- FASE 2.3.4: Tests de funcionalidad Remember Me

**Valor:** Mejora UX sin comprometer seguridad

---

#### 4. **Sin Auditoría Completa**
**Ubicación:** Servicios críticos (Usuario, Factura, Pago, etc.)  
**Estado:** ⚠️ **PARCIAL** - Sistema incompleto  
**Problema:**
- Auditoría no cubre todos los eventos críticos
- Falta seguimiento de modificaciones
- No hay log de cambios en configuraciones

**Acción Sprint 7:**
- 🔧 Implementar auditoría completa con Spring Data Envers
- 🔧 O implementar manualmente tabla `Auditoria`
- 🔧 Registrar: CREAR, MODIFICAR, ELIMINAR, LOGIN, LOGOUT
- 🔧 Incluir IP, user-agent, timestamp
- 🔧 Reporte de actividad de usuarios

**Tareas relacionadas:**
- FASE 2.4.1: Diseñar modelo `Auditoria` completo
- FASE 2.4.2: Implementar interceptor de auditoría
- FASE 2.4.3: Auditar operaciones críticas
- FASE 2.4.4: Vista de logs de auditoría
- FASE 2.4.5: Exportar logs a CSV

**Eventos a auditar:**
- Login/Logout exitosos y fallidos
- Creación/modificación/eliminación de facturas
- Cambios en configuración de empresa
- Creación/modificación de usuarios
- Cambios de permisos

**Valor:** Cumplimiento normativo, trazabilidad completa

---

### 🔧 Otros Hallazgos Técnicos

#### 5. **Queries N+1 en Reportes**
**Estado:** ⚠️ Problema de rendimiento  
**Acción:** Implementar fetch joins y optimización

#### 6. **Sin Rate Limiting**
**Estado:** ⚠️ Vulnerable a DDoS  
**Acción:** Implementar con Bucket4j o similar

#### 7. **Validaciones Inconsistentes**
**Estado:** ⚠️ Algunas entidades sin @Valid  
**Acción:** Estandarizar validaciones en todos los endpoints

---

## 🔗 DEPENDENCIAS

### Dependencias Técnicas

**Nuevas dependencias Maven:**
- Spring Security 2FA (TOTP)
- JWT (io.jsonwebtoken)
- Bucket4j (rate limiting)
- Spring Data Envers (auditoría opcional)
- Hibernate Validator
- Redis (caché de segundo nivel - opcional)

### Dependencias de Sprints Anteriores

**Requiere completados:**
- ✅ Sprint 1-6: Sistema funcional completo
- ✅ Usuarios y autenticación básica
- ✅ Configuración de empresa

---

## ⚠️ RIESGOS Y MITIGACIONES

### Riesgos Identificados

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|---------|------------|
| Migración de username rompe login | Alta | Crítico | Tests exhaustivos, rollback plan, migración gradual |
| Timestamp a LocalDateTime afecta queries | Alta | Alto | Revisar todos los queries, tests de regresión |
| 2FA complica UX | Media | Medio | Hacer opcional por usuario, guía clara |
| Producción innecesaria para mayoría | Alta | Bajo | Marcar como OPCIONAL, priorizar Fases 2 y 3 |
| Migración de datos falla | Media | Crítico | Backup completo antes, dry-run en staging |

---

## 📅 CRONOGRAMA ESTIMADO

### Con Producción (128 tareas)
```
Semana 1 (17-23 Mar):  FASE 1 - Producción (Completa) [OPCIONAL]
Semana 2 (24-30 Mar):  FASE 2 - Mejoras Técnicas (Migraciones críticas)
Semana 3 (31 Mar-6 Abr): FASE 2 - Mejoras Técnicas (Optimizaciones)
Semana 4 (7-13 Abr):   FASE 3 - Seguridad + FASE 4 - Testing + FASE 5 - Docs
```

### Sin Producción (76 tareas - RECOMENDADO)
```
Semana 1 (17-23 Mar):  FASE 2 - Mejoras Técnicas (Completa)
Semana 2 (24-30 Mar):  FASE 3 - Seguridad (Completa)
Semana 3 (31 Mar-6 Abr): FASE 4 - Testing + FASE 5 - Documentación
```

**Fecha límite:** 13 de abril de 2026 (completo) o 6 de abril (sin producción)

---

## 🔄 SIGUIENTES PASOS

### Inmediatos (Antes de iniciar)
1. ✅ Revisar y aprobar ÍNDICE_SPRINT_7.md
2. 🔍 **CRÍTICO:** Auditar todas las entidades con `Timestamp`
3. 🔍 **CRÍTICO:** Identificar ubicación exacta del campo username=teléfono
4. 🔍 Revisar estado actual del sistema de auditoría
5. 🔍 Verificar si existe "Remember Me" parcialmente implementado
6. 📋 Crear CHECKLIST_SPRINT_7.md detallado
7. 📋 Crear SPRINT_7_PLAN_MAESTRO.md

### Decisión Crítica
8. ⚠️ **DECIDIR:** ¿Implementar módulo de Producción? (depende del negocio)
   - ✅ **SI:** Manufactura, ensamblaje, transformación de productos
   - ❌ **NO:** Solo comercio, servicios, retail

### Primera Fase (si NO hay producción)
9. 🚀 Iniciar FASE 2: Mejoras Técnicas (migración username)

---

## 📚 REFERENCIAS

- [Clasificación Sprints Futuros](../CLASIFICACION_SPRINTS_FUTUROS.md)
- [Hallazgos del Código](../../refactorizacion/HALLAZGOS_CODIGO.md) *(si existe)*
- [Estado Proyecto](../../reportes/ESTADO_PROYECTO.md)
- [Sprint 6 - Multi-Divisa](../SPRINT_6/)
- [Spring Security Remember Me](https://docs.spring.io/spring-security/reference/servlet/authentication/rememberme.html)
- [Java Time API](https://docs.oracle.com/javase/8/docs/api/java/time/package-summary.html)

---

## 💡 NOTAS TÉCNICAS

### Migración Username

**Antes (actual):**
```java
@Entity
public class Usuario {
    @Column(unique = true)
    private String username; // Contiene TELÉFONO ⚠️
    private String telefono; // ¿Duplicado?
}
```

**Después (Sprint 7):**
```java
@Entity
public class Usuario {
    @Column(unique = true)
    private String username; // Email o usuario real
    
    @Column(unique = true)
    private String email;
    
    private String telefono; // Solo para contacto
}

// Login permitido con username O email
```

**Script de migración:**
```sql
-- Paso 1: Añadir columna username_nuevo
ALTER TABLE usuario ADD COLUMN username_nuevo VARCHAR(100);

-- Paso 2: Migrar datos (username_nuevo = email si existe, sino = telefono)
UPDATE usuario SET username_nuevo = COALESCE(email, CONCAT('user_', telefono));

-- Paso 3: Validar migración
-- SELECT * FROM usuario WHERE username_nuevo IS NULL;

-- Paso 4: Renombrar columnas
-- ALTER TABLE usuario CHANGE username telefono_legacy VARCHAR(20);
-- ALTER TABLE usuario CHANGE username_nuevo username VARCHAR(100);
```

---

### Migración Timestamp → LocalDateTime

**Antes (actual):**
```java
@Entity
public class Factura {
    private Timestamp fechaCreacion; // ⚠️ Deprecated
}
```

**Después (Sprint 7):**
```java
@Entity
public class Factura {
    private LocalDateTime fechaCreacion; // ✅ Moderno
}
```

**Configuración Jackson:**
```java
@Configuration
public class JacksonConfig {
    @Bean
    public Jackson2ObjectMapperBuilder jacksonBuilder() {
        return new Jackson2ObjectMapperBuilder()
            .serializers(new LocalDateTimeSerializer(DateTimeFormatter.ISO_DATE_TIME))
            .deserializers(new LocalDateTimeDeserializer(DateTimeFormatter.ISO_DATE_TIME));
    }
}
```

---

### Remember Me Implementation

**SecurityConfig:**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.rememberMe()
            .key("uniqueAndSecret")
            .tokenValiditySeconds(86400 * 30) // 30 días
            .rememberMeParameter("remember-me")
            .rememberMeCookieName("remember-me-cookie");
        return http.build();
    }
}
```

**Login form:**
```html
<form th:action="@{/login}" method="post">
    <input type="text" name="username" />
    <input type="password" name="password" />
    <input type="checkbox" name="remember-me" /> Recordarme
    <button type="submit">Iniciar sesión</button>
</form>
```

---

### Sistema de Auditoría Completo

**Modelo:**
```java
@Entity
public class Auditoria {
    @Id
    @GeneratedValue
    private Long id;
    
    private String usuario;
    private String accion; // CREAR, MODIFICAR, ELIMINAR, LOGIN, LOGOUT
    private String entidad; // Usuario, Factura, Producto, etc.
    private Long entidadId;
    private String ip;
    private String userAgent;
    private LocalDateTime timestamp;
    private String detalles; // JSON con cambios
}
```

**Interceptor:**
```java
@Aspect
@Component
public class AuditoriaAspect {
    
    @AfterReturning("@annotation(Auditable)")
    public void auditar(JoinPoint joinPoint) {
        // Registrar en tabla auditoria
    }
}
```

---

**Documento creado:** 16 de enero de 2026  
**Creado por:** GitHub Copilot  
**Versión:** 1.0  
**Estado:** 📋 PLANIFICADO  
**Decisión pendiente:** ¿Implementar módulo de Producción?
