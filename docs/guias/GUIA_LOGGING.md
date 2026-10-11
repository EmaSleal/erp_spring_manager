# 📋 Guía de Buenas Prácticas de Logging

## 📅 Fecha: 26/10/2025

---

## 🎯 Objetivo

Establecer estándares consistentes para el logging en todo el proyecto WhatsApp Orders Manager, facilitando la depuración, monitoreo y auditoría del sistema.

---

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

## 🎨 Formato de Mensajes

### ✅ **Buenos ejemplos:**

```java
// ✅ Descriptivo con contexto
log.info("Usuario {} creó factura {} para cliente {}", 
         usuario.getNombre(), factura.getId(), cliente.getNombre());

// ✅ Con valores importantes
log.info("Factura {} actualizada - Total: {} → {}", 
         id, totalAnterior, totalNuevo);

// ✅ Con operación y resultado
log.info("✅ Reporte PDF generado exitosamente - {} bytes", pdfBytes.length);

// ✅ Con detalles de error
log.error("Error al exportar Excel: {} - Facturas procesadas: {}/{}", 
          e.getMessage(), procesadas, total, e);
```

### ❌ **Malos ejemplos:**

```java
// ❌ No descriptivo
log.info("Operación completada");

// ❌ Sin contexto
log.error("Error");

// ❌ Demasiado verboso
log.info("El usuario con nombre " + usuario.getNombre() + " y rol " + 
         usuario.getRol() + " ha creado una nueva factura...");

// ❌ Información sensible
log.info("Password del usuario: {}", password); // ⛔ NUNCA
```

---

## 🔐 Información Sensible - NO LOGGEAR

### ⛔ **NUNCA loggear:**
- ❌ Contraseñas (plain text o encriptadas)
- ❌ Tokens de autenticación
- ❌ Números de tarjetas de crédito
- ❌ Datos personales sensibles (sin anonimizar)
- ❌ API Keys o secrets

### ✅ **Alternativas seguras:**

```java
// ❌ MAL
log.info("Login con password: {}", password);

// ✅ BIEN
log.info("Intento de login para usuario: {}", username);

// ❌ MAL
log.info("Token generado: {}", token);

// ✅ BIEN
log.info("Token generado para usuario: {} - Expira en: {}", username, expiresIn);
```

---

## 📝 Patrones Comunes

### **1. Inicio y fin de operaciones importantes**

```java
@GetMapping("/reporte/ventas")
public String reporteVentas(...) {
    log.info("=== Generando reporte de ventas ===");
    log.info("Filtros - Inicio: {}, Fin: {}, ClienteId: {}", fechaInicio, fechaFin, clienteId);
    
    try {
        // ... lógica ...
        
        log.info("✅ Reporte generado - {} facturas encontradas", facturas.size());
        return "reportes/ventas";
    } catch (Exception e) {
        log.error("Error al generar reporte de ventas: {}", e.getMessage(), e);
        return "error/error";
    }
}
```

### **2. Operaciones CRUD**

```java
// CREATE
log.info("Creando nuevo cliente: {}", cliente.getNombre());
Cliente guardado = clienteService.save(cliente);
log.info("✅ Cliente creado exitosamente: ID {}", guardado.getIdCliente());

// UPDATE
log.info("Actualizando cliente ID: {} - Cambios: nombre, email", id);
clienteService.update(cliente);
log.info("✅ Cliente actualizado exitosamente");

// DELETE
log.info("Eliminando cliente ID: {}", id);
clienteService.delete(id);
log.info("✅ Cliente eliminado");

// READ
log.debug("Consultando cliente ID: {}", id);
Optional<Cliente> cliente = clienteService.findById(id);
log.debug("Cliente encontrado: {}", cliente.isPresent());
```

### **3. Validaciones y errores de negocio**

```java
if (!validarDatos(cliente)) {
    log.warn("Validación fallida - Cliente con datos incompletos: {}", cliente.getNombre());
    return ResponseUtil.error("Datos incompletos");
}

if (facturaRepository.existeNumero(numero)) {
    log.warn("Número de factura duplicado: {}", numero);
    throw new IllegalArgumentException("Número de factura ya existe");
}
```

### **4. Paginación y búsquedas**

```java
log.info("Listando clientes - Página: {}, Tamaño: {}, Orden: {} {}", 
         page, size, sortBy, sortDir);

Page<Cliente> resultado = clienteService.findAll(pageable);

log.info("Clientes cargados: {} de {} total", 
         resultado.getContent().size(), resultado.getTotalElements());
```

### **5. Exportaciones y generación de archivos**

```java
log.info("=== Exportando reporte a PDF ===");
log.info("Tipo: {}, Registros: {}", tipo, datos.size());

byte[] pdfBytes = exportService.generarPDF(datos);

log.info("✅ PDF generado exitosamente - {} bytes", pdfBytes.length);
```

---

## 🎯 Configuración por Ambiente

### **application.yml** (Base)
```yaml
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
  level:
    root: INFO
    api.astro.whats_orders_manager: INFO
```

### **application-dev.yml** (Desarrollo)
```yaml
logging:
  level:
    root: INFO
    api.astro.whats_orders_manager: DEBUG
    org.springframework: INFO
    org.hibernate.SQL: DEBUG
```

### **application-prod.yml** (Producción)
```yaml
logging:
  level:
    root: WARN
    api.astro.whats_orders_manager: INFO
    org.springframework: WARN
  file:
    name: /var/log/whatsapp-orders-manager/application.log
    max-size: 10MB
    max-history: 30
```

---

## ✅ Checklist de Logging

Para cada endpoint/método importante:

- [ ] ¿Tiene log de inicio con parámetros relevantes?
- [ ] ¿Tiene log de éxito con resultado?
- [ ] ¿Maneja excepciones con log.error()?
- [ ] ¿Los mensajes son descriptivos?
- [ ] ¿Incluye contexto (IDs, usuarios)?
- [ ] ¿No expone información sensible?
- [ ] ¿Usa el nivel apropiado (DEBUG/INFO/WARN/ERROR)?
- [ ] ¿Incluye emojis para facilitar lectura?

---

## 🚀 Mejoras Aplicadas

### **AuthController** (v1.1)
- ✅ Agregado @Slf4j
- ✅ Eliminado System.out.println
- ✅ Logging de intentos de login (exitosos y fallidos)
- ✅ Logging de registros de usuarios
- ✅ Uso de emojis (✅, ❌) para claridad

**Antes:**
```java
System.out.println("username: " + username);
System.out.println("password: " + password);
```

**Después:**
```java
log.info("Intento de login para usuario: {}", username);
log.info("✅ Login exitoso para usuario: {}", username);
log.warn("❌ Login fallido para usuario: {} - Razón: {}", username, e.getMessage());
```

---

## 📚 Recursos Adicionales

- **SLF4J Documentation:** https://www.slf4j.org/manual.html
- **Logback Configuration:** https://logback.qos.ch/manual/configuration.html
- **Spring Boot Logging:** https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.logging

---

## 🎓 Conclusión

Un buen logging es fundamental para:
- ✅ **Debugging** - Identificar problemas rápidamente
- ✅ **Monitoreo** - Supervisar el estado del sistema
- ✅ **Auditoría** - Rastrear operaciones críticas
- ✅ **Performance** - Detectar cuellos de botella
- ✅ **Seguridad** - Detectar intentos de acceso no autorizado

**Regla de oro:** Si no está en los logs, no pasó. Loguea lo importante, pero sin excesos.
