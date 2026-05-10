## Patrones de Eventos

### Patrón 1: Cambio en Tabla → Sincronizar Card

```html
<!-- Fila de tabla -->
<tr>
    <select name="producto" onchange="actualizarProductoSeleccionado(this)">
        <!-- El evento `this` apunta a este select -->
    </select>
    <input name="cantidad" oninput="actualizarProductoSeleccionado(this)">
</tr>
```

**Manejador**:
```javascript
function actualizarProductoSeleccionado(element) {
    const row = obtenerFilaLinea(element);  // Encuentra la fila
    // ... actualiza tabla ...
    sincronizarCardLinea(row);  // Sincroniza SOLO el card asociado
}
```

### Patrón 2: Cambio en Card → Sincronizar Tabla

El mismo HTML funciona en card porque `obtenerFilaLinea()` sabe encontrar la fila correspondiente:

```html
<!-- Card -->
<select name="producto" onchange="actualizarProductoSeleccionado(this)">
    <!-- El evento `this` apunta a este select del card -->
</select>
```

Cuando se ejecuta `actualizarProductoSeleccionado()`:
1. `obtenerFilaLinea(this)` busca por `.closest("tr")` ← No encuentra (es card)
2. Luego busca por `.closest(".card")` ← Encuentra el card
3. Extrae `idLinea` del card
4. Busca la fila en tabla con ese mismo `idLinea`
5. Actualiza la fila
6. Sincroniza el card

### Patrón 3: Event Delegation para Elementos Dinámicos

Los cards se crean dinámicamente con `insertAdjacentHTML()`, así que no tenemos referencias a los elementos nuevos.

**Solución**: Los eventos se definen en el HTML inyectado:

```javascript
function createLineaCard(linea) {
    return `
    <div class="card mb-3">
        <select onchange="actualizarProductoSeleccionado(this)">
            <!-- Este onchange ya está en el HTML inyectado -->
        </select>
        <button onclick="removeLinea(this)" class="btn btn-danger">
            <!-- Este onclick ya está en el HTML inyectado -->
        </button>
    </div>
    `;
}
```

Los listeners `onchange`, `oninput`, `onclick` en atributos HTML se adjuntan a elementos nuevos automáticamente.

---

