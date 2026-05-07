## 🧪 CASOS DE PRUEBA

### **Test Case 1: Crear primera configuración**
```
GIVEN: No existe configuración
WHEN: Admin accede a /configuracion/facturacion
THEN: 
  ✓ Sistema crea config por defecto (F001, 18%, PEN)
  ✓ Preview muestra "F001-00001"
  ✓ Formulario cargado con valores por defecto
```

### **Test Case 2: Guardar configuración**
```
GIVEN: Formulario completo
WHEN: Admin presiona "Guardar"
THEN:
  ✓ Validaciones pasan
  ✓ Config guardada en BD
  ✓ Mensaje de éxito
  ✓ Redirect a /configuracion/facturacion
```

### **Test Case 3: Crear factura con numeración**
```
GIVEN: Config activa (F001, número actual = 1)
WHEN: Usuario crea factura con subtotal = S/ 100
THEN:
  ✓ Número generado: "F001-00001"
  ✓ Serie: "F001"
  ✓ Subtotal: S/ 100.00
  ✓ IGV: S/ 18.00
  ✓ Total: S/ 118.00
  ✓ Número actual incrementado a 2
```

### **Test Case 4: Preview en tiempo real**
```
GIVEN: Usuario editando formato
WHEN: Cambia formato a "{serie}/{numero}"
THEN:
  ✓ Preview actualizado sin recargar: "F001/00001"
  
WHEN: Cambia serie a "B001"
THEN:
  ✓ Preview actualizado: "B001/00001"
```

### **Test Case 5: Validaciones**
```
GIVEN: Formulario con errores
WHEN: Serie vacía
THEN: ✓ Error: "La serie es requerida"

WHEN: Formato sin {numero}
THEN: ✓ Error: "El formato debe contener {numero}"

WHEN: Moneda con 2 letras
THEN: ✓ Error: "Debe tener 3 letras (ISO 4217)"

WHEN: Número actual < número inicial
THEN: ✓ Error: "Número actual no puede ser menor"
```

---

