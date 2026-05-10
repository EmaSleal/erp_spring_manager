## 🎯 COMPORTAMIENTO DEL SISTEMA

### Escenario 1: Primera factura del sistema

```
1. Usuario crea primera factura
2. No existe configuración → se crea automáticamente con valores por defecto
3. Se genera número: "F001-00001"
4. Se calcula IGV: subtotal * 18%
5. Se calcula total: subtotal + IGV
6. Se guarda factura
7. Número se incrementa a 2 para próxima factura
```

### Escenario 2: Facturas subsiguientes

```
1. Usuario crea nueva factura
2. Existe configuración activa
3. Se genera número: "F001-00002" (auto-incrementado)
4. Se calculan impuestos según configuración
5. Se guarda factura
6. Número se incrementa a 3
```

### Escenario 3: Cambio de configuración

```
1. Admin cambia serie de "F001" a "F002"
2. Admin establece número inicial: 100
3. Próxima factura será: "F002-00100"
4. Subsiguientes: F002-00101, F002-00102...
```

---

