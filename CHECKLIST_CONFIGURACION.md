# ✅ Checklist de Verificación - Configuración Completa

## 📋 Pre-requisitos

- [ ] Tener instalado JDK 17 o superior
- [ ] Tener instalado Maven
- [ ] Tener acceso a MySQL (192.168.100.8:3306)
- [ ] Tener cuenta de Gmail con App Password generado
- [ ] (Opcional) Tener credenciales de WhatsApp Business API

---

## 🔐 Configuración de Seguridad

### 1. Variables de Entorno

- [ ] Verificar que `.env.local` existe
  ```powershell
  Test-Path .\.env.local
  # Debe retornar: True
  ```

- [ ] Verificar que `.env.local` está en `.gitignore`
  ```powershell
  git check-ignore .env.local
  # Debe retornar: .env.local
  ```

- [ ] Verificar que `.env.local` NO está en el repositorio
  ```powershell
  git status
  # .env.local NO debe aparecer
  ```

### 2. Credenciales Completas

Abrir `.env.local` y verificar que todas las variables tienen valores:

- [ ] `DB_URL` - URL completa de MySQL
- [ ] `DB_USERNAME` - Usuario de base de datos
- [ ] `DB_PASSWORD` - Contraseña de base de datos
- [ ] `EMAIL_HOST` - smtp.gmail.com
- [ ] `EMAIL_PORT` - 587
- [ ] `EMAIL_USERNAME` - Tu email de Gmail
- [ ] `EMAIL_PASSWORD` - App Password (16 caracteres sin espacios)
- [ ] `META_WHATSAPP_PHONE_NUMBER_ID` - ID de WhatsApp (si aplica)
- [ ] `META_WHATSAPP_ACCESS_TOKEN` - Token de acceso (si aplica)
- [ ] `META_WEBHOOK_VERIFY_TOKEN` - Token de verificación

---

## 🔧 Configuración de Logging

### 1. Archivo application.yml

- [ ] Verificar que `application.yml` usa variables de entorno
  ```yaml
  datasource:
    url: ${DB_URL:...}
    username: ${DB_USERNAME:...}
    password: ${DB_PASSWORD:...}
  ```

- [ ] Verificar configuración de logging
  ```yaml
  logging:
    level:
      api.astro.whats_orders_manager: INFO
  ```

- [ ] Verificar perfiles `dev` y `prod` configurados

### 2. Estructura de Directorios

- [ ] Crear directorio de logs si no existe
  ```powershell
  New-Item -ItemType Directory -Force -Path "logs"
  ```

---

## 🚀 Prueba de Carga de Variables

### 1. Ejecutar Script de Carga

- [ ] Ejecutar `load-env.ps1`
  ```powershell
  .\load-env.ps1
  ```

- [ ] Verificar output del script:
  - ✅ Variables cargadas: X
  - ✅ DB_URL = jdbc:mysql://...
  - ✅ EMAIL_HOST = smtp.gmail.com

### 2. Verificar Variables Manualmente

- [ ] Ver variables de base de datos
  ```powershell
  Get-ChildItem Env: | Where-Object { $_.Name -like "DB_*" }
  ```

- [ ] Ver variables de email
  ```powershell
  Get-ChildItem Env: | Where-Object { $_.Name -like "EMAIL_*" }
  ```

- [ ] Verificar valor de DB_URL
  ```powershell
  $env:DB_URL
  # Debe mostrar: jdbc:mysql://192.168.100.8:3306/...
  ```

---

## 🏃 Prueba de Ejecución

### 1. Compilación

- [ ] Compilar sin errores
  ```powershell
  .\mvnw clean compile
  ```
  
  Resultado esperado:
  ```
  [INFO] BUILD SUCCESS
  ```

### 2. Ejecución con Perfil DEV

- [ ] Cargar variables
  ```powershell
  .\load-env.ps1
  ```

- [ ] Establecer perfil dev
  ```powershell
  $env:SPRING_PROFILES_ACTIVE="dev"
  ```

- [ ] Ejecutar aplicación
  ```powershell
  .\mvnw spring-boot:run
  ```

- [ ] Verificar en logs:
  - [ ] Conexión a base de datos exitosa
    ```
    HikariPool-1 - Start completed.
    ```
  
  - [ ] Servidor iniciado
    ```
    Tomcat started on port(s): 8080 (http)
    ```
  
  - [ ] SQL queries visibles (perfil dev)
    ```
    Hibernate: select ...
    ```

### 3. Prueba de Endpoints

- [ ] Acceder a login
  ```
  http://localhost:8080/auth/login
  ```
  
  Verificar en logs:
  ```
  DEBUG a.a.w.controllers.AuthController - Acceso a página de login
  ```

- [ ] Hacer login con credenciales válidas
  
  Verificar en logs:
  ```
  INFO  a.a.w.controllers.AuthController - Intento de login para usuario: admin
  INFO  a.a.w.controllers.AuthController - ✅ Login exitoso para usuario: admin
  ```

- [ ] Intentar login con credenciales inválidas
  
  Verificar en logs:
  ```
  WARN  a.a.w.controllers.AuthController - ❌ Login fallido para usuario: test
  ```

---

## 📊 Verificación de Archivos de Log

### 1. Archivo de Log Creado

- [ ] Verificar que existe el archivo
  ```powershell
  Test-Path logs\whats-orders-manager.log
  # Debe retornar: True
  ```

### 2. Contenido del Log

- [ ] Ver últimas 20 líneas
  ```powershell
  Get-Content logs\whats-orders-manager.log -Tail 20
  ```

- [ ] Verificar formato correcto
  ```
  2025-10-26 14:30:15 [thread] LEVEL logger - mensaje
  ```

### 3. Monitoreo en Tiempo Real

- [ ] Monitorear logs en tiempo real
  ```powershell
  Get-Content logs\whats-orders-manager.log -Wait -Tail 10
  ```

- [ ] Realizar acciones en la aplicación
- [ ] Verificar que los logs aparecen inmediatamente

---

## 🎭 Prueba de Perfiles

### Perfil: DEV

- [ ] Establecer perfil
  ```powershell
  $env:SPRING_PROFILES_ACTIVE="dev"
  ```

- [ ] Ejecutar aplicación

- [ ] Verificar características:
  - [ ] SQL queries visibles en consola
  - [ ] Parámetros de queries mostrados (TRACE)
  - [ ] Logs detallados (DEBUG level)

### Perfil: PROD

- [ ] Establecer perfil
  ```powershell
  $env:SPRING_PROFILES_ACTIVE="prod"
  ```

- [ ] Compilar JAR
  ```powershell
  .\mvnw clean package -DskipTests
  ```

- [ ] Ejecutar JAR
  ```powershell
  java -jar target\whats-orders-manager.jar
  ```

- [ ] Verificar características:
  - [ ] SQL queries ocultos
  - [ ] Solo WARN y ERROR en consola
  - [ ] Mayor rendimiento (menos I/O)

---

## 🔍 Verificación de Seguridad

### 1. Logs NO deben contener:

- [ ] Contraseñas en texto plano
  ```powershell
  Select-String -Path logs\whats-orders-manager.log -Pattern "password.*=" -CaseSensitive
  # No debe encontrar nada
  ```

- [ ] Tokens de acceso
  ```powershell
  Select-String -Path logs\whats-orders-manager.log -Pattern "token.*=" -CaseSensitive
  # No debe encontrar nada
  ```

### 2. Variables de Entorno

- [ ] `.env.local` tiene permisos restrictivos (solo lectura para ti)
  
- [ ] `.env.local` NO está en historial de Git
  ```powershell
  git log --all --full-history -- .env.local
  # No debe retornar nada
  ```

---

## 📚 Documentación

### 1. Archivos de Documentación Existen

- [ ] `INICIO_RAPIDO.md` existe
- [ ] `docs/CONFIGURACION_ENV_LOGGING.md` existe
- [ ] `docs/GUIA_LOGGING.md` existe
- [ ] `docs/ESTADO_PROYECTO.md` actualizado

### 2. Contenido Correcto

- [ ] `INICIO_RAPIDO.md` tiene instrucciones claras
- [ ] `CONFIGURACION_ENV_LOGGING.md` tiene ejemplos completos
- [ ] `GUIA_LOGGING.md` tiene niveles de log documentados
- [ ] `ESTADO_PROYECTO.md` refleja últimas mejoras

---

## 🎯 Test Final - Flujo Completo

### Desde Cero (Simular Nuevo Desarrollador)

1. [ ] Clonar repositorio (simular)
2. [ ] Copiar `.env.example` a `.env.local`
   ```powershell
   Copy-Item .env.example .env.local
   ```
3. [ ] Editar `.env.local` con credenciales
4. [ ] Cargar variables
   ```powershell
   .\load-env.ps1
   ```
5. [ ] Verificar variables cargadas
6. [ ] Ejecutar aplicación
   ```powershell
   .\start.ps1
   ```
7. [ ] Acceder a `http://localhost:8080`
8. [ ] Hacer login
9. [ ] Verificar logs
10. [ ] Todo funciona ✅

---

## ✅ Resultado Final

Si todos los checks están marcados:

```
╔════════════════════════════════════════════════════════╗
║                                                        ║
║     ✅ CONFIGURACIÓN VERIFICADA Y FUNCIONANDO         ║
║                                                        ║
║     Tu aplicación está lista para:                    ║
║     • Desarrollo seguro                               ║
║     • Logging profesional                             ║
║     • Deploy a producción                             ║
║                                                        ║
╚════════════════════════════════════════════════════════╝
```

---

## 🚨 Solución de Problemas

### Error: "Could not find .env.local"

```powershell
Copy-Item .env.example .env.local
notepad .env.local
```

### Error: "Access denied for user"

Verificar en `.env.local`:
- DB_USERNAME correcto
- DB_PASSWORD correcto
- MySQL corriendo en 192.168.100.8:3306

### Error: "Authentication failed" (Email)

1. Generar App Password: https://myaccount.google.com/apppasswords
2. Usar ese password (NO tu contraseña normal)
3. Actualizar `EMAIL_PASSWORD` en `.env.local`

### No aparecen SQL queries

Verificar perfil:
```powershell
$env:SPRING_PROFILES_ACTIVE
# Debe ser "dev" para ver queries
```

### Logs no se guardan en archivo

Verificar que existe el directorio:
```powershell
New-Item -ItemType Directory -Force -Path "logs"
```

---

**Fecha de checklist:** 26 de octubre de 2025  
**Versión:** 1.0
