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

