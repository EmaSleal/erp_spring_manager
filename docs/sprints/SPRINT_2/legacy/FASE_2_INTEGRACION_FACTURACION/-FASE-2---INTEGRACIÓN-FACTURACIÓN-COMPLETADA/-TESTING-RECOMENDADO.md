## 🧪 TESTING RECOMENDADO

### Test Case 1: Primera factura
```
Given: No existe configuración
When: Se crea primera factura
Then: 
  - Se crea configuración por defecto
  - Número generado: "F001-00001"
  - IGV calculado correctamente
  - Total calculado correctamente
```

### Test Case 2: Numeración secuencial
```
Given: Existe configuración con número actual = 5
When: Se crean 3 facturas consecutivas
Then:
  - Factura 1: "F001-00005"
  - Factura 2: "F001-00006"
  - Factura 3: "F001-00007"
  - Número actual = 8
```

### Test Case 3: Cálculo de IGV incluido
```
Given: incluirIgvEnPrecio = true, igv = 18%
When: Se crea factura con subtotal = 118.00
Then:
  - Base = 100.00
  - IGV = 18.00
  - Total = 118.00
```

### Test Case 4: Cálculo de IGV no incluido
```
Given: incluirIgvEnPrecio = false, igv = 18%
When: Se crea factura con subtotal = 100.00
Then:
  - Base = 100.00
  - IGV = 18.00
  - Total = 118.00
```

---

