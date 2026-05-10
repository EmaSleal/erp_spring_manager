## 🔮 Mejoras Futuras (Opcional)

### Opción 1: Crear SP específico
```sql
CREATE PROCEDURE sp_actualizar_estado_factura(
    IN p_id_factura INT,
    IN p_entregado BOOLEAN,
    IN p_update_by INT
)
BEGIN
    UPDATE factura
    SET entregado = p_entregado,
        update_by = p_update_by,
        update_date = CURRENT_TIMESTAMP
    WHERE id_factura = p_id_factura;
END;
```

### Opción 2: Unificar en un solo endpoint
```java
@PutMapping("/actualizar/{id}")
public ResponseEntity<String> actualizarFacturaCompleta(
    @PathVariable Integer id,
    @RequestBody FacturaUpdateDTO dto
) {
    // Actualizar factura (estado, descripción, etc.)
    // Actualizar líneas
    // Todo en una transacción
}
```

### Opción 3: Agregar validación de transición de estados
```java
// Evitar cambios no permitidos
if (facturaActual.isEntregado() && !nuevoEstado) {
    throw new InvalidStateTransitionException(
        "No se puede cambiar de Entregado a Pendiente"
    );
}
```

---

