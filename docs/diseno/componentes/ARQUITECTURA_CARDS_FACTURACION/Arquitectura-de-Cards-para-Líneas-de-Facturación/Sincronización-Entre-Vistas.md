## Sincronización Entre Vistas

### Función: `obtenerFilaLinea()` (Ubicador Versátil)

```javascript
function obtenerFilaLinea(element) {
    // Si el evento viene de la TABLA, retorna la fila directamente
    const row = element.closest("tr");
    if (row) return row;

    // Si viene de un CARD, localiza la fila correspondiente en la tabla
    const card = element.closest(".card");
    if (!card) return null;

    const idLinea = card.querySelector('input[name="idLinea"]')?.value;
    const rows = document.querySelectorAll("#lineas-body tr");
    
    return Array.from(rows).find(r => 
        r.querySelector('input[name="idLinea"]')?.value == idLinea
    );
}
```

**Uso**: Permite que manejadores de eventos funcionen desde cualquier vista.

### Función: `obtenerCardLineaPorId()` (Localizador de Card)

```javascript
function obtenerCardLineaPorId(idLinea) {
    return Array.from(
        document.querySelectorAll(".card")
    ).find(card => 
        card.querySelector('input[name="idLinea"]')?.value == idLinea
    );
}
```

### Función: `sincronizarCardLinea()` (Actualización Selectiva)

```javascript
function sincronizarCardLinea(row) {
    const idLinea = row.querySelector('input[name="idLinea"]')?.value;
    const card = idLinea ? obtenerCardLineaPorId(idLinea) : null;
    
    if (!card) return;  // No hay card (desktop view)

    // Leer valores actuales de la fila (tabla = fuente)
    const selectValue = row.querySelector('select[name="producto"]').value;
    const cantidad = row.querySelector('input[name="cantidad"]')?.value ?? '';
    const precio = row.querySelector('input[name="precio"]')?.value ?? '';
    const subtotal = row.querySelector('input[name="subtotal"]')?.value ?? '';

    // Actualizar SOLO este card (sin reconstruir todos)
    card.querySelector('select[name="producto"]').value = selectValue;
    card.querySelector('input[name="cantidad"]').value = cantidad;
    card.querySelector('input[name="precio"]').value = precio;
    card.querySelector('input[name="subtotal"]').value = subtotal;
}
```

**Contraste**:
```javascript
//  INCORRECTO: Reconstruye todo
function actualizarVistaLineas() {
    const container = document.getElementById("lineas-cards-container");
    container.innerHTML = '';  // ← Borra TODOS los cards
    
    // Recrea cada card desde cero
    // ← Problemas: Pierde foco, estado, etc.
}

//  CORRECTO: Actualiza selectivamente
function sincronizarCardLinea(row) {
    const card = obtenerCardLineaPorId(...);
    if (card) {
        card.querySelector('input').value = newValue;  // ← Solo este
    }
}
```

---

