## 🔍 Análisis de Causa Raíz

### Flujo del Bug

1. **Usuario agrega línea vacía:**
   - Click en botón "Agregar línea"
   - Se ejecuta `addLinea()`
   
2. **Línea se crea con ID temporal:**
   ```javascript
   const randomId = Date.now(); // Ej: 1729446527000
   const html = createLineaRow({
       ...
       idProducto: randomId,  // ❌ ID temporal (timestamp)
       ...
   });
   ```

3. **Usuario NO selecciona producto:**
   - El dropdown queda en opción por defecto
   - `idProducto` mantiene el valor temporal (timestamp)
   - Función `actualizarProductoSeleccionado()` nunca se ejecuta

4. **Al guardar:**
   ```javascript
   function guardarLineas() {
       rows.forEach((row, index) => {
           const idProducto = row.querySelector('input[name="idProducto"]').value;
           // idProducto = 1729446527000 (timestamp inválido)
           
           lineas.push({
               id_producto: parseInt(idProducto), // ❌ Envía timestamp
               ...
           });
       });
   }
   ```

5. **Backend intenta insertar:**
   ```sql
   CALL sp_actualizar_linea_factura(
       NULL,              -- id_linea_factura
       1,                 -- numero_linea
       1729446527000,     -- ❌ id_producto inválido
       26,                -- id_factura
       NULL,              -- descripcion
       1,                 -- cantidad
       0,                 -- precio
       0                  -- subtotal
   );
   ```

6. **Constraint violation:**
   - La FK `id_producto` no encuentra un producto con ese ID
   - MySQL lanza: `Column 'id_producto' cannot be null`

### Archivos Afectados

- **`editar-factura.js`** - Función `guardarLineas()` y `createLineaRow()`
- **Stored Procedure:** `sp_actualizar_linea_factura`

---

