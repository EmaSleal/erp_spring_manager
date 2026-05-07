## 🔧 Mejoras Futuras Opcionales

### 1. DTO específicos para Create y Update

```java
// Crear
@PostMapping
public ResponseEntity<?> crearConfiguracion(@RequestBody ConfiguracionCreateDTO dto) { }

// Actualizar (requiere ID)
@PutMapping("/{id}")
public ResponseEntity<?> actualizarConfiguracion(
    @PathVariable Integer id, 
    @RequestBody ConfiguracionUpdateDTO dto) { }
```

### 2. Validaciones con Bean Validation

```java
@PostMapping
public ResponseEntity<?> crearConfiguracion(
    @Valid @RequestBody ConfiguracionFacturacion config) {
    // Spring validará automáticamente
}
```

### 3. PATCH para actualizaciones parciales

```java
@PatchMapping("/{id}")
public ResponseEntity<?> actualizarParcial(
    @PathVariable Integer id,
    @RequestBody Map<String, Object> updates) {
    // Solo actualiza campos enviados
}
```

---

