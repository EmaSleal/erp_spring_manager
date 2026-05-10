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
$env:DB_URL="jdbc:mysql://192.168.100.93:3306/facturas_monrachem?useSSL=false&serverTimezone=UTC"
$env:DB_USERNAME="m4n0"
$env:DB_PASSWORD="Chismosear01"

# Email
$env:EMAIL_HOST="smtp.gmail.com"
$env:EMAIL_PORT="587"
$env:EMAIL_USERNAME="manusl2908@gmail.com"
$env:EMAIL_PASSWORD="syzm qsxg mmiw hdsn"

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

