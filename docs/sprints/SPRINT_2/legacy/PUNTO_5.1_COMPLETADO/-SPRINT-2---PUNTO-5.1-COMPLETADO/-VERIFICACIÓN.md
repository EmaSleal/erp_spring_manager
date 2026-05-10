## 🧪 VERIFICACIÓN

### 1. Verificar compilación

```bash
mvn clean compile -DskipTests
```

**Resultado esperado:**
```
[INFO] BUILD SUCCESS
```

---

### 2. Verificar configuración

**Iniciar aplicación:**
```bash
mvn spring-boot:run
```

**Buscar en logs:**
```
JavaMailSender has been initialized
```

---

### 3. Configurar variables (ejemplo con Gmail)

**Windows PowerShell:**
```powershell
$env:EMAIL_HOST="smtp.gmail.com"
$env:EMAIL_PORT="587"
$env:EMAIL_USERNAME="tu-email@gmail.com"
$env:EMAIL_PASSWORD="tu-contraseña-de-aplicacion"
```

**Linux/Mac:**
```bash
export EMAIL_HOST="smtp.gmail.com"
export EMAIL_PORT="587"
export EMAIL_USERNAME="tu-email@gmail.com"
export EMAIL_PASSWORD="tu-contraseña-de-aplicacion"
```

---

