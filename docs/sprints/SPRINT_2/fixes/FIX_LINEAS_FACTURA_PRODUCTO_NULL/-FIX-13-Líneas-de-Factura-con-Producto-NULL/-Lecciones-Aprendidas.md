## 💡 Lecciones Aprendidas

### 1. Validación en Frontend

**Problema:** Se confiaba en que el usuario siempre llenaría todos los campos.

**Solución:** Implementar validaciones defensivas que detecten datos incompletos o inválidos.

**Buena práctica:**
```javascript
// ✅ Validar antes de enviar
if (!productoSeleccionado || !idProductoValido) {
    return; // Omitir línea inválida
}
```

### 2. IDs Temporales

**Problema:** Usar `Date.now()` como ID temporal puede causar confusión.

**Mejora futura:**
- Usar IDs negativos para temporales (ej: -1, -2, -3)
- O usar prefijo: "temp_1", "temp_2"

**Ejemplo:**
```javascript
// Mejor alternativa
let tempIdCounter = -1;
function addLinea() {
    const tempId = tempIdCounter--;
    // tempId = -1, -2, -3, etc.
}
```

### 3. Feedback al Usuario

**Mejora:** El fix incluye mensajes claros cuando hay líneas incompletas.

```javascript
if (lineas.length === 0) {
    Swal.fire({
        title: 'Sin productos',
        text: 'Debe seleccionar al menos un producto válido'
    });
}
```

---

