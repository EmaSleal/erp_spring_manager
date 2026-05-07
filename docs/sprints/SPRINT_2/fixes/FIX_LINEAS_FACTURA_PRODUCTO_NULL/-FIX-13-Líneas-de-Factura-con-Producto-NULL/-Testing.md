## 🧪 Testing

### Caso de Prueba 1: Línea Completamente Vacía

**Pasos:**
1. Abrir modal de nueva factura
2. Completar paso 1 (cliente, fecha)
3. Ir a paso 2 (líneas)
4. Click en "Agregar línea"
5. **No modificar ningún campo**
6. Click en "Guardar factura"

**Resultado Esperado:**
- ✅ Sistema muestra alerta: "Debe seleccionar al menos un producto válido"
- ✅ No se envía la línea vacía al backend
- ✅ No se lanza error de base de datos

---

### Caso de Prueba 2: Mix de Líneas Válidas y Vacías

**Pasos:**
1. Abrir modal de nueva factura
2. Completar paso 1
3. Agregar 3 líneas:
   - Línea 1: Producto A, cantidad 2 ✅
   - Línea 2: **Sin producto** ❌
   - Línea 3: Producto B, cantidad 1 ✅
4. Click en "Guardar"

**Resultado Esperado:**
- ✅ Línea 2 se omite automáticamente
- ✅ Solo se guardan líneas 1 y 3
- ✅ Líneas se renumeran: 1, 2 (no 1, 3)
- ✅ Console log: "Se omitieron 1 línea(s) vacía(s)"
- ✅ Factura se guarda exitosamente

---

### Caso de Prueba 3: Línea Parcialmente Completa

**Pasos:**
1. Agregar línea
2. Cambiar cantidad a 5
3. **No seleccionar producto**
4. Guardar

**Resultado Esperado:**
- ✅ Línea se omite (falta el producto más importante)
- ✅ Alerta: "Debe seleccionar al menos un producto válido"

---

### Caso de Prueba 4: Edición de Factura Existente

**Pasos:**
1. Editar factura existente con 2 líneas
2. Agregar tercera línea pero no seleccionar producto
3. Guardar

**Resultado Esperado:**
- ✅ Las 2 líneas originales se mantienen
- ✅ Tercera línea vacía se omite
- ✅ Factura actualizada correctamente

---

