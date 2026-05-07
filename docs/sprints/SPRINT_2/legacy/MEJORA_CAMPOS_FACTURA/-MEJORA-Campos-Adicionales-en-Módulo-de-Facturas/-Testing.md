## 🧪 Testing

### Caso de Prueba 1: Campos Automáticos

**Pasos:**
1. Abrir formulario de nueva factura
2. Seleccionar cliente
3. Seleccionar fecha de entrega: `2025-10-20`
4. **NO** ingresar serie ni número
5. Continuar a Paso 2
6. Agregar líneas
7. Guardar

**Resultado Esperado:**
- ✅ `fechaPago` se calcula automáticamente: `2025-10-27` (+7 días)
- ✅ `serie` se genera desde configuración: "FA01"
- ✅ `numeroFactura` se genera automáticamente: "FA01-00001"
- ✅ Factura se guarda correctamente

---

### Caso de Prueba 2: Campos Manuales

**Pasos:**
1. Abrir formulario de nueva factura
2. Seleccionar cliente
3. Seleccionar fecha de entrega: `2025-10-20`
4. Ingresar serie: "B001"
5. Ingresar número: "FACTURA-2025-XYZ"
6. Modificar fecha de pago: `2025-10-30`
7. Continuar y guardar

**Resultado Esperado:**
- ✅ `serie` = "B001" (respeta valor manual)
- ✅ `numeroFactura` = "FACTURA-2025-XYZ" (respeta valor manual)
- ✅ `fechaPago` = "2025-10-30" (respeta valor manual)
- ✅ No se auto-genera nada

---

### Caso de Prueba 3: Resumen de Totales

**Pasos:**
1. Crear factura
2. Agregar línea 1: Producto A, cantidad 2, precio $1,000
3. **Verificar:** Subtotal = $2,000, Total = $2,000
4. Agregar línea 2: Producto B, cantidad 1, precio $5,000
5. **Verificar:** Subtotal = $7,000, Total = $7,000
6. Cambiar cantidad línea 1 a 5
7. **Verificar:** Subtotal = $10,000, Total = $10,000
8. Eliminar línea 2
9. **Verificar:** Subtotal = $5,000, Total = $5,000

**Resultado Esperado:**
- ✅ Resumen se actualiza en tiempo real
- ✅ Sin necesidad de recargar página
- ✅ Cálculos correctos

---

### Caso de Prueba 4: Visualización en Tabla

**Pasos:**
1. Ir a `/facturas`
2. Observar tabla de facturas

**Resultado Esperado:**
- ✅ Columna "N° Factura" visible
- ✅ Número destacado en azul
- ✅ Serie mostrada como subtítulo
- ✅ Todas las facturas muestran su número

---

### Caso de Prueba 5: Modal de Detalle

**Pasos:**
1. Abrir detalle de factura
2. Verificar sección "Información General"

**Resultado Esperado:**
- ✅ N° Factura visible y en negrita
- ✅ Serie visible
- ✅ Fecha límite de pago visible
- ✅ Si no hay fecha de pago: "No especificada"

---

