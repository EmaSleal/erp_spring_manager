## Sistema de IDs

### Reto Original
El sistema usaba `Date.now()` para IDs temporales de nuevas líneas:
```javascript
const id = Date.now();  // ≈ 1778175998678 (13 dígitos)
// PROBLEMA: En Java, Integer.MAX_VALUE = 2,147,483,647 (solo 10 dígitos)
// → Error: "Numeric value out of range of int"
```

### Solución: Números Negativos Decrecientes

```javascript
let temporalLineaId = -1;  // Inicializa en -1

function addLinea() {
    const randomId = temporalLineaId--;  // Primera línea: -1, segunda: -2, tercera: -3, etc.
    // ...
}
```

### Ventajas
- Nunca excede `Integer.MAX_VALUE`  
- Fácil de identificar: negativos = temporales, positivos = persistidos  
- Números únicos y predecibles dentro de la misma sesión  
- Garantiza no conflictuar con IDs de BD

### Ciclo de Vida de IDs

```
Línea Nueva          Línea Existente
   ↓                      ↓
   -1 (temporal)    id_linea_factura > 0 (de BD)
   ↓                      ↓
[Usuario agrega]    [Carga inicial]
   ↓                      ↓
Guardada en Tabla   Guardada en Tabla
   ↓                      ↓
Enviada al POST    Enviada al PUT
   ↓                      ↓
   ↓ (Servidor retorna id_linea_factura real)
   ↓ (Tabla refresca con nuevo ID)
```

---

