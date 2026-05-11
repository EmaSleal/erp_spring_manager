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
2025-10-26 14:30:15 [http-nio-9090-exec-1] DEBUG a.a.w.controllers.AuthController - Acceso a página de login
2025-10-26 14:30:16 [http-nio-9090-exec-1] DEBUG org.hibernate.SQL - select u1_0.id, u1_0.nombre from usuario u1_0 where u1_0.username=?
2025-10-26 14:30:16 [http-nio-9090-exec-1] TRACE o.h.type.descriptor.sql.BasicBinder - binding parameter [1] as [VARCHAR] - [admin]
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
2025-10-26 14:30:15 [http-nio-9090-exec-1] INFO  a.a.w.controllers.AuthController - ✅ Login exitoso para usuario: admin
2025-10-26 14:30:20 [http-nio-9090-exec-2] WARN  a.a.w.controllers.AuthController - ❌ Login fallido para usuario: hacker - Razón: Credenciales inválidas
```

---

### Perfil: Default (Sin perfil específico)

**Comportamiento:**
- Usa configuración base del `application.yml`
- Log level: `INFO` (balanceado)
- SQL queries: `DEBUG` (útil para desarrollo)
- Archivos de log: `logs/whats-orders-manager.log`

---

