# 📚 FASE 5: Documentación

**Sprint:** 7  
**Fase:** 5 de 5  
**Duración estimada:** 1-2 días  
**Prioridad:** ⭐ MEDIA  
**Estado:** 📋 PENDIENTE (0/4 tareas)

---

## 📋 OBJETIVO DE LA FASE

Documentar exhaustivamente las mejoras técnicas y de seguridad implementadas en el Sprint 7:
- Manual de seguridad
- Guía de configuración 2FA
- Documentación de migraciones
- Guía de autenticación JWT

---

## 📊 PROGRESO GENERAL

```
Progreso: [0/4] ░░░░░░░░░░░░░░░░░░░░ 0%

├─ 1. Manual de Seguridad              [0/1]  ░░░░░░░░░░ 0%
├─ 2. Guía de Configuración 2FA        [0/1]  ░░░░░░░░░░ 0%
├─ 3. Guía de Migraciones              [0/1]  ░░░░░░░░░░ 0%
└─ 4. Documentación Técnica JWT        [0/1]  ░░░░░░░░░░ 0%
```

---

## 📦 1. MANUAL DE SEGURIDAD (1 tarea)

#### Tareas:

- [ ] **1.1** Crear manual completo de seguridad

**Ubicación:** `docs/guias/MANUAL_SEGURIDAD.md`

**Contenido:**

```markdown
# Manual de Seguridad - ERP Orders Manager

## 📋 Índice

1. [Introducción](#introducción)
2. [Políticas de Contraseña](#políticas-de-contraseña)
3. [Autenticación de Dos Factores (2FA)](#autenticación-de-dos-factores-2fa)
4. [Bloqueo de Cuentas](#bloqueo-de-cuentas)
5. [JWT y Refresh Tokens](#jwt-y-refresh-tokens)
6. [Remember Me](#remember-me)
7. [Rate Limiting](#rate-limiting)
8. [Headers de Seguridad](#headers-de-seguridad)
9. [Auditoría](#auditoría)
10. [Buenas Prácticas](#buenas-prácticas)

---

## 🔐 Introducción

Este manual documenta todas las medidas de seguridad implementadas en el sistema ERP Orders Manager para proteger los datos de la empresa y los usuarios.

### Niveles de Seguridad

- **Autenticación:** Email/Username + Contraseña + 2FA (opcional)
- **Autorización:** Roles y permisos granulares
- **Protección:** Bloqueo de cuenta, rate limiting, headers de seguridad
- **Auditoría:** Registro completo de eventos de seguridad

---

## 🔑 Políticas de Contraseña

### Requisitos Obligatorios

Todas las contraseñas DEBEN cumplir:

- ✅ Mínimo 8 caracteres
- ✅ Al menos 1 mayúscula (A-Z)
- ✅ Al menos 1 minúscula (a-z)
- ✅ Al menos 1 número (0-9)
- ✅ Al menos 1 carácter especial (!@#$%^&*...)

### Ejemplos

❌ **Contraseñas RECHAZADAS:**
- `password` - Sin mayúsculas, números ni caracteres especiales
- `Password` - Sin números ni caracteres especiales
- `Pass123` - Sin caracteres especiales y muy corta
- `Pass@123` - Muy corta (7 caracteres)

✅ **Contraseñas ACEPTADAS:**
- `Password123!`
- `MyP@ssw0rd`
- `Secure#2026`

### Expiración

- Las contraseñas expiran cada **90 días**
- El sistema notifica al usuario 7 días antes
- No se pueden reutilizar las últimas 5 contraseñas

### Cambio de Contraseña

1. Ir a **Perfil > Seguridad**
2. Ingresar contraseña actual
3. Ingresar nueva contraseña (cumpliendo requisitos)
4. Confirmar nueva contraseña

---

## 🔐 Autenticación de Dos Factores (2FA)

### ¿Qué es 2FA?

La autenticación de dos factores agrega una **capa adicional de seguridad** requiriendo:
1. **Algo que sabes:** Tu contraseña
2. **Algo que tienes:** Tu teléfono/dispositivo

### Métodos Disponibles

1. **TOTP (Recomendado):** Google Authenticator, Authy, Microsoft Authenticator
2. **Email:** Código enviado por correo electrónico
3. **Códigos de Recuperación:** Para emergencias

### Habilitar 2FA

#### Paso 1: Configuración Inicial

1. Ir a **Perfil > Seguridad > Autenticación de Dos Factores**
2. Hacer clic en **"Habilitar 2FA"**
3. Seleccionar método (TOTP recomendado)

#### Paso 2: Configurar Aplicación Autenticadora

1. Descargar **Google Authenticator** o **Authy** en tu teléfono
2. Escanear el **código QR** mostrado en pantalla
3. La app generará códigos de 6 dígitos cada 30 segundos

#### Paso 3: Verificar Configuración

1. Ingresar el código de 6 dígitos de la app
2. Sistema valida el código
3. 2FA queda **habilitado**

#### Paso 4: Guardar Códigos de Recuperación

⚠️ **MUY IMPORTANTE:**

- Se mostrarán **10 códigos de recuperación**
- **Guardarlos en lugar seguro** (password manager, papel en caja fuerte)
- Cada código se puede usar **una sola vez**
- Son tu **única alternativa** si pierdes acceso a la app

```
Ejemplos de códigos de recuperación:
12345678
87654321
11223344
...
```

### Usar 2FA en Login

1. Ingresar email/username y contraseña
2. Sistema redirige a página de verificación 2FA
3. Ingresar código de 6 dígitos de la app
4. ✅ Acceso concedido

### Usar Código de Recuperación

Si pierdes acceso a tu app autenticadora:

1. En página de verificación 2FA, clic en **"Usar código de recuperación"**
2. Ingresar uno de los 10 códigos guardados
3. ⚠️ El código queda **invalidado** (no se puede reusar)
4. **Reconfigurar 2FA inmediatamente**

### Deshabilitar 2FA

1. Ir a **Perfil > Seguridad**
2. Clic en **"Deshabilitar 2FA"**
3. Ingresar código TOTP actual para confirmar
4. 2FA queda deshabilitado

⚠️ **Nota:** Solo ADMIN puede deshabilitar 2FA de otro usuario.

---

## 🚫 Bloqueo de Cuentas

### Política de Bloqueo

Para prevenir ataques de fuerza bruta:

- **5 intentos fallidos** consecutivos → Cuenta bloqueada
- Duración del bloqueo: **30 minutos**
- Contador se resetea tras login exitoso

### ¿Qué hacer si tu cuenta está bloqueada?

#### Opción 1: Esperar 30 minutos

El bloqueo se levanta automáticamente.

#### Opción 2: Contactar Administrador

El administrador puede:
- Desbloquear manualmente la cuenta
- Resetear contador de intentos fallidos
- Resetear contraseña si olvidaste la tuya

### Para Administradores

**Desbloquear cuenta:**

1. Ir a **Usuarios > Gestión**
2. Buscar usuario bloqueado (filtro: "Cuenta Bloqueada")
3. Clic en **"Desbloquear Cuenta"**
4. Confirmar acción

---

## 🔑 JWT y Refresh Tokens

### ¿Qué es JWT?

**JSON Web Token (JWT)** es un estándar para autenticación sin estado (stateless).

### Tipos de Tokens

#### Access Token
- **Duración:** 1 hora
- **Uso:** Autenticar cada request API
- **Almacenamiento:** `localStorage` o memoria

#### Refresh Token
- **Duración:** 30 días
- **Uso:** Obtener nuevo access token sin re-login
- **Almacenamiento:** Cookie `httpOnly` segura

### Flujo de Autenticación

```
1. Usuario hace login
   ↓
2. Backend genera Access Token + Refresh Token
   ↓
3. Frontend almacena tokens
   ↓
4. Cada request API incluye: Authorization: Bearer {accessToken}
   ↓
5. Si access token expira:
   - Frontend usa refresh token para obtener nuevo access token
   - Si refresh token también expiró → Redirigir a login
```

### Uso desde Frontend (JavaScript)

```javascript
// Login
const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
});

const { accessToken, refreshToken } = await response.json();

// Guardar tokens
localStorage.setItem('accessToken', accessToken);
localStorage.setItem('refreshToken', refreshToken);

// Hacer request autenticado
const data = await fetch('/api/usuarios/perfil', {
    headers: {
        'Authorization': `Bearer ${accessToken}`
    }
});

// Refresh token cuando access token expira
const refreshResponse = await fetch('/api/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken })
});

const { accessToken: newAccessToken } = await refreshResponse.json();
localStorage.setItem('accessToken', newAccessToken);
```

### Revocar Tokens

**Logout:**
- Elimina tokens del frontend
- Revoca refresh token en backend

**Revocar todos los tokens de un usuario:**
- Útil si se sospecha compromiso de cuenta
- Administrador puede forzar logout de todos los dispositivos

---

## 🔐 Remember Me

### Funcionamiento

- Checkbox **"Recordarme"** en login
- Si activado: crea cookie segura con duración de **30 días**
- Usuario no necesita re-login durante 30 días

### Configuración

**Para usuarios:**
- Marcar checkbox "Recordarme en este dispositivo" al hacer login

**Para administradores:**
- Configurar duración en `application.properties`:

```properties
# Duración de Remember Me (en segundos)
# 30 días = 2592000 segundos
security.remember-me.validity-seconds=2592000
```

### Seguridad

- Cookie es **httpOnly** (no accesible por JavaScript)
- Cookie es **secure** (solo HTTPS en producción)
- Cookie se elimina al hacer logout
- Tokens se guardan en base de datos (revocables)

---

## ⏱️ Rate Limiting

### Límites por IP

Para prevenir abuso:

| Endpoint | Límite | Ventana |
|----------|--------|---------|
| `/login` | 10 intentos | 1 hora |
| `/api/**` | 100 requests | 1 minuto |
| `/registro` | 5 registros | 1 hora |

### Respuesta cuando se excede límite

```json
HTTP 429 Too Many Requests
{
    "error": "Rate limit exceeded",
    "message": "Demasiados intentos. Intenta en 45 minutos.",
    "retryAfter": 2700
}
```

---

## 🛡️ Headers de Seguridad

### Headers Implementados

```http
# Prevenir XSS
X-XSS-Protection: 1; mode=block

# Prevenir Clickjacking
X-Frame-Options: DENY

# Content Security Policy
Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline'

# HSTS (Force HTTPS)
Strict-Transport-Security: max-age=31536000; includeSubDomains

# Prevenir MIME sniffing
X-Content-Type-Options: nosniff
```

### CORS

```java
// Configuración CORS
@CrossOrigin(origins = "https://erp.miempresa.com")
```

---

## 📊 Auditoría

### Eventos Auditados

Todos los siguientes eventos se registran:

- ✅ Login exitoso/fallido
- ✅ Logout
- ✅ Habilitación/deshabilitación 2FA
- ✅ Cambio de contraseña
- ✅ Creación/modificación/eliminación de usuarios
- ✅ Cambios en permisos/roles
- ✅ Bloqueo/desbloqueo de cuentas
- ✅ Intentos de acceso no autorizado

### Consultar Auditoría

**Para administradores:**

1. Ir a **Seguridad > Auditoría**
2. Filtrar por:
   - Usuario
   - Tipo de evento
   - Rango de fechas
   - Resultado (éxito/fallo)
   - IP
3. Ver detalles de cada evento

### Retención

- Eventos se guardan por **2 años**
- Backup semanal de tabla de auditoría

---

## ✅ Buenas Prácticas

### Para Usuarios

1. ✅ Usar contraseñas fuertes y únicas
2. ✅ Habilitar 2FA
3. ✅ Guardar códigos de recuperación
4. ✅ Cerrar sesión al terminar (especialmente en dispositivos compartidos)
5. ✅ No compartir contraseñas
6. ✅ Reportar actividad sospechosa

### Para Administradores

1. ✅ Forzar 2FA para usuarios con permisos elevados
2. ✅ Revisar logs de auditoría regularmente
3. ✅ Implementar política de cambio de contraseña cada 90 días
4. ✅ Revocar acceso de empleados que dejan la empresa
5. ✅ Mantener sistema actualizado
6. ✅ Realizar auditorías de seguridad periódicas

---

## 🆘 Soporte

Para problemas de seguridad:
- **Email:** seguridad@erp.com
- **Urgencias:** +506 XXXX-XXXX
- **Documentación:** https://docs.erp.com/seguridad

---

**Última actualización:** Enero 2026  
**Versión:** 7.0.0
```

---

## 📦 2. GUÍA DE CONFIGURACIÓN 2FA (1 tarea)

#### Tareas:

- [ ] **2.1** Crear guía visual de configuración 2FA

**Ubicación:** `docs/guias/GUIA_CONFIGURACION_2FA.md`

**Contenido:**

```markdown
# Guía de Configuración 2FA

## 📱 Paso a Paso con Capturas

### 1. Descargar Aplicación Autenticadora

#### Opciones recomendadas:

**Google Authenticator:**
- [Android](https://play.google.com/store/apps/details?id=com.google.android.apps.authenticator2)
- [iOS](https://apps.apple.com/app/google-authenticator/id388497605)

**Microsoft Authenticator:**
- [Android](https://play.google.com/store/apps/details?id=com.azure.authenticator)
- [iOS](https://apps.apple.com/app/microsoft-authenticator/id983156458)

**Authy:**
- [Android](https://play.google.com/store/apps/details?id=com.authy.authy)
- [iOS](https://apps.apple.com/app/authy/id494168017)

### 2. Iniciar Configuración

1. Login en el sistema
2. Ir a **Perfil** (esquina superior derecha)
3. Clic en **Seguridad**
4. Buscar sección **"Autenticación de Dos Factores"**
5. Clic en botón **"Habilitar 2FA"**

### 3. Escanear Código QR

1. En la app autenticadora, clic en **+** o **"Añadir cuenta"**
2. Seleccionar **"Escanear código QR"**
3. Apuntar cámara al QR en pantalla del sistema
4. La app agregará la cuenta automáticamente

**Nombre de la cuenta:** ERP Orders Manager (tu-email@ejemplo.com)

### 4. Ingresar Código de Verificación

1. En la app, verás un código de **6 dígitos** (cambia cada 30 segundos)
2. Ingresar ese código en el campo del sistema
3. Clic en **"Verificar"**

✅ Si el código es correcto: "2FA habilitado exitosamente"

### 5. Guardar Códigos de Recuperación

⚠️ **CRÍTICO:**

```
Códigos de recuperación:
12345678
87654321
11223344
44332211
99887766
66778899
55443322
22334455
88776655
00112233
```

**Opciones de almacenamiento:**
1. ✅ Password manager (1Password, LastPass, Bitwarden)
2. ✅ Captura de pantalla en dispositivo seguro
3. ✅ Imprimir y guardar en caja fuerte
4. ❌ NO dejar en archivo de texto sin encriptar

### 6. Probar 2FA

1. Hacer logout
2. Ingresar email y contraseña
3. Sistema pide código 2FA
4. Abrir app autenticadora
5. Ingresar código de 6 dígitos
6. ✅ Login exitoso

---

## 🆘 Problemas Comunes

### "Código inválido"

**Causa:** Reloj del dispositivo desincronizado

**Solución:**
1. Ir a **Ajustes > Fecha y hora**
2. Activar **"Fecha y hora automáticas"**
3. Reintentar

### Perdí acceso a la app

**Solución:**
1. Usar **código de recuperación**
2. Reconfigurar 2FA con nueva app

### No guardé códigos de recuperación

**Solución:**
1. Contactar administrador
2. Administrador puede deshabilitar tu 2FA
3. Reconfigurar 2FA y esta vez **guardar códigos**

---

**Versión:** 1.0  
**Fecha:** Enero 2026
```

---

## 📦 3. GUÍA DE MIGRACIONES (1 tarea)

#### Tareas:

- [ ] **3.1** Documentar procedimiento de migraciones

**Ubicación:** `docs/guias/GUIA_MIGRACIONES_SPRINT_7.md`

**Contenido:**

```markdown
# Guía de Migraciones - Sprint 7

## 📋 Resumen

Sprint 7 incluye **3 migraciones críticas**:

1. ⚠️ Username → Email/Username
2. ⚠️ Timestamp → LocalDateTime
3. 🔐 Tablas de seguridad (2FA, refresh tokens, auditoría)

---

## 🔄 Migración 1: Username → Email

### Objetivo

Separar campo `username` (que actualmente guarda teléfono) en:
- `email` (obligatorio, para login)
- `username` (opcional, alternativa para login)
- `telefono` (campo separado)

### Script SQL

**Archivo:** `MIGRATION_USERNAME_EMAIL_SPRINT_7.sql`

### Pasos de Ejecución

#### 1. Backup

```bash
mysqldump -u root -p erp_db usuarios > backup_usuarios_pre_migration.sql
```

#### 2. Ejecutar Migration

```sql
source docs/base\ de\ datos/MIGRATION_USERNAME_EMAIL_SPRINT_7.sql;
```

#### 3. Verificar

```sql
-- Verificar que email fue generado
SELECT id, email, username, telefono FROM usuarios LIMIT 10;

-- Verificar que no hay duplicados
SELECT email, COUNT(*) FROM usuarios GROUP BY email HAVING COUNT(*) > 1;
SELECT username, COUNT(*) FROM usuarios WHERE username IS NOT NULL GROUP BY username HAVING COUNT(*) > 1;
```

#### 4. Validar Aplicación

```bash
mvn test -Dtest=MigracionUsernameEmailTest
```

### Rollback

```sql
-- Si algo sale mal:
DROP TABLE usuarios;
mysql -u root -p erp_db < backup_usuarios_pre_migration.sql
```

---

## 🕒 Migración 2: Timestamp → LocalDateTime

### Objetivo

Reemplazar `java.sql.Timestamp` (deprecated) con `java.time.LocalDateTime` en todas las entidades.

### Tablas Afectadas

- `usuarios`
- `clientes`
- `productos`
- `facturas`
- `pagos`
- `empresas`
- `notificaciones`
- ... y todas las tablas con `created_at`/`updated_at`

### Script SQL

**Archivo:** `MIGRATION_TIMESTAMP_TO_DATETIME_SPRINT_7.sql`

### Ejecución

```sql
source docs/base\ de\ datos/MIGRATION_TIMESTAMP_TO_DATETIME_SPRINT_7.sql;
```

### Verificación

```sql
-- Ver tipo de columna
DESCRIBE usuarios;

-- Debe mostrar:
-- created_at | datetime(6) | NO | | CURRENT_TIMESTAMP(6)
```

---

## 🔐 Migración 3: Tablas de Seguridad

### Tablas Nuevas

1. `two_factor_codes` - Códigos 2FA por email
2. `refresh_tokens` - Refresh tokens JWT
3. `auditoria_eventos` - Auditoría completa
4. `persistent_logins` - Remember Me persistente
5. `password_history` - Historial de contraseñas

### Scripts

```bash
docs/base de datos/MIGRATION_2FA_SPRINT_7.sql
docs/base de datos/MIGRATION_REMEMBER_ME_TOKENS_SPRINT_7.sql
# etc.
```

### Ejecución

```sql
-- Ejecutar en orden:
source docs/base\ de\ datos/MIGRATION_2FA_SPRINT_7.sql;
source docs/base\ de\ datos/MIGRATION_REMEMBER_ME_TOKENS_SPRINT_7.sql;
-- ...
```

---

## ✅ Checklist Post-Migración

- [ ] Backup restaurado y funcional (en caso de emergencia)
- [ ] Migraciones SQL ejecutadas sin errores
- [ ] Tests unitarios pasando
- [ ] Tests de integración pasando
- [ ] Login con email funciona
- [ ] Login con username funciona
- [ ] Remember Me funciona
- [ ] 2FA se puede habilitar
- [ ] Auditoría registra eventos
- [ ] No hay errores en logs de aplicación

---

**Responsable:** DevOps / DBA  
**Duración estimada:** 2-3 horas  
**Downtime requerido:** Opcional (migrations soportan dual-write)
```

---

## 📦 4. DOCUMENTACIÓN TÉCNICA JWT (1 tarea)

#### Tareas:

- [ ] **4.1** Documentar arquitectura JWT

**Ubicación:** `docs/referencias/ARQUITECTURA_JWT.md`

**Contenido:**

```markdown
# Arquitectura JWT - ERP Orders Manager

## 🏗️ Diagrama de Flujo

```
┌──────────┐
│ Frontend │
└────┬─────┘
     │ 1. POST /api/auth/login
     │    { email, password }
     ▼
┌─────────────────┐
│ AuthController  │
└────┬────────────┘
     │ 2. Validar credenciales
     ▼
┌──────────────────────┐
│ AuthenticationManager│
└────┬─────────────────┘
     │ 3. Usuario válido
     ▼
┌─────────────┐
│ JwtService  │
└────┬────────┘
     │ 4. Generar tokens
     │    - Access Token (1h)
     │    - Refresh Token (30d)
     ▼
┌──────────┐
│ Frontend │ 5. Guardar tokens
└──────────┘

... (requests subsiguientes) ...

┌──────────┐
│ Frontend │ GET /api/usuarios
└────┬─────┘
     │ Authorization: Bearer {accessToken}
     ▼
┌────────────────────────┐
│ JwtAuthenticationFilter│
└────┬───────────────────┘
     │ Validar token
     ▼
┌────────────┐
│ Controller │ Request autenticado
└────────────┘
```

## 🔑 Estructura de Tokens

### Access Token

```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "user@example.com",
    "email": "user@example.com",
    "roles": ["ROLE_ADMIN", "ROLE_USER"],
    "iat": 1674567890,
    "exp": 1674571490
  },
  "signature": "..."
}
```

### Refresh Token

```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "user@example.com",
    "iat": 1674567890,
    "exp": 1677246290
  },
  "signature": "..."
}
```

## 🔒 Configuración

### application.properties

```properties
# JWT Secret (cambiar en producción)
jwt.secret=YourBase64EncodedSecretKeyHere

# Duración Access Token (1 hora = 3600000 ms)
jwt.expiration=3600000

# Duración Refresh Token (30 días = 2592000000 ms)
jwt.refresh-expiration=2592000000
```

### Generar Secret

```bash
# Generar secret seguro:
openssl rand -base64 64
```

## 📡 Endpoints

### POST /api/auth/login

**Request:**
```json
{
  "email": "user@example.com",
  "password": "Password123!"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

### POST /api/auth/refresh

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

### POST /api/auth/logout

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response:**
```json
{
  "message": "Logout exitoso"
}
```

---

**Versión:** 1.0  
**Fecha:** Enero 2026
```

---

## 📊 CRITERIOS DE ACEPTACIÓN

✅ Manual de seguridad completo y publicado  
✅ Guía de configuración 2FA con capturas  
✅ Documentación de migraciones detallada  
✅ Arquitectura JWT documentada  
✅ Todos los manuales accesibles desde docs/  

---

## 📚 DEPENDENCIAS

**Requiere:**
- ✅ FASE 2: Mejoras Técnicas
- ✅ FASE 3: Seguridad Avanzada
- ✅ FASE 4: Testing

**Habilita:**
- 🚀 Usuarios pueden configurar 2FA
- 🚀 Equipo puede ejecutar migraciones
- 🚀 Sprint 7 completo

---

## 🔄 PRÓXIMOS PASOS

1. ✅ Publicar documentación
2. ✅ Capacitar usuarios en 2FA
3. 🚀 Sprint 7 COMPLETO → Continuar con Sprint 8 o 9

---

**Fase creada:** 16 de enero de 2026  
**Responsable:** Equipo de Documentación  
**Prioridad:** MEDIA - Soporte a usuarios
