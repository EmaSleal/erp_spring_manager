## 📧 Configuración con Gmail (Recomendado)

### Paso 1: Habilitar verificación en dos pasos

1. Ve a tu cuenta de Google: https://myaccount.google.com/security
2. Busca "Verificación en dos pasos"
3. Actívala si no está activada

### Paso 2: Generar contraseña de aplicación

1. Ve a "Contraseñas de aplicaciones": https://myaccount.google.com/apppasswords
2. Selecciona "Correo" como aplicación
3. Selecciona "Otra" como dispositivo (escribe "ERP Orders Manager")
4. Copia la contraseña generada (16 caracteres, ej: `abcd efgh ijkl mnop`)

### Paso 3: Configurar variables de entorno

**Windows (PowerShell):**
```powershell
$env:EMAIL_HOST="smtp.gmail.com"
$env:EMAIL_PORT="587"
$env:EMAIL_USERNAME="tu-email@gmail.com"
$env:EMAIL_PASSWORD="abcd efgh ijkl mnop"
```

**Windows (CMD):**
```cmd
set EMAIL_HOST=smtp.gmail.com
set EMAIL_PORT=587
set EMAIL_USERNAME=tu-email@gmail.com
set EMAIL_PASSWORD=abcd efgh ijkl mnop
```

**Linux/Mac:**
```bash
export EMAIL_HOST=smtp.gmail.com
export EMAIL_PORT=587
export EMAIL_USERNAME=tu-email@gmail.com
export EMAIL_PASSWORD="abcd efgh ijkl mnop"
```

### Paso 4: Crear archivo .env (Opcional)

Copia `.env.example` a `.env` y completa los valores:

```env
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_USERNAME=tu-email@gmail.com
EMAIL_PASSWORD=abcd efgh ijkl mnop
```

---

