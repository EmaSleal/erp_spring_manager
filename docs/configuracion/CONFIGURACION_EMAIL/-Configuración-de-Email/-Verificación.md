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

