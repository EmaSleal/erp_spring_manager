## 📊 Configuración de Logging

### Niveles Implementados

| Nivel | Paquete | Cuándo Se Usa |
|-------|---------|---------------|
| **INFO** | `root` | Eventos generales del sistema |
| **INFO** | `api.astro.whats_orders_manager` | Operaciones importantes |
| **INFO** | `*.controllers` | Requests HTTP, operaciones de usuario |
| **INFO** | `*.services` | Lógica de negocio, procesamiento |
| **DEBUG** | `*.repositories` | Acceso a datos, queries |
| **DEBUG** | `org.hibernate.SQL` | SQL queries generadas |
| **TRACE** | `org.hibernate.type.descriptor.sql.BasicBinder` | Parámetros de SQL |
| **INFO** | `org.springframework.web` | Eventos de Spring MVC |
| **DEBUG** | `org.springframework.jdbc.core` | JDBC operations |

---

### Formato de Log

**Patrón:**
```
%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
```

**Ejemplo Real:**
```
2025-10-26 14:30:15 [http-nio-9090-exec-1] INFO  a.a.w.controllers.AuthController - ✅ Login exitoso para usuario: admin
2025-10-26 14:30:16 [http-nio-9090-exec-2] DEBUG org.hibernate.SQL - select u1_0.id, u1_0.nombre from usuario u1_0 where u1_0.username=?
2025-10-26 14:30:16 [http-nio-9090-exec-2] TRACE o.h.type.descriptor.sql.BasicBinder - binding parameter [1] as [VARCHAR] - [admin]
```

---

### Archivos de Log

**Configuración:**
- **Ruta:** `logs/whats-orders-manager.log`
- **Tamaño máximo por archivo:** 10 MB
- **Archivos históricos:** 30 días
- **Límite total:** 1 GB

**Rotación automática:**
```
logs/
  whats-orders-manager.log           (actual)
  whats-orders-manager.2025-10-25.log
  whats-orders-manager.2025-10-24.log
  ...
```

---

### Diferencias por Perfil

#### DEV (Desarrollo)
```
🔍 Muy detallado
✅ SQL queries visibles
✅ Parámetros mostrados
✅ DEBUG level
✅ Consola colorizada
📁 Logs en: logs/whats-orders-manager.log
```

#### PROD (Producción)
```
⚠️ Solo alertas y errores
❌ SQL queries ocultos
❌ Sin parámetros
⚠️ WARN level
🚀 Mayor rendimiento
📁 Logs en: /var/log/whats-orders-manager/application.log
```

---

