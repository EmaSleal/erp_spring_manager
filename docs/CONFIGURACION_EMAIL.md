# 📧 Configuración de Email - WhatsApp Orders Manager

## 📋 Índice
- [Introducción](#introducción)
- [Requisitos](#requisitos)
- [Configuración con Gmail](#configuración-con-gmail)
- [Configuración con Outlook](#configuración-con-outlook)
- [Otras opciones](#otras-opciones)
- [Variables de entorno](#variables-de-entorno)
- [Verificación](#verificación)
- [Troubleshooting](#troubleshooting)

---

## 🎯 Introducción

Este proyecto utiliza **Spring Boot Mail** para enviar notificaciones por email. La configuración se realiza mediante variables de entorno para mayor seguridad.

**Funcionalidades de email:**
- ✉️ Enviar facturas por email
- 🔑 Enviar credenciales a nuevos usuarios
- ⏰ Recordatorios de pago automáticos
- 📊 Notificaciones de sistema

---

## ✅ Requisitos

1. **Cuenta de email activa** (Gmail, Outlook, Yahoo, o servidor SMTP propio)
2. **Contraseña de aplicación** (no usar contraseña normal de email)
3. **Variables de entorno configuradas**

---

## 📧 Configuración con Gmail (Recomendado)

### Paso 1: Habilitar verificación en dos pasos

1. Ve a tu cuenta de Google: https://myaccount.google.com/security
2. Busca "Verificación en dos pasos"
3. Actívala si no está activada

### Paso 2: Generar contraseña de aplicación

1. Ve a "Contraseñas de aplicaciones": https://myaccount.google.com/apppasswords
2. Selecciona "Correo" como aplicación
3. Selecciona "Otra" como dispositivo (escribe "WhatsApp Orders Manager")
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

## 📨 Configuración con Outlook/Hotmail

### Variables de entorno:

```bash
EMAIL_HOST=smtp-mail.outlook.com
EMAIL_PORT=587
EMAIL_USERNAME=tu-email@outlook.com
EMAIL_PASSWORD=tu-contraseña
```

**Nota:** Outlook permite usar la contraseña normal de la cuenta (no requiere contraseña de aplicación).

---

## 🌐 Otras Opciones

### Yahoo Mail

```bash
EMAIL_HOST=smtp.mail.yahoo.com
EMAIL_PORT=465
EMAIL_USERNAME=tu-email@yahoo.com
EMAIL_PASSWORD=tu-contraseña-de-aplicacion
```

### Servidor SMTP Personalizado

```bash
EMAIL_HOST=smtp.tudominio.com
EMAIL_PORT=587
EMAIL_USERNAME=tu-email@tudominio.com
EMAIL_PASSWORD=tu-contraseña
```

---

## 🔧 Variables de Entorno

### Tabla de configuración

| Variable | Descripción | Valor por defecto | Requerido |
|----------|-------------|-------------------|-----------|
| `EMAIL_HOST` | Servidor SMTP | `smtp.gmail.com` | ✅ |
| `EMAIL_PORT` | Puerto SMTP (587=TLS, 465=SSL) | `587` | ✅ |
| `EMAIL_USERNAME` | Dirección de email | - | ✅ |
| `EMAIL_PASSWORD` | Contraseña de aplicación | - | ✅ |

### Métodos de configuración

#### 1. Variables de entorno del sistema

**Ventaja:** Más seguro, no se guardan en archivos  
**Desventaja:** Hay que configurarlas en cada terminal

#### 2. Archivo .env

**Ventaja:** Fácil de configurar, persiste entre sesiones  
**Desventaja:** Hay que tener cuidado de no subirlo al repositorio

#### 3. IDE (IntelliJ/Eclipse)

Configura las variables en "Run Configuration" → "Environment Variables"

---

## ✅ Verificación

### 1. Verificar que las variables están configuradas

**PowerShell:**
```powershell
echo $env:EMAIL_USERNAME
```

**Linux/Mac:**
```bash
echo $EMAIL_USERNAME
```

### 2. Iniciar la aplicación

```bash
mvn spring-boot:run
```

### 3. Ver logs de configuración

Busca en los logs:
```
JavaMailSender has been initialized
```

### 4. Probar envío de email

En el módulo de **Configuración → Notificaciones** habrá un botón "Probar Email" que enviará un email de prueba.

---

## 🐛 Troubleshooting

### Error: "Authentication failed"

**Causa:** Credenciales incorrectas o contraseña de aplicación mal configurada.

**Solución:**
1. Verifica que `EMAIL_USERNAME` sea correcto
2. Verifica que `EMAIL_PASSWORD` sea la contraseña de aplicación (no la contraseña normal)
3. Verifica que la verificación en dos pasos esté activa (Gmail)

### Error: "Could not connect to SMTP host"

**Causa:** Host o puerto incorrecto, o firewall bloqueando.

**Solución:**
1. Verifica `EMAIL_HOST` y `EMAIL_PORT`
2. Verifica tu conexión a internet
3. Verifica que tu firewall no bloquee el puerto 587/465

### Error: "Connection timed out"

**Causa:** Puerto bloqueado o configuración SSL/TLS incorrecta.

**Solución:**
1. Intenta cambiar el puerto:
   - Puerto 587 (TLS) → Puerto 465 (SSL)
   - O viceversa
2. Verifica configuración de firewall

### Las variables no se cargan

**Causa:** Variables no configuradas en la sesión actual.

**Solución Windows:**
```powershell
# Configurar permanentemente (requiere reiniciar terminal)
[System.Environment]::SetEnvironmentVariable("EMAIL_USERNAME", "tu-email@gmail.com", "User")
[System.Environment]::SetEnvironmentVariable("EMAIL_PASSWORD", "tu-password", "User")
```

**Solución Linux/Mac:**
```bash
# Agregar a ~/.bashrc o ~/.zshrc
echo 'export EMAIL_USERNAME="tu-email@gmail.com"' >> ~/.bashrc
echo 'export EMAIL_PASSWORD="tu-password"' >> ~/.bashrc
source ~/.bashrc
```

---

## 🔒 Seguridad

### ✅ Mejores prácticas

1. **NUNCA** subas archivos `.env` al repositorio
2. **NUNCA** hardcodees credenciales en el código
3. **SIEMPRE** usa contraseñas de aplicación (Gmail)
4. **SIEMPRE** mantén `.env` en `.gitignore`
5. **ROTACIÓN:** Cambia las contraseñas periódicamente

### 🚫 NO hacer

```java
// ❌ MAL - Hardcodear credenciales
spring.mail.username=mi-email@gmail.com
spring.mail.password=mi-password-secreta
```

```java
// ✅ BIEN - Usar variables de entorno
spring.mail.username=${EMAIL_USERNAME}
spring.mail.password=${EMAIL_PASSWORD}
```

---

## 📚 Referencias

- [Spring Boot Mail Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/messaging.html#messaging.email)
- [Gmail App Passwords](https://support.google.com/accounts/answer/185833)
- [Outlook SMTP Settings](https://support.microsoft.com/en-us/office/pop-imap-and-smtp-settings-8361e398-8af4-4e97-b147-6c6c4ac95353)

---

## 🆘 Soporte

Si tienes problemas con la configuración de email:

1. Revisa la sección de [Troubleshooting](#troubleshooting)
2. Verifica los logs de la aplicación
3. Contacta al administrador del sistema

---

**Última actualización:** 13 de octubre de 2025  
**Versión:** 1.0
