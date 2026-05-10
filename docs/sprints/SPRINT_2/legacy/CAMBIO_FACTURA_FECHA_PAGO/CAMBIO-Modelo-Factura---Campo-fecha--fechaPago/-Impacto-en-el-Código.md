## 🎯 Impacto en el Código

### **Archivos Modificados**

#### 1. **Factura.java** ✅
```java
// Antes
private Timestamp fecha;

// Después
@Column(name = "fechaPago")
private Date fechaPago;
```

#### 2. **Vistas HTML** (Pendiente de actualizar)
- `facturas/facturas.html` - Tabla de facturas
- `facturas/form.html` - Formulario crear/editar
- `facturas/detalle.html` - Vista detallada

**Cambios necesarios:**
- Agregar columna "Fecha de Pago" en tabla
- Campo `fechaPago` en formulario (auto-calculado o editable)
- Mostrar fecha de pago en detalle

#### 3. **JavaScript** (Pendiente de actualizar)
- `facturas.js` - Lógica del formulario

**Cambios necesarios:**
- Calcular `fechaPago` automáticamente al seleccionar `fechaEntrega`
- Validar que `fechaPago >= fechaEntrega`

---

