## 🔍 Análisis del Error

### Código Problemático

**ANTES - Controller con múltiples anotaciones:**
```java
@PostMapping
@PutMapping  // ❌ Esta anotación era IGNORADA por Spring
public ResponseEntity<?> guardarConfiguracion(@RequestBody ConfiguracionFacturacion config) {
    // Lógica de negocio mezclada con lógica de decisión
    if (config.getId() != null && config.getId() > 0) {
        guardada = service.update(config);
    } else {
        guardada = service.save(config);
    }
    return ResponseEntity.ok(guardada);
}
```

### Problemas Identificados

1. **Spring ignora segunda anotación:** Solo `@PostMapping` funcionaba, `@PutMapping` era ignorado
2. **Lógica mezclada:** El controller tenía lógica de decisión (save vs update)
3. **Violación SRP:** El método hacía dos cosas (crear y actualizar)
4. **Frontend confundido:** Ambos métodos HTTP apuntaban al mismo endpoint

---

