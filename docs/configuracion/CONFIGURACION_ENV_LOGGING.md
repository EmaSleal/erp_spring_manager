# 🔧 Configuración de Variables de Entorno y Logging

**Fecha:** 26 de octubre de 2025  
**Versión:** 1.0  
**Estado:** ✅ Implementado

---

## 📋 Tabla de Contenidos

1. [Resumen](#resumen)
2. [Variables de Entorno](#variables-de-entorno)
3. [Configuración de Logging](#configuración-de-logging)
4. [Perfiles de Spring](#perfiles-de-spring)
5. [Guía de Instalación](#guía-de-instalación)

---

## 🎯 Resumen

Se ha implementado un sistema robusto de configuración que separa las credenciales sensibles del código fuente y proporciona un logging profesional configurable por ambiente.

### ✅ Mejoras Implementadas

- **Seguridad:** Credenciales movidas a `.env.local` (no se commitea)
- **Logging:** Configuración profesional con niveles por paquete
- **Perfiles:** Configuración separada para `dev` y `prod`
- **SQL Logging:** Control fino de queries de Hibernate
- **Archivos de Log:** Rotación automática con límites de tamaño

---

## 🔐 Variables de Entorno

### Archivos de Configuración

| Archivo | Propósito | Git |
|---------|-----------|-----|
| `.env.example` | Plantilla con ejemplos | ✅ Commitear |
| `.env.local` | Credenciales reales | ❌ **NUNCA** commitear |

### Variables Definidas

#### Base de Datos
```bash
DB_URL=jdbc:mysql://host:port/database?params
DB_USERNAME=usuario
DB_PASSWORD=contraseña
```

#### Email (Gmail SMTP)
```bash
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_USERNAME=tu-email@gmail.com
EMAIL_PASSWORD=app-password-generado
```

#### WhatsApp Meta API
```bash
META_WHATSAPP_PHONE_NUMBER_ID=phone-id
META_WHATSAPP_ACCESS_TOKEN=token-temporal
META_WHATSAPP_API_VERSION=v18.0
META_WHATSAPP_API_URL=https://graph.facebook.com
META_WEBHOOK_VERIFY_TOKEN=token-verificacion
```

---

## 📊 Configuración de Logging

### Niveles de Log por Paquete

#### Aplicación (Default - INFO)
```yaml
api.astro.whats_orders_manager: INFO
api.astro.whats_orders_manager.controllers: INFO
api.astro.whats_orders_manager.services: INFO
api.astro.whats_orders_manager.repositories: DEBUG
```

#### Hibernate/JPA (DEBUG)
```yaml
org.hibernate.SQL: DEBUG                              # Muestra queries SQL
org.hibernate.type.descriptor.sql.BasicBinder: TRACE  # Muestra parámetros
org.hibernate.orm.jdbc.bind: TRACE                    # Muestra binding
```

#### Spring Framework (INFO)
```yaml
org.springframework.web: INFO
org.springframework.security: INFO
org.springframework.jdbc.core: DEBUG
org.springframework.transaction: DEBUG
```

### Formato de Salida

```
%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
```

**Ejemplo:**
```
2025-10-26 14:30:15 [http-nio-8080-exec-1] INFO  a.a.w.controllers.AuthController - ✅ Login exitoso para usuario: admin
```

### Archivos de Log

| Propiedad | Valor | Descripción |
|-----------|-------|-------------|
| **Ruta** | `logs/whats-orders-manager.log` | Ubicación del archivo |
| **Tamaño Máximo** | 10 MB | Por archivo individual |
| **Historial** | 30 días | Archivos antiguos se eliminan |
| **Límite Total** | 1 GB | Tamaño máximo de todos los logs |

---

## 🎭 Perfiles de Spring

### Perfil: `dev` (Desarrollo)

**Activación:**
```bash
# Windows PowerShell
$env:SPRING_PROFILES_ACTIVE="dev"
java -jar app.jar

# Linux/Mac
export SPRING_PROFILES_ACTIVE=dev
java -jar app.jar
```

**Características:**
- ✅ Log level: `DEBUG` (muy detallado)
- ✅ SQL queries visibles
- ✅ Parámetros de queries mostrados
- ✅ Logs en consola
- ✅ Formato colorizado (si el terminal lo soporta)

**Ejemplo de salida:**
```
2025-10-26 14:30:15 [http-nio-8080-exec-1] DEBUG a.a.w.controllers.AuthController - Acceso a página de login
2025-10-26 14:30:16 [http-nio-8080-exec-1] DEBUG org.hibernate.SQL - select u1_0.id, u1_0.nombre from usuario u1_0 where u1_0.username=?
2025-10-26 14:30:16 [http-nio-8080-exec-1] TRACE o.h.type.descriptor.sql.BasicBinder - binding parameter [1] as [VARCHAR] - [admin]
```

---

### Perfil: `prod` (Producción)

**Activación:**
```bash
# Windows PowerShell
$env:SPRING_PROFILES_ACTIVE="prod"
java -jar app.jar

# Linux/Mac
export SPRING_PROFILES_ACTIVE=prod
java -jar app.jar
```

**Características:**
- ⚠️ Log level: `WARN` (solo alertas y errores)
- ⚠️ SQL queries ocultos
- ⚠️ Solo información crítica
- 💾 Logs en archivo: `/var/log/whats-orders-manager/application.log`
- 🔒 Mayor rendimiento (menos I/O)

**Ejemplo de salida:**
```
2025-10-26 14:30:15 [http-nio-8080-exec-1] INFO  a.a.w.controllers.AuthController - ✅ Login exitoso para usuario: admin
2025-10-26 14:30:20 [http-nio-8080-exec-2] WARN  a.a.w.controllers.AuthController - ❌ Login fallido para usuario: hacker - Razón: Credenciales inválidas
```

---

### Perfil: Default (Sin perfil específico)

**Comportamiento:**
- Usa configuración base del `application.yml`
- Log level: `INFO` (balanceado)
- SQL queries: `DEBUG` (útil para desarrollo)
- Archivos de log: `logs/whats-orders-manager.log`

---

## 📝 Guía de Instalación

### 1. Configurar Variables de Entorno

```powershell
# 1. Copiar plantilla
Copy-Item .env.example .env.local

# 2. Editar .env.local con tus credenciales reales
notepad .env.local
```

### 2. Cargar Variables en PowerShell

**Opción A: Script Automático (Recomendado)**

Crear archivo `load-env.ps1`:
```powershell
# Leer .env.local y cargar variables
Get-Content .env.local | ForEach-Object {
    if ($_ -match '^([^#][^=]*)=(.*)$') {
        $name = $matches[1].Trim()
        $value = $matches[2].Trim()
        [Environment]::SetEnvironmentVariable($name, $value, 'Process')
        Write-Host "✅ $name cargado" -ForegroundColor Green
    }
}
```

Ejecutar:
```powershell
.\load-env.ps1
```

**Opción B: Manual**
```powershell
# Base de datos
$env:DB_URL="jdbc:mysql://192.168.100.8:3306/facturas_monrachem?useSSL=false&serverTimezone=UTC"
$env:DB_USERNAME="m4n0"
$env:DB_PASSWORD="Chismosear01"

# Email
$env:EMAIL_HOST="smtp.gmail.com"
$env:EMAIL_PORT="587"
$env:EMAIL_USERNAME="EMAIL_PLACEHOLDER"
$env:EMAIL_PASSWORD="REDACTED"

# WhatsApp
$env:META_WEBHOOK_VERIFY_TOKEN="tu-token"
```

### 3. Verificar Variables

```powershell
# Ver todas las variables DB_*
Get-ChildItem Env: | Where-Object { $_.Name -like "DB_*" }

# Ver todas las variables EMAIL_*
Get-ChildItem Env: | Where-Object { $_.Name -like "EMAIL_*" }
```

### 4. Ejecutar la Aplicación

```powershell
# Desarrollo (con logs detallados)
$env:SPRING_PROFILES_ACTIVE="dev"
.\mvnw spring-boot:run

# Producción (logs mínimos)
$env:SPRING_PROFILES_ACTIVE="prod"
java -jar target/whats-orders-manager.jar
```

---

## 🔍 Verificación de Logging

### Verificar Logs en Consola

Al iniciar la aplicación, deberías ver:
```
2025-10-26 14:30:00 [main] INFO  o.s.b.w.embedded.tomcat.TomcatWebServer - Tomcat initialized with port(s): 8080 (http)
2025-10-26 14:30:01 [main] INFO  c.z.hikari.HikariDataSource - HikariPool-1 - Starting...
2025-10-26 14:30:02 [main] INFO  c.z.hikari.HikariDataSource - HikariPool-1 - Start completed.
```

### Verificar Archivos de Log

```powershell
# Ver últimas líneas del log
Get-Content logs\whats-orders-manager.log -Tail 50

# Monitorear log en tiempo real
Get-Content logs\whats-orders-manager.log -Wait -Tail 10
```

### Probar Logging en Controllers

1. **Acceder a Login:**
   ```
   GET http://localhost:8080/auth/login
   ```
   Debe aparecer en logs:
   ```
   DEBUG a.a.w.controllers.AuthController - Acceso a página de login
   ```

2. **Hacer Login:**
   ```
   POST http://localhost:8080/auth/login
   ```
   Debe aparecer:
   ```
   INFO  a.a.w.controllers.AuthController - Intento de login para usuario: admin
   INFO  a.a.w.controllers.AuthController - ✅ Login exitoso para usuario: admin
   ```

---

## 🎨 Niveles de Log - Guía Rápida

| Nivel | Cuándo Usar | Ejemplo |
|-------|-------------|---------|
| **TRACE** | Detalles extremadamente finos | Parámetros de SQL binding |
| **DEBUG** | Información de depuración | "Acceso a página de login" |
| **INFO** | Eventos importantes | "✅ Login exitoso para usuario: admin" |
| **WARN** | Situaciones anormales | "❌ Login fallido - Credenciales inválidas" |
| **ERROR** | Errores que requieren atención | "Error al conectar con base de datos" |

---

## 🛡️ Seguridad

### ⚠️ Nunca Loggear:

- ❌ Contraseñas
- ❌ Tokens de acceso
- ❌ Datos de tarjetas de crédito
- ❌ Información personal sensible (PII)
- ❌ Claves API completas

### ✅ Sí Loggear:

- ✅ IDs de usuario (no nombres completos)
- ✅ Timestamps de operaciones
- ✅ Resultados de operaciones (éxito/fallo)
- ✅ Métricas (cantidad de registros, tiempo de respuesta)
- ✅ Errores (con stack trace limitado)

---

## 📚 Referencias

- **Guía de Logging:** `docs/GUIA_LOGGING.md`
- **Spring Boot Logging:** https://docs.spring.io/spring-boot/logging.html
- **Logback Documentation:** https://logback.qos.ch/documentation.html
- **SLF4J:** https://www.slf4j.org/manual.html

---

## ✅ Checklist de Implementación

- [x] Crear `.env.example` con plantilla
- [x] Crear `.env.local` con credenciales reales
- [x] Verificar que `.env.local` está en `.gitignore`
- [x] Configurar `application.yml` con variables de entorno
- [x] Agregar configuración de logging profesional
- [x] Definir perfiles `dev` y `prod`
- [x] Configurar rotación de archivos de log
- [x] Documentar niveles de log por paquete
- [x] Crear script de carga de variables
- [x] Actualizar documentación del proyecto

---

**¡Configuración completada!** 🎉
