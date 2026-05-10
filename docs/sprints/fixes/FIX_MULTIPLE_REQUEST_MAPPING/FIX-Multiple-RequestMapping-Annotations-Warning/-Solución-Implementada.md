## ✅ Solución Implementada

### Estrategia: Separar métodos + Mover lógica al Service

#### 1. Nuevo método en Service Interface

**Archivo:** `ConfiguracionFacturacionService.java`

```java
/**
 * Guarda o actualiza una configuración de facturación según tenga o no ID.
 * Si la configuración tiene ID, se actualiza; si no, se crea una nueva.
 * 
 * Este método abstrae la lógica de decisión entre save() y update().
 * 
 * @param configuracion Configuración a guardar o actualizar
 * @return Configuración guardada/actualizada
 * @throws IllegalArgumentException si los datos son inválidos
 * @throws IllegalStateException si ya existe una configuración activa (solo para nuevas)
 */
ConfiguracionFacturacion saveOrUpdate(ConfiguracionFacturacion configuracion);
```

#### 2. Implementación en Service

**Archivo:** `ConfiguracionFacturacionServiceImpl.java`

```java
@Override
@Transactional
@CacheEvict(value = "configuracionFacturacion", allEntries = true)
public ConfiguracionFacturacion saveOrUpdate(ConfiguracionFacturacion configuracion) {
    log.debug("Guardando o actualizando configuración de facturación");
    
    // Validar datos primero
    validarConfiguracion(configuracion);
    
    ConfiguracionFacturacion resultado;
    
    if (configuracion.getId() != null && configuracion.getId() > 0) {
        // Actualizar configuración existente
        log.info("Actualizando configuración existente con ID: {}", configuracion.getId());
        resultado = update(configuracion);
    } else {
        // Crear nueva configuración
        log.info("Creando nueva configuración de facturación");
        resultado = save(configuracion);
    }
    
    return resultado;
}
```

#### 3. Controllers Refactorizados

**DESPUÉS - Controller con métodos separados:**

**Archivo:** `ConfiguracionFacturacionRestController.java`

```java
/**
 * Crea una nueva configuración de facturación
 * POST /api/configuracion/facturacion
 */
@PostMapping
public ResponseEntity<?> crearConfiguracion(@RequestBody ConfiguracionFacturacion configuracion) {
    try {
        log.info("POST /api/configuracion/facturacion - Creando configuración");
        
        ConfiguracionFacturacion guardada = configuracionFacturacionService.saveOrUpdate(configuracion);
        log.info("Configuración guardada: ID {}", guardada.getId());
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Configuración guardada exitosamente",
                "data", guardada
        ));
        
    } catch (IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("success", false, "message", e.getMessage()));
    } catch (IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("success", false, "message", e.getMessage()));
    } catch (Exception e) {
        log.error("Error al crear configuración", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error: " + e.getMessage()));
    }
}

/**
 * Actualiza la configuración de facturación existente
 * PUT /api/configuracion/facturacion
 */
@PutMapping
public ResponseEntity<?> actualizarConfiguracion(@RequestBody ConfiguracionFacturacion configuracion) {
    try {
        log.info("PUT /api/configuracion/facturacion - Actualizando configuración");
        
        ConfiguracionFacturacion guardada = configuracionFacturacionService.saveOrUpdate(configuracion);
        log.info("Configuración actualizada: ID {}", guardada.getId());
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Configuración actualizada exitosamente",
                "data", guardada
        ));
        
    } catch (IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("success", false, "message", e.getMessage()));
    } catch (IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("success", false, "message", e.getMessage()));
    } catch (Exception e) {
        log.error("Error al actualizar configuración", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error: " + e.getMessage()));
    }
}
```

---

