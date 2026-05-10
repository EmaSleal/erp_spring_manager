## 🚀 Cómo Usar

### Primera Vez (Configuración Inicial)

```powershell
# 1. Copiar plantilla
Copy-Item .env.example .env.local

# 2. Editar credenciales
notepad .env.local

# 3. Completar:
#    - DB_URL, DB_USERNAME, DB_PASSWORD
#    - EMAIL_USERNAME, EMAIL_PASSWORD (App Password de Gmail)
#    - META_WHATSAPP_* (si aplica)

# 4. Cargar variables
.\load-env.ps1

# 5. Verificar
$env:DB_URL  # Debe mostrar tu URL de MySQL
```

---

### Ejecutar Aplicación (Desarrollo)

**Opción 1: Script automático (Recomendado)**
```powershell
.\start.ps1
```

**Opción 2: Manual**
```powershell
# Cargar variables
.\load-env.ps1

# Establecer perfil
$env:SPRING_PROFILES_ACTIVE="dev"

# Ejecutar
.\mvnw spring-boot:run
```

---

### Ejecutar Aplicación (Producción)

```powershell
# 1. Cargar variables
.\load-env.ps1

# 2. Compilar JAR
.\mvnw clean package -DskipTests

# 3. Establecer perfil
$env:SPRING_PROFILES_ACTIVE="prod"

# 4. Ejecutar JAR
java -jar target\whats-orders-manager.jar
```

---

### Ver Logs

**En consola:**
Los logs aparecen automáticamente en la consola al ejecutar la aplicación.

**En archivo:**
```powershell
# Ver últimas 50 líneas
Get-Content logs\whats-orders-manager.log -Tail 50

# Monitorear en tiempo real
Get-Content logs\whats-orders-manager.log -Wait -Tail 20

# Buscar errores
Select-String -Path logs\whats-orders-manager.log -Pattern "ERROR"

# Buscar por usuario
Select-String -Path logs\whats-orders-manager.log -Pattern "admin"
```

---

