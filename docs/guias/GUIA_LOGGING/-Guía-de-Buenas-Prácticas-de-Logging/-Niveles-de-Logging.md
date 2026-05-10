## 📊 Niveles de Logging

### 🔍 **DEBUG** - Información de depuración detallada

**Cuándo usar:**
- Acceso a vistas/formularios
- Valores de parámetros en desarrollo
- Flujo de ejecución interno
- Información técnica detallada

**Ejemplos:**
```java
log.debug("Acceso a formulario de registro");
log.debug("Ordenando por campo: {} en dirección: {}", sortBy, sortDir);
log.debug("Usuario cargado: {} ({})", usuario.getNombre(), usuario.getRol());
```

**Configuración:** Activar solo en desarrollo (`application-dev.yml`)

---

### ℹ️ **INFO** - Eventos importantes del sistema

**Cuándo usar:**
- Inicio/fin de operaciones importantes
- Operaciones CRUD exitosas
- Autenticación y autorización
- Cambios de estado del sistema
- Métricas de negocio

**Ejemplos:**
```java
log.info("Usuario {} inició sesión correctamente", username);
log.info("Cliente creado: ID {} - {}", cliente.getIdCliente(), cliente.getNombre());
log.info("Factura {} actualizada a estado: {}", facturaId, nuevoEstado);
log.info("Generando reporte de ventas - Periodo: {} a {}", fechaInicio, fechaFin);
```

**Emojis recomendados para operaciones:**
- ✅ `log.info("✅ Operación exitosa: ...")`
- 📧 `log.info("📧 Email enviado a: ...")`
- 💾 `log.info("💾 Datos guardados: ...")`
- 🔄 `log.info("🔄 Proceso ejecutado: ...")`
- 📊 `log.info("📊 Reporte generado: ...")`

---

### ⚠️ **WARN** - Situaciones anormales que no son errores

**Cuándo usar:**
- Validaciones fallidas
- Recursos no encontrados (sin impacto crítico)
- Operaciones deprecadas
- Límites cercanos a alcanzarse
- Intentos fallidos de autenticación

**Ejemplos:**
```java
log.warn("Intento de login fallido para usuario: {}", username);
log.warn("Cliente no encontrado con ID: {}", clienteId);
log.warn("Las contraseñas no coinciden para usuario: {}", usuario.getNombre());
log.warn("Stock bajo para producto ID: {} - Stock actual: {}", productoId, stock);
```

**Emojis recomendados:**
- ⚠️ `log.warn("⚠️ Advertencia: ...")`
- ❌ `log.warn("❌ Operación fallida: ...")`

---

### 🔥 **ERROR** - Errores que requieren atención

**Cuándo usar:**
- Excepciones no manejadas
- Fallos en operaciones críticas
- Errores de integración (DB, APIs externas)
- Pérdida de datos
- Problemas de configuración

**Ejemplos:**
```java
log.error("Error al guardar factura: {}", e.getMessage(), e);
log.error("Error al conectar con base de datos: {}", e.getMessage());
log.error("Error al enviar email a {}: {}", destinatario, e.getMessage(), e);
log.error("Error inesperado en scheduler de recordatorios", e);
```

**⚠️ IMPORTANTE:** Siempre incluir la excepción como último parámetro para el stack trace.

---

