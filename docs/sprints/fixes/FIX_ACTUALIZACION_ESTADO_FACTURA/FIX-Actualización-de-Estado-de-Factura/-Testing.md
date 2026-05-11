## 🧪 Testing

### Pasos para probar:

1. **Ir a Facturas:**
   ```
   http://localhost:9090/facturas
   ```

2. **Editar una factura existente:**
   - Click en el botón "Editar" de cualquier factura

3. **Cambiar el estado:**
   - Cambiar de "Pendiente" a "Entregado" (o viceversa)
   - Modificar alguna línea de producto (opcional)

4. **Guardar:**
   - Click en "Guardar Factura"

5. **Verificar en logs del servidor:**
   ```
   INFO ... Estado de factura 3 actualizado a: true
   ```

6. **Verificar en la base de datos:**
   ```sql
   SELECT id_factura, entregado, update_date 
   FROM factura 
   WHERE id_factura = 3;
   ```

7. **Verificar visualmente:**
   - El badge en la lista de facturas debe cambiar:
     - 🟢 "Entregado" (verde)
     - 🟡 "Pendiente" (amarillo)

---

