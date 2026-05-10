## Troubleshooting

###  Problema: Se borran todos los cards al editar uno

**Causa**: Llamar `actualizarVistaLineas()` en manejador de evento

**Solución**: Usar `sincronizarCardLinea()` para actualización selectiva

```javascript
//  MALO
function actualizarProductoSeleccionado(element) {
    // ...
    actualizarVistaLineas();  // Reconstruye TODOS
}

//  BUENO
function actualizarProductoSeleccionado(element) {
    const row = obtenerFilaLinea(element);
    // ...
    sincronizarCardLinea(row);  // Actualiza solo este
}
```

---

###  Problema: Eliminar línea crea una fila vacía

**Causa**: `removeLinea()` no encuentra el card

**Solución**: Verificar que `obtenerCardLineaPorId()` busca correctamente

```javascript
function removeLinea(button) {
    const row = obtenerFilaLinea(button);
    const idLinea = row.querySelector('input[name="idLinea"]')?.value;
    
    console.log("Eliminando línea con ID:", idLinea);  // Debug
    
    row.remove();
    
    const card = obtenerCardLineaPorId(idLinea);
    console.log("Card encontrado:", card);  // Debug
    
    if (card) {
        card.remove();
    }
}
```

---

###  Problema: Los valores no se sincronizan entre tabla y card

**Causa**: Campo tiene nombre diferente en tabla vs card

**Solución**: Asegurar que los `name` de inputs sean idénticos

```html
<!-- TABLA -->
<input name="cantidad" value="5">

<!-- CARD -->
<input name="cantidad" value="5">  <!-- Mismo name -->
```

---

###  Problema: El botón "Guardar" aparece deshabilitado permanentemente

**Causa**: `facturaCreadaPromise` nunca se resuelve (error en POST)

**Solución**: Verificar respuesta del servidor

```javascript
function mostrarPaso2() {
    facturaCreadaPromise = fetch('/facturas/guardar', ...)
        .then(res => {
            console.log("Status:", res.status);  // Debug
            if (res.ok) {
                return res.json().then(data => {
                    facturaId = data.idFactura;
                    document.getElementById("btnGuardar").disabled = false;  // ← Enable aquí
                    return data;
                });
            } else {
                console.error("Error response:", res);  // Debug
                document.getElementById("btnGuardar").disabled = true;
                throw new Error('Factura creation failed');
            }
        });
}
```

---

###  Problema: Cantidades grandes generan error "out of range"

**Causa**: Usando `Date.now()` para IDs temporales (13 dígitos > Integer.MAX_VALUE)

**Solución**: Usar números negativos decrecientes

```javascript
let temporalLineaId = -1;  // ← Empieza en -1

function addLinea() {
    const id = temporalLineaId--;  // -1, -2, -3... (siempre dentro de rango)
}
```

---

