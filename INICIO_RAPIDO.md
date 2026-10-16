# 🚀 Inicio Rápido - Variables de Entorno

## 📋 Configuración Inicial (Solo la primera vez)

### 1. Copiar plantilla de variables de entorno

```powershell
Copy-Item .env.example .env.local
```

### 2. Editar `.env.local` con tus credenciales

```powershell
notepad .env.local
```

Completa los valores:
- **DB_URL**, **DB_USERNAME**, **DB_PASSWORD** - Credenciales de MySQL
- **META_WHATSAPP_*** - Credenciales de WhatsApp Business API

El SMTP no va en variables de entorno: se configura en **Configuración → Email** (host, usuario, contraseña de aplicación), se prueba con el botón de prueba y luego se activa el envío.

### 3. Verificar que `.env.local` no se commitee

```powershell
git status
# .env.local NO debe aparecer en la lista
# Si aparece, asegúrate de que esté en .gitignore
```

---

## ▶️ Ejecutar la Aplicación

### Opción 1: Script Automático (Recomendado)

```powershell
.\start.ps1
```

Este script:
- ✅ Carga automáticamente las variables de `.env.local`
- ✅ Verifica que el archivo existe
- ✅ Inicia la aplicación con Maven

---

### Opción 2: Manual (Desarrollo)

```powershell
# 1. Cargar variables de entorno
.\load-env.ps1

# 2. Ejecutar con Maven (perfil dev)
$env:SPRING_PROFILES_ACTIVE="dev"
.\mvnw spring-boot:run
```

---

### Opción 3: Manual (Producción)

```powershell
# 1. Cargar variables de entorno
.\load-env.ps1

# 2. Compilar JAR
.\mvnw clean package -DskipTests

# 3. Ejecutar JAR (perfil prod)
$env:SPRING_PROFILES_ACTIVE="prod"
java -jar target\whats-orders-manager.jar
```

---

## 🔍 Verificar Configuración

### Ver variables cargadas

```powershell
# Base de datos
Get-ChildItem Env: | Where-Object { $_.Name -like "DB_*" }

# WhatsApp
Get-ChildItem Env: | Where-Object { $_.Name -like "META_*" }
```

### Ver logs de la aplicación

```powershell
# Ver archivo de log
Get-Content logs\whats-orders-manager.log -Tail 50

# Monitorear en tiempo real
Get-Content logs\whats-orders-manager.log -Wait -Tail 20
```

---

## 🎭 Perfiles de Spring

### Desarrollo (logs detallados)

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
.\mvnw spring-boot:run
```

**Características:**
- 🔍 Log level: DEBUG
- 📊 SQL queries visibles
- 🎨 Formato colorizado en consola

---

### Producción (logs optimizados)

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
java -jar target\whats-orders-manager.jar
```

**Características:**
- ⚠️ Log level: WARN (solo alertas)
- 🚀 Mayor rendimiento
- 💾 Logs guardados en `/var/log/`

---

## 📚 Documentación Completa

Para más detalles, consulta:

- **Configuración completa:** `docs/CONFIGURACION_ENV_LOGGING.md`
- **Guía de logging:** `docs/GUIA_LOGGING.md`
- **Estado del proyecto:** `docs/ESTADO_PROYECTO.md`

---

## ⚠️ Solución de Problemas

### Error: "No se encontró .env.local"

```powershell
Copy-Item .env.example .env.local
notepad .env.local
```

### Error: "Access denied for user"

Verifica en `.env.local`:
- ✅ `DB_USERNAME` es correcto
- ✅ `DB_PASSWORD` es correcto
- ✅ `DB_URL` apunta al servidor correcto

### Error: "Authentication failed" (Email)

Para Gmail:
1. Ve a https://myaccount.google.com/apppasswords
2. Genera una "Contraseña de aplicación"
3. Guárdala en **Configuración → Email** (no tu contraseña normal) y ejecuta la prueba de envío

---

## 🔒 Seguridad

### ⛔ NUNCA hacer:

- ❌ Commitear `.env.local` al repositorio
- ❌ Compartir credenciales en Slack/Email
- ❌ Usar contraseñas reales de Gmail (usa App Passwords)
- ❌ Dejar credenciales en capturas de pantalla

### ✅ SÍ hacer:

- ✅ Mantener `.env.local` en `.gitignore`
- ✅ Usar variables de entorno del servidor en producción
- ✅ Rotar tokens periódicamente
- ✅ Usar App Passwords para Gmail

---

**¡Listo para comenzar!** 🎉
